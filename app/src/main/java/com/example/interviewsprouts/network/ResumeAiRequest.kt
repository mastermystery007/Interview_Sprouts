package com.example.interviewsprouts.network

data class ResumeAiRequest(
    val resumeText: String,
    val targetRole: String,
    val experienceLevel: String,
    val jobSpecification: String,
    val requestedFeatures: List<String>
)
