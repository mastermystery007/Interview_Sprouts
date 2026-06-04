package com.example.interviewsprouts.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApiService {
    @POST("api/analyze-resume")
    fun analyzeResume(@Body request: ResumeAiRequest): Call<ResumeAiResponse>
}
