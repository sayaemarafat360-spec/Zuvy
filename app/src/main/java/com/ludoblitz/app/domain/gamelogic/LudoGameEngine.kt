package com.ludoblitz.app.domain.gamelogic

import com.ludoblitz.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/**
 * Core Game Logic Engine for Ludo Blitz
 * Handles all game rules, token movement, captures, and game state management
 */
class LudoGameEngine(private val gameRules: GameRules = GameRules()) {

    companion object {
        // Board constants
        const val TOTAL_POSITIONS = 52
        const val HOME_STRETCH_LENGTH = 5
        const val TOKENS_PER_PLAYER = 4
        const val FINAL_HOME_POSITION = 57 // Home position (finish)
        
        // Safe positions (stars on board)
        val SAFE_POSITIONS = setOf(1, 9, 14, 22, 27, 35, 40, 48)
        
        // Starting positions for each color
        val START_POSITIONS = mapOf(
            TokenColor.RED to 1,
            TokenColor.GREEN to 14,
            TokenColor.YELLOW to 27,
            TokenColor.BLUE to 40
        )
        
        // Home entry positions (last position before home stretch)
        val HOME_ENTRY_POSITIONS = mapOf(
            TokenColor.RED to 51,
            TokenColor.GREEN to 12,
            TokenColor.YELLOW to 25,
            TokenColor.BLUE to 38
        )
        
        // Home stretch start positions (52-56 are home stretch)
        val HOME_STRETCH_START = mapOf(
            TokenColor.RED to 52,
            TokenColor.GREEN to 58,
            TokenColor.YELLOW to 64,
            TokenColor.BLUE to 70
        )
    }

    /**
     * Roll the dice
     * Returns a random value between 1-6
     */
    fun rollDice(): Int {
        return Random.nextInt(1, 7)
    }

    /**
     * Check if a token can move with the given dice value
     */
    fun canTokenMove(token: Token, diceValue: Int, player: Player): Boolean {
        // Token is already home
        if (token.isHome) return false
        
        // Token in base - needs 6 to come out (if classic rules)
        if (token.isInBase()) {
            return if (gameRules.requireSixToRelease) {
                diceValue == 6
            } else {
                true // Modern mode - any value can release
            }
        }
        
        // Token on board - check if move is valid
        val potentialPosition = calculateNewPosition(token, diceValue, player.color)
        return potentialPosition != null
    }

    /**
     * Calculate the new position after moving
     * Returns null if move is invalid
     */
    fun calculateNewPosition(token: Token, diceValue: Int, color: TokenColor): Int? {
        if (token.isHome) return null
        
        if (token.isInBase()) {
            // Token coming out of base
            return if (gameRules.requireSixToRelease && diceValue != 6) {
                null
            } else {
                START_POSITIONS[color]
            }
        }
        
        val currentPos = token.position
        val stepsRemaining = token.stepsTaken
        val totalStepsToHome = TOTAL_POSITIONS + HOME_STRETCH_LENGTH + 1
        val stepsAfterMove = stepsRemaining + diceValue
        
        // Check if would exceed home
        if (stepsAfterMove > totalStepsToHome) {
            return null // Cannot overshoot home
        }
        
        // Calculate new position
        return if (stepsRemaining < TOTAL_POSITIONS) {
            // Still on main track
            val homeEntry = HOME_ENTRY_POSITIONS[color]!!
            val distanceToHomeEntry = calculateDistance(currentPos, homeEntry, color)
            
            if (diceValue > distanceToHomeEntry && stepsRemaining + diceValue > TOTAL_POSITIONS) {
                // Entering home stretch
                val excessSteps = diceValue - distanceToHomeEntry
                HOME_STRETCH_START[color]!! + excessSteps - 1
            } else {
                // Normal movement on track
                normalizePosition(currentPos + diceValue)
            }
        } else {
            // Already in home stretch
            currentPos + diceValue
        }
    }

