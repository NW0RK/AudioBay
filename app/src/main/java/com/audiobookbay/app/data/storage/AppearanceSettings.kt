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
class AppearanceSettings @Inject constructor(
    private val preferences: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val fontPercent: Flow<Int> = preferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { (it[FONT_PERCENT] ?: 100).coerceIn(80, 150) }
        .distinctUntilChanged()

    suspend fun setFontPercent(percent: Int) = withContext(io) {
        preferences.edit { it[FONT_PERCENT] = percent.coerceIn(80, 150) }
        Unit
    }

    companion object { internal val FONT_PERCENT = intPreferencesKey("appearance_font_percent") }
}
