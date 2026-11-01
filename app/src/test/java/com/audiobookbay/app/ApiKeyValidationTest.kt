package com.audiobookbay.app

import com.audiobookbay.app.data.storage.normalizeApiKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiKeyValidationTest {
    @Test fun trimsPastedKeysAndAllowsRemoval() {
        assertEquals("test_Abc-123", normalizeApiKey("  test_Abc-123\n"))
        assertEquals("", normalizeApiKey("  "))
    }

    @Test fun acceptsQuotedAndInvisibleClipboardEdges() {
        val key = "AIza" + "x".repeat(35)
        assertEquals(key, normalizeApiKey("\uFEFF\u200B\"$key\"\u200B"))
        assertEquals(key, normalizeApiKey(" '$key' "))
    }

    @Test fun rejectsHeadersWhitespaceAndOversizedInput() {
        listOf("key\r\nheader", "Bearer key", "a".repeat(513), "ключ").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { normalizeApiKey(value) }
        }
    }
}
