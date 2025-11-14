package com.example.desainmoviereview2.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 1. Define UI State: Represents the continuous state of the screen.
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false
)

// 2. Define Events: Represents one-time events or side effects.
sealed class LoginEvent {
    object Success : LoginEvent()
    data class Error(val message: String) : LoginEvent()
}

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")

    // StateFlow for the UI state
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // SharedFlow for one-time events
    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun loginUser() {
        val currentState = _uiState.value
        if (currentState.email.isEmpty() || currentState.password.isEmpty()) {
            viewModelScope.launch {
                _loginEvent.emit(LoginEvent.Error("Please enter both email and password"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val authResult = auth.signInWithEmailAndPassword(currentState.email, currentState.password).await()
                val user = authResult.user
                if (user != null) {
                    updateFcmToken(user.uid)
                    _loginEvent.emit(LoginEvent.Success)
                } else {
                    _loginEvent.emit(LoginEvent.Error("Login failed: User is null"))
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("There is no user record") == true ->
                        "⚠️ No account found with this email"
                    e.message?.contains("The password is invalid") == true ->
                        "⚠️ Incorrect password"
                    e.message?.contains("network") == true ->
                        "⚠️ Network error. Check your connection"
                    e.message?.contains("badly formatted") == true ->
                        "⚠️ Invalid email format"
                    else ->
                        "⚠️ Login failed: ${e.message}"
                }
                _loginEvent.emit(LoginEvent.Error(errorMessage))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun updateFcmToken(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            database.getReference("users").child(uid).child("fcmToken").setValue(token).await()
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Failed to update FCM token", e)
        }
    }
}
