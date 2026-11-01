package com.audiobookbay.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: AudiobookSearchRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _uiState = MutableStateFlow<UiState<List<Audiobook>>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()
    val history = repository.getSearchHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private var request: Job? = null
    private var page = 0
    private val _endReached = MutableStateFlow(false)
    val endReached = _endReached.asStateFlow()
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()
    private val _pageError = MutableStateFlow(false)
    val pageError = _pageError.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
        request?.cancel()
        page = 0
        _endReached.value = false
        _isLoadingMore.value = false
        _pageError.value = false
        _uiState.value = if (value.isBlank()) UiState.Idle else UiState.Loading
        if (value.isBlank()) return
        request = viewModelScope.launch { delay(300); fetch(value.trim(), 1) }
    }

    fun retry() {
        if (request?.isActive == true || query.value.isBlank()) return
        request = viewModelScope.launch { fetch(query.value.trim(), if (page == 0) 1 else page + 1) }
    }

    fun loadMore() {
        if (_endReached.value || request?.isActive == true || page == 0) return
        request = viewModelScope.launch { fetch(query.value.trim(), page + 1) }
    }

    fun clearHistory() { viewModelScope.launch { repository.clearSearchHistory() } }

    private suspend fun fetch(query: String, targetPage: Int) {
        _isLoadingMore.value = targetPage > 1
        _pageError.value = false
        val result = try { repository.searchAudiobooks(query, targetPage) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Resource.Error(e.message.orEmpty(), e) }
        when (result) {
            is Resource.Success -> {
                val previous = if (targetPage == 1) emptyList() else (_uiState.value as? UiState.Success)?.data.orEmpty()
                val books = (previous + result.data).distinctBy { it.source to it.id }
                _endReached.value = result.data.isEmpty() || (targetPage > 1 && books.size == previous.size)
                page = targetPage
                _uiState.value = UiState.Success(books)
            }
            is Resource.Error, Resource.Loading -> {
                if (targetPage > 1) _pageError.value = true
                else _uiState.value = UiState.Error((result as? Resource.Error)?.message.orEmpty())
            }
        }
        _isLoadingMore.value = false
    }
}
