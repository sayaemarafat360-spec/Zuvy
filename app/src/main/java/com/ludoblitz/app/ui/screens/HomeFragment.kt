package com.ludoblitz.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ludoblitz.app.R
import com.ludoblitz.app.data.model.BotDifficulty
import com.ludoblitz.app.data.model.GameMode
import com.ludoblitz.app.data.model.User
import com.ludoblitz.app.databinding.FragmentHomeBinding
import com.ludoblitz.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home Fragment - Main landing screen after login
 * Shows quick play options, daily rewards, spin wheel, and stats
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupClickListeners()
        observeData()
        startAnimations()
    }

    private fun setupViews() {
        // Setup card backgrounds
        binding.cardQuickPlay.setBackgroundResource(R.drawable.card_gradient_primary)
        binding.cardOnline.setBackgroundResource(R.drawable.card_gradient_secondary)
        binding.cardVsAi.setBackgroundResource(R.drawable.card_gradient_accent)
    }

    private fun setupClickListeners() {
        // Quick Play
        binding.cardQuickPlay.setOnClickListener {
            startGame(GameMode.LOCAL, 4, BotDifficulty.MEDIUM)
        }
        
        // Online Multiplayer
        binding.cardOnline.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_online_lobby)
        }
        
        // VS AI
        binding.cardVsAi.setOnClickListener {
            showDifficultySelection()
        }
        
        // Spin Wheel
        binding.cardSpinWheel.setOnClickListener {
            showSpinWheel()
        }
        
        // Daily Reward
        binding.cardDailyReward.setOnClickListener {
            viewModel.claimDailyReward(1) // Will be dynamic
        }
        
        // Stats
        binding.cardStats.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
        
        // Leaderboard
        binding.cardLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_leaderboard)
        }
        
        // Friends
        binding.cardFriends.setOnClickListener {
            startActivity(Intent(requireContext(), FriendsActivity::class.java))
        }
    }

    private fun observeData() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let { updateUserInfo(it) }
        }
    }

    private fun updateUserInfo(user: User) {
        binding.apply {
            tvWelcome.text = "Welcome back, ${user.displayName}!"
            tvCoins.text = formatNumber(user.coins)
            tvGems.text = formatNumber(user.gems)
            tvWins.text = user.totalWins.toString()
            tvWinRate.text = "${String.format("%.1f", user.getWinRate())}%"
            tvLevel.text = "Level ${user.level}"
            
            // XP Progress
            progressXp.progress = (user.getXpProgress() * 100).toInt()
        }
    }

    private fun startAnimations() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Stagger card animations
            val cards = listOf(
                binding.cardQuickPlay,
                binding.cardOnline,
                binding.cardVsAi,
                binding.cardSpinWheel,
                binding.cardDailyReward,
                binding.cardStats
            )
            
            cards.forEachIndexed { index, card ->
                card.alpha = 0f
                card.translationY = 50f
            }
            
            delay(100)
            
            cards.forEachIndexed { index, card ->
                delay(80)
                card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
        }
    }

    private fun startGame(mode: GameMode, playerCount: Int, difficulty: BotDifficulty) {
        val intent = Intent(requireContext(), GameActivity::class.java).apply {
            putExtra("game_mode", mode)
            putExtra("player_count", playerCount)
            putExtra("bot_difficulty", difficulty)
        }
        startActivity(intent)
        
        // Override transition
        requireActivity().overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun showDifficultySelection() {
        val bottomSheet = DifficultySelectionBottomSheet.newInstance(
            onDifficultySelected = { difficulty, playerCount ->
                startGame(GameMode.VS_AI, playerCount, difficulty)
            }
        )
        bottomSheet.show(childFragmentManager, "difficulty_selection")
    }

    private fun showSpinWheel() {
        val bottomSheet = SpinWheelBottomSheet.newInstance(
            onSpinComplete = { reward ->
                // Handle reward
            }
        )
        bottomSheet.show(childFragmentManager, "spin_wheel")
    }

    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> number.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
