package com.example.desainmoviereview2

import com.google.firebase.database.Exclude
import kotlinx.serialization.Serializable

@Serializable
data class ForumPost(
    @get:Exclude
    var id: String? = null, // The post's key from Firebase
    val movieId: String? = null,
    var title: String? = null, // Title of the post
    val content: String? = null,
    val authorUid: String? = null,
    var authorUsername: String? = null,
    val timestamp: Long? = null,
    val lastEdited: Long? = null,
    val ratingsSummary: RatingsSummary? = null
) {
    // Default constructor is required for Firebase DataSnapshot.getValue(ForumPost::class.java)
    constructor() : this(null, null, null, null, null, null, null, null, null)
}

@Serializable
data class RatingsSummary(
    val averageRating: Double? = null,
    val totalRatings: Int? = null
) {
    constructor() : this(0.0, 0)
}
