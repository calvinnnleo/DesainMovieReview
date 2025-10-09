package com.example.desainmoviereview2.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for the Retrofit API client.
 */
object ApiClient {
    private const val BASE_URL = "https://MovieMLEnthusiast.pythonanywhere.com/"

    /**
     * Lazy-initialized Retrofit service.
     */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
