package com.example.desainmoviereview2.forum

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Data class for a forum post.
 */
@Parcelize
data class ForumPost(
    val post_id: String? = null,
    val movie_id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val author_uid: String? = null,
    val author_username: String? = null,
    val author_avatar_base64: String? = "",
    val user_rating: Int? = 0,
    val created_at: Long? = null,
    val replies: @RawValue Map<String, Reply> = emptyMap(),
    val isEdited: Boolean = false
) : Parcelable
