package com.audiobookbay.app

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.audiobookbay.app.torrent.TorrentDownloadManagerImpl

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadStartupEntryPoint {
    fun manager(): TorrentDownloadManagerImpl
}


