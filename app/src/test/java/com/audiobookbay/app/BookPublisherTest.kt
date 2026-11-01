package com.audiobookbay.app

import com.audiobookbay.app.data.storage.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.nio.file.Files

class BookPublisherTest {
    private lateinit var temp: File
    private lateinit var source: File
    private lateinit var shared: File
    private lateinit var store: TestDocumentStorage
    private lateinit var publisher: BookPublisher
    @Before fun setup() {
        temp = Files.createTempDirectory("audiobay-publication").toFile()
        source = File(temp, "staging").apply { mkdirs() }
        shared = File(temp, "AudioBooks").apply { mkdirs() }
        store = TestDocumentStorage()
        publisher = BookPublisher(File(temp, "journals"), store, Json)
    }
    @After fun cleanup() { temp.deleteRecursively() }
    private fun tracks(): List<PublicationFile> {
        File(source, "Book/Disc 1").mkdirs()
        File(source, "Book/Disc 1/01.mp3").writeBytes(ByteArray(20000) { 12 })
        File(source, "Book/Disc 1/02.mp3").writeBytes(ByteArray(20000) { 34 })
        return BookFolders.files(source, listOf("Book/Disc 1/01.mp3", "Book/Disc 1/02.mp3"), "Book")
    }
    @Test fun publishesAuthorBookAndPreservesDiscLayout() = runTest {
        val result = publisher.publish("one", shared.path, "Jane Austen", "My Book", tracks()) { }
        assertEquals(File(shared, "Jane Austen/My Book").path, result)
        assertArrayEquals(File(source, "Book/Disc 1/01.mp3").readBytes(), File(result, "Disc 1/01.mp3").readBytes())
        assertFalse(File(shared, "Jane Austen/.audiobay-one").exists())
    }
    @Test fun existingBookIsNeverOverwritten() = runTest {
        val tracks = tracks()
        publisher.publish("one", shared.path, "Author", "Book", tracks) { }
        val second = publisher.publish("two", shared.path, "Author", "Book", tracks) { }
        assertEquals("Book (2)", File(second).name)
        assertTrue(File(shared, "Author/Book/Disc 1/01.mp3").isFile)
    }
    @Test fun failedCopyResumesFromVerifiedCheckpointAfterRecreation() = runTest {
        val tracks = tracks()
        store.failFileName = "02.mp3"
        try { publisher.publish("one", shared.path, "Author", "Book", tracks) { }; fail("Expected full disk") } catch (_: IOException) { }
        assertFalse(File(shared, "Author/Book").exists())
        assertTrue(File(tracks[0].source).isFile)
        val first = File(shared, "Author/.audiobay-one/Disc 1/01.mp3").path
        assertEquals(1, store.writes[first])
        store.failFileName = null
        publisher = BookPublisher(File(temp, "journals"), store, Json)
        val result = publisher.publish("one", shared.path, "Author", "Book", tracks) { }
        assertEquals(1, store.writes[first])
        assertArrayEquals(File(tracks[1].source).readBytes(), File(result, "Disc 1/02.mp3").readBytes())
    }
    @Test fun permissionRevocationRetainsSourceAndAllowsRetry() = runTest {
        val tracks = tracks(); store.allowed = false
        try { publisher.publish("one", shared.path, "Author", "Book", tracks) { }; fail("Expected denied permission") } catch (_: SecurityException) { }
        assertTrue(File(tracks[0].source).isFile)
        store.allowed = true
        assertTrue(File(publisher.publish("one", shared.path, "Author", "Book", tracks) { }).isDirectory)
    }
    @Test fun renameCrashRecoversWithoutDuplicatingBook() = runTest {
        val tracks = tracks(); store.crashAfterRename = true
        try { publisher.publish("one", shared.path, "Author", "Book", tracks) { }; fail("Expected interruption") } catch (_: IOException) { }
        store.crashAfterRename = false
        publisher = BookPublisher(File(temp, "journals"), store, Json)
        assertEquals(File(shared, "Author/Book").path, publisher.publish("one", shared.path, "Author", "Book", tracks) { })
        assertFalse(File(shared, "Author/Book (2)").exists())
    }
    @Test fun destinationCanChangeAfterFailedPublicationWithoutRedownloading() = runTest {
        val tracks = tracks(); store.failFileName = "02.mp3"
        try { publisher.publish("one", shared.path, "Author", "Book", tracks) { } } catch (_: IOException) { }
        val other = File(temp, "Other").apply { mkdirs() }; store.failFileName = null
        assertEquals(File(other, "Correct Author/Book").path, publisher.publish("one", other.path, "Correct Author", "Book", tracks) { })
    }
    @Test fun cancelledPublicationResumesWithoutRecopyingVerifiedAudio() = runTest {
        val tracks = tracks()
        try {
            publisher.publish("one", shared.path, "Author", "Book", tracks) { if (it > 0) throw kotlinx.coroutines.CancellationException("Stopped") }
        } catch (_: kotlinx.coroutines.CancellationException) { }
        val first = File(shared, "Author/.audiobay-one/Disc 1/01.mp3").path
        publisher = BookPublisher(File(temp, "journals"), store, Json)
        val result = publisher.publish("one", shared.path, "Author", "Book", tracks) { }
        assertEquals(1, store.writes[first])
        assertTrue(File(result, "Disc 1/02.mp3").exists())
    }
    @Test fun corruptedCopyIsNotPublishedAndCanBeRetried() = runTest {
        val tracks = tracks(); store.corruptReadName = "01.mp3"
        try { publisher.publish("one", shared.path, "Author", "Book", tracks) { }; fail("Corrupt copy accepted") } catch (_: IllegalStateException) { }
        assertFalse(File(shared, "Author/Book").exists())
        store.corruptReadName = null
        val result = publisher.publish("one", shared.path, "Author", "Book", tracks) { }
        assertArrayEquals(File(tracks[0].source).readBytes(), File(result, "Disc 1/01.mp3").readBytes())
    }
    @Test fun sanitizesFoldersAndRejectsTraversal() {
        assertEquals("Ursula K. Le Guin", BookFolders.name(" Ursula K. Le Guin "))
        assertEquals("A_B__C", BookFolders.name("A/B:*C"))
        assertEquals("თომას მანნი", BookFolders.name("თომას მანნი"))
        try { BookFolders.files(source, listOf("../outside.mp3"), null); fail("Unsafe path accepted") } catch (_: IllegalArgumentException) { }
        try { BookFolders.name(".."); fail("Empty name accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun wrapperIsRemovedButDiscFoldersAndTrackNamesRemain() {
        val result = BookFolders.files(source, listOf("Disc 1/02.mp3"), "Disc 1")
        assertEquals("Disc 1/02.mp3", result.single().relativePath)
    }
}

internal class TestDocumentStorage : PublicationStorage {
    var allowed = true
    var failFileName: String? = null
    var crashAfterRename = false
    var corruptReadName: String? = null
    val writes = mutableMapOf<String, Int>()
    override fun root(treeUri: String): String { if (!allowed) throw SecurityException("Access revoked"); return treeUri }
    override fun exists(uri: String) = File(uri).exists()
    override fun children(parent: String) = File(parent).listFiles().orEmpty().map { PublicationStorage.Document(it.path, it.name, it.isDirectory) }
    override fun directory(parent: String, name: String) = File(parent, name).also { check(it.mkdir()) }.path
    override fun file(parent: String, name: String) = File(parent, name).also { check(it.createNewFile()) }.path
    override fun read(uri: String): InputStream = if (File(uri).name == corruptReadName) {
        val bytes = File(uri).readBytes(); if (bytes.isNotEmpty()) bytes[0] = (bytes[0].toInt() xor 1).toByte()
        ByteArrayInputStream(bytes)
    } else File(uri).inputStream()
    override fun write(uri: String): OutputStream {
        writes[uri] = (writes[uri] ?: 0) + 1
        val output = File(uri).outputStream()
        if (File(uri).name != failFileName) return output
        return object : OutputStream() {
            override fun write(value: Int) { output.write(value); throw IOException("Disk full") }
            override fun write(bytes: ByteArray, offset: Int, length: Int) { output.write(bytes, offset, minOf(100, length)); throw IOException("Disk full") }
            override fun close() { output.close() }
        }
    }
    override fun rename(uri: String, name: String): String {
        val target = File(File(uri).parentFile, name)
        check(!target.exists() && File(uri).renameTo(target))
        if (crashAfterRename) throw IOException("Process interrupted after rename")
        return target.path
    }
    override fun delete(uri: String) { check(File(uri).deleteRecursively()) }
}
