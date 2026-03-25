package com.zuvy.app.navigation

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.zuvy.app.R

/**
 * Common back press interceptors for the app
 */

/**
 * BottomSheetInterceptor - Handles back press when bottom sheets are visible
 */
class BottomSheetInterceptor(
    private val fragmentManager: FragmentManager
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        // Check for visible bottom sheets
        val fragments = fragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is BottomSheetDialogFragment && fragment.isVisible) {
                // Let the bottom sheet handle its own dismiss
                return false
            }
        }
        return false
    }
}

/**
 * MiniPlayerInterceptor - Handles mini player expansion on back press
 */
class MiniPlayerInterceptor(
    private val isMiniPlayerExpanded: () -> Boolean,
    private val collapseMiniPlayer: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (isMiniPlayerExpanded()) {
            collapseMiniPlayer()
            return true
        }
        return false
    }
}

/**
 * MultiSelectInterceptor - Handles back press during multi-select mode
 */
class MultiSelectInterceptor(
    private val isInMultiSelectMode: () -> Boolean,
    private val exitMultiSelectMode: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (isInMultiSelectMode()) {
            exitMultiSelectMode()
            return true
        }
        return false
    }
}

/**
 * SearchInterceptor - Handles back press when search is active
 */
class SearchInterceptor(
    private val isSearchActive: () -> Boolean,
    private val closeSearch: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (isSearchActive()) {
            closeSearch()
            return true
        }
        return false
    }
}

/**
 * UnsavedChangesInterceptor - Handles back press with unsaved changes
 */
class UnsavedChangesInterceptor(
    private val hasUnsavedChanges: () -> Boolean,
    private val showUnsavedChangesDialog: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog()
            return true
        }
        return false
    }
}

/**
 * FullscreenInterceptor - Handles back press when in fullscreen mode
 */
class FullscreenInterceptor(
    private val isFullscreen: () -> Boolean,
    private val exitFullscreen: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (isFullscreen()) {
            exitFullscreen()
            return true
        }
        return false
    }
}

/**
 * PlaybackInterceptor - Stops playback on back press from player
 */
class PlaybackInterceptor(
    private val isPlaying: () -> Boolean,
    private val shouldStopOnBack: () -> Boolean,
    private val stopPlayback: () -> Unit
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        if (isPlaying() && shouldStopOnBack()) {
            stopPlayback()
            return false // Continue with normal back navigation
        }
        return false
    }
}

/**
 * CompositeBackPressInterceptor - Combines multiple interceptors
 */
class CompositeBackPressInterceptor(
    private vararg val interceptors: BackPressInterceptor
) : BackPressInterceptor {

    override fun onInterceptBackPress(): Boolean {
        for (interceptor in interceptors) {
            if (interceptor.onInterceptBackPress()) {
                return true
            }
        }
        return false
    }
}

/**
 * BackPressHandler - Central handler for all back press logic
 */
class BackPressHandler(
    private val navigationEngine: NavigationEngine,
    private val fragmentManager: FragmentManager
) {

    private val interceptors = mutableListOf<BackPressInterceptor>()

    /**
     * Add an interceptor
     */
    fun addInterceptor(interceptor: BackPressInterceptor) {
        interceptors.add(interceptor)
        navigationEngine.addBackPressInterceptor(interceptor)
    }

    /**
     * Remove an interceptor
     */
    fun removeInterceptor(interceptor: BackPressInterceptor) {
        interceptors.remove(interceptor)
        navigationEngine.removeBackPressInterceptor(interceptor)
    }

    /**
     * Setup common interceptors
     */
    fun setupCommonInterceptors(
        miniPlayerProvider: MiniPlayerProvider? = null,
        searchProvider: SearchProvider? = null,
        fullscreenProvider: FullscreenProvider? = null,
        multiSelectProvider: MultiSelectProvider? = null
    ) {
        // Bottom sheet interceptor
        addInterceptor(BottomSheetInterceptor(fragmentManager))

        // Mini player interceptor
        miniPlayerProvider?.let { provider ->
            addInterceptor(MiniPlayerInterceptor(
                isMiniPlayerExpanded = { provider.isExpanded() },
                collapseMiniPlayer = { provider.collapse() }
            ))
        }

        // Search interceptor
        searchProvider?.let { provider ->
            addInterceptor(SearchInterceptor(
                isSearchActive = { provider.isActive() },
                closeSearch = { provider.close() }
            ))
        }

        // Fullscreen interceptor
        fullscreenProvider?.let { provider ->
            addInterceptor(FullscreenInterceptor(
                isFullscreen = { provider.isFullscreen() },
                exitFullscreen = { provider.exitFullscreen() }
            ))
        }

        // Multi-select interceptor
        multiSelectProvider?.let { provider ->
            addInterceptor(MultiSelectInterceptor(
                isInMultiSelectMode = { provider.isInMultiSelectMode() },
                exitMultiSelectMode = { provider.exitMultiSelectMode() }
            ))
        }
    }

    /**
     * Clear all interceptors
     */
    fun clearInterceptors() {
        interceptors.forEach { navigationEngine.removeBackPressInterceptor(it) }
        interceptors.clear()
    }
}

// Provider interfaces
interface MiniPlayerProvider {
    fun isExpanded(): Boolean
    fun collapse()
}

interface SearchProvider {
    fun isActive(): Boolean
    fun close()
}

interface FullscreenProvider {
    fun isFullscreen(): Boolean
    fun exitFullscreen()
}

interface MultiSelectProvider {
    fun isInMultiSelectMode(): Boolean
    fun exitMultiSelectMode()
}
