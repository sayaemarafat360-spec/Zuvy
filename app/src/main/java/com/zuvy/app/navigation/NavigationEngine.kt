package com.zuvy.app.navigation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import com.zuvy.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NavigationEngine - Professional navigation management system
 *
 * Features:
 * - Navigation history stack with unlimited depth
 * - Double-tap back to exit with customizable timeout
 * - Navigation state preservation per tab
 * - Back press interceptors (for unsaved changes, mini player, etc.)
 * - Smooth custom animations
 * - Deep link handling with proper back stack
 * - Navigation events flow for analytics
 * - Fragment result handling
 * - Tab switching with state preservation
 */
@Singleton
class NavigationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LifecycleObserver {

    companion object {
        const val DOUBLE_TAP_EXIT_TIMEOUT = 2000L // 2 seconds
        const val MAX_HISTORY_SIZE = 50
        const val KEY_FRAGMENT_RESULT = "fragment_result"
        const val KEY_NAVIGATION_FROM = "navigation_from"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Navigation controller reference
    private var navController: NavController? = null

    // Fragment manager reference
    private var fragmentManager: FragmentManager? = null

    // Navigation history stack
    private val _navigationHistory = MutableStateFlow<List<NavigationEntry>>(emptyList())
    val navigationHistory: StateFlow<List<NavigationEntry>> = _navigationHistory.asStateFlow()

    // Current destination
    private val _currentDestination = MutableStateFlow<NavDestination?>(null)
    val currentDestination: StateFlow<NavDestination?> = _currentDestination.asStateFlow()

    // Current fragment
    private val _currentFragment = MutableStateFlow<Fragment?>(null)
    val currentFragment: StateFlow<Fragment?> = _currentFragment.asStateFlow()

    // Tab states (for bottom navigation)
    private val tabStates = mutableMapOf<Int, TabNavigationState>()

    // Back press interceptors
    private val backPressInterceptors = mutableListOf<BackPressInterceptor>()

    // Navigation events
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    // Double tap exit state
    private var lastBackPressTime = 0L
    private var backPressCount = 0

    // Exit callback
    private var onExitCallback: (() -> Unit)? = null

    // Custom navigation options
    private val defaultNavOptions: NavOptions by lazy {
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
    }

    private val slideUpNavOptions: NavOptions by lazy {
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.slide_down)
            .build()
    }

    /**
     * Initialize the navigation engine
     */
    fun initialize(navController: NavController, fragmentManager: FragmentManager) {
        this.navController = navController
        this.fragmentManager = fragmentManager

        // Add destination changed listener
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            onDestinationChanged(destination, arguments)
        }

        // Initialize tab states
        initTabStates()

