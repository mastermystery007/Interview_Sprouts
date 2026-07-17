package com.example.interviewsprouts

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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

private const val LOCKED_TAB_MESSAGE = "Watch an ad to unlock detailed analysis for this section."

class ResumeReportActivity : AppCompatActivity() {
    private lateinit var tabStrengths: TextView
    private lateinit var tabGaps: TextView
    private lateinit var tabKeywords: TextView
    private lateinit var tabBespoke: TextView
    private lateinit var textTabContent: TextView
    private val advancedLoadingHandler = Handler(Looper.getMainLooper())
    private var advancedLoadingRunnable: Runnable? = null
    private var advancedLoadingDotCount = 0
    private var isFirstAdUnlocked = false
    private var isSecondAdUnlocked = false

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
        val scoreLabel =
            scoreRatingLabel(report.overallScore)

        findViewById<TextView>(R.id.textScoreValue).apply {
            text = scoreLabel

            textSize = when {
                scoreLabel.length >= 17 -> 16f
                scoreLabel.length >= 13 -> 18f
                else -> 24f
            }
        }
        findViewById<TextView>(R.id.textAlignmentLabel).text = alignmentLabel(report.overallScore)
        findViewById<TextView>(R.id.textJdChip).text = if (jdProvided) "JD: Attached" else "JD: Not attached"
        findViewById<TextView>(R.id.chipTargetRole).text = shortenLabel(targetRole, 24)
        findViewById<TextView>(R.id.chipExperienceLevel).text = experienceLevel
        findViewById<TextView>(R.id.chipJdStatus).text = if (jdProvided) "JD: Attached" else "JD: Not attached"
        findViewById<ScoreCircleView>(R.id.scoreCircleView).setScore(report.overallScore)

        findViewById<TextView>(R.id.textBasicFeedback).text = applyReportFormatting(report.basicFeedback)
        findViewById<TextView>(R.id.textMissingKeywordsHook).text = report.missingKeywordsHook
        findViewById<TextView>(R.id.textFullReport).text = applyReportFormatting(report.fullReport)
        findViewById<TextView>(R.id.textAdvancedLockedMessage).text = "Get tailored resume improvements and resume-specific interview questions.\n\nYour resume text and job description are sent to the backend only after you choose to view this analysis."
        findViewById<TextView>(R.id.textLocalSaveNote).text = "Saved reports are stored locally on this device only."

        tabStrengths = findViewById(R.id.tabStrengths)
        tabGaps = findViewById(R.id.tabGaps)
        tabKeywords = findViewById(R.id.tabKeywords)
        tabBespoke = findViewById(R.id.tabBespoke)
        textTabContent = findViewById(R.id.textTabContent)

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

        val tabScrollContainer =
            findViewById<View>(R.id.tabScrollContainer)

        val textFullReport =
            findViewById<TextView>(R.id.textFullReport)

        val textLockedReportMessage =
            findViewById<TextView>(R.id.textLockedReportMessage)

        val textAdvancedLockedMessage =
            findViewById<TextView>(R.id.textAdvancedLockedMessage)

        val textAdvancedLlmReview =
            findViewById<TextView>(R.id.textAdvancedLlmReview)

        val btnUnlockFullReport =
            findViewById<Button>(R.id.btnUnlockFullReport)

        val btnUnlockAdvancedLlmReview =
            findViewById<Button>(R.id.btnUnlockAdvancedLlmReview)

        val headerUnlockDetailed =
            findViewById<TextView>(R.id.headerUnlockDetailed)

        val headerDetailedAnalysis =
            findViewById<TextView>(R.id.headerDetailedAnalysis)

        val headerUnlockAdvanced =
            findViewById<TextView>(R.id.headerUnlockAdvanced)

        val headerAdvancedReview =
            findViewById<TextView>(R.id.headerAdvancedReview)

        headerUnlockDetailed.visibility = View.VISIBLE
        textLockedReportMessage.visibility = View.VISIBLE
        btnUnlockFullReport.visibility = View.VISIBLE

        headerDetailedAnalysis.visibility = View.GONE
        tabScrollContainer.visibility = View.GONE
        textTabContent.visibility = View.GONE
        textFullReport.visibility = View.GONE

        headerUnlockAdvanced.visibility = View.GONE
        textAdvancedLockedMessage.visibility = View.GONE
        btnUnlockAdvancedLlmReview.visibility = View.GONE

        headerAdvancedReview.visibility = View.GONE
        textAdvancedLlmReview.visibility = View.GONE

