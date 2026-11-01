package com.audiobookbay.app

import com.audiobookbay.app.diagnostics.CrashText
import org.junit.Assert.*
import org.junit.Test

class CrashTextTest {
    @Test fun excludesExceptionMessagesAndKeepsCauseFrames() {
        val text = CrashText.throwable(IllegalStateException("secret-key and book title", IllegalArgumentException("private URL")))
        assertFalse(text.contains("secret-key"))
        assertFalse(text.contains("private URL"))
        assertTrue(text.contains("IllegalStateException"))
        assertTrue(text.contains("IllegalArgumentException"))
        assertTrue(text.contains("CrashTextTest"))
    }
    @Test fun nativeTraceIncludesOnlyFramesWithSanitizedPaths() {
        val text = CrashText.nativeFrames("secret header\n#00 pc 000001 /data/private/book/libtorrent.so (function+10)\nprivate URL")
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("private"))
        assertTrue(text.contains("libtorrent.so"))
        assertTrue(text.contains("000001"))
    }
    @Test fun logSummaryOnlyIncludesAllowlistedDetails() {
        val raw = "secret credential https://private/book\nFatal signal 2 (SIGINT), secret name\nat com.audiobookbay.app.Test.run(Test.kt:12)\navc: denied secret path"
        val text = CrashText.logSummary(raw)
        assertTrue(text.contains("Fatal signal 2 (SIGINT)"))
        assertTrue(text.contains("Test.kt:12"))
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("https"))
        assertFalse(text.contains("private"))
        assertEquals("", CrashText.logSummary("token=private book title"))
    }
    @Test fun limitsLargeReports() {
        val error = RuntimeException("x".repeat(100000))
        assertTrue(CrashText.throwable(error).length <= 16000)
        assertEquals("", CrashText.nativeFrames("private data"))
    }
}
