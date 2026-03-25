package com.zuvy.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val thumbnailPath: String? = null
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val mediaUri: String,
    val mediaName: String,
    val mediaType: String, // "video" or "audio"
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaUri: String,
    val mediaName: String,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaUri: String,
    val mediaName: String,
    val mediaType: String,
    val lastPosition: Long = 0,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaUri: String,
    val mediaName: String,
    val mediaType: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
