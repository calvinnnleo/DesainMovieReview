package com.example.desainmoviereview2.network

/**
 * Data class for the recommendation response body.
 */
data class RecommendationResponse(
    val recommendations: List<Recommendation>,
    val requested_movie: String,
    val status: String
)

/**
 * Data class for a single recommendation.
 */
data class Recommendation(
    val title: String
)
