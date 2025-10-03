package com.example.desainmoviereview2

import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class User(
    val username: String? = null,
    val email: String? = null,
    val profilePictureUrl: String? = null,
    val joinedDate: String? = null,
    val settings: UserSettings? = null
) {
    // Null default constructor for Firebase
    constructor() : this(null, null, null, null, null)
}

@Serializable
@IgnoreExtraProperties
data class UserSettings(
    val enableNotifications: Boolean? = null,
    val appTheme: String? = null
) {
    // Null default constructor for Firebase
    constructor() : this(false, "light")
}
