package com.audiobookbay.app.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.audiobookbay.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchFormattingSettings @Inject constructor(
    private val preferences: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val enabled: Flow<Boolean> = preferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ENABLED] ?: false }.distinctUntilChanged()
    suspend fun setEnabled(enabled: Boolean) = withContext(io) {
        preferences.edit { it[ENABLED] = enabled }
        Unit
    }
    companion object { internal val ENABLED = booleanPreferencesKey("ai_format_search_results") }
}
