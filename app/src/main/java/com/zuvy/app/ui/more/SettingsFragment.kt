package com.zuvy.app.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentSettingsBinding
import com.zuvy.app.databinding.DialogThemePickerBinding
import com.zuvy.app.databinding.DialogAccentColorBinding
import com.zuvy.app.databinding.DialogSeekDurationBinding
import com.zuvy.app.utils.PreferenceManager
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSettings()
        loadSettings()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSettings() {
        // Theme
        binding.themeSetting.setOnClickListener {
            showThemeDialog()
        }

        // Accent Color
        binding.accentColorSetting.setOnClickListener {
            showAccentColorDialog()
        }

        // Playback Speed
        binding.playbackSpeedSetting.setOnClickListener {
            showPlaybackSpeedDialog()
        }

        // Seek Duration
        binding.seekDurationSetting.setOnClickListener {
            showSeekDurationDialog()
        }

        // Clear Cache
        binding.clearCacheSetting.setOnClickListener {
            clearCache()
        }

        // App Lock
        binding.appLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveAppLock(isChecked)
            if (isChecked) {
                ToastUtils.showInfo(requireContext(), "App lock enabled")
            } else {
                ToastUtils.showInfo(requireContext(), "App lock disabled")
            }
        }

        // Rate App
        binding.rateAppSetting.setOnClickListener {
            openPlayStore()
        }

        // Share App
        binding.shareAppSetting.setOnClickListener {
            shareApp()
        }

        // Privacy Policy
        binding.privacyPolicySetting.setOnClickListener {
            openPrivacyPolicy()
        }
    }

    private fun loadSettings() {
        // Theme
        val themeMode = preferenceManager.getThemeMode()
        val themeName = when (themeMode) {
            0 -> "System Default"
            1 -> "Dark"
            2 -> "Light"
            else -> "System Default"
        }
        binding.themeValue.text = themeName

        // Playback Speed
        val speed = preferenceManager.getPlaybackSpeed()
        binding.playbackSpeedValue.text = "${speed}x"

        // Seek Duration
        val seekDuration = preferenceManager.getSeekDuration()
        binding.seekDurationValue.text = "${seekDuration}s"

        // Cache Size (placeholder)
        binding.cacheSizeValue.text = "24 MB"

        // App Lock
        binding.appLockSwitch.isChecked = preferenceManager.isAppLockEnabled()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System Default", "Dark", "Light")
        val currentTheme = preferenceManager.getThemeMode()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Theme")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                saveTheme(which)
                dialog.dismiss()
            }
            .show()
    }

    private fun saveTheme(themeMode: Int) {
        preferenceManager.setThemeMode(themeMode)
        
        val mode = when (themeMode) {
            0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            2 -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        
        val themeName = when (themeMode) {
            0 -> "System Default"
            1 -> "Dark"
            2 -> "Light"
            else -> "System Default"
        }
        binding.themeValue.text = themeName
        ToastUtils.showSuccess(requireContext(), "Theme changed to $themeName")
    }

    private fun showAccentColorDialog() {
        val colors = listOf(
            "#6C63FF" to "Purple",
            "#00BCD4" to "Cyan",
            "#4CAF50" to "Green",
            "#FF9800" to "Orange",
            "#E91E63" to "Pink",
            "#2196F3" to "Blue"
        )
        
        val colorNames = colors.map { it.second }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accent Color")
            .setItems(colorNames) { _, which ->
                saveAccentColor(colors[which].first, colors[which].second)
            }
            .show()
    }

    private fun saveAccentColor(color: String, name: String) {
        preferenceManager.setAccentColor(color)
        binding.accentColorValue.text = name
        ToastUtils.showSuccess(requireContext(), "Accent color changed to $name")
    }

    private fun showPlaybackSpeedDialog() {
        val speeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val speedLabels = speeds.map { "${it}x" }.toTypedArray()
        val currentSpeed = preferenceManager.getPlaybackSpeed()
        val currentIndex = speeds.indexOfFirst { it == currentSpeed }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Default Playback Speed")
            .setSingleChoiceItems(speedLabels, currentIndex) { dialog, which ->
                preferenceManager.setPlaybackSpeed(speeds[which])
                binding.playbackSpeedValue.text = "${speeds[which]}x"
                dialog.dismiss()
                ToastUtils.showSuccess(requireContext(), "Default speed set to ${speeds[which]}x")
            }
            .show()
    }

    private fun showSeekDurationDialog() {
        val durations = intArrayOf(5, 10, 15, 20, 30)
        val durationLabels = durations.map { "${it} seconds" }.toTypedArray()
        val currentDuration = preferenceManager.getSeekDuration()
        val currentIndex = durations.indexOf(currentDuration).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seek Duration")
            .setSingleChoiceItems(durationLabels, currentIndex) { dialog, which ->
                preferenceManager.setSeekDuration(durations[which])
                binding.seekDurationValue.text = "${durations[which]}s"
                dialog.dismiss()
                ToastUtils.showSuccess(requireContext(), "Seek duration set to ${durations[which]}s")
            }
            .show()
    }

    private fun clearCache() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cache?")
            .setMessage("This will clear temporary files and free up storage space.")
            .setPositiveButton("Clear") { _, _ ->
                // Clear cache
                try {
                    requireContext().cacheDir.deleteRecursively()
                    binding.cacheSizeValue.text = "0 MB"
                    ToastUtils.showSuccess(requireContext(), "Cache cleared successfully")
                } catch (e: Exception) {
                    ToastUtils.showError(requireContext(), "Failed to clear cache")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAppLock(enabled: Boolean) {
        preferenceManager.setAppLock(enabled)
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
            startActivity(intent)
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Zuvy Media Player")
            putExtra(Intent.EXTRA_TEXT, "Check out Zuvy Media Player! https://play.google.com/store/apps/details?id=${requireContext().packageName}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
        ToastUtils.showInfo(requireContext(), "Share this app with friends!")
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zuvy.app/privacy"))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
