package com.example.desainmoviereview2.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/recommend")
    suspend fun getRecommendations(@Body request: RecommendationRequest): RecommendationResponse
}
