package com.example.desainmoviereview2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class for a reply to a forum post.
 */
@Parcelize
data class Reply(
    val post_id: String? = null,
    val author_uid: String? = null,
    val author_username: String? = null,
    val author_avatar_base64: String? = "",
    val content: String? = null,
    val created_at: Long? = null,
    val isEdited: Boolean = false
) : Parcelable
