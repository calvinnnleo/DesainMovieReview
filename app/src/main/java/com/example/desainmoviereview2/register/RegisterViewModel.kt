package com.example.desainmoviereview2.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RegisterUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registrationSuccess: Boolean = false
)

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun registerUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val uiState = _uiState.value
            try {
                val result = auth.createUserWithEmailAndPassword(uiState.email, uiState.password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = mapOf(
                        "fullName" to uiState.fullName,
                        "username" to uiState.username,
                        "email" to uiState.email
                    )
                    database.child("users").child(firebaseUser.uid).setValue(user).await()
                    _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
                } else {
                     _uiState.update { it.copy(isLoading = false, error = "Registration failed: user is null.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}