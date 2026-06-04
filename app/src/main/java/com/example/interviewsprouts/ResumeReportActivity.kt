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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResumeReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_report)

        val resumeText = intent.getStringExtra("resume_text") ?: ""
        val targetRole = intent.getStringExtra("target_role") ?: "General Job Applicant"
        val experienceLevel = intent.getStringExtra("experience_level") ?: "Not specified"
        val jobSpecification = intent.getStringExtra("job_specification") ?: ""

        val textTargetRole = findViewById<TextView>(R.id.textTargetRole)
        val textOverallScore = findViewById<TextView>(R.id.textOverallScore)
        val textBasicFeedback = findViewById<TextView>(R.id.textBasicFeedback)
        val textMissingKeywordsHook = findViewById<TextView>(R.id.textMissingKeywordsHook)
        val textFullReport = findViewById<TextView>(R.id.textFullReport)
        val textLockedReportMessage = findViewById<TextView>(R.id.textLockedReportMessage)
        val textUnlockedBulletSuggestions = findViewById<TextView>(R.id.textUnlockedBulletSuggestions)
        val textUnlockedInterviewQuestions = findViewById<TextView>(R.id.textUnlockedInterviewQuestions)
        val textAdvancedLockedMessage = findViewById<TextView>(R.id.textAdvancedLockedMessage)
        val textAdvancedLlmReview = findViewById<TextView>(R.id.textAdvancedLlmReview)

        val btnUnlockFullReport = findViewById<Button>(R.id.btnUnlockFullReport)
        val btnUnlockAdvancedLlmReview = findViewById<Button>(R.id.btnUnlockAdvancedLlmReview)
        val btnSaveReport = findViewById<Button>(R.id.btnSaveReport)

        val report = createResumeReport(
            resumeText = resumeText,
            targetRole = targetRole,
            experienceLevel = experienceLevel,
            jobSpecification = jobSpecification
        )

        textTargetRole.text = "Target Role: $targetRole | Level: $experienceLevel"
        textOverallScore.text = "${report.overallScore} / 100"
        textBasicFeedback.text = report.basicFeedback
        textMissingKeywordsHook.text = report.missingKeywordsHook
        textFullReport.text = report.fullReport

        btnUnlockFullReport.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Simulated Ad")
                .setMessage("In the real app, a rewarded ad will play here. For now, tap Continue to unlock.")
                .setPositiveButton("Continue") { _, _ ->
                    textUnlockedBulletSuggestions.text = generateBulletRewriteSuggestions(resumeText, targetRole)
                    textUnlockedInterviewQuestions.text = generateInterviewQuestionsFromResume(
                        resumeText = resumeText,
                        targetRole = targetRole,
                        experienceLevel = experienceLevel,
                        jobSpecification = jobSpecification
                    )
                    textFullReport.visibility = View.VISIBLE
                    btnUnlockFullReport.visibility = View.GONE
                    textLockedReportMessage.visibility = View.GONE
                    textUnlockedBulletSuggestions.visibility = View.VISIBLE
                    textUnlockedInterviewQuestions.visibility = View.VISIBLE
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
                    requestAdvancedAiReview(
                        resumeText = resumeText,
                        targetRole = targetRole,
                        experienceLevel = experienceLevel,
                        jobSpecification = jobSpecification,
                        textAdvancedLlmReview = textAdvancedLlmReview
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnSaveReport.setOnClickListener {
            saveReportLocally(
                targetRole = targetRole,
                experienceLevel = experienceLevel,
                report = report
            )
            Toast.makeText(this, "Report saved locally on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAdvancedAiReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String,
        textAdvancedLlmReview: TextView
    ) {
        if (AiClient.isPlaceholderBackendUrl()) {
            textAdvancedLlmReview.text = buildOfflineAdvancedFallback(
                resumeText = resumeText,
                targetRole = targetRole,
                experienceLevel = experienceLevel,
                jobSpecification = jobSpecification
            )
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
                "interviewQuestions",
                "bulletRewriteSuggestions"
            )
        )

        AiClient.service.analyzeResume(request).enqueue(object : Callback<ResumeAiResponse> {
            override fun onResponse(call: Call<ResumeAiResponse>, response: Response<ResumeAiResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.error.isNullOrBlank()) {
                    textAdvancedLlmReview.text = formatBackendAdvancedReview(body)
                } else {
                    textAdvancedLlmReview.text = buildOfflineAdvancedFallback(
                        resumeText = resumeText,
                        targetRole = targetRole,
                        experienceLevel = experienceLevel,
                        jobSpecification = jobSpecification
                    )
                }
            }

            override fun onFailure(call: Call<ResumeAiResponse>, t: Throwable) {
                textAdvancedLlmReview.text = buildOfflineAdvancedFallback(
                    resumeText = resumeText,
                    targetRole = targetRole,
                    experienceLevel = experienceLevel,
                    jobSpecification = jobSpecification
                )
            }
        })
    }

    private fun formatBackendAdvancedReview(response: ResumeAiResponse): String {
        val sections = mutableListOf<String>()
        response.advancedReview.takeIf { it.isNotBlank() }?.let { sections.add("Advanced AI Review:\n$it") }
        response.tailoredResumeSuggestions.takeIf { it.isNotBlank() }?.let { sections.add("Tailored Resume Suggestions:\n$it") }
        response.interviewQuestions.takeIf { it.isNotBlank() }?.let { sections.add("Resume-Specific Interview Questions:\n$it") }
        response.bulletRewriteSuggestions.takeIf { it.isNotBlank() }?.let { sections.add("Bullet Rewrite Suggestions:\n$it") }

        return sections.joinToString("\n\n").ifBlank {
            "AI backend returned no usable content. Showing offline fallback is recommended."
        }
    }

    private fun buildOfflineAdvancedFallback(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        return "AI backend unavailable. Showing offline fallback.\n\n" + generateAdvancedLlmReview(
            resumeText = resumeText,
            targetRole = targetRole,
            experienceLevel = experienceLevel,
            jobSpecification = jobSpecification
        )
    }

    private fun generateAdvancedLlmReview(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        val tailoredSuggestions = generateTailoredJobDescriptionSuggestions(
            resumeText = resumeText,
            jobSpecification = jobSpecification,
            targetRole = targetRole
        )
        val metricSignalCount = countMetricSignals(resumeText)
        val sectionSignals = detectSectionSignals(resumeText)
        val bulletExamples = extractCandidateBullets(resumeText).take(3)
        val bulletFocus = if (bulletExamples.isEmpty()) {
            "• Add 2–4 role-relevant bullets that start with strong action verbs, mention real tools or responsibilities, and include true placeholders for metrics such as [X%], [number], or [hours]."
        } else {
            bulletExamples.joinToString("\n") { bullet ->
                val cleanedTask = cleanBulletTask(bullet)
                "• Rewrite: $cleanedTask — clarify the action, tool/context, audience, and true measurable result for $targetRole."
            }
        }
        val advancedQuestions = generateResumeSpecificAdvancedQuestions(
            resumeText = resumeText,
            targetRole = targetRole,
            jobSpecification = jobSpecification
        )

        return """
Advanced AI Review:
• Offline fallback assessment for a backend-powered DeepSeek V4 Flash response.
• Target role: $targetRole.
• Experience level: $experienceLevel.
• Resume structure signals found: ${sectionSignals.joinToString(", ").ifBlank { "limited section signals" }}.
• Measurable-impact signals found: $metricSignalCount. Strengthen bullets by adding only true metrics, scope, tools, and outcomes.

Tailored Job Description Suggestions:
$tailoredSuggestions

Advanced Bullet Rewrite Strategy:
$bulletFocus
• Use this pattern: Action Verb + Task + Tool/Context + Audience + True Result.
• Keep placeholders like [X%], [number], and [hours] until you can replace them with real evidence.

$advancedQuestions

Truth Warning:
Only add skills, metrics, and responsibilities that are true.
        """.trimIndent()
    }

    private fun generateTailoredJobDescriptionSuggestions(
        resumeText: String,
        jobSpecification: String,
        targetRole: String
    ): String {
        if (jobSpecification.isBlank()) {
            return "Paste a job description to get tailored resume suggestions."
        }

        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())
        val expandedKeywords = (jdKeywords + getRoleKeywordBank(targetRole.lowercase()))
            .distinctBy { keyword -> keyword.lowercase() }
            .filter { keyword -> jobSpecification.contains(keyword, ignoreCase = true) }
        val resumeLower = resumeText.lowercase()
        val matchedKeywords = expandedKeywords.filter { keyword -> resumeLower.contains(keyword.lowercase()) }
        val missingKeywords = expandedKeywords.filterNot { keyword -> resumeLower.contains(keyword.lowercase()) }

        val matchedText = matchedKeywords.take(12).joinToString("\n") { keyword -> "• $keyword" }
            .ifBlank { "• No clear matched JD keywords detected yet." }
        val missingText = missingKeywords.take(12).joinToString("\n") { keyword -> "• $keyword" }
            .ifBlank { "• No obvious missing JD keywords from the known keyword list." }
        val whereToAddText = missingKeywords.take(8).joinToString("\n") { keyword ->
            "• $keyword — add under ${suggestWhereToAddKeyword(keyword)} only if true."
        }.ifBlank { "• Keep evidence-focused bullets aligned to the job description without adding fake skills." }

        return """
Matched JD Keywords:
$matchedText

Missing JD Keywords:
$missingText

Where to Add Them:
$whereToAddText
        """.trimIndent()
    }

    private fun saveReportLocally(
        targetRole: String,
        experienceLevel: String,
        report: ResumeReportResult
    ) {
        val prefs = getSharedPreferences(SavedReportsActivity.PREFS_NAME, MODE_PRIVATE)
        val reports = JSONArray(prefs.getString(SavedReportsActivity.KEY_REPORTS, "[]") ?: "[]")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val savedReport = JSONObject()
            .put("target_role", targetRole)
            .put("experience_level", experienceLevel)
            .put("overall_score", report.overallScore)
            .put("basic_feedback", report.basicFeedback)
            .put("full_report", report.fullReport)
            .put("timestamp", timestamp)

        reports.put(savedReport)
        prefs.edit().putString(SavedReportsActivity.KEY_REPORTS, reports.toString()).apply()
    }


    private fun generateBulletRewriteSuggestions(
        resumeText: String,
        targetRole: String
    ): String {
        val candidates = extractCandidateBullets(resumeText)
        if (candidates.isEmpty()) {
            return """
                Bullet Rewrite Suggestions

                No clear resume bullets were detected. Add bullets with action verbs, tools, and measurable outcomes.

                Truth Warning:
                Only use metrics and responsibilities that are true.
            """.trimIndent()
        }

        val weakPhraseRegex = Regex(
            """\b(worked on|helped with|responsible for|handled|supported|assisted with|involved in|contributed to)\b""",
            RegexOption.IGNORE_CASE
        )
        val weakBullets = candidates.filter { weakPhraseRegex.containsMatchIn(it) }
        val selectedBullets = (weakBullets + candidates.filterNot { it in weakBullets })
            .distinct()
            .take(3)
        val profile = rewriteProfileForRole(targetRole)

        return buildString {
            appendLine("Bullet Rewrite Suggestions")
            appendLine()
            selectedBullets.forEachIndexed { index, bullet ->
                val cleanedTask = removeDuplicatedLeadingVerb(cleanBulletTask(bullet), profile.improvedVerb)
                val metricTask = removeDuplicatedLeadingVerb(cleanedTask, profile.metricVerb)
                appendLine("Suggestion ${index + 1}")
                appendLine("Original Bullet:")
                appendLine("• ${cleanBulletTask(bullet)}")
                appendLine()
                appendLine("Improved Bullet:")
                appendLine("• ${profile.improvedVerb} $cleanedTask to improve ${profile.outcome}.")
                appendLine()
                appendLine("Metric Version:")
                appendLine("• ${profile.metricVerb} $metricTask, contributing to measurable gains such as ${profile.metricPlaceholders}.")
                appendLine()
            }
            appendLine("Truth Warning:")
            appendLine("Only use metrics and responsibilities that are true.")
        }.trim()
    }

    private fun cleanBulletTask(input: String): String {
        var cleaned = input
            .trim()
            .replace(Regex("""^(?:[•\-*]+|\d{1,2}[.)])\s*"""), "")
            .replace(Regex("""^[•\-*]+\s*"""), "")
            .trim()

        val weakPhrases = listOf(
            "worked on",
            "worked with",
            "helped with",
            "responsible for",
            "handled",
            "supported",
            "assisted with",
            "involved in",
            "participated in",
            "contributed to"
        )

        for (phrase in weakPhrases) {
            cleaned = cleaned.replace(
                Regex("""^${Regex.escape(phrase)}\b[:\-\s]*""", RegexOption.IGNORE_CASE),
                ""
            ).trim()
        }

        cleaned = cleaned
            .replace(Regex("""^[•\-*]+\s*"""), "")
            .replace("•", "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleaned.isBlank()) return "resume responsibilities"

        cleaned = cleaned.replaceFirstChar { char ->
            if (char.isUpperCase() && cleaned.getOrNull(1)?.isUpperCase() != true) {
                char.lowercase()
            } else {
                char.toString()
            }
        }

        return cleaned.ifBlank { "resume responsibilities" }
    }

    private fun removeDuplicatedLeadingVerb(task: String, verb: String): String {
        return task.replace(
            Regex("""^${Regex.escape(verb)}\b[:\-\s]*""", RegexOption.IGNORE_CASE),
            ""
        ).trim().ifBlank { "resume responsibilities" }
    }

    private fun extractCandidateBullets(resumeText: String): List<String> {
        val actionWords = listOf(
            "worked", "built", "developed", "analyzed", "managed", "created", "implemented",
            "improved", "designed", "coordinated", "led", "generated", "optimized",
            "handled", "supported", "assisted", "documented", "launched"
        )
        val seen = linkedSetOf<String>()

        resumeText.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .forEach { line ->
                val startsLikeBullet = line.matches(Regex("""^\s*(?:[•\-*]+|\d{1,2}[.)])\s+.+"""))
                val words = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val hasActionWord = actionWords.any { actionWord ->
                    line.contains(Regex("""\b${Regex.escape(actionWord)}\b""", RegexOption.IGNORE_CASE))
                }

                if (startsLikeBullet || (words.size in 5..35 && hasActionWord)) {
                    seen.add(line)
                }
            }

        return seen.take(20)
    }

    private fun rewriteProfileForRole(targetRole: String): RewriteProfile {
        val roleLower = targetRole.lowercase()
        return when {
            roleLower.contains("software engineer") -> RewriteProfile("Developed", "Optimized", "reliability, scalability, and user experience", "[X% improvement], [number] users/features, or [hours] saved")
            roleLower.contains("data analyst") -> RewriteProfile("Analyzed", "Automated", "reporting accuracy, KPI visibility, and business insights", "[X% faster reporting], [number] dashboards, or [hours] saved")
            roleLower.contains("business analyst") -> RewriteProfile("Documented", "Streamlined", "stakeholder alignment, requirement clarity, and process efficiency", "[X% fewer clarification cycles], [number] stakeholders, or [hours] saved")
            roleLower.contains("marketing") || roleLower.contains("digital marketing") -> RewriteProfile("Managed", "Optimized", "campaign performance, engagement, and lead generation", "[X% higher CTR], [number] leads, or [X% lower CPC]")
            roleLower.contains("product manager") -> RewriteProfile("Prioritized", "Improved", "product clarity, roadmap alignment, and user value", "[number] users, [X% adoption], or [number] features shipped")
            roleLower.contains("finance analyst") -> RewriteProfile("Analyzed", "Improved", "forecasting accuracy, budgeting clarity, and financial decision-making", "[X% variance reduction], [amount] cost visibility, or [hours] saved")
            roleLower.contains("sales executive") -> RewriteProfile("Managed", "Increased", "pipeline quality, client engagement, and revenue opportunities", "[number] leads, [X% conversion], or [amount] pipeline value")
            roleLower.contains("operations analyst") -> RewriteProfile("Improved", "Reduced", "workflow efficiency, process consistency, and operational productivity", "[X% cost reduction], [hours] saved, or [number] processes improved")
            roleLower.contains("hr executive") -> RewriteProfile("Supported", "Improved", "candidate experience, onboarding quality, and HR operations", "[X% faster hiring], [number] candidates, or [hours] saved")
            roleLower.contains("ux") || roleLower.contains("ui") -> RewriteProfile("Designed", "Improved", "usability, user flows, and product experience", "[X% task completion], [number] screens, or [number] users tested")
            roleLower.contains("project manager") -> RewriteProfile("Coordinated", "Improved", "delivery predictability, stakeholder alignment, and risk visibility", "[number] milestones, [X% on-time delivery], or [hours] saved")
            else -> RewriteProfile("Improved", "Strengthened", "clarity, execution, and measurable team outcomes", "[X% improvement], [number] outcomes, or [hours] saved")
        }
    }

    private fun generateInterviewQuestionsFromResume(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): String {
        val candidateLines = extractCandidateBullets(resumeText)
        val signalQuestions = buildResumeSignalQuestions(resumeText, candidateLines, targetRole, experienceLevel)
            .take(8)
            .toMutableList()

        candidateLines.forEach { line ->
            if (signalQuestions.size < 5) {
                val excerpt = shortExcerpt(cleanBulletTask(line))
                signalQuestions.add("Based on your resume line \"$excerpt\", explain the context, your exact contribution, tools used, and the result you can prove.")
            }
        }

        if (signalQuestions.isEmpty()) {
            signalQuestions.add("Based on the clearest resume responsibility you listed, explain the task, decisions you owned, and evidence that shows readiness for $targetRole.")
        }

        val jobDescriptionSection = buildJobDescriptionMatchQuestions(jobSpecification, resumeText)

        return buildString {
            appendLine("Resume-Based Interview Questions:")
            signalQuestions.take(8).forEachIndexed { index, question ->
                appendLine("${index + 1}. $question")
            }
            if (jobDescriptionSection.isNotBlank()) {
                appendLine()
                append(jobDescriptionSection)
            }
        }.trim()
    }

    private fun buildResumeSignalQuestions(
        resumeText: String,
        candidateLines: List<String>,
        targetRole: String,
        experienceLevel: String
    ): List<String> {
        val lower = resumeText.lowercase()
        val levelContext = if (experienceLevel.isBlank() || experienceLevel == "advanced") "" else " for the $experienceLevel level"
        val questions = linkedSetOf<String>()
        fun excerptFor(vararg keywords: String): String {
            val line = candidateLines.firstOrNull { candidate ->
                keywords.any { keyword -> candidate.contains(keyword, ignoreCase = true) }
            }
            return line?.let { shortExcerpt(cleanBulletTask(it)) }.orEmpty()
        }
        fun addSignal(signal: String, keywords: List<String>, question: (String) -> String) {
            if (keywords.any { lower.contains(it.lowercase()) }) {
                questions.add(question(excerptFor(*keywords.toTypedArray()).ifBlank { signal }))
            }
        }

        addSignal("project", listOf("project")) { ref -> "Based on your resume reference to \"$ref\", walk through the project goal, your exact ownership, and the outcome$levelContext." }
        addSignal("SQL", listOf("SQL")) { ref -> "You mention SQL in \"$ref\". Walk through a query or analysis you wrote, how you checked accuracy, and what decision it supported." }
        addSignal("Python", listOf("Python")) { ref -> "You mention Python in \"$ref\". Explain the problem, libraries or scripts used, and how you verified the output." }
        addSignal("Excel", listOf("Excel")) { ref -> "You mention Excel in \"$ref\". Describe the workbook, formulas, analysis, or reporting process and what it improved." }
        addSignal("Power BI", listOf("Power BI")) { ref -> "You mention Power BI in \"$ref\". Explain the dashboard model, audience, refresh process, and business question it answered." }
        addSignal("Tableau", listOf("Tableau")) { ref -> "You mention Tableau in \"$ref\". Walk through the visualization choices, data source, and insight delivered." }
        addSignal("dashboard/reporting", listOf("dashboard", "reporting", "report")) { ref -> "Based on \"$ref\", explain the reporting workflow, users, metrics tracked, and how the output changed decisions." }
        addSignal("API/backend/system/Java/Kotlin", listOf("API", "backend", "system", "Java", "Kotlin")) { ref -> "Based on \"$ref\", explain the technical design, tradeoffs, testing approach, and reliability considerations." }
        addSignal("campaign metrics", listOf("campaign", "CTR", "CPC", "leads", "conversion")) { ref -> "You reference campaign or funnel work in \"$ref\". Explain the channel, metric definitions, optimization steps, and measurable result." }
        addSignal("stakeholder/UAT/requirements/Jira/Agile", listOf("stakeholder", "UAT", "requirements", "Jira", "Agile")) { ref -> "Based on \"$ref\", describe how you gathered requirements, handled stakeholder feedback, tracked work, and confirmed acceptance." }
        addSignal("leadership", listOf("managed", "led", "coordinated")) { ref -> "Based on \"$ref\", explain who or what you coordinated, how you kept execution on track, and what changed because of your involvement." }
        if (extractMetricExamples(resumeText).isNotEmpty()) {
            val ref = shortExcerpt(extractMetricExamples(resumeText).first())
            questions.add("Based on the measurable line \"$ref\", explain how the number was calculated, your contribution, and why the metric mattered for $targetRole.")
        }

        return questions.toList()
    }

    private fun buildJobDescriptionMatchQuestions(jobSpecification: String, resumeText: String): String {
        if (jobSpecification.isBlank()) return ""
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()).take(3)
        if (jdKeywords.isEmpty()) return ""
        val resumeLower = resumeText.lowercase()
        return buildString {
            appendLine("Job Description Match Questions:")
            jdKeywords.forEachIndexed { index, keyword ->
                val evidence = if (resumeLower.contains(keyword.lowercase())) "show evidence of this" else "need honest evidence before adding it"
                appendLine("${index + 1}. The JD emphasizes $keyword. Where in your resume do you $evidence, and what proof would you discuss?")
            }
        }.trim()
    }

    private fun shortExcerpt(text: String, maxLength: Int = 120): String {
        val compact = text.replace(Regex("""\s+"""), " ").trim().trim('.', ';', ',')
        return if (compact.length <= maxLength) compact else compact.take(maxLength - 1).trimEnd() + "…"
    }

    private fun generateResumeSpecificAdvancedQuestions(
        resumeText: String,
        targetRole: String,
        jobSpecification: String
    ): String {
        val candidateLines = extractCandidateBullets(resumeText)
        val baseQuestions = buildResumeSignalQuestions(resumeText, candidateLines, targetRole, "advanced")
            .toMutableList()

        candidateLines.forEach { line ->
            if (baseQuestions.size < 10) {
                val excerpt = shortExcerpt(cleanBulletTask(line))
                baseQuestions.add("Based on your resume line \"$excerpt\", explain the business or technical problem, your decisions, and the evidence behind the result.")
            }
        }

        if (baseQuestions.size < 8) {
            extractMetricExamples(resumeText).forEach { metricLine ->
                if (baseQuestions.size < 10) {
                    val excerpt = shortExcerpt(metricLine)
                    baseQuestions.add("Based on the measurable resume excerpt \"$excerpt\", explain how you calculated that result and what you personally influenced.")
                }
            }
        }

        if (baseQuestions.isEmpty()) {
            baseQuestions.add("Choose one actual line from your resume and explain the work behind it, including task scope, tools, constraints, and verifiable outcome for $targetRole.")
        }

        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase()).take(2)
        jdKeywords.forEach { keyword ->
            if (baseQuestions.size < 10) {
                baseQuestions.add("The job description emphasizes $keyword. Point to the closest actual resume evidence and explain the gap honestly if the evidence is incomplete.")
            }
        }

        return buildString {
            appendLine("Advanced Resume-Specific Interview Questions:")
            baseQuestions.take(10).forEachIndexed { index, question ->
                appendLine("${index + 1}. $question")
                appendLine("   Why this may be asked: Interviewers need to verify that the resume claim is real, role-relevant, and owned by you.")
                appendLine("   Strong answer should mention: context, your specific action, tools or stakeholders, constraints, and a truthful result or metric.")
                appendLine("   Follow-up probe: What evidence, artifact, calculation, or decision would you show to support this answer?")
            }
        }.trim()
    }

    private data class RewriteProfile(
        val improvedVerb: String,
        val metricVerb: String,
        val outcome: String,
        val metricPlaceholders: String
    )

    private fun createResumeReport(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): ResumeReportResult {
        val resumeLower = resumeText.lowercase()
        val jobSpecLower = jobSpecification.lowercase()
        val roleLower = targetRole.lowercase()

        val roleKeywords = getRoleKeywordBank(roleLower)
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecLower)
        val combinedKeywords = (roleKeywords + jdKeywords).distinct()

        val foundKeywords = combinedKeywords.filter { keyword ->
            resumeLower.contains(keyword.lowercase())
        }

        val missingKeywords = combinedKeywords.filter { keyword ->
            !resumeLower.contains(keyword.lowercase())
        }

        val keywordMatchScore = calculateKeywordMatchScore(foundKeywords.size, combinedKeywords.size)
        val measurableImpactScore = calculateMeasurableImpactScore(resumeText)
        val actionVerbScore = calculateStrongActionVerbScore(resumeText)
        val sectionClarityScore = calculateSectionClarityScore(resumeText)
        val roleRelevanceScore = calculateRoleRelevanceScore(
            keywordMatchScore = keywordMatchScore,
            sectionClarityScore = sectionClarityScore,
            jobSpecificationProvided = jobSpecification.isNotBlank()
        )

        val overallScore = weightedOverallScore(
            keywordMatchScore = keywordMatchScore,
            measurableImpactScore = measurableImpactScore,
            actionVerbScore = actionVerbScore,
            sectionClarityScore = sectionClarityScore,
            roleRelevanceScore = roleRelevanceScore
        )

        val foundKeywordText = if (foundKeywords.isEmpty()) {
            "No strong role/job keywords detected yet."
        } else {
            foundKeywords.take(15).joinToString("\n") { "• $it" }
        }

        val missingKeywordText = if (missingKeywords.isEmpty()) {
            "No obvious missing keywords found from the current role/job specification."
        } else {
            missingKeywords.take(15).joinToString("\n") { keyword ->
                "• $keyword — add under ${suggestWhereToAddKeyword(keyword)}"
            }
        }

        val missingKeywordsHook = if (missingKeywords.isEmpty()) {
            """
Missing Keywords Hook:
Good news: no major missing keywords were detected from the selected role/job description.

Watch an ad to unlock the full breakdown, bullet rewrites, scoring logic, and interview questions.
            """.trimIndent()
        } else {
            """
Missing Keywords Found:
${missingKeywords.take(8).joinToString("\n") { keyword ->
                "• $keyword — add under ${suggestWhereToAddKeyword(keyword)}"
            }}

Watch an ad to unlock the full report with detailed scoring, bullet rewrites, and interview questions.
            """.trimIndent()
        }

        val measurableExplanation = explainMeasurableImpactScore(measurableImpactScore, resumeText)
        val actionVerbExplanation = explainActionVerbScore(actionVerbScore, resumeText)
        val clarityExplanation = explainSectionClarityScore(sectionClarityScore, resumeText)

        val basicFeedback = """
Strong:
• Resume reviewed for: $targetRole.
• Experience level considered: $experienceLevel.
• Found ${foundKeywords.size} relevant keyword(s) out of ${combinedKeywords.size} expected keyword(s).

Quick Scores:
• Keyword Match: $keywordMatchScore/100
• Measurable Details: $measurableImpactScore/100
• Strong Action Verbs: $actionVerbScore/100
• Section Clarity: $sectionClarityScore/100

Top fixes:
1. Add missing role/job keywords honestly.
2. Add measurable results such as %, users, revenue, time saved, cost reduced, accuracy, CTR, ROAS, or efficiency.
3. Start bullets with stronger action verbs like built, analyzed, improved, optimized, managed, automated, launched, coordinated.
        """.trimIndent()

        val fullReport = """
Detailed Resume Report

Category Scores:

Keyword Match: $keywordMatchScore/100
Found ${foundKeywords.size} out of ${combinedKeywords.size} expected role/job keywords.

Found Keywords:
$foundKeywordText

Missing Keywords:
$missingKeywordText

Measurable Impact: $measurableImpactScore/100
$measurableExplanation

Strong Action Verbs: $actionVerbScore/100
$actionVerbExplanation

Section Clarity: $sectionClarityScore/100
$clarityExplanation

Role Relevance: $roleRelevanceScore/100
This is based on resume evidence, keyword match, and job description alignment.

How to Add Keywords Honestly:
• Add tools like SQL, Excel, Power BI, Python, Figma, CRM under Skills only if you used them.
• Add responsibility keywords like stakeholder management, recruitment, campaign management, UAT, project planning under Experience only if you actually did them.
• Add project keywords under Projects only if your project involved them.
• Do not add fake skills or fake metrics.

Better Bullet Structure:
Action Verb + Task + Tool/Skill + Measurable Result

Example:
Weak:
Worked on reports and analysis.

Better:
Analyzed business performance data using Excel and dashboards to identify process gaps for stakeholders.

Stronger with Metric:
Analyzed business performance data using Excel dashboards, reducing manual reporting time by [X%] and improving weekly decision visibility for [team/business unit].

Truth Warning:
Only add keywords, tools, metrics, and responsibilities that reflect your real experience.
        """.trimIndent()

        return ResumeReportResult(
            overallScore = overallScore,
            keywordMatchScore = keywordMatchScore,
            measurableImpactScore = measurableImpactScore,
            actionVerbScore = actionVerbScore,
            sectionClarityScore = sectionClarityScore,
            roleRelevanceScore = roleRelevanceScore,
            foundKeywords = foundKeywords,
            missingKeywords = missingKeywords,
            basicFeedback = basicFeedback,
            missingKeywordsHook = missingKeywordsHook,
            fullReport = fullReport
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

        val strongActionVerbs = listOf(
            "achieved", "analyzed", "automated", "built", "coordinated",
            "created", "delivered", "designed", "developed", "executed",
            "generated", "implemented", "improved", "increased", "launched",
            "led", "managed", "optimized", "reduced", "streamlined",
            "resolved", "scaled", "planned", "tested", "deployed"
        )

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

        var score = 30
        score += (detectedSections.size * 12).coerceAtMost(60)

        if (wordCount in 250..1000) score += 10
        if (wordCount < 120) score -= 15
        if (wordCount > 1400) score -= 10

        return score.coerceIn(20, 95)
    }

    private fun calculateRoleRelevanceScore(
        keywordMatchScore: Int,
        sectionClarityScore: Int,
        jobSpecificationProvided: Boolean
    ): Int {
        var score = ((keywordMatchScore * 0.75) + (sectionClarityScore * 0.25)).roundToInt()
        if (jobSpecificationProvided) score += 5
        return score.coerceIn(20, 95)
    }

    private fun weightedOverallScore(
        keywordMatchScore: Int,
        measurableImpactScore: Int,
        actionVerbScore: Int,
        sectionClarityScore: Int,
        roleRelevanceScore: Int
    ): Int {
        val weighted =
            keywordMatchScore * 0.25 +
                    measurableImpactScore * 0.25 +
                    actionVerbScore * 0.20 +
                    sectionClarityScore * 0.20 +
                    roleRelevanceScore * 0.10

        return weighted.roundToInt().coerceIn(0, 100)
    }

    private fun detectSectionSignals(resumeText: String): List<String> {
        val lower = resumeText.lowercase()
        val detected = mutableListOf<String>()

        if (lower.contains("education") || lower.contains("university") || lower.contains("college") || lower.contains("degree")) {
            detected.add("Education")
        }

        if (lower.contains("experience") || lower.contains("work experience") || lower.contains("employment") || lower.contains("internship") || lower.contains("intern")) {
            detected.add("Past Experience / Internship")
        }

        if (lower.contains("skills") || lower.contains("technical skills") || lower.contains("tools") || lower.contains("technologies")) {
            detected.add("Skills")
        }

        if (lower.contains("projects") || lower.contains("project")) {
            detected.add("Projects")
        }

        if (lower.contains("summary") || lower.contains("profile") || lower.contains("objective")) {
            detected.add("Summary / Profile")
        }

        if (lower.contains("certification") || lower.contains("certifications") || lower.contains("certificate")) {
            detected.add("Certifications")
        }

        return detected
    }

    private fun detectMissingSections(resumeText: String): List<String> {
        val detected = detectSectionSignals(resumeText).toSet()
        val important = listOf("Education", "Past Experience / Internship", "Skills", "Projects")

        return important.filter { it !in detected }
    }

    private fun countMetricSignals(text: String): Int {
        val regex = Regex(
            """(\d+%|\d+\+?|\$|₹|€|£|users|customers|clients|hours|days|weeks|months|revenue|cost|accuracy|ctr|cpc|cpa|roas|roi|sales|growth|budget|leads)""",
            RegexOption.IGNORE_CASE
        )
        return regex.findAll(text).count()
    }

    private fun countResultWords(text: String): Int {
        val lower = text.lowercase()
        val resultWords = listOf(
            "improved", "reduced", "increased", "saved", "optimized",
            "growth", "revenue", "accuracy", "efficiency", "conversion",
            "users", "customers", "clients", "performance", "cost",
            "time", "sales", "leads", "roi", "roas", "ctr"
        )

        return resultWords.count { lower.contains(it) }
    }

    private fun explainMeasurableImpactScore(score: Int, resumeText: String): String {
        val examples = extractMetricExamples(resumeText)
        val guidance = when {
            score >= 80 -> "Your measurable-impact score is strong because the resume includes evidence signals below."
            score >= 60 -> "Your resume has some measurable evidence, but more bullets should connect work to concrete results."
            else -> "Your resume needs more measurable evidence such as %, users, revenue, cost saved, time saved, accuracy, CTR, ROAS, ROI, or efficiency."
        }
        val examplesText = if (examples.isEmpty()) {
            "No clear measurable examples detected."
        } else {
            "Detected measurable examples:\n" + examples.joinToString("\n") { "• $it" }
        }
        return "$guidance\n$examplesText"
    }

    private fun extractMetricExamples(resumeText: String): List<String> {
        val metricRegex = Regex(
            """(%|\b\d+[\w.%+-]*\b|users?|customers?|clients?|hours?|days?|weeks?|months?|revenue|cost|budget|sales|leads?|accuracy|performance|latency|CTR|CPC|CPA|ROAS|ROI|improved|reduced|increased|saved|optimized)""",
            RegexOption.IGNORE_CASE
        )
        return resumeText.lineSequence()
            .map { line -> shortExcerpt(cleanBulletTask(line), 140) }
            .filter { line -> line.isNotBlank() && metricRegex.containsMatchIn(line) }
            .distinct()
            .take(5)
            .toList()
    }

    private fun explainActionVerbScore(score: Int, resumeText: String): String {
        val examples = extractActionVerbExamples(resumeText)
        val guidance = when {
            score >= 80 -> "Your resume uses strong action verbs in the examples below."
            score >= 60 -> "Your resume uses some action verbs, but several bullets can start with stronger verbs."
            else -> "Your resume needs stronger action verbs. Avoid weak phrases like 'worked on' and use verbs like analyzed, built, improved, managed, optimized, automated, or launched."
        }
        val examplesText = if (examples.isEmpty()) {
            "No strong action-verb examples detected."
        } else {
            "Detected action-verb examples:\n" + examples.joinToString("\n") { "• $it" }
        }
        return "$guidance\n$examplesText"
    }

    private fun extractActionVerbExamples(resumeText: String): List<String> {
        val strongVerbRegex = Regex(
            """\b(achieved|analyzed|automated|built|coordinated|created|delivered|designed|developed|executed|generated|implemented|improved|increased|launched|led|managed|optimized|reduced|streamlined|resolved|scaled|planned|tested|deployed)\b""",
            RegexOption.IGNORE_CASE
        )
        return resumeText.lineSequence()
            .map { line -> shortExcerpt(cleanBulletTask(line), 140) }
            .filter { line -> line.isNotBlank() && strongVerbRegex.containsMatchIn(line) }
            .distinct()
            .take(5)
            .toList()
    }

    private fun explainSectionClarityScore(score: Int, resumeText: String): String {
        val detected = detectSectionSignals(resumeText)
        val missing = detectMissingSections(resumeText)
        val guidance = when {
            score >= 80 -> "Your resume has clear section structure based on the sections detected below."
            score >= 60 -> "Your resume has some important sections, but review the missing list below for possible structure gaps."
            else -> "Your resume structure is weak. Add clear sections such as Education, Experience, Skills, and Projects where truthful."
        }
        val detectedText = if (detected.isEmpty()) {
            "Detected sections:\n• No clear section headings detected."
        } else {
            "Detected sections:\n" + detected.joinToString("\n") { "• $it" }
        }
        val missingText = if (missing.isEmpty()) {
            "Missing important sections:\nNo major missing section detected."
        } else {
            "Missing important sections:\n" + missing.joinToString("\n") { "• $it" }
        }
        return "$guidance\n$detectedText\n\n$missingText"
    }

    private fun getRoleKeywordBank(roleLower: String): List<String> {
        return when {
            roleLower.contains("software engineer") -> listOf(
                "Java", "Kotlin", "Python", "REST API", "SQL", "Git",
                "unit testing", "debugging", "CI/CD", "system design",
                "database", "cloud", "Agile", "backend", "frontend", "API integration"
            )

            roleLower.contains("data analyst") -> listOf(
                "SQL", "Excel", "Python", "Power BI", "Tableau",
                "data cleaning", "data visualization", "dashboard", "KPI",
                "statistics", "reporting", "business insights", "data modeling",
                "ETL", "trend analysis"
            )

            roleLower.contains("business analyst") -> listOf(
                "requirements gathering", "stakeholder management",
                "business process analysis", "user stories", "UAT", "Jira",
                "Agile", "SQL", "Excel", "Power BI", "Tableau", "KPI",
                "gap analysis", "process improvement", "BRD", "FRD"
            )

            roleLower.contains("marketing executive") -> listOf(
                "campaign management", "brand awareness", "lead generation",
                "market research", "customer segmentation", "content strategy",
                "social media marketing", "email marketing", "conversion rate",
                "CTR", "CPC", "ROI", "marketing funnel", "copywriting"
            )

            roleLower.contains("digital marketing") -> listOf(
                "SEO", "SEM", "Google Analytics", "Google Ads", "Meta Ads",
                "campaign management", "conversion rate", "CTR", "CPC",
                "CPA", "ROAS", "lead generation", "A/B testing",
                "email marketing", "landing page optimization"
            )

            roleLower.contains("product manager") -> listOf(
                "product roadmap", "user research", "market research",
                "stakeholder management", "PRD", "user stories", "analytics",
                "A/B testing", "KPI", "go-to-market", "prioritization",
                "customer feedback", "feature planning", "product strategy"
            )

            roleLower.contains("finance analyst") -> listOf(
                "financial analysis", "Excel", "financial modeling",
                "forecasting", "budgeting", "variance analysis", "valuation",
                "reporting", "Power BI", "risk analysis", "P&L", "cash flow",
                "cost analysis", "scenario analysis"
            )

            roleLower.contains("sales executive") -> listOf(
                "lead generation", "CRM", "sales pipeline", "cold calling",
                "client relationship management", "negotiation", "sales targets",
                "revenue growth", "B2B sales", "B2C sales",
                "customer acquisition", "account management", "conversion rate"
            )

            roleLower.contains("operations analyst") -> listOf(
                "process improvement", "operations management",
                "workflow optimization", "Excel", "Power BI", "KPI",
                "cost reduction", "supply chain", "inventory management",
                "vendor management", "reporting", "root cause analysis",
                "SOP", "quality control"
            )

            roleLower.contains("hr executive") -> listOf(
                "recruitment", "talent acquisition", "onboarding",
                "employee engagement", "HR operations", "payroll",
                "performance management", "HRIS", "sourcing", "screening",
                "interview coordination", "employee relations", "training"
            )

            roleLower.contains("ux") || roleLower.contains("ui") -> listOf(
                "user research", "wireframing", "prototyping", "Figma",
                "usability testing", "design systems", "user flows",
                "interaction design", "visual design", "accessibility",
                "information architecture", "customer journey", "A/B testing"
            )

            roleLower.contains("project manager") -> listOf(
                "project planning", "stakeholder management", "risk management",
                "Agile", "Scrum", "Jira", "timeline management",
                "budget management", "resource allocation",
                "cross-functional collaboration", "status reporting",
                "delivery management", "scope management"
            )

            else -> listOf(
                "communication", "analysis", "problem solving",
                "project management", "stakeholder management", "reporting",
                "Excel", "presentation", "collaboration", "process improvement"
            )
        }
    }

    private fun extractSimpleKeywordsFromJobSpec(jobSpecLower: String): List<String> {
        if (jobSpecLower.isBlank()) return emptyList()

        val possibleKeywords = listOf(
            "sql", "excel", "python", "power bi", "tableau",
            "google analytics", "seo", "sem", "google ads", "meta ads",
            "stakeholder management", "requirements gathering", "user stories",
            "uat", "jira", "agile", "scrum", "rest api", "git", "kotlin",
            "java", "campaign management", "lead generation", "a/b testing",
            "conversion rate", "ctr", "cpc", "cpa", "roas",
            "financial modeling", "forecasting", "budgeting", "dashboard",
            "reporting", "market research", "product roadmap", "communication",
            "project management", "crm", "sales pipeline", "cold calling",
            "client relationship management", "recruitment", "talent acquisition",
            "onboarding", "figma", "wireframing", "prototyping",
            "usability testing", "process improvement", "operations management",
            "workflow optimization", "supply chain", "vendor management",
            "risk management"
        )

        return possibleKeywords.filter { keyword ->
            jobSpecLower.contains(keyword)
        }
    }

    private fun suggestWhereToAddKeyword(keyword: String): String {
        val lower = keyword.lowercase()

        return when {
            lower in listOf(
                "sql", "excel", "python", "power bi", "tableau",
                "google analytics", "jira", "git", "kotlin", "java",
                "figma", "crm"
            ) -> "Skills"

            lower.contains("management") ||
                    lower.contains("gathering") ||
                    lower.contains("stories") ||
                    lower.contains("uat") ||
                    lower.contains("campaign") ||
                    lower.contains("recruitment") ||
                    lower.contains("sales") ||
                    lower.contains("client") ||
                    lower.contains("onboarding") -> "Experience"

            lower.contains("dashboard") ||
                    lower.contains("analysis") ||
                    lower.contains("forecasting") ||
                    lower.contains("modeling") ||
                    lower.contains("testing") ||
                    lower.contains("research") ||
                    lower.contains("optimization") -> "Projects or Experience"

            else -> "Skills or Experience"
        }
    }
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
    val fullReport: String
)
