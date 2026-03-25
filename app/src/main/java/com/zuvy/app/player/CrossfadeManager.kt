package com.zuvy.app.player

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.*

/**
 * Manages crossfade between tracks for smooth transitions
 */
class CrossfadeManager(
    private val scope: CoroutineScope,
    private val context: Context
) {
    
    companion object {
        const val PREFS_NAME = "crossfade_prefs"
        const val KEY_CROSSFADE_ENABLED = "crossfade_enabled"
        const val KEY_CROSSFADE_DURATION = "crossfade_duration"
        const val KEY_GAPLESS_ENABLED = "gapless_enabled"
        
        const val MIN_CROSSFADE_DURATION = 1  // seconds
        const val MAX_CROSSFADE_DURATION = 12 // seconds
        const val DEFAULT_CROSSFADE_DURATION = 3
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var crossfadeEnabled: Boolean = false
    private var crossfadeDuration: Int = DEFAULT_CROSSFADE_DURATION
    private var gaplessEnabled: Boolean = true
    
    private var crossfadeJob: Job? = null
    private var currentFadeVolume = 1f
    
    // Callbacks
    private var onFadeOut: (() -> Unit)? = null
    private var onFadeIn: (() -> Unit)? = null
    private var onVolumeChange: ((Float) -> Unit)? = null
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        crossfadeEnabled = prefs.getBoolean(KEY_CROSSFADE_ENABLED, false)
        crossfadeDuration = prefs.getInt(KEY_CROSSFADE_DURATION, DEFAULT_CROSSFADE_DURATION)
        gaplessEnabled = prefs.getBoolean(KEY_GAPLESS_ENABLED, true)
    }
    
    /**
     * Enable/disable crossfade
     */
    fun setCrossfadeEnabled(enabled: Boolean) {
        crossfadeEnabled = enabled
        prefs.edit { putBoolean(KEY_CROSSFADE_ENABLED, enabled) }
    }
    
    /**
     * Check if crossfade is enabled
     */
    fun isCrossfadeEnabled(): Boolean = crossfadeEnabled
    
    /**
     * Set crossfade duration in seconds
     */
    fun setCrossfadeDuration(durationSeconds: Int) {
        crossfadeDuration = durationSeconds.coerceIn(MIN_CROSSFADE_DURATION, MAX_CROSSFADE_DURATION)
        prefs.edit { putInt(KEY_CROSSFADE_DURATION, crossfadeDuration) }
    }
    
    /**
     * Get crossfade duration
     */
    fun getCrossfadeDuration(): Int = crossfadeDuration
    
    /**
     * Enable/disable gapless playback
     */
    fun setGaplessEnabled(enabled: Boolean) {
        gaplessEnabled = enabled
        prefs.edit { putBoolean(KEY_GAPLESS_ENABLED, enabled) }
    }
    
    /**
     * Check if gapless is enabled
     */
    fun isGaplessEnabled(): Boolean = gaplessEnabled
    
    /**
     * Set callbacks
     */
    fun setCallbacks(
        onFadeOut: () -> Unit,
        onFadeIn: () -> Unit,
        onVolumeChange: (Float) -> Unit
    ) {
        this.onFadeOut = onFadeOut
        this.onFadeIn = onFadeIn
        this.onVolumeChange = onVolumeChange
    }
    
    /**
     * Start crossfade when approaching end of track
     */
    fun startCrossfade(
        currentPosition: Long,
        duration: Long,
        onNextTrack: () -> Unit
    ) {
        if (!crossfadeEnabled || crossfadeDuration == 0) return
        
        val crossfadeMs = crossfadeDuration * 1000L
        val triggerPoint = duration - crossfadeMs
        
        if (currentPosition >= triggerPoint && duration > crossfadeMs) {
            // Start fade out
            startFadeOut(onNextTrack)
        }
    }
    
    /**
     * Fade out current track
     */
    private fun startFadeOut(onComplete: () -> Unit) {
        crossfadeJob?.cancel()
        
        val steps = crossfadeDuration * 20 // 20 updates per second
        val stepDuration = (crossfadeDuration * 1000L) / steps
        
        crossfadeJob = scope.launch {
            for (i in steps downTo 0) {
                val volume = i.toFloat() / steps
                onVolumeChange?.invoke(volume)
                currentFadeVolume = volume
                
                if (i == 0) {
                    onComplete()
                    break
                }
                
                delay(stepDuration)
            }
        }
    }
    
    /**
     * Fade in new track
     */
    fun startFadeIn() {
        crossfadeJob?.cancel()
        
        val steps = crossfadeDuration * 20
        val stepDuration = (crossfadeDuration * 1000L) / steps
        
        crossfadeJob = scope.launch {
            for (i in 0..steps) {
                val volume = i.toFloat() / steps
                onVolumeChange?.invoke(volume)
                currentFadeVolume = volume
                delay(stepDuration)
            }
            
            onFadeIn?.invoke()
        }
    }
    
    /**
     * Cancel any ongoing crossfade
     */
    fun cancelCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        currentFadeVolume = 1f
        onVolumeChange?.invoke(1f)
    }
    
    /**
     * Get current fade volume
     */
    fun getCurrentVolume(): Float = currentFadeVolume
    
    /**
     * Calculate time remaining before crossfade should start
     */
    fun getTimeUntilCrossfade(currentPosition: Long, duration: Long): Long {
        if (!crossfadeEnabled || duration <= 0) return Long.MAX_VALUE
        
        val crossfadeMs = crossfadeDuration * 1000L
        val triggerPoint = duration - crossfadeMs
        
        return (triggerPoint - currentPosition).coerceAtLeast(0)
    }
    
    /**
     * Check if should start crossfade
     */
    fun shouldStartCrossfade(currentPosition: Long, duration: Long): Boolean {
        if (!crossfadeEnabled || duration <= 0) return false
        
        val crossfadeMs = crossfadeDuration * 1000L
        val triggerPoint = duration - crossfadeMs
        
        return currentPosition >= triggerPoint && duration > crossfadeMs
    }
    
    /**
     * Release resources
     */
    fun release() {
        crossfadeJob?.cancel()
        crossfadeJob = null
    }
}

