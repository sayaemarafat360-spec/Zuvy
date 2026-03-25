package com.zuvy.app.player

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.core.content.edit

/**
 * Manages audio equalizer with presets and custom settings
 */
class EqualizerManager(
    private val context: Context,
    private val audioSessionId: Int
) {
    
    companion object {
        const val PREFS_NAME = "equalizer_prefs"
        const val KEY_ENABLED = "eq_enabled"
        const val KEY_PRESET = "eq_preset"
        const val KEY_CUSTOM_BANDS = "eq_custom_bands"
        const val KEY_BASS_BOOST = "bass_boost"
        const val KEY_BASS_BOOST_ENABLED = "bass_boost_enabled"
        const val KEY_VIRTUALIZER = "virtualizer"
        const val KEY_VIRTUALIZER_ENABLED = "virtualizer_enabled"
        
        // Preset definitions
        val PRESETS = mapOf(
            "Flat" to shortArrayOf(0, 0, 0, 0, 0),
            "Bass Boost" to shortArrayOf(6, 4, 0, -2, -4),
            "Treble Boost" to shortArrayOf(-4, -2, 0, 4, 6),
            "Bass Reducer" to shortArrayOf(-4, -2, 0, 0, 0),
            "Treble Reducer" to shortArrayOf(0, 0, 0, -2, -4),
            "Vocal Boost" to shortArrayOf(-2, -1, 4, 4, -1),
            "Rock" to shortArrayOf(5, 4, 1, 3, 5),
            "Pop" to shortArrayOf(-1, 2, 4, 2, -1),
            "Jazz" to shortArrayOf(4, 3, 1, 3, 4),
            "Classical" to shortArrayOf(5, 4, 3, 3, 5),
            "Electronic" to shortArrayOf(5, 3, 0, 3, 5),
            "Hip Hop" to shortArrayOf(5, 4, 1, 3, 4),
            "Latin" to shortArrayOf(4, 2, 0, 2, 4),
            "Acoustic" to shortArrayOf(3, 2, 0, 2, 3),
            "Loudness" to shortArrayOf(5, 4, 3, 4, 5)
        )
        
        // Band frequency labels
        val BAND_FREQUENCIES = listOf(
            "60 Hz",
            "250 Hz",
            "1 kHz",
            "4 kHz",
            "16 kHz"
        )
        
        const val MIN_BAND_LEVEL = -15
        const val MAX_BAND_LEVEL = 15
    }
    
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var isEnabled = false
    private var currentPreset = "Flat"
    private var customBands = shortArrayOf(0, 0, 0, 0, 0)
    private var bassBoostStrength = 0
    private var isBassBoostEnabled = false
    private var virtualizerStrength = 0
    private var isVirtualizerEnabled = false
    
    private var numberOfBands = 0
    private var bandLevelRange: ShortArray? = null
    
    init {
        loadSettings()
        initEqualizer()
    }
    
    private fun initEqualizer() {
        try {
            // Initialize Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEnabled
                numberOfBands = numberOfBands()
                bandLevelRange = bandLevelRange
            }
            
            // Initialize Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isBassBoostEnabled
                setStrength(bassBoostStrength.toShort())
            }
            
            // Initialize Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = isVirtualizerEnabled
                setStrength(virtualizerStrength.toShort())
            }
            
            // Apply saved settings
            if (isEnabled) {
                applyPreset(currentPreset)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Enable/disable equalizer
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        equalizer?.enabled = enabled
        saveSettings()
    }
    
    /**
     * Check if equalizer is enabled
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Apply a preset
     */
    fun applyPreset(presetName: String) {
        currentPreset = presetName
        PRESETS[presetName]?.let { levels ->
            levels.forEachIndexed { index, level ->
                setBandLevel(index, level.toInt())
            }
        }
        saveSettings()
    }
    
    /**
     * Get current preset name
     */
    fun getCurrentPreset(): String = currentPreset
    
    /**
     * Get all preset names
     */
    fun getPresetNames(): List<String> = PRESETS.keys.toList()
    
    /**
     * Set band level (-15 to +15 dB)
     */
    fun setBandLevel(bandIndex: Int, level: Int) {
        val safeLevel = level.coerceIn(MIN_BAND_LEVEL, MAX_BAND_LEVEL)
        
        equalizer?.let { eq ->
            val bandId = bandIndex.toShort()
            val millibelLevel = (safeLevel * 100).toShort()
            eq.setBandLevel(bandId, millibelLevel)
        }
        
        customBands[bandIndex] = safeLevel.toShort()
        currentPreset = "Custom"
        saveSettings()
    }
    
    /**
     * Get band level
     */
    fun getBandLevel(bandIndex: Int): Int {
        return equalizer?.getBandLevel(bandIndex.toShort())?.div(100) ?: 0
    }
    
    /**
     * Get number of bands
     */
    fun getNumberOfBands(): Int = equalizer?.numberOfBands?.toInt() ?: 5
    
    /**
     * Get center frequency of a band
     */
    fun getCenterFrequency(bandIndex: Int): Int {
        return equalizer?.getCenterFreq(bandIndex.toShort())?.toInt() ?: 0
    }
    
    /**
     * Enable/disable bass boost
     */
    fun setBassBoostEnabled(enabled: Boolean) {
        isBassBoostEnabled = enabled
        bassBoost?.enabled = enabled
        saveSettings()
    }
    
    /**
     * Set bass boost strength (0-1000)
     */
    fun setBassBoostStrength(strength: Int) {
        bassBoostStrength = strength.coerceIn(0, 1000)
        bassBoost?.setStrength(bassBoostStrength.toShort())
        saveSettings()
    }
    
    /**
     * Get bass boost settings
     */
    fun isBassBoostEnabled(): Boolean = isBassBoostEnabled
    fun getBassBoostStrength(): Int = bassBoostStrength
    
    /**
     * Enable/disable virtualizer
     */
    fun setVirtualizerEnabled(enabled: Boolean) {
        isVirtualizerEnabled = enabled
        virtualizer?.enabled = enabled
        saveSettings()
    }
    
    /**
     * Set virtualizer strength (0-1000)
     */
    fun setVirtualizerStrength(strength: Int) {
        virtualizerStrength = strength.coerceIn(0, 1000)
        virtualizer?.setStrength(virtualizerStrength.toShort())
        saveSettings()
    }
    
    /**
     * Get virtualizer settings
     */
    fun isVirtualizerEnabled(): Boolean = isVirtualizerEnabled
    fun getVirtualizerStrength(): Int = virtualizerStrength
    
    /**
     * Reset all settings to default
     */
    fun resetToDefault() {
        setEnabled(false)
        applyPreset("Flat")
        setBassBoostEnabled(false)
        setBassBoostStrength(0)
        setVirtualizerEnabled(false)
        setVirtualizerStrength(0)
    }
    
    /**
     * Get custom band levels
     */
    fun getCustomBands(): ShortArray = customBands.copyOf()
    
    /**
     * Set custom band levels
     */
    fun setCustomBands(bands: ShortArray) {
        bands.forEachIndexed { index, level ->
            setBandLevel(index, level.toInt())
        }
        currentPreset = "Custom"
        saveSettings()
    }
    
    private fun loadSettings() {
        isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        currentPreset = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"
        bassBoostStrength = prefs.getInt(KEY_BASS_BOOST, 0)
        isBassBoostEnabled = prefs.getBoolean(KEY_BASS_BOOST_ENABLED, false)
        virtualizerStrength = prefs.getInt(KEY_VIRTUALIZER, 0)
        isVirtualizerEnabled = prefs.getBoolean(KEY_VIRTUALIZER_ENABLED, false)
        
        // Load custom bands
        val bandsString = prefs.getString(KEY_CUSTOM_BANDS, "0,0,0,0,0") ?: "0,0,0,0,0"
        customBands = bandsString.split(",").map { it.toShort() }.toShortArray()
    }
    
    private fun saveSettings() {
        prefs.edit {
            putBoolean(KEY_ENABLED, isEnabled)
            putString(KEY_PRESET, currentPreset)
            putString(KEY_CUSTOM_BANDS, customBands.joinToString(","))
            putInt(KEY_BASS_BOOST, bassBoostStrength)
            putBoolean(KEY_BASS_BOOST_ENABLED, isBassBoostEnabled)
            putInt(KEY_VIRTUALIZER, virtualizerStrength)
            putBoolean(KEY_VIRTUALIZER_ENABLED, isVirtualizerEnabled)
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
