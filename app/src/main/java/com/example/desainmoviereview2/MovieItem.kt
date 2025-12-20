package com.example.desainmoviereview2

import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Data class for a movie item.
 */
@IgnoreExtraProperties
@Parcelize
data class MovieItem(
    var movie_id: String? = null,
    val title: String? = null,
    val year: @RawValue Any? = null,
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
) : Parcelable {
    @Exclude
    fun getYearString(): String? {
        return when (year) {
            is String -> year
            is Long -> year.toString()
            else -> null
        }
    }
}
