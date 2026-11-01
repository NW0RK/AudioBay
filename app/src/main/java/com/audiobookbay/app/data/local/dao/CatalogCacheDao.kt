package com.audiobookbay.app.data.local.dao

import androidx.room.*
import com.audiobookbay.app.data.local.entity.CatalogCacheEntity

@Dao
interface CatalogCacheDao {
    @Query("SELECT * FROM catalog_cache WHERE cacheKey = :key")
    suspend fun get(key: String): CatalogCacheEntity?
    @Query("DELETE FROM catalog_cache WHERE cacheKey LIKE :prefix")
    suspend fun deleteMatching(prefix: String)
    @Upsert suspend fun put(entry: CatalogCacheEntity)
}
