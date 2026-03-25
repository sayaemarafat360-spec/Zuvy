package com.ludoblitz.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.ludoblitz.app.R
import com.ludoblitz.app.data.model.BotDifficulty
import com.ludoblitz.app.data.model.GameMode
import com.ludoblitz.app.databinding.FragmentPlayBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Play Fragment - Game mode selection screen
 * Shows all available game modes and settings
 */
@AndroidEntryPoint
class PlayFragment : Fragment() {

    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    
    private var selectedPlayerCount = 4
    private var selectedDifficulty = BotDifficulty.MEDIUM
    private var isClassicMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPlayerSelection()
        setupDifficultySelection()
        setupGameModeSelection()
        setupClickListeners()
    }

    private fun setupPlayerSelection() {
        binding.chipGroupPlayers.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedPlayerCount = when (checkedIds.firstOrNull()) {
                R.id.chip2Players -> 2
                R.id.chip3Players -> 3
                R.id.chip4Players -> 4
                else -> 4
            }
            updatePlayerPreview()
        }
    }

    private fun setupDifficultySelection() {
        binding.chipGroupDifficulty.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedDifficulty = when (checkedIds.firstOrNull()) {
                R.id.chipEasy -> BotDifficulty.EASY
                R.id.chipMedium -> BotDifficulty.MEDIUM
                R.id.chipHard -> BotDifficulty.HARD
                R.id.chipExpert -> BotDifficulty.EXPERT
                else -> BotDifficulty.MEDIUM
            }
        }
    }

    private fun setupGameModeSelection() {
        binding.switchGameMode.setOnCheckedChangeListener { _, isChecked ->
            isClassicMode = !isChecked
            binding.tvGameModeDesc.text = if (isClassicMode) {
                getString(R.string.classic_desc)
            } else {
                getString(R.string.modern_desc)
            }
        }
    }

    private fun setupClickListeners() {
        // Local Multiplayer
        binding.cardLocalMultiplayer.setOnClickListener {
            startGame(GameMode.LOCAL)
        }
        
        // Online Multiplayer
        binding.cardOnlineMultiplayer.setOnClickListener {
            findNavController().navigate(R.id.action_play_to_online_lobby)
        }
        
        // VS AI
        binding.cardVsAi.setOnClickListener {
            startGame(GameMode.VS_AI)
        }
        
        // Quick Match
        binding.btnQuickMatch.setOnClickListener {
            findNavController().navigate(R.id.action_play_to_matchmaking)
        }
        
        // Create Room
        binding.btnCreateRoom.setOnClickListener {
            showCreateRoomDialog()
        }
        
        // Join Room
        binding.btnJoinRoom.setOnClickListener {
            showJoinRoomDialog()
        }
        
        // Start Game Button
        binding.btnStartGame.setOnClickListener {
            startGame(GameMode.VS_AI)
        }
    }

    private fun updatePlayerPreview() {
        // Update visual preview of selected players
        val visibility2 = if (selectedPlayerCount >= 2) View.VISIBLE else View.GONE
        val visibility3 = if (selectedPlayerCount >= 3) View.VISIBLE else View.GONE
        val visibility4 = if (selectedPlayerCount >= 4) View.VISIBLE else View.GONE
        
        binding.playerToken2.visibility = visibility2
        binding.playerToken3.visibility = visibility3
        binding.playerToken4.visibility = visibility4
    }

    private fun startGame(mode: GameMode) {
        val intent = Intent(requireContext(), GameActivity::class.java).apply {
            putExtra("game_mode", mode)
            putExtra("player_count", selectedPlayerCount)
            putExtra("bot_difficulty", selectedDifficulty)
            putExtra("classic_mode", isClassicMode)
        }
        startActivity(intent)
        
        requireActivity().overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun showCreateRoomDialog() {
        CreateRoomBottomSheet.newInstance(
            onCreateRoom = { roomName, maxPlayers ->
                // Create room in Firebase
                findNavController().navigate(R.id.action_play_to_lobby)
            }
        ).show(childFragmentManager, "create_room")
    }

    private fun showJoinRoomDialog() {
        JoinRoomBottomSheet.newInstance(
            onJoinRoom = { roomCode ->
                // Join room in Firebase
                findNavController().navigate(R.id.action_play_to_lobby)
            }
        ).show(childFragmentManager, "join_room")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
