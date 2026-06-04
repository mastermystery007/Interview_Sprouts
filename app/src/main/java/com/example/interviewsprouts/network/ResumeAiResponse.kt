package com.example.interviewsprouts.network

data class ResumeAiResponse(
    val advancedReview: String = "",
    val tailoredResumeSuggestions: String = "",
    val interviewQuestions: String = "",
    val bulletRewriteSuggestions: String = "",
    val error: String? = null
)
