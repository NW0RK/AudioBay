package com.audiobookbay.app.data.storage

import java.io.IOException

/** Verify persistence before publishing state; restore the previous bytes on failed replacement. */
internal object VerifiedReplacement {
    fun replace(bytes: ByteArray, read: () -> ByteArray?, write: (ByteArray?) -> Unit) {
        val previous = read()
        try {
            write(bytes)
            if (read()?.contentEquals(bytes) != true) throw IOException("Credential persistence failed")
        } catch (e: Exception) {
            try { write(previous) } catch (_: Exception) { /* Report failure without exposing contents. */ }
            throw e
        }
    }
}
