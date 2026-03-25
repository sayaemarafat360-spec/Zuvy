package com.ludoblitz.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a user profile in Ludo Blitz
 */
@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val coins: Long = 1000,
    val gems: Long = 0,
    val xp: Long = 0,
    val level: Int = 1,
    val totalWins: Int = 0,
    val totalGames: Int = 0,
    val winStreak: Int = 0,
    val maxWinStreak: Int = 0,
    val rating: Int = 1000,
    val selectedBoardTheme: String = "classic",
    val selectedTokenStyle: String = "default",
    val selectedDiceStyle: String = "default",
    val unlockedBoards: List<String> = listOf("classic"),
    val unlockedTokens: List<String> = listOf("default"),
    val unlockedDice: List<String> = listOf("default"),
    val achievements: List<String> = emptyList(),
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val adsWatched: Int = 0,
    val lastLoginDate: Long = System.currentTimeMillis(),
    val dailyRewardDay: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun getWinRate(): Float {
        return if (totalGames > 0) {
            (totalWins.toFloat() / totalGames) * 100
        } else {
            0f
        }
    }

    fun getXpForNextLevel(): Long {
        return (level * 1000L).coerceAtLeast(1000)
    }

    fun getXpProgress(): Float {
        val xpForNext = getXpForNextLevel()
        return if (xpForNext > 0) {
            (xp.toFloat() / xpForNext) * 100
        } else {
            0f
        }
    }
}

/**
 * Token colors in Ludo
 */
enum class TokenColor {
    RED, GREEN, YELLOW, BLUE;
    
    fun getStartPosition(): Int = when (this) {
        RED -> 1
        GREEN -> 14
        YELLOW -> 27
        BLUE -> 40
    }
    
    fun getHomeStartPosition(): Int = when (this) {
        RED -> 51
        GREEN -> 12
        YELLOW -> 25
        BLUE -> 38
    }
}

/**
 * Represents a single token on the board
 */
@Parcelize
data class Token(
    val id: Int,
    val color: TokenColor,
    var position: Int = -1, // -1 means in base
    var isHome: Boolean = false,
    var isInSafeZone: Boolean = false,
    var stepsTaken: Int = 0
) : Parcelable {
    
    fun isInBase(): Boolean = position == -1
    
    fun isFinished(): Boolean = isHome
}

/**
 * Represents a player in a game
 */
@Parcelize
data class Player(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val color: TokenColor,
    val tokens: List<Token>,
    var isBot: Boolean = false,
    var botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    var isCurrentTurn: Boolean = false,
    var hasRolled: Boolean = false,
    var consecutiveSixes: Int = 0,
    var isOnline: Boolean = true,
    var finishedPosition: Int = 0 // 0 means not finished, 1-4 means finishing position
) : Parcelable {
    
    fun getFinishedTokensCount(): Int = tokens.count { it.isHome }
    
    fun getActiveTokensCount(): Int = tokens.count { !it.isInBase() && !it.isHome }
    
    fun getTokensInBaseCount(): Int = tokens.count { it.isInBase() }
    
    fun hasWon(): Boolean = tokens.all { it.isHome }
    
    fun canMove(diceValue: Int, gameRules: GameRules): Boolean {
        // Check if any token can move with the given dice value
        return tokens.any { token ->
            gameRules.canTokenMove(token, diceValue, this)
        }
    }
}

/**
 * Bot difficulty levels
 */
enum class BotDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

/**
 * Game rules configuration
 */
@Parcelize
data class GameRules(
    val requireSixToRelease: Boolean = true, // Classic: need 6 to bring token out
    val threeSixesRule: Boolean = true, // 3 consecutive 6s = turn skip
    val safeZonesEnabled: Boolean = true,
    val captureGivesBonus: Boolean = true,
    val homeStretchBonus: Boolean = true,
    val maxPlayers: Int = 4,
    val tokenCount: Int = 4
) : Parcelable

/**
 * Represents a Ludo game session
 */
