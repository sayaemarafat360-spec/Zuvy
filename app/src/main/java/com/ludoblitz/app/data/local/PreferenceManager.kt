package com.ludoblitz.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ludoblitz.app.data.model.GameRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ludo_blitz_prefs")

/**
 * Manages user preferences and local data storage using DataStore
 */
@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Keys
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val USER_ID = stringPreferencesKey("user_id")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val LAST_DAILY_REWARD = longPreferencesKey("last_daily_reward")
        val DAILY_REWARD_DAY = intPreferencesKey("daily_reward_day")
        val TOTAL_GAMES_PLAYED = intPreferencesKey("total_games_played")
        val TOTAL_WINS = intPreferencesKey("total_wins")
        val COINS = longPreferencesKey("coins")
        val GEMS = longPreferencesKey("gems")
        val XP = longPreferencesKey("xp")
        val LEVEL = intPreferencesKey("level")
        val SELECTED_BOARD = stringPreferencesKey("selected_board")
        val SELECTED_TOKEN = stringPreferencesKey("selected_token")
        val SELECTED_DICE = stringPreferencesKey("selected_dice")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val REQUIRE_SIX_TO_RELEASE = booleanPreferencesKey("require_six_to_release")
        val THREE_SIXES_RULE = booleanPreferencesKey("three_sixes_rule")
        val LAST_AD_SHOWN = longPreferencesKey("last_ad_shown")
        val ADS_WATCHED_TODAY = intPreferencesKey("ads_watched_today")
        val ADS_RESET_DATE = longPreferencesKey("ads_reset_date")
        val PREMIUM_USER = booleanPreferencesKey("premium_user")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
    }

    // Theme Preferences
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DARK_MODE] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    // Sound Preferences
    val isSoundEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SOUND_ENABLED] ?: true }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    fun isSoundEnabled(): Boolean {
        var enabled = true
        context.dataStore.data.map { preferences ->
            enabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true
        }
        return enabled
    }

    // Music Preferences
    val isMusicEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.MUSIC_ENABLED] ?: true }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_ENABLED] = enabled
        }
    }

    // Vibration Preferences
    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    // Notifications Preferences
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // Language
    val language: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LANGUAGE] ?: "en" }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = lang
        }
    }

    // User ID
    val userId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_ID] }

    suspend fun setUserId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = id
        }
    }

    // Guest Mode
    val isGuestMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GUEST_MODE] ?: false }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GUEST_MODE] = isGuest
        }
    }

    // Daily Reward
    val lastDailyReward: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_DAILY_REWARD] ?: 0L }

    suspend fun setLastDailyReward(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DAILY_REWARD] = timestamp
        }
    }

    val dailyRewardDay: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DAILY_REWARD_DAY] ?: 0 }

    suspend fun setDailyRewardDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REWARD_DAY] = day
        }
    }

    // Game Stats
    val totalGamesPlayed: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.TOTAL_GAMES_PLAYED] ?: 0 }

    suspend fun setTotalGamesPlayed(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_GAMES_PLAYED] = count
        }
    }

    val totalWins: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.TOTAL_WINS] ?: 0 }

    suspend fun setTotalWins(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_WINS] = count
        }
    }

    // Currency
    val coins: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.COINS] ?: 1000L }

    suspend fun setCoins(amount: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COINS] = amount
        }
    }

    val gems: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GEMS] ?: 0L }

    suspend fun setGems(amount: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMS] = amount
        }
    }

    val xp: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.XP] ?: 0L }

    suspend fun setXp(amount: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.XP] = amount
        }
    }

    val level: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LEVEL] ?: 1 }

    suspend fun setLevel(lvl: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEVEL] = lvl
        }
    }

    // Selected Cosmetics
    val selectedBoard: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SELECTED_BOARD] ?: "classic" }

    suspend fun setSelectedBoard(board: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_BOARD] = board
        }
    }

    val selectedToken: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SELECTED_TOKEN] ?: "default" }

    suspend fun setSelectedToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_TOKEN] = token
        }
    }

    val selectedDice: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SELECTED_DICE] ?: "default" }

    suspend fun setSelectedDice(dice: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_DICE] = dice
        }
    }

    // Tutorial
    val isTutorialCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.TUTORIAL_COMPLETED] ?: false }

    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TUTORIAL_COMPLETED] = completed
        }
    }

    // Game Rules
    val gameRules: Flow<GameRules> = context.dataStore.data
        .map { preferences ->
            GameRules(
                requireSixToRelease = preferences[PreferencesKeys.REQUIRE_SIX_TO_RELEASE] ?: true,
                threeSixesRule = preferences[PreferencesKeys.THREE_SIXES_RULE] ?: true
            )
        }

    suspend fun setGameRules(rules: GameRules) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REQUIRE_SIX_TO_RELEASE] = rules.requireSixToRelease
            preferences[PreferencesKeys.THREE_SIXES_RULE] = rules.threeSixesRule
        }
    }

    // Ads
    val lastAdShown: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_AD_SHOWN] ?: 0L }

    suspend fun setLastAdShown(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_AD_SHOWN] = timestamp
        }
    }

    // Premium
    val isPremiumUser: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.PREMIUM_USER] ?: false }

    suspend fun setPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREMIUM_USER] = isPremium
        }
    }

    // Remember Me
    val rememberMe: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.REMEMBER_ME] ?: false }

    suspend fun setRememberMe(remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] = remember
        }
    }

    val savedEmail: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SAVED_EMAIL] ?: "" }

    suspend fun setSavedEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVED_EMAIL] = email
        }
    }

    // Clear all preferences
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
