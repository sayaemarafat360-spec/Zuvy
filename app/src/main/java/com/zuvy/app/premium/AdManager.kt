package com.zuvy.app.premium

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager - Centralized AdMob ad management
 * 
 * Features:
 * - Banner Ads
 * - Interstitial Ads
 * - Rewarded Ads
 * - Native Ads
 * - Test IDs for development
 * - Premium user awareness (no ads for premium)
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumEngine: PremiumEngine
) {
    companion object {
        // ============================================
        // AdMob Test IDs (Use these for development)
        // ============================================
        
        // Banner Ad Test ID
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        
        // Interstitial Ad Test ID
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        
        // Rewarded Ad Test ID
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        
        // Native Advanced Ad Test ID
        const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        
        // ============================================
        // Production Ad IDs (Replace with your real IDs)
        // ============================================
        
        // TODO: Replace with your actual AdMob Ad Unit IDs for production
        const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        
        // Test Device IDs - Add your device IDs here for testing
        val TEST_DEVICE_IDS = listOf(
            "TEST_DEVICE_ID_1", // Replace with actual device ID
            "TEST_DEVICE_ID_2"
        )
        
        // Interstitial ad display intervals
        const val INTERSTITIAL_MIN_INTERVAL_MS = 60000L // 1 minute minimum between ads
        const val INTERSTITIAL_MIN_ACTIONS = 5 // Minimum actions before showing ad
    }

    // Use test IDs in debug mode
    private val isDebugMode = true // Set to false for production

    private val bannerAdUnitId: String
        get() = if (isDebugMode) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    private val interstitialAdUnitId: String
        get() = if (isDebugMode) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    private val rewardedAdUnitId: String
        get() = if (isDebugMode) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    private val nativeAdUnitId: String
        get() = if (isDebugMode) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID

    // Ad states
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShowTime = 0L
    private var actionCount = 0

    // Loading states
    private val _isInterstitialLoading = MutableStateFlow(false)
    val isInterstitialLoading: Flow<Boolean> = _isInterstitialLoading.asStateFlow()

    private val _isRewardedLoading = MutableStateFlow(false)
    val isRewardedLoading: Flow<Boolean> = _isRewardedLoading.asStateFlow()

    // Callbacks
    private var onRewarded: (() -> Unit)? = null
    private var onInterstitialClosed: (() -> Unit)? = null

    /**
     * Initialize AdMob SDK
     * Call this in Application.onCreate()
     */
    fun initialize() {
        MobileAds.initialize(context) {
            // Configure test devices
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(TEST_DEVICE_IDS)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
            
            // Preload interstitial ad
            preloadInterstitialAd()
        }
    }

    /**
     * Check if ads should be shown (not premium user)
     */
    private suspend fun shouldShowAds(): Boolean {
        return !premiumEngine.isPremium.collect { } as Boolean
    }

    // ============================================
    // BANNER ADS
    // ============================================

    /**
     * Create and load a banner ad
     */
    fun createBannerAd(
        context: Context,
        adSize: AdSize = AdSize.BANNER
    ): AdView {
        return AdView(context).apply {
            setAdSize(adSize)
            adUnitId = bannerAdUnitId
            loadAd(getAdRequest())
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    // Banner loaded successfully
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Handle error
                }
            }
        }
    }

    /**
     * Create adaptive banner ad (recommended for modern apps)
     */
    fun createAdaptiveBannerAd(
        context: Context,
        width: Int
    ): AdView {
        val adView = AdView(context)
        adView.adUnitId = bannerAdUnitId
        
        // Get adaptive ad size
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
        adView.setAdSize(adSize)
        adView.loadAd(getAdRequest())
        
        return adView
    }

    /**
     * Add banner ad to container
     */
    fun addBannerToContainer(
        container: FrameLayout,
        adSize: AdSize = AdSize.BANNER
    ) {
        container.removeAllViews()
        val adView = createBannerAd(container.context, adSize)
        container.addView(adView)
    }

    // ============================================
    // INTERSTITIAL ADS
    // ============================================

    /**
     * Preload interstitial ad
     */
    fun preloadInterstitialAd() {
        if (_isInterstitialLoading.value || interstitialAd != null) return

        _isInterstitialLoading.value = true
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            getAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialLoading.value = false
                    setupInterstitialCallbacks(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialLoading.value = false
                }
            }
        )
    }

    /**
     * Setup interstitial ad callbacks
     */
    private fun setupInterstitialCallbacks(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onInterstitialClosed?.invoke()
                preloadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preloadInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                lastInterstitialShowTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * Show interstitial ad with frequency capping
     * @param activity The activity to show the ad from
     * @param forceShow Force show without frequency capping
     * @param onClosed Callback when ad is closed
     */
    fun showInterstitialAd(
        activity: Activity,
        forceShow: Boolean = false,
        onClosed: (() -> Unit)? = null
    ) {
        onInterstitialClosed = onClosed

        // Check frequency capping
        if (!forceShow) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInterstitialShowTime < INTERSTITIAL_MIN_INTERVAL_MS) {
                onClosed?.invoke()
                return
            }
        }

        // Show ad or preload
        interstitialAd?.let { ad ->
            ad.show(activity)
        } ?: run {
            preloadInterstitialAd()
            onClosed?.invoke()
        }
    }

    /**
     * Record action for action-based interstitial display
     */
    fun recordAction() {
        actionCount++
    }

    /**
     * Check if should show interstitial based on action count
     */
    fun shouldShowActionBasedInterstitial(): Boolean {
        return actionCount >= INTERSTITIAL_MIN_ACTIONS
    }

    /**
     * Reset action count
     */
    fun resetActionCount() {
        actionCount = 0
    }

    // ============================================
    // REWARDED ADS
    // ============================================

    /**
     * Preload rewarded ad
     */
    fun preloadRewardedAd() {
        if (_isRewardedLoading.value || rewardedAd != null) return

        _isRewardedLoading.value = true
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            getAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedLoading.value = false
                    setupRewardedCallbacks(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isRewardedLoading.value = false
                }
            }
        )
    }

    /**
     * Setup rewarded ad callbacks
     */
    private fun setupRewardedCallbacks(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                preloadRewardedAd()
            }
        }
    }

    /**
     * Show rewarded ad
     * @param activity The activity to show the ad from
     * @param onRewarded Callback when user earns reward
     * @param onFailed Callback when ad fails to show
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        this.onRewarded = onRewarded

        rewardedAd?.let { ad ->
            ad.show(activity) { rewardItem ->
                // User earned reward
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                onRewarded.invoke()
            }
        } ?: run {
            onFailed?.invoke()
            preloadRewardedAd()
        }
    }

    /**
     * Check if rewarded ad is ready
     */
    fun isRewardedAdReady(): Boolean = rewardedAd != null

    // ============================================
    // NATIVE ADS
    // ============================================

    /**
     * Load native ad
     * @param onLoaded Callback when native ad is loaded
     */
    fun loadNativeAd(
        context: Context,
        onLoaded: (NativeAd) -> Unit,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val adLoader = AdLoader.Builder(context, nativeAdUnitId)
            .forNativeAd { nativeAd ->
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed?.invoke(error)
                }
            })
            .build()

        adLoader.loadAd(getAdRequest())
    }

    /**
     * Populate native ad view
     */
    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the native ad view
        adView.setNativeAd(nativeAd)

        // Register views for click handling
        adView.iconView = adView.findViewById(com.google.android.gms.ads.R.id.ad_app_icon)
        adView.headlineView = adView.findViewById(com.google.android.gms.ads.R.id.ad_headline)
        adView.bodyView = adView.findViewById(com.google.android.gms.ads.R.id.ad_body)
        adView.callToActionView = adView.findViewById(com.google.android.gms.ads.R.id.ad_call_to_action)
        adView.starRatingView = adView.findViewById(com.google.android.gms.ads.R.id.ad_stars)
        adView.advertiserView = adView.findViewById(com.google.android.gms.ads.R.id.ad_advertiser)

        // Populate views
        (adView.headlineView as? android.widget.TextView)?.text = nativeAd.headline
        (adView.bodyView as? android.widget.TextView)?.text = nativeAd.body
        (adView.callToActionView as? android.widget.Button)?.text = nativeAd.callToAction
        (adView.advertiserView as? android.widget.TextView)?.text = nativeAd.advertiser
        nativeAd.icon?.let { icon ->
            (adView.iconView as? android.widget.ImageView)?.setImageDrawable(icon.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Build ad request with targeting
     */
    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder()
            // Add keywords for better targeting
            .addKeyword("music")
            .addKeyword("media player")
            .addKeyword("audio")
            .addKeyword("video")
            .addKeyword("entertainment")
            .build()
    }

    /**
     * Destroy all ads and release resources
     */
    fun destroy() {
        interstitialAd = null
        rewardedAd = null
    }

    /**
     * Pause ads (call in Activity.onPause)
     */
    fun pause() {
        // Pause any ongoing ad operations if needed
    }

    /**
     * Resume ads (call in Activity.onResume)
     */
    fun resume() {
        // Resume ad operations if needed
    }
}

/**
 * Extension function to show interstitial after song/video change
 */
fun AdManager.showAdAfterMediaChange(activity: Activity) {
    recordAction()
    if (shouldShowActionBasedInterstitial()) {
        showInterstitialAd(activity)
        resetActionCount()
    }
}
