package com.zuvy.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.zuvy.app.BuildConfig
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentMoreBinding
import com.zuvy.app.databinding.ItemSettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupSettings() {
        // Theme
        binding.itemTheme.apply {
            icon.setImageResource(R.drawable.ic_theme)
            title.text = getString(R.string.theme)
            arrowOrValue.text = getString(R.string.dark_mode)
        }

        // Accent Color
        binding.itemAccentColor.apply {
            icon.setImageResource(R.drawable.ic_palette)
            title.text = getString(R.string.accent_color)
        }

        // Playback Speed
        binding.itemPlaybackSpeed.apply {
            icon.setImageResource(R.drawable.ic_speed)
            title.text = getString(R.string.default_playback_speed)
            arrowOrValue.text = "1.0x"
        }

        // Gestures
        binding.itemGestures.apply {
            icon.setImageResource(R.drawable.ic_gesture)
            title.text = getString(R.string.gesture_controls)
        }

        // Equalizer
        binding.itemEqualizer.apply {
            icon.setImageResource(R.drawable.ic_equalizer)
            title.text = getString(R.string.equalizer)
        }

        // Scan Storage
        binding.itemScanStorage.apply {
            icon.setImageResource(R.drawable.ic_folder)
            title.text = getString(R.string.scan_storage)
        }

        // Clear Cache
        binding.itemClearCache.apply {
            icon.setImageResource(R.drawable.ic_cache)
            title.text = getString(R.string.clear_cache)
            arrowOrValue.text = "24 MB"
        }

        // Exclude Folders
        binding.itemExcludeFolders.apply {
            icon.setImageResource(R.drawable.ic_folder_off)
            title.text = getString(R.string.exclude_folders)
        }

        // App Lock
        binding.itemAppLock.apply {
            icon.setImageResource(R.drawable.ic_lock)
            title.text = getString(R.string.app_lock)
            toggle.visibility = View.VISIBLE
            arrowOrValue.visibility = View.GONE
        }

        // Hide Folders
        binding.itemHideFolders.apply {
            icon.setImageResource(R.drawable.ic_visibility_off)
            title.text = getString(R.string.hide_folders)
        }

        // Rate Us
        binding.itemRateUs.apply {
            icon.setImageResource(R.drawable.ic_star)
            title.text = getString(R.string.rate_us)
            arrowOrValue.visibility = View.GONE
        }

        // Share
        binding.itemShare.apply {
            icon.setImageResource(R.drawable.ic_share)
            title.text = getString(R.string.share_app)
            arrowOrValue.visibility = View.GONE
        }

        // Help
        binding.itemHelp.apply {
            icon.setImageResource(R.drawable.ic_help)
            title.text = getString(R.string.help_feedback)
            arrowOrValue.visibility = View.GONE
        }

        // Version
        binding.itemVersion.apply {
            icon.setImageResource(R.drawable.ic_info)
            title.text = getString(R.string.version)
            arrowOrValue.text = BuildConfig.VERSION_NAME
        }

        // Click listeners
        binding.premiumCard.setOnClickListener {
            // TODO: Show premium dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
