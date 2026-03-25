package com.ludoblitz.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ludoblitz.app.data.model.Reward
import com.ludoblitz.app.databinding.BottomSheetSpinWheelBinding

/**
 * Bottom Sheet for the Lucky Spin Wheel
 */
class SpinWheelBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSpinWheelBinding? = null
    private val binding get() = _binding!!
    
    private var isSpinning = false
    var onSpinComplete: ((Reward) -> Unit)? = null

    companion object {
        fun newInstance(onComplete: (Reward) -> Unit): SpinWheelBottomSheet {
            return SpinWheelBottomSheet().apply {
                onSpinComplete = onComplete
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSpinWheelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupSpinWheel()
    }

    private fun setupSpinWheel() {
        binding.btnSpin.setOnClickListener {
            if (!isSpinning) {
                spinWheel()
            }
        }
    }

    private fun spinWheel() {
        isSpinning = true
        
        // Animate the spin wheel
        binding.spinWheel.animate()
            .rotationBy(360f * 5 + (Math.random() * 360).toFloat())
            .setDuration(4000)
            .withEndAction {
                isSpinning = false
                showReward()
            }
            .start()
    }

    private fun showReward() {
        // Generate random reward
        val rewards = listOf(
            Reward(coins = 100),
            Reward(coins = 200),
            Reward(coins = 500),
            Reward(gems = 1),
            Reward(gems = 3),
            Reward(gems = 5)
        )
        
        val reward = rewards.random()
        
        // Show reward animation
        binding.rewardContainer.visibility = View.VISIBLE
        binding.tvRewardCoins.text = if (reward.coins > 0) "+${reward.coins}" else ""
        binding.tvRewardGems.text = if (reward.gems > 0) "+${reward.gems}" else ""
        
        onSpinComplete?.invoke(reward)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
