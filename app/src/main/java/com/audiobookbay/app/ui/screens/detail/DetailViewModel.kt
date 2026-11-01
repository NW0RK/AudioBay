package com.audiobookbay.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookRepository
import com.audiobookbay.app.ui.navigation.NavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AudiobookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<NavRoute.AudiobookDetail>().audiobookId
    private val _uiState = MutableStateFlow<UiState<AudiobookDetail>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null
    init { load() }
    fun load() {
        request?.cancel()
        _uiState.value = UiState.Loading
        request = viewModelScope.launch {
            val result = try { repository.getAudiobookDetail(id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Resource.Error(e.message.orEmpty(), e) }
            _uiState.value = when (result) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.message)
                Resource.Loading -> UiState.Loading
            }
        }
    }
}
