package com.example.interviewsprouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceAwareMatcherTest {
    @Test
    fun experienceEvidenceOutranksSkillsMention() {
        val result = EvidenceAwareMatcher.analyze(
            resumeText = """
Skills
Kotlin

Experience
Built an Android checkout flow in Kotlin and reduced payment failures by 18%.
            """.trimIndent(),
            jobDescription = "Kotlin is required.",
            roleKeywords = listOf("Kotlin"),
            jdKeywords = listOf("Kotlin")
        )

        val kotlinEvidence = result.requirements.first { it.requirement == "Kotlin" }
        assertEquals("Clearly evidenced", kotlinEvidence.status)
        assertTrue(kotlinEvidence.confidence >= 0.90)
    }

    @Test
    fun synonymMatchingRecognizesKubernetesAndK8s() {
        val result = EvidenceAwareMatcher.analyze(
            resumeText = """
Projects
Deployed a service to K8s and reduced release time by 30%.
            """.trimIndent(),
            jobDescription = "Kubernetes experience is required.",
            roleKeywords = emptyList(),
            jdKeywords = listOf("Kubernetes")
        )

        assertTrue(result.confidenceFor("Kubernetes") >= 0.90)
    }

    @Test
    fun weakBulletPreviewDoesNotInventAnOutcome() {
        val preview = EvidenceAwareMatcher.buildBulletPreview(
            resumeText = """
Experience
Responsible for Android application development.
            """.trimIndent(),
            relevantKeywords = listOf("Android")
        )

        assertTrue(preview.improvedStructure.contains("[truthful outcome or metric]"))
        assertTrue(preview.weakness.contains("responsibility", ignoreCase = true))
    }
}
