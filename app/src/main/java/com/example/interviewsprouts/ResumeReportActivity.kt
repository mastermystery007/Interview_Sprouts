package com.example.interviewsprouts

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
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

private const val LOCKED_TAB_MESSAGE = "Watch an ad to unlock this section."

class ResumeReportActivity : AppCompatActivity() {
    private lateinit var tabOverview: TextView
    private lateinit var tabStrengths: TextView
    private lateinit var tabGaps: TextView
    private lateinit var tabKeywords: TextView
    private lateinit var tabBespoke: TextView
    private lateinit var textTabContent: TextView
    private val advancedLoadingHandler = Handler(Looper.getMainLooper())
    private var advancedLoadingRunnable: Runnable? = null
    private var advancedLoadingDotCount = 0
    private var isFirstAdUnlocked = false

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
        findViewById<TextView>(R.id.textScoreValue).text = scoreRatingLabel(report.overallScore)
        findViewById<TextView>(R.id.textAlignmentLabel).text = alignmentLabel(report.overallScore)
        findViewById<TextView>(R.id.textJdChip).text = if (jdProvided) "JD Provided" else "No JD"
        findViewById<TextView>(R.id.chipTargetRole).text = shortenLabel(targetRole, 24)
        findViewById<TextView>(R.id.chipExperienceLevel).text = experienceLevel
        findViewById<TextView>(R.id.chipJdStatus).text = if (jdProvided) "JD Provided" else "No JD"
        findViewById<ScoreCircleView>(R.id.scoreCircleView).setScore(report.overallScore)

        findViewById<TextView>(R.id.textBasicFeedback).text = applyReportFormatting(report.basicFeedback)
        findViewById<TextView>(R.id.textMissingKeywordsHook).text = report.missingKeywordsHook
        findViewById<TextView>(R.id.textFullReport).text = applyReportFormatting(report.fullReport)
        findViewById<TextView>(R.id.textAdvancedLockedMessage).text =
            "Advanced AI review with optimized resume points, JD-specific missing points, and detailed resume-based interview questions is locked. Watch another ad to unlock.\n\nAdvanced AI review sends your resume text and job description to our backend only after this second unlock."
        findViewById<TextView>(R.id.textLocalSaveNote).text = "Saved reports are stored locally on this device only."

        tabOverview = findViewById(R.id.tabOverview)
        tabStrengths = findViewById(R.id.tabStrengths)
        tabGaps = findViewById(R.id.tabGaps)
        tabKeywords = findViewById(R.id.tabKeywords)
        tabBespoke = findViewById(R.id.tabBespoke)
        textTabContent = findViewById(R.id.textTabContent)

        tabOverview.setOnClickListener { selectTab(tabOverview, report.overviewContent) }
        tabStrengths.setOnClickListener {
            selectTab(tabStrengths, if (isFirstAdUnlocked) report.strengthsContent else LOCKED_TAB_MESSAGE)
        }
        tabGaps.setOnClickListener {
            selectTab(tabGaps, if (isFirstAdUnlocked) report.gapsContent else LOCKED_TAB_MESSAGE)
        }
        tabKeywords.setOnClickListener {
            selectTab(tabKeywords, if (isFirstAdUnlocked) report.keywordsContent else LOCKED_TAB_MESSAGE)
        }
        tabBespoke.setOnClickListener {
            selectTab(tabBespoke, if (isFirstAdUnlocked) report.bespokeContent else LOCKED_TAB_MESSAGE)
        }

        val tabScrollContainer = findViewById<View>(R.id.tabScrollContainer)
        val textFullReport = findViewById<TextView>(R.id.textFullReport)
        val textUnlockedPointSuggestions = findViewById<TextView>(R.id.textUnlockedPointSuggestions)
        val textUnlockedInterviewQuestions = findViewById<TextView>(R.id.textUnlockedInterviewQuestions)
        textUnlockedPointSuggestions.visibility = View.GONE
        textUnlockedInterviewQuestions.visibility = View.GONE
        val textLockedReportMessage = findViewById<TextView>(R.id.textLockedReportMessage)
        val textAdvancedLockedMessage = findViewById<TextView>(R.id.textAdvancedLockedMessage)
        val textAdvancedLlmReview = findViewById<TextView>(R.id.textAdvancedLlmReview)
        val btnUnlockFullReport = findViewById<Button>(R.id.btnUnlockFullReport)
        val btnUnlockAdvancedLlmReview = findViewById<Button>(R.id.btnUnlockAdvancedLlmReview)

        tabScrollContainer.visibility = View.VISIBLE
        textTabContent.visibility = View.VISIBLE
        setAllTabsVisible()
        textFullReport.visibility = View.GONE
        selectTab(tabOverview, report.overviewContent)

