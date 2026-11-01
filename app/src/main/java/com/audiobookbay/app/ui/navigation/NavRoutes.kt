package com.audiobookbay.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 */
sealed interface NavRoute {

    @Serializable
    data object Home : NavRoute

    @Serializable
    data object Search : NavRoute

    @Serializable
    data object Categories : NavRoute

    @Serializable
    data class AudiobookDetail(val audiobookId: String) : NavRoute

    @Serializable
    data class Downloads(val audiobookId: String? = null) : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Saved : NavRoute
}
