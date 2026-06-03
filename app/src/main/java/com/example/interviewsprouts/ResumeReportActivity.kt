package com.example.interviewsprouts

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

        val btnUnlockFullReport = findViewById<Button>(R.id.btnUnlockFullReport)
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
                val cleanedTask = cleanBulletTask(bullet)
                appendLine("Suggestion ${index + 1}")
                appendLine("Original Bullet:")
                appendLine("• ${bullet.removePrefix("-").removePrefix("•").removePrefix("*").trim()}")
                appendLine()
                appendLine("Improved Bullet:")
                appendLine("• ${profile.improvedVerb} $cleanedTask to improve ${profile.outcome}.")
                appendLine()
                appendLine("Metric Version:")
                appendLine("• ${profile.metricVerb} $cleanedTask, contributing to measurable gains such as ${profile.metricPlaceholders}.")
                appendLine()
            }
            appendLine("Truth Warning:")
            appendLine("Only use metrics and responsibilities that are true.")
        }.trim()
    }

    private fun cleanBulletTask(input: String): String {
        var cleaned = input
            .trim()
            .removePrefix("-")
            .removePrefix("•")
            .removePrefix("*")
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
                val startsLikeBullet = line.startsWith("-") || line.startsWith("•") || line.startsWith("*")
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
        // TODO: Replace offline generation with backend LLM call. Do not call LLM API directly from Android.
        val resumeLower = resumeText.lowercase()
        val resumeQuestions = mutableListOf<String>()

        if (resumeLower.contains("project")) {
            resumeQuestions.add("Walk me through the strongest project on your resume and explain your exact contribution.")
        }
        if (resumeText.contains("SQL", ignoreCase = true)) {
            resumeQuestions.add("How have you used SQL in your resume experience, and how did you validate the results?")
        }
        if (resumeText.contains("Python", ignoreCase = true)) {
            resumeQuestions.add("Describe a Python project from your resume and the problem it solved.")
        }
        if (resumeLower.contains("dashboard") || resumeLower.contains("reporting")) {
            resumeQuestions.add("Tell me about a dashboard or reporting workflow you built and what insights it provided.")
        }
        if (resumeLower.contains("api") || resumeLower.contains("backend") || resumeLower.contains("system")) {
            resumeQuestions.add("Explain a technical design decision you made for an API, backend, or system mentioned on your resume.")
        }
        if (resumeLower.contains("campaign") || resumeLower.contains("ctr") || resumeLower.contains("leads")) {
            resumeQuestions.add("How did you measure campaign performance, CTR, leads, or conversion impact in your work?")
        }
        if (resumeLower.contains("stakeholder") || resumeLower.contains("uat") || resumeLower.contains("requirements")) {
            resumeQuestions.add("How did you gather requirements, manage stakeholders, or support UAT in a resume experience?")
        }

        while (resumeQuestions.size < 3) {
            resumeQuestions.add(
                when (resumeQuestions.size) {
                    0 -> "Which resume achievement best proves you are ready for a $targetRole role?"
                    1 -> "What was the most challenging responsibility on your resume, and how did you handle it?"
                    else -> "Which resume bullet would you strengthen with a true metric, and what evidence would support it?"
                }
            )
        }

        val roleQuestions = listOf(
            "Why are you targeting $targetRole roles at the $experienceLevel level?",
            "Which skills from your resume most closely match the expectations for $targetRole?",
            "What would you prioritize in your first 30 days in a $targetRole position?"
        )
        val behavioralQuestions = listOf(
            "Tell me about a time you solved a difficult problem using experience shown on your resume.",
            "Describe a time you received feedback and improved your work.",
            "Tell me about a time you worked with a teammate, stakeholder, or manager to complete an important task."
        )

        val jobDescriptionSection = if (jobSpecification.isNotBlank()) {
            """

                Job Description Match Questions:
                1. Which requirement in the job specification best matches your resume, and what proof can you share?
                2. Which job specification keyword is missing or weak in your resume, and how are you improving it honestly?
                3. What would you clarify about this job specification before accepting or starting the role?
            """.trimIndent()
        } else {
            ""
        }

        return """
            Resume-Based Interview Questions:
            ${resumeQuestions.take(3).mapIndexed { index, question -> "${index + 1}. $question" }.joinToString("\n")}

            Role-Specific Interview Questions:
            ${roleQuestions.mapIndexed { index, question -> "${index + 1}. $question" }.joinToString("\n")}

            Behavioral Questions:
            ${behavioralQuestions.mapIndexed { index, question -> "${index + 1}. $question" }.joinToString("\n")}
            $jobDescriptionSection
        """.trimIndent()
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

        val measurableExplanation = explainMeasurableImpactScore(measurableImpactScore)
        val actionVerbExplanation = explainActionVerbScore(actionVerbScore)
        val clarityExplanation = explainSectionClarityScore(sectionClarityScore)

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

Overall Score: $overallScore/100

Scoring Formula:
• Keyword Match: 25%
• Measurable Impact: 25%
• Strong Action Verbs: 20%
• Section Clarity: 20%
• Role Relevance: 10%

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
This combines keyword match, resume structure, and whether you provided a job specification.

Detected Resume Sections:
${detectSectionSignals(resumeText).joinToString("\n") { "• $it" }}

Missing Important Sections:
${detectMissingSections(resumeText).joinToString("\n") { "• $it" }.ifBlank { "No major missing section detected." }}

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

Role-Specific Interview Questions:
1. Why are you interested in this $targetRole role?
2. Which part of your resume best matches this job specification?
3. Tell me about a project or experience where you used one of the key required skills.
4. What measurable result did you create in your past work or project?
5. Which missing skill from this job description are you currently improving?

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

    private fun explainMeasurableImpactScore(score: Int): String {
        return when {
            score >= 80 -> "Your resume includes strong measurable details such as numbers, percentages, users, revenue, time saved, or performance improvements."
            score >= 60 -> "Your resume has some measurable details, but more bullets should include concrete results."
            else -> "Your resume has few measurable details. Add numbers such as %, users, revenue, cost saved, time saved, accuracy, CTR, ROAS, or efficiency."
        }
    }

    private fun explainActionVerbScore(score: Int): String {
        return when {
            score >= 80 -> "Your resume uses many strong action verbs."
            score >= 60 -> "Your resume uses some action verbs, but several bullets can start with stronger verbs."
            else -> "Your resume needs stronger action verbs. Avoid weak phrases like 'worked on' and use verbs like analyzed, built, improved, managed, optimized, automated, or launched."
        }
    }

    private fun explainSectionClarityScore(score: Int): String {
        return when {
            score >= 80 -> "Your resume has clear important sections such as education, experience, skills, and projects."
            score >= 60 -> "Your resume has some important sections, but one or more core sections may be missing or unclear."
            else -> "Your resume structure is weak. Add clear sections such as Education, Experience, Skills, and Projects."
        }
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
