package com.fathtube.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.fathtube.app.data.local.AppDatabase
import com.fathtube.app.data.local.dao.NotificationDao
import com.fathtube.app.data.local.dao.PlaylistDao
import com.fathtube.app.data.local.dao.VideoDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabase.getDatabase(context)

    @Provides
    fun provideVideoDao(database: AppDatabase): VideoDao = database.videoDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideCacheDao(database: AppDatabase): com.fathtube.app.data.local.dao.CacheDao = database.cacheDao()

    @Provides
    fun provideDownloadDao(database: AppDatabase): com.fathtube.app.data.local.dao.DownloadDao = database.downloadDao()

    @Provides
    fun provideRecognitionHistoryDao(database: AppDatabase): com.fathtube.app.data.local.dao.RecognitionHistoryDao =
        database.recognitionHistoryDao()

    @Provides
    fun provideSubscriptionGroupDao(database: AppDatabase): com.fathtube.app.data.local.dao.SubscriptionGroupDao =
        database.subscriptionGroupDao()

    @Provides
    fun provideWatchHistoryDao(database: AppDatabase): com.fathtube.app.data.local.dao.WatchHistoryDao = database.watchHistoryDao()

    @Provides
    fun provideSyncLogDao(database: AppDatabase): com.fathtube.app.data.local.dao.SyncLogDao = database.syncLogDao()

    @Provides
    fun provideSyncPeerDao(database: AppDatabase): com.fathtube.app.data.local.dao.SyncPeerDao = database.syncPeerDao()

    @Provides
    fun provideMusicGraphDao(database: AppDatabase): com.fathtube.app.data.local.dao.MusicGraphDao = database.musicGraphDao()
}
