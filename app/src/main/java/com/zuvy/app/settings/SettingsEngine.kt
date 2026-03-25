package com.zuvy.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Advanced Settings Engine - Central management for all app settings
 */
class SettingsEngine(private val context: Context) {
    
    companion object {
        const val PREFS_NAME = "zuvy_advanced_settings"
        
        // Theme Keys
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PURE_BLACK = "pure_black_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_PLAYER_STYLE = "player_style"
        
        // Audio Keys
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_AUDIO_BOOST = "audio_boost"
        const val KEY_AUDIO_BOOST_LEVEL = "audio_boost_level"
        const val KEY_AUTOPLAY_SIMILAR = "autoplay_similar"
        const val KEY_CROSSFADE_ENABLED = "crossfade_enabled"
        const val KEY_CROSSFADE_DURATION = "crossfade_duration"
        const val KEY_GAPLESS_PLAYBACK = "gapless_playback"
        const val KEY_VOLUME_NORMALIZATION = "volume_normalization"
        const val KEY_TARGET_LUFS = "target_lufs"
        
        // Video Keys
        const val KEY_DEFAULT_QUALITY = "default_quality"
        const val KEY_HARDWARE_DECODING = "hardware_decoding"
        const val KEY_SUBTITLE_LANGUAGE = "subtitle_language"
        const val KEY_SUBTITLE_SIZE = "subtitle_size"
        const val KEY_SHOW_SUBTITLES = "show_subtitles"
        const val KEY_ASPECT_RATIO = "aspect_ratio"
        const val KEY_AUTO_ROTATE = "auto_rotate"
        const val KEY_PIP_ENABLED = "pip_enabled"
        const val KEY_AUTO_PIP = "auto_pip"
        
        // Network Keys
        const val KEY_WIFI_QUALITY = "wifi_quality"
        const val KEY_MOBILE_QUALITY = "mobile_quality"
        const val KEY_RESTRICT_HD_MOBILE = "restrict_hd_mobile"
        const val KEY_CACHE_SIZE = "cache_size"
        const val KEY_PROXY_ENABLED = "proxy_enabled"
        const val KEY_PROXY_ADDRESS = "proxy_address"
        const val KEY_PROXY_PORT = "proxy_port"
        
        // Privacy Keys
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_LOCK_METHOD = "lock_method"
        const val KEY_HIDE_FOLDERS = "hide_folders"
        const val KEY_REMEMBER_POSITION = "remember_position"
        const val KEY_SEARCH_HISTORY = "search_history"
        const val KEY_ANALYTICS = "analytics"
        
        // Gesture Keys
        const val KEY_SWIPE_BRIGHTNESS = "swipe_brightness"
        const val KEY_SWIPE_VOLUME = "swipe_volume"
        const val KEY_SWIPE_SENSITIVITY = "swipe_sensitivity"
        const val KEY_DOUBLE_TAP_SEEK = "double_tap_seek"
        const val KEY_DOUBLE_TAP_DURATION = "double_tap_duration"
        const val KEY_PINCH_ZOOM = "pinch_zoom"
        
        // UI Keys
        const val KEY_SHOW_NAV_LABELS = "show_nav_labels"
        const val KEY_DEFAULT_TAB = "default_tab"
        const val KEY_LIST_STYLE = "list_style"
        const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_SHOW_VISUALIZER = "show_visualizer"
        const val KEY_VISUALIZER_MODE = "visualizer_mode"
        const val KEY_SHOW_LYRICS = "show_lyrics"
        
        // Notification Keys
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIF_ALBUM_ART = "notif_album_art"
        const val KEY_NOTIF_CONTROLS = "notif_controls"
        const val KEY_LOCK_SCREEN_VISIBILITY = "lock_screen_visibility"
        const val KEY_NOTIF_VIBRATE = "notif_vibrate"
        
        // Developer Keys
        const val KEY_DEBUG_MODE = "debug_mode"
        const val KEY_PERFORMANCE_OVERLAY = "performance_overlay"
        const val KEY_LOG_PLAYBACK = "log_playback"
        const val KEY_NEW_RENDERER = "new_renderer"
        const val KEY_THREAD_COUNT = "thread_count"
        const val KEY_BUFFER_SIZE = "buffer_size"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _settingsState = MutableStateFlow(loadAllSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()
    
    private fun loadAllSettings(): AppSettings {
        return AppSettings(
            appearance = AppearanceSettings(
                themeMode = prefs.getInt(KEY_THEME_MODE, 0),
                pureBlack = prefs.getBoolean(KEY_PURE_BLACK, false),
                accentColor = prefs.getString(KEY_ACCENT_COLOR, "#6C63FF") ?: "#6C63FF",
                playerStyle = prefs.getString(KEY_PLAYER_STYLE, "glassmorphic") ?: "glassmorphic"
            ),
            audio = AudioSettings(
                skipSilence = prefs.getBoolean(KEY_SKIP_SILENCE, false),
                audioBoost = prefs.getBoolean(KEY_AUDIO_BOOST, false),
                audioBoostLevel = prefs.getInt(KEY_AUDIO_BOOST_LEVEL, 100),
                autoPlaySimilar = prefs.getBoolean(KEY_AUTOPLAY_SIMILAR, true),
                crossfadeEnabled = prefs.getBoolean(KEY_CROSSFADE_ENABLED, false),
                crossfadeDuration = prefs.getInt(KEY_CROSSFADE_DURATION, 3),
                gaplessPlayback = prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true),
                volumeNormalization = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false),
                targetLufs = prefs.getInt(KEY_TARGET_LUFS, -14)
            ),
            video = VideoSettings(
                defaultQuality = prefs.getString(KEY_DEFAULT_QUALITY, "auto") ?: "auto",
                hardwareDecoding = prefs.getBoolean(KEY_HARDWARE_DECODING, true),
                subtitleLanguage = prefs.getString(KEY_SUBTITLE_LANGUAGE, "en") ?: "en",
                subtitleSize = prefs.getString(KEY_SUBTITLE_SIZE, "medium") ?: "medium",
                showSubtitles = prefs.getBoolean(KEY_SHOW_SUBTITLES, true),
                aspectRatio = prefs.getString(KEY_ASPECT_RATIO, "fit") ?: "fit",
                autoRotate = prefs.getBoolean(KEY_AUTO_ROTATE, true),
                pipEnabled = prefs.getBoolean(KEY_PIP_ENABLED, true),
                autoPip = prefs.getBoolean(KEY_AUTO_PIP, true)
            ),
            network = NetworkSettings(
                wifiQuality = prefs.getString(KEY_WIFI_QUALITY, "1080p") ?: "1080p",
                mobileQuality = prefs.getString(KEY_MOBILE_QUALITY, "720p") ?: "720p",
                restrictHdMobile = prefs.getBoolean(KEY_RESTRICT_HD_MOBILE, true),
                cacheSize = prefs.getInt(KEY_CACHE_SIZE, 500),
                proxyEnabled = prefs.getBoolean(KEY_PROXY_ENABLED, false),
                proxyAddress = prefs.getString(KEY_PROXY_ADDRESS, "") ?: "",
                proxyPort = prefs.getInt(KEY_PROXY_PORT, 8080)
            ),
            privacy = PrivacySettings(
                appLockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false),
                lockMethod = prefs.getString(KEY_LOCK_METHOD, "fingerprint") ?: "fingerprint",
                hideFolders = prefs.getBoolean(KEY_HIDE_FOLDERS, false),
                rememberPosition = prefs.getBoolean(KEY_REMEMBER_POSITION, true),
                searchHistory = prefs.getBoolean(KEY_SEARCH_HISTORY, true),
                analytics = prefs.getBoolean(KEY_ANALYTICS, false)
            ),
            gestures = GestureSettings(
                swipeBrightness = prefs.getBoolean(KEY_SWIPE_BRIGHTNESS, true),
                swipeVolume = prefs.getBoolean(KEY_SWIPE_VOLUME, true),
                swipeSensitivity = prefs.getInt(KEY_SWIPE_SENSITIVITY, 50),
                doubleTapSeek = prefs.getBoolean(KEY_DOUBLE_TAP_SEEK, true),
                doubleTapDuration = prefs.getInt(KEY_DOUBLE_TAP_DURATION, 10),
                pinchZoom = prefs.getBoolean(KEY_PINCH_ZOOM, true)
            ),
            ui = UISettings(
                showNavLabels = prefs.getBoolean(KEY_SHOW_NAV_LABELS, true),
                defaultTab = prefs.getInt(KEY_DEFAULT_TAB, 0),
                listStyle = prefs.getString(KEY_LIST_STYLE, "list") ?: "list",
                showThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true),
                gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 2),
                showVisualizer = prefs.getBoolean(KEY_SHOW_VISUALIZER, true),
                visualizerMode = prefs.getString(KEY_VISUALIZER_MODE, "bars") ?: "bars",
                showLyrics = prefs.getBoolean(KEY_SHOW_LYRICS, true)
            ),
            notifications = NotificationSettings(
                enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
                albumArt = prefs.getBoolean(KEY_NOTIF_ALBUM_ART, true),
                controls = prefs.getBoolean(KEY_NOTIF_CONTROLS, true),
                lockScreenVisibility = prefs.getString(KEY_LOCK_SCREEN_VISIBILITY, "all") ?: "all",
                vibrate = prefs.getBoolean(KEY_NOTIF_VIBRATE, true)
            ),
            developer = DeveloperSettings(
                debugMode = prefs.getBoolean(KEY_DEBUG_MODE, false),
                performanceOverlay = prefs.getBoolean(KEY_PERFORMANCE_OVERLAY, false),
                logPlayback = prefs.getBoolean(KEY_LOG_PLAYBACK, false),
                newRenderer = prefs.getBoolean(KEY_NEW_RENDERER, false),
                threadCount = prefs.getInt(KEY_THREAD_COUNT, 4),
                bufferSize = prefs.getInt(KEY_BUFFER_SIZE, 50)
            )
        )
    }
    
    // Appearance
    fun setThemeMode(mode: Int) = update { putInt(KEY_THEME_MODE, mode) }
    fun setPureBlack(enabled: Boolean) = update { putBoolean(KEY_PURE_BLACK, enabled) }
    fun setAccentColor(color: String) = update { putString(KEY_ACCENT_COLOR, color) }
    fun setPlayerStyle(style: String) = update { putString(KEY_PLAYER_STYLE, style) }
    
    // Audio
    fun setSkipSilence(enabled: Boolean) = update { putBoolean(KEY_SKIP_SILENCE, enabled) }
    fun setAudioBoost(enabled: Boolean, level: Int = 100) = update { 
        putBoolean(KEY_AUDIO_BOOST, enabled)
        putInt(KEY_AUDIO_BOOST_LEVEL, level)
    }
    fun setAutoPlaySimilar(enabled: Boolean) = update { putBoolean(KEY_AUTOPLAY_SIMILAR, enabled) }
    fun setCrossfade(enabled: Boolean, duration: Int = 3) = update {
        putBoolean(KEY_CROSSFADE_ENABLED, enabled)
        putInt(KEY_CROSSFADE_DURATION, duration)
    }
    fun setGaplessPlayback(enabled: Boolean) = update { putBoolean(KEY_GAPLESS_PLAYBACK, enabled) }
    fun setVolumeNormalization(enabled: Boolean, targetLufs: Int = -14) = update {
        putBoolean(KEY_VOLUME_NORMALIZATION, enabled)
        putInt(KEY_TARGET_LUFS, targetLufs)
    }
    
    // Video
    fun setDefaultQuality(quality: String) = update { putString(KEY_DEFAULT_QUALITY, quality) }
    fun setHardwareDecoding(enabled: Boolean) = update { putBoolean(KEY_HARDWARE_DECODING, enabled) }
    fun setSubtitleSettings(language: String, size: String) = update {
        putString(KEY_SUBTITLE_LANGUAGE, language)
        putString(KEY_SUBTITLE_SIZE, size)
    }
    fun setShowSubtitles(enabled: Boolean) = update { putBoolean(KEY_SHOW_SUBTITLES, enabled) }
    fun setAspectRatio(ratio: String) = update { putString(KEY_ASPECT_RATIO, ratio) }
    fun setAutoRotate(enabled: Boolean) = update { putBoolean(KEY_AUTO_ROTATE, enabled) }
    fun setPipSettings(enabled: Boolean, autoPip: Boolean) = update {
        putBoolean(KEY_PIP_ENABLED, enabled)
        putBoolean(KEY_AUTO_PIP, autoPip)
    }
    
    // Network
    fun setStreamingQuality(wifi: String, mobile: String) = update {
        putString(KEY_WIFI_QUALITY, wifi)
        putString(KEY_MOBILE_QUALITY, mobile)
    }
    fun setRestrictHdMobile(enabled: Boolean) = update { putBoolean(KEY_RESTRICT_HD_MOBILE, enabled) }
    fun setCacheSize(sizeMb: Int) = update { putInt(KEY_CACHE_SIZE, sizeMb) }
    fun setProxy(enabled: Boolean, address: String, port: Int) = update {
        putBoolean(KEY_PROXY_ENABLED, enabled)
        putString(KEY_PROXY_ADDRESS, address)
        putInt(KEY_PROXY_PORT, port)
    }
    
    // Privacy
    fun setAppLock(enabled: Boolean, method: String) = update {
        putBoolean(KEY_APP_LOCK_ENABLED, enabled)
        putString(KEY_LOCK_METHOD, method)
    }
    fun setHideFolders(enabled: Boolean) = update { putBoolean(KEY_HIDE_FOLDERS, enabled) }
    fun setRememberPosition(enabled: Boolean) = update { putBoolean(KEY_REMEMBER_POSITION, enabled) }
    fun setSearchHistory(enabled: Boolean) = update { putBoolean(KEY_SEARCH_HISTORY, enabled) }
    fun setAnalytics(enabled: Boolean) = update { putBoolean(KEY_ANALYTICS, enabled) }
    
    // Gestures
    fun setSwipeGestures(brightness: Boolean, volume: Boolean, sensitivity: Int) = update {
        putBoolean(KEY_SWIPE_BRIGHTNESS, brightness)
        putBoolean(KEY_SWIPE_VOLUME, volume)
        putInt(KEY_SWIPE_SENSITIVITY, sensitivity)
    }
    fun setDoubleTapSeek(enabled: Boolean, duration: Int) = update {
        putBoolean(KEY_DOUBLE_TAP_SEEK, enabled)
        putInt(KEY_DOUBLE_TAP_DURATION, duration)
    }
    fun setPinchZoom(enabled: Boolean) = update { putBoolean(KEY_PINCH_ZOOM, enabled) }
    
    // UI
    fun setShowNavLabels(enabled: Boolean) = update { putBoolean(KEY_SHOW_NAV_LABELS, enabled) }
    fun setDefaultTab(tab: Int) = update { putInt(KEY_DEFAULT_TAB, tab) }
    fun setListStyle(style: String) = update { putString(KEY_LIST_STYLE, style) }
    fun setShowThumbnails(enabled: Boolean) = update { putBoolean(KEY_SHOW_THUMBNAILS, enabled) }
    fun setGridColumns(columns: Int) = update { putInt(KEY_GRID_COLUMNS, columns) }
    fun setVisualizer(enabled: Boolean, mode: String) = update {
        putBoolean(KEY_SHOW_VISUALIZER, enabled)
        putString(KEY_VISUALIZER_MODE, mode)
    }
    fun setShowLyrics(enabled: Boolean) = update { putBoolean(KEY_SHOW_LYRICS, enabled) }
    
    // Notifications
    fun setNotificationsEnabled(enabled: Boolean) = update { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    fun setNotifStyle(albumArt: Boolean, controls: Boolean, lockScreen: String) = update {
        putBoolean(KEY_NOTIF_ALBUM_ART, albumArt)
        putBoolean(KEY_NOTIF_CONTROLS, controls)
        putString(KEY_LOCK_SCREEN_VISIBILITY, lockScreen)
    }
    fun setNotifVibrate(enabled: Boolean) = update { putBoolean(KEY_NOTIF_VIBRATE, enabled) }
    
    // Developer
    fun setDebugMode(enabled: Boolean) = update { putBoolean(KEY_DEBUG_MODE, enabled) }
    fun setPerformanceOverlay(enabled: Boolean) = update { putBoolean(KEY_PERFORMANCE_OVERLAY, enabled) }
    fun setLogPlayback(enabled: Boolean) = update { putBoolean(KEY_LOG_PLAYBACK, enabled) }
    fun setNewRenderer(enabled: Boolean) = update { putBoolean(KEY_NEW_RENDERER, enabled) }
    fun setAdvancedPlayer(threads: Int, buffer: Int) = update {
        putInt(KEY_THREAD_COUNT, threads)
        putInt(KEY_BUFFER_SIZE, buffer)
    }
    
    // Utilities
    private inline fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit(block)
        _settingsState.value = loadAllSettings()
    }
    
    fun resetAllSettings() {
        prefs.edit { clear() }
        _settingsState.value = loadAllSettings()
    }
    
    fun exportSettings(): Map<String, *> = prefs.all
    
    fun importSettings(settings: Map<String, Any>) {
        prefs.edit {
            settings.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is Float -> putFloat(key, value)
                }
            }
        }
        _settingsState.value = loadAllSettings()
    }
}

