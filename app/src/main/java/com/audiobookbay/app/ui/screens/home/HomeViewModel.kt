package com.audiobookbay.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.Resource
import com.audiobookbay.app.domain.model.UiState
import com.audiobookbay.app.domain.repository.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeContent(
    val books: List<Audiobook> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AudiobookRepository,
    val discovery: com.audiobookbay.app.domain.repository.RecommendationRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _generating = MutableStateFlow(false)
    val generating = _generating.asStateFlow()
    private val _discoveryError = MutableStateFlow<String?>(null)
    val discoveryError = _discoveryError.asStateFlow()
    fun generatePicks() {
        if (_generating.value || discovery == null) return
        _generating.value = true
        _discoveryError.value = null
        viewModelScope.launch {
            try { _discoveryError.value = (discovery.generate() as? Resource.Error)?.message }
            finally { _generating.value = false }
        }
    }
    private var page = 0
    private var failedPage = 1
    private var request: Job? = null

    private val _categories = MutableStateFlow<List<com.audiobookbay.app.domain.model.Category>>(emptyList())
    val categories = _categories.asStateFlow()
    init {
        viewModelScope.launch { discovery?.restore() }
        refresh()
        viewModelScope.launch {
            try {
                val result = repository.getCategories()
                if (result is Resource.Success) _categories.value = result.data
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { timber.log.Timber.w(e, "Categories unavailable") }
        }
    }

    fun refresh() {
        request?.cancel()
        load(1)
    }

    fun loadMore() {
        val content = (_uiState.value as? UiState.Success)?.data ?: return
        if (request?.isActive == true || content.endReached || content.hasError) return
        load(page + 1)
    }

    fun retry() {
        if (request?.isActive == true) return
        load(failedPage)
    }

    private fun load(targetPage: Int) {
        val previous = (_uiState.value as? UiState.Success)?.data
        _uiState.value = previous?.let {
            UiState.Success(it.copy(isRefreshing = targetPage == 1, isLoadingMore = targetPage > 1, hasError = false))
        } ?: UiState.Loading
        request = viewModelScope.launch {
            val result = try {
                repository.getRecentAudiobooks(targetPage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Resource.Error(e.message.orEmpty(), e)
            }
            when (result) {
                is Resource.Success -> {
                    val oldBooks = if (targetPage == 1) emptyList() else previous?.books.orEmpty()
                    val books = (oldBooks + result.data).distinctBy { it.source to it.id }
                    page = targetPage
                    _uiState.value = UiState.Success(HomeContent(
                        books = books,
                        endReached = result.data.isEmpty() || (targetPage > 1 && books.size == oldBooks.size),
                    ))
                }
                is Resource.Error, Resource.Loading -> {
                    failedPage = targetPage
                    _uiState.value = previous?.let {
                        UiState.Success(it.copy(isRefreshing = false, isLoadingMore = false, hasError = true))
                    } ?: UiState.Error((result as? Resource.Error)?.message.orEmpty())
                }
            }
        }
    }
}
