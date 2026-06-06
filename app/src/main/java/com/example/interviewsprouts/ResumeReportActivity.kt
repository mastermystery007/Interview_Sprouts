package com.example.interviewsprouts

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.interviewsprouts.network.AiClient
import com.example.interviewsprouts.network.ResumeAiRequest
import com.example.interviewsprouts.network.ResumeAiResponse
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ResumeReportActivity : AppCompatActivity() {
    private lateinit var tabOverview: TextView
    private lateinit var tabStrengths: TextView
    private lateinit var tabGaps: TextView
    private lateinit var tabKeywords: TextView
    private lateinit var tabBespoke: TextView
    private lateinit var textTabContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_report)

        val resumeText = intent.getStringExtra("resume_text") ?: ""
        val targetRole = intent.getStringExtra("target_role") ?: "General Job Applicant"
        val experienceLevel = intent.getStringExtra("experience_level") ?: "Not specified"
        val jobSpecification = intent.getStringExtra("job_specification") ?: ""
        val jdProvided = jobSpecification.isNotBlank()

        val report = createResumeReport(resumeText, targetRole, experienceLevel, jobSpecification)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textReportSubtitle).text = shortenLabel(targetRole, 52)
        findViewById<TextView>(R.id.textScoreTargetRole).text = targetRole
        findViewById<TextView>(R.id.textScoreExperience).text = experienceLevel
        findViewById<TextView>(R.id.textScoreValue).text = "Dashboard View"
        findViewById<TextView>(R.id.textAlignmentLabel).text = alignmentLabel(report.overallScore)
        findViewById<TextView>(R.id.textJdChip).text = if (jdProvided) "JD Provided" else "No JD"
        findViewById<TextView>(R.id.chipTargetRole).text = shortenLabel(targetRole, 24)
        findViewById<TextView>(R.id.chipExperienceLevel).text = experienceLevel
        findViewById<TextView>(R.id.chipJdStatus).text = if (jdProvided) "JD Provided" else "No JD"
        findViewById<ScoreCircleView>(R.id.scoreCircleView).setScore(report.overallScore)

        findViewById<TextView>(R.id.textBasicFeedback).text = report.basicFeedback
        findViewById<TextView>(R.id.textMissingKeywordsHook).text = report.missingKeywordsHook
        findViewById<TextView>(R.id.textFullReport).text = report.fullReport
        findViewById<TextView>(R.id.textAdvancedLockedMessage).text =
            "Advanced AI review with optimized resume points, JD-specific missing points, and detailed resume-based interview questions is locked. Watch another ad to unlock.\n\nAdvanced AI review sends your resume text and job description to our backend after unlock."
        findViewById<TextView>(R.id.textLocalSaveNote).text = "Saved reports are stored locally on this device only."

        tabOverview = findViewById(R.id.tabOverview)
        tabStrengths = findViewById(R.id.tabStrengths)
        tabGaps = findViewById(R.id.tabGaps)
        tabKeywords = findViewById(R.id.tabKeywords)
        tabBespoke = findViewById(R.id.tabBespoke)
        textTabContent = findViewById(R.id.textTabContent)

        val tabContent = mapOf(
            tabOverview to report.overviewContent,
            tabStrengths to report.strengthsContent,
            tabGaps to report.gapsContent,
            tabKeywords to report.keywordsContent,
            tabBespoke to report.bespokeContent
        )
        tabContent.forEach { (tab, content) -> tab.setOnClickListener { selectTab(tab, content) } }
        selectTab(tabOverview, report.overviewContent)

        val textFullReport = findViewById<TextView>(R.id.textFullReport)
        val textUnlockedPointSuggestions = findViewById<TextView>(R.id.textUnlockedPointSuggestions)
        val textUnlockedInterviewQuestions = findViewById<TextView>(R.id.textUnlockedInterviewQuestions)
        val textLockedReportMessage = findViewById<TextView>(R.id.textLockedReportMessage)
        val textAdvancedLockedMessage = findViewById<TextView>(R.id.textAdvancedLockedMessage)
        val textAdvancedLlmReview = findViewById<TextView>(R.id.textAdvancedLlmReview)
        val btnUnlockFullReport = findViewById<Button>(R.id.btnUnlockFullReport)
        val btnUnlockAdvancedLlmReview = findViewById<Button>(R.id.btnUnlockAdvancedLlmReview)

        btnUnlockFullReport.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Simulated Ad")
                .setMessage("In the real app, a rewarded ad will play here. For now, tap Continue to unlock.")
                .setPositiveButton("Continue") { _, _ ->
                    textUnlockedPointSuggestions.text = generateResumePointSuggestions(resumeText, targetRole)
                    textUnlockedInterviewQuestions.text = generateInterviewQuestionsFromResume(resumeText, targetRole, experienceLevel, jobSpecification)
                    textFullReport.visibility = View.VISIBLE
                    textUnlockedPointSuggestions.visibility = View.VISIBLE
                    textUnlockedInterviewQuestions.visibility = View.VISIBLE
                    textLockedReportMessage.visibility = View.GONE
                    btnUnlockFullReport.visibility = View.GONE
                    textAdvancedLockedMessage.visibility = View.VISIBLE
                    btnUnlockAdvancedLlmReview.visibility = View.VISIBLE
                    textAdvancedLlmReview.visibility = View.GONE
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnUnlockAdvancedLlmReview.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Simulated Ad")
                .setMessage("In the real app, another rewarded ad will unlock the Advanced AI Review. For now, tap Continue to unlock.")
                .setPositiveButton("Continue") { _, _ ->
                    textAdvancedLlmReview.text = "Generating Advanced AI Review..."
                    textAdvancedLlmReview.visibility = View.VISIBLE
                    textAdvancedLockedMessage.visibility = View.GONE
                    btnUnlockAdvancedLlmReview.visibility = View.GONE
                    requestAdvancedAiReview(resumeText, targetRole, experienceLevel, jobSpecification, textAdvancedLlmReview)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnSaveReport).setOnClickListener {
            saveReportLocally(targetRole, experienceLevel, report)
            Toast.makeText(this, "Report saved locally on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectTab(selected: TextView, content: String) {
        listOf(tabOverview, tabStrengths, tabGaps, tabKeywords, tabBespoke).forEach { tab ->
            tab.setBackgroundResource(if (tab == selected) R.drawable.bg_tab_selected else R.drawable.bg_chip)
            tab.setTextColor(if (tab == selected) 0xFF0B63CE.toInt() else 0xFF333333.toInt())
        }
        textTabContent.text = content
    }

    private fun requestAdvancedAiReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String,
        textAdvancedLlmReview: TextView
    ) {
        if (AiClient.isPlaceholderBackendUrl()) {
            textAdvancedLlmReview.text = buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification)
            return
        }

        val request = ResumeAiRequest(
            resumeText = resumeText,
            targetRole = targetRole,
            experienceLevel = experienceLevel,
            jobSpecification = jobSpecification,
            requestedFeatures = listOf(
                "advancedReview",
                "optimizedResumePoints",
                "missingJobDescriptionPoints",
                "interviewQuestions"
            )
        )

        AiClient.service.analyzeResume(request).enqueue(object : Callback<ResumeAiResponse> {
            override fun onResponse(call: Call<ResumeAiResponse>, response: Response<ResumeAiResponse>) {
                val body = response.body()
                textAdvancedLlmReview.text = if (response.isSuccessful && body != null && body.error.isNullOrBlank()) {
                    formatBackendAdvancedReview(body)
                } else {
                    buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification)
                }
            }

            override fun onFailure(call: Call<ResumeAiResponse>, t: Throwable) {
                textAdvancedLlmReview.text = buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification)
            }
        })
    }

    private fun formatBackendAdvancedReview(response: ResumeAiResponse): String {
        val sections = mutableListOf<String>()
        response.advancedReview.takeIf { it.isNotBlank() }?.let { sections.add("Advanced AI Review\n$it") }
        response.tailoredResumeSuggestions.takeIf { it.isNotBlank() }?.let { sections.add("Optimized Resume Points + Missing JD-Based Points\n$it") }
        response.interviewQuestions.takeIf { it.isNotBlank() }?.let { sections.add("Resume-Specific Interview Questions\n$it") }
        response.bulletRewriteSuggestions.takeIf { it.isNotBlank() }?.let { sections.add("Optional Resume Point Rewrites\n$it") }
        return sections.joinToString("\n\n").ifBlank { "AI backend returned no usable content. Showing offline fallback is recommended." }
    }

    private fun buildOfflineAdvancedFallback(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String = "AI backend unavailable. Showing offline fallback.\n\n" + generateAdvancedLlmReview(resumeText, targetRole, experienceLevel, jobSpecification)

    private fun generateAdvancedLlmReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        val bullets = extractCandidateBullets(resumeText).take(6)
        val metrics = extractMetricExamples(resumeText)
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())
        val resumeLower = resumeText.lowercase()
        val matchedJd = jdKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingJd = jdKeywords.filterNot { resumeLower.contains(it.lowercase()) }
        val optimized = if (bullets.isEmpty()) {
            "• Add role-relevant resume points using your real projects, tools, responsibilities, and truthful placeholders like [X%] or [number]."
        } else {
            bullets.joinToString("\n") { "• Improve this point with clearer action, tool, scope, and truthful impact: \"${shortenLabel(it, 120)}\"" }
        }
        val missingText = if (jobSpecification.isBlank()) {
            "JD-specific suggestions require a pasted job description."
        } else {
            "Matched JD keywords with evidence: ${matchedJd.ifEmpty { listOf("No clear matches detected") }.joinToString(", ")}\n" +
                "Missing or weak JD evidence: ${missingJd.ifEmpty { listOf("No major JD gaps detected") }.joinToString(", ")}\n" +
                "Where to add them honestly: Skills / Experience / Projects / Summary — only if true."
        }
        return """
Advanced AI Review
• Target role: $targetRole ($experienceLevel).
• Strong evidence: ${bullets.take(2).joinToString("; ").ifBlank { "Add clearer role evidence from your resume." }}
• Weak or missing evidence: ${missingJd.take(5).joinToString(", ").ifBlank { "No major JD-specific weakness detected from available text." }}

Optimized Resume Points
$optimized

Missing JD-Based Points
$missingText

Resume-Specific Interview Questions
${generateInterviewQuestionsFromResume(resumeText, targetRole, experienceLevel, jobSpecification)}

Metric evidence detected
${metrics.ifEmpty { listOf("No clear measurable examples detected.") }.joinToString("\n") { "• $it" }}
        """.trimIndent()
    }

    private fun createResumeReport(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): ResumeReportResult {
        val roleKeywords = getKeywordsForRole(targetRole)
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())
        val combinedKeywords = (roleKeywords + jdKeywords).distinctBy { it.lowercase() }
        val resumeLower = resumeText.lowercase()
        val foundKeywords = combinedKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingKeywords = combinedKeywords.filterNot { resumeLower.contains(it.lowercase()) }

        val keywordMatchScore = calculateKeywordMatchScore(foundKeywords.size, combinedKeywords.size)
        val measurableImpactScore = calculateMeasurableImpactScore(resumeText)
        val actionVerbScore = calculateStrongActionVerbScore(resumeText)
        val sectionClarityScore = calculateSectionClarityScore(resumeText)
        val roleRelevanceScore = calculateRoleRelevanceScore(keywordMatchScore, sectionClarityScore, jobSpecification.isNotBlank())
        val overallScore = weightedOverallScore(keywordMatchScore, measurableImpactScore, actionVerbScore, sectionClarityScore, roleRelevanceScore)

        val foundText = bulletList(foundKeywords, "No matching role/JD keywords detected yet.")
        val missingText = bulletList(missingKeywords.take(12), "No major missing keyword detected from the current role/JD keyword set.")
        val actionVerbExplanation = explainActionVerbScore(actionVerbScore, resumeText)
        val measurableExplanation = explainMeasurableImpactScore(measurableImpactScore, resumeText)
        val clarityExplanation = explainSectionClarityScore(sectionClarityScore, resumeText)
        val missingSections = detectMissingSections(resumeText)

        val basicFeedback = """
Strong:
• Resume reviewed for $targetRole at $experienceLevel level.
• Found ${foundKeywords.size} relevant keyword(s) out of ${combinedKeywords.size} expected keyword(s).

Quick category scores:
• Keyword Match: $keywordMatchScore/100
• Measurable Details: $measurableImpactScore/100
• Strong Action Verbs: $actionVerbScore/100
• Section Clarity: $sectionClarityScore/100

Top fixes:
1. Add missing role/JD keywords honestly.
2. Add measurable impact only where you can support it with real outcomes.
3. Start more bullets with strong action verbs and concrete tools or responsibilities.
        """.trimIndent()

        val missingKeywordsHook = if (missingKeywords.isEmpty()) {
            "Keyword hook: Your resume already covers the main target keywords detected for this role/JD. Keep the wording truthful and specific."
        } else {
            "Keyword hook: You may be missing ${missingKeywords.take(6).joinToString(", ")}. Add them only where they reflect real skills, projects, or responsibilities."
        }

        val overview = """
Your resume shows ${alignmentLabel(overallScore).lowercase()} for $targetRole. The strongest visible signals are ${foundKeywords.take(5).joinToString(", ").ifBlank { "still developing" }}. Improve the next version by tightening resume points, adding honest role keywords, and showing real impact examples where available.
        """.trimIndent()

        val strengths = """
Found keywords:
$foundText

Action verb examples from your resume:
${formatExamples(extractActionVerbExamples(resumeText), "No strong action-verb examples detected.")}

Measurable examples from your resume:
${formatExamples(extractMetricExamples(resumeText), "No clear measurable examples detected.")}

Detected sections:
${formatExamples(detectSectionSignals(resumeText), "No standard section headings detected.")}
        """.trimIndent()

        val gaps = """
Missing keywords:
$missingText

Missing important sections:
${formatExamples(missingSections, "No major missing section detected.")}

Weak bullets:
${formatExamples(findWeakBullets(resumeText), "No obvious weak bullet detected from the extracted text.")}

Missing measurable impact:
${if (extractMetricExamples(resumeText).isEmpty()) "• Add truthful metrics such as %, users, revenue, time saved, cost reduced, accuracy, CTR, ROAS, or efficiency." else "• You have some measurable examples; add impact to other major achievements where true."}
        """.trimIndent()

        val keywords = """
Found role/JD keywords:
$foundText

Missing role/JD keywords:
$missingText

Where to add missing keywords honestly:
${missingKeywords.take(8).joinToString("\n") { "• $it → ${suggestWhereToAddKeyword(it)} (only if true)" }.ifBlank { "• No priority keyword placement needed from current inputs." }}
        """.trimIndent()

        val bespoke = if (jobSpecification.isBlank()) {
            "Paste a job description next time for tailored suggestions."
        } else {
            val matched = jdKeywords.filter { resumeLower.contains(it.lowercase()) }
            val missing = jdKeywords.filterNot { resumeLower.contains(it.lowercase()) }
            """
JD-specific suggestions:
• Matched JD keywords: ${matched.ifEmpty { listOf("No clear JD keyword matches detected") }.joinToString(", ")}
• Missing or weak JD keywords: ${missing.ifEmpty { listOf("No major JD keyword gaps detected") }.joinToString(", ")}
• Add missing JD language under Skills, Experience, Projects, or Summary only if it reflects real evidence.
            """.trimIndent()
        }

        val fullReport = """
Full Heuristic Analysis

Category Scores:
• Keyword Match: $keywordMatchScore/100 — Found ${foundKeywords.size} of ${combinedKeywords.size} expected role/JD keywords.
• Measurable Details: $measurableImpactScore/100
$measurableExplanation

• Strong Action Verbs: $actionVerbScore/100
$actionVerbExplanation

• Section Clarity: $sectionClarityScore/100
$clarityExplanation

• Role Relevance: $roleRelevanceScore/100 — Based on resume evidence, keyword match, and job description alignment.

How to add keywords honestly:
• Add tools under Skills only if you used them.
• Add responsibility keywords under Experience only if you actually did them.
• Add project keywords under Projects only if your project involved them.
• Do not add fake skills or fake metrics.
        """.trimIndent()

        return ResumeReportResult(
            overallScore, keywordMatchScore, measurableImpactScore, actionVerbScore, sectionClarityScore,
            roleRelevanceScore, foundKeywords, missingKeywords, basicFeedback, missingKeywordsHook, fullReport,
            overview, strengths, gaps, keywords, bespoke
        )
    }

    private fun calculateKeywordMatchScore(foundCount: Int, totalCount: Int): Int {
        if (totalCount == 0) return 50
        val ratio = foundCount.toDouble() / totalCount.toDouble()
        return when {
            ratio >= 0.80 -> 92
            ratio >= 0.65 -> 84
            ratio >= 0.50 -> 75
            ratio >= 0.35 -> 65
            ratio >= 0.20 -> 52
            ratio > 0.0 -> 40
            else -> 25
        }
    }

    private fun calculateMeasurableImpactScore(resumeText: String): Int {
        val metricCount = countMetricSignals(resumeText)
        val resultWordCount = countResultWords(resumeText)
        var score = 25
        score += (metricCount * 8).coerceAtMost(48)
        score += (resultWordCount * 5).coerceAtMost(22)
        return score.coerceIn(20, 95)
    }

    private fun calculateStrongActionVerbScore(resumeText: String): Int {
        val lower = resumeText.lowercase()
        val count = strongActionVerbs.count { lower.contains(it) }
        return when {
            count >= 10 -> 92
            count >= 7 -> 82
            count >= 5 -> 72
            count >= 3 -> 60
            count >= 1 -> 45
            else -> 25
        }
    }

    private fun calculateSectionClarityScore(resumeText: String): Int {
        val detectedSections = detectSectionSignals(resumeText)
        val wordCount = resumeText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        var score = 30 + (detectedSections.size * 12).coerceAtMost(60)
        if (wordCount in 250..1000) score += 10
        if (wordCount < 120) score -= 15
        if (wordCount > 1400) score -= 10
        return score.coerceIn(20, 95)
    }

    private fun calculateRoleRelevanceScore(keywordMatchScore: Int, sectionClarityScore: Int, jobSpecificationProvided: Boolean): Int {
        var score = ((keywordMatchScore * 0.75) + (sectionClarityScore * 0.25)).roundToInt()
        if (jobSpecificationProvided) score += 5
        return score.coerceIn(20, 95)
    }

    private fun weightedOverallScore(keywordMatchScore: Int, measurableImpactScore: Int, actionVerbScore: Int, sectionClarityScore: Int, roleRelevanceScore: Int): Int {
        val weighted = keywordMatchScore * 0.25 + measurableImpactScore * 0.25 + actionVerbScore * 0.20 + sectionClarityScore * 0.20 + roleRelevanceScore * 0.10
        return weighted.roundToInt().coerceIn(0, 100)
    }

    private fun explainMeasurableImpactScore(score: Int, resumeText: String): String {
        val examples = extractMetricExamples(resumeText)
        val headline = when {
            examples.isEmpty() -> "No clear measurable examples detected."
            score >= 80 -> "Detected measurable examples:"
            score >= 60 -> "Some measurable examples detected:"
            else -> "Limited measurable examples detected:"
        }
        return if (examples.isEmpty()) headline else headline + "\n" + examples.joinToString("\n") { "• $it" }
    }

    private fun explainSectionClarityScore(score: Int, resumeText: String): String {
        val detected = detectSectionSignals(resumeText)
        val missing = detectMissingSections(resumeText)
        return """
Detected sections:
${formatExamples(detected, "No standard section headings detected.")}

Missing important sections:
${formatExamples(missing, "No major missing section detected.")}
        """.trimIndent()
    }

    private fun explainActionVerbScore(score: Int, resumeText: String): String {
        val examples = extractActionVerbExamples(resumeText)
        val headline = when {
            examples.isEmpty() -> "No strong action-verb examples detected."
            score >= 80 -> "Strong action-verb examples detected:"
            score >= 60 -> "Some action-verb examples detected:"
            else -> "Limited action-verb examples detected:"
        }
        return if (examples.isEmpty()) headline else headline + "\n" + examples.joinToString("\n") { "• $it" }
    }

    private fun extractMetricExamples(resumeText: String): List<String> {
        val regex = Regex("""(%|\b\d+[\w%+,.]*\b|users?|customers?|clients?|hours?|days?|weeks?|months?|revenue|cost|budget|sales|leads|accuracy|performance|latency|ctr|cpc|cpa|roas|roi|improved|reduced|increased|saved|optimized)""", RegexOption.IGNORE_CASE)
        return resumeLines(resumeText).filter { regex.containsMatchIn(it) }.map { shortenLabel(it, 150) }.distinct().take(5)
    }

    private fun extractActionVerbExamples(resumeText: String): List<String> {
        val regex = Regex("""\b(${strongActionVerbs.joinToString("|")})\b""", RegexOption.IGNORE_CASE)
        return resumeLines(resumeText).filter { regex.containsMatchIn(it) }.map { shortenLabel(it, 150) }.distinct().take(5)
    }

    private fun generateResumePointSuggestions(resumeText: String, targetRole: String): String {
        val bullets = extractCandidateBullets(resumeText).take(5)
        val suggestions = if (bullets.isEmpty()) {
            "• Add 3–5 $targetRole points based on real coursework, internships, projects, tools, or responsibilities.\n• Use: Action verb + task + tool/skill + truthful result.\n• Add placeholders like [X%], [number], or [hours] only until you can verify the real value."
        } else {
            bullets.joinToString("\n") { bullet -> "• Optimize: \"${shortenLabel(bullet, 110)}\" → clarify action, tool, scope, and truthful impact." }
        }
        return "Heuristic Resume Point Suggestions\n$suggestions\n\nTruth warning: Only add keywords, tools, metrics, and responsibilities that reflect your real experience."
    }

    private fun generateInterviewQuestionsFromResume(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        val signals = mutableListOf<String>()
        signals.addAll(extractMetricExamples(resumeText))
        signals.addAll(extractActionVerbExamples(resumeText))
        signals.addAll(getKeywordsForRole(targetRole).filter { resumeText.contains(it, ignoreCase = true) }.map { "Detected skill/keyword: $it" })
        if (jobSpecification.isNotBlank()) {
            val jdMatches = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()).filter { resumeText.contains(it, ignoreCase = true) }
            signals.addAll(jdMatches.map { "JD match: $it" })
        }
        signals.addAll(extractCandidateBullets(resumeText))

        val uniqueSignals = signals.map { shortenLabel(it, 120) }.filter { it.isNotBlank() }.distinct().take(8)
        if (uniqueSignals.isEmpty()) {
            return "Resume-Specific Interview Questions\n• Add more detailed resume lines so questions can reference real projects, skills, tools, or achievements."
        }

        return uniqueSignals.take(8).mapIndexed { index, signal ->
            val focus = when {
                signal.startsWith("Detected skill/keyword:") -> "how you used this skill in a real $targetRole context"
                signal.startsWith("JD match:") -> "how this JD requirement appears in your resume evidence"
                Regex("[%0-9]").containsMatchIn(signal) -> "the measurable result and how it was achieved"
                else -> "the work behind this resume line"
            }
            "Q${index + 1}. Your resume mentions \"$signal\". Can you explain $focus, your exact role, and what outcome you can verify?"
        }.joinToString("\n\n")
    }

    private fun detectSectionSignals(resumeText: String): List<String> {
        val lower = resumeText.lowercase()
        val detected = mutableListOf<String>()
        if (lower.contains("education") || lower.contains("university") || lower.contains("college") || lower.contains("degree")) detected.add("Education")
        if (lower.contains("experience") || lower.contains("work experience") || lower.contains("employment") || lower.contains("internship") || lower.contains("intern")) detected.add("Experience")
        if (lower.contains("skills") || lower.contains("technical skills") || lower.contains("tools") || lower.contains("technologies")) detected.add("Skills")
        if (lower.contains("projects") || lower.contains("project")) detected.add("Projects")
        if (lower.contains("summary") || lower.contains("profile") || lower.contains("objective")) detected.add("Summary / Profile")
        if (lower.contains("certification") || lower.contains("certifications") || lower.contains("certificate")) detected.add("Certifications")
        return detected
    }

    private fun detectMissingSections(resumeText: String): List<String> {
        val detected = detectSectionSignals(resumeText).toSet()
        return listOf("Education", "Experience", "Skills", "Projects").filter { it !in detected }
    }

    private fun countMetricSignals(text: String): Int = Regex("""(\d+%|\d+\+?|\$|₹|€|£|users|customers|clients|hours|days|weeks|months|revenue|cost|accuracy|performance|latency|ctr|cpc|cpa|roas|roi|sales|growth|budget|leads)""", RegexOption.IGNORE_CASE).findAll(text).count()

    private fun countResultWords(text: String): Int {
        val lower = text.lowercase()
        return listOf("improved", "reduced", "increased", "saved", "optimized", "grew", "accelerated", "streamlined", "delivered", "resolved").count { lower.contains(it) }
    }

    private fun extractCandidateBullets(resumeText: String): List<String> = resumeLines(resumeText)
        .filter { line -> line.length >= 25 && !looksLikeHeading(line) }
        .take(12)

    private fun findWeakBullets(resumeText: String): List<String> = resumeLines(resumeText)
        .filter { it.length in 15..140 }
        .filter { line ->
            val lower = line.lowercase()
            lower.startsWith("worked on") || lower.startsWith("responsible for") || lower.startsWith("helped") ||
                (!Regex("[%0-9]").containsMatchIn(line) && strongActionVerbs.none { lower.contains(it) })
        }
        .map { shortenLabel(it, 140) }
        .distinct()
        .take(5)

    private fun resumeLines(resumeText: String): List<String> = resumeText
        .split('\n', '•', '●', '–', '-')
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }

    private fun looksLikeHeading(line: String): Boolean = line.length <= 24 && line.uppercase() == line && line.any { it.isLetter() }

    private fun getKeywordsForRole(targetRole: String): List<String> {
        val roleLower = targetRole.lowercase()
        return when {
            roleLower.contains("software") || roleLower.contains("android") -> listOf("Kotlin", "Java", "Android", "REST API", "Git", "testing", "debugging", "UI", "XML", "database", "Agile", "architecture")
            roleLower.contains("data analyst") -> listOf("SQL", "Excel", "Python", "Power BI", "Tableau", "data cleaning", "data visualization", "dashboard", "KPI", "statistics", "reporting", "business insights", "data modeling", "ETL", "trend analysis")
            roleLower.contains("business analyst") -> listOf("requirements gathering", "stakeholder management", "business process analysis", "user stories", "UAT", "Jira", "Agile", "SQL", "Excel", "Power BI", "Tableau", "KPI", "gap analysis", "process improvement", "BRD", "FRD")
            roleLower.contains("marketing") -> listOf("SEO", "SEM", "Google Analytics", "Google Ads", "Meta Ads", "campaign management", "conversion rate", "CTR", "CPC", "CPA", "ROAS", "lead generation", "A/B testing", "email marketing", "landing page optimization")
            roleLower.contains("product manager") -> listOf("product roadmap", "user research", "market research", "stakeholder management", "PRD", "user stories", "analytics", "A/B testing", "KPI", "go-to-market", "prioritization", "customer feedback", "feature planning", "product strategy")
            roleLower.contains("finance") -> listOf("financial analysis", "Excel", "financial modeling", "forecasting", "budgeting", "variance analysis", "valuation", "reporting", "Power BI", "risk analysis", "P&L", "cash flow", "cost analysis", "scenario analysis")
            roleLower.contains("sales") -> listOf("lead generation", "CRM", "sales pipeline", "cold calling", "client relationship management", "negotiation", "sales targets", "revenue growth", "B2B sales", "B2C sales", "customer acquisition", "account management", "conversion rate")
            roleLower.contains("operations") -> listOf("process improvement", "operations management", "workflow optimization", "Excel", "Power BI", "KPI", "cost reduction", "supply chain", "inventory management", "vendor management", "reporting", "root cause analysis", "SOP", "quality control")
            roleLower.contains("hr") -> listOf("recruitment", "talent acquisition", "onboarding", "employee engagement", "HR operations", "payroll", "performance management", "HRIS", "sourcing", "screening", "interview coordination", "employee relations", "training")
            roleLower.contains("ux") || roleLower.contains("ui") -> listOf("user research", "wireframing", "prototyping", "Figma", "usability testing", "design systems", "user flows", "interaction design", "visual design", "accessibility", "information architecture", "customer journey", "A/B testing")
            roleLower.contains("project manager") -> listOf("project planning", "stakeholder management", "risk management", "Agile", "Scrum", "Jira", "timeline management", "budget management", "resource allocation", "cross-functional collaboration", "status reporting", "delivery management", "scope management")
            else -> listOf("communication", "analysis", "problem solving", "project management", "stakeholder management", "reporting", "Excel", "presentation", "collaboration", "process improvement")
        }
    }

    private fun extractSimpleKeywordsFromJobSpec(jobSpecLower: String): List<String> {
        if (jobSpecLower.isBlank()) return emptyList()
        val possibleKeywords = listOf("sql", "excel", "python", "power bi", "tableau", "google analytics", "seo", "sem", "google ads", "meta ads", "stakeholder management", "requirements gathering", "user stories", "uat", "jira", "agile", "scrum", "rest api", "git", "kotlin", "java", "campaign management", "lead generation", "a/b testing", "conversion rate", "ctr", "cpc", "cpa", "roas", "financial modeling", "forecasting", "budgeting", "dashboard", "reporting", "market research", "product roadmap", "communication", "project management", "crm", "sales pipeline", "cold calling", "client relationship management", "recruitment", "talent acquisition", "onboarding", "figma", "wireframing", "prototyping", "usability testing", "process improvement", "operations management", "workflow optimization", "supply chain", "vendor management", "risk management")
        return possibleKeywords.filter { jobSpecLower.contains(it) }
    }

    private fun suggestWhereToAddKeyword(keyword: String): String {
        val lower = keyword.lowercase()
        return when {
            lower in listOf("sql", "excel", "python", "power bi", "tableau", "google analytics", "jira", "git", "kotlin", "java", "figma", "crm") -> "Skills"
            lower.contains("project") || lower.contains("dashboard") || lower.contains("prototype") -> "Projects"
            lower.contains("summary") || lower.contains("communication") || lower.contains("strategy") -> "Summary"
            else -> "Experience"
        }
    }

    private fun saveReportLocally(targetRole: String, experienceLevel: String, report: ResumeReportResult) {
        val prefs = getSharedPreferences(SavedReportsActivity.PREFS_NAME, MODE_PRIVATE)
        val existing = JSONArray(prefs.getString(SavedReportsActivity.KEY_REPORTS, "[]") ?: "[]")
        val item = JSONObject().apply {
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
            put("target_role", targetRole)
            put("experience_level", experienceLevel)
            put("overall_score", report.overallScore)
            put("basic_feedback", report.basicFeedback)
            put("full_report", report.fullReport)
            put("local_only_note", "Saved reports are stored locally on this device only.")
        }
        existing.put(item)
        prefs.edit().putString(SavedReportsActivity.KEY_REPORTS, existing.toString()).apply()
    }

    private fun bulletList(items: List<String>, emptyText: String): String = if (items.isEmpty()) "• $emptyText" else items.joinToString("\n") { "• $it" }
    private fun formatExamples(items: List<String>, emptyText: String): String = if (items.isEmpty()) "• $emptyText" else items.joinToString("\n") { "• $it" }
    private fun shortenLabel(value: String, maxLength: Int): String = if (value.length <= maxLength) value else value.take(maxLength - 1).trimEnd() + "…"
    private fun alignmentLabel(score: Int): String = when {
        score >= 80 -> "Strong Alignment"
        score >= 60 -> "Good Alignment"
        score >= 40 -> "Moderate Alignment"
        else -> "Needs Work"
    }

    private val strongActionVerbs = listOf(
        "achieved", "analyzed", "automated", "built", "coordinated", "created", "delivered", "designed", "developed", "executed", "generated", "implemented", "improved", "increased", "launched", "led", "managed", "optimized", "reduced", "streamlined", "resolved", "scaled", "planned", "tested", "deployed"
    )
}

data class ResumeReportResult(
    val overallScore: Int,
    val keywordMatchScore: Int,
    val measurableImpactScore: Int,
    val actionVerbScore: Int,
    val sectionClarityScore: Int,
    val roleRelevanceScore: Int,
    val foundKeywords: List<String>,
    val missingKeywords: List<String>,
    val basicFeedback: String,
    val missingKeywordsHook: String,
    val fullReport: String,
    val overviewContent: String,
    val strengthsContent: String,
    val gapsContent: String,
    val keywordsContent: String,
    val bespokeContent: String
)
