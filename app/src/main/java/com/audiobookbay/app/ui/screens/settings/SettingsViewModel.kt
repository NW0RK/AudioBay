package com.audiobookbay.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.data.storage.DownloadFolderSettings
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val folders: DownloadFolderSettings,
    private val manager: TorrentDownloadManager,
    network: DownloadNetworkSettings,
    private val keys: com.audiobookbay.app.data.storage.ApiKeySettings,
) : ViewModel() {
    val keyConfigured = keys.configured
    private val _keyBusy = MutableStateFlow(false)
    val keyBusy = _keyBusy.asStateFlow()
    private val _keyError = MutableStateFlow(false)
    val keyError = _keyError.asStateFlow()
    private val _keySaved = MutableStateFlow(false)
    val keySaved = _keySaved.asStateFlow()

    fun saveApiKey(input: String) {
        if (_keyBusy.value) return
        _keyBusy.value = true
        _keyError.value = false
        _keySaved.value = false
        viewModelScope.launch {
            try {
                keys.save(input)
                _keySaved.value = true
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _keyError.value = true }
            finally { _keyBusy.value = false }
        }
    }
    val folder = folders.folder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val wifiOnly = network.wifiOnly.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val _error = MutableStateFlow(false)
    val error = _error.asStateFlow()
    private val _networkError = MutableStateFlow(false)
    val networkError = _networkError.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun label(uri: String?) = folders.display(uri)
    fun select(uri: String) {
        if (_busy.value) return
        _busy.value = true; _error.value = false
        viewModelScope.launch {
            try { folders.select(uri) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = true }
            finally { _busy.value = false }
        }
    }
    fun setWifiOnly(enabled: Boolean) {
        _networkError.value = false
        viewModelScope.launch {
            try { manager.updateNetworkSettings(enabled, maxDownloadSpeedBytes = 0, maxUploadSpeedBytes = 0) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _networkError.value = true }
        }
    }
}
