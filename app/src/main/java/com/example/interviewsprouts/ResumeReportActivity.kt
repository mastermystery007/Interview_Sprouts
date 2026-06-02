package com.example.interviewsprouts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
        val textFullReport = findViewById<TextView>(R.id.textFullReport)

        val btnUnlockFullReport = findViewById<Button>(R.id.btnUnlockFullReport)
        val btnRewriteBullets = findViewById<Button>(R.id.btnRewriteBullets)
        val btnInterviewQuestions = findViewById<Button>(R.id.btnInterviewQuestions)

        val dummyReport = createDummyResumeReport(
            resumeText = resumeText,
            targetRole = targetRole,
            experienceLevel = experienceLevel,
            jobSpecification = jobSpecification
        )

        textTargetRole.text = "Target Role: $targetRole | Level: $experienceLevel"
        textOverallScore.text = "${dummyReport.overallScore} / 100"
        textBasicFeedback.text = dummyReport.basicFeedback
        textFullReport.text = dummyReport.fullReport

        btnUnlockFullReport.setOnClickListener {
            textFullReport.visibility = View.VISIBLE
            btnUnlockFullReport.visibility = View.GONE
        }

        btnRewriteBullets.setOnClickListener {
            startActivity(Intent(this, BulletRewriterActivity::class.java))
        }

        btnInterviewQuestions.setOnClickListener {
            startActivity(Intent(this, InterviewPracticeActivity::class.java))
        }
    }

    private fun createDummyResumeReport(
        resumeText: String,
        targetRole: String,
        experienceLevel: String,
        jobSpecification: String
    ): ResumeReportDummy {
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

        val hasMetrics = resumeText.contains("%") ||
                resumeLower.contains("improved") ||
                resumeLower.contains("reduced") ||
                resumeLower.contains("increased") ||
                resumeLower.contains("saved") ||
                resumeLower.contains("optimized")

        val hasProjects = resumeLower.contains("project")
        val hasExperience = resumeLower.contains("experience") ||
                resumeLower.contains("intern") ||
                resumeLower.contains("worked") ||
                resumeLower.contains("managed")

        var score = 55

        if (hasExperience) score += 8
        if (hasProjects) score += 8
        if (hasMetrics) score += 12
        if (foundKeywords.size >= 3) score += 8
        if (foundKeywords.size >= 6) score += 7
        if (jobSpecification.isNotBlank()) score += 5

        if (score > 92) score = 92

        val missingKeywordText = if (missingKeywords.isEmpty()) {
            "No obvious missing keywords found from the current role/job specification."
        } else {
            missingKeywords.take(12).joinToString(separator = "\n") { keyword ->
                val whereToAdd = suggestWhereToAddKeyword(keyword)
                "• $keyword — add under $whereToAdd only if you genuinely have this experience."
            }
        }

        val foundKeywordText = if (foundKeywords.isEmpty()) {
            "No strong role/job keywords detected yet."
        } else {
            foundKeywords.take(12).joinToString(separator = "\n") { keyword ->
                "• $keyword"
            }
        }

        val jobSpecNote = if (jobSpecification.isBlank()) {
            "No job specification was pasted. For better bespoke suggestions, paste the exact job description next time."
        } else {
            "Job specification was used to compare your resume against the role requirements."
        }

        val basicFeedback = """
Strong:
• Your resume has been reviewed for: $targetRole.
• Experience level considered: $experienceLevel.
• $jobSpecNote

Weak:
• Some bullets may be too general.
• Add measurable outcomes where possible.
• Add role-specific keywords only when they honestly match your experience.

Top fixes:
1. Add missing role/job keywords where truthful.
2. Rewrite weak bullets using action verbs and measurable impact.
3. Match your strongest projects or experience to the job specification.
        """.trimIndent()

        val fullReport = """
Detailed Resume Report

Overall logic:
This report compares your resume against:
• Target role: $targetRole
• Experience level: $experienceLevel
• Optional job specification: ${if (jobSpecification.isBlank()) "Not provided" else "Provided"}

Category Scores

Clarity: 76/100
Your resume can be understood, but some bullets may need sharper wording.

Impact: ${if (hasMetrics) "74" else "55"}/100
${if (hasMetrics) "You have some measurable/action-oriented language." else "Your resume needs more metrics such as %, users, revenue, cost, time saved, accuracy, or campaign results."}

Role Relevance: ${if (foundKeywords.size >= 4) "78" else "60"}/100
Your role relevance depends on how many expected role/job keywords appear naturally in your resume.

Keyword Match: ${if (foundKeywords.size >= 6) "82" else "58"}/100
Found keywords:
$foundKeywordText

Missing or Weak Keywords:
$missingKeywordText

How to Add Keywords Honestly:
• Add tools under Skills only if you have used them.
• Add responsibilities under Experience only if you actually performed them.
• Add domain terms under Projects only if the project involved them.
• Do not keyword-stuff or add fake skills.

Suggested Resume Improvements:
1. Start bullets with strong action verbs: analyzed, built, managed, optimized, improved, automated, coordinated.
2. Add measurable results: increased CTR by [X%], reduced processing time by [Y%], improved reporting accuracy, saved [hours] per week.
3. Use job-specific language from the job description.
4. Move the most relevant experience/projects higher.
5. Add a short professional summary tailored to $targetRole.

Example Bullet Rewrite:
Original:
Worked on reports and analysis.

Improved:
Analyzed business performance data and prepared actionable reports for stakeholders, helping identify process gaps and improvement opportunities.

Metric Version:
Analyzed business performance data and prepared stakeholder reports, reducing manual reporting time by [X%] and improving decision-making visibility across [team/business unit].

Role-Specific Interview Questions:
1. Why are you interested in this $targetRole role?
2. Which part of your resume best matches this job specification?
3. Tell me about a project or experience where you used one of the key required skills.
4. What measurable result did you create in your past work or project?
5. Which missing skill from this job description are you currently improving?

Truth Warning:
Only add keywords, tools, metrics, and responsibilities that reflect your real experience. If a keyword is important but you do not have experience with it, build a small project or take a short course before adding it.
        """.trimIndent()

        return ResumeReportDummy(
            overallScore = score,
            basicFeedback = basicFeedback,
            fullReport = fullReport
        )
    }

    private fun getRoleKeywordBank(roleLower: String): List<String> {
        return when {
            roleLower.contains("business analyst") -> listOf(
                "requirements gathering",
                "stakeholder management",
                "business process analysis",
                "user stories",
                "UAT",
                "Jira",
                "Agile",
                "SQL",
                "Excel",
                "Power BI",
                "Tableau",
                "KPI",
                "gap analysis",
                "process improvement"
            )

            roleLower.contains("marketing") -> listOf(
                "SEO",
                "SEM",
                "Google Analytics",
                "Meta Ads",
                "Google Ads",
                "campaign management",
                "conversion rate",
                "CTR",
                "CPC",
                "CPA",
                "ROAS",
                "lead generation",
                "email marketing",
                "A/B testing",
                "content strategy"
            )

            roleLower.contains("data analyst") -> listOf(
                "SQL",
                "Excel",
                "Python",
                "Power BI",
                "Tableau",
                "data cleaning",
                "data visualization",
                "dashboard",
                "KPI",
                "statistics",
                "reporting",
                "business insights"
            )

            roleLower.contains("software") ||
                    roleLower.contains("developer") ||
                    roleLower.contains("engineer") -> listOf(
                "Java",
                "Kotlin",
                "Python",
                "REST API",
                "SQL",
                "Git",
                "unit testing",
                "debugging",
                "CI/CD",
                "system design",
                "database",
                "cloud",
                "Agile"
            )

            roleLower.contains("product") -> listOf(
                "roadmap",
                "user research",
                "market research",
                "stakeholder management",
                "PRD",
                "user stories",
                "analytics",
                "A/B testing",
                "KPI",
                "go-to-market",
                "prioritization"
            )

            roleLower.contains("finance") -> listOf(
                "financial analysis",
                "Excel",
                "financial modeling",
                "forecasting",
                "budgeting",
                "variance analysis",
                "valuation",
                "reporting",
                "Power BI",
                "risk analysis"
            )

            else -> listOf(
                "communication",
                "analysis",
                "problem solving",
                "project management",
                "stakeholder management",
                "reporting",
                "Excel",
                "presentation",
                "collaboration",
                "process improvement"
            )
        }
    }

    private fun extractSimpleKeywordsFromJobSpec(jobSpecLower: String): List<String> {
        if (jobSpecLower.isBlank()) return emptyList()

        val possibleKeywords = listOf(
            "sql",
            "excel",
            "python",
            "power bi",
            "tableau",
            "google analytics",
            "seo",
            "sem",
            "stakeholder management",
            "requirements gathering",
            "user stories",
            "uat",
            "jira",
            "agile",
            "rest api",
            "git",
            "kotlin",
            "java",
            "campaign management",
            "lead generation",
            "a/b testing",
            "conversion rate",
            "financial modeling",
            "forecasting",
            "dashboard",
            "reporting",
            "market research",
            "product roadmap",
            "communication",
            "project management"
        )

        return possibleKeywords.filter { keyword ->
            jobSpecLower.contains(keyword)
        }
    }

    private fun suggestWhereToAddKeyword(keyword: String): String {
        val lower = keyword.lowercase()

        return when {
            lower in listOf(
                "sql",
                "excel",
                "python",
                "power bi",
                "tableau",
                "google analytics",
                "jira",
                "git",
                "kotlin",
                "java"
            ) -> "Skills or Projects"

            lower.contains("management") ||
                    lower.contains("gathering") ||
                    lower.contains("stories") ||
                    lower.contains("uat") ||
                    lower.contains("campaign") -> "Experience"

            lower.contains("dashboard") ||
                    lower.contains("analysis") ||
                    lower.contains("forecasting") ||
                    lower.contains("modeling") -> "Projects or Experience"

            else -> "Skills, Projects, or Experience"
        }
    }
}

data class ResumeReportDummy(
    val overallScore: Int,
    val basicFeedback: String,
    val fullReport: String
)