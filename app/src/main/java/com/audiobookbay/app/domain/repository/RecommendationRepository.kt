package com.audiobookbay.app.domain.repository
import com.audiobookbay.app.domain.model.*
import kotlinx.coroutines.flow.*
interface DownloadHistoryRepository { val history: Flow<List<DownloadedBook>> }
interface RecommendationRepository : DownloadHistoryRepository {
    val available: Flow<Boolean>
    val picks: StateFlow<List<DiscoveryPick>>
    suspend fun restore()
    suspend fun generate(): Resource<List<DiscoveryPick>>
    suspend fun clear()
}
