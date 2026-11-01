package com.audiobookbay.app.data.repository

import android.content.Context
import com.audiobookbay.app.BuildConfig
import com.audiobookbay.app.data.remote.*
import com.audiobookbay.app.di.IoDispatcher
import com.audiobookbay.app.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Serializable
private data class CachedUpdate(val update: AppUpdate, val checksum: String)

@Singleton
class GitHubAppUpdateRepository @Inject constructor(
    @ApplicationContext context: Context,
    client: OkHttpClient,
    private val verifier: UpdateApkVerifier,
    private val device: UpdateDevice,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AppUpdateRepository {
    private val directory = File(context.cacheDir, "updates")
    private val apk = File(directory, "update.apk")
    private val partial = File(directory, "update.part.apk")
    private val record = File(directory, "ready.json")
    private val preferences = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val client = client.newBuilder().apply { interceptors().clear(); networkInterceptors().clear() }
        .followSslRedirects(false).callTimeout(5, TimeUnit.MINUTES).readTimeout(30, TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(AppUpdateState())
    override val state = _state.asStateFlow()
    private var cached: CachedUpdate? = null
    @Volatile private var downloadJob: Job? = null
    @Volatile private var downloadCall: Call? = null
    private val initialized = scope.launch {
        mutex.withLock {
            partial.delete()
            try {
                if (record.isFile && record.length() <= 65_536) {
                    val saved = json.decodeFromString<CachedUpdate>(record.readText())
                    require(UpdateReleaseParser.validAssetUrl(saved.update.downloadUrl))
                    verifier.verify(apk, saved.update, saved.checksum)
                    cached = saved
                    _state.value = AppUpdateState(UpdatePhase.READY, saved.update)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { record.delete(); apk.delete() }
        }
    }

    override suspend fun check(automatic: Boolean) = withContext(io) {
        initialized.join()
        if (downloadJob?.isActive == true) return@withContext
        mutex.withLock {
            if (_state.value.phase == UpdatePhase.READY) return@withLock
            val now = System.currentTimeMillis()
            if (automatic && !UpdateReleaseParser.shouldCheck(now, preferences.getLong("last_attempt", 0))) return@withLock
            preferences.edit().putLong("last_attempt", now).apply()
            val previous = _state.value
            if (!automatic) _state.value = previous.copy(phase = UpdatePhase.CHECKING, error = null, announce = false)
            try {
                val body = text("https://api.github.com/repos/${UpdateReleaseParser.REPOSITORY}/releases?per_page=30", 1_048_576)
                val update = UpdateReleaseParser.select(body, BuildConfig.VERSION_NAME, device.abis)
                currentCoroutineContext().ensureActive()
                _state.value = if (update == null) AppUpdateState(UpdatePhase.CURRENT)
                    else AppUpdateState(UpdatePhase.AVAILABLE, update, announce = automatic)
            } catch (e: CancellationException) {
                _state.value = previous
                throw e
            } catch (e: Exception) {
                _state.value = if (automatic) previous else previous.copy(phase = UpdatePhase.ERROR, error = category(e), announce = false)
            }
        }
    }

    override fun dismissAnnouncement() { _state.value = _state.value.copy(announce = false) }

    @Synchronized
    override fun download() {
        if (downloadJob?.isActive == true || state.value.phase !in setOf(UpdatePhase.AVAILABLE, UpdatePhase.ERROR)) return
        downloadJob = scope.launch {
            initialized.join()
            mutex.withLock {
                val update = _state.value.update ?: return@withLock
                _state.value = AppUpdateState(UpdatePhase.DOWNLOADING, update)
                try {
                    if (!directory.isDirectory && !directory.mkdirs()) throw UpdateFailure(UpdateError.STORAGE)
                    val checksum = try { UpdateReleaseParser.checksum(text(update.checksumUrl, 16_384, downloading = true), update.assetName) }
                        catch (e: IllegalArgumentException) { throw UpdateFailure(UpdateError.CHECKSUM) }
                    currentCoroutineContext().ensureActive()
                    require(UpdateReleaseParser.validAssetUrl(update.downloadUrl))
                    val call = client.newCall(Request.Builder().url(update.downloadUrl).build())
                    downloadCall = call
                    call.awaitResponse().use { response ->
                        requireSuccess(response)
                        val body = response.body ?: throw UpdateFailure(UpdateError.NETWORK)
                        if (body.contentLength() > UpdateReleaseParser.MAX_APK_BYTES) throw UpdateFailure(UpdateError.RELEASE)
                        body.byteStream().use { input ->
                            disk { partial.outputStream().buffered() }.use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var copied = 0L
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    copied += count
                                    if (copied > update.size) throw UpdateFailure(UpdateError.CHECKSUM)
                                    disk { output.write(buffer, 0, count) }
                                    _state.value = _state.value.copy(downloaded = copied)
                                }
                                disk { output.flush() }
                            }
                        }
                    }
                    verifier.verify(partial, update, checksum)
                    currentCoroutineContext().ensureActive()
                    if (apk.exists() && !apk.delete() || !partial.renameTo(apk)) throw UpdateFailure(UpdateError.STORAGE)
                    val ready = CachedUpdate(update, checksum)
                    disk { record.writeText(json.encodeToString(CachedUpdate.serializer(), ready)) }
                    cached = ready
                    _state.value = AppUpdateState(UpdatePhase.READY, update)
                } catch (e: CancellationException) {
                    _state.value = AppUpdateState(UpdatePhase.AVAILABLE, update)
                    throw e
                } catch (e: Exception) {
                    if (!currentCoroutineContext().isActive) {
                        _state.value = AppUpdateState(UpdatePhase.AVAILABLE, update)
                        throw CancellationException("Update download cancelled")
                    }
                    _state.value = AppUpdateState(UpdatePhase.ERROR, update, error = category(e))
                } finally { downloadCall = null; partial.delete() }
            }
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadCall?.cancel()
    }

    override suspend fun installationFile(): File? = withContext(io) {
        initialized.join()
        mutex.withLock {
            val ready = cached ?: return@withLock null
            try {
                verifier.verify(apk, ready.update, ready.checksum)
                apk
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                record.delete(); apk.delete(); cached = null
                _state.value = AppUpdateState(UpdatePhase.ERROR, ready.update, error = category(e))
                null
            }
        }
    }

    private suspend fun text(url: String, limit: Long, downloading: Boolean = false): String {
        val request = Request.Builder().url(url).header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AudioBay/${BuildConfig.VERSION_NAME}").build()
        val call = client.newCall(request)
        if (downloading) downloadCall = call
        return call.awaitResponse().use { response ->
            requireSuccess(response)
            val body = response.body ?: throw UpdateFailure(UpdateError.NETWORK)
            if (body.contentLength() > limit) throw UpdateFailure(UpdateError.RELEASE)
            val source = body.source()
            source.request(limit + 1)
            if (source.buffer.size > limit) throw UpdateFailure(UpdateError.RELEASE)
            source.readUtf8()
        }
    }

    private inline fun <T> disk(block: () -> T): T = try { block() }
        catch (_: IOException) { throw UpdateFailure(UpdateError.STORAGE) }

    private fun requireSuccess(response: Response) {
        if (response.code == 403 || response.code == 429) throw UpdateFailure(UpdateError.RATE_LIMIT)
        if (!response.isSuccessful) throw UpdateFailure(UpdateError.NETWORK)
    }

    private fun category(e: Exception): UpdateError = when (e) {
        is UpdateFailure -> e.kind
        is java.io.FileNotFoundException, is java.nio.file.FileSystemException -> UpdateError.STORAGE
        is IOException -> UpdateError.NETWORK
        else -> UpdateError.RELEASE
    }
}

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { continuation.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}
