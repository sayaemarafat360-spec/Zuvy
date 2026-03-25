package com.zuvy.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val path: String,
    val name: String,
    val videoCount: Int = 0,
    val totalSize: Long = 0
) : Parcelable

@Parcelize
data class Playlist(
    val id: String,
    val name: String,
    val videoCount: Int = 0,
    val totalDuration: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val iconRes: Int? = null,
    val thumbnails: List<String> = emptyList()
) : Parcelable
