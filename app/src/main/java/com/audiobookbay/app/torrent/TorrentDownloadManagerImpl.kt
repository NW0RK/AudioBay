package com.audiobookbay.app.torrent

import com.audiobookbay.app.data.storage.BookPublisher
import com.audiobookbay.app.data.storage.BookFolders
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import com.audiobookbay.app.data.storage.PublicationFile
import com.audiobookbay.app.diagnostics.CrashReports
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.AtomicFile
import com.audiobookbay.app.data.local.dao.DownloadDao
import com.audiobookbay.app.data.local.entity.DownloadEntity
import com.audiobookbay.app.di.IoDispatcher
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.service.TorrentDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.libtorrent4j.*
import org.libtorrent4j.TorrentInfo as NativeTorrentInfo
import org.libtorrent4j.alerts.*
import org.libtorrent4j.swig.error_code
import org.libtorrent4j.swig.libtorrent
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SavedTorrent(
    val item: DownloadItem,
    val magnet: String? = null,
    val torrentPath: String? = null,
    val selected: List<Int>? = null,
    val files: List<TorrentFile>? = null,
    val verifiedFiles: List<PublicationFile>? = null,
)

/** Owns native objects on IO threads; Room is the persistent download view. */
@Singleton
class TorrentDownloadManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val json: Json,
    private val publisher: BookPublisher,
    private val networkSettings: DownloadNetworkSettings,
    private val networkStatus: NetworkStatus,
    private val crashReports: CrashReports,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TorrentDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val lock = Mutex()
    @Volatile private var session: SessionManager? = null
    @Volatile private var wifiOnly = false
    private var loaded = false
    private val publications = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val runtimeItems = MutableStateFlow<Map<String, DownloadItem?>>(emptyMap())
    private var poller: Job? = null
    private val records = mutableMapOf<String, SavedTorrent>()
    private val prepared = mutableSetOf<String>()
    private val pendingSaves = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val stateDir get() = File(context.filesDir, "torrent-state").apply { mkdirs() }

    init {
        // A settings change must take effect immediately, even while the app is not on screen.
        scope.launch {
            networkSettings.wifiOnly
                .catch { Timber.w(it, "Cannot observe Wi-Fi-only setting") }
                .collect { enabled ->
                    lock.withLock {
                        wifiOnly = enabled
                        enforceNetworkPolicyLocked()
                    }
                }
        }
    }

    private val listener = object : AlertListener {
        override fun types(): IntArray? = null
        override fun alert(alert: Alert<*>) {
            // Alert-backed native memory is only valid during this callback.
            if (alert is SaveResumeDataAlert) {
                val hash = alert.handle().infoHash().toHex()
                val bytes = AddTorrentParams.writeResumeDataBuf(alert.params())
                scope.launch {
                    try { atomicWrite(File(stateDir, "$hash.resume"), bytes) }
                    catch (e: Exception) { Timber.e(e, "Cannot save torrent resume state") }
                    finally { pendingSaves.remove(hash)?.complete(Unit) }
                }
            } else if (alert is AddTorrentAlert && alert.error().isError) {
                val hash = alert.params().infoHashes.best.toHex()
                val message = alert.error().message
                scope.launch { lock.withLock { fail(hash, IllegalStateException(message)) } }
            } else if (alert is SaveResumeDataFailedAlert) {
                pendingSaves.remove(alert.handle().infoHash().toHex())?.complete(Unit)
            }
        }
    }

    override suspend fun initialize() = withContext(io) {
        lock.withLock { initializeLocked() }
    }

    private suspend fun loadRecordsLocked() {
        if (loaded) return
        stateDir.listFiles { file -> file.extension == "json" }.orEmpty().forEach { file ->
            try {
                val saved = json.decodeFromString<SavedTorrent>(AtomicFile(file).readFully().decodeToString())
                val item = saved.item.let { if (it.status in setOf(DownloadStatus.COMPLETED, DownloadStatus.PUBLICATION_FAILED)) it else it.copy(status = if (it.status == DownloadStatus.PUBLISHING) DownloadStatus.PUBLICATION_FAILED else DownloadStatus.PAUSED, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0) }
                records[item.infoHash] = saved.copy(item = item)
                persist(saved.copy(item = item))
            } catch (e: CancellationException) { throw e } catch (e: Exception) { Timber.w(e, "Cannot restore torrent state") }
        }
        loaded = true
    }

    private suspend fun initializeLocked() {
        loadRecordsLocked()
        wifiOnly = networkSettings.wifiOnly.first()
        if (session != null) return
        check(Build.VERSION.SDK_INT >= 28) { "Torrent downloads require Android 9 or newer" }
        crashReports.checkpoint(CrashReports.Stage.ENGINE_START)
        val native = SessionManager()
        native.addListener(listener)
        try {
            native.start(SessionParams(SettingsPack().apply {
                setEnableDht(true)
                connectionsLimit(80)
                activeDownloads(2)
                activeLimit(2)
            }))
            crashReports.checkpoint(CrashReports.Stage.ENGINE_RUNNING)
            session = native
            poller = scope.launch {
                var tick = 0
                while (isActive) {
                    delay(2000)
                    tick++
                    lock.withLock {
                        try { enforceNetworkPolicyLocked() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { Timber.e(e, "Cannot apply Wi-Fi-only policy") }
                        records.keys.toList().forEach { hash ->
                            try { updateProgress(hash, tick % 15 == 0) }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { fail(hash, e) }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            crashReports.checkpoint(CrashReports.Stage.ENGINE_STOP)
            native.stop()
            crashReports.engineStopped()
            session = null
            throw e
        }
    }

    override suspend fun shutdown() = withContext(io) { lock.withLock { shutdownLocked() } }

    internal suspend fun shutdownIfIdle() = withContext(io) {
        lock.withLock {
            if (records.values.none { it.item.status in ACTIVE_STATUSES }) shutdownLocked()
        }
    }

    private suspend fun shutdownLocked() {
        publications.values.forEach { it.cancel() }
        publications.clear()
        records.values.toList().filter { it.item.status == DownloadStatus.PUBLISHING }.forEach {
            runCatching { persist(it.copy(item = it.item.copy(status = DownloadStatus.PUBLICATION_FAILED, errorMessage = "Publication interrupted"))) }
                .onFailure { error -> Timber.e(error, "Cannot persist interrupted publication") }
        }
        val native = session ?: return
        try {
            native.pause()
            records.values.toList().forEach { saved ->
                if (saved.item.status in ACTIVE_STATUSES) {
                    persist(saved.copy(item = saved.item.copy(status = DownloadStatus.PAUSED, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0)))
                }
            }
            val saves = records.keys.mapNotNull { hash -> native.find(Sha1Hash.parseHex(hash))?.takeIf { it.isValid }?.let { saveResume(hash, it) } }
            withTimeoutOrNull(3000) { saves.awaitAll() }
        } finally {
            poller?.cancel()
            poller = null
            native.removeListener(listener)
            crashReports.checkpoint(CrashReports.Stage.ENGINE_STOP)
            native.stop()
            crashReports.engineStopped()
            pendingSaves.values.forEach { it.complete(Unit) }
            pendingSaves.clear()
            session = null
            prepared.clear()
        }
    }

    override suspend fun startDownload(audiobookId: String, magnetUri: String, metadata: DownloadMetadata, selectedFiles: List<Int>?): Result<DownloadItem> = operation {
        initializeLocked()
        val params = AddTorrentParams.parseMagnetUri(magnetUri)
        require(params.infoHashes.hasV1()) { "Only v1 or hybrid magnets are supported" }
        val hash = params.infoHashes.v1.toHex()
        BookFolders.name(metadata.author); BookFolders.name(metadata.title)
        val dir = stagingDirectory(hash)
        val item = DownloadItem(hash, audiobookId, metadata.title.trim(), hash, dir.path, DownloadStatus.QUEUED,
            author = metadata.author.trim(), destinationTreeUri = metadata.destinationTreeUri)
        enqueue(SavedTorrent(item, magnet = magnetUri, selected = selectedFiles))
    }

    override suspend fun startDownloadFromFile(audiobookId: String, torrentFile: File, metadata: DownloadMetadata, selectedFiles: List<Int>?): Result<DownloadItem> = operation {
        initializeLocked()
        require(torrentFile.length() in 1..(10 * 1024 * 1024)) { "Torrent file is empty or too large" }
        val info = NativeTorrentInfo(torrentFile)
        require(info.isValid && info.infoHashes().hasV1()) { "Invalid or unsupported torrent" }
        BookFolders.name(metadata.author); BookFolders.name(metadata.title)
        val hash = info.infoHashes().v1.toHex()
        val dir = stagingDirectory(hash)
        validate(info, dir, selectedFiles)
        val stored = File(stateDir, "$hash.torrent")
        atomicWrite(stored, torrentFile.readBytes())
        val item = DownloadItem(hash, audiobookId, metadata.title.trim(), hash, dir.path, DownloadStatus.QUEUED,
            author = metadata.author.trim(), destinationTreeUri = metadata.destinationTreeUri)
        enqueue(SavedTorrent(item, torrentPath = stored.path, selected = selectedFiles))
    }

    private suspend fun enqueue(saved: SavedTorrent): DownloadItem {
        records[saved.item.infoHash]?.let { return it.item }
        try {
            persist(saved)
            TorrentDownloadService.start(context)
            addNative(saved)
            enforceNetworkPolicyLocked()
        } catch (e: Exception) { fail(saved.item.infoHash, e); throw e }
        return saved.item
    }

    private fun addNative(saved: SavedTorrent) {
        val native = checkNotNull(session)
        val hash = saved.item.infoHash
        val resume = File(stateDir, "$hash.resume")
        val params = if (resume.isFile) {
            val ec = error_code()
            val raw = libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(AtomicFile(resume).readFully()), ec)
            check(ec.value() == 0) { "Invalid resume state: ${ec.message()}" }
            AddTorrentParams(raw)
        } else if (saved.magnet != null) AddTorrentParams.parseMagnetUri(saved.magnet)
        else AddTorrentParams().apply { torrentInfo = NativeTorrentInfo(File(checkNotNull(saved.torrentPath))) }
        params.savePath = saved.item.destinationPath
        // Upload mode permits metadata exchange but prevents piece requests until
        // metadata paths and selected files have been validated. Zero priorities
        // during metadata exchange can make peers mistake this client for a seed.
        params.flags = params.flags.and_(TorrentFlags.PAUSED.or_(TorrentFlags.AUTO_MANAGED)
            .or_(TorrentFlags.DEFAULT_DONT_DOWNLOAD).inv()).or_(TorrentFlags.UPLOAD_MODE)
        crashReports.checkpoint(CrashReports.Stage.ADD_TORRENT)
        SessionHandle(native.swig()).asyncAddTorrent(params)
        crashReports.checkpoint(CrashReports.Stage.TORRENT_SUBMITTED)
    }

    override suspend fun updateMetadata(infoHash: String, metadata: DownloadMetadata): Result<Unit> = operation {
        loadRecordsLocked()
        val saved = records[infoHash] ?: error("Download not found")
        check(saved.item.status != DownloadStatus.PUBLISHING && saved.item.publishedFolderUri == null) { "Cannot rename a publishing or published book" }
        BookFolders.name(metadata.author); BookFolders.name(metadata.title)
        persist(saved.copy(item = saved.item.copy(author = metadata.author.trim(), title = metadata.title.trim(), destinationTreeUri = metadata.destinationTreeUri)))
    }

    override suspend fun pauseDownload(infoHash: String): Result<Unit> = operation {
        val saved = records[infoHash] ?: error("Download not found")
        val handle = session?.find(Sha1Hash.parseHex(infoHash))
        handle?.unsetFlags(TorrentFlags.AUTO_MANAGED)
        handle?.pause()
        handle?.let { saveResume(infoHash, it) }
        persist(saved.copy(item = saved.item.copy(status = DownloadStatus.PAUSED, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0)))
    }

    override suspend fun resumeDownload(infoHash: String): Result<Unit> = operation {
        loadRecordsLocked()
        var saved = records[infoHash] ?: error("Download not found")
        require(!saved.item.author.isNullOrBlank() && !saved.item.destinationTreeUri.isNullOrBlank()) { "Enter author, title and shared folder before resuming" }
        check(saved.item.publishedFolderUri == null) { "Book is already published" }
        TorrentDownloadService.start(context)
        if (saved.verifiedFiles != null) {
            persist(saved.copy(item = saved.item.copy(status = DownloadStatus.PUBLISHING, errorMessage = null)))
            startPublication(infoHash)
            return@operation
        }
        initializeLocked()
        // External emulated storage can crash the Android 17 FUSE daemon during native I/O.
        // Preserve legacy partial files; start a clean, verified transfer on internal storage.
        val safeDirectory = stagingDirectory(infoHash)
        if (File(saved.item.destinationPath).canonicalFile != safeDirectory.canonicalFile) {
            val resume = File(stateDir, "$infoHash.resume")
            if (resume.exists()) check(resume.renameTo(File(stateDir, "$infoHash.legacy-resume-${System.currentTimeMillis()}"))) { "Cannot preserve legacy resume state" }
            saved = saved.copy(item = saved.item.copy(destinationPath = safeDirectory.path,
                progress = 0f, bytesDownloaded = 0, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0), files = null)
            persist(saved)
        }
        val handle = session?.find(Sha1Hash.parseHex(infoHash))
        if (handle?.isValid == true) {
            handle.unsetFlags(TorrentFlags.AUTO_MANAGED)
            handle.resume()
        } else addNative(saved)
        persist(saved.copy(item = saved.item.copy(status = DownloadStatus.QUEUED, errorMessage = null)))
        enforceNetworkPolicyLocked()
    }

    override suspend fun cancelDownload(infoHash: String, deleteFiles: Boolean): Result<Unit> = operation {
        loadRecordsLocked()
        publications.remove(infoHash)?.cancel()
        val saved = records[infoHash] ?: error("Download not found")
        if (deleteFiles) checkedDestination(File(saved.item.destinationPath))
        session?.find(Sha1Hash.parseHex(infoHash))?.let { handle ->
            if (deleteFiles) session?.remove(handle, SessionHandle.DELETE_FILES) else session?.remove(handle)
        }
        records.remove(infoHash)
        runtimeItems.update { it + (infoHash to null) }
        prepared.remove(infoHash)
        dao.deleteDownloadByInfoHash(infoHash)
        listOf("json", "resume", "torrent").forEach { extension -> AtomicFile(File(stateDir, "$infoHash.$extension")).delete() }
    }

    override suspend fun fetchTorrentMetadata(magnetUri: String, timeoutMs: Long): Result<com.audiobookbay.app.domain.model.TorrentInfo> = operation {
        initializeLocked()
        val bytes = session?.fetchMagnet(magnetUri, (timeoutMs / 1000).toInt().coerceIn(1, 120), context.cacheDir)
            ?: error("Torrent metadata timed out")
        val info = NativeTorrentInfo(bytes)
        com.audiobookbay.app.domain.model.TorrentInfo(info.infoHash().toHex(), magnetUri, totalSizeBytes = info.totalSize(), files = (0 until info.numFiles()).map {
            val path = info.files().filePath(it)
            TorrentFile(it, path, info.files().fileSize(it), TorrentFiles.isAudio(path))
        })
    }

    override fun observeDownloads(): Flow<List<DownloadItem>> = combine(dao.observeAllDownloads(), runtimeItems) { rows, runtime ->
        val items = rows.associate { it.infoHash to it.toDomain() }.toMutableMap()
        runtime.forEach { (hash, item) -> if (item == null) items.remove(hash) else items[hash] = item }
        items.values.sortedByDescending { it.addedTimestamp }.map(::visibleState)
    }
    override fun observeDownload(infoHash: String): Flow<DownloadItem?> = combine(dao.observeDownloadByInfoHash(infoHash), runtimeItems) { row, runtime ->
        (if (runtime.containsKey(infoHash)) runtime[infoHash] else row?.toDomain())?.let(::visibleState)
    }

    private fun visibleState(item: DownloadItem): DownloadItem = when {
        item.status == DownloadStatus.PUBLISHING && !runtimeItems.value.containsKey(item.infoHash) -> item.copy(status = DownloadStatus.PUBLICATION_FAILED)
        session == null && item.status in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING, DownloadStatus.WAITING_FOR_WIFI) ->
            item.copy(status = DownloadStatus.PAUSED, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0)
        else -> item
    }

    override suspend fun listFiles(infoHash: String): Result<List<TorrentFile>> = operation {
        loadRecordsLocked()
        val saved = records[infoHash] ?: error("Download not found")
        saved.files?.let { return@operation it }
        val info = metadataFor(saved) ?: error("Torrent metadata is not ready yet")
        val selected = validate(info, File(saved.item.destinationPath), saved.selected)
        describe(info, selected).also { files -> persist(saved.copy(selected = selected.toList(), files = files)) }
    }

    override suspend fun setFilePriorities(infoHash: String, filePriorities: Map<Int, Boolean>): Result<Unit> = operation {
        loadRecordsLocked()
        val saved = records[infoHash] ?: error("Download not found")
        val known = saved.files ?: metadataFor(saved)?.let { info ->
            val selected = validate(info, File(saved.item.destinationPath), saved.selected)
            describe(info, selected).also { files -> persist(saved.copy(selected = selected.toList(), files = files)) }
        } ?: error("Torrent metadata is not ready yet")
        val updated = FileSelectionPolicy.apply(known, filePriorities)
        val selected = updated.filter { it.isSelectedForDownload }.map { it.index }.toSet()
        val current = records[infoHash] ?: saved
        persist(current.copy(selected = selected.sorted(), files = updated))
        val handle = session?.find(Sha1Hash.parseHex(infoHash))?.takeIf { it.isValid }
        val info = handle?.torrentFile()
        if (handle != null && info != null) {
            val valid = validate(info, File(current.item.destinationPath), selected.toList())
            handle.prioritizeFiles(Array(info.numFiles()) { if (it in valid) Priority.DEFAULT else Priority.IGNORE })
            saveResume(infoHash, handle)
        }
    }

    /** Reads metadata from the live session when possible, otherwise from the stored .torrent. */
    private fun metadataFor(saved: SavedTorrent): NativeTorrentInfo? =
        session?.find(Sha1Hash.parseHex(saved.item.infoHash))?.takeIf { it.isValid }?.torrentFile()
            ?: saved.torrentPath?.let { path -> runCatching { NativeTorrentInfo(File(path)) }.getOrNull() }

    private fun describe(info: NativeTorrentInfo, selected: Set<Int>): List<TorrentFile> =
        (0 until info.numFiles()).map { index ->
            val path = info.files().filePath(index)
            TorrentFile(index, path, info.files().fileSize(index), TorrentFiles.isAudio(path), index in selected)
        }

    override suspend fun updateNetworkSettings(wifiOnly: Boolean, maxDownloadSpeedBytes: Long, maxUploadSpeedBytes: Long): Unit = withContext(io) {
        require(maxDownloadSpeedBytes >= 0 && maxUploadSpeedBytes >= 0)
        lock.withLock {
            // Persist first so enforcement on a later process start honours the new choice.
            this@TorrentDownloadManagerImpl.wifiOnly = wifiOnly
            networkSettings.setWifiOnly(wifiOnly)
            session?.applySettings(SettingsPack().downloadRateLimit(maxDownloadSpeedBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .uploadRateLimit(maxUploadSpeedBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
            enforceNetworkPolicyLocked()
        }
    }

    /**
     * Pauses torrent transfers while Wi-Fi-only is enabled on a metered or unavailable connection,
     * and resumes the ones that were waiting once an unmetered connection is back. Shared-folder
     * publication and already completed downloads are untouched because they use no network.
     */
    private suspend fun enforceNetworkPolicyLocked() {
        val native = session ?: return
        if (!wifiOnly || networkStatus.isUnmetered()) {
            records.values.toList()
                .filter { it.item.status == DownloadStatus.WAITING_FOR_WIFI }
                .forEach { saved ->
                    val hash = saved.item.infoHash
                    val handle = native.find(Sha1Hash.parseHex(hash))?.takeIf { it.isValid }
                    if (handle != null) {
                        handle.unsetFlags(TorrentFlags.AUTO_MANAGED)
                        handle.resume()
                    } else {
                        addNative(saved.copy(item = saved.item.copy(status = DownloadStatus.QUEUED)))
                    }
                    persist(saved.copy(item = saved.item.copy(status = DownloadStatus.QUEUED, errorMessage = null)))
                }
            return
        }
        records.values.toList()
            .filter { it.item.status in TRANSFER_STATUSES }
            .forEach { saved ->
                val hash = saved.item.infoHash
                val handle = native.find(Sha1Hash.parseHex(hash))?.takeIf { it.isValid }
                handle?.unsetFlags(TorrentFlags.AUTO_MANAGED)
                handle?.pause()
                handle?.let { saveResume(hash, it) }
                persist(saved.copy(item = saved.item.copy(
                    status = DownloadStatus.WAITING_FOR_WIFI,
                    downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0,
                )))
            }
    }

    private suspend fun updateProgress(hash: String, checkpoint: Boolean) {
        val saved = records[hash] ?: return
        val handle = session?.find(Sha1Hash.parseHex(hash))?.takeIf { it.isValid } ?: return
        if (saved.item.status !in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING)) return
        crashReports.checkpoint(CrashReports.Stage.PROGRESS_READ)
        val info = handle.torrentFile()
        crashReports.checkpoint(CrashReports.Stage.METADATA_READY)
        if (info != null && hash !in prepared) {
            val selected = validate(info, File(saved.item.destinationPath), saved.selected)
            crashReports.checkpoint(CrashReports.Stage.APPLY_PRIORITIES)
            handle.prioritizeFiles(Array(info.numFiles()) { if (it in selected) Priority.DEFAULT else Priority.IGNORE })
            handle.unsetFlags(TorrentFlags.UPLOAD_MODE)
            crashReports.checkpoint(CrashReports.Stage.PRIORITIES_APPLIED)
            prepared.add(hash)
            persist(saved.copy(selected = selected.toList(), files = describe(info, selected)))
            return // Native priority changes are applied asynchronously.
        }
        crashReports.checkpoint(CrashReports.Stage.STATUS_READ)
        val status = handle.status()
        crashReports.checkpoint(CrashReports.Stage.STATUS_READY)
        check(!status.errorCode().isError) { status.errorCode().message }
        val completed = info != null && hash in prepared && status.totalWanted() > 0 && status.isFinished
        val item = saved.item.copy(
            status = if (completed) DownloadStatus.PUBLISHING else if (info == null) DownloadStatus.QUEUED else DownloadStatus.DOWNLOADING,
            progress = status.progress(), bytesDownloaded = status.totalWantedDone(), totalBytes = status.totalWanted(),
            downloadSpeedBytesPerSec = status.downloadRate().toLong(), uploadSpeedBytesPerSec = status.uploadRate().toLong(),
            seeders = status.numSeeds(), peers = status.numPeers(),
            etaSeconds = if (status.downloadRate() > 0) (status.totalWanted() - status.totalWantedDone()) / status.downloadRate() else -1,
            completedTimestamp = null,
        )
        if (completed) {
            handle.pause()
            val metadata = checkNotNull(info)
            val selected = validate(metadata, File(item.destinationPath), saved.selected)
            val paths = selected.sorted().map { metadata.files().filePath(it) }
            val files = BookFolders.files(File(item.destinationPath), paths, metadata.name())
            persist(saved.copy(item = item, verifiedFiles = files))
            session?.remove(handle)
            startPublication(hash)
        } else {
            persist(saved.copy(item = item))
            if (checkpoint) saveResume(hash, handle)
        }
    }

    private suspend fun startPublication(hash: String) {
        if (publications[hash]?.isActive == true) return
        val saved = records[hash] ?: return
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val publicationJob = currentCoroutineContext()[Job]
            try {
                val uri = publisher.publish(hash, checkNotNull(saved.item.destinationTreeUri), checkNotNull(saved.item.author), saved.item.title,
                    checkNotNull(saved.verifiedFiles)) { progress ->
                    lock.withLock {
                        currentCoroutineContext().ensureActive()
                        records[hash]?.let { persist(it.copy(item = it.item.copy(publicationProgress = progress))) }
                    }
                }
                lock.withLock {
                    currentCoroutineContext().ensureActive()
                    records[hash]?.let {
                        persist(it.copy(item = it.item.copy(status = DownloadStatus.COMPLETED, publishedFolderUri = uri,
                            publicationProgress = 1f, progress = 1f, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0,
                            completedTimestamp = System.currentTimeMillis(), errorMessage = null)))
                        runCatching { cleanStaging(it) }.onFailure { error -> Timber.w(error, "Staging cleanup deferred") }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Timber.e(e, "Cannot publish shared audiobook")
                lock.withLock {
                    records[hash]?.let {
                        runCatching { persist(it.copy(item = it.item.copy(status = DownloadStatus.PUBLICATION_FAILED,
                            errorMessage = e.message, downloadSpeedBytesPerSec = 0, uploadSpeedBytesPerSec = 0))) }
                            .onFailure { failure -> Timber.e(failure, "Cannot persist publication error; retaining runtime error and staging") }
                    }
                }
            } finally { withContext(NonCancellable) { lock.withLock { if (publications[hash] == publicationJob) publications.remove(hash) } } }
        }
        publications[hash] = job
        job.start()
    }

    private fun stagingDirectory(hash: String): File {
        return checkedDestination(File(File(context.filesDir, "torrent-staging"), hash))
    }

    private fun cleanStaging(saved: SavedTorrent) {
        val directory = File(saved.item.destinationPath).canonicalFile
        val owned = stagingRoots().firstOrNull { directory.toPath().startsWith(it.toPath()) }
            ?: error("Invalid staging location")
        val root = owned
        if (directory.parentFile == owned && directory.name == saved.item.infoHash && directory.toPath().startsWith(root.toPath())) {
            check(directory.deleteRecursively() || !directory.exists()) { "Cannot remove staging files" }
        } else {
            saved.verifiedFiles.orEmpty().forEach { entry ->
                val file = File(entry.source).canonicalFile
                require(file.toPath().startsWith(root.toPath()))
                check(!file.exists() || file.delete()) { "Cannot remove staged audio" }
            }
        }
    }

    private fun validate(info: NativeTorrentInfo, root: File, selected: List<Int>?): Set<Int> {
        val paths = (0 until info.numFiles()).map { info.files().filePath(it) }
        paths.forEach { TorrentFiles.resolveInside(root, it) }
        return TorrentFiles.selected(paths, selected)
    }

    private fun stagingRoots(): List<File> = listOfNotNull(
        File(context.filesDir, "torrent-staging").canonicalFile,
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "torrents").canonicalFile },
    )

    private fun checkedDestination(directory: File): File {
        val target = directory.canonicalFile
        require(stagingRoots().any { target.toPath().startsWith(it.toPath()) }) { "Downloads must use app-specific storage" }
        check(target.isDirectory || target.mkdirs()) { "Cannot create download folder" }
        check(target.usableSpace >= 1024 * 1024) { "Insufficient storage space" }
        return target
    }

    private fun saveResume(hash: String, handle: TorrentHandle): Deferred<Unit> {
        pendingSaves[hash]?.let { return it }
        val done = CompletableDeferred<Unit>()
        pendingSaves[hash] = done
        handle.saveResumeData(TorrentHandle.SAVE_INFO_DICT.or_(TorrentHandle.FLUSH_DISK_CACHE))
        return done
    }

    private suspend fun persist(saved: SavedTorrent) {
        val item = saved.item
        records[item.infoHash] = saved
        runtimeItems.update { it + (item.infoHash to item) }
        atomicWrite(File(stateDir, "${item.infoHash}.json"), json.encodeToString(saved).encodeToByteArray())
        dao.upsertDownload(DownloadEntity(item.id, item.audiobookId, item.title, item.infoHash, item.destinationPath,
            item.status.name, item.progress, item.bytesDownloaded, item.totalBytes, item.downloadSpeedBytesPerSec,
            item.uploadSpeedBytesPerSec, item.seeders, item.peers, item.etaSeconds, item.errorMessage, item.addedTimestamp, item.completedTimestamp,
            item.author, item.destinationTreeUri, item.publishedFolderUri, item.publicationProgress))
        records[item.infoHash] = saved
    }

    private suspend fun fail(hash: String, error: Exception) {
        session?.find(Sha1Hash.parseHex(hash))?.pause()
        records[hash]?.let { saved ->
            runCatching { persist(saved.copy(item = saved.item.copy(status = DownloadStatus.ERROR, errorMessage = error.message, downloadSpeedBytesPerSec = 0))) }
                .onFailure { Timber.e(it, "Cannot persist torrent error; retaining runtime error") }
        }
        crashReports.recordFailure(error)
        Timber.e(error, "Torrent operation failed")
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(bytes); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }

    private companion object {
        /** Statuses that actively use the network; these are gated by the Wi-Fi-only policy. */
        val TRANSFER_STATUSES = setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING)

        /** Statuses that keep the download service alive, including transfers waiting for Wi-Fi. */
        val ACTIVE_STATUSES = TRANSFER_STATUSES + setOf(DownloadStatus.PUBLISHING, DownloadStatus.WAITING_FOR_WIFI)
    }

    private suspend fun <T> operation(block: suspend () -> T): Result<T> = withContext(io) {
        try { Result.success(lock.withLock { block() }) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { crashReports.recordFailure(e); Timber.e(e, "Torrent operation failed"); Result.failure(e) }
        // libtorrent4j wraps loadLibrary failures in LinkageError. A later attempt
        // can instead throw NoClassDefFoundError after failed class initialization.
        catch (e: LinkageError) {
            crashReports.recordFailure(e)
            Timber.e(e, "Cannot load torrent engine")
            Result.failure(IllegalStateException("Torrent engine is unavailable on this device", e))
        }
    }
}