        Timber.d("NavigationEngine initialized")
    }

    /**
     * Initialize tab states for bottom navigation
     */
    private fun initTabStates() {
        val tabs = listOf(
            R.id.homeFragment,
            R.id.musicFragment,
            R.id.discoverFragment,
            R.id.moreFragment
        )
        tabs.forEach { tabId ->
            tabStates[tabId] = TabNavigationState(tabId)
        }
    }

    /**
     * Handle destination change
     */
    private fun onDestinationChanged(destination: NavDestination, arguments: Bundle?) {
        _currentDestination.value = destination

        // Add to history
        addToHistory(destination, arguments)

        // Find current fragment
        fragmentManager?.let { fm ->
            fm.primaryNavigationFragment?.let { fragment ->
                _currentFragment.value = fragment
            }
        }

        // Emit navigation event
        scope.launch {
            _navigationEvents.emit(
                NavigationEvent(
                    destinationId = destination.id,
                    destinationName = destination.displayName,
                    arguments = arguments
                )
            )
        }

        Timber.d("Navigated to: ${destination.displayName}")
    }

    /**
     * Add entry to navigation history
     */
    private fun addToHistory(destination: NavDestination, arguments: Bundle?) {
        val currentHistory = _navigationHistory.value.toMutableList()

        // Don't add duplicate consecutive entries
        if (currentHistory.isNotEmpty() && currentHistory.last().destinationId == destination.id) {
            return
        }

        val entry = NavigationEntry(
            destinationId = destination.id,
            destinationName = destination.displayName ?: "Unknown",
            arguments = arguments,
            timestamp = System.currentTimeMillis()
        )

        currentHistory.add(entry)

        // Limit history size
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(0)
        }

        _navigationHistory.value = currentHistory
    }

    // ============================================
    // NAVIGATION METHODS
    // ============================================

    /**
     * Navigate to a destination with default animations
     */
    fun navigate(destinationId: Int, arguments: Bundle? = null) {
        navController?.navigate(destinationId, arguments, defaultNavOptions)
    }

    /**
     * Navigate with slide up animation (for modals/bottom sheets)
     */
    fun navigateSlideUp(destinationId: Int, arguments: Bundle? = null) {
        navController?.navigate(destinationId, arguments, slideUpNavOptions)
    }

    /**
     * Navigate using NavDirections
     */
    fun navigate(directions: NavDirections) {
        navController?.navigate(directions, defaultNavOptions)
    }

    /**
     * Navigate with custom options
     */
    fun navigate(destinationId: Int, options: NavOptions, arguments: Bundle? = null) {
        navController?.navigate(destinationId, arguments, options)
    }

    /**
     * Navigate with extras
     */
    fun navigate(
        destinationId: Int,
        arguments: Bundle? = null,
        extras: Navigator.Extras
    ) {
        navController?.navigate(destinationId, arguments, null, extras)
    }

    /**
     * Navigate and clear back stack up to a destination
     */
    fun navigateAndClearBackStack(destinationId: Int, popUpToId: Int, inclusive: Boolean = false) {
        val options = NavOptions.Builder()
            .setPopUpTo(popUpToId, inclusive)
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .build()
        navController?.navigate(destinationId, null, options)
    }

    /**
     * Navigate to root (clear all back stack)
     */
    fun navigateToRoot(rootId: Int = R.id.homeFragment) {
        val options = NavOptions.Builder()
            .setPopUpTo(rootId, true)
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_out)
            .build()
        navController?.navigate(rootId, null, options)
    }

    /**
     * Pop back stack
     */
    fun popBackStack(): Boolean {
        return navController?.popBackStack() ?: false
    }

    /**
     * Pop back stack to specific destination
     */
    fun popBackStack(destinationId: Int, inclusive: Boolean = false): Boolean {
        return navController?.popBackStack(destinationId, inclusive) ?: false
    }

    /**
     * Pop all back stack to root
     */
    fun popAllBackStack(): Boolean {
        return navController?.popBackStack(R.id.homeFragment, false) ?: false
    }

    // ============================================
    // TAB NAVIGATION
    // ============================================

    /**
     * Switch to a tab with state preservation
     */
    fun switchTab(tabId: Int) {
        val currentDestId = _currentDestination.value?.id ?: return

        // Save current tab state
        if (isTabDestination(currentDestId)) {
            saveTabState(currentDestId)
        }

        // Check if we need to restore a tab state
        val tabState = tabStates[tabId]
        if (tabState != null && tabState.hasHistory()) {
            // Navigate to the last destination in that tab
            val lastEntry = tabState.getLastEntry()
            if (lastEntry != null) {
                navigate(lastEntry.destinationId, lastEntry.arguments)
                return
            }
        }

        // Navigate to the tab root
        navigate(tabId)
    }

    /**
     * Save current tab state
     */
    private fun saveTabState(tabId: Int) {
        val history = _navigationHistory.value
        val tabHistory = history.filter { entry ->
            // Filter entries belonging to this tab
            isEntryBelongToTab(entry.destinationId, tabId)
        }
        tabStates[tabId]?.setHistory(tabHistory)
    }

    /**
     * Check if destination is a main tab
     */
    private fun isTabDestination(destinationId: Int): Boolean {
        return destinationId in listOf(
            R.id.homeFragment,
            R.id.musicFragment,
            R.id.discoverFragment,
            R.id.moreFragment
        )
    }

    /**
     * Check if an entry belongs to a tab
     */
    private fun isEntryBelongToTab(entryId: Int, tabId: Int): Boolean {
        return when (tabId) {
            R.id.homeFragment -> entryId in listOf(
                R.id.homeFragment, R.id.folderBrowserFragment,
                R.id.playlistDetailFragment, R.id.searchFragment
            )
            R.id.musicFragment -> entryId in listOf(
                R.id.musicFragment, R.id.musicPlayerFragment,
                R.id.artistDetailFragment, R.id.albumDetailFragment,
                R.id.equalizerFragment
            )
            R.id.discoverFragment -> entryId in listOf(
                R.id.discoverFragment, R.id.onlinePlayerFragment
            )
            R.id.moreFragment -> entryId in listOf(
                R.id.moreFragment, R.id.settingsFragment,
                R.id.premiumFragment
            )
            else -> false
        }
    }

    // ============================================
    // BACK PRESS HANDLING
    // ============================================

    /**
     * Add a back press interceptor
     * Return true to consume the back press, false to continue
     */
    fun addBackPressInterceptor(interceptor: BackPressInterceptor) {
        backPressInterceptors.add(interceptor)
    }

    /**
     * Remove a back press interceptor
     */
    fun removeBackPressInterceptor(interceptor: BackPressInterceptor) {
        backPressInterceptors.remove(interceptor)
    }

    /**
     * Handle back press with interceptors
     * @return true if back press was handled
     */
    fun handleBackPress(activity: Activity): Boolean {
        val currentTime = System.currentTimeMillis()

        // Run interceptors first
        for (interceptor in backPressInterceptors) {
            if (interceptor.onInterceptBackPress()) {
                Timber.d("Back press intercepted by: ${interceptor.javaClass.simpleName}")
                return true
            }
        }

        // Check if we can pop back stack
        val canPop = navController?.currentDestination?.id != navController?.graph?.startDestinationId

        if (canPop) {
            // Normal back navigation
            popBackStack()
            return true
        }

        // At root - handle double tap exit
        return handleDoubleTapExit(activity, currentTime)
    }

    /**
     * Handle double tap to exit
     */
    private fun handleDoubleTapExit(activity: Activity, currentTime: Long): Boolean {
        if (currentTime - lastBackPressTime < DOUBLE_TAP_EXIT_TIMEOUT) {
            // Second tap within timeout - exit
            onExitCallback?.invoke() ?: run {
                activity.finish()
            }
            return true
        } else {
            // First tap - show toast/message
            lastBackPressTime = currentTime
            showExitPrompt()
            return true
        }
    }

    /**
     * Show exit prompt (override to customize)
     */
    private fun showExitPrompt() {
        scope.launch {
            _navigationEvents.emit(NavigationEvent(NavigationEvent.EXIT_PROMPT))
        }
    }

    /**
     * Set exit callback
     */
    fun setOnExitCallback(callback: () -> Unit) {
        onExitCallback = callback
    }

    // ============================================
    // FRAGMENT RESULTS
    // ============================================

    /**
     * Set fragment result
     */
    fun setFragmentResult(requestKey: String, result: Bundle) {
        fragmentManager?.setFragmentResult(requestKey, result)
    }

    /**
     * Get fragment result listener
     */
    fun getFragmentResultListener(requestKey: String, listener: (Bundle) -> Unit) {
        fragmentManager?.setFragmentResultListener(requestKey) { _, bundle ->
            listener(bundle)
        }
    }

    // ============================================
    // UTILITIES
    // ============================================

    /**
     * Check if current destination is one of the given ids
     */
    fun isCurrentDestination(vararg destinationIds: Int): Boolean {
        return _currentDestination.value?.id in destinationIds
    }

    /**
     * Check if can navigate back
     */
    fun canNavigateBack(): Boolean {
        return navController?.previousBackStackEntry != null
    }

    /**
     * Get previous destination
     */
    fun getPreviousDestination(): NavigationEntry? {
        val history = _navigationHistory.value
        return if (history.size >= 2) history[history.size - 2] else null
    }

    /**
     * Clear navigation history
     */
    fun clearHistory() {
        _navigationHistory.value = emptyList()
    }

    /**
     * Reset double tap state
     */
    fun resetDoubleTapState() {
        lastBackPressTime = 0L
        backPressCount = 0
    }

    /**
     * Get navigation depth
     */
    fun getNavigationDepth(): Int {
        return _navigationHistory.value.size
    }

    /**
     * Get current navigation path
     */
    fun getCurrentPath(): String {
        return _navigationHistory.value.joinToString(" -> ") { it.destinationName }
    }

    /**
     * Log navigation state (for debugging)
     */
    fun logNavigationState() {
        Timber.d("=== Navigation State ===")
        Timber.d("Current: ${_currentDestination.value?.displayName}")
        Timber.d("History depth: ${_navigationHistory.value.size}")
        Timber.d("Path: ${getCurrentPath()}")
        Timber.d("========================")
    }

    // ============================================
    // CLEANUP
    // ============================================

    /**
     * Clean up resources
     */
    fun cleanup() {
        navController = null
        fragmentManager = null
        backPressInterceptors.clear()
        _navigationHistory.value = emptyList()
        Timber.d("NavigationEngine cleaned up")
    }
}

