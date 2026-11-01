package com.audiobookbay.app.data.remote

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.audiobookbay.app.di.IoDispatcher
import com.audiobookbay.app.domain.repository.AppUpdate
import com.audiobookbay.app.domain.repository.UpdateError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject

internal class UpdateFailure(val kind: UpdateError) : Exception(kind.name)

internal data class ApkIdentity(
    val packageName: String, val version: String, val code: Long, val minSdk: Int,
    val abis: Set<String>, val signers: Set<String>,
)

internal fun validateUpdateIdentity(apk: ApkIdentity, expected: AppUpdate, installed: ApkIdentity, sdk: Int, abis: Set<String>) {
    if (apk.packageName != installed.packageName || apk.code <= installed.code ||
        ReleaseVersion.parse(apk.version) != ReleaseVersion.parse(expected.version)) throw UpdateFailure(UpdateError.PACKAGE)
    if (apk.minSdk > sdk || apk.abis.isEmpty() || apk.abis.intersect(abis).isEmpty()) throw UpdateFailure(UpdateError.INCOMPATIBLE)
    if (apk.signers.isEmpty() || apk.signers != installed.signers) throw UpdateFailure(UpdateError.SIGNATURE)
}

class UpdateApkVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    @Suppress("DEPRECATION")
    suspend fun verify(file: File, expected: AppUpdate, checksum: String) = withContext(io) {
        if (!file.isFile || file.length() != expected.size) throw UpdateFailure(UpdateError.CHECKSUM)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        if (digest.digest().joinToString("") { "%02x".format(it) } != checksum) throw UpdateFailure(UpdateError.CHECKSUM)
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val manager = context.packageManager
        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw UpdateFailure(UpdateError.PACKAGE)
        val installed = manager.getPackageInfo(context.packageName, flags)
        val nativeAbis = try {
            ZipFile(file).use { zip -> zip.entries().asSequence().mapNotNull {
                Regex("^lib/([^/]+)/libtorrent4j\\.so$").matchEntire(it.name)?.groupValues?.get(1)
            }.toSet() }
        } catch (_: Exception) { throw UpdateFailure(UpdateError.PACKAGE) }
        fun identity(info: PackageInfo, abis: Set<String>): ApkIdentity {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return ApkIdentity(info.packageName, info.versionName.orEmpty(),
                if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                info.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE, abis,
                signatures.orEmpty().map { signature ->
                    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
                }.toSet())
        }
        validateUpdateIdentity(identity(archive, nativeAbis), expected, identity(installed, emptySet()),
            Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.toSet())
    }
}
