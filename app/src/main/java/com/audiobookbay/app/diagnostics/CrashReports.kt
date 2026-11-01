package com.audiobookbay.app.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.AtomicFile
import com.audiobookbay.app.BuildConfig
import com.audiobookbay.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Deliberately excludes exception messages, book names, paths, URLs and credentials. */
object CrashText {
    fun throwable(error: Throwable): String = buildString {
        var current: Throwable? = error
        val seen = mutableSetOf<Throwable>()
        while (current != null && seen.add(current) && seen.size <= 8) {
            appendLine(current.javaClass.name)
            current.stackTrace.take(40).forEach { appendLine("    at $it") }
            current = current.cause
        }
    }.take(16000)

    /** Strict allowlist: never export arbitrary log messages containing book titles or keys. */
    fun logSummary(raw: String): String = raw.lineSequence().mapNotNull { line ->
        val signal = Regex("Fatal signal ([0-9]+) \\(([A-Z0-9]+)\\)").find(line)
        val frame = Regex("\\bat ((?:com\\.audiobookbay|org\\.libtorrent4j|java|kotlin|android)[A-Za-z0-9_.$]*)\\(([A-Za-z0-9_.:-]+)\\)").find(line)
        when {
            signal != null -> signal.value
            frame != null -> frame.value
            line.contains("avc: denied") -> "Android security denial present (details omitted)."
            else -> null
        }
    }.take(80).joinToString("\n").take(16000)

    fun nativeFrames(trace: String): String = trace.lineSequence()
        .filter { it.trim().matches(Regex("#[0-9]+\\s+pc\\s+[0-9a-fA-F]+\\s+.*")) }
        .take(60).joinToString("\n") { line ->
            // Keep addresses and library basenames, never private directory names.
            line.trim().replace(Regex("/[^\\s()]+/"), "<path>/")
        }.take(16000)
}

