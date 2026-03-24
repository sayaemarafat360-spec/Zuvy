package com.zuvy.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.zuvy.app.data.model.Folder
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.data.model.Song
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

    suspend fun loadAllMedia() = withContext(Dispatchers.IO) {
        loadVideos()
        loadMusic()
        loadFolders()
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
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

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
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn)
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
            }
        }
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
            MediaStore.Audio.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} = 1",
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val path = it.getString(pathColumn)
                val albumId = it.getLong(albumIdColumn)
                val dateAdded = it.getLong(dateAddedColumn)

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
                        dateAdded = dateAdded
                    )
                )
            }
        }
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
    }

    fun getVideos(): List<MediaItem> = _videos.toList()
    fun getMusic(): List<Song> = _music.toList()
    fun getFolders(): List<String> = _folders.map { it.path }

    fun searchVideos(query: String): List<MediaItem> {
        return _videos.filter { it.name.contains(query, ignoreCase = true) }
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
