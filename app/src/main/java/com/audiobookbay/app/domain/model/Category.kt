package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Genre or category descriptor.
 */
@Serializable
data class Category(
    val slug: String,
    val displayName: String,
    val description: String? = null,
    val bookCount: Int = 0,
)
