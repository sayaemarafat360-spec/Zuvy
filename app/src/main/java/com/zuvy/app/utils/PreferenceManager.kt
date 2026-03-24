package com.zuvy.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zuvy_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode")
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val LAUNCH_COUNT = intPreferencesKey("launch_count")
        private val HAS_RATED = booleanPreferencesKey("has_rated")
        private val LAST_SCAN = longPreferencesKey("last_scan")
        private val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val SEEK_DURATION = intPreferencesKey("seek_duration")
        private val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        private val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        private val DOUBLE_TAP_SEEK = booleanPreferencesKey("double_tap_seek")
        private val APP_LOCK = booleanPreferencesKey("app_lock")
        private val FINGERPRINT_LOCK = booleanPreferencesKey("fingerprint_lock")
        private val PIN_CODE = stringPreferencesKey("pin_code")
    }

    // Theme
    fun getThemeMode(): Int = runBlocking {
        context.dataStore.data.map { it[THEME_MODE] ?: 1 }.first()
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    // Accent Color
    fun getAccentColor(): String = runBlocking {
        context.dataStore.data.map { it[ACCENT_COLOR] ?: "#6C63FF" }.first()
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[ACCENT_COLOR] = color }
    }

    // Launch Count
    fun getLaunchCount(): Int = runBlocking {
        context.dataStore.data.map { it[LAUNCH_COUNT] ?: 0 }.first()
    }

    fun incrementLaunchCount() = runBlocking {
        context.dataStore.edit { prefs ->
            prefs[LAUNCH_COUNT] = (prefs[LAUNCH_COUNT] ?: 0) + 1
        }
    }

    // Rating
    fun hasRatedApp(): Boolean = runBlocking {
        context.dataStore.data.map { it[HAS_RATED] ?: false }.first()
    }

    fun setRatedApp(rated: Boolean) = runBlocking {
        context.dataStore.edit { it[HAS_RATED] = rated }
    }

    // Last Scan
    fun getLastScan(): Long = runBlocking {
        context.dataStore.data.map { it[LAST_SCAN] ?: 0L }.first()
    }

    suspend fun setLastScan(time: Long) {
        context.dataStore.edit { it[LAST_SCAN] = time }
    }

    // Playback Speed
    fun getPlaybackSpeed(): Float = runBlocking {
        context.dataStore.data.map { it[PLAYBACK_SPEED] ?: 1.0f }.first()
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    // Seek Duration (seconds)
    fun getSeekDuration(): Int = runBlocking {
        context.dataStore.data.map { it[SEEK_DURATION] ?: 10 }.first()
    }

    suspend fun setSeekDuration(duration: Int) {
        context.dataStore.edit { it[SEEK_DURATION] = duration }
    }

    // Gesture Settings
    fun isGestureBrightnessEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[GESTURE_BRIGHTNESS] ?: true }.first()
    }

    fun isGestureVolumeEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[GESTURE_VOLUME] ?: true }.first()
    }

    fun isDoubleTapSeekEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[DOUBLE_TAP_SEEK] ?: true }.first()
    }

    // App Lock
    fun isAppLockEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[APP_LOCK] ?: false }.first()
    }

    suspend fun setAppLock(enabled: Boolean) {
        context.dataStore.edit { it[APP_LOCK] = enabled }
    }

    fun isFingerprintLockEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[FINGERPRINT_LOCK] ?: false }.first()
    }

    suspend fun setFingerprintLock(enabled: Boolean) {
        context.dataStore.edit { it[FINGERPRINT_LOCK] = enabled }
    }

    fun getPinCode(): String? = runBlocking {
        context.dataStore.data.map { it[PIN_CODE] }.first()
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { it[PIN_CODE] = pin }
    }
}
