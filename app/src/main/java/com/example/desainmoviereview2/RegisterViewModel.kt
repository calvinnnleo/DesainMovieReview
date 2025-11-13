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

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun registerUser(fullName: String, username: String, email: String, password: String) {
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _registerState.value = RegisterState.Error("Please fill all fields")
            return
        }

        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                val uid = firebaseUser?.uid
                if (uid != null) {
                    val fcmToken = try {
                        FirebaseMessaging.getInstance().token.await()
                    } catch (e: Exception) {
                        ""
                    }

                    val user = User(
                        uid = uid,
                        username = username,
                        email = email,
                        joinedDate = System.currentTimeMillis(),
                        fullName = fullName,
                        avatarBase64 = "",
                        fcmToken = fcmToken
                    )

                    database.getReference("users").child(uid).setValue(user).await()
                    _registerState.value = RegisterState.Success
                } else {
                    _registerState.value = RegisterState.Error("Registration failed: Could not get user ID")
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("email address is already") == true ->
                        "⚠️ Email already in use"
                    e.message?.contains("password") == true ->
                        "⚠️ Password should be at least 6 characters"
                    e.message?.contains("network") == true ->
                        "⚠️ Network error. Check your connection"
                    e.message?.contains("badly formatted") == true ->
                        "⚠️ Invalid email format"
                    else -> "⚠️ Registration failed: ${e.message}"
                }
                _registerState.value = RegisterState.Error(errorMessage)
            }
        }
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}
