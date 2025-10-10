package com.example.desainmoviereview2.network

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("imdb_id")
    val imdbId: String?
)