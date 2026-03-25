package com.zuvy.app.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: String,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val dateAdded: Long = 0
) : Parcelable

@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val albumArtUri: Uri? = null
) : Parcelable

@Parcelize
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long,
    val songCount: Int = 0,
    val albumArtUri: Uri? = null,
    val year: Int = 0
) : Parcelable

@Parcelize
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int = 0
) : Parcelable
