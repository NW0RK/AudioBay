package com.audiobookbay.app.torrent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Describes the connection used by the torrent engine.
 *
 * A network without internet or without the unmetered capability (including no network at all)
 * is treated as blocked for Wi-Fi-only downloads. Callers re-check this value while downloads
 * run so a dropped Wi-Fi connection pauses transfers instead of falling back to mobile data.
 */
@Singleton
class NetworkStatus @Inject constructor(@ApplicationContext private val context: Context) {
    fun isUnmetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
