package com.audiobookbay.app

import com.audiobookbay.app.torrent.TorrentFiles
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TorrentFilesTest {
    @Test fun selectsAudioFilesAndIgnoresArtwork() {
        assertEquals(setOf(0, 2), TorrentFiles.selected(listOf("chapter.MP3", "cover.jpg", "book.m4b", "readme.txt"), null))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsEmptySelection() {
        TorrentFiles.selected(listOf("cover.jpg"), null)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidIndex() {
        TorrentFiles.selected(listOf("book.mp3"), listOf(2))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsTraversal() {
        TorrentFiles.resolveInside(File("downloads"), "../outside.mp3")
    }
    @Test fun allowsNestedAudioPath() {
        val root = File("downloads").canonicalFile
        assertEquals(File(root, "book/chapter.mp3"), TorrentFiles.resolveInside(root, "book/chapter.mp3"))
    }
}
