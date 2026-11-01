package com.audiobookbay.app.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.audiobookbay.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

enum class ApiKeyFailure { FORMAT, ENCRYPTION, STORAGE }
class ApiKeySaveException(val failure: ApiKeyFailure) : Exception(failure.name)

/** Device-local encrypted credential; neither ciphertext nor key enters Android backups. */
@Singleton
class ApiKeySettings @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "gemini-key"))
    private val mutex = Mutex()
    private var value = ""
    private val _configured = MutableStateFlow(false)
    val configured = _configured.asStateFlow()
    private val loaded = CoroutineScope(SupervisorJob() + io).launch {
        mutex.withLock {
            value = try {
                decrypt(file.readFully())
            } catch (_: Exception) { "" }
            _configured.value = value.isNotBlank()
        }
    }

    suspend fun key(): String = withContext(io) {
        loaded.join()
        mutex.withLock { value }
    }

    suspend fun save(input: String) = withContext(io) {
        val key = try { normalizeApiKey(input) }
        catch (_: IllegalArgumentException) { throw ApiKeySaveException(ApiKeyFailure.FORMAT) }
        loaded.join()
        mutex.withLock {
            if (key.isEmpty()) {
                try {
                    file.delete()
                    check(!file.baseFile.exists())
                } catch (_: Exception) { throw ApiKeySaveException(ApiKeyFailure.STORAGE) }
            } else {
                val bytes = try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, secret())
                    (cipher.iv + cipher.doFinal(key.toByteArray(Charsets.UTF_8))).also {
                        check(decrypt(it) == key)
                    }
                } catch (_: Exception) { throw ApiKeySaveException(ApiKeyFailure.ENCRYPTION) }
                try { verifiedReplace(file, bytes) }
                catch (_: Exception) { throw ApiKeySaveException(ApiKeyFailure.STORAGE) }
            }
            value = key
            _configured.value = key.isNotEmpty()
        }
    }

    private fun decrypt(bytes: ByteArray): String {
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // Look up an existing key only after checking its alias; create first-use keys explicitly.
        if (store.containsAlias("audiobay-gemini")) {
            (store.getKey("audiobay-gemini", null) as? SecretKey)?.let { return it }
            throw java.security.KeyStoreException("Credential key is unavailable")
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("audiobay-gemini",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}

private fun verifiedReplace(file: AtomicFile, bytes: ByteArray) = VerifiedReplacement.replace(
    bytes,
    read = { if (file.baseFile.exists()) file.readFully() else null },
    write = { content ->
        if (content == null) file.delete() else {
            val output = file.startWrite()
            try {
                output.write(content)
                file.finishWrite(output)
            } catch (e: Exception) {
                file.failWrite(output)
                throw e
            }
        }
    },
)

internal fun normalizeApiKey(input: String): String {
    fun String.cleanEdges() = trim { it.isWhitespace() || it == '\uFEFF' || it == '\u200B' }
    val trimmed = input.cleanEdges()
    val key = if (trimmed.length >= 2 && trimmed.first() == trimmed.last() && trimmed.first() in "\"'") {
        trimmed.substring(1, trimmed.lastIndex).cleanEdges()
    } else trimmed
    return key.also {
        require(it.length <= 512 && it.all { char -> char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '-' || char == '_' }) {
            "Invalid API key"
        }
    }
}