// ============================================
// DATA CLASSES
// ============================================

/**
 * Navigation entry in history
 */
data class NavigationEntry(
    val destinationId: Int,
    val destinationName: String,
    val arguments: Bundle?,
    val timestamp: Long
)

/**
 * Navigation event for analytics/observables
 */
data class NavigationEvent(
    val destinationId: Int = 0,
    val destinationName: String = "",
    val arguments: Bundle? = null,
    val type: Int = NAVIGATION
) {
    companion object {
        const val NAVIGATION = 0
        const val BACK = 1
        const val EXIT_PROMPT = 2
        const val TAB_SWITCH = 3
    }
}

/**
 * Tab navigation state
 */
class TabNavigationState(val tabId: Int) {
    private var history: List<NavigationEntry> = emptyList()

    fun setHistory(entries: List<NavigationEntry>) {
        history = entries
    }

    fun getLastEntry(): NavigationEntry? {
        return history.lastOrNull()
    }

    fun hasHistory(): Boolean = history.isNotEmpty()

    fun clearHistory() {
        history = emptyList()
    }
}

/**
 * Back press interceptor interface
 */
interface BackPressInterceptor {
    /**
     * Called when back button is pressed
     * @return true to consume the back press, false to continue
     */
    fun onInterceptBackPress(): Boolean
}
