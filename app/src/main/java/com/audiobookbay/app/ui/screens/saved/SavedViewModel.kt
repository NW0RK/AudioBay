package com.audiobookbay.app.ui.screens.saved
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.SavedBooksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SavedViewModel @Inject constructor(private val repository: SavedBooksRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = reload.flatMapLatest { repository.observeSaved() }.map { result ->
        when (result) {
            is Resource.Success -> UiState.Success(result.data)
            is Resource.Error -> UiState.Error(result.message)
            Resource.Loading -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    private val _failedBook = MutableStateFlow<Audiobook?>(null)
    val failedBook = _failedBook.asStateFlow()
    private var failedTarget = false
    fun retryLoad() { reload.value++ }
    fun change(book: Audiobook, saved: Boolean) {
        viewModelScope.launch {
            when (repository.setSaved(book, saved)) {
                is Resource.Error -> { failedTarget = saved; _failedBook.value = book }
                else -> _failedBook.value = null
            }
        }
    }
    fun retryChange() { _failedBook.value?.let { change(it, failedTarget) } }
}
