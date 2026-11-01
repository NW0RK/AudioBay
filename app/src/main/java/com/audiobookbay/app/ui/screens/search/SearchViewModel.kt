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
class SearchViewModel @Inject constructor(
    private val repository: AudiobookSearchRepository,
    private val formattingSettings: com.audiobookbay.app.data.storage.SearchFormattingSettings,
    private val corrector: com.audiobookbay.app.domain.repository.BookNameCorrector,
) : ViewModel() {
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

    private var originals = emptyList<Audiobook>()
    private val nameCache = linkedMapOf<Audiobook, com.audiobookbay.app.domain.repository.CorrectedBookNames>()
    private var formattingJob: Job? = null
    private var formattingGeneration = 0
    private var formattingAllowed = false
    private val _formatting = MutableStateFlow(false)
    val formatting = _formatting.asStateFlow()
    private val _formattingFailed = MutableStateFlow(false)
    val formattingFailed = _formattingFailed.asStateFlow()

    init {
        viewModelScope.launch {
            combine(formattingSettings.enabled, corrector.availability) { enabled, available -> enabled && available }
                .distinctUntilChanged().collect { formattingAllowed = it; formatNames() }
        }
    }

    private fun cancelFormatting() {
        formattingGeneration++
        formattingJob?.cancel()
        _formatting.value = false
        _formattingFailed.value = false
    }

    private fun displayedBooks() = originals.map { book ->
        val names = if (formattingAllowed) nameCache[book] else null
        book.copy(title = names?.title?.takeIf { it.isNotBlank() } ?: book.title,
            author = names?.author?.takeIf { it.isNotBlank() } ?: book.author)
    }

    private fun formatNames() {
        cancelFormatting()
        if (_uiState.value !is UiState.Success) return
        _uiState.value = UiState.Success(displayedBooks())
        if (!formattingAllowed) return
        val missing = originals.filter { it !in nameCache }
        if (missing.isEmpty()) return
        val generation = formattingGeneration
        formattingJob = viewModelScope.launch {
            _formatting.value = true
            try {
                for (batch in missing.chunked(20)) {
                    val result = corrector.formatSearchResults(batch)
                    ensureActive()
                    if (generation != formattingGeneration) return@launch
                    if (result !is Resource.Success || result.data.size != batch.size) {
                        _formattingFailed.value = true
                        break
                    }
                    batch.zip(result.data).forEach { (book, names) -> nameCache[book] = names }
                    while (nameCache.size > 200) nameCache.remove(nameCache.keys.first())
                    _uiState.value = UiState.Success(displayedBooks())
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == formattingGeneration) _formattingFailed.value = true }
            finally { if (generation == formattingGeneration) _formatting.value = false }
        }
    }

    fun reset() {
        request?.cancel()
        cancelFormatting()
        originals = emptyList()
        _query.value = ""
        page = 0
        _endReached.value = false
        _isLoadingMore.value = false
        _pageError.value = false
        _uiState.value = UiState.Idle
    }

    fun setQuery(value: String) {
        if (_query.value == value) return
        _query.value = value
        request?.cancel()
        cancelFormatting()
        originals = emptyList()
        page = 0
        _endReached.value = false
        _isLoadingMore.value = false
        _pageError.value = false
        _uiState.value = if (value.isBlank()) UiState.Idle else UiState.Loading
        if (value.isBlank()) return
        request = viewModelScope.launch { delay(300); fetch(value.trim(), 1) }
    }

    fun submit() {
        val term = query.value.trim().replace(Regex("\\s+"), " ")
        if (term.isBlank()) return
        request?.cancel()
        cancelFormatting()
        originals = emptyList()
        page = 0
        _endReached.value = false
        _pageError.value = false
        _isLoadingMore.value = false
        _uiState.value = UiState.Loading
        request = viewModelScope.launch { fetch(term, 1) }
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
                val previous = if (targetPage == 1) emptyList() else originals
                val books = (previous + result.data).distinctBy { it.source to it.id }
                _endReached.value = result.data.isEmpty() || (targetPage > 1 && books.size == previous.size)
                page = targetPage
                originals = books
                _uiState.value = UiState.Success(displayedBooks())
                formatNames()
            }
            is Resource.Error, Resource.Loading -> {
                if (targetPage > 1) _pageError.value = true
                else _uiState.value = UiState.Error((result as? Resource.Error)?.message.orEmpty())
            }
        }
        _isLoadingMore.value = false
    }
}
