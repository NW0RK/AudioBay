package com.audiobookbay.app

import android.content.Context
import com.audiobookbay.app.data.local.dao.DownloadDao
import com.audiobookbay.app.data.storage.BookPublisher
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import com.audiobookbay.app.domain.model.DownloadMetadata
import com.audiobookbay.app.torrent.NetworkStatus
import com.audiobookbay.app.torrent.TorrentDownloadManagerImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TorrentStartupTest {
    @Test fun nativeLinkageErrorsReturnFailureOnFirstStartAndRetry() = runBlocking {
        // Inject errors inside the operation boundary, before Android/native API
        // calls. These are the two errors libtorrent's failed loader can produce.
        val context = mockk<Context>()
        every { context.filesDir } throwsMany listOf(
            LinkageError("Native library unavailable"), NoClassDefFoundError("Failed native initialization"))
        val settings = mockk<DownloadNetworkSettings>()
        every { settings.wifiOnly } returns flowOf(false)
        val manager = TorrentDownloadManagerImpl(context, mockk<DownloadDao>(), Json,
            mockk<BookPublisher>(), settings, mockk<NetworkStatus>(), Dispatchers.IO)
        repeat(2) {
            val result = manager.startDownload("test", "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567",
                DownloadMetadata("Author", "Title", "content://test/unused"))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertTrue(result.exceptionOrNull()?.cause is LinkageError)
        }
    }
}