    /**
     * Move a token
     * Returns the move result including any captures
     */
    suspend fun moveToken(
        game: Game,
        playerIndex: Int,
        tokenIndex: Int,
        diceValue: Int
    ): MoveResult = withContext(Dispatchers.Default) {
        val player = game.players[playerIndex]
        val token = player.tokens[tokenIndex]
        
        // Validate move
        if (!canTokenMove(token, diceValue, player)) {
            return@withContext MoveResult(
                success = false,
                game = game,
                capturedTokens = emptyList(),
                tokenReachedHome = false,
                bonusTurn = false
            )
        }
        
        val oldPosition = token.position
        val newPosition = calculateNewPosition(token, diceValue, player.color)!!
        
        // Update token
        val updatedToken = token.copy(
            position = newPosition,
            stepsTaken = if (token.isInBase()) 0 else token.stepsTaken + diceValue,
            isInSafeZone = newPosition in SAFE_POSITIONS || isInHomeStretch(newPosition, player.color),
            isHome = newPosition >= FINAL_HOME_POSITION || 
                     (isInHomeStretch(newPosition, player.color) && newPosition == getFinalHomePosition(player.color))
        )
        
        // Check for captures
        val capturedTokens = mutableListOf<CapturedToken>()
        var captured = false
        
        if (!updatedToken.isHome && !updatedToken.isInSafeZone && !isInHomeStretch(newPosition, player.color)) {
            // Check if any opponent token is at new position
            game.players.forEachIndexed { pIdx, p ->
                if (pIdx != playerIndex) {
                    p.tokens.forEachIndexed { tIdx, t ->
                        if (!t.isInBase() && !t.isHome && t.position == newPosition && !t.isInSafeZone) {
                            capturedTokens.add(CapturedToken(pIdx, tIdx))
                            captured = true
                        }
                    }
                }
            }
        }
        
        // Update player's tokens
        val updatedTokens = player.tokens.toMutableList()
        updatedTokens[tokenIndex] = updatedToken
        
        // Reset captured tokens to base
        capturedTokens.forEach { captured ->
            val capturedPlayer = game.players[captured.playerIndex]
            val capturedTokenList = capturedPlayer.tokens.toMutableList()
            capturedTokenList[captured.tokenIndex] = capturedTokenList[captured.tokenIndex].copy(
                position = -1,
                stepsTaken = 0,
                isInSafeZone = false,
                isHome = false
            )
        }
        
        // Determine if bonus turn is granted
        val bonusTurn = diceValue == 6 || captured
        
        // Check for three consecutive sixes
        val updatedConsecutiveSixes = if (diceValue == 6) {
            player.consecutiveSixes + 1
        } else {
            0
        }
        
        // If three sixes, end turn without bonus
        val finalBonusTurn = if (gameRules.threeSixesRule && updatedConsecutiveSixes >= 3) {
            false
        } else {
            bonusTurn
        }
        
        // Update game state
        val updatedPlayers = game.players.toMutableList()
        updatedPlayers[playerIndex] = player.copy(
            tokens = updatedTokens,
            consecutiveSixes = updatedConsecutiveSixes
        )
        
        // Check for game over
        val tokenReachedHome = updatedToken.isHome
        val isGameOver = updatedTokens.all { it.isHome }
        
        val updatedGame = game.copy(
            players = updatedPlayers,
            diceValue = diceValue,
            updatedAt = System.currentTimeMillis()
        )
        
        MoveResult(
            success = true,
            game = updatedGame,
            capturedTokens = capturedTokens,
            tokenReachedHome = tokenReachedHome,
            bonusTurn = finalBonusTurn,
            isGameOver = isGameOver,
            threeConsecutiveSixes = updatedConsecutiveSixes >= 3
        )
    }

    /**
     * Release a token from base
     */
    suspend fun releaseToken(
        game: Game,
        playerIndex: Int,
        tokenIndex: Int
    ): MoveResult = withContext(Dispatchers.Default) {
        val player = game.players[playerIndex]
        val token = player.tokens[tokenIndex]
        
        if (!token.isInBase()) {
            return@withContext MoveResult(
                success = false,
                game = game,
                capturedTokens = emptyList(),
                tokenReachedHome = false,
                bonusTurn = false
            )
        }
        
        val startPosition = START_POSITIONS[player.color]!!
        
        // Check if start position is blocked by own tokens
        val ownTokensAtStart = player.tokens.count { 
            it.position == startPosition && !it.isHome 
        }
        
        if (ownTokensAtStart >= 2) {
            // Cannot release - start position is blocked
            return@withContext MoveResult(
                success = false,
                game = game,
                capturedTokens = emptyList(),
                tokenReachedHome = false,
                bonusTurn = false
            )
        }
        
        // Update token
        val updatedToken = token.copy(
            position = startPosition,
            stepsTaken = 0,
            isInSafeZone = startPosition in SAFE_POSITIONS
        )
        
        // Check for captures at start position
        val capturedTokens = mutableListOf<CapturedToken>()
        game.players.forEachIndexed { pIdx, p ->
            if (pIdx != playerIndex) {
                p.tokens.forEachIndexed { tIdx, t ->
                    if (!t.isInBase() && !t.isHome && 
                        t.position == startPosition && !t.isInSafeZone) {
                        capturedTokens.add(CapturedToken(pIdx, tIdx))
                    }
                }
            }
        }
        
        // Update player's tokens
        val updatedTokens = player.tokens.toMutableList()
        updatedTokens[tokenIndex] = updatedToken
        
        // Update game
        val updatedPlayers = game.players.toMutableList()
        updatedPlayers[playerIndex] = player.copy(tokens = updatedTokens)
        
        val updatedGame = game.copy(
            players = updatedPlayers,
            diceValue = 6,
            updatedAt = System.currentTimeMillis()
        )
        
        MoveResult(
            success = true,
            game = updatedGame,
            capturedTokens = capturedTokens,
            tokenReachedHome = false,
            bonusTurn = capturedTokens.isNotEmpty() || gameRules.captureGivesBonus,
            newPosition = startPosition
        )
    }

