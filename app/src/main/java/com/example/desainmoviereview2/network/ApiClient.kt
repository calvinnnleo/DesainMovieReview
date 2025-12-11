package com.example.desainmoviereview2.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for providing Retrofit API services.
 */
object ApiClient {

    private const val RECOMMENDATION_BASE_URL = "https://MovieMLEnthusiast.pythonanywhere.com/"
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

    /**
     * Creates a Retrofit instance for a given base URL.
     */
    private fun getRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazy-initialized service for the Recommendation API.
     */
    val recommendationService: RecommendationApiService by lazy {
        getRetrofit(RECOMMENDATION_BASE_URL).create(RecommendationApiService::class.java)
    }

    /**
     * Lazy-initialized service for the TMDb API.
     */
    val tmdbService: TmdbApiService by lazy {
        getRetrofit(TMDB_BASE_URL).create(TmdbApiService::class.java)
    }
}