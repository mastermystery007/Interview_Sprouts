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
        val targetRole = intent.getStringExtra("target_role") ?: "Software Engineer"
        val experienceLevel = intent.getStringExtra("experience_level") ?: "Fresh Graduate"

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
            experienceLevel = experienceLevel
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
        experienceLevel: String
    ): ResumeReportDummy {
        val hasProjects = resumeText.contains("project", ignoreCase = true)
        val hasPython = resumeText.contains("python", ignoreCase = true)
        val hasMetrics = resumeText.contains("%") ||
                resumeText.contains("improved", ignoreCase = true) ||
                resumeText.contains("reduced", ignoreCase = true)

        var score = 65

        if (hasProjects) score += 8
        if (hasPython) score += 5
        if (hasMetrics) score += 10

        if (score > 90) score = 90

        val basicFeedback = """
Strong:
• Your resume shows relevant background for $targetRole.
• Projects and technical skills are useful for interview discussion.

Weak:
• Some bullet points may be too general.
• Add more measurable impact where possible.
• Tailor keywords more strongly toward $targetRole.

Top fixes:
1. Rewrite bullets using action verbs.
2. Add metrics such as users, accuracy, latency, time saved, or cost reduced.
3. Move the strongest projects and experience higher.
        """.trimIndent()

        val fullReport = """
Detailed Resume Report

Clarity: 78/100
Your resume is understandable, but some bullets can be sharper.

Impact: 62/100
You describe tasks, but not enough outcomes. Recruiters prefer achievements.

Role Relevance: 70/100
The resume is somewhat relevant for $targetRole, but it should include more role-specific keywords.

Quantification: 45/100
Add numbers wherever possible:
• improved accuracy by [X%]
• reduced processing time by [X%]
• served [number] users
• automated [task], saving [hours] per week

ATS Keyword Match: 68/100
Possible missing keywords:
• REST APIs
• SQL
• Git
• Unit testing
• Cloud deployment
• CI/CD
• System design

Rewritten Bullet Example:
Original:
Worked on backend APIs.

Improved:
Developed and maintained REST APIs for core application workflows, improving backend reliability and supporting scalable feature delivery.

Metric Version:
Developed and optimized REST APIs for core application workflows, reducing response time by [X%] and supporting [number] monthly users.

Interview Questions:
1. Walk me through your strongest project.
2. What technical challenge did you face in your project?
3. How did you measure success?
4. What would you improve if you rebuilt it?
5. Tell me about a time you solved a difficult problem.

Note:
Replace all placeholders like [X%] and [number] with real values only.
        """.trimIndent()

        return ResumeReportDummy(
            overallScore = score,
            basicFeedback = basicFeedback,
            fullReport = fullReport
        )
    }
}

data class ResumeReportDummy(
    val overallScore: Int,
    val basicFeedback: String,
    val fullReport: String
)