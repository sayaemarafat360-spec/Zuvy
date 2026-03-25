package com.zuvy.app.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.databinding.BottomSheetQuickSettingsBinding
import com.zuvy.app.databinding.DialogPlaybackSpeedBinding
import com.zuvy.app.player.VideoFilters
import com.zuvy.app.utils.ToastUtils

class QuickSettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuickSettingsBinding? = null
    private val binding get() = _binding!!
    
    // Current values
    private var currentSpeed = 1.0f
    private var currentAspect = "Fit"
    private var currentFilter = "None"
    private var currentQuality = "Auto"
    private var repeatActive = false
    
    // Callbacks
    var onSpeedChanged: ((Float) -> Unit)? = null
    var onAspectChanged: ((String) -> Unit)? = null
    var onAudioTrackSelected: (() -> Unit)? = null
    var onSubtitleSelected: (() -> Unit)? = null
    var onFilterChanged: ((VideoFilters.Filter) -> Unit)? = null
    var onQualityChanged: ((String) -> Unit)? = null
    var onRepeatToggled: ((Boolean) -> Unit)? = null
    var onEqualizerOpened: (() -> Unit)? = null
    var onTimerSet: (() -> Unit)? = null
    var onScreenshot: (() -> Unit)? = null
    var onGifRecord: (() -> Unit)? = null
    var onBookmark: (() -> Unit)? = null

    companion object {
        fun newInstance(
            speed: Float = 1.0f,
            aspect: String = "Fit",
            filter: String = "None",
            quality: String = "Auto"
        ): QuickSettingsBottomSheet {
            return QuickSettingsBottomSheet().apply {
                arguments = Bundle().apply {
                    putFloat("speed", speed)
                    putString("aspect", aspect)
                    putString("filter", filter)
                    putString("quality", quality)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetQuickSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        currentSpeed = arguments?.getFloat("speed", 1.0f) ?: 1.0f
        currentAspect = arguments?.getString("aspect") ?: "Fit"
        currentFilter = arguments?.getString("filter") ?: "None"
        currentQuality = arguments?.getString("quality") ?: "Auto"
        
        updateUI()
        setupClickListeners()
    }
    
    private fun updateUI() {
        binding.speedValue.text = "${currentSpeed}x"
        binding.aspectValue.text = currentAspect
        binding.filterValue.text = currentFilter
        binding.qualityValue.text = currentQuality
        binding.repeatValue.text = if (repeatActive) "On" else "Off"
    }

    private fun setupClickListeners() {
        // Speed
        binding.speedSetting.setOnClickListener {
            showSpeedDialog()
        }
        
        // Aspect Ratio
        binding.aspectSetting.setOnClickListener {
            showAspectDialog()
        }
        
        // Audio Track
        binding.audioSetting.setOnClickListener {
            dismiss()
            onAudioTrackSelected?.invoke()
        }
        
        // Subtitle
        binding.subtitleSetting.setOnClickListener {
            dismiss()
            onSubtitleSelected?.invoke()
        }
        
        // Filter
        binding.filterSetting.setOnClickListener {
            showFilterDialog()
        }
        
        // Quality
        binding.qualitySetting.setOnClickListener {
            dismiss()
            onQualityChanged?.invoke(currentQuality)
        }
        
        // A-B Repeat
        binding.repeatSetting.setOnClickListener {
            repeatActive = !repeatActive
            binding.repeatValue.text = if (repeatActive) "On" else "Off"
            onRepeatToggled?.invoke(repeatActive)
            ToastUtils.showInfo(requireContext(), if (repeatActive) "Set point A" else "A-B Repeat off")
        }
        
        // Equalizer
        binding.equalizerSetting.setOnClickListener {
            dismiss()
            onEqualizerOpened?.invoke()
        }
        
        // Timer
        binding.timerSetting.setOnClickListener {
            dismiss()
            onTimerSet?.invoke()
        }
        
        // Screenshot
        binding.screenshotBtn.setOnClickListener {
            dismiss()
            onScreenshot?.invoke()
        }
        
        // GIF
        binding.gifBtn.setOnClickListener {
            dismiss()
            onGifRecord?.invoke()
        }
        
        // Bookmark
        binding.bookmarkBtn.setOnClickListener {
            dismiss()
            onBookmark?.invoke()
        }
    }
    
    private fun showSpeedDialog() {
        val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f)
        val labels = speeds.map { "${it}x" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Playback Speed")
            .setSingleChoiceItems(labels, speeds.indexOfFirst { it == currentSpeed }) { dialog, which ->
                currentSpeed = speeds[which]
                binding.speedValue.text = "${currentSpeed}x"
                onSpeedChanged?.invoke(currentSpeed)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAspectDialog() {
        val aspects = arrayOf("Fit", "Fill", "Crop", "16:9", "4:3", "1:1", "Original")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aspect Ratio")
            .setSingleChoiceItems(aspects, aspects.indexOf(currentAspect)) { dialog, which ->
                currentAspect = aspects[which]
                binding.aspectValue.text = currentAspect
                onAspectChanged?.invoke(currentAspect)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showFilterDialog() {
        val filters = VideoFilters.ALL_PRESETS
        val filterNames = filters.map { it.name }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Video Filter")
            .setSingleChoiceItems(filterNames, filters.indexOfFirst { it.name == currentFilter }) { dialog, which ->
                val selectedFilter = filters[which]
                currentFilter = selectedFilter.name
                binding.filterValue.text = currentFilter
                onFilterChanged?.invoke(selectedFilter)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
