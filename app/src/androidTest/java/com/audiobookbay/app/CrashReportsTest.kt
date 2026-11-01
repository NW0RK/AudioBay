package com.audiobookbay.app

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.diagnostics.CrashReports
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CrashReportsTest {
    @Test fun persistsSanitizedReportAndDelegatesToAndroidHandler() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(target.cacheDir, "crash-report-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = root
        }
        val original = Thread.getDefaultUncaughtExceptionHandler()
        var delegated = false
        try {
            Thread.setDefaultUncaughtExceptionHandler { _, _ -> delegated = true }
            CrashReports(context, Dispatchers.IO).installHandler()
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(
                Thread.currentThread(), IllegalStateException("private credential must be excluded"))
            assertTrue(delegated)
            val saved = File(root, "crash-reports").listFiles()!!.single().readText()
            assertTrue(saved.contains("IllegalStateException"))
            assertTrue(saved.contains("Device:"))
            assertFalse(saved.contains("private credential"))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
            root.deleteRecursively()
        }
    }
}
