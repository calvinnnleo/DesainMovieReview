package com.example.desainmoviereview2.profile

import androidx.lifecycle.ViewModel
import com.example.desainmoviereview2.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
    val user: User = User(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSaveEnabled: Boolean = false,
    val pendingAvatarBase64: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private var originalUsername: String? = null
    private var originalAvatarBase64: String? = null

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
             _uiState.update { it.copy(errorMessage = "User not logged in") }
             return
        }

        _uiState.update { it.copy(isLoading = true) }

        database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val userProfile = dataSnapshot.getValue(User::class.java)
                if (userProfile != null) {
                    originalUsername = userProfile.username
                    originalAvatarBase64 = userProfile.avatarBase64
                    val email = auth.currentUser?.email ?: userProfile.email
                    val updatedUser = userProfile.copy(email = email)
                    
                    _uiState.update { 
                        it.copy(
                            user = updatedUser, 
                            isLoading = false,
                            pendingAvatarBase64 = null
                        ) 
                    }
                    // Check save enabled state initially (usually false)
                    validateSaveState(updatedUser.username, null)
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "User data is null") }
                }
            } else {
                 _uiState.update { it.copy(isLoading = false, errorMessage = "User data not found") }
            }
        }.addOnFailureListener { e ->
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
        }
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(user = it.user.copy(username = newUsername)) }
        validateSaveState(newUsername, _uiState.value.pendingAvatarBase64)
    }

    fun onImageSelected(base64Image: String) {
        _uiState.update { it.copy(pendingAvatarBase64 = base64Image) }
        validateSaveState(_uiState.value.user.username, base64Image)
    }
    
    private fun validateSaveState(username: String, pendingAvatar: String?) {
        val isModified = username != originalUsername || pendingAvatar != null
        val isValid = username.isNotBlank()
        _uiState.update { it.copy(isSaveEnabled = isModified && isValid) }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val currentState = _uiState.value
        val newUsername = currentState.user.username.trim()
        val newAvatar = currentState.pendingAvatarBase64

        if (newUsername.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Username cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        val updates = mutableMapOf<String, Any>()
        if (newUsername != originalUsername) {
            updates["username"] = newUsername
        }
        if (newAvatar != null) {
            updates["avatarBase64"] = newAvatar
        }

        if (updates.isNotEmpty()) {
             database.child("users").child(userId).updateChildren(updates).addOnSuccessListener {
                 if (updates.containsKey("username")) originalUsername = newUsername
                 if (updates.containsKey("avatarBase64")) originalAvatarBase64 = newAvatar
                 
                 updateUserContributions(userId, updates) {
                     _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            successMessage = "Profile and all contributions updated successfully",
                            user = it.user.copy(avatarBase64 = newAvatar ?: it.user.avatarBase64),
                            pendingAvatarBase64 = null
                        )
                     }
                     validateSaveState(newUsername, null)
                     loadUserProfile() 
                 }
             }.addOnFailureListener { e ->
                 _uiState.update { it.copy(isLoading = false, errorMessage = "Error updating profile: ${e.message}") }
             }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            validateSaveState(newUsername, newAvatar)
        }
    }
    
    private fun updateUserContributions(userId: String, updates: Map<String, Any>, onComplete: () -> Unit) {
        val forumPostsRef = database.child("forum_posts")
        val contributionUpdates = mutableMapOf<String, Any?>()

        forumPostsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    if (postSnapshot.child("author_uid").value == userId) {
                        if (updates.containsKey("username")) {
                            contributionUpdates["${postSnapshot.key}/author_username"] = updates["username"]
                        }
                        if (updates.containsKey("avatarBase64")) {
                            contributionUpdates["${postSnapshot.key}/author_avatar_base64"] = updates["avatarBase64"]
                        }
                    }

                    for (replySnapshot in postSnapshot.child("replies").children) {
                        if (replySnapshot.child("author_uid").value == userId) {
                            if (updates.containsKey("username")) {
                                contributionUpdates["${postSnapshot.key}/replies/${replySnapshot.key}/author_username"] = updates["username"]
                            }
                            if (updates.containsKey("avatarBase64")) {
                                contributionUpdates["${postSnapshot.key}/replies/${replySnapshot.key}/author_avatar_base64"] = updates["avatarBase64"]
                            }
                        }
                    }
                }

                if (contributionUpdates.isNotEmpty()) {
                    forumPostsRef.updateChildren(contributionUpdates).addOnCompleteListener {
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                 _uiState.update { it.copy(errorMessage = "Failed to update contributions: ${error.message}") }
                 onComplete() 
            }
        })
    }

    fun logout() {
        auth.signOut()
    }
}
