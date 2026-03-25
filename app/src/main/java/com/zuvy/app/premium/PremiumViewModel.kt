package com.zuvy.app.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PremiumViewModel - ViewModel for Premium screen
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumEngine: PremiumEngine,
    private val adManager: AdManager
) : ViewModel() {

    // Premium status
    val isPremium = premiumEngine.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Current subscription plan
    val currentPlan = premiumEngine.currentPlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Days remaining
    val daysRemaining = premiumEngine.daysRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Subscription info
    val subscriptionInfo = premiumEngine.getSubscriptionInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI State
    private val _selectedPlan = MutableStateFlow(PremiumEngine.SubscriptionPlan.YEARLY)
    val selectedPlan = _selectedPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showPurchaseSuccess = MutableStateFlow(false)
    val showPurchaseSuccess = _showPurchaseSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Available plans
    val plans = PremiumEngine.SubscriptionPlan.entries

    // Premium features list
    val premiumFeatures = listOf(
        PremiumEngine.PremiumFeature.NO_ADS,
        PremiumEngine.PremiumFeature.EQUALIZER,
        PremiumEngine.PremiumFeature.AUDIO_BOOST,
        PremiumEngine.PremiumFeature.CROSSFADE,
        PremiumEngine.PremiumFeature.VISUALIZER,
        PremiumEngine.PremiumFeature.LYRICS,
        PremiumEngine.PremiumFeature.SLEEP_TIMER,
        PremiumEngine.PremiumFeature.THEME,
        PremiumEngine.PremiumFeature.BACKUP,
        PremiumEngine.PremiumFeature.RINGTONE,
        PremiumEngine.PremiumFeature.LYRICS_EDITOR,
        PremiumEngine.PremiumFeature.VIDEO_FILTERS,
        PremiumEngine.PremiumFeature.GIF_RECORDER,
        PremiumEngine.PremiumFeature.A_B_REPEAT
    )

    /**
     * Select a subscription plan
     */
    fun selectPlan(plan: PremiumEngine.SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    /**
     * Get savings percentage for plan
     */
    fun getSavings(plan: PremiumEngine.SubscriptionPlan): Int {
        return premiumEngine.calculateSavings(plan)
    }

    /**
     * Get formatted price
     */
    fun getFormattedPrice(plan: PremiumEngine.SubscriptionPlan): String {
        return premiumEngine.getFormattedPrice(plan)
    }

    /**
     * Get price per month
     */
    fun getPricePerMonth(plan: PremiumEngine.SubscriptionPlan): String {
        val price = premiumEngine.getPricePerMonth(plan)
        return String.format("$%.2f/month", price)
    }

    /**
     * Start purchase flow
     * Note: Actual billing will be implemented later
     */
    fun startPurchase(activity: android.app.Activity? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // For now, simulate purchase success
                // TODO: Implement actual billing with Google Play Billing
                
                // Simulate network delay
                kotlinx.coroutines.delay(1500)
                
                // Activate premium (simulated)
                premiumEngine.activatePremium(_selectedPlan.value)
                
                _showPurchaseSuccess.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Purchase failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Simulate premium activation for testing
     */
    fun simulatePremiumActivation() {
        viewModelScope.launch {
            premiumEngine.activatePremium(_selectedPlan.value)
            _showPurchaseSuccess.value = true
        }
    }

    /**
     * Restore purchases
     */
    fun restorePurchases() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Implement actual purchase restoration
                kotlinx.coroutines.delay(1000)
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Restore failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Dismiss success dialog
     */
    fun dismissSuccessDialog() {
        _showPurchaseSuccess.value = false
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Watch rewarded ad for temporary premium
     */
    fun watchRewardedAd(activity: android.app.Activity) {
        adManager.showRewardedAd(
            activity = activity,
            onRewarded = {
                // Could grant temporary premium benefits here
            }
        )
    }
}
