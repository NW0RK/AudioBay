package com.audiobookbay.app.di

import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.TorrentDownloadManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindTorrentDownloadManager(
        impl: TorrentDownloadManagerImpl,
    ): TorrentDownloadManager

}
