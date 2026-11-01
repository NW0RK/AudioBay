package com.audiobookbay.app

import com.audiobookbay.app.data.local.entity.AudiobookEntity
import com.audiobookbay.app.data.local.entity.PlaybackProgressEntity
import com.audiobookbay.app.domain.model.AudiobookSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMappingTest {

    @Test
    fun audiobookEntity_toDomain_mapsAllFieldsCorrectly() {
        val entity = AudiobookEntity(
            id = "test-123",
            title = "Test Audiobook",
            author = "Test Author",
            narrator = "Test Narrator",
            coverImageUrl = "https://example.com/cover.jpg",
            category = "Sci-Fi",
            durationSeconds = 3600L,
            formattedSize = "150 MB",
            detailUrl = "https://audiobookbay.lu/audio/test-123",
            infoHash = "abcdef1234567890abcdef1234567890abcdef12",
            source = AudiobookSource.AUDIOBOOKBAY.name,
        )

        val domain = entity.toDomain()

        assertEquals("test-123", domain.id)
        assertEquals("Test Audiobook", domain.title)
        assertEquals("Test Author", domain.author)
        assertEquals("Test Narrator", domain.narrator)
        assertEquals(AudiobookSource.AUDIOBOOKBAY, domain.source)
    }

    @Test
    fun playbackProgressEntity_toDomain_mapsCorrectly() {
        val entity = PlaybackProgressEntity(
            audiobookId = "book-456",
            currentPositionMs = 120_000L,
            totalDurationMs = 600_000L,
            currentChapterIndex = 2,
            isCompleted = false,
        )

        val domain = entity.toDomain()

        assertEquals("book-456", domain.audiobookId)
        assertEquals(120_000L, domain.currentPositionMs)
        assertEquals(600_000L, domain.totalDurationMs)
        assertEquals(2, domain.currentChapterIndex)
        assertFalse(domain.isCompleted)
    }
}
