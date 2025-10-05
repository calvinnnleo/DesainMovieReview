package com.example.desainmoviereview2

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class MovieItem(
    // Changed to var to allow setting the key from the snapshot
    var movie_id: String? = null,
    val title: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val num_votes: Double? = null,
    val runtime_minutes: Double? = null,
    val directors: String? = null,
    val writers: String? = null,
    val genres: String? = null,
    val overview: String? = null,
    val crew: String? = null,
    val primary_image_url: String? = null,
    val thumbnail_url: String? = null
) : Parcelable
