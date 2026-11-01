package com.audiobookbay.app.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import com.audiobookbay.app.BuildConfig
import com.audiobookbay.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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

    private fun header() = "AudioBay ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nABI: ${Build.SUPPORTED_ABIS.firstOrNull()}\n"

    @Synchronized private fun save(id: String, text: String) {
        val target = File(directory, "$id.txt")
        if (!target.exists()) target.writeText(text.take(20000))
        directory.listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(5).forEach { it.delete() }
    }

    suspend fun load(): List<String> = withContext(io) {
        if (Build.VERSION.SDK_INT >= 30) {
            val watermark = preferences.getLong("lastExit", 0)
            val exits = context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(context.packageName, 0, 10)
            exits.filter { it.timestamp > watermark && it.reason in setOf(ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE, ApplicationExitInfo.REASON_ANR) }
                .forEach { exit ->
                    val frames = if (exit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                        runCatching { exit.traceInputStream?.bufferedReader()?.use { reader ->
                            val chars = CharArray(65536)
                            val count = reader.read(chars)
                            CrashText.nativeFrames(if (count > 0) String(chars, 0, count) else "")
                        }.orEmpty() }.getOrDefault("")
                    } else ""
                    save("exit-${exit.timestamp}", header() + "\nPrevious process exit: ${exit.reason}\nTime (UTC epoch ms): ${exit.timestamp}\nReported app version: current installed version; exit may predate an upgrade.\n" + frames)
                }
            exits.maxOfOrNull { it.timestamp }?.let { preferences.edit().putLong("lastExit", it).apply() }
        }
        directory.listFiles().orEmpty().sortedByDescending { it.lastModified() }.map { it.readText() }
    }

    suspend fun clear() = withContext(io) {
        directory.listFiles().orEmpty().forEach { check(it.delete()) }
    }
}
