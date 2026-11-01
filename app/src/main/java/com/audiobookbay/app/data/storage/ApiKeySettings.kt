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
                val bytes = file.readFully()
                require(bytes.size > 28)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
            } catch (_: Exception) { "" }
            _configured.value = value.isNotBlank()
        }
    }

    suspend fun key(): String = withContext(io) {
        loaded.join()
        mutex.withLock { value }
    }

    suspend fun save(input: String) = withContext(io) {
        val key = normalizeApiKey(input)
        loaded.join()
        mutex.withLock {
            if (key.isEmpty()) {
                file.delete()
                check(!file.baseFile.exists())
            } else {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secret())
                val bytes = cipher.iv + cipher.doFinal(key.toByteArray(Charsets.UTF_8))
                val output = file.startWrite()
                try {
                    output.write(bytes)
                    file.finishWrite(output)
                } catch (e: Exception) {
                    file.failWrite(output)
                    throw e
                }
            }
            value = key
            _configured.value = key.isNotEmpty()
        }
    }

    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("audiobay-gemini", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("audiobay-gemini",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}

internal fun normalizeApiKey(input: String): String = input.trim().also {
    require(it.length <= 512 && it.all { char -> char.isLetterOrDigit() && char.code < 128 || char == '-' || char == '_' }) {
        "Invalid API key"
    }
}
