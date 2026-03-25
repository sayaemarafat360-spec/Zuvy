package com.zuvy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zuvy.app.data.local.dao.*
import com.zuvy.app.data.local.entity.*

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        QueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
}
