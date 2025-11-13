package com.example.desainmoviereview2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.value = LoginState.Error("Please enter both email and password")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    updateFcmToken(user.uid)
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Login failed: User is null")
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
                _loginState.value = LoginState.Error(errorMessage)
            }
        }
    }

    private suspend fun updateFcmToken(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            database.getReference("users").child(uid).child("fcmToken").setValue(token).await()
        } catch (e: Exception) {
            // Handle exception
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
