package com.ludoblitz.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ludoblitz.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main repository for Firebase operations
 * Handles authentication, user data, and real-time game data
 */
@Singleton
class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase
) {

    // Collections
    private val usersCollection = firestore.collection("users")
    private val gamesCollection = firestore.collection("games")
    private val roomsRef = realtimeDb.getReference("rooms")
    private val onlineGamesRef = realtimeDb.getReference("online_games")
    private val leaderboardRef = realtimeDb.getReference("leaderboard")

    // ==================== AUTHENTICATION ====================

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = getUserById(result.user?.uid ?: "")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")
            
            val newUser = User(
                id = uid,
                email = email,
                displayName = displayName,
                coins = 1000,
                gems = 10,
                xp = 0,
                level = 1,
                createdAt = System.currentTimeMillis()
            )
            
            usersCollection.document(uid).set(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")
            
            // Check if user exists, if not create new
            val existingUser = try {
                getUserById(uid)
            } catch (e: Exception) {
                null
            }
            
            val user = if (existingUser != null) {
                existingUser
            } else {
                val newUser = User(
                    id = uid,
                    email = result.user?.email ?: "",
                    displayName = result.user?.displayName ?: "Player",
                    avatarUrl = result.user?.photoUrl?.toString() ?: "",
                    coins = 1000,
                    gems = 10
                )
                usersCollection.document(uid).set(newUser).await()
                newUser
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in as guest
     */
    suspend fun signInAsGuest(): Result<User> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: throw Exception("User ID not found")
            
            val guestUser = User(
                id = uid,
                displayName = "Guest_${uid.take(6)}",
                coins = 500,
                gems = 0
            )
            
            usersCollection.document(uid).set(guestUser).await()
            Result.success(guestUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // ==================== USER DATA ====================

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User {
        val document = usersCollection.document(userId).get().await()
        return document.toObject(User::class.java) ?: throw Exception("User not found")
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): User? {
        val uid = getCurrentUserId() ?: return null
        return try {
            getUserById(uid)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update specific user fields
     */
    suspend fun updateUserFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add coins to user
     */
    suspend fun addCoins(userId: String, amount: Long): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("coins", FieldValue.increment(amount))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add XP to user
     */
    suspend fun addXp(userId: String, amount: Long): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("xp", FieldValue.increment(amount))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user online status
     */
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        val updates = mapOf(
            "isOnline" to isOnline,
            "lastSeen" to System.currentTimeMillis()
        )
        usersCollection.document(userId).update(updates)
    }

    // ==================== GAME ROOMS ====================

    /**
     * Create a new game room
     */
    suspend fun createRoom(room: GameRoom): Result<String> {
        return try {
            val roomRef = roomsRef.push()
            val roomId = roomRef.key ?: throw Exception("Failed to generate room ID")
            val newRoom = room.copy(
                id = roomId,
                roomCode = generateRoomCode()
            )
            roomRef.setValue(newRoom).await()
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join a game room
     */
    suspend fun joinRoom(roomCode: String, userId: String): Result<GameRoom> {
        return try {
            val snapshot = roomsRef.orderByChild("roomCode").equalTo(roomCode).get().await()
            val roomSnapshot = snapshot.children.firstOrNull() 
                ?: throw Exception("Room not found")
            
            val room = roomSnapshot.getValue(GameRoom::class.java) 
                ?: throw Exception("Invalid room data")
            
            if (room.currentPlayers >= room.maxPlayers) {
                throw Exception("Room is full")
            }
            
            if (room.status != RoomStatus.WAITING) {
                throw Exception("Game already in progress")
            }
            
            // Add player to room
            val updatedPlayers = room.players + userId
            roomSnapshot.ref.updateChildren(mapOf(
                "players" to updatedPlayers,
                "currentPlayers" to updatedPlayers.size
            )).await()
            
            Result.success(room.copy(players = updatedPlayers, currentPlayers = updatedPlayers.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave a game room
     */
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit> {
        return try {
            val roomSnapshot = roomsRef.child(roomId).get().await()
            val room = roomSnapshot.getValue(GameRoom::class.java) 
                ?: throw Exception("Room not found")
            
            val updatedPlayers = room.players.filter { it != userId }
            
            if (updatedPlayers.isEmpty()) {
                // Delete room if no players left
                roomsRef.child(roomId).removeValue().await()
            } else {
                roomsRef.child(roomId).updateChildren(mapOf(
                    "players" to updatedPlayers,
                    "currentPlayers" to updatedPlayers.size
                )).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen to room changes
     */
    fun observeRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(GameRoom::class.java)
                trySend(room)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        roomsRef.child(roomId).addValueEventListener(listener)
        
        awaitClose {
            roomsRef.child(roomId).removeEventListener(listener)
        }
    }

    // ==================== ONLINE GAMES ====================

    /**
     * Create an online game
     */
    suspend fun createOnlineGame(game: Game): Result<String> {
        return try {
            val gameRef = onlineGamesRef.push()
            val gameId = gameRef.key ?: throw Exception("Failed to generate game ID")
            gameRef.setValue(game.copy(id = gameId)).await()
            Result.success(gameId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update online game state
     */
    suspend fun updateOnlineGame(game: Game): Result<Unit> {
        return try {
            onlineGamesRef.child(game.id).setValue(game).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen to online game changes
     */
    fun observeOnlineGame(gameId: String): Flow<Game?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(Game::class.java)
                trySend(game)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        onlineGamesRef.child(gameId).addValueEventListener(listener)
        
        awaitClose {
            onlineGamesRef.child(gameId).removeEventListener(listener)
        }
    }

    // ==================== FRIENDS ====================

    /**
     * Send friend request
     */
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            val requestId = "$fromUserId-$toUserId"
            val request = FriendRequest(
                id = requestId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                createdAt = System.currentTimeMillis()
            )
            
            firestore.collection("friend_requests").document(requestId).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val requestDoc = firestore.collection("friend_requests").document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java) 
                ?: throw Exception("Request not found")
            
            // Add each user to the other's friends list
            usersCollection.document(request.fromUserId)
                .update("friends", FieldValue.arrayUnion(request.toUserId)).await()
            usersCollection.document(request.toUserId)
                .update("friends", FieldValue.arrayUnion(request.fromUserId)).await()
            
            // Delete the request
            firestore.collection("friend_requests").document(requestId).delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get friend requests for a user
     */
    suspend fun getFriendRequests(userId: String): List<FriendRequest> {
        val snapshot = firestore.collection("friend_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .get().await()
        
        return snapshot.toObjects(FriendRequest::class.java)
    }

    // ==================== LEADERBOARD ====================

    /**
     * Get global leaderboard
     */
    suspend fun getGlobalLeaderboard(limit: Long = 100): List<LeaderboardEntry> {
        val snapshot = usersCollection
            .orderBy("rating")
            .limitToLast(limit)
            .get().await()
        
        return snapshot.documents.mapIndexed { index, doc ->
            val user = doc.toObject(User::class.java)!!
            LeaderboardEntry(
                rank = index + 1,
                userId = user.id,
                userName = user.displayName,
                avatarUrl = user.avatarUrl,
                rating = user.rating,
                wins = user.totalWins,
                winRate = user.getWinRate()
            )
        }.reversed()
    }

    // ==================== MATCHMAKING ====================

    /**
     * Find a random match
     */
    suspend fun findRandomMatch(userId: String): Result<GameRoom> {
        return try {
            // Look for an available room
            val snapshot = roomsRef
                .orderByChild("status")
                .equalTo(RoomStatus.WAITING.name)
                .get().await()
            
            for (roomSnapshot in snapshot.children) {
                val room = roomSnapshot.getValue(GameRoom::class.java)
                if (room != null && room.currentPlayers < room.maxPlayers && !room.isPrivate) {
                    return joinRoom(room.roomCode, userId)
                }
            }
            
            // No room found, create a new one
            val newRoom = GameRoom(
                hostId = userId,
                players = listOf(userId),
                currentPlayers = 1,
                isPrivate = false
            )
            val roomId = createRoom(newRoom).getOrThrow()
            Result.success(newRoom.copy(id = roomId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
