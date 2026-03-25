package com.zuvy.app.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController

/**
 * Navigation Extensions for easier navigation throughout the app
 */

// ============================================
// FRAGMENT EXTENSIONS
// ============================================

/**
 * Navigate with default animations
 */
fun Fragment.navigate(@IdRes destinationId: Int, arguments: Bundle? = null) {
    findNavController().navigate(destinationId, arguments, getDefaultNavOptions())
}

/**
 * Navigate with slide up animation (for modals)
 */
fun Fragment.navigateSlideUp(@IdRes destinationId: Int, arguments: Bundle? = null) {
    findNavController().navigate(destinationId, arguments, getSlideUpNavOptions())
}

/**
 * Navigate using NavDirections
 */
fun Fragment.navigate(directions: NavDirections) {
    findNavController().navigate(directions, getDefaultNavOptions())
}

/**
 * Navigate with fade animation
 */
fun Fragment.navigateWithFade(@IdRes destinationId: Int, arguments: Bundle? = null) {
    findNavController().navigate(destinationId, arguments, getFadeNavOptions())
}

/**
 * Pop back stack
 */
fun Fragment.navigateBack(): Boolean {
    return findNavController().popBackStack()
}

/**
 * Pop back stack to specific destination
 */
fun Fragment.navigateBackTo(@IdRes destinationId: Int, inclusive: Boolean = false): Boolean {
    return findNavController().popBackStack(destinationId, inclusive)
}

/**
 * Navigate to root
 */
fun Fragment.navigateToRoot() {
    val navController = findNavController()
    val startDestination = navController.graph.startDestinationId
    val options = NavOptions.Builder()
        .setPopUpTo(startDestination, true)
        .build()
    navController.navigate(startDestination, null, options)
}

/**
 * Check if can navigate back
 */
fun Fragment.canNavigateBack(): Boolean {
    return findNavController().previousBackStackEntry != null
}

/**
 * Get navigation result
 */
fun <T> Fragment.getNavigationResult(key: String, onResult: (T) -> Unit) {
    parentFragmentManager.setFragmentResultListener(key, viewLifecycleOwner) { _, bundle ->
        @Suppress("UNCHECKED_CAST")
        val result = bundle.get(key) as? T
        result?.let { onResult(it) }
    }
}

/**
 * Set navigation result
 */
fun <T> Fragment.setNavigationResult(key: String, result: T) {
    parentFragmentManager.setFragmentResult(key, bundleOf(key to result))
}

/**
 * Get navigation result once
 */
fun <T> Fragment.getNavigationResultOnce(key: String, onResult: (T) -> Unit) {
    parentFragmentManager.setFragmentResultListener(key, viewLifecycleOwner) { _, bundle ->
        @Suppress("UNCHECKED_CAST")
        val result = bundle.get(key) as? T
        result?.let { onResult(it) }
        parentFragmentManager.clearFragmentResultListener(key)
    }
}

// ============================================
// NAV OPTIONS BUILDERS
// ============================================

/**
 * Default navigation options with slide animations
 */
fun getDefaultNavOptions(): NavOptions {
    return NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()
}

/**
 * Slide up navigation options (for modals/bottom sheets)
 */
fun getSlideUpNavOptions(): NavOptions {
    return NavOptions.Builder()
        .setEnterAnim(R.anim.slide_up)
        .setExitAnim(R.anim.fade_out)
        .setPopEnterAnim(R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_down)
        .build()
}

/**
 * Fade navigation options
 */
fun getFadeNavOptions(): NavOptions {
    return NavOptions.Builder()
        .setEnterAnim(R.anim.fade_in)
        .setExitAnim(R.anim.fade_out)
        .setPopEnterAnim(R.anim.fade_in)
        .setPopExitAnim(R.anim.fade_out)
        .build()
}

/**
 * Clear back stack navigation options
 */
fun getClearBackStackOptions(@IdRes popUpTo: Int, inclusive: Boolean = false): NavOptions {
    return NavOptions.Builder()
        .setPopUpTo(popUpTo, inclusive)
        .setEnterAnim(R.anim.fade_in)
        .setExitAnim(R.anim.fade_out)
        .build()
}

// ============================================
// NAV CONTROLLER EXTENSIONS
// ============================================

/**
 * Navigate with animation helper
 */
fun NavController.navigateWithAnimation(
    @IdRes destinationId: Int,
    arguments: Bundle? = null,
    animationType: NavigationAnimation = NavigationAnimation.SLIDE
) {
    val options = when (animationType) {
        NavigationAnimation.SLIDE -> getDefaultNavOptions()
        NavigationAnimation.SLIDE_UP -> getSlideUpNavOptions()
        NavigationAnimation.FADE -> getFadeNavOptions()
        NavigationAnimation.NONE -> null
    }
    navigate(destinationId, arguments, options)
}

/**
 * Animation types for navigation
 */
enum class NavigationAnimation {
    SLIDE,
    SLIDE_UP,
    FADE,
    NONE
}

// ============================================
// SAFE NAVIGATION
// ============================================

/**
 * Safe navigation that catches exceptions
 */
fun NavController.safeNavigate(@IdRes destinationId: Int, arguments: Bundle? = null): Boolean {
    return try {
        navigate(destinationId, arguments)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Safe navigation with directions
 */
fun NavController.safeNavigate(directions: NavDirections): Boolean {
    return try {
        navigate(directions)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Safe pop back stack
 */
fun NavController.safePopBackStack(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        false
    }
}

// ============================================
// FRAGMENT LIFECYCLE NAVIGATION HELPERS
// ============================================

/**
 * Observe navigation events from the fragment
 */
fun Fragment.observeNavigationEvents(
    lifecycleOwner: LifecycleOwner = viewLifecycleOwner,
    onNavigate: (NavigationEvent) -> Unit
) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            // Navigation events are handled through NavigationEngine
        }
    })
}

/**
 * Register for fragment result with lifecycle awareness
 */
fun <T> Fragment.registerForNavigationResult(
    key: String,
    lifecycleOwner: LifecycleOwner = viewLifecycleOwner,
    onResult: (T) -> Unit
) {
    parentFragmentManager.setFragmentResultListener(key, lifecycleOwner) { _, bundle ->
        @Suppress("UNCHECKED_CAST")
        val result = bundle.get(key) as? T
        result?.let { onResult(it) }
    }
}
