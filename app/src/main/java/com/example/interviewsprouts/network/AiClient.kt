package com.example.interviewsprouts.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AiClient {
    // This is the public backend base URL, not an API key. Replace with deployed backend URL.
    // DeepSeek key must stay only in backend/.env or deployment env variables.
    private const val BACKEND_BASE_URL = "https://YOUR_BACKEND_URL/"
    private val retrofitBaseUrl = if (isPlaceholderBackendUrl()) {
        "https://example.com/"
    } else {
        BACKEND_BASE_URL
    }

    fun isPlaceholderBackendUrl(): Boolean = BACKEND_BASE_URL.contains("YOUR_BACKEND_URL")

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: AiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(retrofitBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
    }
}
