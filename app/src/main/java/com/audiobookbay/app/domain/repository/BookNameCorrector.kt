package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Resource

/** Null fields mean the supplied name could not be resolved reliably. */
data class CorrectedBookNames(val title: String?, val author: String?)

interface BookNameCorrector {
    val available: Boolean
    val availability: kotlinx.coroutines.flow.Flow<Boolean>
        get() = kotlinx.coroutines.flow.flowOf(available)
    suspend fun correct(title: String, author: String): Resource<CorrectedBookNames>
}
