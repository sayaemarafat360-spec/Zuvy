package com.zuvy.app.data.local.dao

import androidx.room.*
import com.zuvy.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removeFromPlaylist(playlistId: Long, mediaUri: String)

    @Query("UPDATE playlists SET itemCount = (SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId), updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun updateItemCount(playlistId: Long, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaUri = :mediaUri)")
    suspend fun isFavorite(mediaUri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaUri = :mediaUri")
    suspend fun removeFromFavorites(mediaUri: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY playCount DESC LIMIT 20")
    fun getMostPlayed(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE mediaUri = :mediaUri LIMIT 1")
    suspend fun getByUri(mediaUri: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Query("UPDATE history SET lastPosition = :position, lastPlayedAt = :playedAt WHERE mediaUri = :mediaUri")
    suspend fun updatePosition(mediaUri: String, position: Long, playedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position ASC")
    fun getQueue(): Flow<List<QueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(item: QueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(items: List<QueueEntity>)

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun removeFromQueue(id: Long)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()

    @Query("UPDATE queue SET position = :newPosition WHERE id = :id")
    suspend fun updatePosition(id: Long, newPosition: Int)

    @Transaction
    suspend fun reorderQueue(fromPosition: Int, toPosition: Int) {
        val queue = getQueueSync()
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                queue[i].let { updatePosition(it.id, i + 1) }
            }
        } else {
            for (i in toPosition until fromPosition) {
                queue[i].let { updatePosition(it.id, i + 1) }
            }
        }
    }

    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun getQueueSync(): List<QueueEntity>
}
