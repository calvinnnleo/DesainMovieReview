package com.example.desainmoviereview2

import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
data class MovieItem(
    @get:Exclude
    var id: String? = null,
    val title: String? = null,
    val director: String? = null,
    val releaseYear: Int? = null,
    val genre: List<String> = emptyList(),
    val posterUrl: String? = null
) : Parcelable {
    // Default constructor for Firebase
    constructor() : this(null, null, null, null, emptyList(), null)
}