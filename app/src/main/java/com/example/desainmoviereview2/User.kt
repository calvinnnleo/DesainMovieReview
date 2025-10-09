package com.example.desainmoviereview2

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data class for a user.
 */
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val joinedDate: Long = 0L,
    val fullName: String = "",
    val avatarBase64: String = "",
    val fcmToken: String = ""
)
