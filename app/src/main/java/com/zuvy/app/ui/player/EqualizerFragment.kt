package com.zuvy.app.ui.player

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentEqualizerBinding
import com.zuvy.app.player.EqualizerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EqualizerFragment : Fragment() {

    private var _binding: FragmentEqualizerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var equalizerManager: EqualizerManager

    private val presetChips = mutableListOf<Chip>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEqualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPresets()
        setupBands()
        setupEffects()
        setupToggle()
        updateUI()
    }

    private fun setupToggle() {
        binding.equalizerToggle.isChecked = equalizerManager.isEnabled()
        
        binding.equalizerToggle.setOnCheckedChangeListener { _, isChecked ->
            equalizerManager.setEnabled(isChecked)
            updateBandStates(isChecked)
        }
    }

    private fun setupPresets() {
        val presets = equalizerManager.getPresetNames()
        
        presets.forEach { preset ->
            val chip = Chip(requireContext()).apply {
                text = preset
                isCheckable = true
                isChecked = preset == equalizerManager.getCurrentPreset()
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                
                setOnClickListener {
                    equalizerManager.applyPreset(preset)
                    updateBandValues()
                }
            }
            presetChips.add(chip)
            binding.presetGroup.addView(chip)
        }
    }

    private fun setupBands() {
        val bands = listOf(
            binding.band1 to binding.band1Value,
            binding.band2 to binding.band2Value,
            binding.band3 to binding.band3Value,
            binding.band4 to binding.band4Value,
            binding.band5 to binding.band5Value
        )

        bands.forEachIndexed { index, (seekBar, valueText) ->
            val level = equalizerManager.getBandLevel(index)
            seekBar.progress = level + 15  // Offset by 15 because range is 0-30

            valueText.text = formatDB(level)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val adjustedLevel = progress - 15
                        equalizerManager.setBandLevel(index, adjustedLevel)
                        valueText.text = formatDB(adjustedLevel)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun setupEffects() {
        // Bass Boost
        binding.bassBoostToggle.setOnClickListener {
            val newState = !equalizerManager.isBassBoostEnabled()
            equalizerManager.setBassBoostEnabled(newState)
            updateEffectToggle(binding.bassBoostToggle, newState)
        }

        binding.bassBoostSlider.progress = equalizerManager.getBassBoostStrength() / 10
        binding.bassBoostSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    equalizerManager.setBassBoostStrength(progress * 10)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Virtualizer
        binding.virtualizerToggle.setOnClickListener {
            val newState = !equalizerManager.isVirtualizerEnabled()
            equalizerManager.setVirtualizerEnabled(newState)
            updateEffectToggle(binding.virtualizerToggle, newState)
        }

        binding.virtualizerSlider.progress = equalizerManager.getVirtualizerStrength() / 10
        binding.virtualizerSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    equalizerManager.setVirtualizerStrength(progress * 10)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Reset button
        binding.resetButton.setOnClickListener {
            equalizerManager.resetToDefault()
            updateUI()
        }

        // Back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateUI() {
        binding.equalizerToggle.isChecked = equalizerManager.isEnabled()
        updateBandValues()
        updateEffectToggle(binding.bassBoostToggle, equalizerManager.isBassBoostEnabled())
        updateEffectToggle(binding.virtualizerToggle, equalizerManager.isVirtualizerEnabled())
        binding.bassBoostSlider.progress = equalizerManager.getBassBoostStrength() / 10
        binding.virtualizerSlider.progress = equalizerManager.getVirtualizerStrength() / 10
    }

    private fun updateBandValues() {
        val bands = listOf(binding.band1, binding.band2, binding.band3, binding.band4, binding.band5)
        val values = listOf(binding.band1Value, binding.band2Value, binding.band3Value, binding.band4Value, binding.band5Value)

        bands.forEachIndexed { index, seekBar ->
            val level = equalizerManager.getBandLevel(index)
            seekBar.progress = level + 15
            values[index].text = formatDB(level)
        }
    }

    private fun updateBandStates(enabled: Boolean) {
        val bands = listOf(binding.band1, binding.band2, binding.band3, binding.band4, binding.band5)
        bands.forEach { seekBar ->
            seekBar.isEnabled = enabled
        }
    }

    private fun updateEffectToggle(button: View, enabled: Boolean) {
        button.alpha = if (enabled) 1f else 0.5f
        button.setBackgroundColor(
            if (enabled) resources.getColor(R.color.accent, null)
            else Color.TRANSPARENT
        )
    }

    private fun formatDB(value: Int): String {
        return if (value >= 0) "+${value}dB" else "${value}dB"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
