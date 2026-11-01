package com.audiobookbay.app.torrent

import android.content.Context
import android.net.Uri
import com.audiobookbay.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun deleteTemporary(file: File) = withContext(io) { file.delete(); Unit }

    suspend fun copyTorrent(uri: String): File = withContext(io) {
        val target = File.createTempFile("import-", ".torrent", context.cacheDir)
        try {
            context.contentResolver.openInputStream(Uri.parse(uri)).use { input ->
                checkNotNull(input) { "Unable to read torrent file" }
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var size = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= 10 * 1024 * 1024) { "Torrent file is too large" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            target
        } catch (e: Exception) { target.delete(); throw e }
    }
}
