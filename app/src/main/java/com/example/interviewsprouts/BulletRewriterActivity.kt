package com.example.interviewsprouts

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BulletRewriterActivity : AppCompatActivity() {

    private val professions = listOf(
        "Software Engineer",
        "Data Analyst",
        "Business Analyst",
        "Marketing Executive",
        "Digital Marketing Specialist",
        "Product Manager",
        "Finance Analyst",
        "Sales Executive",
        "Operations Analyst",
        "HR Executive",
        "UX/UI Designer",
        "Project Manager"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bullet_rewriter)

        val resumeText = intent.getStringExtra("resume_text") ?: ""
        val targetRole = intent.getStringExtra("target_role") ?: ""
        val sourceStatus = findViewById<TextView>(R.id.textBulletSourceStatus)
        val resumeBulletSpinner = findViewById<Spinner>(R.id.spinnerResumeBullets)
        val bulletInput = findViewById<EditText>(R.id.editWeakBullet)
        val professionSpinner = findViewById<Spinner>(R.id.spinnerProfession)
        val rewriteButton = findViewById<Button>(R.id.btnRewriteBullet)
        val outputText = findViewById<TextView>(R.id.textRewriteOutput)

        val professionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, professions)
        professionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        professionSpinner.adapter = professionAdapter

        val selectedRoleIndex = professions.indexOfFirst { it.equals(targetRole, ignoreCase = true) }
        if (selectedRoleIndex >= 0) {
            professionSpinner.setSelection(selectedRoleIndex)
        }

        val extractedBullets = extractCandidateBullets(resumeText)
        if (extractedBullets.isNotEmpty()) {
            sourceStatus.text = "Found ${extractedBullets.size} likely resume bullet(s). Select one below or edit manually if needed."
            resumeBulletSpinner.visibility = View.VISIBLE
            bulletInput.visibility = View.GONE
            val bulletAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, extractedBullets)
            bulletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            resumeBulletSpinner.adapter = bulletAdapter
        } else {
            sourceStatus.text = "No resume bullet was detected from the report flow. Paste a bullet manually below."
            resumeBulletSpinner.visibility = View.GONE
            bulletInput.visibility = View.VISIBLE
        }

        rewriteButton.setOnClickListener {
            val weakBullet = if (extractedBullets.isNotEmpty()) {
                resumeBulletSpinner.selectedItem?.toString()?.trim() ?: ""
            } else {
                bulletInput.text.toString().trim()
            }

            if (weakBullet.length < 10) {
                bulletInput.error = "Enter a bullet with at least 10 characters."
                outputText.text = "Please enter or select a resume bullet with at least 10 characters."
                return@setOnClickListener
            }

            val profession = professionSpinner.selectedItem.toString()
            outputText.text = buildRewriteOutput(weakBullet, profession)
        }
    }

    private fun extractCandidateBullets(resumeText: String): List<String> {
        val actionWords = listOf(
            "worked", "built", "developed", "analyzed", "managed", "created", "implemented",
            "improved", "designed", "coordinated", "led", "generated", "optimized", "handled", "supported"
        )
        val seen = linkedSetOf<String>()

        resumeText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val startsLikeBullet = line.startsWith("-") || line.startsWith("•") || line.startsWith("*")
                val words = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val hasActionWord = actionWords.any { actionWord ->
                    line.contains(Regex("\\b${Regex.escape(actionWord)}\\b", RegexOption.IGNORE_CASE))
                }

                if (startsLikeBullet || (words.size in 5..35 && hasActionWord)) {
                    seen.add(line.removePrefix("-").removePrefix("•").removePrefix("*").trim())
                }
            }

        return seen.take(20)
    }

    private fun buildRewriteOutput(input: String, profession: String): String {
        val profile = profileForProfession(profession)
        val cleanedInput = input
            .removePrefix("•")
            .removePrefix("-")
            .removePrefix("*")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return """
            Improved Bullet:
            • ${profile.primaryVerb} $cleanedInput while applying ${profile.focusPhrase} to improve ${profile.outcomePhrase}.

            Metric Version:
            • ${profile.metricVerb} $cleanedInput, contributing to measurable gains such as [X% improvement], [number] ${profile.metricNoun}, or [hours] saved through ${profile.metricPhrase}.

            Suggested Action Verbs:
            • ${profile.actionVerbs.joinToString(", ")}

            Truth Warning:
            Only use metrics, tools, responsibilities, and outcomes that are true. Replace placeholders like [X%], [number], and [hours] only with real details you can explain in an interview.
        """.trimIndent()
    }

    private fun profileForProfession(profession: String): RewriteProfile {
        return when (profession) {
            "Software Engineer" -> RewriteProfile("Developed and implemented", "Optimized", "API, debugging, testing, performance, and system design practices", "reliability, scalability, and user experience", "performance tuning and system improvements", "features/users/errors resolved", listOf("Developed", "Implemented", "Debugged", "Tested", "Optimized"))
            "Data Analyst" -> RewriteProfile("Analyzed and translated", "Built dashboard-driven insights from", "SQL, dashboarding, data cleaning, KPI tracking, and insight generation", "decision-making and reporting accuracy", "trend analysis and stakeholder reporting", "dashboards/reports/stakeholders supported", listOf("Analyzed", "Cleaned", "Visualized", "Reported", "Identified"))
            "Business Analyst" -> RewriteProfile("Documented and improved", "Streamlined", "requirements gathering, stakeholder alignment, UAT, and process gap analysis", "business clarity and delivery readiness", "workflow mapping and UAT coordination", "requirements/processes/stakeholders supported", listOf("Gathered", "Documented", "Mapped", "Validated", "Facilitated"))
            "Marketing Executive", "Digital Marketing Specialist" -> RewriteProfile("Executed and improved", "Increased campaign impact for", "campaign optimization, CTR, conversion, SEO, lead generation, and audience analysis", "audience engagement and marketing performance", "conversion tracking and lead-generation improvements", "campaigns/leads/conversions", listOf("Launched", "Optimized", "Converted", "Generated", "Promoted"))
            "Product Manager" -> RewriteProfile("Prioritized and shaped", "Improved product outcomes for", "roadmap planning, prioritization, user feedback, and product metrics", "customer value and product alignment", "roadmap tradeoffs and user feedback analysis", "features/users/stakeholders", listOf("Prioritized", "Defined", "Launched", "Validated", "Coordinated"))
            "Finance Analyst" -> RewriteProfile("Performed financial analysis for", "Improved forecasting visibility for", "forecasting, budgeting, variance analysis, and financial modeling", "budget accuracy and financial planning", "variance tracking and cost/revenue analysis", "models/reports/business units", listOf("Forecasted", "Analyzed", "Modeled", "Reconciled", "Reported"))
            "Sales Executive" -> RewriteProfile("Managed and advanced", "Expanded pipeline results for", "lead generation, CRM updates, pipeline tracking, negotiation, and revenue growth", "client relationships and sales opportunities", "CRM discipline and pipeline management", "leads/accounts/opportunities", listOf("Prospected", "Negotiated", "Closed", "Generated", "Managed"))
            "Operations Analyst" -> RewriteProfile("Improved and monitored", "Reduced operational friction in", "workflow analysis, cost reduction, efficiency tracking, and process improvement", "operational consistency and team productivity", "workflow optimization and efficiency tracking", "workflows/processes/teams", listOf("Improved", "Streamlined", "Reduced", "Coordinated", "Measured"))
            "HR Executive" -> RewriteProfile("Supported and strengthened", "Improved people operations for", "recruitment, onboarding, screening, employee engagement, and HR coordination", "candidate experience and employee satisfaction", "hiring process and onboarding improvements", "candidates/employees/roles", listOf("Recruited", "Onboarded", "Screened", "Engaged", "Supported"))
            "UX/UI Designer" -> RewriteProfile("Designed and refined", "Improved usability outcomes for", "user research, wireframes, prototypes, and usability testing", "user flows, accessibility, and product usability", "prototype iteration and usability feedback", "screens/prototypes/users tested", listOf("Designed", "Researched", "Prototyped", "Tested", "Iterated"))
            "Project Manager" -> RewriteProfile("Coordinated and delivered", "Improved delivery predictability for", "timelines, risk management, stakeholder communication, and delivery planning", "on-time execution and cross-functional alignment", "timeline tracking and risk mitigation", "milestones/risks/stakeholders", listOf("Delivered", "Coordinated", "Planned", "Mitigated", "Aligned"))
            else -> RewriteProfile("Improved", "Strengthened", "clear ownership, collaboration, and measurable outcomes", "team results", "structured execution", "tasks/stakeholders/outcomes", listOf("Improved", "Led", "Delivered", "Coordinated", "Supported"))
        }
    }

    private data class RewriteProfile(
        val primaryVerb: String,
        val metricVerb: String,
        val focusPhrase: String,
        val outcomePhrase: String,
        val metricPhrase: String,
        val metricNoun: String,
        val actionVerbs: List<String>
    )
}
