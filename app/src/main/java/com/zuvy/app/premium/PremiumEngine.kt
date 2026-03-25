package com.zuvy.app.premium

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.premiumDataStore by preferencesDataStore(name = "premium_settings")

/**
 * PremiumEngine - Core engine for managing premium subscription status
 * 
 * Features:
 * - Subscription status management (Monthly, Yearly, Lifetime)
 * - Premium features access control
 * - Subscription expiration tracking
 * - Premium benefits management
 */
@Singleton
class PremiumEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Subscription Plans
        enum class SubscriptionPlan(val id: String, val price: Double, val durationMonths: Int, val displayName: String) {
            MONTHLY("zuvy_premium_monthly", 1.99, 1, "Monthly"),
            YEARLY("zuvy_premium_yearly", 9.99, 12, "Yearly"),
            LIFETIME("zuvy_premium_lifetime", 24.99, 30, "Lifetime") // 2.5 years = 30 months
        }

        // Premium Features
        enum class PremiumFeature(val id: String, val displayName: String) {
            NO_ADS("no_ads", "Ad-Free Experience"),
            EQUALIZER("equalizer", "Advanced Equalizer"),
            AUDIO_BOOST("audio_boost", "Audio Boost up to 200%"),
            CROSSFADE("crossfade", "Crossfade & Gapless Playback"),
            VISUALIZER("visualizer", "Audio Visualizer"),
            LYRICS("lyrics", "Synced Lyrics"),
            SLEEP_TIMER("sleep_timer", "Advanced Sleep Timer"),
            THEME("theme", "Premium Themes"),
            BACKUP("backup", "Cloud Backup & Restore"),
            RINGTONE("ringtone", "Ringtone Maker"),
            LYRICS_EDITOR("lyrics_editor", "Lyrics Editor"),
            VIDEO_FILTERS("video_filters", "Video Filters"),
            GIF_RECORDER("gif_recorder", "GIF Recorder"),
            A_B_REPEAT("ab_repeat", "A-B Repeat"),
            HIGH_RES("high_res", "High-Resolution Audio"),
            CASTING("casting", "Chromecast Support")
        }

        // DataStore Keys
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
        private val SUBSCRIPTION_PLAN_KEY = stringPreferencesKey("subscription_plan")
        private val EXPIRATION_TIME_KEY = longPreferencesKey("expiration_time")
        private val PURCHASE_TIME_KEY = longPreferencesKey("purchase_time")
        private val PURCHASE_TOKEN_KEY = stringPreferencesKey("purchase_token")
        private val TRIAL_USED_KEY = booleanPreferencesKey("trial_used")
    }

    // Premium status flow
    val isPremium: Flow<Boolean> = context.premiumDataStore.data
        .map { preferences ->
            val isPremium = preferences[IS_PREMIUM_KEY] ?: false
            val expirationTime = preferences[EXPIRATION_TIME_KEY] ?: 0L
            
            if (isPremium && expirationTime > 0) {
                // Check if subscription is still valid
                System.currentTimeMillis() < expirationTime
            } else {
                isPremium
            }
        }

    // Current subscription plan
    val currentPlan: Flow<SubscriptionPlan?> = context.premiumDataStore.data
        .map { preferences ->
            val planId = preferences[SUBSCRIPTION_PLAN_KEY]
            SubscriptionPlan.entries.find { it.id == planId }
        }

    // Expiration time
    val expirationTime: Flow<Long> = context.premiumDataStore.data
        .map { preferences ->
            preferences[EXPIRATION_TIME_KEY] ?: 0L
        }

    // Days remaining
    val daysRemaining: Flow<Int> = context.premiumDataStore.data
        .map { preferences ->
            val expiration = preferences[EXPIRATION_TIME_KEY] ?: 0L
            val now = System.currentTimeMillis()
            if (expiration > now) {
                ((expiration - now) / (1000 * 60 * 60 * 24)).toInt()
            } else {
                0
            }
        }

    // Trial status
    val isTrialUsed: Flow<Boolean> = context.premiumDataStore.data
        .map { preferences ->
            preferences[TRIAL_USED_KEY] ?: false
        }

    /**
     * Activate premium subscription
     */
    suspend fun activatePremium(
        plan: SubscriptionPlan,
        purchaseToken: String? = null
    ) {
        val calendar = Calendar.getInstance()
        val purchaseTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, plan.durationMonths)
        val expirationTime = calendar.timeInMillis

        context.premiumDataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = true
            preferences[SUBSCRIPTION_PLAN_KEY] = plan.id
            preferences[EXPIRATION_TIME_KEY] = if (plan == SubscriptionPlan.LIFETIME) {
                Long.MAX_VALUE // Lifetime never expires
            } else {
                expirationTime
            }
            preferences[PURCHASE_TIME_KEY] = purchaseTime
            purchaseToken?.let { preferences[PURCHASE_TOKEN_KEY] = it }
        }
    }

    /**
     * Deactivate premium subscription
     */
    suspend fun deactivatePremium() {
        context.premiumDataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = false
            preferences[SUBSCRIPTION_PLAN_KEY] = ""
            preferences[EXPIRATION_TIME_KEY] = 0L
            preferences[PURCHASE_TIME_KEY] = 0L
            preferences[PURCHASE_TOKEN_KEY] = ""
        }
    }

    /**
     * Mark trial as used
     */
    suspend fun markTrialUsed() {
        context.premiumDataStore.edit { preferences ->
            preferences[TRIAL_USED_KEY] = true
        }
    }

    /**
     * Check if a specific premium feature is available
     */
    fun isFeatureAvailable(feature: PremiumFeature): Boolean {
        // For now, all premium features require premium subscription
        // In future, some features could be available for free users
        return true // Will be checked with isPremium flow in UI
    }

    /**
     * Get subscription status info
     */
    fun getSubscriptionInfo(): Flow<SubscriptionInfo> {
        return context.premiumDataStore.data.map { preferences ->
            val isPremium = preferences[IS_PREMIUM_KEY] ?: false
            val planId = preferences[SUBSCRIPTION_PLAN_KEY]
            val expiration = preferences[EXPIRATION_TIME_KEY] ?: 0L
            val purchaseTime = preferences[PURCHASE_TIME_KEY] ?: 0L
            
            SubscriptionInfo(
                isPremium = isPremium && (expiration > System.currentTimeMillis() || expiration == Long.MAX_VALUE),
                plan = SubscriptionPlan.entries.find { it.id == planId },
                expirationTime = expiration,
                purchaseTime = purchaseTime,
                isActive = if (expiration == Long.MAX_VALUE) true else expiration > System.currentTimeMillis()
            )
        }
    }

    /**
     * Calculate savings percentage compared to monthly
     */
    fun calculateSavings(plan: SubscriptionPlan): Int {
        return when (plan) {
            SubscriptionPlan.MONTHLY -> 0
            SubscriptionPlan.YEARLY -> {
                val monthlyCost = SubscriptionPlan.MONTHLY.price * 12
                val yearlyCost = plan.price
                ((monthlyCost - yearlyCost) / monthlyCost * 100).toInt()
            }
            SubscriptionPlan.LIFETIME -> {
                val monthlyCost = SubscriptionPlan.MONTHLY.price * 30 // 2.5 years
                val lifetimeCost = plan.price
                ((monthlyCost - lifetimeCost) / monthlyCost * 100).toInt()
            }
        }
    }

    /**
     * Get formatted price
     */
    fun getFormattedPrice(plan: SubscriptionPlan): String {
        return when (plan) {
            SubscriptionPlan.MONTHLY -> "$${plan.price}/month"
            SubscriptionPlan.YEARLY -> "$${plan.price}/year"
            SubscriptionPlan.LIFETIME -> "$${plan.price} one-time"
        }
    }

    /**
     * Get price per month
     */
    fun getPricePerMonth(plan: SubscriptionPlan): Double {
        return plan.price / plan.durationMonths
    }

    data class SubscriptionInfo(
        val isPremium: Boolean,
        val plan: SubscriptionPlan?,
        val expirationTime: Long,
        val purchaseTime: Long,
        val isActive: Boolean
    ) {
        val daysRemaining: Int
            get() {
                if (expirationTime == Long.MAX_VALUE) return Int.MAX_VALUE
                val now = System.currentTimeMillis()
                return if (expirationTime > now) {
                    ((expirationTime - now) / (1000 * 60 * 60 * 24)).toInt()
                } else {
                    0
                }
            }
    }
}
