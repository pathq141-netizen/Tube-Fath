package com.fathtube.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.fathtube.app.data.local.dao.CacheDao
import com.fathtube.app.data.local.dao.DownloadDao
import com.fathtube.app.data.local.dao.HomeFeedCacheDao
import com.fathtube.app.data.local.dao.MusicGraphDao
import com.fathtube.app.data.local.dao.NotificationDao
import com.fathtube.app.data.local.dao.PlaylistDao
import com.fathtube.app.data.local.dao.RecognitionHistoryDao
import com.fathtube.app.data.local.dao.SubscriptionGroupDao
import com.fathtube.app.data.local.dao.SyncLogDao
import com.fathtube.app.data.local.dao.SyncPeerDao
import com.fathtube.app.data.local.dao.VideoDao
import com.fathtube.app.data.local.dao.WatchHistoryDao
import com.fathtube.app.data.local.entity.DownloadEntity
import com.fathtube.app.data.local.entity.DownloadItemEntity
import com.fathtube.app.data.local.entity.HomeFeedCacheEntity
import com.fathtube.app.data.local.entity.MusicGraphAlbumEntity
import com.fathtube.app.data.local.entity.MusicGraphArtistEntity
import com.fathtube.app.data.local.entity.MusicGraphEdgeEntity
import com.fathtube.app.data.local.entity.MusicGraphPlaylistEntity
import com.fathtube.app.data.local.entity.MusicGraphTrackEntity
import com.fathtube.app.data.local.entity.MusicHomeCacheEntity
import com.fathtube.app.data.local.entity.MusicHomeChipEntity
import com.fathtube.app.data.local.entity.NotificationEntity
import com.fathtube.app.data.local.entity.PlaylistEntity
import com.fathtube.app.data.local.entity.PlaylistVideoCrossRef
import com.fathtube.app.data.local.entity.RecognitionHistoryEntity
import com.fathtube.app.data.local.entity.SubscriptionFeedEntity
import com.fathtube.app.data.local.entity.SubscriptionGroupEntity
import com.fathtube.app.data.local.entity.SyncLogEntity
import com.fathtube.app.data.local.entity.SyncPeerEntity
import com.fathtube.app.data.local.entity.VideoEntity
import com.fathtube.app.data.local.entity.WatchHistoryEntity
import com.fathtube.app.data.local.migrations.MIGRATIONS
import com.fathtube.app.data.local.migrations.Migration24To25

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        NotificationEntity::class,
        SubscriptionFeedEntity::class,
        MusicHomeCacheEntity::class,
        MusicHomeChipEntity::class,
        DownloadEntity::class,
        DownloadItemEntity::class,
        WatchHistoryEntity::class,
        HomeFeedCacheEntity::class,
        SubscriptionGroupEntity::class,
        RecognitionHistoryEntity::class,
        SyncLogEntity::class,
        SyncPeerEntity::class,
        MusicGraphTrackEntity::class,
        MusicGraphArtistEntity::class,
        MusicGraphAlbumEntity::class,
        MusicGraphPlaylistEntity::class,
        MusicGraphEdgeEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 24, to = 25, spec = Migration24To25::class),
        AutoMigration(from = 25, to = 26),
    ],
    version = 26,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun notificationDao(): NotificationDao

    abstract fun cacheDao(): CacheDao

    abstract fun downloadDao(): DownloadDao

    abstract fun watchHistoryDao(): WatchHistoryDao

    abstract fun homeFeedCacheDao(): HomeFeedCacheDao

    abstract fun subscriptionGroupDao(): SubscriptionGroupDao

    abstract fun recognitionHistoryDao(): RecognitionHistoryDao

    abstract fun syncLogDao(): SyncLogDao

    abstract fun syncPeerDao(): SyncPeerDao

    abstract fun musicGraphDao(): MusicGraphDao

    companion object {
        @Volatile
        @Suppress("ktlint:standard:property-naming")
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "flow_database",
                    ).addMigrations(*MIGRATIONS)
                        .fallbackToDestructiveMigration(false)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
