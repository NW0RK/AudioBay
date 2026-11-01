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
    data object Downloads : NavRoute

    @Serializable
    data object Library : NavRoute

    @Serializable
    data object NowPlaying : NavRoute

    @Serializable
    data object Settings : NavRoute
}
