package com.audiobookbay.app.torrent

import java.io.File

/** Rules shared by metadata inspection and native file selection. */
internal object TorrentFiles {
    private val audioExtensions = setOf("mp3", "m4b", "m4a", "aac", "ogg", "opus", "flac", "wav")
    fun isAudio(path: String) = path.substringAfterLast('.', "").lowercase() in audioExtensions
    fun resolveInside(root: File, path: String): File {
        require(!File(path).isAbsolute) { "Absolute torrent file path" }
        val base = root.canonicalFile
        val file = File(base, path).canonicalFile
        require(file.toPath().startsWith(base.toPath()) && file != base) { "Torrent path escapes download directory" }
        return file
    }
    fun selected(paths: List<String>, indices: List<Int>?): Set<Int> {
        val chosen = indices?.toSet() ?: paths.indices.filter { isAudio(paths[it]) }.toSet()
        require(chosen.isNotEmpty()) { "No audio files selected" }
        require(chosen.all { it in paths.indices }) { "Invalid file selection" }
        return chosen
    }
}
