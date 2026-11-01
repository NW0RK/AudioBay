package com.audiobookbay.app.torrent

import com.audiobookbay.app.domain.model.TorrentFile

/**
 * Applies a user's file selection without allowing an unusable download.
 *
 * Only audio tracks may be enabled and at least one must remain selected, because the publisher
 * rejects an empty or non-audio manifest. Files that the UI never sends keep their previous state.
 */
internal object FileSelectionPolicy {
    fun apply(files: List<TorrentFile>, requested: Map<Int, Boolean>): List<TorrentFile> {
        require(files.isNotEmpty()) { "No files in this torrent" }
        val known = files.associateBy { it.index }
        require(requested.keys.all { it in known }) { "Unknown torrent file index" }
        require(requested.all { (index, selected) -> !selected || known.getValue(index).isAudioFile }) {
            "Only audio files can be selected"
        }
        val updated = files.map { file ->
            requested[file.index]?.let { selected -> file.copy(isSelectedForDownload = selected) } ?: file
        }
        require(updated.any { it.isAudioFile && it.isSelectedForDownload }) { "Select at least one audio file" }
        return updated
    }
}
