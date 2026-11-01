package com.audiobookbay.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.io.File

@kotlinx.serialization.Serializable
data class AppUpdate(
    val version: String,
    val notes: String,
    val assetName: String,
    val downloadUrl: String,
    val checksumUrl: String,
    val size: Long,
)

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, ERROR }
enum class UpdateError { NETWORK, RATE_LIMIT, RELEASE, STORAGE, CHECKSUM, PACKAGE, SIGNATURE, INCOMPATIBLE }
data class AppUpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val update: AppUpdate? = null,
    val downloaded: Long = 0,
    val error: UpdateError? = null,
    val announce: Boolean = false,
)

interface AppUpdateRepository {
    val state: StateFlow<AppUpdateState>
    suspend fun check(automatic: Boolean = false)
    fun dismissAnnouncement()
    fun download()
    fun cancelDownload()
    suspend fun installationFile(): File?
}
