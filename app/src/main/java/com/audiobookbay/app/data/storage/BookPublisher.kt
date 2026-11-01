package com.audiobookbay.app.data.storage

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Serializable
private data class CopiedFile(val uri: String, val size: Long, val sha256: String)
@Serializable
private data class PublicationJournal(
    val tree: String, val author: String, val title: String, val pendingName: String,
    val files: List<PublicationFile>, val copied: Map<String, CopiedFile> = emptyMap(),
    val finalName: String? = null, val finalUri: String? = null,
)

/** A durable publication transaction, independent of native torrent/session lifetime. */
class BookPublisher(private val journals: File, private val storage: PublicationStorage, private val json: Json) {
    private val mutex = Mutex()

    suspend fun publish(id: String, tree: String, author: String, title: String, files: List<PublicationFile>, onProgress: suspend (Float) -> Unit): String = mutex.withLock {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,80}")))
        require(files.isNotEmpty()) { "No verified audio to publish" }
        journals.mkdirs()
        val journalFile = File(journals, "$id.publication")
        var journal = if (journalFile.isFile) json.decodeFromString<PublicationJournal>(journalFile.readText())
            else PublicationJournal(tree, BookFolders.name(author), BookFolders.name(title), ".audiobay-$id", files)
        if (journal.tree != tree || journal.author != BookFolders.name(author) || journal.title != BookFolders.name(title)) {
            check(journal.finalUri == null) { "This book is already published" }
            journal = PublicationJournal(tree, BookFolders.name(author), BookFolders.name(title), ".audiobay-$id", files)
        }
        require(journal.files == files) { "Verified files changed during publication" }
        val root = storage.root(tree)
        val authorDir = folder(root, journal.author)
        journal.finalUri?.takeIf(storage::exists)?.let { onProgress(1f); return@withLock it }
        // Recover a successful rename if the process stopped before saving the returned URI.
        journal.finalName?.let { name ->
            child(authorDir, name)?.takeIf { it.isDirectory }?.let { final ->
                if (owns(final.uri, id)) {
                    journal = journal.copy(finalUri = final.uri)
                    save(journalFile, journal)
                    onProgress(1f)
                    return@withLock final.uri
                }
            }
        }
        save(journalFile, journal)
        val pending = child(authorDir, journal.pendingName)?.let {
            require(it.isDirectory) { "Publication path is not a folder" }
            if (!owns(it.uri, id)) {
                val children = storage.children(it.uri)
                require(children.all { child -> child.name == MARKER }) { "Publication folder belongs to another download" }
                val marker = children.firstOrNull()?.uri ?: storage.file(it.uri, MARKER)
                storage.write(marker).use { output -> output.write(id.toByteArray()) }
            }
            it.uri
        } ?: storage.directory(authorDir, journal.pendingName).also { uri ->
            val marker = storage.file(uri, MARKER)
            storage.write(marker).use { it.write(id.toByteArray()) }
        }
        val total = journal.files.sumOf { File(it.source).length() }.coerceAtLeast(1)
        var copiedBytes = 0L
        for (entry in journal.files) {
            currentCoroutineContext().ensureActive()
            val source = File(entry.source)
            require(source.isFile) { "Verified staging audio is missing" }
            val checkpoint = journal.copied[entry.relativePath]
            if (checkpoint != null && storage.exists(checkpoint.uri) && checkpoint.size == source.length() && digest(checkpoint.uri) == checkpoint.sha256) {
                copiedBytes += source.length()
                onProgress(copiedBytes.toFloat() / total)
                continue
            }
            val parts = entry.relativePath.split('/')
            require(parts.all { it.isNotBlank() && it != "." && it != ".." })
            var parent = pending
            parts.dropLast(1).forEach { parent = folder(parent, it) }
            child(parent, parts.last())?.let { require(!it.isDirectory); storage.delete(it.uri) }
            val destination = storage.file(parent, parts.last())
            val hash = MessageDigest.getInstance("SHA-256")
            var count = 0L
            source.inputStream().use { input -> storage.write(destination).use { output ->
                val buffer = ByteArray(128 * 1024)
                var lastReported = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    hash.update(buffer, 0, read)
                    count += read
                    if (count - lastReported >= 4 * 1024 * 1024) {
                        onProgress((copiedBytes + count).toFloat() / total)
                        lastReported = count
                    }
                }
            } }
            check(count == source.length()) { "Source changed during publication" }
            val expected = hash.digest().hex()
            check(digest(destination) == expected) { "Shared copy verification failed" }
            copiedBytes += count
            journal = journal.copy(copied = journal.copied + (entry.relativePath to CopiedFile(destination, count, expected)))
            save(journalFile, journal)
            onProgress(copiedBytes.toFloat() / total)
        }
        var finalName = journal.title
        var suffix = 2
        while (child(authorDir, finalName) != null) finalName = "${journal.title} (${suffix++})"
        journal = journal.copy(finalName = finalName)
        save(journalFile, journal)
        val result = storage.rename(pending, finalName)
        journal = journal.copy(finalUri = result)
        save(journalFile, journal)
        onProgress(1f)
        result
    }

    private fun child(parent: String, name: String) = storage.children(parent).firstOrNull { it.name.equals(name, ignoreCase = true) }
    private fun folder(parent: String, name: String): String = child(parent, name)?.let {
        require(it.isDirectory) { "A file already uses the requested folder name" }; it.uri
    } ?: storage.directory(parent, name)
    private fun owns(uri: String, id: String) = child(uri, MARKER)?.let { marker ->
        storage.read(marker.uri).use { it.bufferedReader().readText().take(100) == id }
    } ?: false
    private suspend fun digest(uri: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
        storage.read(uri).use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().hex()
    }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun save(file: File, journal: PublicationJournal) {
        val pending = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(pending).use { it.write(json.encodeToString(journal).toByteArray()); it.fd.sync() }
        Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
    companion object { private const val MARKER = ".audiobay-publication" }
}
