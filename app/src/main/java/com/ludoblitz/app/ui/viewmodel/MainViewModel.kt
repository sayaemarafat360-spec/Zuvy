package com.ludoblitz.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoblitz.app.data.local.PreferenceManager
import com.ludoblitz.app.data.model.*
import com.ludoblitz.app.data.repository.FirebaseRepository
import com.ludoblitz.app.domain.gamelogic.LudoGameEngine
import com.ludoblitz.app.utils.AdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for Ludo Blitz
 * Handles user authentication, profile management, and app state
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val preferenceManager: PreferenceManager,
    private val adManager: AdManager,
    private val gameEngine: LudoGameEngine
) : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> = _uiState

    // User data
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Navigation events
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Theme
    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    init {
        _uiState.value = MainUiState.Initial
        checkAuthState()
        loadThemePreference()
    }

    /**
     * Check if user is already authenticated
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            _isLoading.value = true
            
            if (firebaseRepository.isUserLoggedIn()) {
                val user = firebaseRepository.getCurrentUser()
                _currentUser.value = user
                
                if (user != null) {
                    checkDailyReward(user)
                    _uiState.value = MainUiState.Authenticated(user)
                } else {
                    _uiState.value = MainUiState.Unauthenticated
                }
            } else {
                // Check for remembered credentials
                val rememberMe = preferenceManager.rememberMe.first()
                if (rememberMe) {
                    // User wanted to stay logged in but session expired
                    _uiState.value = MainUiState.SessionExpired
                } else {
                    _uiState.value = MainUiState.Unauthenticated
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Load theme preference
     */
    private fun loadThemePreference() {
        viewModelScope.launch {
            preferenceManager.isDarkMode.collect { isDark ->
                _isDarkMode.value = isDark
            }
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = MainUiState.Loading
            
            val result = firebaseRepository.signInWithEmail(email, password)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = MainUiState.Authenticated(user)
                    
                    if (rememberMe) {
                        preferenceManager.setRememberMe(true)
                        preferenceManager.setSavedEmail(email)
                    }
                    
                    checkDailyReward(user)
                    updateOnlineStatus(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Sign in failed"
                    _uiState.value = MainUiState.Error(error.message ?: "Sign in failed")
                }
            )
            
            _isLoading.value = false
        }
    }

    /**
     * Sign up with email and password
     */
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = MainUiState.Loading
            
            val result = firebaseRepository.signUpWithEmail(email, password, displayName)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = MainUiState.Authenticated(user)
                    _navigationEvent.value = NavigationEvent.ShowTutorial
                    updateOnlineStatus(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Sign up failed"
                    _uiState.value = MainUiState.Error(error.message ?: "Sign up failed")
                }
            )
            
            _isLoading.value = false
        }
    }

    /**
     * Sign in with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = MainUiState.Loading
            
            val result = firebaseRepository.signInWithGoogle(idToken)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _uiState.value = MainUiState.Authenticated(user)
                    checkDailyReward(user)
                    updateOnlineStatus(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Google sign in failed"
                    _uiState.value = MainUiState.Error(error.message ?: "Google sign in failed")
                }
            )
            
            _isLoading.value = false
        }
    }

    /**
     * Sign in as guest
     */
    fun signInAsGuest() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = MainUiState.Loading
            
            val result = firebaseRepository.signInAsGuest()
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    preferenceManager.setGuestMode(true)
                    _uiState.value = MainUiState.Authenticated(user)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Guest sign in failed"
                    _uiState.value = MainUiState.Error(error.message ?: "Guest sign in failed")
                }
            )
            
            _isLoading.value = false
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                updateOnlineStatus(false)
            }
            
            firebaseRepository.signOut()
            preferenceManager.setGuestMode(false)
            _currentUser.value = null
            _uiState.value = MainUiState.Unauthenticated
        }
    }

    /**
     * Update user online status
     */
    private fun updateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                firebaseRepository.updateUserOnlineStatus(user.id, isOnline)
            }
        }
    }

    /**
     * Check and show daily reward
     */
    private fun checkDailyReward(user: User) {
        viewModelScope.launch {
            val lastReward = preferenceManager.lastDailyReward.first()
            val currentDay = preferenceManager.dailyRewardDay.first()
            val now = System.currentTimeMillis()
            
            // Check if it's a new day (more than 24 hours)
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (now - lastReward >= oneDayMs) {
                val newDay = if (now - lastReward >= oneDayMs * 2) {
                    // Missed a day, reset streak
                    1
                } else {
                    (currentDay % 7) + 1
                }
                
                _navigationEvent.value = NavigationEvent.ShowDailyReward(newDay)
            }
        }
    }

    /**
     * Claim daily reward
     */
    fun claimDailyReward(day: Int) {
        viewModelScope.launch {
            val rewards = getDailyRewardForDay(day)
            
            _currentUser.value?.let { user ->
                val updatedUser = user.copy(
                    coins = user.coins + rewards.coins,
                    gems = user.gems + rewards.gems,
                    xp = user.xp + rewards.xp,
                    dailyRewardDay = day,
                    lastLoginDate = System.currentTimeMillis()
                )
                
                firebaseRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                
                preferenceManager.setLastDailyReward(System.currentTimeMillis())
                preferenceManager.setDailyRewardDay(day)
            }
        }
    }

    /**
     * Get reward for specific day
     */
    private fun getDailyRewardForDay(day: Int): Reward {
        return when (day) {
            1 -> Reward(coins = 100, xp = 50)
            2 -> Reward(coins = 150, xp = 75)
            3 -> Reward(coins = 200, gems = 1, xp = 100)
            4 -> Reward(coins = 250, xp = 125)
            5 -> Reward(coins = 300, gems = 2, xp = 150)
            6 -> Reward(coins = 400, xp = 200)
            7 -> Reward(coins = 500, gems = 5, xp = 300)
            else -> Reward(coins = 100, xp = 50)
        }
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentMode = _isDarkMode.value ?: false
            preferenceManager.setDarkMode(!currentMode)
            _isDarkMode.value = !currentMode
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Navigation handled
     */
    fun onNavigationHandled() {
        _navigationEvent.value = NavigationEvent.None
    }

    /**
     * Refresh user data
     */
    fun refreshUserData() {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val freshUser = firebaseRepository.getUserById(user.id)
                _currentUser.value = freshUser
            }
        }
    }
}

/**
 * Main UI State
 */
sealed class MainUiState {
    object Initial : MainUiState()
    object Loading : MainUiState()
    object Unauthenticated : MainUiState()
    object SessionExpired : MainUiState()
    data class Authenticated(val user: User) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

/**
 * Navigation events
 */
sealed class NavigationEvent {
    object None : NavigationEvent()
    object ShowTutorial : NavigationEvent()
    data class ShowDailyReward(val day: Int) : NavigationEvent()
    object NavigateToGame : NavigationEvent()
    object NavigateToProfile : NavigationEvent()
}
