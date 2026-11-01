package com.audiobookbay.app.ui.screens.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CategoryContent(
    val categories: List<Category> = emptyList(), val slug: String? = null,
    val books: List<Audiobook> = emptyList(), val loading: Boolean = false,
    val failed: Boolean = false, val endReached: Boolean = false,
)
@HiltViewModel
class CategoriesViewModel @Inject constructor(private val repository: AudiobookRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(CategoryContent(slug = if (saved.contains("selectedCategory")) saved.get<String>("selectedCategory") else saved.get<String>("slug")))
    val state = mutable.asStateFlow()
    private var page = 0
    private var request: Job? = null
    init { loadCategories(); if (mutable.value.slug != null) loadMore() }
    fun loadCategories() {
        viewModelScope.launch {
            if (mutable.value.slug == null) mutable.update { it.copy(loading = true, failed = false) }
            when (val result = safely { repository.getCategories() }) {
                is Resource.Success -> mutable.update { it.copy(categories = result.data, loading = if (it.slug == null) false else it.loading) }
                else -> if (mutable.value.slug == null) mutable.update { it.copy(loading = false, failed = true) }
            }
        }
    }
    fun select(slug: String?) {
        request?.cancel(); page = 0; saved["selectedCategory"] = slug
        mutable.update { CategoryContent(categories = it.categories, slug = slug) }
        if (slug != null) loadMore() else if (mutable.value.categories.isEmpty()) loadCategories()
    }
    fun retry() { if (mutable.value.slug == null) loadCategories() else loadMore() }
    fun loadMore() {
        val slug = mutable.value.slug ?: return
        if (request?.isActive == true || mutable.value.endReached) return
        mutable.update { it.copy(loading = true, failed = false) }
        request = viewModelScope.launch {
            when (val result = safely { repository.getAudiobooksByCategory(slug, page + 1) }) {
                is Resource.Success -> {
                    val previous = mutable.value.books
                    val books = (previous + result.data).distinctBy { it.source to it.id }
                    page++
                    mutable.update { it.copy(books = books, loading = false, endReached = result.data.isEmpty() || books.size == previous.size) }
                }
                else -> mutable.update { it.copy(loading = false, failed = true) }
            }
        }
    }
    private suspend fun <T> safely(block: suspend () -> Resource<T>): Resource<T> = try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Resource.Error(e.message.orEmpty(), e) }
}
