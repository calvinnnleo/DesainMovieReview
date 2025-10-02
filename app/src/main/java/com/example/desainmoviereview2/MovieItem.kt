package com.example.desainmoviereview2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MovieItem(
    val imageRes: Int,
    val title: String,
    val desc: String,
    val releaseDate: String
) : Parcelable
