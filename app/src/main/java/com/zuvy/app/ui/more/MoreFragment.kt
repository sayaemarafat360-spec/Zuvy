package com.zuvy.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.zuvy.app.BuildConfig
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentMoreBinding
import com.zuvy.app.premium.AdManager
import com.zuvy.app.premium.PremiumEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var premiumEngine: PremiumEngine

    @Inject
    lateinit var adManager: AdManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSettings()
        setupPremiumCard()
        setupAdBanner()
    }

    private fun setupSettings() {
        // Theme
        binding.itemTheme.apply {
            icon.setImageResource(R.drawable.ic_theme)
            title.text = getString(R.string.theme)
            arrowOrValue.text = getString(R.string.dark_mode)
        }
        binding.itemTheme.root.setOnClickListener {
            // TODO: Show theme dialog
        }

        // Accent Color
        binding.itemAccentColor.apply {
            icon.setImageResource(R.drawable.ic_palette)
            title.text = getString(R.string.accent_color)
        }
        binding.itemAccentColor.root.setOnClickListener {
            // TODO: Show color picker
        }

        // Playback Speed
        binding.itemPlaybackSpeed.apply {
            icon.setImageResource(R.drawable.ic_speed)
            title.text = getString(R.string.default_playback_speed)
            arrowOrValue.text = "1.0x"
        }
        binding.itemPlaybackSpeed.root.setOnClickListener {
            // TODO: Show speed dialog
        }

        // Gestures
        binding.itemGestures.apply {
            icon.setImageResource(R.drawable.ic_gesture)
            title.text = getString(R.string.gesture_controls)
        }
        binding.itemGestures.root.setOnClickListener {
            // TODO: Navigate to gesture settings
        }

        // Equalizer
        binding.itemEqualizer.apply {
            icon.setImageResource(R.drawable.ic_equalizer)
            title.text = getString(R.string.equalizer)
        }
        binding.itemEqualizer.root.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_equalizerFragment)
        }

        // Scan Storage
        binding.itemScanStorage.apply {
            icon.setImageResource(R.drawable.ic_folder)
            title.text = getString(R.string.scan_storage)
        }
        binding.itemScanStorage.root.setOnClickListener {
            // TODO: Trigger media scan
        }

        // Clear Cache
        binding.itemClearCache.apply {
            icon.setImageResource(R.drawable.ic_cache)
            title.text = getString(R.string.clear_cache)
            arrowOrValue.text = "24 MB"
        }
        binding.itemClearCache.root.setOnClickListener {
            // TODO: Clear cache
        }

        // Exclude Folders
        binding.itemExcludeFolders.apply {
            icon.setImageResource(R.drawable.ic_folder_off)
            title.text = getString(R.string.exclude_folders)
        }
        binding.itemExcludeFolders.root.setOnClickListener {
            // TODO: Show exclude folders
        }

        // App Lock
        binding.itemAppLock.apply {
            icon.setImageResource(R.drawable.ic_lock)
            title.text = getString(R.string.app_lock)
            toggle.visibility = View.VISIBLE
            arrowOrValue.visibility = View.GONE
        }
        binding.itemAppLock.root.setOnClickListener {
            // TODO: Toggle app lock
        }

        // Hide Folders
        binding.itemHideFolders.apply {
            icon.setImageResource(R.drawable.ic_visibility_off)
            title.text = getString(R.string.hide_folders)
        }
        binding.itemHideFolders.root.setOnClickListener {
            // TODO: Show hide folders
        }

        // Rate Us
        binding.itemRateUs.apply {
            icon.setImageResource(R.drawable.ic_star)
            title.text = getString(R.string.rate_us)
            arrowOrValue.visibility = View.GONE
        }
        binding.itemRateUs.root.setOnClickListener {
            // TODO: Open Play Store
        }

        // Share
        binding.itemShare.apply {
            icon.setImageResource(R.drawable.ic_share)
            title.text = getString(R.string.share_app)
            arrowOrValue.visibility = View.GONE
        }
        binding.itemShare.root.setOnClickListener {
            // TODO: Share app
        }

        // Help
        binding.itemHelp.apply {
            icon.setImageResource(R.drawable.ic_help)
            title.text = getString(R.string.help_feedback)
            arrowOrValue.visibility = View.GONE
        }
        binding.itemHelp.root.setOnClickListener {
            // TODO: Show help
        }

        // Version
        binding.itemVersion.apply {
            icon.setImageResource(R.drawable.ic_info)
            title.text = getString(R.string.version)
            arrowOrValue.text = BuildConfig.VERSION_NAME
        }
    }

    private fun setupPremiumCard() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                premiumEngine.isPremium.collect { isPremium ->
                    updatePremiumCard(isPremium)
                }
            }
        }

        binding.premiumCard.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_premiumFragment)
        }
    }

    private fun updatePremiumCard(isPremium: Boolean) {
        if (isPremium) {
            binding.premiumCard.apply {
                strokeColor = resources.getColor(R.color.playlist_accent, null)
                strokeWidth = 2
            }
            // Update the premium card text if needed
            // Could change the card to show "Premium Active" status
        } else {
            binding.premiumCard.apply {
                strokeColor = resources.getColor(R.color.primary, null)
                strokeWidth = 1
            }
        }
    }

    private fun setupAdBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                premiumEngine.isPremium.collect { isPremium ->
                    if (!isPremium) {
                        // Show banner ad for non-premium users
                        showBannerAd()
                    } else {
                        // Hide ad container for premium users
                        binding.adContainer.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun showBannerAd() {
        binding.adContainer.visibility = View.VISIBLE
        adManager.addBannerToContainer(binding.adContainer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
