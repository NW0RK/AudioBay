package com.audiobookbay.app

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.diagnostics.CrashReports
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CrashReportsTest {
    @Test fun interruptedNativeSessionRecoveredWithoutAndroidCrashRecord() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(target.cacheDir, "native-marker-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(target) { override fun getNoBackupFilesDir(): File = root }
        try {
            CrashReports(context, Dispatchers.IO).checkpoint(CrashReports.Stage.ADD_TORRENT)
            val nextProcess = CrashReports(context, Dispatchers.IO)
            nextProcess.recoverInterruptedSession()
            val saved = File(root, "crash-reports").listFiles()!!.single().readText()
            assertTrue(saved.contains("ADD_TORRENT"))
            assertTrue(saved.contains("not proof of a crash"))
            nextProcess.recoverInterruptedSession()
            assertEquals(1, File(root, "crash-reports").listFiles()!!.size)
            nextProcess.checkpoint(CrashReports.Stage.ENGINE_RUNNING)
            nextProcess.engineStopped()
            nextProcess.recoverInterruptedSession()
            assertEquals(1, File(root, "crash-reports").listFiles()!!.size)
        } finally { root.deleteRecursively() }
    }

    @Test fun exportIncludesDurableStagesAndNoDownloadMetadata() = kotlinx.coroutines.runBlocking {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(target.cacheDir, "export-diagnostic-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = root
            override fun getSystemService(name: String): Any? = throw SecurityException("History unavailable")
        }
        try {
            val reports = CrashReports(context, Dispatchers.IO)
            repeat(90) { reports.checkpoint(CrashReports.Stage.TORRENT_SUBMITTED) }
            val text = reports.exportText()
            assertTrue(text.contains("DURABLE TORRENT EVENTS"))
            assertTrue(text.contains("TORRENT_SUBMITTED"))
            assertTrue(text.contains("Memory page size:"))
            assertTrue(text.contains("Own-UID Android log summary") || text.contains("App log query"))
            assertFalse(text.contains("History unavailable"))
            val journal = File(root, "torrent-diagnostic-events").readLines()
            assertEquals(80, journal.size)
            reports.clear()
            assertFalse(File(root, "torrent-diagnostic-events").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun manualDiagnosticRemainsAvailableWhenAndroidHistoryFails() = kotlinx.coroutines.runBlocking {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(target.cacheDir, "manual-diagnostic-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = root
            override fun getSystemService(name: String): Any? = throw SecurityException("History unavailable")
        }
        try {
            val reports = CrashReports(context, Dispatchers.IO)
            reports.createDiagnostic()
            val saved = reports.load().single()
            assertTrue(saved.contains("Manually requested diagnostic"))
            assertTrue(saved.contains("not a confirmed crash"))
            reports.clear()
            assertTrue(reports.load().isEmpty())
        } finally { root.deleteRecursively() }
    }

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
