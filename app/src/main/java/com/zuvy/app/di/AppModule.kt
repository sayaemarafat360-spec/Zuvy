package com.zuvy.app.di

import android.content.Context
import androidx.room.Room
import com.zuvy.app.data.local.AppDatabase
import com.zuvy.app.data.local.dao.FavoriteDao
import com.zuvy.app.data.local.dao.HistoryDao
import com.zuvy.app.data.local.dao.PlaylistDao
import com.zuvy.app.data.local.dao.QueueDao
import com.zuvy.app.data.repository.MediaRepository
import com.zuvy.app.notifications.NotificationEngine
import com.zuvy.app.search.SearchEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "zuvy_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideQueueDao(database: AppDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context
    ): MediaRepository {
        return MediaRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationEngine(
        @ApplicationContext context: Context
    ): NotificationEngine {
        return NotificationEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideSearchEngine(
        @ApplicationContext context: Context,
        mediaRepository: MediaRepository
    ): SearchEngine {
        return SearchEngine(context, mediaRepository)
    }
}
