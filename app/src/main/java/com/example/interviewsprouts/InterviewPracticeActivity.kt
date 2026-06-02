package com.example.interviewsprouts

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InterviewPracticeActivity : AppCompatActivity() {

    private val professions = listOf(
        "Software Engineer", "Data Analyst", "Business Analyst", "Marketing Executive",
        "Digital Marketing Specialist", "Product Manager", "Finance Analyst", "Sales Executive",
        "Operations Analyst", "HR Executive", "UX/UI Designer", "Project Manager"
    )

    private val experiences = listOf(
        "Student", "Internship Applicant", "Fresh Graduate", "0-1 Years", "1-2 Years",
        "2-4 Years", "4-7 Years", "7+ Years", "Career Switcher"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_practice)

        val professionSpinner = findViewById<Spinner>(R.id.spinnerInterviewProfession)
        val experienceSpinner = findViewById<Spinner>(R.id.spinnerInterviewExperience)
        val jobDescriptionInput = findViewById<EditText>(R.id.editInterviewJobDescription)
        val generateButton = findViewById<Button>(R.id.btnGenerateQuestions)
        val outputText = findViewById<TextView>(R.id.textInterviewOutput)

        professionSpinner.adapter = spinnerAdapter(professions)
        experienceSpinner.adapter = spinnerAdapter(experiences)

        val targetRole = intent.getStringExtra("target_role") ?: ""
        val targetIndex = professions.indexOfFirst { it.equals(targetRole, ignoreCase = true) }
        if (targetIndex >= 0) professionSpinner.setSelection(targetIndex)

        generateButton.setOnClickListener {
            val profession = professionSpinner.selectedItem.toString()
            val experience = experienceSpinner.selectedItem.toString()
            val jobDescription = jobDescriptionInput.text.toString().trim()
            outputText.text = buildQuestions(profession, experience, jobDescription)
        }
    }

    private fun spinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun buildQuestions(profession: String, experience: String, jobDescription: String): String {
        val topics = roleTopics(profession)
        val jdQuestions = if (jobDescription.isNotBlank()) {
            """

            Job-Description Questions:
            1. Which requirement in this job description best matches your current experience, and what proof can you share?
            2. Which requirement is your weakest area, and how are you improving it honestly?
            3. If hired, what would you clarify first from this job description before starting work?
            """.trimIndent()
        } else {
            ""
        }

        return """
            Interview Practice Set
            Role: $profession
            Experience Level: $experience

            Behavioral Questions:
            1. Tell me about yourself and why you are targeting $profession roles.
            2. Describe a time you solved a difficult problem with limited guidance.
            3. Tell me about a time you received feedback and improved your work.
            4. Describe a time you worked with a teammate or stakeholder who had a different opinion.
            5. Tell me about a time you had to learn something quickly to complete a task.

            Role-Specific Questions:
            1. How have you used or learned ${topics[0]} in a project, internship, class, or job?
            2. Walk me through your approach to ${topics[1]} for a $profession task.
            3. What mistakes can happen in ${topics[2]}, and how would you prevent them?
            4. How would you explain ${topics[3]} to a non-technical stakeholder?
            5. Describe a practical example where ${topics[4]} improved the final outcome.

            Resume / Project Questions:
            1. Which resume project or experience best proves you can do this $profession role?
            2. What was your exact contribution, and what would your teammate or manager say you owned?
            3. What measurable result can you honestly add to that project, such as [X%], [number], or [hours]?
            $jdQuestions

            Practice Tip:
            Answer with Situation, Task, Action, and Result. Use real metrics only; placeholders are reminders, not invented numbers.
        """.trimIndent()
    }

    private fun roleTopics(profession: String): List<String> {
        return when (profession) {
            "Software Engineer" -> listOf("APIs", "debugging", "testing", "performance", "system design")
            "Data Analyst" -> listOf("SQL", "dashboards", "data cleaning", "insights", "KPIs")
            "Business Analyst" -> listOf("requirements", "stakeholders", "UAT", "process gaps", "workflow documentation")
            "Marketing Executive", "Digital Marketing Specialist" -> listOf("campaigns", "CTR", "conversion", "SEO", "leads")
            "Product Manager" -> listOf("roadmap", "prioritization", "user feedback", "metrics", "feature tradeoffs")
            "Finance Analyst" -> listOf("forecasting", "budgeting", "variance", "modeling", "financial reporting")
            "Sales Executive" -> listOf("leads", "CRM", "pipeline", "negotiation", "revenue")
            "Operations Analyst" -> listOf("workflow", "cost reduction", "efficiency", "process improvement", "root-cause analysis")
            "HR Executive" -> listOf("recruitment", "onboarding", "screening", "engagement", "interview coordination")
            "UX/UI Designer" -> listOf("user research", "wireframes", "prototypes", "usability", "design systems")
            "Project Manager" -> listOf("timeline", "risks", "stakeholders", "delivery", "scope management")
            else -> listOf("communication", "problem solving", "collaboration", "metrics", "stakeholder management")
        }
    }
}
