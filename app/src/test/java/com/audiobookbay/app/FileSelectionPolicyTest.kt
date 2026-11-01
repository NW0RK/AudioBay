package com.audiobookbay.app

import com.audiobookbay.app.domain.model.TorrentFile
import com.audiobookbay.app.torrent.FileSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSelectionPolicyTest {
    private val files = listOf(
        TorrentFile(0, "chapter-01.mp3", 10, isAudioFile = true),
        TorrentFile(1, "cover.jpg", 5, isAudioFile = false, isSelectedForDownload = false),
        TorrentFile(2, "chapter-02.mp3", 10, isAudioFile = true),
    )

    @Test fun togglingOneTrackKeepsTheRest() {
        val updated = FileSelectionPolicy.apply(files, mapOf(0 to false))
        assertFalse(updated.first { it.index == 0 }.isSelectedForDownload)
        assertTrue(updated.first { it.index == 2 }.isSelectedForDownload)
        assertFalse(updated.first { it.index == 1 }.isSelectedForDownload)
    }

    @Test(expected = IllegalArgumentException::class) fun cannotDeselectEveryTrack() {
        FileSelectionPolicy.apply(files, mapOf(0 to false, 2 to false))
    }

    @Test(expected = IllegalArgumentException::class) fun cannotSelectNonAudioFile() {
        FileSelectionPolicy.apply(files, mapOf(1 to true))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownIndex() {
        FileSelectionPolicy.apply(files, mapOf(9 to false))
    }

    @Test fun explicitFalseForNonAudioFileIsAllowed() {
        val updated = FileSelectionPolicy.apply(files, mapOf(1 to false))
        assertEquals(files, updated)
    }
}
