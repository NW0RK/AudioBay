package com.audiobookbay.app.ui.screens.downloads

/**
 * Formats a torrent engine ETA for display.
 *
 * Sub-minute estimates are intentionally coarse because native rates change every poll. A
 * non-positive value means "unknown" (for example while metadata is still being fetched).
 */
internal fun formatRemainingTime(seconds: Long): String? {
    if (seconds <= 0) return null
    if (seconds < 60) return "<1m"
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${(minutes % 60).toString().padStart(2, '0')}m" else "${minutes}m"
}