        btnUnlockFullReport.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Simulated Ad")
                .setMessage("In the real app, a rewarded ad will play here. For now, tap Continue to unlock.")
                .setPositiveButton("Continue") { _, _ ->
                    isFirstAdUnlocked = true
                    tabScrollContainer.visibility = View.VISIBLE
                    textTabContent.visibility = View.VISIBLE
                    setAllTabsVisible()
                    selectTab(tabOverview, report.overviewContent)
                    textFullReport.visibility = View.VISIBLE
                    textUnlockedPointSuggestions.visibility = View.GONE
                    textUnlockedInterviewQuestions.visibility = View.GONE
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
                    textAdvancedLlmReview.visibility = View.VISIBLE
                    startAdvancedLoadingAnimation(textAdvancedLlmReview)
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

    private fun startAdvancedLoadingAnimation(textView: TextView) {
        stopAdvancedLoadingAnimation()
        advancedLoadingDotCount = 0
        advancedLoadingRunnable = object : Runnable {
            override fun run() {
                val dots = ".".repeat(advancedLoadingDotCount)
                textView.text = "Generating Advanced AI Review$dots"
                advancedLoadingDotCount = (advancedLoadingDotCount + 1) % 4
                advancedLoadingHandler.postDelayed(this, 500L)
            }
        }
        advancedLoadingRunnable?.run()
    }

    private fun stopAdvancedLoadingAnimation() {
        advancedLoadingRunnable?.let { advancedLoadingHandler.removeCallbacks(it) }
        advancedLoadingRunnable = null
        advancedLoadingDotCount = 0
    }

    override fun onDestroy() {
        stopAdvancedLoadingAnimation()
        super.onDestroy()
    }

    private fun setAllTabsVisible() {
        listOf(tabOverview, tabStrengths, tabGaps, tabKeywords, tabBespoke).forEach { tab ->
            tab.visibility = View.VISIBLE
        }
    }

    private fun selectTab(selected: TextView, content: String) {
        listOf(tabOverview, tabStrengths, tabGaps, tabKeywords, tabBespoke).forEach { tab ->
            tab.setBackgroundResource(if (tab == selected) R.drawable.bg_tab_selected else R.drawable.bg_chip)
            tab.setTextColor(if (tab == selected) 0xFF0B63CE.toInt() else 0xFF333333.toInt())
        }
        textTabContent.text = applyReportFormatting(content)
    }

    private fun requestAdvancedAiReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String,
        textAdvancedLlmReview: TextView
    ) {
        if (AiClient.isPlaceholderBackendUrl()) {
            stopAdvancedLoadingAnimation()
            textAdvancedLlmReview.text = applyReportFormatting(buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification))
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
                stopAdvancedLoadingAnimation()
                textAdvancedLlmReview.text = applyReportFormatting(
                    if (response.isSuccessful && body != null && body.error.isNullOrBlank()) {
                        formatBackendAdvancedReview(body, buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification))
                    } else {
                        buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification)
                    }
                )
            }

            override fun onFailure(call: Call<ResumeAiResponse>, t: Throwable) {
                stopAdvancedLoadingAnimation()
                textAdvancedLlmReview.text = applyReportFormatting(buildOfflineAdvancedFallback(resumeText, targetRole, experienceLevel, jobSpecification))
            }
        })
    }

    private fun formatBackendAdvancedReview(response: ResumeAiResponse, offlineFallback: String): String {
        val sections = mutableListOf<String>()
        val advancedReview = compactAdvancedReview(response.advancedReview)
        val suggestions = compactSuggestions(response.tailoredResumeSuggestions)
        val questions = compactQuestions(response.interviewQuestions)

        if (advancedReview.isNotBlank()) {
            sections.add("Advanced AI Review\n$advancedReview")
        }
        if (suggestions.isNotBlank()) {
            sections.add("Optimized Resume Points + Missing JD-Based Points\n$suggestions")
        }
        if (questions.isNotBlank()) {
            sections.add("Resume-Specific Interview Questions\n$questions")
        }

        return sections.joinToString("\n\n").ifBlank {
            "AI backend returned no usable content. Showing offline fallback.\n\n${offlineFallback.substringAfter("\n\n", offlineFallback)}"
        }
    }

    private fun compactAdvancedReview(text: String?): String {
        val cleaned = stripDuplicateSectionHeading(text.orEmpty(), "Advanced AI Review", "Advanced Review")
        if (cleaned.isBlank() || cleaned.equals("null", ignoreCase = true)) return ""

        val lines = cleaned.lines()
            .map { normalizeVisibleLine(it) }
            .filter { it.isNotBlank() && !isRemovedAdvancedLine(it) }

        val bulletTexts = lines.mapNotNull { line ->
            val withoutBullet = line.removePrefix("•").trim()
            when {
                withoutBullet.isBlank() -> null
                looksLikeStandaloneHeading(withoutBullet) -> null
                line.startsWith("•") || line.matches(Regex("^[-*]\\s+.+")) || line.matches(Regex("^\\d+[.)]\\s+.+")) -> withoutBullet
                else -> null
            }
        }.ifEmpty {
            splitParagraphIntoSentences(cleaned)
        }

        return bulletTexts
            .map { it.trim().trimStart('-', '*', '•').replace(Regex("^\\d+[.)]\\s*"), "").trim() }
            .filter { it.isNotBlank() && !looksLikeStandaloneHeading(it) && !isRemovedAdvancedLine(it) }
            .take(4)
            .joinToString("\n") { "• $it" }
    }

    private fun compactSuggestions(text: String?): String {
        val cleaned = stripDuplicateSectionHeading(
            text.orEmpty(),
            "Optimized Resume Points + Missing JD-Based Points",
            "Tailored Resume Suggestions",
            "Add suggested skills or tools only if they are true."
        )
        if (cleaned.isBlank() || cleaned.equals("null", ignoreCase = true)) return ""

        val optimized = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val general = mutableListOf<String>()
        var currentSection = "general"

        cleaned.lines().forEach { rawLine ->
            val line = normalizeVisibleLine(rawLine)
            if (line.isBlank() || isRemovedAdvancedLine(line)) return@forEach
            when {
                isSectionHeadingLine(line, "Optimized Resume Points") -> currentSection = "optimized"
                isSectionHeadingLine(line, "Missing JD-Based Points") -> currentSection = "missing"
                looksLikeStandaloneHeading(line) -> Unit
                else -> {
                    val bullet = line.trim().trimStart('•', '-', '*').replace(Regex("^\\d+[.)]\\s*"), "").trim()
                    if (bullet.isNotBlank()) {
                        when (currentSection) {
                            "optimized" -> optimized.add(bullet)
                            "missing" -> missing.add(bullet)
                            else -> general.add(bullet)
                        }
                    }
                }
            }
        }

        if (optimized.isEmpty() && missing.isEmpty() && general.isEmpty()) {
            general.addAll(splitParagraphIntoSentences(cleaned))
        }

        val selectedOptimized: List<String>
        val selectedMissing: List<String>
        val selectedGeneral: List<String>
        if (optimized.isNotEmpty() && missing.isNotEmpty()) {
            selectedOptimized = optimized.take(2)
            selectedMissing = missing.take(4 - selectedOptimized.size).take(2)
            val remaining = 4 - selectedOptimized.size - selectedMissing.size
            selectedGeneral = if (remaining > 0) (optimized.drop(selectedOptimized.size) + missing.drop(selectedMissing.size) + general).take(remaining) else emptyList()
        } else {
            selectedOptimized = optimized.take(4)
            selectedMissing = missing.take(4 - selectedOptimized.size)
            selectedGeneral = general.take(4 - selectedOptimized.size - selectedMissing.size)
        }

        val parts = mutableListOf<String>()
        if (selectedOptimized.isNotEmpty()) {
            parts.add("Optimized Resume Points\n" + selectedOptimized.joinToString("\n") { "• $it" })
        }
        if (selectedMissing.isNotEmpty()) {
            parts.add("Missing JD-Based Points\n" + selectedMissing.joinToString("\n") { "• $it" })
        }
        if (parts.isEmpty() && selectedGeneral.isNotEmpty()) {
            parts.add(selectedGeneral.joinToString("\n") { "• $it" })
        }
        return removeBlankLines(parts.joinToString("\n"))
    }

    private fun compactQuestions(text: String?): String {
        val cleaned = stripDuplicateSectionHeading(text.orEmpty(), "Resume-Specific Interview Questions", "Interview Questions")
        if (cleaned.isBlank() || cleaned.equals("null", ignoreCase = true)) return ""

        val blocks = Regex("(?im)(?=^(?:Q\\d+\\.|\\d+[.)])\\s+)")
            .split(cleaned)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { cleaned.lines().filter { it.trim().endsWith("?") } }

        val formattedBlocks = blocks.mapNotNull { block ->
            val keptLines = block.lines()
                .map { normalizeVisibleLine(it) }
                .filter { it.isNotBlank() && !isRemovedQuestionLine(it) }
            val questionLine = keptLines.firstOrNull { line ->
                line.matches(Regex("^(?:Q\\d+\\.|\\d+[.)])\\s+.+", RegexOption.IGNORE_CASE)) || line.endsWith("?")
            } ?: return@mapNotNull null
            val question = questionLine
                .replace(Regex("^(?:Q\\d+\\.|\\d+[.)])\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
                .trimStart('-', '*', '•')
                .trim()
            if (question.isBlank()) return@mapNotNull null
            val strongAnswer = keptLines.firstOrNull { it.startsWith("Strong answer should mention:", ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "concrete resume evidence and a verified outcome."
            question to strongAnswer
        }.take(4)

        return formattedBlocks.mapIndexed { index, (question, strongAnswer) ->
            "Q${index + 1}. $question\nStrong answer should mention: $strongAnswer"
        }.joinToString("\n\n")
    }

    private fun removeBlankLines(text: String): String = text.lines()
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .trim()

    private fun stripDuplicateSectionHeading(text: String, vararg headings: String): String {
        var cleaned = text.trim()
        headings.forEach { heading ->
            val pattern = Regex("^" + Regex.escape(heading) + "\\s*[:：-]?\\s*", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replaceFirst(pattern, "").trimStart()
        }
        return cleaned.trim()
    }

    private fun normalizeVisibleLine(line: String): String = line.trim().replace(Regex("\\s+"), " ")

    private fun isRemovedAdvancedLine(line: String): Boolean = isRemovedQuestionLine(line) ||
        isSectionHeadingLine(line, "Metric Evidence Detected") ||
        line.startsWith("Honesty reminder", ignoreCase = true) ||
        line.startsWith("Add suggested skills or tools only if they are true", ignoreCase = true)

    private fun isRemovedQuestionLine(line: String): Boolean =
        line.startsWith("Based on:", ignoreCase = true) ||
            line.startsWith("Why this may be asked:", ignoreCase = true) ||
            line.startsWith("Follow-up probe:", ignoreCase = true) ||
            line.startsWith("Follow up probe:", ignoreCase = true)

    private fun looksLikeStandaloneHeading(line: String): Boolean = listOf(
        "Advanced AI Review",
        "Advanced Review",
        "Metric Evidence Detected",
        "Optimized Resume Points",
        "Missing JD-Based Points",
        "Optimized Resume Points + Missing JD-Based Points",
        "Resume-Specific Interview Questions",
        "Interview Questions",
        "Optional Resume Point Rewrites",
        "Optional Resume Point Rewrites / cleanup notes",
        "Tailored Resume Suggestions"
    ).any { isSectionHeadingLine(line, it) }

    private fun isSectionHeadingLine(line: String, heading: String): Boolean =
        line.trim().trimEnd(':', '：', '-', '–').trim().equals(heading, ignoreCase = true)

    private fun splitParagraphIntoSentences(text: String): List<String> = text
        .replace("\n", " ")
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim().trimStart('-', '*', '•').replace(Regex("^\\d+[.)]\\s*"), "").trim() }
        .filter { it.isNotBlank() && !looksLikeStandaloneHeading(it) && !isRemovedAdvancedLine(it) }

    private fun applyReportFormatting(text: String): CharSequence {
        val builder = SpannableStringBuilder(text)
        val headings = listOf(
            "Missing keywords:",
            "Missing important sections:",
            "Weak bullets:",
            "Missing measurable impact:",
            "Role Keywords Found:",
            "Role Keywords Missing:",
            "JD Keywords Found:",
            "JD Keywords Missing:",
            "JD-specific missing points:",
            "JD keywords already evidenced:",
            "Advanced AI Review",
            "Metric Evidence Detected",
            "Optimized Resume Points",
            "Missing JD-Based Points",
            "Resume-Specific Interview Questions",
            "Optional Resume Point Rewrites",
            "Full Heuristic Analysis",
            "Category Findings:"
        )
        headings.forEach { heading ->
            var searchStart = 0
            while (searchStart < text.length) {
                val index = text.indexOf(heading, searchStart, ignoreCase = true)
                if (index == -1) break
                builder.setSpan(StyleSpan(Typeface.BOLD), index, index + heading.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                searchStart = index + heading.length
            }
        }
        return builder
    }


    private fun buildOfflineAdvancedFallback(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String = "AI backend unavailable or unreachable. Showing offline fallback.\n\n" + generateAdvancedLlmReview(resumeText, targetRole, experienceLevel, jobSpecification)

    private fun generateAdvancedLlmReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        val bullets = extractCandidateBullets(resumeText).take(4)
        val metrics = extractMetricExamples(resumeText)
        val roleAndJdKeywords = (getKeywordsForRole(targetRole) + extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()))
            .distinctBy { it.lowercase() }
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())
        val resumeLower = resumeText.lowercase()
        val matchedJd = jdKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingJd = jdKeywords.filterNot { resumeLower.contains(it.lowercase()) }

        val metricSummary = metrics.firstOrNull()?.let { "Measurable evidence: ${shortenLabel(it, 110)}" }
            ?: "Measurable evidence: no clear metrics detected; add numbers only where verifiable."
        val advanced = listOf(
            "Target role: $targetRole ($experienceLevel).",
            "Strong evidence: ${bullets.take(2).joinToString("; ").ifBlank { "Add clearer role evidence from your resume." }}",
            "Weak or missing evidence: ${missingJd.take(3).joinToString(", ").ifBlank { "No major JD-specific weakness detected from available text." }}",
            metricSummary
        ).joinToString("\n") { "• $it" }

        val optimizedBullets = if (bullets.isEmpty()) {
            listOf("Add role-relevant resume points based on real coursework, internships, projects, tools, or responsibilities.")
        } else {
            bullets.take(2).map { bullet -> buildOptimizedBulletSuggestion(bullet, roleAndJdKeywords) }
        }
        val missingBullets = if (jobSpecification.isBlank()) {
            listOf("Paste a job description to identify JD-based points not clearly evidenced.")
        } else {
            listOf(
                "Matched JD evidence: ${matchedJd.take(4).ifEmpty { listOf("No clear matches detected") }.joinToString(", ")}.",
                "Not clearly evidenced: ${missingJd.take(4).ifEmpty { listOf("No major JD gaps detected") }.joinToString(", ")}."
            )
        }
        val suggestions = buildString {
            if (optimizedBullets.isNotEmpty()) {
                appendLine("Optimized Resume Points")
                optimizedBullets.take(2).forEach { appendLine("• $it") }
            }
            if (missingBullets.isNotEmpty()) {
                appendLine("Missing JD-Based Points")
                missingBullets.take(4 - optimizedBullets.take(2).size).forEach { appendLine("• $it") }
            }
        }.trim()

        return """
Advanced AI Review
${compactAdvancedReview(advanced)}

Optimized Resume Points + Missing JD-Based Points
${compactSuggestions(suggestions)}

Resume-Specific Interview Questions
${compactQuestions(generateInterviewQuestionsFromResume(resumeText, targetRole, experienceLevel, jobSpecification))}
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
        val foundRoleKeywords = roleKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingRoleKeywords = roleKeywords.filterNot { resumeLower.contains(it.lowercase()) }
        val foundJdKeywords = jdKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingJdKeywords = jdKeywords.filterNot { resumeLower.contains(it.lowercase()) }
        val foundKeywords = combinedKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingKeywords = combinedKeywords.filterNot { resumeLower.contains(it.lowercase()) }

        val keywordMatchScore = calculateKeywordMatchScore(foundKeywords.size, combinedKeywords.size)
        val measurableImpactScore = calculateMeasurableImpactScore(resumeText)
        val actionVerbScore = calculateStrongActionVerbScore(resumeText)
        val sectionClarityScore = calculateSectionClarityScore(resumeText)
        val roleRelevanceScore = calculateRoleRelevanceScore(keywordMatchScore, sectionClarityScore, jobSpecification.isNotBlank())
        val overallScore = weightedOverallScore(keywordMatchScore, measurableImpactScore, actionVerbScore, sectionClarityScore, roleRelevanceScore)

        val foundText = bulletList(foundKeywords, "No matching role/JD keywords detected yet.")
        val missingText = missingKeywords.take(12).joinToString("\n") { keyword ->
            "• $keyword → ${keywordAddSuggestion(keyword)}"
        }.ifBlank { "• No major missing keyword detected from the current role/JD keyword set." }
        val foundRoleText = bulletList(foundRoleKeywords, "No matching role keywords detected yet.")
        val missingRoleText = missingRoleKeywords.take(12).joinToString("\n") { keyword ->
            "• $keyword → ${keywordAddSuggestion(keyword)}"
        }.ifBlank { "• No major missing role keyword detected." }
        val foundJdText = bulletList(foundJdKeywords, "No matching JD keywords detected yet.")
        val missingJdText = missingJdKeywords.take(12).joinToString("\n") { keyword ->
            "• $keyword → ${keywordAddSuggestion(keyword)}"
        }.ifBlank { "• No major missing JD keyword detected." }
        val actionVerbExplanation = explainActionVerbScore(actionVerbScore, resumeText)
        val measurableExplanation = explainMeasurableImpactScore(measurableImpactScore, resumeText)
        val clarityExplanation = explainSectionClarityScore(sectionClarityScore, resumeText)
        val missingSections = detectMissingSections(resumeText)

        val basicFeedback = """
Overview:
• Resume reviewed for $targetRole at $experienceLevel level.
• Found ${foundKeywords.size} relevant keyword(s) from the role/JD scan.
• Overall fit: ${alignmentLabel(overallScore)}.

Category signals:
• Keyword Match: ${scoreRatingLabel(keywordMatchScore)}
• Measurable Details: ${scoreRatingLabel(measurableImpactScore)}
• Strong Action Verbs: ${scoreRatingLabel(actionVerbScore)}
• Section Clarity: ${scoreRatingLabel(sectionClarityScore)}

Top fixes:
1. Add missing role/JD keywords honestly.
2. Add measurable impact only where you can support it with real outcomes.
3. Strengthen weak bullets with concrete tools, responsibilities, and verified outcomes.
        """.trimIndent()

        val missingKeywordsHook = if (missingKeywords.isEmpty()) {
            "Your resume covers the main keywords detected for this role/JD."
        } else {
            "Possible missing keywords: ${missingKeywords.take(3).joinToString(", ")}. Watch an ad to see the full keyword and gap analysis."
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
Role Keywords Found:
$foundRoleText

Role Keywords Missing:
$missingRoleText

JD Keywords Found:
$foundJdText

JD Keywords Missing:
$missingJdText

Where to add missing keywords honestly:
${missingKeywords.take(8).joinToString("\n") { "• $it → ${keywordAddSuggestion(it)}" }.ifBlank { "• No priority keyword placement needed from current inputs." }}
        """.trimIndent()

        val bespoke = if (jobSpecification.isBlank()) {
            "Paste a job description next time for tailored JD matching."
        } else {
            """
JD-specific missing points:
${missingJdKeywords.take(10).joinToString("\n") { "• $it → ${keywordAddSuggestion(it)}" }.ifBlank { "• No major JD-specific keyword gaps detected." }}

JD keywords already evidenced:
${foundJdKeywords.take(10).joinToString("\n") { "• $it" }.ifBlank { "• No clear JD keyword matches detected yet." }}
            """.trimIndent()
        }

        val fullReport = """
Full Heuristic Analysis

Category Findings:
• Keyword Match: ${scoreRatingLabel(keywordMatchScore)} — Found ${foundKeywords.size} of ${combinedKeywords.size} expected role/JD keywords.
• Measurable Details: ${scoreRatingLabel(measurableImpactScore)}
$measurableExplanation

• Strong Action Verbs: ${scoreRatingLabel(actionVerbScore)}
$actionVerbExplanation

• Section Clarity: ${scoreRatingLabel(sectionClarityScore)}
$clarityExplanation

• Role Relevance: ${scoreRatingLabel(roleRelevanceScore)} — Based on resume evidence, keyword match, and job description alignment.

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
        if (totalCount == 0) return 45
        val ratio = foundCount.toDouble() / totalCount.toDouble()
        val base = when {
            ratio >= 0.80 -> 92
            ratio >= 0.65 -> 82
            ratio >= 0.50 -> 72
            ratio >= 0.35 -> 58
            ratio >= 0.20 -> 45
            ratio > 0.0 -> 32
            else -> 20
        }
        val cap = when {
            foundCount <= 0 -> 20
            foundCount <= 2 -> 35
            foundCount == 3 -> 45
            foundCount <= 5 -> 60
            foundCount <= 9 -> 80
            else -> 95
        }
        return minOf(base, cap).coerceIn(20, 95)
    }

    private fun calculateMeasurableImpactScore(resumeText: String): Int {
        val metricCount = countMetricSignals(resumeText)
        val resultWordCount = countResultWords(stripContactNoise(resumeText))
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
        return resumeLines(resumeText)
            .map { stripContactNoise(it).replace(Regex("""\s+"""), " ").trim() }
            .filter { it.length >= 10 }
            .filter { hasMeasurableImpactSignal(it) }
            .map { shortenLabel(it, 150) }
            .distinct()
            .take(5)
    }

    private fun stripContactNoise(text: String): String {
        return text
            .replace(Regex("""\b[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}\b"""), " ")
            .replace(Regex("""https?://\S+|www\.\S+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\b(?:linkedin|github)\.com/\S+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""(?<!\w)(?:\+?\d[\d\s().-]{7,}\d)(?!\w)"""), " ")
            .replace(Regex("""\b\d{7,}\b"""), " ")
            .replace(Regex("""\b(?:19|20)\d{2}\b"""), " ")
    }

    private fun hasMeasurableImpactSignal(line: String): Boolean {
        val cleaned = stripContactNoise(line)
        val lower = cleaned.lowercase()
        val hasNumber = Regex("""\b\d+(?:[.,]\d+)?\+?\b""").containsMatchIn(lower)
        val hasPercentage = Regex("""\b\d+(?:[.,]\d+)?\s*%""").containsMatchIn(lower)
        val hasCurrency = Regex("""[$₹€£]\s*\d|\b\d+(?:[.,]\d+)?\s*(?:usd|inr|eur|gbp|dollars?|rupees?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)
        val impactUnits = listOf(
            "users", "user", "customers", "customer", "clients", "client", "hours", "hour", "days", "day", "weeks", "week", "months", "month",
            "leads", "lead", "sales", "projects", "project", "reports", "report", "dashboards", "dashboard", "revenue", "cost", "budget"
        )
        val impactWords = listOf("improved", "reduced", "increased", "saved", "optimized", "accelerated", "streamlined", "delivered", "resolved")
        val unitPattern = impactUnits.joinToString("|")
        val hasImpactUnitNearNumber = Regex("""\b\d+(?:[.,]\d+)?\+?\s*(?:$unitPattern)\b|\b(?:$unitPattern)\b\W{0,24}\b\d+(?:[.,]\d+)?\+?\b""", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)
        val hasNumberAndImpactWord = hasNumber && impactWords.any { lower.contains(it) }
        return hasPercentage || hasCurrency || hasImpactUnitNearNumber || hasNumberAndImpactWord
    }

    private fun buildOptimizedBulletSuggestion(bullet: String, roleAndJdKeywords: List<String>): String {
        val shortenedBullet = shortenLabel(bullet, 120)
        val lower = bullet.lowercase()
        val detectedKeyword = roleAndJdKeywords
            .filter { it.length >= 3 }
            .firstOrNull { lower.contains(it.lowercase()) }
        val hasActionVerb = strongActionVerbs.any { Regex("""\b${Regex.escape(it)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(bullet) }
        val hasMetric = hasMeasurableImpactSignal(bullet)
        val looksGeneric = lower.startsWith("worked on") || lower.startsWith("responsible for") || lower.startsWith("helped") ||
            lower.split(Regex("""\s+""")).size < 8
        val advice = when {
            detectedKeyword != null -> "Make the tool usage more specific: explain what you built, changed, tested, analyzed, or delivered using $detectedKeyword."
            hasActionVerb -> "Keep the strong action verb, but add scope/context: who used it, what system/process it affected, and what changed."
            hasMetric -> "Preserve the measurable result and clarify how you achieved it."
            looksGeneric -> "Make this less generic by naming the project/process, your exact responsibility, and the concrete output."
            else -> "Add more concrete evidence: project context, responsibility, tool used, and outcome if verifiable."
        }
        return "\"$shortenedBullet\" → $advice"
    }

    private fun extractActionVerbExamples(resumeText: String): List<String> {
        val regex = Regex("""\b(${strongActionVerbs.joinToString("|")})\b""", RegexOption.IGNORE_CASE)
        return resumeLines(resumeText).filter { regex.containsMatchIn(it) }.map { shortenLabel(it, 150) }.distinct().take(5)
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

        val uniqueSignals = signals.map { shortenLabel(it, 120) }.filter { it.isNotBlank() }.distinct().take(4)
        if (uniqueSignals.isEmpty()) {
            return "Q1. What real project, skill, or achievement best supports your fit for $targetRole?\nStrong answer should mention: concrete resume evidence and a verifiable result."
        }

        return uniqueSignals.take(4).mapIndexed { index, signal ->
            val focus = when {
                signal.startsWith("Detected skill/keyword:") -> "how you used this skill in a real $targetRole context"
                signal.startsWith("JD match:") -> "how this JD requirement appears in your resume evidence"
                Regex("[%0-9]").containsMatchIn(signal) -> "the measurable result and how it was achieved"
                else -> "the work behind this resume line"
            }
            "Q${index + 1}. Your resume mentions \"$signal\". Can you explain $focus, your exact role, and what outcome you can verify?\nStrong answer should mention: specific resume evidence and a verifiable result."
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

    private fun countMetricSignals(text: String): Int = resumeLines(stripContactNoise(text)).count { hasMeasurableImpactSignal(it) }

    private fun countResultWords(text: String): Int {
        val lower = text.lowercase()
        return listOf("improved", "reduced", "increased", "saved", "optimized", "grew", "accelerated", "streamlined", "delivered", "resolved").count { lower.contains(it) }
    }

    private fun extractCandidateBullets(resumeText: String): List<String> = resumeLines(resumeText)
        .filter { line -> line.length >= 25 && !looksLikeHeading(line) }
        .take(12)

    private fun findWeakBullets(resumeText: String): List<String> = resumeLines(resumeText)
        .filterNot { isBareStopwordFragment(it) }
        .filter { isUsefulResumeFragment(it) }
        .filter { line ->
            val lower = line.lowercase()
            lower.startsWith("worked on") || lower.startsWith("responsible for") || lower.startsWith("helped") ||
                (!Regex("[%0-9]").containsMatchIn(line) && strongActionVerbs.none { lower.contains(it) })
        }
        .map { line ->
            val label = shortenLabel(line, 120)
            if (looksLikeSplitSentenceFragment(line)) "... $label" else label
        }
        .distinct()
        .take(5)

    private fun resumeLines(resumeText: String): List<String> = resumeText
        .split('\n', '•', '●', '–', '-')
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }
        .filterNot { isBareStopwordFragment(it) }

    private fun isBareStopwordFragment(line: String): Boolean {
        val normalized = line.lowercase()
            .replace(Regex("""[^a-z]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return normalized in setOf("and", "or", "with", "for", "to", "in", "on", "of", "the", "a", "an")
    }

    private fun isUsefulResumeFragment(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length in 18..140) return true
        if (trimmed.length > 140) return false
        return containsMeaningfulTechnicalOrProjectContent(trimmed)
    }

    private fun containsMeaningfulTechnicalOrProjectContent(line: String): Boolean {
        val lower = line.lowercase()
        val meaningfulTerms = listOf(
            "api", "sql", "python", "java", "kotlin", "android", "dashboard", "project", "app", "database", "analytics",
            "excel", "tableau", "power bi", "figma", "jira", "agile", "scrum", "crm", "campaign", "research", "prototype"
        )
        return meaningfulTerms.any { lower.contains(it) } || Regex("""\d|%|\$""").containsMatchIn(line)
    }

    private fun looksLikeSplitSentenceFragment(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false
        val lower = trimmed.lowercase()
        val fragmentStarters = listOf("and ", "or ", "with ", "for ", "to ", "in ", "on ", "of ")
        return fragmentStarters.any { lower.startsWith(it) } || trimmed.first().isLowerCase()
    }

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
        val normalized = jobSpecLower.replace(Regex("""[^a-z0-9+/\s.-]"""), " ").replace(Regex("""\s+"""), " ").trim()
        val knownKeywords = possibleKeywords.filter { normalized.contains(it) }
        val stopwords = setOf(
            "the", "and", "or", "with", "for", "from", "this", "that", "role", "candidate", "experience", "years", "year", "team", "work", "ability", "strong", "good", "excellent", "required", "preferred", "plus", "etc",
            "must", "have", "will", "you", "our", "your", "about", "across", "responsibilities", "responsibility", "requirements", "requirement", "skills", "skill", "knowledge", "looking", "seeking", "join", "company", "business",
            "manage", "support", "using", "hands", "proven", "nice", "bonus", "degree", "bachelor", "master", "field", "related", "environment", "fast", "paced", "written", "verbal", "highly", "detail", "oriented"
        )
        val technicalNouns = setOf(
            "api", "apis", "sql", "python", "java", "kotlin", "javascript", "typescript", "react", "node", "android", "aws", "azure", "gcp", "tableau", "figma", "jira", "agile", "scrum", "analytics", "dashboard", "dashboards", "database", "databases", "crm", "seo", "sem", "excel", "power", "bi", "testing", "automation", "forecasting", "budgeting", "reporting", "research", "roadmap", "pipeline", "recruitment", "onboarding", "prototype", "prototyping"
        )
        fun normalizeToken(token: String): String = token.trim('.', '-', '/')
        fun isUsefulToken(token: String): Boolean {
            return token.length >= 3 && token.none { it.isDigit() } && token !in stopwords
        }
        fun hasTechnicalToken(value: String): Boolean = value.split(" ").any { token -> token in technicalNouns || token.removeSuffix("s") in technicalNouns }

        val rawTokens = normalized.split(Regex("""\s+""")).map(::normalizeToken).filter { it.isNotBlank() }
        val usefulTokens = rawTokens.filter(::isUsefulToken)
        val wordKeywords = usefulTokens.groupingBy { it }.eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { if (hasTechnicalToken(it.first)) 1 else 0 }.thenBy { it.first })
            .map { it.first }

        val phraseKeywords = rawTokens.zipWithNext()
            .mapNotNull { (first, second) ->
                if (isUsefulToken(first) && isUsefulToken(second)) "$first $second" else null
            }
            .filterNot { phrase -> knownKeywords.any { it.equals(phrase, ignoreCase = true) } }
            .groupingBy { it }.eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { if (hasTechnicalToken(it.first)) 1 else 0 }.thenBy { it.first })
            .map { it.first }

        val dynamicKeywords = (phraseKeywords + wordKeywords).distinctBy { it.lowercase() }.take(15)
        return (knownKeywords + dynamicKeywords).distinctBy { it.lowercase() }.take(25)
    }

    private fun suggestWhereToAddKeyword(keyword: String): String {
        val lower = keyword.lowercase()
        return when {
            lower in listOf("sql", "excel", "python", "power bi", "tableau", "google analytics", "jira", "git", "kotlin", "java", "figma", "crm") -> "Skills"
            lower.contains("project") || lower.contains("dashboard") || lower.contains("prototype") || lower.contains("portfolio") -> "Projects"
            lower.contains("summary") || lower.contains("communication") || lower.contains("strategy") || lower.contains("leadership") -> "Summary"
            else -> "Experience"
        }
    }

    private fun keywordAddSuggestion(keyword: String): String {
        return when (suggestWhereToAddKeyword(keyword)) {
            "Skills" -> "Add under Skills if you have used it."
            "Projects" -> "Add under Projects if your project involved it."
            "Summary" -> "Add under Summary if you can clearly evidence it."
            else -> "Add under Experience if you actually did this work."
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
    private fun scoreRatingLabel(score: Int): String = when {
        score >= 85 -> "Excellent"
        score >= 70 -> "Good"
        score >= 50 -> "Needs Improvement"
        else -> "Lacking"
    }

    private fun alignmentLabel(score: Int): String = when {
        score >= 85 -> "Strong role fit"
        score >= 70 -> "Good role fit"
        score >= 50 -> "Moderate role fit"
        else -> "Low role fit"
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