/**
 * Manages smooth volume fading for various scenarios
 */
class AudioFadeManager(
    private val scope: CoroutineScope
) {
    
    enum class FadeType {
        LINEAR, EXPONENTIAL, LOGARITHMIC, S_CURVE
    }
    
    private var fadeJob: Job? = null
    
    /**
     * Fade volume from current to target
     */
    fun fadeTo(
        targetVolume: Float,
        durationMs: Long = 500,
        fadeType: FadeType = FadeType.LINEAR,
        onVolumeChange: (Float) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        fadeJob?.cancel()
        
        val steps = (durationMs / 16).toInt() // ~60fps
        val stepDuration = durationMs / steps
        
        fadeJob = scope.launch {
            for (i in 0..steps) {
                val progress = i.toFloat() / steps
                val volume = when (fadeType) {
                    FadeType.LINEAR -> targetVolume * progress
                    FadeType.EXPONENTIAL -> targetVolume * (progress * progress)
                    FadeType.LOGARITHMIC -> targetVolume * (1 - Math.log(1 - progress + 1).toFloat())
                    FadeType.S_CURVE -> targetVolume * (0.5f * (1 + Math.tanh((progress * 4 - 2).toDouble()).toFloat()))
                }
                
                onVolumeChange(volume.coerceIn(0f, 1f))
                delay(stepDuration)
            }
            
            onVolumeChange(targetVolume)
            onComplete()
        }
    }
    
    /**
     * Fade out completely
     */
    fun fadeOut(
        durationMs: Long = 500,
        fadeType: FadeType = FadeType.LINEAR,
        onVolumeChange: (Float) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        fadeTo(0f, durationMs, fadeType, onVolumeChange, onComplete)
    }
    
    /**
     * Fade in to full volume
     */
    fun fadeIn(
        durationMs: Long = 500,
        fadeType: FadeType = FadeType.LINEAR,
        onVolumeChange: (Float) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        fadeTo(1f, durationMs, fadeType, onVolumeChange, onComplete)
    }
    
    /**
     * Duck volume (lower temporarily, e.g., for notifications)
     */
    fun duck(
        duckVolume: Float = 0.3f,
        durationMs: Long = 200,
        onVolumeChange: (Float) -> Unit
    ) {
        fadeTo(duckVolume, durationMs, FadeType.LINEAR, onVolumeChange)
    }
    
    /**
     * Unduck volume (restore to full)
     */
    fun unduck(
        durationMs: Long = 200,
        onVolumeChange: (Float) -> Unit
    ) {
        fadeIn(durationMs, FadeType.LINEAR, onVolumeChange)
    }
    
    /**
     * Cancel ongoing fade
     */
    fun cancel() {
        fadeJob?.cancel()
        fadeJob = null
    }
    
    /**
     * Release resources
     */
    fun release() {
        cancel()
    }
}