        btnUnlockFullReport.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("View Gold Star Analysis")
                .setMessage("A short ad unlocks the complete role-fit breakdown for this report.")
                .setPositiveButton("Watch Ad") { _, _ ->
                    isFirstAdUnlocked = true

                    headerUnlockDetailed.visibility = View.GONE
                    textLockedReportMessage.visibility = View.GONE
                    btnUnlockFullReport.visibility = View.GONE

                    headerDetailedAnalysis.visibility = View.VISIBLE
                    tabScrollContainer.visibility = View.VISIBLE
                    textTabContent.visibility = View.VISIBLE
                    textFullReport.visibility = View.VISIBLE

                    headerUnlockAdvanced.visibility = View.VISIBLE
                    textAdvancedLockedMessage.visibility = View.VISIBLE
                    btnUnlockAdvancedLlmReview.visibility = View.VISIBLE

                    selectTab(
                        tabGaps,
                        report.gapsContent
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnUnlockAdvancedLlmReview.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("View Diamond Star Analysis")
                .setMessage(
                    "A short ad opens tailored resume suggestions " +
                        "and resume-specific interview questions."
                )
                .setPositiveButton("Watch Ad") { _, _ ->
                    isSecondAdUnlocked = true

                    headerUnlockAdvanced.visibility = View.GONE
                    textAdvancedLockedMessage.visibility = View.GONE
                    btnUnlockAdvancedLlmReview.visibility = View.GONE

                    headerAdvancedReview.visibility = View.VISIBLE
                    textAdvancedLlmReview.visibility = View.VISIBLE

                    startAdvancedLoadingAnimation(
                        textAdvancedLlmReview
                    )

                    requestAdvancedAiReview(
                        resumeText,
                        targetRole,
                        experienceLevel,
                        jobSpecification,
                        textAdvancedLlmReview
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnSaveReport).setOnClickListener {
            saveReportLocally(targetRole, experienceLevel, report)
            Toast.makeText(this, "Report saved locally on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAdvancedLoadingAnimation(
        textView: TextView
    ) {
        stopAdvancedLoadingAnimation()
        advancedLoadingDotCount = 0

        advancedLoadingRunnable = object : Runnable {
            override fun run() {
                val dots =
                    ".".repeat(advancedLoadingDotCount + 1)

                textView.text =
                    "Generating Diamond Star Analysis$dots"

                advancedLoadingDotCount =
                    (advancedLoadingDotCount + 1) % 3

                advancedLoadingHandler.postDelayed(
                    this,
                    500L
                )
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

    private fun selectTab(selected: TextView, content: String) {
        listOf(tabGaps, tabStrengths, tabKeywords, tabBespoke).forEach { tab ->
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
                "tailoredResumeSuggestions",
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

    private fun formatBackendAdvancedReview(
        response: ResumeAiResponse,
        offlineFallback: String
    ): String {
        val sections = mutableListOf<String>()

        val advancedReview =
            compactAdvancedReview(response.advancedReview)

        val suggestions =
            compactSuggestions(
                response.tailoredResumeSuggestions
            )

        val questions =
            compactQuestions(response.interviewQuestions)

        if (advancedReview.isNotBlank()) {
            sections.add(
                "Diamond Star Analysis\n$advancedReview"
            )
        }

        if (suggestions.isNotBlank()) {
            sections.add(
                "Resume Improvement Suggestions\n$suggestions"
            )
        }

        if (questions.isNotBlank()) {
            sections.add(
                "Resume-Specific Interview Questions\n$questions"
            )
        }

        return sections.joinToString("\n\n").ifBlank {
            "AI backend returned no usable content. " +
                "Showing offline fallback.\n\n" +
                offlineFallback.substringAfter(
                    "\n\n",
                    offlineFallback
                )
        }
    }

    private fun compactAdvancedReview(text: String?): String {
        val cleaned = stripDuplicateSectionHeading(
            text.orEmpty(),
            "Diamond Star Analysis",
            "Advanced AI Review",
            "Advanced Review",
            "AI Review"
        )
        if (cleaned.isBlank() || isNullLiteral(cleaned)) return ""

        val items = extractBulletLikeItems(cleaned, splitSemicolons = true)
            .map { sanitizeAdvancedBullet(it) }
            .filter { it.isNotBlank() && !isNullLiteral(it) && !looksLikeStandaloneHeading(it) && !isRemovedAdvancedLine(it) }
            .distinctBy { it.lowercase() }
            .take(4)

        return items.joinToString("\n") { "• $it" }
    }

    private fun compactSuggestions(text: String?): String {
        val cleaned = removeLegacySuggestionHeadings(text.orEmpty())
        if (cleaned.isBlank() || isNullLiteral(cleaned)) return ""

        val items = extractBulletLikeItems(cleaned, splitSemicolons = false)
            .map { sanitizeSuggestionBullet(removeLegacySuggestionHeadings(it)) }
            .filter { it.isNotBlank() && !isNullLiteral(it) && !looksLikeStandaloneHeading(it) && !isRemovedAdvancedLine(it) }
            .distinctBy { it.lowercase() }
            .take(4)

        return items.joinToString("\n") { "• $it" }
    }

    private fun removeLegacySuggestionHeadings(text: String): String {
        var cleaned = text
        listOf(
            "Tailored Resume Suggestions",
            "Resume Improvement Suggestions"
        ).forEach { heading ->
            cleaned = cleaned.replace(Regex("""(?im)^\s*""" + Regex.escape(heading) + """\s*[:：-]?\s*$"""), "")
            cleaned = cleaned.replace(Regex("(?i)" + Regex.escape(heading) + """\s*[:：-]\s*"""), "")
        }
        return cleaned.trim()
    }

    private fun compactQuestions(text: String?): String {
        val cleaned = stripDuplicateSectionHeading(text.orEmpty(), "Resume-Specific Interview Questions", "Interview Questions")
        if (cleaned.isBlank() || isNullLiteral(cleaned)) return ""

        val normalized = cleaned
            .replace(Regex("""(?i)(?=\bQ\d+[.)]\s+)"""), "\n")
            .replace(Regex("""(?m)^\s*[-*•]\s*"""), "")

        val questionPrefix = Regex("""^(?:Q\d+\.|\d+[.)])\s*(.+)""", RegexOption.IGNORE_CASE)
        val questions = normalized.lines()
            .map { normalizeVisibleLine(it) }
            .flatMap { line -> splitQuestionLine(line, questionPrefix) }
            .mapNotNull { rawQuestion -> sanitizeQuestion(rawQuestion) }
            .filter { isCompleteSpecificQuestion(it) }
            .distinctBy { it.lowercase() }
            .take(4)

        return questions.mapIndexed { index, question -> "Q${index + 1}. $question" }.joinToString("\n")
    }

    private fun extractBulletLikeItems(text: String, splitSemicolons: Boolean): List<String> {
        val normalized = text
            .replace("\r", "\n")
            .replace(Regex("""\s*[•▪◦]\s*"""), "\n• ")
            .replace(Regex("""(?m)(?<!^)\s+(?=\d+[.)]\s+)"""), "\n")
            .replace(Regex("""\s+-\s+"""), "\n")

        val splitPattern = if (splitSemicolons) {
            Regex("""\s*(?:;|[•▪◦]|\b\d+[.)])\s*""")
        } else {
            Regex("""\s*(?:[•▪◦]|\b\d+[.)])\s*""")
        }

        val lineItems = normalized.lines()
            .map { normalizeVisibleLine(it) }
            .filter { it.isNotBlank() && !looksLikeStandaloneHeading(it) }
            .flatMap { line ->
                val base = line.trim().trimStart('-', '*', '•', '▪', '◦').replace(Regex("""^\d+[.)]\s*"""), "").trim()
                base.split(splitPattern)
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return lineItems.ifEmpty { splitParagraphIntoSentences(text) }
    }

    private fun sanitizeAdvancedBullet(text: String): String {
        val withoutNestedMarkers = text
            .replace(Regex("[•▪◦]+"), " ")
            .replace(Regex("""\b\d+[.)]\s*"""), " ")
            .replace(Regex("""\s+-\s+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return firstSentence(withoutNestedMarkers)
    }

    private fun sanitizeSuggestionBullet(text: String): String = text
        .replace(Regex("[•▪◦]+"), " ")
        .replace(Regex("""^\d+[.)]\s*"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun splitQuestionLine(line: String, questionPrefix: Regex): List<String> {
        if (line.isBlank() || looksLikeStandaloneHeading(line)) return emptyList()
        val prefixedQuestion = questionPrefix.find(line)?.groupValues?.getOrNull(1)
        val candidate = prefixedQuestion ?: line
        return candidate.split(Regex("""(?<=\?)\s+(?=(?:Q\d+\.|\d+[.)])?\s*[A-Z])"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun sanitizeQuestion(text: String): String? {
        var question = text.trim().trimStart('-', '*', '•').replace(Regex("""^(?:Q\d+\.|\d+[.)])\s*""", RegexOption.IGNORE_CASE), "").trim()
        question = question.replace(Regex("""^(?:Follow[- ]?up|Follow up|Probe|Behavioral)\s*[:：-]\s*""", RegexOption.IGNORE_CASE), "").trim()
        question = question
            .substringBefore("Strong answer should mention", question)
            .substringBefore("Suggested answer", question)
            .substringBefore("Why this may be asked", question)
            .substringBefore("Based on", question)
            .substringBefore("Answer:", question)
            .substringBefore("Hint:", question)
            .trim()
        if (Regex("""\b(follow[- ]?up|probe|behavioral)\b""", RegexOption.IGNORE_CASE).containsMatchIn(question)) return null
        if (!question.endsWith("?")) return null
        return question
    }

    private fun isCompleteSpecificQuestion(question: String): Boolean {
        val lower = question.lowercase()
        if (question.length < 45) return false
        if (lower.contains("tell me about") || lower.contains("describe your experience generally")) return false
        val genericPatterns = listOf(
            "why this role", "tell me about yourself", "strengths", "weaknesses", "first 30 days", "teamwork", "conflict"
        )
        if (genericPatterns.any { lower.contains(it) }) return false
        return true
    }

    private fun firstSentence(text: String): String {
        val match = Regex("""^(.+?[.!?])(?:\s+|$)""").find(text)
        return (match?.groupValues?.getOrNull(1) ?: text).trim()
    }

    private fun stripDuplicateSectionHeading(text: String, vararg headings: String): String {
        var cleaned = text.trim()
        headings.forEach { heading ->
            val pattern = Regex("^" + Regex.escape(heading) + "\\s*[:：-]?\\s*", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replaceFirst(pattern, "").trimStart()
        }
        return cleaned.trim()
    }

    private fun normalizeVisibleLine(line: String): String = line.trim().replace(Regex("\\s+"), " ")

    private fun isNullLiteral(text: String): Boolean = text.trim().equals("null", ignoreCase = true)

    private fun isRemovedAdvancedLine(line: String): Boolean = isRemovedQuestionLine(line) ||
        isSectionHeadingLine(line, "Metric Evidence Detected") ||
        line.startsWith("Honesty reminder", ignoreCase = true) ||
        line.startsWith("Add suggested skills or tools only if they are true", ignoreCase = true)

    private fun isRemovedQuestionLine(line: String): Boolean =
        Regex(
            """^(Strong answer should mention|Based on|Why this may be asked|Follow[- ]?up probe|Follow[- ]?up|Probe|Behavioral|Answer|Suggested answer|Hint)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(line.trim())

    private fun looksLikeStandaloneHeading(
        line: String
    ): Boolean = listOf(
        "Diamond Star Analysis",
        "Advanced AI Review",
        "Advanced Review",
        "AI Review",
        "Metric Evidence Detected",
        "Resume-Specific Interview Questions",
        "Interview Questions",
        "Optional Resume Point Rewrites",
        "Optional Resume Point Rewrites / cleanup notes",
        "Tailored Resume Suggestions",
        "Resume Improvement Suggestions"
    ).any { heading ->
        isSectionHeadingLine(line, heading)
    }

    private fun isSectionHeadingLine(line: String, heading: String): Boolean =
        line.trim().trimEnd(':', '：', '-', '–').trim().equals(heading, ignoreCase = true)

    private fun splitParagraphIntoSentences(text: String): List<String> = text
        .replace("\n", " ")
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim().trimStart('-', '*', '•').replace(Regex("^\\d+[.)]\\s*"), "").trim() }
        .filter { it.isNotBlank() && !isNullLiteral(it) && !looksLikeStandaloneHeading(it) && !isRemovedAdvancedLine(it) }

    private fun applyReportFormatting(text: String): CharSequence {
        val builder = SpannableStringBuilder(text)
        val headings = listOf(
            "Detailed Analysis",
            "Diamond Star Analysis",
            "Resume Improvement Suggestions",
            "Resume-Specific Interview Questions",
            "Achievement evidence:",

            "Top improvement",
            "Why this matters",
            "Overall fit",
            "JD status",

            "Missing role evidence",
            "Weak or vague bullets",
            "Quantified impact",
            "First recommended action",

            "Strengths",
            "Role signals found",
            "Evidence highlights",
            "Tool and skill evidence",

            "Keywords",
            "Role keywords found",
            "Role keywords to strengthen",
            "JD keywords found",
            "JD keywords to strengthen",
            "Suggested placement",

            "JD Match",
            "JD-specific gaps",
            "Where to add them",
            "Already evidenced",

            "Score Breakdown",
            "Relevant keyword coverage",
            "Quantified achievement evidence",
            "Action-oriented bullet writing",
            "Resume section clarity",
            "Target-role alignment",

            "Target-role and experience alignment",
            "Evidence quality",
            "Resume Structure",
            "Priority Fixes",

            "Top improvement:",
            "Main concern:",
            "Measurable impact:",
            "Section clarity:",
            "Main structure concern:",
            "Strengthen:",
            "Add:",
            "Rewrite:",

            "Status:",
            "Current analysis:",
            "For more tailored matching:",
            "Keywords found:",
            "Keywords not clearly evidenced:"
        )
        headings.forEach { heading ->
            applySpanToMatches(text, heading) { start, end ->
                builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        val positiveColor = Color.rgb(24, 128, 82)
        val gapColor = Color.rgb(190, 82, 32)
        val positiveSignals = listOf(
            "Found keywords:",
            "Role Keywords Found:",
            "JD Keywords Found:",
            "Clearly evidenced",
            "Proof signal"
        )
        val gapSignals = listOf(
            "Missing keywords:",
            "Role Keywords Missing:",
            "JD Keywords Not Found:",
            "Weak bullets:",
            "Missing measurable impact:",
            "Main gap:",
            "Main hiring concern:",
            "Interview risk:",
            "Recruiter Red Flags:",
            "Not clearly evidenced"
        )
        positiveSignals.forEach { signal ->
            applySpanToMatches(text, signal) { start, end ->
                builder.setSpan(ForegroundColorSpan(positiveColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        gapSignals.forEach { signal ->
            applySpanToMatches(text, signal) { start, end ->
                builder.setSpan(ForegroundColorSpan(gapColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        applyStatusLineColors(
            builder,
            text
        )
        return builder
    }

    private fun applyStatusLineColors(
        builder: SpannableStringBuilder,
        text: String
    ) {
        val positiveColor =
            Color.rgb(24, 128, 82)

        val warningColor =
            Color.rgb(190, 82, 32)

        val positivePattern = Regex(
            """(?im)(?:^|[—–:]\s*)(Strong role fit|Good role fit|Excellent|Good)\s*$"""
        )

        val warningPattern = Regex(
            """(?im)(?:^|[—–:]\s*)(Needs Improvement|Lacking|Moderate role fit|Low role fit)\s*$"""
        )

        positivePattern.findAll(text).forEach { match ->
            val statusGroup =
                match.groups[1]
                    ?: return@forEach

            builder.setSpan(
                ForegroundColorSpan(
                    positiveColor
                ),
                statusGroup.range.first,
                statusGroup.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        warningPattern.findAll(text).forEach { match ->
            val statusGroup =
                match.groups[1]
                    ?: return@forEach

            builder.setSpan(
                ForegroundColorSpan(
                    warningColor
                ),
                statusGroup.range.first,
                statusGroup.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applySpanToMatches(text: String, target: String, apply: (Int, Int) -> Unit) {
        var searchStart = 0
        while (searchStart < text.length) {
            val index = text.indexOf(target, searchStart, ignoreCase = true)
            if (index == -1) break
            apply(index, index + target.length)
            searchStart = index + target.length
        }
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
        val bullets =
            selectEvidenceHighlights(
                resumeText,
                targetRole,
                jobSpecification
            ).take(4)
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
            "Achievement evidence: ${bullets.take(2).joinToString("; ").ifBlank { "No achievement-focused evidence was detected yet." }}",
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
                optimizedBullets.take(2).forEach { appendLine("• $it") }
            }
            if (missingBullets.isNotEmpty()) {
                missingBullets.take(4 - optimizedBullets.take(2).size).forEach { appendLine("• $it") }
            }
        }.trim()

        return """
Diamond Star Analysis
${compactAdvancedReview(advanced)}

Resume Improvement Suggestions
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

        val atsParserRisks =
            detectAtsParserRisks(resumeText)
        val impactSignals = extractImpactSignals(resumeText)
        val toolSignals = extractToolSignals(resumeText, targetRole, jobSpecification)
        val evidenceHighlights =
            selectEvidenceHighlights(
                resumeText,
                targetRole,
                jobSpecification
            )
        val vaguePhrases = detectVaguePhrases(resumeText)
        val genericClaims = detectGenericClaims(resumeText)
        val responsibilityNoOutcome = detectResponsibilityWithoutOutcome(resumeText)
        val missingRoleEvidence = detectMissingRoleEvidence(resumeText, targetRole, jobSpecification)

        val keywordMatchScore =
            calculateKeywordMatchScore(
                foundKeywords.size,
                combinedKeywords.size
            )

        val measurableImpactScore =
            calculateMeasurableImpactScore(resumeText)

        val actionVerbScore =
            calculateStrongActionVerbScore(resumeText)

        val sectionClarityScore =
            calculateSectionClarityScore(resumeText)

        val roleRelevanceScore =
            calculateRoleRelevanceScore(
                keywordMatchScore,
                sectionClarityScore,
                jobSpecification.isNotBlank()
            )

        val seniorityAlignmentScore =
            calculateSeniorityAlignmentScore(
                resumeText,
                experienceLevel
            )

        val evidenceDensityScore =
            calculateEvidenceDensityScore(resumeText)

        val specificityScore =
            calculateSpecificityScore(resumeText)

        val atsReadabilityScore =
            calculateAtsReadabilityScore(resumeText)

        val toolEvidenceScore =
            calculateToolEvidenceScore(
                resumeText,
                targetRole,
                jobSpecification
            )

        val responsibilityOutcomeScore =
            calculateResponsibilityOutcomeScore(
                resumeText
            )

        val evidenceSpecificityScore = (
            (
                evidenceDensityScore +
                    specificityScore +
                    responsibilityOutcomeScore
                ) / 3.0
            ).roundToInt()

        val bulletQualityScore =
            calculateBulletQualityScore(resumeText)

        val overallScore = weightedOverallScore(
            jdRoleMatchScore = keywordMatchScore,
            evidenceConfidenceScore =
                evidenceSpecificityScore,
            measurableImpactScore =
                measurableImpactScore,
            bulletQualityScore = bulletQualityScore,
            seniorityAlignmentScore =
                seniorityAlignmentScore,
            atsReadabilityScore = atsReadabilityScore,
            toolSkillProofScore = toolEvidenceScore,
            candidatePositioningScore =
                roleRelevanceScore,
            actionVerbScore = actionVerbScore,
            resumeText = resumeText,
            jobSpecification = jobSpecification,
            experienceLevel = experienceLevel
        )
        val sectionOrderIssues = detectSectionOrderIssues(resumeText)
        val missingSections = detectMissingSections(resumeText)
        val topGap = when {
            missingJdKeywords.isNotEmpty() ->
                "${missingJdKeywords.first()} is not clearly evidenced for the attached JD."

            missingRoleKeywords.isNotEmpty() ->
                "${missingRoleKeywords.first()} is not clearly evidenced for the selected role."

            missingSections.isNotEmpty() ->
                "${missingSections.first()} section is not clearly detected."

            atsParserRisks.isNotEmpty() ->
                atsParserRisks.first()

            else ->
                "No major role-fit gap was detected."
        }

        val topImprovement = when {
            missingJdKeywords.isNotEmpty() ->
                "Add concrete evidence of ${missingJdKeywords.first()} where it truthfully applies."

            missingRoleKeywords.isNotEmpty() ->
                "Show how you used ${missingRoleKeywords.first()} in a project, job, or measurable result."

            impactSignals.isEmpty() ->
                "Add one truthful metric such as users, %, time saved, accuracy, revenue, cost, latency, scale, or efficiency."

            responsibilityNoOutcome.isNotEmpty() ->
                "Rewrite one responsibility-only bullet to show the result or outcome."

            vaguePhrases.isNotEmpty() ->
                "Replace vague wording with a specific task, tool, and result."

            else ->
                "Improve the most important bullet by adding context, tools used, and outcome."
        }
        val bestSectionToImprove = suggestWhereToAddKeyword((missingJdKeywords + missingRoleKeywords).firstOrNull() ?: "role evidence")
        val jdStatus = if (jobSpecification.isBlank()) "JD: Not attached" else "JD: Attached"

        val basicFeedback = """
Top improvement

$topImprovement

Why this matters

$topGap

Overall fit

${alignmentLabel(overallScore)}

JD status

$jdStatus
""".trimIndent()

        val missingKeywordsHook = when {
            missingKeywords.isEmpty() && jobSpecification.isBlank() ->
                "Your resume covers the main keywords detected for the selected role."

            missingKeywords.isEmpty() ->
                "Your resume covers the main keywords detected for the selected role and JD."

            jobSpecification.isBlank() ->
                """
Keyword preview

${missingKeywords.take(3).joinToString("\n") { "• $it" }}

• View the full analysis for complete role-keyword recommendations.
                """.trimIndent()

            else ->
                """
Keyword preview

${missingKeywords.take(3).joinToString("\n") { "• $it" }}

• View the full analysis for complete role and JD recommendations.
                """.trimIndent()
        }

        val strengths = """
Strengths

Role signals found

${formatExamples((foundRoleKeywords + foundJdKeywords).distinct().take(8), "No clear role or JD signals detected yet.")}

Evidence highlights

${formatExamples(
            evidenceHighlights,
            "No achievement-focused evidence lines were detected yet."
        )}

Tool and skill evidence

${formatExamples(toolSignals, "Tools or skills are not clearly tied to work evidence yet.")}
        """.trimIndent()

        val gaps = """
Missing role evidence

${formatExamples(
            missingRoleEvidence.take(5),
            "No major missing role evidence was detected."
        )}

Weak or vague bullets

${formatExamples(
            (responsibilityNoOutcome + vaguePhrases + genericClaims)
                .distinct()
                .take(5),
            "No major weak or vague bullet was detected."
        )}

Quantified impact

${
            if (impactSignals.isEmpty()) {
                "• Add at least one truthful result involving scale, time, quality, revenue, users, accuracy, or efficiency."
            } else {
                "• Measurable evidence exists, but add outcomes to other important achievements where appropriate."
            }
        }

First recommended action

• $topImprovement
""".trimIndent()

        val keywords = """
Keywords

Role keywords found

${bulletList(foundRoleKeywords, "No matching role keywords detected yet.")}

Role keywords to strengthen

${missingRoleKeywords.take(12).joinToString("\n") { "• $it → ${keywordAddSuggestion(it)}" }.ifBlank { "• No major role keyword gap detected." }}

JD keywords found

${bulletList(foundJdKeywords, "No matching JD keywords detected yet.")}

JD keywords to strengthen

${missingJdKeywords.take(12).joinToString("\n") { "• $it → ${keywordAddSuggestion(it)}" }.ifBlank { "• No major JD keyword gap detected." }}

Suggested placement

${missingKeywords.take(8).joinToString("\n") { "• $it → ${suggestWhereToAddKeyword(it)}" }.ifBlank { "• No priority keyword placement needed from current inputs." }}
        """.trimIndent()

        val bespoke = if (jobSpecification.isBlank()) {
            """
JD Match

• Status:
  JD not attached.

• Current analysis:
  The score and recommendations are based on the selected target role.

• For more tailored matching:
  Paste a job description during the next analysis.
            """.trimIndent()
        } else {
            """
JD Match

JD-specific gaps

${formatExamples(missingJdKeywords.take(6).map { "$it → add in ${suggestWhereToAddKeyword(it)} if supported by real evidence." }, "No major JD-specific keyword gaps detected.")}

Where to add them

${formatExamples(missingRoleEvidence.take(4).map { "$it Best location: $bestSectionToImprove." }, "Current resume already covers the clearest detected JD signals.")}

Already evidenced

${formatExamples(foundJdKeywords.take(8), "No clear JD keyword matches detected yet.")}
            """.trimIndent()
        }

        val jdMatchSection =
            if (jobSpecification.isBlank()) {
                """
JD Match

• Status:
  JD not attached.

• Current analysis:
  This analysis is based on the selected target role.
        """.trimIndent()
            } else {
                val foundJdText = foundJdKeywords
                    .take(8)
                    .joinToString("\n") { "  • $it" }
                    .ifBlank {
                        "  • No clear JD keyword matches detected."
                    }

                val missingJdText = missingJdKeywords
                    .take(8)
                    .joinToString("\n") { "  • $it" }
                    .ifBlank {
                        "  • No major JD keyword gaps detected."
                    }

                """
JD Match

• Keywords found:
$foundJdText

• Keywords not clearly evidenced:
$missingJdText
        """.trimIndent()
            }

        val sectionScores = """
Score Breakdown

• Relevant keyword coverage — $keywordMatchScore/100
• Quantified achievement evidence — $measurableImpactScore/100
• Action-oriented bullet writing — $actionVerbScore/100
• Resume section clarity — $sectionClarityScore/100
• Target-role alignment — $roleRelevanceScore/100
""".trimIndent()

        val fullReport = """
$sectionScores

Target-role and experience alignment

• Overall alignment — ${alignmentLabel(roleRelevanceScore)}
• Experience-level evidence — ${scoreRatingLabel(seniorityAlignmentScore)}
• Assessment — ${
            seniorityAlignmentNote(
                resumeText,
                experienceLevel
            )
        }

Evidence quality

• Top improvement:
  $topImprovement

• Main concern:
  $topGap

• Measurable impact:
  ${
            if (impactSignals.isEmpty()) {
                "Clear measurable outcomes were not detected."
            } else {
                "Some measurable outcomes are present."
            }
        }

$jdMatchSection

Resume Structure

• Section clarity:
  $sectionClarityScore/100

• Main structure concern:
  ${
            (
                atsParserRisks +
                    sectionOrderIssues +
                    missingSections
                ).firstOrNull()
                ?: "No major resume structure concern detected."
        }

Priority Fixes

• Strengthen:
  ${
            missingKeywords.firstOrNull()
                ?: "The most important role requirement"
        }

• Add:
  A truthful measurable outcome to the most important experience or project point.

• Rewrite:
  Replace vague lines with a specific task, tool used, and real outcome.
""".trimIndent()

        return ResumeReportResult(
            overallScore,
            keywordMatchScore,
            measurableImpactScore,
            actionVerbScore,
            sectionClarityScore,
            roleRelevanceScore,
            foundKeywords,
            missingKeywords,
            basicFeedback,
            missingKeywordsHook,
            fullReport,
            strengths,
            gaps,
            keywords,
            bespoke,
            jdStatus
        )
    }

    private fun calculateEvidenceDensityScore(resumeText: String): Int {
        val lines = extractCandidateBullets(resumeText)
        if (lines.isEmpty()) return 25
        val dense = lines.count { line ->
            val lower = line.lowercase()
            val hasVerb = strongActionVerbs.any { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) }
            val hasTool = containsMeaningfulTechnicalOrProjectContent(line)
            val hasOutcome = hasMeasurableImpactSignal(line) || resultWords.any { lower.contains(it) }
            listOf(hasVerb, hasTool, hasOutcome).count { it } >= 2
        }
        return (30 + (dense.toDouble() / lines.size * 65)).roundToInt().coerceIn(20, 95)
    }

    private fun calculateSpecificityScore(resumeText: String): Int {
        val vagueCount = detectVaguePhrases(resumeText).size + detectGenericClaims(resumeText).size
        val evidenceCount = extractImpactSignals(resumeText).size + extractToolSignals(resumeText, "", "").size
        return (55 + evidenceCount * 7 - vagueCount * 8).coerceIn(20, 95)
    }

    private fun calculateSeniorityAlignmentScore(resumeText: String, experienceLevel: String): Int {
        val lower = resumeText.lowercase()
        val seniorSignals = listOf("led", "managed", "owned", "mentored", "strategy", "roadmap", "stakeholder", "cross-functional", "budget", "architecture", "hired", "trained").count { lower.contains(it) }
        val level = experienceLevel.lowercase()
        return when {
            level.contains("senior") || level.contains("lead") || level.contains("manager") -> (35 + seniorSignals * 10).coerceIn(25, 95)
            level.contains("entry") || level.contains("junior") || level.contains("intern") -> (70 + minOf(seniorSignals, 2) * 5).coerceIn(50, 90)
            else -> (50 + seniorSignals * 7).coerceIn(30, 92)
        }
    }

    private fun calculateAtsReadabilityScore(resumeText: String): Int {
        val wordCount = resumeText.split(Regex("\\s+")).count { it.isNotBlank() }
        var score = 55 + detectSectionSignals(resumeText).size * 7
        if (wordCount in 250..1100) score += 10
        if (wordCount < 120) score -= 20
        if (wordCount > 1400) score -= 15
        score -= detectSectionOrderIssues(resumeText).size * 8
        return score.coerceIn(20, 95)
    }

    private fun calculateToolEvidenceScore(resumeText: String, targetRole: String, jobSpecification: String): Int {
        val signals = extractToolSignals(resumeText, targetRole, jobSpecification).size
        return (30 + signals * 10).coerceIn(20, 95)
    }

    private fun calculateProjectExperienceDepthScore(resumeText: String): Int {
        val lines = extractCandidateBullets(resumeText)
        val depth = lines.count { line -> containsMeaningfulTechnicalOrProjectContent(line) && (hasMeasurableImpactSignal(line) || strongActionVerbs.any { line.contains(it, true) }) }
        return (35 + depth * 9).coerceIn(20, 95)
    }

    private fun calculateResponsibilityOutcomeScore(resumeText: String): Int = (80 - detectResponsibilityWithoutOutcome(resumeText).size * 10 + extractImpactSignals(resumeText).size * 4).coerceIn(20, 95)

    private fun detectVaguePhrases(resumeText: String): List<String> = resumeLines(resumeText).filter { line ->
        val lower = line.lowercase()
        listOf("hard working", "team player", "detail oriented", "fast learner", "responsible for", "worked on", "helped with", "various tasks", "good communication").any { lower.contains(it) }
    }.map { shortenLabel(it, 130) }.distinct().take(5)

    private fun detectGenericClaims(resumeText: String): List<String> = resumeLines(resumeText).filter { line ->
        val lower = line.lowercase()
        (lower.startsWith("worked on") || lower.startsWith("responsible for") || lower.startsWith("helped")) && !hasMeasurableImpactSignal(line)
    }.map { shortenLabel(it, 130) }.distinct().take(5)

    private fun detectResponsibilityWithoutOutcome(resumeText: String): List<String> = extractCandidateBullets(resumeText).filter { line ->
        val lower = line.lowercase()
        (lower.contains("responsible for") || lower.contains("managed") || lower.contains("handled") || lower.contains("worked on") || strongActionVerbs.any { lower.contains(it) }) && !hasMeasurableImpactSignal(line) && resultWords.none { lower.contains(it) }
    }.map { shortenLabel(it, 130) }.distinct().take(5)

    private fun detectMissingRoleEvidence(resumeText: String, targetRole: String, jobSpecification: String): List<String> {
        val lower = resumeText.lowercase()
        val missing = (getKeywordsForRole(targetRole) + extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()))
            .distinctBy { it.lowercase() }
            .filterNot { lower.contains(it.lowercase()) }
            .take(6)
        return missing.map { "$it is not clearly evidenced." }
    }

    private fun detectSectionOrderIssues(resumeText: String): List<String> {
        val lower = resumeText.lowercase()
        val skillsIndex = lower.indexOf("skills")
        val experienceIndex = lower.indexOf("experience")
        val educationIndex = lower.indexOf("education")
        val issues = mutableListOf<String>()
        if (skillsIndex >= 0 && experienceIndex >= 0 && skillsIndex > experienceIndex && lower.length > 1800) issues.add("Skills may be too low for ATS keyword scanning.")
        if (educationIndex >= 0 && experienceIndex >= 0 && educationIndex < experienceIndex && !lower.contains("intern")) issues.add("Experience may need to appear before Education for experienced roles.")
        return issues
    }

    private fun extractImpactSignals(resumeText: String): List<String> = extractMetricExamples(resumeText).ifEmpty {
        resumeLines(resumeText).filter { line -> resultWords.any { line.contains(it, true) } }.map { shortenLabel(it, 140) }.take(5)
    }

    private fun extractToolSignals(resumeText: String, targetRole: String, jobSpecification: String): List<String> {
        val keywords = (getKeywordsForRole(targetRole) + extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())).distinctBy { it.lowercase() }
        return resumeLines(resumeText).filter { line -> keywords.any { key -> key.length >= 3 && line.contains(key, ignoreCase = true) } }
            .map { shortenLabel(it, 140) }.distinct().take(6)
    }

    private fun seniorityAlignmentNote(resumeText: String, experienceLevel: String): String {
        val score = calculateSeniorityAlignmentScore(resumeText, experienceLevel)
        return if (score >= 70) "Experience level is supported by ownership, scope, or delivery signals." else "Experience level is partly supported, but leadership, scope, or delivery evidence is thin."
    }

    private fun rankGapSeverity(
        resumeText: String,
        targetRole: String,
        jobSpecification: String
    ): String {
        val lower = resumeText.lowercase()
        val missingCount = (getKeywordsForRole(targetRole) + extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()))
            .distinctBy { it.lowercase() }
            .count { !lower.contains(it.lowercase()) }

        return when {
            missingCount >= 8 -> "High"
            missingCount >= 4 -> "Medium"
            else -> "Low"
        }
    }

    private fun detectRecruiterRedFlags(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): List<String> {
        val flags = mutableListOf<String>()

        if (resumeText.length < 600) {
            flags.add("Resume may be too short to show enough role evidence.")
        }

        if (detectSectionSignals(resumeText).size < 3) {
            flags.add("Important resume sections are not clearly visible.")
        }

        if (extractImpactSignals(resumeText).isEmpty()) {
            flags.add("Measurable outcomes are not clearly shown.")
        }

        if (extractToolSignals(resumeText, targetRole, jobSpecification).isEmpty()) {
            flags.add("Tools or skills are not clearly connected to work evidence.")
        }

        if (detectVaguePhrases(resumeText).isNotEmpty()) {
            flags.add("Some lines sound vague or generic.")
        }

        return flags.distinct().take(5)
    }

    private fun detectAtsParserRisks(resumeText: String): List<String> {
        val risks = mutableListOf<String>()
        val lower = resumeText.lowercase()

        if (detectSectionSignals(resumeText).size < 3) {
            risks.add("Standard resume sections are not clearly detected.")
        }

        if (!lower.contains("skills")) {
            risks.add("Skills section is not clearly detected.")
        }

        if (!lower.contains("experience") && !lower.contains("projects") && !lower.contains("internship")) {
            risks.add("Experience or Projects section is not clearly detected.")
        }

        if (resumeText.length > 9000) {
            risks.add("Resume text may be too long and dense.")
        }

        return risks.distinct().take(5)
    }

    private fun diagnoseBulletQuality(resumeText: String): List<String> {
        val issues = mutableListOf<String>()
        val weakBullets = findWeakBullets(resumeText)
        val vague = detectVaguePhrases(resumeText)
        val noOutcome = detectResponsibilityWithoutOutcome(resumeText)

        if (weakBullets.isNotEmpty()) {
            issues.add("Some bullets need stronger action and clearer results.")
        }

        if (vague.isNotEmpty()) {
            issues.add("Some bullets use vague wording.")
        }

        if (noOutcome.isNotEmpty()) {
            issues.add("Some responsibility lines do not show outcomes.")
        }

        if (extractImpactSignals(resumeText).isEmpty()) {
            issues.add("Few measurable outcomes are visible.")
        }

        return issues.distinct().take(5)
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

    private fun calculateBulletQualityScore(
        resumeText: String
    ): Int {
        val bullets =
            extractCandidateBullets(resumeText)

        if (bullets.isEmpty()) {
            return 25
        }

        val total = bullets.sumOf { bullet ->
            val lower = bullet.lowercase()
            var points = 0

            val hasStrongActionVerb =
                strongActionVerbs.any { verb ->
                    Regex(
                        "\\b${Regex.escape(verb)}\\b",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(bullet)
                }

            if (hasStrongActionVerb) {
                points += 30
            }

            if (
                containsMeaningfulTechnicalOrProjectContent(
                    bullet
                )
            ) {
                points += 25
            }

            if (
                hasMeasurableImpactSignal(bullet) ||
                resultWords.any { lower.contains(it) }
            ) {
                points += 30
            }

            if (bullet.length in 35..220) {
                points += 15
            }

            if (
                lower.startsWith("worked on") ||
                lower.startsWith("responsible for") ||
                lower.startsWith("helped")
            ) {
                points -= 20
            }

            points.coerceIn(0, 100)
        }

        return (total.toDouble() / bullets.size)
            .roundToInt()
            .coerceIn(20, 95)
    }

    private fun calculateSectionClarityScore(resumeText: String): Int {
        val detectedSections = detectSectionSignals(resumeText)
        val wordCount = resumeText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
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

    private fun weightedOverallScore(
        jdRoleMatchScore: Int,
        evidenceConfidenceScore: Int,
        measurableImpactScore: Int,
        bulletQualityScore: Int,
        seniorityAlignmentScore: Int,
        atsReadabilityScore: Int,
        toolSkillProofScore: Int,
        candidatePositioningScore: Int,
        actionVerbScore: Int,
        resumeText: String,
        jobSpecification: String,
        experienceLevel: String
    ): Int {
        val score = (
            jdRoleMatchScore * 0.18 +
                evidenceConfidenceScore * 0.16 +
                measurableImpactScore * 0.14 +
                bulletQualityScore * 0.12 +
                seniorityAlignmentScore * 0.10 +
                atsReadabilityScore * 0.10 +
                toolSkillProofScore * 0.08 +
                candidatePositioningScore * 0.08 +
                actionVerbScore * 0.04
            ).roundToInt()

        var cappedScore = score

        if (resumeText.length < 300) {
            cappedScore =
                minOf(cappedScore, 55)
        }

        if (
            jobSpecification.isNotBlank() &&
            jdRoleMatchScore <= 20
        ) {
            cappedScore =
                minOf(cappedScore, 60)
        }

        if (measurableImpactScore < 40) {
            cappedScore =
                minOf(cappedScore, 78)
        }

        val lower = resumeText.lowercase()

        if (
            !lower.contains("experience") &&
            !lower.contains("project") &&
            !lower.contains("work history")
        ) {
            cappedScore =
                minOf(cappedScore, 65)
        }

        val isSenior =
            experienceLevel.contains(
                "Senior",
                ignoreCase = true
            ) ||
                experienceLevel.contains(
                    "Lead",
                    ignoreCase = true
                ) ||
                experienceLevel.contains("7+") ||
                experienceLevel.contains("4-7")

        if (
            isSenior &&
            measurableImpactScore < 40
        ) {
            cappedScore =
                minOf(cappedScore, 70)
        }

        if (atsReadabilityScore < 40) {
            cappedScore =
                minOf(cappedScore, 75)
        }

        return cappedScore.coerceIn(0, 100)
    }

    private fun extractMetricExamples(resumeText: String): List<String> {
        return resumeLines(resumeText)
            .map { stripContactNoise(it).replace(Regex("\\s+"), " ").trim() }
            .filter { it.length >= 10 }
            .filter { hasMeasurableImpactSignal(it) }
            .map { shortenLabel(it, 150) }
            .distinct()
            .take(5)
    }

    private fun stripContactNoise(text: String): String {
        return text
            .replace(Regex("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b"), " ")
            .replace(Regex("https?://\\S+|www\\.\\S+", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\b(?:linkedin|github)\\.com/\\S+", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("(?<!\\w)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\w)"), " ")
            .replace(Regex("\\b\\d{7,}\\b"), " ")
            .replace(Regex("\\b(?:19|20)\\d{2}\\b"), " ")
    }

    private fun hasMeasurableImpactSignal(line: String): Boolean {
        val cleaned = stripContactNoise(line)
        val lower = cleaned.lowercase()
        val hasNumber = Regex("\\b\\d+(?:[.,]\\d+)?\\+?\\b").containsMatchIn(lower)
        val hasPercentage = Regex("\\b\\d+(?:[.,]\\d+)?\\s*%").containsMatchIn(lower)
        val hasCurrency = Regex("[$₹€£]\\s*\\d|\\b\\d+(?:[.,]\\d+)?\\s*(?:usd|inr|eur|gbp|dollars?|rupees?)", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)
        val impactUnits = listOf(
            "users", "user", "customers", "customer", "clients", "client", "hours", "hour", "days", "day", "weeks", "week", "months", "month",
            "leads", "lead", "sales", "projects", "project", "reports", "report", "dashboards", "dashboard", "revenue", "cost", "budget"
        )
        val impactWords = listOf("improved", "reduced", "increased", "saved", "optimized", "accelerated", "streamlined", "delivered", "resolved")
        val unitPattern = impactUnits.joinToString("|")
        val hasImpactUnitNearNumber = Regex(
            "\\b\\d+(?:[.,]\\d+)?\\+?\\s*(?:$unitPattern)\\b|" +
                "\\b(?:$unitPattern)\\b\\W{0,24}" +
                "\\b\\d+(?:[.,]\\d+)?\\+?\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(cleaned)
        val hasNumberAndImpactWord = hasNumber && impactWords.any { lower.contains(it) }
        return hasPercentage || hasCurrency || hasImpactUnitNearNumber || hasNumberAndImpactWord
    }

    private fun buildOptimizedBulletSuggestion(bullet: String, roleAndJdKeywords: List<String>): String {
        val shortenedBullet = shortenLabel(bullet, 120)
        val lower = bullet.lowercase()
        val detectedKeyword = roleAndJdKeywords
            .filter { it.length >= 3 }
            .firstOrNull { lower.contains(it.lowercase()) }
        val hasActionVerb = strongActionVerbs.any { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(bullet) }
        val hasMetric = hasMeasurableImpactSignal(bullet)
        val looksGeneric = lower.startsWith("worked on") || lower.startsWith("responsible for") || lower.startsWith("helped") ||
            lower.split(Regex("\\s+")).size < 8
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
        val regex = Regex("\\b(${strongActionVerbs.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
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
        val fallbackSignals = listOf(
            "target role: $targetRole",
            "experience level: $experienceLevel",
            "strongest resume project or responsibility",
            "most relevant resume skill or tool"
        )
        val questionSignals = (uniqueSignals + fallbackSignals).distinct().take(4)

        return questionSignals.mapIndexed { index, signal ->
            val focus = when {
                signal.startsWith("Detected skill/keyword:") -> "what technical or design tradeoff did you make while using this skill in a real $targetRole context"
                signal.startsWith("JD match:") -> "how does this JD requirement appear in your resume evidence, and what implementation details prove your contribution"
                Regex("[%0-9]").containsMatchIn(signal) -> "how was this measurable result achieved, validated, and limited by your exact contribution"
                else -> "what implementation decisions, failure cases, and ownership details sit behind this resume line"
            }
            "Q${index + 1}. Your resume mentions \"$signal\". $focus?"
        }.joinToString("\n")
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

    private fun isLikelyProfileOrTitleLine(
        line: String
    ): Boolean {
        val cleaned =
            line.trim()

        val lower =
            cleaned.lowercase()

        val hasActionVerb =
            strongActionVerbs.any { verb ->
                Regex(
                    "\\b${Regex.escape(verb)}\\b",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(cleaned)
            }

        val hasOutcome =
            hasMeasurableImpactSignal(cleaned) ||
                resultWords.any { lower.contains(it) }

        if (hasActionVerb || hasOutcome) {
            return false
        }

        val profilePatterns = listOf(
            "years of experience",
            "year of experience",
            "software development engineer",
            "software engineer with",
            "developer with",
            "engineer with",
            "professional with",
            "experienced professional",
            "results-driven",
            "highly motivated"
        )

        return profilePatterns.any {
            lower.contains(it)
        }
    }

    private fun selectEvidenceHighlights(
        resumeText: String,
        targetRole: String,
        jobSpecification: String
    ): List<String> {
        val relevantKeywords =
            (
                getKeywordsForRole(targetRole) +
                    extractSimpleKeywordsFromJobSpec(
                        jobSpecification.lowercase()
                    )
                )
                .distinctBy {
                    it.lowercase()
                }

        return extractCandidateBullets(resumeText)
            .filterNot {
                isLikelyProfileOrTitleLine(it)
            }
            .map { line ->
                val lower =
                    line.lowercase()

                var score = 0

                if (hasMeasurableImpactSignal(line)) {
                    score += 5
                }

                if (
                    resultWords.any {
                        lower.contains(it)
                    }
                ) {
                    score += 3
                }

                if (
                    strongActionVerbs.any { verb ->
                        Regex(
                            "\\b${Regex.escape(verb)}\\b",
                            RegexOption.IGNORE_CASE
                        ).containsMatchIn(line)
                    }
                ) {
                    score += 3
                }

                if (
                    relevantKeywords.any { keyword ->
                        keyword.length >= 3 &&
                            line.contains(
                                keyword,
                                ignoreCase = true
                            )
                    }
                ) {
                    score += 2
                }

                if (
                    containsMeaningfulTechnicalOrProjectContent(
                        line
                    )
                ) {
                    score += 1
                }

                line to score
            }
            .filter {
                it.second >= 3
            }
            .sortedWith(
                compareByDescending<Pair<String, Int>> {
                    it.second
                }.thenByDescending {
                    it.first.length
                }
            )
            .map {
                shortenLabel(
                    it.first,
                    150
                )
            }
            .distinct()
            .take(6)
    }

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

    private fun resumeLines(
        text: String
    ): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        val lines = text
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val reconstructed =
            mutableListOf<String>()

        var currentBuffer =
            StringBuilder()

        val continuationStarter = Regex(
            """^(and|or|with|for|to|in|on|of|using|through|while|which|that)\b""",
            RegexOption.IGNORE_CASE
        )

        fun flushBuffer() {
            if (currentBuffer.isEmpty()) {
                return
            }

            reconstructed.add(
                currentBuffer
                    .toString()
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            )

            currentBuffer = StringBuilder()
        }

        for (line in lines) {
            val isBulletStart =
                line.startsWith("•") ||
                    line.startsWith("*") ||
                    line.startsWith("▪") ||
                    line.startsWith("◦")

            val isHeading =
                isResumeSectionHeading(line)

            val startsLowercase =
                line.firstOrNull()
                    ?.isLowerCase() == true

            val startsWithContinuationWord =
                continuationStarter
                    .containsMatchIn(line)

            val previousIsHeading =
                currentBuffer.isNotEmpty() &&
                    isResumeSectionHeading(
                        currentBuffer.toString()
                    )

            val isContinuation =
                currentBuffer.isNotEmpty() &&
                    !previousIsHeading &&
                    !isBulletStart &&
                    !isHeading &&
                    (
                        startsLowercase ||
                            startsWithContinuationWord
                        )

            if (isContinuation) {
                currentBuffer
                    .append(" ")
                    .append(line)
            } else {
                flushBuffer()
                currentBuffer.append(line)
            }
        }

        flushBuffer()

        return reconstructed.filterNot {
            isBareStopwordFragment(it)
        }
    }

    private fun isResumeSectionHeading(
        line: String
    ): Boolean {
        val cleaned =
            line.trim().trimEnd(':')

        val headings = setOf(
            "summary",
            "profile",
            "objective",
            "experience",
            "work experience",
            "professional experience",
            "employment",
            "education",
            "skills",
            "technical skills",
            "projects",
            "certifications",
            "awards",
            "publications",
            "research",
            "languages",
            "interests"
        )

        return cleaned.lowercase() in headings ||
            looksLikeHeading(cleaned)
    }

    private fun isBareStopwordFragment(line: String): Boolean {
        val normalized = line.lowercase()
            .replace(Regex("[^a-z]"), " ")
            .replace(Regex("\\s+"), " ")
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
        return meaningfulTerms.any { lower.contains(it) } || Regex("\\d|%|\\$").containsMatchIn(line)
    }

    private fun looksLikeSplitSentenceFragment(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return false
        val lower = trimmed.lowercase()
        val fragmentStarters = listOf(
            "and ",
            "or ",
            "with ",
            "for ",
            "to ",
            "in ",
            "on ",
            "of ",
            "using ",
            "through ",
            "while ",
            "which ",
            "that "
        )
        return fragmentStarters.any { lower.startsWith(it) } || trimmed.first().isLowerCase()
    }

    private fun looksLikeHeading(line: String): Boolean = line.length <= 24 && line.uppercase() == line && line.any { it.isLetter() }

    private fun getKeywordsForRole(targetRole: String): List<String> {
        val roleLower = targetRole.lowercase()
        return when {
            roleLower == "software engineer" -> listOf(
                "Java",
                "Kotlin",
                "Python",
                "JavaScript",
                "TypeScript",
                "C++",
                "Data Structures",
                "Algorithms",
                "Object-Oriented Programming",
                "System Design",
                "REST API",
                "SQL",
                "Git",
                "Unit Testing",
                "Integration Testing",
                "Debugging",
                "CI/CD",
                "Docker",
                "Cloud Computing",
                "Agile",
                "Performance Optimization",
                "Security",
                "Documentation"
            )
            roleLower.contains("android") -> listOf(
                "Kotlin", "Android SDK", "Jetpack Compose", "Coroutines", "Flow", "MVVM", "Room", "Retrofit", "Dagger Hilt", "Clean Architecture",
                "Unit Testing", "Espresso", "Google Play Store", "Firebase", "CI/CD", "Git", "Material Design", "Performance Tuning", "Memory Management",
                "Custom Views", "WorkManager", "ViewModel", "LiveData", "Navigation Component", "REST API", "JSON", "SQLite"
            )
            roleLower.contains("ios") -> listOf(
                "Swift", "SwiftUI", "UIKit", "Combine", "Core Data", "XCTest", "App Store Connect", "CocoaPods", "Swift Package Manager", "AVFoundation",
                "Core Animation", "GCD", "Operations", "Auto Layout", "MVVM", "VIPER", "Clean Architecture", "Unit Testing", "UI Testing", "Firebase",
                "Git", "REST API", "JSON", "Memory Management", "Push Notifications", "Bluetooth", "Localization"
            )
            roleLower.contains("backend") -> listOf(
                "Java", "Spring Boot", "Microservices", "REST API", "gRPC", "SQL", "NoSQL", "PostgreSQL", "MongoDB", "Redis", "Kafka", "RabbitMQ",
                "Docker", "Kubernetes", "AWS", "Azure", "GCP", "JUnit", "Unit Testing", "Integration Testing", "System Design", "Scalability",
                "Performance Tuning", "Security", "OAuth2", "Hibernate", "JPA", "Node.js", "Express", "Python", "Django", "Go"
            )
            roleLower.contains("frontend") -> listOf(
                "JavaScript", "TypeScript", "React", "Angular", "Vue.js", "HTML5", "CSS3", "SASS", "Tailwind CSS", "Redux", "Context API",
                "Next.js", "Webpack", "Vite", "Responsive Design", "Web Accessibility", "Unit Testing", "Jest", "Cypress", "Git", "REST API",
                "GraphQL", "State Management", "Performance Optimization", "Browser Compatibility", "UI/UX Principles"
            )
            roleLower.contains("full-stack") -> listOf(
                "JavaScript", "TypeScript", "React", "Node.js", "Express", "SQL", "PostgreSQL", "MongoDB", "REST API", "GraphQL", "HTML5", "CSS3",
                "AWS", "Docker", "Git", "CI/CD", "Unit Testing", "System Design", "Microservices", "Responsive Design", "Redux", "Next.js",
                "Spring Boot", "Java", "Python", "Database Design", "Security", "Deployment"
            )
            roleLower.contains("ml") || roleLower.contains("machine learning") -> listOf(
                "Python", "PyTorch", "TensorFlow", "Scikit-Learn", "Keras", "Pandas", "NumPy", "Deep Learning", "Neural Networks", "NLP",
                "Computer Vision", "Reinforcement Learning", "Feature Engineering", "Model Validation", "MLOps", "SQL", "Spark", "Hadoop",
                "AWS SageMaker", "Data Visualization", "Mathematics", "Statistics", "Linear Algebra", "Calculus", "Big Data", "Data Mining"
            )
            roleLower.contains("ai engineer") -> listOf(
                "Python", "LLMs", "Generative AI", "LangChain", "OpenAI API", "Hugging Face", "Vector Databases", "Prompt Engineering", "NLP",
                "Deep Learning", "PyTorch", "TensorFlow", "MLOps", "RAG", "Fine-tuning", "Model Deployment", "Cloud Computing", "SQL", "Git",
                "Research", "Algorithm Design", "Ethics in AI", "API Integration", "Scalability"
            )
            roleLower.contains("data engineer") -> listOf(
                "Python", "SQL", "Spark", "Hadoop", "ETL", "ELT", "Data Warehousing", "Snowflake", "BigQuery", "Redshift", "Airflow", "Kafka",
                "NoSQL", "Database Design", "Cloud Computing", "AWS", "Azure", "GCP", "Data Governance", "Data Quality", "Distributed Systems",
                "Docker", "Kubernetes", "Terraform", "CI/CD"
            )
            roleLower.contains("devops") -> listOf(
                "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Terraform", "Ansible", "CI/CD", "Jenkins", "GitHub Actions", "GitLab CI",
                "Linux", "Shell Scripting", "Python", "Monitoring", "Prometheus", "Grafana", "Logging", "ELK Stack", "Security", "Network",
                "Cloud Computing", "Infrastructure as Code", "Site Reliability", "Automation"
            )
            roleLower.contains("site reliability") || roleLower.contains("sre") -> listOf(
                "Python", "Go", "Linux", "Shell Scripting", "Kubernetes", "Docker", "AWS", "Azure", "GCP", "Terraform", "Monitoring",
                "Prometheus", "Grafana", "Incident Management", "Error Budgets", "SLAs", "SLOs", "SLIs", "Automation", "System Design",
                "Networking", "Scalability", "Reliability Engineering", "Distributed Systems"
            )
            roleLower.contains("qa automation") -> listOf(
                "Java", "Python", "Selenium", "Appium", "JUnit", "TestNG", "Pytest", "Cypress", "Playwright", "API Testing", "Postman",
                "RestAssured", "CI/CD", "Jenkins", "Git", "Test Automation", "Agile", "Defect Tracking", "Jira", "Performance Testing",
                "Mobile Testing", "Regression Testing", "SDET", "Software Quality Assurance"
            )
            roleLower.contains("embedded") -> listOf(
                "C", "C++", "RTOS", "Microcontrollers", "ARM", "Firmware", "Device Drivers", "Embedded Linux", "I2C", "SPI", "UART",
                "Assembly", "Oscilloscopes", "Logic Analyzers", "PCB Design", "Real-time Systems", "Memory Management", "Optimization",
                "Hardware Abstraction", "Low-level Programming", "Debugging", "Sensors", "Protocol Design"
            )
            roleLower.contains("security") -> listOf(
                "Cybersecurity", "Network Security", "Penetration Testing", "Vulnerability Assessment", "SIEM", "IDS/IPS", "Cryptography",
                "OWASP", "Identity Management", "Cloud Security", "Firewalls", "SOC", "Incident Response", "Compliance", "SOC2", "ISO 27001",
                "Python", "PowerShell", "Linux Security", "Application Security", "Risk Management"
            )
            roleLower.contains("game developer") -> listOf(
                "C++", "C#", "Unity", "Unreal Engine", "Game Physics", "3D Modeling", "Shaders", "Computer Graphics", "Math for Games",
                "Animation", "Game Mechanics", "Optimization", "Networking for Games", "AI for Games", "Mobile Game Development",
                "DirectX", "OpenGL", "Vulkan", "VR/AR", "Gameplay Programming"
            )
            roleLower.contains("data analyst") -> listOf(
                "SQL", "Python", "R", "Excel", "Tableau", "Power BI", "Data Visualization", "Statistics", "Data Cleaning", "Reporting",
                "A/B Testing", "KPIs", "Dashboarding", "Business Intelligence", "Google Analytics", "Data Mining", "Predictive Analytics",
                "Market Research", "Communication", "Presentation Skills", "Data Governance"
            )
            roleLower.contains("data scientist") -> listOf(
                "Python", "R", "SQL", "Machine Learning", "Deep Learning", "Statistics", "Mathematics", "Feature Engineering", "Pandas",
                "NumPy", "Scikit-Learn", "PyTorch", "TensorFlow", "Data Visualization", "Big Data", "Spark", "Cloud Computing",
                "Model Deployment", "Experimental Design", "A/B Testing", "NLP", "Problem Solving"
            )
            roleLower.contains("business analyst") -> listOf(
                "Requirements Gathering", "Stakeholder Management", "Business Process Modeling", "User Stories", "UAT", "Jira", "Agile",
                "SQL", "Excel", "Data Analysis", "Gap Analysis", "BRDs", "FRDs", "Process Improvement", "Product Backlog", "Facilitation",
                "Documentation", "Business Strategy", "SWOT Analysis"
            )
            roleLower.contains("product manager") -> listOf(
                "Product Strategy", "Product Roadmap", "User Research", "Market Analysis", "Prioritization", "Stakeholder Management",
                "Agile", "Scrum", "Jira", "KPIs", "Analytics", "A/B Testing", "User Stories", "PRDs", "Go-to-Market Strategy", "Product Lifecycle",
                "Customer Feedback", "Design Thinking", "Financial Modeling"
            )
            roleLower.contains("project manager") -> listOf(
                "Project Planning", "Budgeting", "Risk Management", "Stakeholder Management", "Agile", "Scrum", "Waterfall", "Jira",
                "Resource Allocation", "Timeline Management", "Scope Management", "Change Management", "Status Reporting", "Team Leadership",
                "Vendor Management", "PMP", "Prince2", "Delivery Management", "Problem Solving"
            )
            roleLower.contains("ux") || roleLower.contains("ui") -> listOf(
                "Figma", "Adobe XD", "User Research", "Wireframing", "Prototyping", "Information Architecture", "Visual Design",
                "Interaction Design", "Design Systems", "Usability Testing", "Accessibility", "User Flows", "Design Thinking",
                "Heuristic Evaluation", "Responsive Design", "Typography", "Color Theory", "Portfolio"
            )
            roleLower.contains("finance") -> listOf(
                "Financial Modeling", "Financial Analysis", "Forecasting", "Budgeting", "P&L Management", "Excel", "VBA", "Power BI",
                "Valuation", "Accounting", "Risk Management", "Corporate Finance", "Investment Analysis", "ERP Systems", "SAP",
                "Financial Reporting", "Variance Analysis", "Strategic Planning"
            )
            roleLower.contains("operations") -> listOf(
                "Process Improvement", "Supply Chain", "Inventory Management", "Logistics", "Operations Management", "Lean Six Sigma",
                "Project Management", "Data Analysis", "KPIs", "Cost Reduction", "Vendor Management", "SOPs", "Quality Control",
                "Workflow Optimization", "Excel", "Strategic Planning", "Change Management"
            )
            roleLower.contains("sales") ||
                roleLower.contains(
                    "business development"
                ) -> listOf(
                "Sales Pipeline", "Lead Generation", "CRM", "Salesforce", "Account Management", "B2B Sales", "Negotiation", "Cold Calling",
                "Closing Deals", "Market Research", "Business Development", "Customer Relationship Management", "Revenue Growth",
                "Presentation Skills", "Communication", "Sales Strategy"
            )
            roleLower.contains("marketing") -> listOf(
                "SEO", "SEM", "Google Analytics", "Google Ads", "Content Marketing", "Social Media Marketing", "Email Marketing",
                "Brand Management", "Market Research", "CRM", "Campaign Management", "A/B Testing", "Conversion Optimization",
                "Copywriting", "Digital Strategy", "KPIs", "Data Analysis"
            )
            roleLower.contains("hr") || roleLower.contains("recruiter") -> listOf(
                "Recruitment", "Talent Acquisition", "Sourcing", "Applicant Tracking Systems", "ATS", "Employee Engagement",
                "Onboarding", "Performance Management", "HR Policies", "Labor Relations", "Compensation & Benefits", "HRIS",
                "Interviewing", "Conflict Resolution", "Organizational Development", "Diversity & Inclusion"
            )
            roleLower.contains("research") -> listOf(
                "Research Methodology", "Data Collection", "Statistical Analysis", "Scientific Writing", "Literature Review",
                "Project Management", "Grant Writing", "Data Visualization", "Python", "R", "SQL", "Experimental Design",
                "Critical Thinking", "Academic Publishing", "Lab Management"
            )
            else -> listOf(
                "Communication", "Problem Solving", "Teamwork", "Project Management", "Time Management", "Analytical Thinking",
                "Adaptability", "Leadership", "Organization", "Customer Service", "Technical Literacy", "Reporting", "Documentation",
                "Strategic Planning", "Presentation Skills", "Critical Thinking"
            )
        }
    }

    private fun extractSimpleKeywordsFromJobSpec(jobSpec: String): List<String> {
        if (jobSpec.isBlank()) return emptyList()
        val jobSpecLower = jobSpec.lowercase()
        
        val knownKeywords = listOf(
            "sql", "python", "java", "kotlin", "swift", "javascript", "typescript", "react", "node", "aws", "azure", "gcp",
            "docker", "kubernetes", "terraform", "jira", "agile", "scrum", "excel", "tableau", "power bi", "figma",
            "seo", "sem", "crm", "sap", "salesforce", "pmp", "r", "spark", "hadoop", "airflow", "kafka", "microservices"
        ).filter { jobSpecLower.contains(it) }

        val capitalizedTokens = Regex("\\b[A-Z][a-zA-Z0-9+#.]{1,}\\b").findAll(jobSpec)
            .map { it.value }
            .filter { it.lowercase() !in setOf("the", "and", "this", "your", "join", "team", "with", "from") }
            .distinct()
            .toList()

        val triggers = listOf("required", "must have", "experience with", "proficient in", "responsible for", "familiarity with", "expertise in", "skills in")
        val extractedPhrases = mutableListOf<String>()
        for (trigger in triggers) {
            val pattern = Regex("$trigger\\s+([^,.;\\n]{2,40})", RegexOption.IGNORE_CASE)
            pattern.findAll(jobSpec).forEach { match ->
                val phrase = match.groupValues[1].trim()
                if (phrase.split(" ").size in 2..4) {
                    extractedPhrases.add(phrase)
                }
            }
        }

        val allSignals = (knownKeywords + capitalizedTokens + extractedPhrases).distinctBy { it.lowercase() }
        return allSignals.take(30)
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
        val existing = try {
            JSONArray(prefs.getString(SavedReportsActivity.KEY_REPORTS, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        val item = JSONObject().apply {
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
            put("target_role", targetRole)
            put("experience_level", experienceLevel)
            put("overall_score", report.overallScore)
            put("rating_label", scoreRatingLabel(report.overallScore))
            put("jd_status", report.jdStatus)
            put("basic_feedback", report.basicFeedback)
            put("full_report", report.fullReport)
            put("keyword_match_score", report.keywordMatchScore)
            put("measurable_impact_score", report.measurableImpactScore)
            put("action_verb_score", report.actionVerbScore)
            put("section_clarity_score", report.sectionClarityScore)
            put("role_fit_score", report.roleRelevanceScore)
            put("local_only_note", "Saved reports are stored locally on this device only.")
        }
        existing.put(item)
        prefs.edit().putString(SavedReportsActivity.KEY_REPORTS, existing.toString()).apply()
    }

    private fun bulletList(items: List<String>, emptyText: String): String = if (items.isEmpty()) "• $emptyText" else items.joinToString("\n") { "• $it" }
    private fun formatExamples(items: List<String>, emptyText: String): String = if (items.isEmpty()) "• $emptyText" else items.joinToString("\n") { "• $it" }
    
    private fun shortenLabel(value: String, maxLength: Int): String {
        if (value.length <= maxLength) return value
        val truncated = value.take(maxLength - 1)
        val lastSpace = truncated.lastIndexOf(' ')
        return if (lastSpace != -1 && lastSpace > maxLength / 2) {
            truncated.take(lastSpace).trimEnd() + "…"
        } else {
            truncated.trimEnd() + "…"
        }
    }

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

    private val resultWords = listOf("improved", "reduced", "increased", "saved", "optimized", "grew", "accelerated", "streamlined", "delivered", "resolved", "launched", "converted", "cut", "raised")
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
    val strengthsContent: String,
    val gapsContent: String,
    val keywordsContent: String,
    val bespokeContent: String,
    val jdStatus: String
)
