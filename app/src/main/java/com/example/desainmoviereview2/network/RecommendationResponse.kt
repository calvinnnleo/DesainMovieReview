package com.example.desainmoviereview2.network

data class RecommendationResponse(
    val recommendations: List<Recommendation>,
    val requested_movie: String,
    val status: String
)

data class Recommendation(
    val title: String
)
