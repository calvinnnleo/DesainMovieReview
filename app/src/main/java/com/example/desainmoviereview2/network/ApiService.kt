package com.example.desainmoviereview2.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API service interface.
 */
interface ApiService {
    /**
     * Sends a POST request to the /recommend endpoint to get movie recommendations.
     */
    @POST("/recommend")
    suspend fun getRecommendations(@Body request: RecommendationRequest): RecommendationResponse
}
