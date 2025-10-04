package com.example.desainmoviereview2

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val joinedDate: Long = 0L
)
