package com.ludoblitz.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoblitz.app.LudoBlitzApp
import com.ludoblitz.app.data.model.*
import com.ludoblitz.app.data.repository.FirebaseRepository
import com.ludoblitz.app.domain.ai.AIFactory
import com.ludoblitz.app.domain.ai.AIDecision
import com.ludoblitz.app.domain.gamelogic.*
import com.ludoblitz.app.utils.AdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Game ViewModel for Ludo Blitz
 * Handles game state, player turns, dice rolling, and AI turns
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val adManager: AdManager,
    private val gameEngine: LudoGameEngine
) : ViewModel() {

    // Game state
    private val _game = MutableLiveData<Game>()
    val game: LiveData<Game> = _game

    // UI State
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    // Dice state
    private val _diceValue = MutableLiveData<Int>()
    val diceValue: LiveData<Int> = _diceValue

    private val _isRolling = MutableLiveData<Boolean>()
    val isRolling: LiveData<Boolean> = _isRolling

    // Current turn info
    private val _currentPlayerIndex = MutableLiveData<Int>()
    val currentPlayerIndex: LiveData<Int> = _currentPlayerIndex

    private val _isPlayerTurn = MutableLiveData<Boolean>()
    val isPlayerTurn: LiveData<Boolean> = _isPlayerTurn

    // Valid moves for selection
    private val _validMoves = MutableLiveData<List<ValidMove>>()
    val validMoves: LiveData<List<ValidMove>> = _validMoves

    // Animation triggers
    private val _animateToken = MutableLiveData<TokenAnimation?>()
    val animateToken: LiveData<TokenAnimation?> = _animateToken

    private val _animateDice = MutableLiveData<Boolean>()
    val animateDice: LiveData<Boolean> = _animateDice

    // Game events
    private val _gameEvent = MutableLiveData<GameEvent>()
    val gameEvent: LiveData<GameEvent> = _gameEvent

    // AI opponents
    private val aiPlayers = mutableMapOf<String, LudoAI>()

    // Game listener job for online games
    private var gameListenerJob: Job? = null

    // Sound effects
    private var soundEnabled = true

    // Current user ID (for determining player's turn in online games)
    private var currentUserId: String? = null

    /**
     * Initialize a new local game
     */
    fun initLocalGame(
        playerCount: Int,
        botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
        userId: String? = null
    ) {
        currentUserId = userId
        
        val players = mutableListOf<Player>()
        val colors = TokenColor.values().take(playerCount)
        
        colors.forEachIndexed { index, color ->
            val tokens = (1..4).map { tokenId ->
                Token(id = tokenId, color = color)
            }
            
            val isBot = index > 0 // First player is always human
            val playerName = if (isBot) "Bot ${index}" else "You"
            
            players.add(
                Player(
                    id = if (isBot) "bot_$index" else (userId ?: "player_1"),
                    name = playerName,
                    avatarUrl = "",
                    color = color,
                    tokens = tokens,
                    isBot = isBot,
                    botDifficulty = if (isBot) botDifficulty else BotDifficulty.EASY
                )
            )
            
            // Initialize AI for bot players
            if (isBot) {
                aiPlayers["bot_$index"] = AIFactory.createAI(botDifficulty, gameEngine)
            }
        }
        
        val newGame = gameEngine.createNewGame(
            players = players,
            gameMode = GameMode.LOCAL,
            rules = GameRules()
        )
        
        _game.value = newGame
        _currentPlayerIndex.value = 0
        _isPlayerTurn.value = true
        _gameState.value = GameState.WaitingForRoll
        
        playSound("game_start")
    }

    /**
     * Initialize an online game
     */
    fun initOnlineGame(gameId: String, userId: String) {
        currentUserId = userId
        
        gameListenerJob?.cancel()
        gameListenerJob = viewModelScope.launch {
            firebaseRepository.observeOnlineGame(gameId).collect { game ->
                game?.let {
                    _game.value = it
                    _currentPlayerIndex.value = it.currentPlayerIndex
                    
                    // Check if it's current user's turn
                    val currentPlayer = it.getCurrentPlayer()
                    _isPlayerTurn.value = currentPlayer?.id == userId
                    
                    when {
                        it.gameStatus == GameStatus.FINISHED -> {
                            _gameState.value = GameState.GameOver(
                                winner = it.players.first { p -> p.hasWon() },
                                rankings = emptyList()
                            )
                        }
                        currentPlayer?.id == userId && !currentPlayer.hasRolled -> {
                            _gameState.value = GameState.WaitingForRoll
                        }
                        else -> {
                            // Waiting for other player
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize a game vs AI
     */
    fun initVsAIGame(
        playerCount: Int,
        difficulty: BotDifficulty,
        userId: String?
    ) {
        currentUserId = userId
        
        val players = mutableListOf<Player>()
        val colors = TokenColor.values().take(playerCount)
        
        colors.forEachIndexed { index, color ->
            val tokens = (1..4).map { tokenId ->
                Token(id = tokenId, color = color)
            }
            
            val isBot = index > 0
            val playerName = if (isBot) "Bot ${index}" else "You"
            
            players.add(
                Player(
                    id = if (isBot) "bot_$index" else (userId ?: "player_1"),
                    name = playerName,
                    avatarUrl = "",
                    color = color,
                    tokens = tokens,
                    isBot = isBot,
                    botDifficulty = if (isBot) difficulty else BotDifficulty.EASY
                )
            )
            
            if (isBot) {
                aiPlayers["bot_$index"] = AIFactory.createAI(difficulty, gameEngine)
            }
        }
        
        val newGame = gameEngine.createNewGame(
            players = players,
            gameMode = GameMode.VS_AI,
            rules = GameRules()
        )
        
        _game.value = newGame
        _currentPlayerIndex.value = 0
        _isPlayerTurn.value = true
        _gameState.value = GameState.WaitingForRoll
        
        playSound("game_start")
    }

    /**
     * Roll the dice
     */
    fun rollDice() {
        val currentGame = _game.value ?: return
        val playerIndex = _currentPlayerIndex.value ?: return
        val player = currentGame.players[playerIndex]
        
        if (player.hasRolled && currentGame.gameMode != GameMode.ONLINE) {
            return // Already rolled, must move
        }
        
        viewModelScope.launch {
            _isRolling.value = true
            _animateDice.value = true
            
            playSound("dice_roll")
            
            // Animate dice roll
            delay(800)
            
            val value = gameEngine.rollDice()
            _diceValue.value = value
            _isRolling.value = false
            _animateDice.value = false
            
            // Check for valid moves
            val moves = gameEngine.getValidMoves(player, value)
            
            if (moves.isEmpty()) {
                // No valid moves, end turn
                _gameEvent.value = GameEvent.NoValidMoves(value)
                delay(1500)
                endTurn(value)
            } else {
                _validMoves.value = moves
                _gameState.value = GameState.SelectingMove(playerIndex, value, moves)
                
                // If it's a bot, make AI decision
                if (player.isBot) {
                    makeAIMove(player, value)
                }
            }
        }
    }

    /**
     * Make AI move
     */
    private fun makeAIMove(player: Player, diceValue: Int) {
        viewModelScope.launch {
            val ai = aiPlayers[player.id] ?: return@launch
            
            val decision = ai.makeMoveDecision(
                game = _game.value!!,
                player = player,
                diceValue = diceValue
            )
            
            if (decision.skipTurn) {
                delay(1000)
                endTurn(diceValue)
            } else {
                delay(500)
                moveToken(decision.tokenIndex)
            }
        }
    }

    /**
     * Move a token
     */
    fun moveToken(tokenIndex: Int) {
        val currentGame = _game.value ?: return
        val playerIndex = _currentPlayerIndex.value ?: return
        val diceVal = _diceValue.value ?: return
        val player = currentGame.players[playerIndex]
        val token = player.tokens.getOrNull(tokenIndex) ?: return

        viewModelScope.launch {
            _gameState.value = GameState.Moving(playerIndex, tokenIndex)
            
            val result = gameEngine.moveToken(currentGame, playerIndex, tokenIndex, diceVal)
            
            if (result.success) {
                _game.value = result.game
                
                // Trigger animation
                _animateToken.value = TokenAnimation(
                    tokenIndex = tokenIndex,
                    playerIndex = playerIndex,
                    fromPosition = token.position,
                    toPosition = result.game.players[playerIndex].tokens[tokenIndex].position
                )
                
                playSound("token_move")
                
                // Handle captures
                if (result.capturedTokens.isNotEmpty()) {
                    playSound("token_kill")
                    _gameEvent.value = GameEvent.TokenCaptured(
                        capturedBy = playerIndex,
                        capturedTokens = result.capturedTokens
                    )
                    delay(500)
                }
                
                // Handle token reaching home
                if (result.tokenReachedHome) {
                    playSound("token_home")
                    _gameEvent.value = GameEvent.TokenReachedHome(playerIndex, tokenIndex)
                }
                
                // Check for six (bonus turn)
                if (diceVal == 6) {
                    playSound("six_rolled")
                    _gameEvent.value = GameEvent.RolledSix
                }
                
                // Check for game over
                if (result.isGameOver) {
                    handleGameOver(result.game, playerIndex)
                    return@launch
                }
                
                // Check for bonus turn
                if (result.bonusTurn && !result.threeConsecutiveSixes) {
                    _gameEvent.value = GameEvent.BonusTurn
                    delay(1000)
                    _gameState.value = GameState.WaitingForRoll
                    _validMoves.value = emptyList()
                    
                    // If it's a bot, roll again
                    if (player.isBot) {
                        delay(500)
                        rollDice()
                    }
                } else {
                    delay(500)
                    endTurn(diceVal)
                }
            }
        }
    }

    /**
     * End current turn
     */
    private fun endTurn(diceValue: Int) {
        val currentGame = _game.value ?: return
        
        viewModelScope.launch {
            val nextIndex = gameEngine.getNextPlayerIndex(currentGame)
            val nextPlayer = currentGame.players[nextIndex]
            
            // Update game
            val updatedGame = currentGame.copy(
                currentPlayerIndex = nextIndex,
                updatedAt = System.currentTimeMillis()
            )
            _game.value = updatedGame
            _currentPlayerIndex.value = nextIndex
            _isPlayerTurn.value = !nextPlayer.isBot
            _validMoves.value = emptyList()
            _diceValue.value = 0
            _gameState.value = GameState.WaitingForRoll
            
            // If it's a bot's turn, automatically roll
            if (nextPlayer.isBot) {
                delay(800)
                rollDice()
            }
        }
    }

    /**
     * Handle game over
     */
    private fun handleGameOver(finishedGame: Game, winnerIndex: Int) {
        viewModelScope.launch {
            val winner = finishedGame.players[winnerIndex]
            
            playSound("victory")
            
            _gameState.value = GameState.GameOver(winner = winner, rankings = emptyList())
            _gameEvent.value = GameEvent.GameOver(winner)
            
            // Update stats if current user won
            if (winner.id == currentUserId) {
                // Award coins and XP for winning
                _gameEvent.value = GameEvent.RewardEarned(
                    coins = 100L,
                    xp = 50L
                )
            }
        }
    }

    /**
     * Auto-select token (for quick play)
     */
    fun autoSelectToken() {
        val moves = _validMoves.value ?: return
        if (moves.isNotEmpty()) {
            moveToken(moves.first().tokenIndex)
        }
    }

    /**
     * Pause game
     */
    fun pauseGame() {
        val currentGame = _game.value ?: return
        _game.value = currentGame.copy(gameStatus = GameStatus.PAUSED)
    }

    /**
     * Resume game
     */
    fun resumeGame() {
        val currentGame = _game.value ?: return
        _game.value = currentGame.copy(gameStatus = GameStatus.IN_PROGRESS)
    }

    /**
     * Quit game
     */
    fun quitGame() {
        gameListenerJob?.cancel()
        _game.value = null
        _gameState.value = null
        _validMoves.value = emptyList()
    }

    /**
     * Play sound effect
     */
    private fun playSound(soundName: String) {
        if (soundEnabled) {
            try {
                LudoBlitzApp.instance.playSound(soundName)
            } catch (e: Exception) {
                // Sound not available
            }
        }
    }

    /**
     * Set sound enabled
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    /**
     * Clear animation trigger
     */
    fun clearAnimation() {
        _animateToken.value = null
        _animateDice.value = false
    }

    /**
     * Clear event
     */
    fun clearEvent() {
        _gameEvent.value = GameEvent.None
    }

    override fun onCleared() {
        super.onCleared()
        gameListenerJob?.cancel()
    }
}

/**
 * Token animation data
 */
data class TokenAnimation(
    val tokenIndex: Int,
    val playerIndex: Int,
    val fromPosition: Int,
    val toPosition: Int
)

/**
 * Game events for UI feedback
 */
sealed class GameEvent {
    object None : GameEvent()
    data class NoValidMoves(val diceValue: Int) : GameEvent()
    object RolledSix : GameEvent()
    object BonusTurn : GameEvent()
    data class TokenCaptured(
        val capturedBy: Int,
        val capturedTokens: List<CapturedToken>
    ) : GameEvent()
    data class TokenReachedHome(val playerIndex: Int, val tokenIndex: Int) : GameEvent()
    data class GameOver(val winner: Player) : GameEvent()
    data class RewardEarned(val coins: Long, val xp: Long) : GameEvent()
    data class PlayerDisconnected(val playerId: String) : GameEvent()
}
