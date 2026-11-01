package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Catalog snapshots are separate from owned books so refreshes cannot overwrite local file paths. */
@Entity(tableName = "catalog_cache")
data class CatalogCacheEntity(@PrimaryKey val cacheKey: String, val payload: String)
