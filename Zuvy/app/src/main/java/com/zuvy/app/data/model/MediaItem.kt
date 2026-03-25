package com.zuvy.app.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val path: String,
    val size: Long,
    val duration: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String? = null,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val isVideo: Boolean = true
) : Parcelable {

    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""

    val isHD: Boolean
        get() = height >= 720

    val isFullHD: Boolean
        get() = height >= 1080

    val is4K: Boolean
        get() = height >= 2160
}