@Parcelize
data class Game(
    val id: String = "",
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val diceValue: Int = 0,
    val gameStatus: GameStatus = GameStatus.WAITING,
    val gameMode: GameMode = GameMode.LOCAL,
    val rules: GameRules = GameRules(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val finishedPlayersOrder: List<String> = emptyList(),
    val moveHistory: List<GameMove> = emptyList(),
    val roomId: String = "",
    val isPrivate: Boolean = false
) : Parcelable {
    
    fun getCurrentPlayer(): Player? = players.getOrNull(currentPlayerIndex)
    
    fun getNextPlayerIndex(): Int {
        var nextIndex = (currentPlayerIndex + 1) % players.size
        var attempts = 0
        while (players[nextIndex].hasWon() && attempts < players.size) {
            nextIndex = (nextIndex + 1) % players.size
            attempts++
        }
        return nextIndex
    }
}

/**
 * Game status
 */
enum class GameStatus {
    WAITING, IN_PROGRESS, PAUSED, FINISHED, CANCELLED
}

/**
 * Game mode
 */
enum class GameMode {
    LOCAL, ONLINE, VS_AI
}

/**
 * Represents a single move in the game
 */
@Parcelize
data class GameMove(
    val playerId: String,
    val tokenId: Int,
    val fromPosition: Int,
    val toPosition: Int,
    val diceValue: Int,
    val capturedTokenId: Int? = null,
    val capturedPlayerId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Online game room
 */
@Parcelize
data class GameRoom(
    val id: String = "",
    val name: String = "",
    val hostId: String = "",
    val players: List<String> = emptyList(),
    val maxPlayers: Int = 4,
    val currentPlayers: Int = 0,
    val isPrivate: Boolean = false,
    val roomCode: String = "",
    val gameRules: GameRules = GameRules(),
    val status: RoomStatus = RoomStatus.WAITING,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class RoomStatus {
    WAITING, FULL, IN_GAME, CLOSED
}

/**
 * Achievement definition
 */
@Parcelize
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val reward: Reward,
    val requirement: AchievementRequirement,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
) : Parcelable

@Parcelize
data class AchievementRequirement(
    val type: String, // WINS, GAMES, STREAK, CAPTURES, etc.
    val target: Int
) : Parcelable

/**
 * Reward structure
 */
@Parcelize
data class Reward(
    val coins: Long = 0,
    val gems: Long = 0,
    val xp: Long = 0,
    val items: List<String> = emptyList()
) : Parcelable

/**
 * Daily reward
 */
@Parcelize
data class DailyReward(
    val day: Int,
    val reward: Reward,
    val isClaimed: Boolean = false
) : Parcelable

/**
 * Shop item
 */
@Parcelize
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val price: Long,
    val currency: Currency,
    val previewUrl: String,
    val isPremium: Boolean = false
) : Parcelable

enum class ItemType {
    BOARD_THEME, TOKEN_STYLE, DICE_STYLE, EMOTE_PACK, POWER_UP
}

enum class Currency {
    COINS, GEMS, REAL_MONEY
}

/**
 * Friend request
 */
@Parcelize
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatar: String,
    val toUserId: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class RequestStatus {
    PENDING, ACCEPTED, REJECTED
}

/**
 * Leaderboard entry
 */
@Parcelize
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val avatarUrl: String,
    val rating: Int,
    val wins: Int,
    val winRate: Float
) : Parcelable

/**
 * Notification
 */
@Parcelize
data class GameNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val data: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class NotificationType {
    FRIEND_REQUEST, GAME_INVITE, ACHIEVEMENT_UNLOCKED, DAILY_REWARD, SYSTEM
}

/**
 * Emoji reaction for in-game chat
 */
@Parcelize
data class EmojiReaction(
    val id: String,
    val emoji: String,
    val name: String,
    val isPremium: Boolean = false
) : Parcelable

/**
 * Spin wheel reward
 */
@Parcelize
data class SpinWheelReward(
    val id: String,
    val reward: Reward,
    val probability: Float, // 0.0 to 1.0
    val color: String
) : Parcelable
