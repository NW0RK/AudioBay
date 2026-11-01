package com.audiobookbay.app

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.data.storage.ApiKeySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ApiKeySettingsTest {
    @Test fun encryptedCredentialSurvivesReopenAndCanBeRemoved() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "key-settings-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            val first = ApiKeySettings(context, Dispatchers.IO)
            assertEquals("", first.key())
            val synthetic = "AIza" + "x".repeat(35)
            first.save("\uFEFF\"$synthetic\"\u200B")
            assertTrue(first.configured.value)
            val bytes = File(directory, "gemini-key").readBytes()
            assertFalse(bytes.toString(Charsets.ISO_8859_1).contains(synthetic))
            val reopened = ApiKeySettings(context, Dispatchers.IO)
            assertEquals(synthetic, reopened.key())
            assertTrue(reopened.configured.value)
            try {
                reopened.save("Bearer bad-input")
                fail("Invalid input must fail")
            } catch (e: com.audiobookbay.app.data.storage.ApiKeySaveException) {
                assertEquals(com.audiobookbay.app.data.storage.ApiKeyFailure.FORMAT, e.failure)
            }
            assertEquals(synthetic, reopened.key())
            reopened.save("replacement-key_123")
            assertEquals("replacement-key_123", ApiKeySettings(context, Dispatchers.IO).key())
            reopened.save("")
            assertFalse(reopened.configured.value)
            assertFalse(File(directory, "gemini-key").exists())
            assertEquals("", ApiKeySettings(context, Dispatchers.IO).key())
        } finally {
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }
}
