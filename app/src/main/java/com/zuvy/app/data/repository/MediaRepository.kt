package com.zuvy.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.zuvy.app.data.model.Folder
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.data.model.Song
import com.zuvy.app.player.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _videos = mutableListOf<MediaItem>()
    private val _music = mutableListOf<Song>()
    private val _folders = mutableListOf<Folder>()

    val videos: List<MediaItem> get() = _videos.toList()
    val music: List<Song> get() = _music.toList()
    val folders: List<Folder> get() = _folders.toList()

    private var isLoaded = false

    suspend fun loadAllMedia(forceReload: Boolean = false) = withContext(Dispatchers.IO) {
        if (isLoaded && !forceReload) return@withContext

        loadVideos()
        loadMusic()
        loadFolders()
        isLoaded = true
    }

    private fun loadVideos() {
        _videos.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RESOLUTION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error querying videos", e)
            null
        }

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: continue
                    val path = it.getString(pathColumn) ?: continue
                    
                    // Skip if file doesn't exist
                    if (!File(path).exists()) continue
                    
                    val size = it.getLong(sizeColumn)
                    val duration = it.getLong(durationColumn)
                    val width = it.getInt(widthColumn)
                    val height = it.getInt(heightColumn)
                    val mimeType = it.getString(mimeColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val dateModified = it.getLong(dateModifiedColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )

                    _videos.add(
                        MediaItem(
                            id = id,
                            name = name,
                            uri = contentUri,
                            path = path,
                            size = size,
                            duration = duration,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            isVideo = true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("MediaRepository", "Error reading video row", e)
                }
            }
        }

        Log.d("MediaRepository", "Loaded ${_videos.size} videos")
    }

    private fun loadMusic() {
        _music.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} = 1",
                null,
                sortOrder
            )
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error querying music", e)
            null
        }

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: continue
                    val path = it.getString(pathColumn) ?: continue
                    
                    // Skip if file doesn't exist
                    if (!File(path).exists()) continue
                    
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn)
                    val duration = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val trackNumber = it.getInt(trackColumn)
                    val year = it.getInt(yearColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                    )

                    _music.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = formatDuration(duration),
                            uri = contentUri,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            trackNumber = trackNumber,
                            year = year
                        )
                    )
                } catch (e: Exception) {
                    Log.e("MediaRepository", "Error reading music row", e)
                }
            }
        }

        Log.d("MediaRepository", "Loaded ${_music.size} songs")
    }

    private fun loadFolders() {
        _folders.clear()

        val folderMap = mutableMapOf<String, Folder>()

        _videos.forEach { video ->
            val file = File(video.path)
            val parentPath = file.parent ?: return@forEach
            val folderName = file.parentFile?.name ?: "Unknown"

            val existing = folderMap[parentPath]
            if (existing != null) {
                folderMap[parentPath] = existing.copy(
                    videoCount = existing.videoCount + 1,
                    totalSize = existing.totalSize + video.size
                )
            } else {
                folderMap[parentPath] = Folder(
                    path = parentPath,
                    name = folderName,
                    videoCount = 1,
                    totalSize = video.size
                )
            }
        }

        _folders.addAll(folderMap.values.sortedByDescending { it.videoCount })
        Log.d("MediaRepository", "Loaded ${_folders.size} folders")
    }

    fun getVideosByFolder(folderPath: String): List<MediaItem> {
        return _videos.filter { it.path.startsWith(folderPath) }
    }

    fun searchVideos(query: String): List<MediaItem> {
        return _videos.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
    }

    fun searchMusic(query: String): List<Song> {
        return _music.filter { 
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album?.contains(query, ignoreCase = true) == true
        }
    }

    fun getMediaByUri(uri: Uri): MediaItem? {
        return _videos.find { it.uri == uri }
    }

    fun getSongByUri(uri: Uri): Song? {
        return _music.find { it.uri == uri }
    }

    fun sortVideos(sortBy: SortBy, ascending: Boolean = true): List<MediaItem> {
        return when (sortBy) {
            SortBy.NAME -> if (ascending) _videos.sortedBy { it.name.lowercase() }
                          else _videos.sortedByDescending { it.name.lowercase() }
            SortBy.DATE -> if (ascending) _videos.sortedBy { it.dateAdded }
                          else _videos.sortedByDescending { it.dateAdded }
            SortBy.SIZE -> if (ascending) _videos.sortedBy { it.size }
                          else _videos.sortedByDescending { it.size }
            SortBy.DURATION -> if (ascending) _videos.sortedBy { it.duration }
                              else _videos.sortedByDescending { it.duration }
        }
    }

    fun sortMusic(sortBy: SortBy, ascending: Boolean = true): List<Song> {
        return when (sortBy) {
            SortBy.NAME -> if (ascending) _music.sortedBy { it.title.lowercase() }
                          else _music.sortedByDescending { it.title.lowercase() }
            SortBy.DATE -> if (ascending) _music.sortedBy { it.dateAdded }
                          else _music.sortedByDescending { it.dateAdded }
            SortBy.DURATION -> if (ascending) _music.sortedBy { it.dateAdded } // Using dateAdded as proxy
                              else _music.sortedByDescending { it.dateAdded }
            else -> _music
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

enum class SortBy {
    NAME, DATE, SIZE, DURATION
}
