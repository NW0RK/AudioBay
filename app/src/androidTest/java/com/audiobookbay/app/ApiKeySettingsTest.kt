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
            first.save("  synthetic-key_123  ")
            assertTrue(first.configured.value)
            val bytes = File(directory, "gemini-key").readBytes()
            assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("synthetic-key_123"))
            val reopened = ApiKeySettings(context, Dispatchers.IO)
            assertEquals("synthetic-key_123", reopened.key())
            assertTrue(reopened.configured.value)
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
