package com.example.desainmoviereview2

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class PostComment(
    @get:Exclude
    var id: String? = null,
    val authorUid: String? = null,
    val authorUsername: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
    val lastEdited: Long? = null,
    val inReplyToCommentId: String? = null
) {
    // Null default constructor for Firebase
    constructor() : this(null, null, null, null, null, null, null)
}
