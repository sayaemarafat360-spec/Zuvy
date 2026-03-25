package com.ludoblitz.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ludoblitz.app.databinding.BottomSheetDailyRewardBinding

/**
 * Bottom Sheet for displaying daily rewards
 */
class DailyRewardBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDailyRewardBinding? = null
    private val binding get() = _binding!!
    
    private var day: Int = 1
    var onClaimClicked: (() -> Unit)? = null

    companion object {
        private const val ARG_DAY = "day"
        
        fun newInstance(day: Int): DailyRewardBottomSheet {
            return DailyRewardBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DAY, day)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = arguments?.getInt(ARG_DAY) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDailyRewardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            tvDay.text = "Day $day"
            
            // Show reward based on day
            val (coins, gems) = getRewardForDay(day)
            tvCoins.text = "+$coins"
            if (gems > 0) {
                tvGems.text = "+$gems"
                tvGems.visibility = View.VISIBLE
            } else {
                tvGems.visibility = View.GONE
            }
            
            btnClaim.setOnClickListener {
                onClaimClicked?.invoke()
                dismiss()
            }
        }
    }

    private fun getRewardForDay(day: Int): Pair<Int, Int> {
        return when (day) {
            1 -> Pair(100, 0)
            2 -> Pair(150, 0)
            3 -> Pair(200, 1)
            4 -> Pair(250, 0)
            5 -> Pair(300, 2)
            6 -> Pair(400, 0)
            7 -> Pair(500, 5)
            else -> Pair(100, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
