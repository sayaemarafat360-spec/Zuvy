package com.ludoblitz.app.domain.ai

import com.ludoblitz.app.data.model.*
import com.ludoblitz.app.domain.gamelogic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/**
 * AI Engine for Ludo Blitz
 * Implements different difficulty levels with varying strategies
 */
class LudoAI(
    private val gameEngine: LudoGameEngine,
    private val difficulty: BotDifficulty
) {

    /**
     * Make a move decision for the AI
     * Returns the selected token index to move
     */
    suspend fun makeMoveDecision(
        game: Game,
        player: Player,
        diceValue: Int
    ): AIDecision = withContext(Dispatchers.Default) {
        
        // Simulate thinking time based on difficulty
        delay(getThinkingTime())
        
        val validMoves = gameEngine.getValidMoves(player, diceValue)
        
        if (validMoves.isEmpty()) {
            return@withContext AIDecision(tokenIndex = -1, skipTurn = true)
        }
        
        // Evaluate each move and score it
        val scoredMoves = validMoves.map { move ->
            move to evaluateMove(game, player, move, diceValue)
        }.sortedByDescending { it.second }
        
        // Apply difficulty-based decision making
        val selectedMove = when (difficulty) {
            BotDifficulty.EASY -> selectEasyMove(scoredMoves)
            BotDifficulty.MEDIUM -> selectMediumMove(scoredMoves)
            BotDifficulty.HARD -> selectHardMove(scoredMoves)
            BotDifficulty.EXPERT -> selectExpertMove(scoredMoves, game, player, diceValue)
        }
        
        AIDecision(
            tokenIndex = selectedMove.tokenIndex,
            skipTurn = false,
            reasoning = getMoveReasoning(selectedMove, scoredMoves)
        )
    }

    /**
     * Evaluate a move and assign a score
     */
    private fun evaluateMove(
        game: Game,
        player: Player,
        move: ValidMove,
        diceValue: Int
    ): Int {
        var score = 0
        val token = player.tokens[move.tokenIndex]
        
        // 1. Reaching home - highest priority
        if (move.newPosition >= LudoGameEngine.FINAL_HOME_POSITION || 
            (move.newPosition >= 52 && token.stepsTaken + diceValue >= 57)) {
            score += 1000
        }
        
        // 2. Entering home stretch
        if (token.stepsTaken < 52 && token.stepsTaken + diceValue >= 52) {
            score += 500
        }
        
        // 3. Capture opportunity
        score += evaluateCaptureOpportunity(game, player, move, diceValue) * 100
        
        // 4. Safety evaluation
        score += evaluateSafety(game, player, move)
        
        // 5. Progress evaluation
        score += evaluateProgress(token, move, diceValue)
        
        // 6. Avoid danger
        score -= evaluateDanger(game, player, move)
        
        // 7. Strategic positioning
        score += evaluateStrategicPosition(game, player, move)
        
        // 8. Block opponent
        score += evaluateBlocking(game, player, move)
        
        return score
    }

    /**
     * Evaluate capture opportunity
     */
    private fun evaluateCaptureOpportunity(
        game: Game,
        player: Player,
        move: ValidMove,
        diceValue: Int
    ): Int {
        var captureScore = 0
        val newPosition = move.newPosition
        
        // Check if any opponent token is at new position
        game.players.forEach { opponent ->
            if (opponent.id != player.id) {
                opponent.tokens.forEach { token ->
                    if (!token.isInBase() && !token.isHome && 
                        token.position == newPosition && !token.isInSafeZone) {
                        // Higher value for capturing more advanced tokens
                        captureScore += 2 + (token.stepsTaken / 10)
                    }
                }
            }
        }
        
        return captureScore
    }

    /**
     * Evaluate safety of the new position
     */
    private fun evaluateSafety(
        game: Game,
        player: Player,
        move: ValidMove
    ): Int {
        var safetyScore = 0
        val newPosition = move.newPosition
        
        // Safe zone bonus
        if (newPosition in LudoGameEngine.SAFE_POSITIONS) {
            safetyScore += 50
        }
        
        // Home stretch is very safe
        if (newPosition >= 52) {
            safetyScore += 100
        }
        
        // Check if paired with own token (stacking for safety)
        val ownTokensAtPosition = player.tokens.count { 
            it.position == newPosition && !it.isHome 
        }
        if (ownTokensAtPosition > 0) {
            safetyScore += 30 // Stacked tokens are harder to capture
        }
        
        return safetyScore
    }

    /**
     * Evaluate progress made by the move
     */
    private fun evaluateProgress(
        token: Token,
        move: ValidMove,
        diceValue: Int
    ): Int {
        var progressScore = 0
        
        // Moving forward is good
        if (!token.isInBase()) {
            progressScore += token.stepsTaken / 5
        }
        
        // Getting out of base
        if (token.isInBase() && diceValue == 6) {
            progressScore += 40
        }
        
        // Progress towards home
        if (!token.isInBase() && !token.isHome) {
            progressScore += (token.stepsTaken + diceValue) / 4
        }
        
        return progressScore
    }

    /**
     * Evaluate danger from opponent tokens
     */
    private fun evaluateDanger(
        game: Game,
        player: Player,
        move: ValidMove
    ): Int {
        var dangerScore = 0
        val newPosition = move.newPosition
        
        // Skip if in safe zone
        if (newPosition in LudoGameEngine.SAFE_POSITIONS || newPosition >= 52) {
            return 0
        }
        
        // Check opponent tokens that could capture
        game.players.forEach { opponent ->
            if (opponent.id != player.id) {
                opponent.tokens.forEach { token ->
                    if (!token.isInBase() && !token.isHome && !token.isInSafeZone) {
                        val distance = calculateDistance(token.position, newPosition)
                        // Danger if within 6 spaces
                        if (distance in 1..6) {
                            dangerScore += (7 - distance) * 10
                        }
                    }
                }
            }
        }
        
        return dangerScore
    }

    /**
     * Evaluate strategic positioning
     */
    private fun evaluateStrategicPosition(
        game: Game,
        player: Player,
        move: ValidMove
    ): Int {
        var strategicScore = 0
        val newPosition = move.newPosition
        
        // Position near opponent's start can capture incoming tokens
        game.players.forEach { opponent ->
            if (opponent.id != player.id) {
                val opponentStart = LudoGameEngine.START_POSITIONS[opponent.color]!!
                val distance = calculateDistance(newPosition, opponentStart)
                
                // Being near opponent's start is strategic
                if (distance in 2..5) {
                    strategicScore += 20
                }
            }
        }
        
        return strategicScore
    }

    /**
     * Evaluate blocking potential
     */
    private fun evaluateBlocking(
        game: Game,
        player: Player,
        move: ValidMove
    ): Int {
        var blockScore = 0
        val newPosition = move.newPosition
        
        // Check if this blocks opponent's path to home
        game.players.forEach { opponent ->
            if (opponent.id != player.id) {
                opponent.tokens.forEach { token ->
                    if (!token.isInBase() && !token.isHome) {
                        val homeEntry = LudoGameEngine.HOME_ENTRY_POSITIONS[opponent.color]!!
                        val tokenToHome = calculateDistance(token.position, homeEntry)
                        
                        // If we're blocking their path to home
                        if (calculateDistance(token.position, newPosition) < tokenToHome) {
                            blockScore += 15
                        }
                    }
                }
            }
        }
        
        return blockScore
    }

    /**
     * Calculate distance between two positions (circular board)
     */
    private fun calculateDistance(from: Int, to: Int): Int {
        val normalizedFrom = ((from - 1) % 52) + 1
        val normalizedTo = ((to - 1) % 52) + 1
        
        return if (normalizedTo >= normalizedFrom) {
            normalizedTo - normalizedFrom
        } else {
            52 - normalizedFrom + normalizedTo
        }
    }

    /**
     * Easy mode: Random selection with slight bias towards progress
     */
    private fun selectEasyMove(scoredMoves: List<Pair<ValidMove, Int>>): ValidMove {
        // 60% chance to pick randomly from all valid moves
        return if (Random.nextFloat() < 0.6f) {
            scoredMoves.random().first
        } else {
            // 40% chance to pick from top half
            val topHalf = scoredMoves.take((scoredMoves.size + 1) / 2)
            topHalf.random().first
        }
    }

    /**
     * Medium mode: Usually picks good moves, sometimes makes mistakes
     */
    private fun selectMediumMove(scoredMoves: List<Pair<ValidMove, Int>>): ValidMove {
        // 70% chance to pick best move
        return if (Random.nextFloat() < 0.7f) {
            scoredMoves.first().first
        } else {
            // 30% chance to pick from top 3
            scoredMoves.take(3).random().first
        }
    }

    /**
     * Hard mode: Almost always picks optimal moves
     */
    private fun selectHardMove(scoredMoves: List<Pair<ValidMove, Int>>): ValidMove {
        // 90% chance to pick best move
        return if (Random.nextFloat() < 0.9f) {
            scoredMoves.first().first
        } else {
            // 10% chance to pick second best
            scoredMoves.getOrNull(1)?.first ?: scoredMoves.first().first
        }
    }

    /**
     * Expert mode: Perfect play with lookahead
     */
    private fun selectExpertMove(
        scoredMoves: List<Pair<ValidMove, Int>>,
        game: Game,
        player: Player,
        diceValue: Int
    ): ValidMove {
        // Add lookahead for expert difficulty
        val enhancedMoves = scoredMoves.map { (move, baseScore) ->
            val lookaheadScore = calculateLookaheadScore(game, player, move, diceValue)
            move to (baseScore + lookaheadScore)
        }.sortedByDescending { it.second }
        
        // 95% optimal play
        return if (Random.nextFloat() < 0.95f) {
            enhancedMoves.first().first
        } else {
            enhancedMoves.first().first
        }
    }

    /**
     * Calculate lookahead score for expert AI
     */
    private fun calculateLookaheadScore(
        game: Game,
        player: Player,
        move: ValidMove,
        diceValue: Int
    ): Int {
        var lookaheadScore = 0
        val token = player.tokens[move.tokenIndex]
        
        // Evaluate future positions after this move
        val futurePosition = move.newPosition
        
        // Check if multiple own tokens can form a blockade
        val tokensNearby = player.tokens.count {
            !it.isInBase() && !it.isHome && 
            calculateDistance(it.position, futurePosition) <= 2
        }
        if (tokensNearby >= 1) {
            lookaheadScore += 25 // Blockade potential
        }
        
        // Evaluate if move sets up for home entry
        val homeEntry = LudoGameEngine.HOME_ENTRY_POSITIONS[player.color]!!
        if (calculateDistance(futurePosition, homeEntry) <= 6) {
            lookaheadScore += 40 // Close to home entry
        }
        
        return lookaheadScore
    }

    /**
     * Get thinking time based on difficulty
     */
    private fun getThinkingTime(): Long {
        return when (difficulty) {
            BotDifficulty.EASY -> Random.nextLong(500, 1000)
            BotDifficulty.MEDIUM -> Random.nextLong(800, 1500)
            BotDifficulty.HARD -> Random.nextLong(1000, 2000)
            BotDifficulty.EXPERT -> Random.nextLong(1500, 2500)
        }
    }

    /**
     * Get reasoning for the move (for debugging/display)
     */
    private fun getMoveReasoning(
        selectedMove: ValidMove,
        allMoves: List<Pair<ValidMove, Int>>
    ): String {
        val selectedIndex = allMoves.indexOfFirst { it.first == selectedMove }
        return when {
            selectedIndex == 0 -> "Optimal move"
            selectedIndex <= allMoves.size / 2 -> "Good move"
            else -> "Suboptimal move"
        }
    }
}

/**
 * AI Decision result
 */
data class AIDecision(
    val tokenIndex: Int,
    val skipTurn: Boolean = false,
    val reasoning: String = ""
)

/**
 * Factory for creating AI instances
 */
object AIFactory {
    fun createAI(difficulty: BotDifficulty, gameEngine: LudoGameEngine): LudoAI {
        return LudoAI(gameEngine, difficulty)
    }
}