    /**
     * Get all valid moves for a player
     */
    fun getValidMoves(player: Player, diceValue: Int): List<ValidMove> {
        val moves = mutableListOf<ValidMove>()
        
        player.tokens.forEachIndexed { index, token ->
            if (canTokenMove(token, diceValue, player)) {
                val newPosition = calculateNewPosition(token, diceValue, player.color)
                moves.add(ValidMove(
                    tokenIndex = index,
                    currentPosition = token.position,
                    newPosition = newPosition ?: -1,
                    canCapture = false, // Will be calculated during move
                    priority = calculateMovePriority(token, diceValue, player)
                ))
            }
        }
        
        return moves.sortedByDescending { it.priority }
    }

    /**
     * Calculate priority for a move (used by AI)
     */
    private fun calculateMovePriority(token: Token, diceValue: Int, player: Player): Int {
        var priority = 0
        
        // Releasing a token has medium priority
        if (token.isInBase() && diceValue == 6) {
            priority += 50
        }
        
        // Moving towards home has higher priority
        if (!token.isInBase() && !token.isHome) {
            priority += 30 + token.stepsTaken
        }
        
        // Reaching home has highest priority
        val newPosition = calculateNewPosition(token, diceValue, player.color)
        if (newPosition != null && isInHomeStretch(newPosition, player.color)) {
            priority += 100
        }
        
        // Safe zone preference
        if (newPosition in SAFE_POSITIONS) {
            priority += 20
        }
        
        return priority
    }

    /**
     * Calculate distance between two positions for a specific color
     */
    private fun calculateDistance(from: Int, to: Int, color: TokenColor): Int {
        val startPos = START_POSITIONS[color]!!
        val normalizedFrom = normalizePosition(from)
        val normalizedTo = normalizePosition(to)
        
        return if (normalizedTo >= normalizedFrom) {
            normalizedTo - normalizedFrom
        } else {
            TOTAL_POSITIONS - normalizedFrom + normalizedTo
        }
    }

    /**
     * Normalize position to 1-52 range
     */
    private fun normalizePosition(position: Int): Int {
        return ((position - 1) % TOTAL_POSITIONS) + 1
    }

    /**
     * Check if position is in home stretch
     */
    private fun isInHomeStretch(position: Int, color: TokenColor): Boolean {
        val homeStretchStart = HOME_STRETCH_START[color]!!
        return position >= homeStretchStart && position < getFinalHomePosition(color)
    }

    /**
     * Get final home position for a color
     */
    private fun getFinalHomePosition(color: TokenColor): Int {
        return HOME_STRETCH_START[color]!! + HOME_STRETCH_LENGTH
    }

    /**
     * Check if a position is safe
     */
    fun isSafePosition(position: Int, color: TokenColor): Boolean {
        return position in SAFE_POSITIONS || isInHomeStretch(position, color)
    }

    /**
     * Get the next player's index
     */
    fun getNextPlayerIndex(game: Game): Int {
        var nextIndex = (game.currentPlayerIndex + 1) % game.players.size
        var attempts = 0
        
        while (game.players[nextIndex].hasWon() && attempts < game.players.size) {
            nextIndex = (nextIndex + 1) % game.players.size
            attempts++
        }
        
        return nextIndex
    }

    /**
     * Start a new game
     */
    fun createNewGame(
        players: List<Player>,
        gameMode: GameMode,
        rules: GameRules = GameRules()
    ): Game {
        return Game(
            players = players,
            currentPlayerIndex = 0,
            gameStatus = GameStatus.IN_PROGRESS,
            gameMode = gameMode,
            rules = rules,
            createdAt = System.currentTimeMillis()
        )
    }
}

/**
 * Result of a token move
 */
data class MoveResult(
    val success: Boolean,
    val game: Game,
    val capturedTokens: List<CapturedToken>,
    val tokenReachedHome: Boolean,
    val bonusTurn: Boolean,
    val isGameOver: Boolean = false,
    val threeConsecutiveSixes: Boolean = false,
    val newPosition: Int = -1
)

/**
 * Represents a captured token
 */
data class CapturedToken(
    val playerIndex: Int,
    val tokenIndex: Int
)

/**
 * Represents a valid move option
 */
data class ValidMove(
    val tokenIndex: Int,
    val currentPosition: Int,
    val newPosition: Int,
    val canCapture: Boolean,
    val priority: Int
)

/**
 * Game state sealed class for state management
 */
sealed class GameState {
    object WaitingForRoll : GameState()
    data class Rolling(val playerIndex: Int) : GameState()
    data class SelectingMove(val playerIndex: Int, val diceValue: Int, val validMoves: List<ValidMove>) : GameState()
    data class Moving(val playerIndex: Int, val tokenIndex: Int) : GameState()
    data class TurnComplete(val playerIndex: Int, val bonusTurn: Boolean) : GameState()
    data class GameOver(val winner: Player, val rankings: List<Player>) : GameState()
}
