package com.ludoblitz.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ludoblitz.app.R
import com.ludoblitz.app.databinding.ActivityMainBinding
import com.ludoblitz.app.ui.viewmodel.MainUiState
import com.ludoblitz.app.ui.viewmodel.MainViewModel
import com.ludoblitz.app.ui.viewmodel.NavigationEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Activity - Container for all fragments and main navigation hub
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        setupNavigation()
        setupObservers()
        setupBackPressedHandler()
        
        // Check if should show login
        if (intent.getBooleanExtra("show_login", false)) {
            showLoginDialog()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Setup bottom navigation
        binding.bottomNav.setupWithNavController(navController)
        
        // Hide bottom nav on certain fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.gameFragment -> {
                    binding.bottomNav.visibility = View.GONE
                    binding.headerCard.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.headerCard.visibility = View.VISIBLE
                }
            }
        }
        
        // Bottom nav item selected listener
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_play -> {
                    navController.navigate(R.id.playFragment)
                    true
                }
                R.id.nav_shop -> {
                    navController.navigate(R.id.shopFragment)
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
        
        // Header coin click
        binding.coinContainer.setOnClickListener {
            // Show earn coins dialog or navigate to shop
            navController.navigate(R.id.shopFragment)
        }
        
        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Notifications button
        binding.btnNotifications.setOnClickListener {
            // Show notifications
        }
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(this) { user ->
            user?.let {
                updateUserInfo(it)
            }
        }
        
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainUiState.Unauthenticated -> {
                    showLoginDialog()
                }
                else -> {}
            }
        }
        
        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is NavigationEvent.ShowTutorial -> {
                    startActivity(Intent(this, TutorialActivity::class.java))
                }
                is NavigationEvent.ShowDailyReward -> {
                    showDailyRewardDialog(event.day)
                }
                else -> {}
            }
        }
        
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun updateUserInfo(user: com.ludoblitz.app.data.model.User) {
        binding.tvUserName.text = user.displayName
        
        // Animate coin update
        val currentCoins = binding.tvCoins.text.toString().replace(",", "").toLongOrNull() ?: 0
        if (currentCoins != user.coins) {
            animateValueChange(binding.tvCoins, currentCoins.toInt(), user.coins.toInt())
        } else {
            binding.tvCoins.text = formatNumber(user.coins)
        }
        
        // Level and XP
        binding.tvLevel.text = "Lvl ${user.level}"
        binding.progressXp.progress = (user.getXpProgress() * 100).toInt()
    }

    private fun animateValueChange(view: android.widget.TextView, from: Int, to: Int) {
        val duration = 500L
        val startTime = System.currentTimeMillis()
        
        lifecycleScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed / duration).coerceAtMost(1f)
                
                val current = (from + (to - from) * progress).toInt()
                view.text = formatNumber(current.toLong())
                
                if (progress >= 1f) break
                delay(16)
            }
        }
    }

    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> number.toString()
        }
    }

    private fun showLoginDialog() {
        // Navigate to login fragment or show bottom sheet
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController.navigate(R.id.loginFragment)
    }

    private fun showDailyRewardDialog(day: Int) {
        DailyRewardBottomSheet.newInstance(day).apply {
            onClaimClicked = {
                viewModel.claimDailyReward(day)
            }
        }.show(supportFragmentManager, "daily_reward")
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    finishAffinity()
                    return
                }
                
                backPressedOnce = true
                Snackbar.make(
                    binding.root,
                    "Press back again to exit",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                lifecycleScope.launch {
                    delay(2000)
                    backPressedOnce = false
                }
            }
        })
    }
}
