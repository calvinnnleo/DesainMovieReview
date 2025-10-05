package com.example.desainmoviereview2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reply(
    val post_id: String? = null,
    val author_uid: String? = null,
    val author_username: String? = null,
    val content: String? = null,
    val created_at: Long? = null
) : Parcelable
