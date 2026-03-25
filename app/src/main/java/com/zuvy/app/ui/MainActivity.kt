package com.zuvy.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.zuvy.app.R
import com.zuvy.app.databinding.ActivityMainBinding
import com.zuvy.app.navigation.BackPressHandler
import com.zuvy.app.navigation.FullscreenProvider
import com.zuvy.app.navigation.MiniPlayerProvider
import com.zuvy.app.navigation.NavigationEngine
import com.zuvy.app.navigation.NavigationEvent
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.utils.PreferenceManager
import com.zuvy.app.utils.showRateAppDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MiniPlayerProvider, FullscreenProvider {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var playerManager: PlayerManager

    @Inject
    lateinit var navigationEngine: NavigationEngine

    private lateinit var backPressHandler: BackPressHandler

    // Mini player state
    private var isMiniPlayerExpanded = false

    // Fullscreen state
    private var isFullscreenMode = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            // Load media
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupWindowInsets()
        setupBackPressed()
        setupMiniPlayer()
        setupNavigationEvents()
        checkPermissions()
        handleIntent(intent)

        // Show rate dialog after some launches
        if (preferenceManager.getLaunchCount() >= 5 && !preferenceManager.hasRatedApp()) {
            showRateAppDialog()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Initialize navigation engine
        navigationEngine.initialize(navController, supportFragmentManager)

        // Setup back press handler
        backPressHandler = BackPressHandler(navigationEngine, supportFragmentManager)
        backPressHandler.setupCommonInterceptors(
            miniPlayerProvider = this,
            fullscreenProvider = this
        )

        // Setup bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Add destination listener for animations and visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.videoPlayerFragment,
                R.id.musicPlayerFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.miniPlayer.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    // Mini player visibility handled by player state
                }
            }
        }

        // Set exit callback for navigation engine
        navigationEngine.setOnExitCallback {
            finish()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Use navigation engine for all back press handling
                if (!navigationEngine.handleBackPress(this@MainActivity)) {
                    // If navigation engine didn't handle it, use default behavior
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setPlayerManager(playerManager)
        binding.miniPlayer.setOnExpandListener {
            isMiniPlayerExpanded = true
            
            // Navigate to music player with slide up animation
            navigationEngine.navigateSlideUp(R.id.musicPlayerFragment)
        }
        
        binding.miniPlayer.setOnCollapseListener {
            isMiniPlayerExpanded = false
        }
        
        binding.miniPlayer.observe(this, playerManager)
    }

    private fun setupNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationEngine.navigationEvents.collect { event ->
                    when (event.type) {
                        NavigationEvent.EXIT_PROMPT -> {
                            showExitPrompt()
                        }
                        NavigationEvent.TAB_SWITCH -> {
                            Timber.d("Tab switched to: ${event.destinationName}")
                        }
                    }
                }
            }
        }
    }

    private fun showExitPrompt() {
        Snackbar.make(
            binding.root,
            getString(R.string.press_again_to_exit),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Read media permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needPermissions.isNotEmpty()) {
            permissionLauncher.launch(needPermissions.toTypedArray())
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                val uri = it.data
                uri?.let { videoUri ->
                    // Handle video file intent
                    // playerManager.playMedia(videoUri, "External Video", MediaType.VIDEO)
                }
            }
        }
    }

    // ============================================
    // MiniPlayerProvider Implementation
    // ============================================

    override fun isExpanded(): Boolean = isMiniPlayerExpanded

    override fun collapse() {
        isMiniPlayerExpanded = false
        binding.miniPlayer.collapse()
    }

    // ============================================
    // FullscreenProvider Implementation
    // ============================================

    override fun isFullscreen(): Boolean = isFullscreenMode

    override fun exitFullscreen() {
        isFullscreenMode = false
        // Exit fullscreen mode
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    /**
     * Set fullscreen mode (called from video player)
     */
    fun setFullscreenMode(fullscreen: Boolean) {
        isFullscreenMode = fullscreen
    }

    // ============================================
    // Activity Lifecycle
    // ============================================

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup navigation engine
        navigationEngine.cleanup()
        backPressHandler.clearInterceptors()
        
        // Increment launch count
        preferenceManager.incrementLaunchCount()
    }

    // ============================================
    // Public Navigation Methods (for fragments)
    // ============================================

    /**
     * Get the navigation engine for use in fragments
     */
    fun getNavigationEngine(): NavigationEngine = navigationEngine

    /**
     * Get bottom navigation view for visibility control
     */
    fun getBottomNavigation() = binding.bottomNavigation

    /**
     * Get mini player view
     */
    fun getMiniPlayer() = binding.miniPlayer

    /**
     * Hide bottom navigation (for fullscreen)
     */
    fun hideBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }

    /**
     * Show bottom navigation
     */
    fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    /**
     * Hide mini player (for player screens)
     */
    fun hideMiniPlayer() {
        binding.miniPlayer.visibility = View.GONE
    }

    /**
     * Show mini player
     */
    fun showMiniPlayer() {
        binding.miniPlayer.visibility = View.VISIBLE
    }
}