@Singleton
class CrashReports @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val directory get() = File(context.noBackupFilesDir, "crash-reports").apply { mkdirs() }
    private val preferences get() = context.getSharedPreferences("crash-history", Context.MODE_PRIVATE)

    // Written before native calls: native signals cannot invoke a Java exception handler.
    // An unfinished marker indicates interruption, not proof of a crash.
    enum class Stage { ENGINE_START, ENGINE_RUNNING, ADD_TORRENT, TORRENT_SUBMITTED, PROGRESS_READ, METADATA_READY, APPLY_PRIORITIES, PRIORITIES_APPLIED, STATUS_READ, STATUS_READY, ENGINE_STOP }
    private val marker get() = AtomicFile(File(context.noBackupFilesDir, "torrent-diagnostic-marker"))

    private val journal get() = AtomicFile(File(context.noBackupFilesDir, "torrent-diagnostic-events"))
    private fun writeAtomic(file: AtomicFile, text: String) {
        val stream = file.startWrite()
        try { stream.write(text.toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    private fun readAtomic(file: AtomicFile, limit: Int = 20000): String =
        runCatching { file.openRead().bufferedReader().use { it.readText().take(limit) } }.getOrDefault("")

    @Synchronized fun checkpoint(stage: Stage) {
        runCatching {
            val events = readAtomic(journal).lineSequence().filter { it.isNotBlank() }.toList().takeLast(79)
            writeAtomic(journal, (events + "${System.currentTimeMillis()} pid=${android.os.Process.myPid()} ${stage.name}").joinToString("\n"))
            val stream = marker.startWrite()
            try {
                stream.write((header() + "\nTime (UTC epoch ms): ${System.currentTimeMillis()}\nLast torrent stage: ${stage.name}\n").toByteArray())
                marker.finishWrite(stream)
            } catch (e: Exception) { marker.failWrite(stream); throw e }
        }
    }

    @Synchronized fun engineStopped() { runCatching { marker.delete() } }

    @Synchronized fun recoverInterruptedSession() {
        runCatching {
            if (marker.baseFile.exists()) {
                val text = marker.openRead().bufferedReader().use { it.readText().take(4000) }
                save("interruption-${System.currentTimeMillis()}", text + "\nTorrent session did not finish normally. This may be a native crash, Android termination, force stop or reboot; it is not proof of a crash.\n")
                marker.delete()
            }
        }
    }

    fun recordFailure(error: Throwable) {
        runCatching { save("failure-${System.currentTimeMillis()}", header() + "\nHandled torrent failure\n" + CrashText.throwable(error)) }
    }

    suspend fun createDiagnostic() = withContext(io) {
        val markerText = runCatching { marker.openRead().bufferedReader().use { it.readText().take(4000) } }.getOrDefault("No active torrent checkpoint.")
        save("manual-${System.currentTimeMillis()}", header() + "\nManually requested diagnostic; not a confirmed crash.\n$markerText")
    }

    private val logFile get() = AtomicFile(File(context.noBackupFilesDir, "diagnostic-log-summary"))
    private val logLock = Any()

    fun collectStartupLogs() {
        CoroutineScope(SupervisorJob() + io).launch { collectLogSummary() }
    }

    /** Android only permits ordinary apps to read their own UID logs. No READ_LOGS grant needed. */
    private fun collectLogSummary(): String = synchronized(logLock) {
        val temporary = File(context.cacheDir, "diagnostic-logcat.tmp")
        var process: Process? = null
        val result = try {
            process = ProcessBuilder("logcat", "-d", "-v", "epoch", "-t", "250", "--uid=${android.os.Process.myUid()}")
                .redirectErrorStream(true).redirectOutput(temporary).start()
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                "App log query timed out; full system logs require privileged Android access."
            } else if (process.exitValue() != 0) {
                "App log query unavailable on this Android build."
            } else {
                val raw = temporary.reader().use { reader ->
                    val chars = CharArray(131072)
                    var count = 0
                    while (count < chars.size) {
                        val n = reader.read(chars, count, chars.size - count)
                        if (n < 0) break
                        count += n
                    }
                    String(chars, 0, count)
                }
                val summary = CrashText.logSummary(raw)
                "Own-UID Android log summary (raw messages excluded):\n" +
                    summary.ifBlank { "No permitted signal or stack entries available. This does not rule out a crash." }
            }
        } catch (_: Exception) { "App log query unavailable; durable torrent events remain available." }
        finally { process?.destroy(); temporary.delete() }
        runCatching { writeAtomic(logFile, "Collected at UTC epoch ms: ${System.currentTimeMillis()}\n$result") }
        result
    }

    suspend fun exportText(): String = withContext(io) {
        val reports = load()
        val previousLogs = readAtomic(logFile)
        val currentLogs = collectLogSummary()
        buildString {
            appendLine(header())
            appendLine("Diagnostic export; not a confirmed crash. Nothing uploaded automatically.")
            appendLine("Full device-wide system logs are restricted by Android. Includes own-app permitted log summaries only.")
            appendLine("\nDURABLE TORRENT EVENTS (oldest first; timestamps are UTC epoch ms)")
            appendLine(readAtomic(journal).ifBlank { "No native torrent events recorded yet." })
            appendLine("\nSAVED REPORTS")
            appendLine(reports.joinToString("\n\n---\n\n").ifBlank { "No Android crash records available." })
            appendLine("\nPREVIOUS APP LOG SUMMARY\n$previousLogs")
            appendLine("\nCURRENT APP LOG SUMMARY\n$currentLogs")
        }
    }

    suspend fun writeExport(uri: android.net.Uri) = withContext(io) {
        val text = exportText()
        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "Cannot open diagnostic destination" }
            .bufferedWriter().use { it.write(text) }
    }

    fun installHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try { save("java-${System.currentTimeMillis()}", header() + "\nTime (UTC epoch ms): ${System.currentTimeMillis()}\nUncaught exception\n" + CrashText.throwable(error)) }
            catch (_: Throwable) { /* Never interfere with Android crash handling. */ }
            finally {
                if (previous != null) previous.uncaughtException(thread, error)
                else { android.os.Process.killProcess(android.os.Process.myPid()); kotlin.system.exitProcess(10) }
            }
        }
    }

    private fun header() = "Memory page size: ${runCatching { android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE) }.getOrDefault(-1)} bytes\nAudioBay ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nABI: ${Build.SUPPORTED_ABIS.firstOrNull()}\n"

    @Synchronized private fun save(id: String, text: String) {
        val target = File(directory, "$id.txt")
        if (!target.exists()) {
            target.writeText(text.take(20000))
            id.substringAfterLast('-').toLongOrNull()?.let { target.setLastModified(it) }
        }
        directory.listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(5).forEach { it.delete() }
    }

    suspend fun load(): List<String> = withContext(io) {
        try { if (Build.VERSION.SDK_INT >= 30) {
            val watermark = preferences.getLong("lastExitV2", 0)
            val exits = context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(context.packageName, 0, 10)
            exits.filter { it.timestamp > watermark }.sortedBy { it.timestamp }
                .forEach { exit ->
                    val frames = if (exit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                        runCatching { exit.traceInputStream?.bufferedReader()?.use { reader ->
                            val chars = CharArray(65536)
                            val count = reader.read(chars)
                            CrashText.nativeFrames(if (count > 0) String(chars, 0, count) else "")
                        }.orEmpty() }.getOrDefault("")
                    } else ""
                    save("exit-${exit.timestamp}", header() + "\nPrevious process exit reason code: ${exit.reason} (not necessarily a crash); status: ${exit.status}\nTime (UTC epoch ms): ${exit.timestamp}\nReported app version: current installed version; exit may predate an upgrade.\n" + frames)
                }
            exits.maxOfOrNull { it.timestamp }?.let { preferences.edit().putLong("lastExitV2", it).apply() }
        }
        } catch (_: Exception) {
            // Historical exits are optional; never hide already saved local reports.
        }
        directory.listFiles().orEmpty().sortedByDescending { it.lastModified() }.mapNotNull { runCatching { it.readText() }.getOrNull() }
    }

    suspend fun clear() = withContext(io) {
        directory.listFiles().orEmpty().forEach { check(it.delete()) }
        synchronized(this@CrashReports) { journal.delete() }
        synchronized(logLock) { logFile.delete() }
    }
}
