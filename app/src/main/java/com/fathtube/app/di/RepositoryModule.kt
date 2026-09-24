package com.fathtube.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.fathtube.app.data.local.PlayerPreferences
import com.fathtube.app.data.repository.YouTubeRepository
import com.fathtube.app.data.shorts.ChannelReelIndex
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideYouTubeRepository(
        playerPreferences: PlayerPreferences,
        channelReelIndex: ChannelReelIndex,
    ): YouTubeRepository = YouTubeRepository.getInstance(playerPreferences, channelReelIndex)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.local.SubscriptionRepository =
        com.fathtube.app.data.local.SubscriptionRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideLikedVideosRepository(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.local.LikedVideosRepository =
        com.fathtube.app.data.local.LikedVideosRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideAccountRepository(
        @ApplicationContext context: Context,
        subscriptionRepository: com.fathtube.app.data.local.SubscriptionRepository,
    ): com.fathtube.app.data.account.AccountRepository =
        com.fathtube.app.data.account.AccountRepository.getInstance(context, subscriptionRepository)

    @Provides
    @Singleton
    fun provideViewHistory(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.local.ViewHistory =
        com.fathtube.app.data.local.ViewHistory
            .getInstance(context)

    @Provides
    @Singleton
    fun provideHomeFeedCacheRepository(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.local.HomeFeedCacheRepository =
        com.fathtube.app.data.local
            .HomeFeedCacheRepository(context)

    @Provides
    @Singleton
    fun provideMusicPlaylistRepository(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.music.PlaylistRepository =
        com.fathtube.app.data.music
            .PlaylistRepository(context)

    // VideoDownloadManager is now @Singleton @Inject — Hilt provides it automatically
    @Provides
    @Singleton
    fun providePlayerPreferences(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.local.PlayerPreferences =
        com.fathtube.app.data.local
            .PlayerPreferences(context)

    @Provides
    @Singleton
    fun provideShortsRepository(
        @ApplicationContext context: Context,
    ): com.fathtube.app.data.shorts.ShortsRepository =
        com.fathtube.app.data.shorts.ShortsRepository
            .getInstance(context)
}
