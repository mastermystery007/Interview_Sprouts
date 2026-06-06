package com.example.interviewsprouts.network

data class ResumeAiResponse(
    val advancedReview: String? = null,
    val tailoredResumeSuggestions: String? = null,
    val interviewQuestions: String? = null,
    val bulletRewriteSuggestions: String? = null,
    val error: String? = null
)