// Data classes
data class AppSettings(
    val appearance: AppearanceSettings,
    val audio: AudioSettings,
    val video: VideoSettings,
    val network: NetworkSettings,
    val privacy: PrivacySettings,
    val gestures: GestureSettings,
    val ui: UISettings,
    val notifications: NotificationSettings,
    val developer: DeveloperSettings
)

data class AppearanceSettings(
    val themeMode: Int,
    val pureBlack: Boolean,
    val accentColor: String,
    val playerStyle: String
)

data class AudioSettings(
    val skipSilence: Boolean,
    val audioBoost: Boolean,
    val audioBoostLevel: Int,
    val autoPlaySimilar: Boolean,
    val crossfadeEnabled: Boolean,
    val crossfadeDuration: Int,
    val gaplessPlayback: Boolean,
    val volumeNormalization: Boolean,
    val targetLufs: Int
)

data class VideoSettings(
    val defaultQuality: String,
    val hardwareDecoding: Boolean,
    val subtitleLanguage: String,
    val subtitleSize: String,
    val showSubtitles: Boolean,
    val aspectRatio: String,
    val autoRotate: Boolean,
    val pipEnabled: Boolean,
    val autoPip: Boolean
)

data class NetworkSettings(
    val wifiQuality: String,
    val mobileQuality: String,
    val restrictHdMobile: Boolean,
    val cacheSize: Int,
    val proxyEnabled: Boolean,
    val proxyAddress: String,
    val proxyPort: Int
)

data class PrivacySettings(
    val appLockEnabled: Boolean,
    val lockMethod: String,
    val hideFolders: Boolean,
    val rememberPosition: Boolean,
    val searchHistory: Boolean,
    val analytics: Boolean
)

data class GestureSettings(
    val swipeBrightness: Boolean,
    val swipeVolume: Boolean,
    val swipeSensitivity: Int,
    val doubleTapSeek: Boolean,
    val doubleTapDuration: Int,
    val pinchZoom: Boolean
)

data class UISettings(
    val showNavLabels: Boolean,
    val defaultTab: Int,
    val listStyle: String,
    val showThumbnails: Boolean,
    val gridColumns: Int,
    val showVisualizer: Boolean,
    val visualizerMode: String,
    val showLyrics: Boolean
)

data class NotificationSettings(
    val enabled: Boolean,
    val albumArt: Boolean,
    val controls: Boolean,
    val lockScreenVisibility: String,
    val vibrate: Boolean
)

data class DeveloperSettings(
    val debugMode: Boolean,
    val performanceOverlay: Boolean,
    val logPlayback: Boolean,
    val newRenderer: Boolean,
    val threadCount: Int,
    val bufferSize: Int
)
