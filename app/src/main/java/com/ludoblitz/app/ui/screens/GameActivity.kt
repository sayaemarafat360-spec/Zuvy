package com.ludoblitz.app.ui.screens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ludoblitz.app.R
import com.ludoblitz.app.data.model.*
import com.ludoblitz.app.databinding.ActivityGameBinding
import com.ludoblitz.app.databinding.DialogGameOverBinding
import com.ludoblitz.app.databinding.DialogPauseMenuBinding
import com.ludoblitz.app.ui.viewmodel.GameEvent
import com.ludoblitz.app.ui.viewmodel.GameState
import com.ludoblitz.app.ui.viewmodel.GameViewModel
import com.ludoblitz.app.ui.viewmodel.TokenAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Game Activity - Main gameplay screen for Ludo Blitz
 * Handles the game board, dice rolling, token movement, and animations
 */
@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()
    
    // Views
    private lateinit var diceView: ImageView
    private lateinit var boardView: View
    private lateinit var tokenViews: MutableMap<String, ImageView>
    
    // Animation
    private var diceAnimation: ObjectAnimator? = null
    
    // Sound
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initViews()
        setupGame()
        setupObservers()
        setupClickListeners()
        setupBackPressedHandler()
    }

    private fun initViews() {
        diceView = binding.diceView
        boardView = binding.boardView
        tokenViews = mutableMapOf()
    }

    private fun setupGame() {
        // Get game parameters from intent
        val gameMode = intent.getSerializableExtra("game_mode") as? GameMode ?: GameMode.LOCAL
        val playerCount = intent.getIntExtra("player_count", 4)
        val botDifficulty = intent.getSerializableExtra("bot_difficulty") as? BotDifficulty 
            ?: BotDifficulty.MEDIUM
        val userId = intent.getStringExtra("user_id")
        
        when (gameMode) {
            GameMode.LOCAL -> {
                viewModel.initLocalGame(playerCount, botDifficulty, userId)
            }
            GameMode.VS_AI -> {
                viewModel.initVsAIGame(playerCount, botDifficulty, userId)
            }
            GameMode.ONLINE -> {
                val gameId = intent.getStringExtra("game_id") ?: return
                viewModel.initOnlineGame(gameId, userId ?: "")
            }
        }
    }

    private fun setupObservers() {
        viewModel.game.observe(this) { game ->
            updateGameUI(game)
        }
        
        viewModel.gameState.observe(this) { state ->
            handleGameState(state)
        }
        
        viewModel.diceValue.observe(this) { value ->
            updateDiceUI(value)
        }
        
        viewModel.isRolling.observe(this) { isRolling ->
            if (isRolling) {
                startDiceRollAnimation()
            }
        }
        
        viewModel.isPlayerTurn.observe(this) { isPlayerTurn ->
            updateTurnIndicator(isPlayerTurn)
        }
        
        viewModel.validMoves.observe(this) { moves ->
            highlightValidMoves(moves)
        }
        
        viewModel.animateToken.observe(this) { animation ->
            animation?.let { animateToken(it) }
        }
        
        viewModel.gameEvent.observe(this) { event ->
            handleGameEvent(event)
        }
    }

    private fun setupClickListeners() {
        // Dice click
        binding.diceContainer.setOnClickListener {
            if (viewModel.isPlayerTurn.value == true) {
                viewModel.rollDice()
            }
        }
        
        // Pause button
        binding.btnPause.setOnClickListener {
            showPauseMenu()
        }
        
        // Auto-select token button
        binding.btnAutoSelect.setOnClickListener {
            viewModel.autoSelectToken()
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showPauseMenu()
            }
        })
    }

    private fun updateGameUI(game: Game) {
        // Update current player indicator
        val currentPlayer = game.getCurrentPlayer()
        binding.tvCurrentPlayer.text = currentPlayer?.name ?: ""
        
        // Update player info cards
        updatePlayerCards(game.players)
        
        // Update tokens on board
        updateTokensOnBoard(game)
    }

    private fun updatePlayerCards(players: List<Player>) {
        players.forEachIndexed { index, player ->
            val card = when (player.color) {
                TokenColor.RED -> binding.playerCardRed
                TokenColor.GREEN -> binding.playerCardGreen
                TokenColor.YELLOW -> binding.playerCardYellow
                TokenColor.BLUE -> binding.playerCardBlue
            }
            
            card.findViewById<TextView>(R.id.tv_player_name)?.text = player.name
            card.findViewById<TextView>(R.id.tv_tokens_home)?.text = 
                "${player.getFinishedTokensCount()}/4"
            
            // Highlight current player
            val alpha = if (index == viewModel.currentPlayerIndex.value) 1f else 0.5f
            card.alpha = alpha
        }
    }

    private fun updateTokensOnBoard(game: Game) {
        // This would update the actual token positions on the board
        // Implementation depends on how you've set up your board view
        game.players.forEach { player ->
            player.tokens.forEach { token ->
                val tokenKey = "${player.color}_${token.id}"
                val tokenView = tokenViews[tokenKey]
                
                if (token.isHome) {
                    tokenView?.visibility = View.INVISIBLE
                } else if (token.isInBase()) {
                    // Position in base
                    positionTokenInBase(tokenView, player.color, token.id)
                } else {
                    // Position on board
                    positionTokenOnBoard(tokenView, token.position)
                }
            }
        }
    }

    private fun positionTokenInBase(tokenView: ImageView?, color: TokenColor, tokenId: Int) {
        // Position token in the appropriate base area
    }

    private fun positionTokenOnBoard(tokenView: ImageView?, position: Int) {
        // Position token on the board at the given position
    }

    private fun handleGameState(state: GameState) {
        when (state) {
            is GameState.WaitingForRoll -> {
                binding.tvTurnStatus.text = getString(R.string.your_turn)
                binding.diceContainer.isClickable = viewModel.isPlayerTurn.value == true
                
                // Enable dice glow effect
                binding.diceContainer.setBackgroundResource(R.drawable.dice_glow_background)
            }
            is GameState.Rolling -> {
                binding.tvTurnStatus.text = getString(R.string.loading)
                binding.diceContainer.isClickable = false
            }
            is GameState.SelectingMove -> {
                binding.tvTurnStatus.text = getString(R.string.select_token)
                binding.diceContainer.isClickable = false
                
                // Remove dice glow
                binding.diceContainer.setBackgroundResource(0)
            }
            is GameState.Moving -> {
                binding.tvTurnStatus.text = getString(R.string.loading)
            }
            is GameState.TurnComplete -> {
                // Handled by game event
            }
            is GameState.GameOver -> {
                showGameOverDialog(state.winner, state.rankings)
            }
        }
    }

    private fun updateDiceUI(value: Int) {
        val drawableRes = when (value) {
            1 -> R.drawable.ic_dice_1
            2 -> R.drawable.ic_dice_2
            3 -> R.drawable.ic_dice_3
            4 -> R.drawable.ic_dice_4
            5 -> R.drawable.ic_dice_5
            6 -> R.drawable.ic_dice_6
            else -> R.drawable.ic_dice
        }
        
        diceView.setImageResource(drawableRes)
    }

    private fun startDiceRollAnimation() {
        val rotateAnimation = RotateAnimation(
            0f, 360f * 3,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotateAnimation.duration = 800
        rotateAnimation.interpolator = AccelerateDecelerateInterpolator()
        
        diceView.startAnimation(rotateAnimation)
    }

    private fun updateTurnIndicator(isPlayerTurn: Boolean) {
        binding.tvTurnStatus.text = if (isPlayerTurn) {
            getString(R.string.your_turn)
        } else {
            getString(R.string.waiting_for, viewModel.game.value?.getCurrentPlayer()?.name)
        }
        
        // Enable/disable dice interaction
        binding.diceContainer.isClickable = isPlayerTurn
        
        // Visual feedback
        val color = if (isPlayerTurn) {
            ContextCompat.getColor(this, R.color.success)
        } else {
            ContextCompat.getColor(this, R.color.text_secondary)
        }
        binding.tvTurnStatus.setTextColor(color)
    }

    private fun highlightValidMoves(moves: List<ValidMove>) {
        // Clear previous highlights
        tokenViews.values.forEach { it.clearAnimation() }
        
        // Highlight valid tokens
        val currentPlayer = viewModel.game.value?.getCurrentPlayer() ?: return
        
        moves.forEach { move ->
            val token = currentPlayer.tokens[move.tokenIndex]
            val tokenKey = "${currentPlayer.color}_${token.id}"
            tokenViews[tokenKey]?.apply {
                // Add pulsing animation
                animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(300)
                    .setInterpolator(BounceInterpolator())
                    .withEndAction {
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
                
                // Make clickable
                setOnClickListener {
                    viewModel.moveToken(move.tokenIndex)
                }
            }
        }
    }

    private fun animateToken(animation: TokenAnimation) {
        val currentPlayer = viewModel.game.value?.players[animation.playerIndex] ?: return
        val tokenKey = "${currentPlayer.color}_${animation.tokenIndex + 1}"
        val tokenView = tokenViews[tokenKey] ?: return
        
        // Animate token movement
        val startX = tokenView.x
        val startY = tokenView.y
        
        // Calculate new position
        val (endX, endY) = calculateTokenPosition(
            animation.toPosition,
            currentPlayer.color
        )
        
        // Animate with path
        tokenView.animate()
            .x(endX)
            .y(endY)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                viewModel.clearAnimation()
            }
            .start()
    }

    private fun calculateTokenPosition(position: Int, color: TokenColor): Pair<Float, Float> {
        // Calculate screen coordinates for board position
        // This would depend on your board implementation
        return Pair(0f, 0f)
    }

    private fun handleGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.None -> {}
            is GameEvent.NoValidMoves -> {
                Toast.makeText(
                    this,
                    getString(R.string.no_valid_moves),
                    Toast.LENGTH_SHORT
                ).show()
            }
            is GameEvent.RolledSix -> {
                showCelebrationAnimation()
                Toast.makeText(
                    this,
                    getString(R.string.rolled_six),
                    Toast.LENGTH_SHORT
                ).show()
            }
            is GameEvent.BonusTurn -> {
                Toast.makeText(
                    this,
                    getString(R.string.bonus_turn),
                    Toast.LENGTH_SHORT
                ).show()
            }
            is GameEvent.TokenCaptured -> {
                showCaptureAnimation()
                Toast.makeText(
                    this,
                    getString(R.string.token_captured),
                    Toast.LENGTH_SHORT
                ).show()
            }
            is GameEvent.TokenReachedHome -> {
                showHomeAnimation()
            }
            is GameEvent.GameOver -> {
                // Already handled by GameState
            }
            is GameEvent.RewardEarned -> {
                showRewardAnimation(event.coins, event.xp)
            }
            is GameEvent.PlayerDisconnected -> {
                Toast.makeText(
                    this,
                    "${event.playerId} disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.clearEvent()
    }

    private fun showCelebrationAnimation() {
        binding.celebrationLottie.apply {
            visibility = View.VISIBLE
            setAnimation("celebration.json")
            playAnimation()
            
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
        }
    }

    private fun showCaptureAnimation() {
        binding.captureLottie.apply {
            visibility = View.VISIBLE
            setAnimation("capture.json")
            playAnimation()
            
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
        }
    }

    private fun showHomeAnimation() {
        binding.homeLottie.apply {
            visibility = View.VISIBLE
            setAnimation("token_home.json")
            playAnimation()
            
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
        }
    }

    private fun showRewardAnimation(coins: Long, xp: Long) {
        // Show floating reward animation
        lifecycleScope.launch {
            binding.rewardContainer.visibility = View.VISIBLE
            binding.tvRewardCoins.text = "+$coins"
            binding.tvRewardXp.text = "+$xp XP"
            
            binding.rewardContainer.animate()
                .translationY(-100f)
                .alpha(0f)
                .setDuration(1500)
                .withEndAction {
                    binding.rewardContainer.visibility = View.GONE
                    binding.rewardContainer.translationY = 0f
                    binding.rewardContainer.alpha = 1f
                }
                .start()
        }
    }

    private fun showPauseMenu() {
        viewModel.pauseGame()
        
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogPauseMenuBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialogBinding.btnResume.setOnClickListener {
            viewModel.resumeGame()
            dialog.dismiss()
        }
        
        dialogBinding.btnRestart.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Restart Game?")
                .setMessage("Are you sure you want to restart? Current progress will be lost.")
                .setPositiveButton("Restart") { _, _ ->
                    setupGame()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        dialogBinding.btnQuit.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Quit Game?")
                .setMessage("Are you sure you want to quit? You'll lose this game.")
                .setPositiveButton("Quit") { _, _ ->
                    viewModel.quitGame()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        dialogBinding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        dialog.show()
    }

    private fun showGameOverDialog(winner: Player, rankings: List<Player>) {
        val dialogBinding = DialogGameOverBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        // Set winner info
        dialogBinding.tvWinnerName.text = winner.name
        dialogBinding.tvWinnerCoins.text = "+100"
        dialogBinding.tvWinnerXp.text = "+50 XP"
        
        // Show confetti
        dialogBinding.confettiLottie.setAnimation("confetti.json")
        dialogBinding.confettiLottie.playAnimation()
        
        dialogBinding.btnPlayAgain.setOnClickListener {
            setupGame()
            dialog.dismiss()
        }
        
        dialogBinding.btnHome.setOnClickListener {
            finish()
        }
        
        dialogBinding.btnShare.setOnClickListener {
            shareResult(winner)
        }
        
        dialog.show()
    }

    private fun shareResult(winner: Player) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "I just won a Ludo Blitz game! 🎲🏆 Can you beat me? Download now and challenge your friends!"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.quitGame()
        mediaPlayer?.release()
        diceAnimation?.cancel()
    }
}
