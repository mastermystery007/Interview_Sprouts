package com.example.interviewsprouts

import android.os.Bundle
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

        val bulletInput = findViewById<EditText>(R.id.editWeakBullet)
        val professionSpinner = findViewById<Spinner>(R.id.spinnerProfession)
        val rewriteButton = findViewById<Button>(R.id.btnRewriteBullet)
        val outputText = findViewById<TextView>(R.id.textRewriteOutput)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            professions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        professionSpinner.adapter = adapter

        rewriteButton.setOnClickListener {
            val weakBullet = bulletInput.text.toString().trim()
            if (weakBullet.length < 10) {
                bulletInput.error = "Enter a bullet with at least 10 characters."
                outputText.text = "Please enter a resume bullet with at least 10 characters."
                return@setOnClickListener
            }

            val profession = professionSpinner.selectedItem.toString()
            outputText.text = buildRewriteOutput(weakBullet, profession)
        }
    }

    private fun buildRewriteOutput(input: String, profession: String): String {
        val profile = profileForProfession(profession)
        val cleanedInput = input
            .removePrefix("•")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return """
            Improved Bullet:
            • ${profile.primaryVerb} $cleanedInput while applying ${profile.focusPhrase} to improve ${profile.outcomePhrase}.

            Metric Version:
            • ${profile.metricVerb} $cleanedInput, contributing to measurable gains such as [X% improvement], [Y hours saved], or [Z stakeholders/users/customers supported] through ${profile.metricPhrase}.

            Suggested Action Verbs:
            • ${profile.actionVerbs.joinToString(", ")}

            Truth Warning:
            Only use metrics and responsibilities that are true.
        """.trimIndent()
    }

    private fun profileForProfession(profession: String): RewriteProfile {
        return when (profession) {
            "Software Engineer" -> RewriteProfile(
                primaryVerb = "Developed and implemented",
                metricVerb = "Optimized",
                focusPhrase = "API, system, and performance best practices",
                outcomePhrase = "reliability, scalability, and user experience",
                metricPhrase = "performance tuning and system improvements",
                actionVerbs = listOf("Developed", "Implemented", "Optimized", "Integrated", "Automated")
            )

            "Data Analyst" -> RewriteProfile(
                primaryVerb = "Analyzed and translated",
                metricVerb = "Built dashboard-driven insights from",
                focusPhrase = "SQL/Excel/Python analysis, dashboards, insights, and trends",
                outcomePhrase = "decision-making and reporting accuracy",
                metricPhrase = "trend analysis and stakeholder reporting",
                actionVerbs = listOf("Analyzed", "Visualized", "Reported", "Identified", "Automated")
            )

            "Business Analyst" -> RewriteProfile(
                primaryVerb = "Documented and improved",
                metricVerb = "Streamlined",
                focusPhrase = "requirements gathering, stakeholder alignment, process gaps, UAT, and workflows",
                outcomePhrase = "business clarity and delivery readiness",
                metricPhrase = "workflow mapping and UAT coordination",
                actionVerbs = listOf("Gathered", "Documented", "Mapped", "Validated", "Facilitated")
            )

            "Marketing Executive", "Digital Marketing Specialist" -> RewriteProfile(
                primaryVerb = "Executed and improved",
                metricVerb = "Increased campaign impact for",
                focusPhrase = "campaign optimization, conversion, CTR, leads, and brand awareness",
                outcomePhrase = "audience engagement and marketing performance",
                metricPhrase = "conversion tracking and lead-generation improvements",
                actionVerbs = listOf("Launched", "Optimized", "Converted", "Generated", "Promoted")
            )

            "Product Manager" -> RewriteProfile(
                primaryVerb = "Prioritized and shaped",
                metricVerb = "Improved product outcomes for",
                focusPhrase = "roadmap planning, user feedback, prioritization, and feature planning",
                outcomePhrase = "customer value and product alignment",
                metricPhrase = "roadmap tradeoffs and user feedback analysis",
                actionVerbs = listOf("Prioritized", "Defined", "Launched", "Validated", "Coordinated")
            )

            "Finance Analyst" -> RewriteProfile(
                primaryVerb = "Performed financial analysis for",
                metricVerb = "Improved forecasting visibility for",
                focusPhrase = "forecasting, variance analysis, cost control, and revenue insights",
                outcomePhrase = "budget accuracy and financial planning",
                metricPhrase = "variance tracking and cost/revenue analysis",
                actionVerbs = listOf("Forecasted", "Analyzed", "Modeled", "Reconciled", "Reported")
            )

            "Sales Executive" -> RewriteProfile(
                primaryVerb = "Managed and advanced",
                metricVerb = "Expanded pipeline results for",
                focusPhrase = "leads, CRM updates, pipeline tracking, client follow-up, and revenue growth",
                outcomePhrase = "client relationships and sales opportunities",
                metricPhrase = "CRM discipline and pipeline management",
                actionVerbs = listOf("Prospected", "Negotiated", "Closed", "Generated", "Managed")
            )

            "Operations Analyst" -> RewriteProfile(
                primaryVerb = "Improved and monitored",
                metricVerb = "Reduced operational friction in",
                focusPhrase = "process improvement, workflow analysis, cost reduction, and efficiency",
                outcomePhrase = "operational consistency and team productivity",
                metricPhrase = "workflow optimization and efficiency tracking",
                actionVerbs = listOf("Improved", "Streamlined", "Reduced", "Coordinated", "Measured")
            )

            "HR Executive" -> RewriteProfile(
                primaryVerb = "Supported and strengthened",
                metricVerb = "Improved people operations for",
                focusPhrase = "recruitment, onboarding, employee engagement, and HR coordination",
                outcomePhrase = "candidate experience and employee satisfaction",
                metricPhrase = "hiring process and onboarding improvements",
                actionVerbs = listOf("Recruited", "Onboarded", "Coordinated", "Engaged", "Supported")
            )

            "UX/UI Designer" -> RewriteProfile(
                primaryVerb = "Designed and refined",
                metricVerb = "Improved usability outcomes for",
                focusPhrase = "user research, wireframes, prototypes, and usability testing",
                outcomePhrase = "user flows, accessibility, and product usability",
                metricPhrase = "prototype iteration and usability feedback",
                actionVerbs = listOf("Designed", "Researched", "Prototyped", "Tested", "Iterated")
            )

            "Project Manager" -> RewriteProfile(
                primaryVerb = "Coordinated and delivered",
                metricVerb = "Improved delivery predictability for",
                focusPhrase = "timelines, stakeholder communication, delivery planning, and risk management",
                outcomePhrase = "on-time execution and cross-functional alignment",
                metricPhrase = "timeline tracking and risk mitigation",
                actionVerbs = listOf("Delivered", "Coordinated", "Planned", "Mitigated", "Aligned")
            )

            else -> RewriteProfile(
                primaryVerb = "Improved",
                metricVerb = "Strengthened",
                focusPhrase = "clear ownership, collaboration, and measurable outcomes",
                outcomePhrase = "team results",
                metricPhrase = "structured execution",
                actionVerbs = listOf("Improved", "Led", "Delivered", "Coordinated", "Supported")
            )
        }
    }

    private data class RewriteProfile(
        val primaryVerb: String,
        val metricVerb: String,
        val focusPhrase: String,
        val outcomePhrase: String,
        val metricPhrase: String,
        val actionVerbs: List<String>
    )
}
