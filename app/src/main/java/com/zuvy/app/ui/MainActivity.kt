package com.zuvy.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.zuvy.app.R
import com.zuvy.app.databinding.ActivityMainBinding
import com.zuvy.app.utils.PreferenceManager
import com.zuvy.app.utils.showRateAppDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by androidx.activity.viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupWindowInsets()
        setupBackPressedHandler()
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

        // Setup bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Add destination listener for animations and visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.videoPlayerFragment,
                R.id.musicPlayerFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                when (navController.currentDestination?.id) {
                    R.id.homeFragment,
                    R.id.musicFragment,
                    R.id.discoverFragment,
                    R.id.moreFragment -> {
                        if (backPressedTime + 2000 > System.currentTimeMillis()) {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.press_again_to_exit),
                                Snackbar.LENGTH_SHORT
                            ).show()
                            backPressedTime = System.currentTimeMillis()
                        }
                    }
                    else -> {
                        navController.popBackStack()
                    }
                }
            }
        })
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
            val requestLauncher = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            registerForActivityResult(requestLauncher) { results ->
                val allGranted = results.values.all { it }
                if (allGranted) {
                    viewModel.loadMedia()
                }
            }.launch(needPermissions.toTypedArray())
        } else {
            viewModel.loadMedia()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                val uri = it.data
                uri?.let { videoUri ->
                    // Handle video file intent
                    viewModel.playVideoFromIntent(videoUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Increment launch count
        preferenceManager.incrementLaunchCount()
    }
}
