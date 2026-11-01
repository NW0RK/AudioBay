package com.audiobookbay.app.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.audiobookbay.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadFolderSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>,
    private val storage: SafPublicationStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val folder: Flow<String?> = preferences.data.map { it[KEY] }
    suspend fun select(uri: String) = withContext(io) {
        val tree = Uri.parse(uri)
        context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val root = storage.root(uri)
        // A small probe checks create/rename/write support before accepting this destination.
        val probe = storage.directory(root, ".audiobay-check-${java.util.UUID.randomUUID()}")
        var cleanup = probe
        try {
            cleanup = storage.rename(probe, ".audiobay-checked-${java.util.UUID.randomUUID()}")
            val file = storage.file(cleanup, "check")
            storage.write(file).use { it.write(1) }
        } finally { storage.delete(cleanup) }
        preferences.edit { it[KEY] = uri }
    }
    suspend fun validate(uri: String) = withContext(io) { storage.root(uri); Unit }

    fun display(uri: String?): String = uri?.let {
        runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(it)).replace("primary:", "Internal storage/") }.getOrDefault(it)
    } ?: "Android/AudioBooks"
    companion object {
        private val KEY = stringPreferencesKey("shared_audiobook_folder")
        val initialUri: Uri get() = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Android")
    }
}
