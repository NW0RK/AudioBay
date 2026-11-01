package com.audiobookbay.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.data.storage.AppearanceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settings: AppearanceSettings) : ViewModel() {
    val fontPercent = settings.fontPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 100)
    private val _saveFailed = MutableStateFlow(false)
    val saveFailed = _saveFailed.asStateFlow()
    private val changes = Channel<Int>(Channel.CONFLATED)

    init { viewModelScope.launch {
        for (percent in changes) {
            try { settings.setFontPercent(percent); _saveFailed.value = false }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _saveFailed.value = true }
        }
    } }

    fun setFontPercent(percent: Int) { changes.trySend(percent.coerceIn(80, 150)) }
}
