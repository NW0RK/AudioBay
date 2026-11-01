package com.audiobookbay.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.domain.model.DownloadMetadata
import com.audiobookbay.app.torrent.TorrentDownloadManagerImpl
import com.audiobookbay.app.torrent.service.TorrentDownloadService
import com.audiobookbay.app.ui.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Exercise the real foreground service and queue UI, which the local transfer test replaces. */
class DownloadStartupTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun startingDownloadKeepsQueueAndForegroundServiceAlive() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = EntryPointAccessors.fromApplication(app, DownloadStartupEntryPoint::class.java).manager()
        // A synthetic hash has no content or peers; this test never publishes shared files.
        val hash = "0123456789abcdef0123456789abcdef01234567"
        try {
            compose.onNodeWithContentDescription("Downloads").performClick()
            manager.startDownload("startup-regression", "magnet:?xt=urn:btih:$hash",
                DownloadMetadata("Startup test", "Download startup test", "content://test/unused")).getOrThrow()
            delay(4000) // Includes the service's initial observation and the first native poll.
            compose.onNodeWithText("Download startup test").assertExists()
            // Android can hide notifications when permission is denied; the
            // foreground service must still run in that case.
            val activities = app.getSystemService(android.app.ActivityManager::class.java)
            @Suppress("DEPRECATION")
            val services = activities.getRunningServices(Int.MAX_VALUE)
            org.junit.Assert.assertTrue(services.any {
                it.service.className == TorrentDownloadService::class.java.name && it.foreground
            })
        } finally {
            manager.cancelDownload(hash, deleteFiles = true)
            manager.shutdown()
            TorrentDownloadService.stop(app)
        }
    }
}

