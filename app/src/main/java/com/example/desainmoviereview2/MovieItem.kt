package com.example.desainmoviereview2

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class MovieItem(
    @get:Exclude
    var id: String? = null, // To hold the movie's key from Firebase
    val title: String? = null,
    val director: String? = null,
    val releaseYear: Int? = null,
    val genre: List<String> = emptyList(),
    val posterUrl: String? = null
) : Parcelable
