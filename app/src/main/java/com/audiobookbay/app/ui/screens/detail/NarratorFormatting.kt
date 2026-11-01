package com.audiobookbay.app.ui.screens.detail

internal fun splitNarrators(value: String?): List<String> = value.orEmpty()
    .split(Regex("[,;\\r\\n]+"))
    .map(String::trim)
    .filter(String::isNotEmpty)
