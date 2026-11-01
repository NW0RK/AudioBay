package com.audiobookbay.app.di

import com.audiobookbay.app.data.remote.AudiobookRemoteDataSource
import com.audiobookbay.app.data.remote.AudiobookRemoteDataSourceImpl
import com.audiobookbay.app.data.repository.AudiobookRepositoryImpl
import com.audiobookbay.app.data.repository.AudiobookSearchRepositoryImpl
import com.audiobookbay.app.data.repository.LibraryRepositoryImpl
import com.audiobookbay.app.data.repository.PlaybackProgressRepositoryImpl
import com.audiobookbay.app.domain.repository.AudiobookRepository
import com.audiobookbay.app.domain.repository.AudiobookSearchRepository
import com.audiobookbay.app.domain.repository.LibraryRepository
import com.audiobookbay.app.domain.repository.PlaybackProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAudiobookRemoteDataSource(
        impl: AudiobookRemoteDataSourceImpl,
    ): AudiobookRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAudiobookRepository(
        impl: AudiobookRepositoryImpl,
    ): AudiobookRepository

    @Binds
    @Singleton
    abstract fun bindAudiobookSearchRepository(
        impl: AudiobookSearchRepositoryImpl,
    ): AudiobookSearchRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl,
    ): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackProgressRepository(
        impl: PlaybackProgressRepositoryImpl,
    ): PlaybackProgressRepository
}
