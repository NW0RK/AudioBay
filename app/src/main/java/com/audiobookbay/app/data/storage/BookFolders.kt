package com.audiobookbay.app.data.storage

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PublicationFile(val source: String, val relativePath: String)

object BookFolders {
    fun name(value: String): String {
        var clean = value.replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*]"), "_").trim().trimEnd('.', ' ')
        require(clean.isNotBlank() && clean != "." && clean != "..") { "Author and title must contain a usable name" }
        while (clean.toByteArray(Charsets.UTF_8).size > 180) clean = clean.dropLast(Character.charCount(clean.codePointBefore(clean.length)))
        return clean
    }

    fun files(staging: File, relativePaths: List<String>, torrentRoot: String?): List<PublicationFile> {
        val root = staging.canonicalFile
        val used = mutableSetOf<String>()
        return relativePaths.map { path ->
            val parts = path.replace('\\', '/').split('/')
            require(parts.all { it.isNotBlank() && it != "." && it != ".." } && !File(path).isAbsolute) { "Unsafe torrent path" }
            val source = File(root, path).canonicalFile
            require(source.toPath().startsWith(root.toPath()) && source != root) { "Unsafe source path" }
            val strip = parts.size > 1 && parts.first() == torrentRoot &&
                !Regex("(?i)(disc|disk|cd|part)[ _-]*[0-9]+").matches(parts.first())
            val safe = (if (strip) parts.drop(1) else parts).map(::name)
            val parent = safe.dropLast(1).joinToString("/").let { if (it.isEmpty()) "" else "$it/" }
            val file = safe.last()
            var candidate = parent + file
            var suffix = 2
            while (!used.add(candidate.lowercase())) {
                val stem = file.substringBeforeLast('.', file)
                val ext = file.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
                candidate = "$parent$stem (${suffix++})$ext"
            }
            PublicationFile(source.path, candidate)
        }
    }
}
