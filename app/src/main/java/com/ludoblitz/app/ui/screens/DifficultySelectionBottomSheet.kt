package com.ludoblitz.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ludoblitz.app.data.model.BotDifficulty
import com.ludoblitz.app.databinding.BottomSheetDifficultyBinding

/**
 * Bottom Sheet for selecting game difficulty
 */
class DifficultySelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDifficultyBinding? = null
    private val binding get() = _binding!!
    
    private var selectedDifficulty = BotDifficulty.MEDIUM
    private var selectedPlayerCount = 4
    var onDifficultySelected: ((BotDifficulty, Int) -> Unit)? = null

    companion object {
        fun newInstance(onSelected: (BotDifficulty, Int) -> Unit): DifficultySelectionBottomSheet {
            return DifficultySelectionBottomSheet().apply {
                onDifficultySelected = onSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDifficultyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.apply {
            chipEasy.setOnClickListener { selectedDifficulty = BotDifficulty.EASY }
            chipMedium.setOnClickListener { selectedDifficulty = BotDifficulty.MEDIUM }
            chipHard.setOnClickListener { selectedDifficulty = BotDifficulty.HARD }
            chipExpert.setOnClickListener { selectedDifficulty = BotDifficulty.EXPERT }
            
            chip2Players.setOnClickListener { selectedPlayerCount = 2 }
            chip3Players.setOnClickListener { selectedPlayerCount = 3 }
            chip4Players.setOnClickListener { selectedPlayerCount = 4 }
            
            btnStartGame.setOnClickListener {
                onDifficultySelected?.invoke(selectedDifficulty, selectedPlayerCount)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
