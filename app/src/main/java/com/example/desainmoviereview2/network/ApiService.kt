package com.example.desainmoviereview2.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecommendationApiService {
    @POST("recommend")
    suspend fun getRecommendations(@Body request: RecommendationRequest): RecommendationResponse

    @POST("addmovie")
    suspend fun addMovie(@Body request: AddMovieRequest): AddMovieResponse
}

interface TmdbApiService {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbMovieDetails
}
data class TmdbMovieDetails(
    @SerializedName("imdb_id")
    val imdbId: String?
)