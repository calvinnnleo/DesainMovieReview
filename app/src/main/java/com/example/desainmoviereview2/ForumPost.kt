package com.example.desainmoviereview2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class ForumPost(
    val post_id: String? = null,
    val movie_id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val author_uid: String? = null,
    val author_username: String? = null,
    val user_rating: Int? = 0,
    val created_at: Long? = null,
    val replies: @RawValue Map<String, Reply> = emptyMap()
) : Parcelable
