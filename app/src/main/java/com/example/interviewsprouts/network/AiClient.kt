package com.example.interviewsprouts.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AiClient {
    // For local phone testing, replace YOUR_LAPTOP_IP with the laptop’s Wi-Fi IPv4 address, for example http://192.168.1.45:3000/. This is only the backend URL, not an API key.
    // DeepSeek key must stay only in backend/.env or deployment env variables.
    private const val BACKEND_BASE_URL = "http://192.168.1.228:3000/"
    private val retrofitBaseUrl = if (isPlaceholderBackendUrl()) {
        "https://example.com/"
    } else {
        BACKEND_BASE_URL.ensureTrailingSlash()
    }

    fun isPlaceholderBackendUrl(): Boolean =
        BACKEND_BASE_URL.contains("YOUR_BACKEND_URL") ||
            BACKEND_BASE_URL.contains("YOUR_LAPTOP_IP")

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

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
