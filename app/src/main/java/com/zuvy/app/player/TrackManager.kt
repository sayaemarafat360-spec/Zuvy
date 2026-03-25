package com.zuvy.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages audio and video track selection
 */
class TrackManager(private val player: ExoPlayer) {
    
    data class TrackInfo(
        val id: Int,
        val name: String,
        val language: String?,
        val mimeType: String?,
        val bitrate: Int?,
        val resolution: String?,
        val isSelected: Boolean
    )
    
    // Audio tracks
    fun getAudioTracks(): List<TrackInfo> {
        val tracks = mutableListOf<TrackInfo>()
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    tracks.add(TrackInfo(
                        id = i,
                        name = format.label ?: format.language ?: "Track ${i + 1}",
                        language = format.language,
                        mimeType = format.sampleMimeType,
                        bitrate = format.bitrate,
                        resolution = null,
                        isSelected = group.isTrackSelected(i)
                    ))
                }
            }
        }
        
        return tracks
    }
    
    // Video tracks (quality)
    fun getVideoTracks(): List<TrackInfo> {
        val tracks = mutableListOf<TrackInfo>()
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val resolution = if (format.width > 0 && format.height > 0) {
                        "${format.width}x${format.height}"
                    } else null
                    
                    tracks.add(TrackInfo(
                        id = i,
                        name = format.label ?: resolution ?: "Quality ${i + 1}",
                        language = format.language,
                        mimeType = format.sampleMimeType,
                        bitrate = format.bitrate,
                        resolution = resolution,
                        isSelected = group.isTrackSelected(i)
                    ))
                }
            }
        }
        
        return tracks.sortedByDescending { it.bitrate ?: 0 }
    }
    
    // Subtitle tracks
    fun getSubtitleTracks(): List<TrackInfo> {
        val tracks = mutableListOf<TrackInfo>()
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    tracks.add(TrackInfo(
                        id = i,
                        name = format.label ?: format.language ?: "Subtitle ${i + 1}",
                        language = format.language,
                        mimeType = format.sampleMimeType,
                        bitrate = null,
                        resolution = null,
                        isSelected = group.isTrackSelected(i)
                    ))
                }
            }
        }
        
        return tracks
    }
    
    fun selectAudioTrack(trackId: Int) {
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                val trackSelectionOverride = TrackSelectionOverride(group.mediaTrackGroup, trackId)
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(trackSelectionOverride)
                    .build()
                return
            }
        }
    }
    
    fun selectVideoTrack(trackId: Int) {
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                val trackSelectionOverride = TrackSelectionOverride(group.mediaTrackGroup, trackId)
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(trackSelectionOverride)
                    .build()
                return
            }
        }
    }
    
    fun selectSubtitleTrack(trackId: Int?) {
        val currentTracks = player.currentTracks
        
        currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                if (trackId == null) {
                    // Disable subtitles
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                } else {
                    val trackSelectionOverride = TrackSelectionOverride(group.mediaTrackGroup, trackId)
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(trackSelectionOverride)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                }
                return
            }
        }
    }
    
    // Audio boost (software amplification)
    fun setAudioBoost(boost: Float) {
        // ExoPlayer doesn't have native audio boost
        // This would need custom audio processing
        // For now, we'll skip this or use a custom AudioProcessor
    }
    
    // Get current quality info
    fun getCurrentQuality(): String? {
        val videoTracks = getVideoTracks()
        return videoTracks.find { it.isSelected }?.resolution
    }
    
    fun getCurrentAudioLanguage(): String? {
        val audioTracks = getAudioTracks()
        return audioTracks.find { it.isSelected }?.language
    }
}
