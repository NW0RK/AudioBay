package com.audiobookbay.app

import com.audiobookbay.app.data.storage.VerifiedReplacement
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class VerifiedReplacementTest {
    @Test fun successfulReplacementIsReadBack() {
        var saved: ByteArray? = "old".toByteArray()
        VerifiedReplacement.replace("new".toByteArray(), { saved }, { saved = it })
        assertArrayEquals("new".toByteArray(), saved)
    }

    @Test fun silentFailedCommitRestoresPreviousCredential() {
        val previous = "previous".toByteArray()
        var saved: ByteArray? = previous
        var attempts = 0
        assertThrows(IOException::class.java) {
            VerifiedReplacement.replace("new".toByteArray(), { saved }, {
                saved = if (attempts++ == 0) "corrupted".toByteArray() else it
            })
        }
        assertArrayEquals(previous, saved)
    }

    @Test fun failedFirstSaveRemovesIncompleteCredential() {
        var saved: ByteArray? = null
        assertThrows(IOException::class.java) {
            VerifiedReplacement.replace("new".toByteArray(), { saved }, {
                saved = it
                if (it != null) throw IOException("Disk full")
            })
        }
        assertNull(saved)
    }
}
