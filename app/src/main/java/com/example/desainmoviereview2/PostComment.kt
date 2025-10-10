package com.example.desainmoviereview2

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ServerValue
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class PostComment(
    @get:Exclude
    var id: String? = null,
    val authorUid: String? = null,
    val authorUsername: String? = null,
    val content: String? = null,
    val timestamp: Any? = ServerValue.TIMESTAMP,
    val lastEdited: Long? = null,
    val inReplyToCommentId: String? = null
) {
    // Null default constructor for Firebase
    constructor() : this(null, null, null, null, ServerValue.TIMESTAMP, null, null)
}
