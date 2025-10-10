package com.example.desainmoviereview2

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

/**
 * Data class for a movie item.
 */
@IgnoreExtraProperties
@Parcelize
data class MovieItem(
    var movie_id: String? = null,
    val title: String? = null,
    val year: String? = null,
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
