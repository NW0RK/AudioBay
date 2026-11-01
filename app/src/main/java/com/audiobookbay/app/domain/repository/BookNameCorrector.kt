package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Resource

/** Null fields mean the supplied name could not be resolved reliably. */
data class CorrectedBookNames(val title: String?, val author: String?)

interface BookNameCorrector {
    val available: Boolean
    val availability: kotlinx.coroutines.flow.Flow<Boolean>
        get() = kotlinx.coroutines.flow.flowOf(available)
    /** One response per supplied book, in order; originals remain authoritative on failure. */
    suspend fun formatSearchResults(books: List<com.audiobookbay.app.domain.model.Audiobook>): Resource<List<CorrectedBookNames>> =
        Resource.Error("Search formatting is unavailable")
    suspend fun correct(title: String, author: String): Resource<CorrectedBookNames>
}
