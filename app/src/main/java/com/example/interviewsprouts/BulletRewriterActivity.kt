package com.example.interviewsprouts

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
        val bulletSourceStatus = findViewById<TextView>(R.id.textBulletSourceStatus)
        val resumeBulletSpinner = findViewById<Spinner>(R.id.spinnerResumeBullets)
        val selectedFullBulletText = findViewById<TextView>(R.id.textSelectedFullBullet)
        val bulletInput = findViewById<EditText>(R.id.editWeakBullet)
        val professionLabel = findViewById<TextView>(R.id.textProfessionLabel)
        val professionSpinner = findViewById<Spinner>(R.id.spinnerProfession)
        val rewriteButton = findViewById<Button>(R.id.btnRewriteBullet)
        val outputText = findViewById<TextView>(R.id.textRewriteOutput)

        val professionAdapter = ArrayAdapter(this, R.layout.spinner_item, professions)
        professionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        professionSpinner.adapter = professionAdapter

        val selectedRoleIndex = professions.indexOfFirst { it.equals(targetRole, ignoreCase = true) }
        if (targetRole.isNotBlank()) {
            professionLabel.visibility = View.GONE
            professionSpinner.visibility = View.GONE
            if (selectedRoleIndex >= 0) {
                professionSpinner.setSelection(selectedRoleIndex)
            }
        } else {
            professionLabel.visibility = View.VISIBLE
            professionSpinner.visibility = View.VISIBLE
        }

        val extractedBullets = extractCandidateBullets(resumeText)
        if (extractedBullets.isNotEmpty()) {
            val bulletPreviews = extractedBullets.map { bullet -> shortBulletPreview(bullet) }
            val bulletAdapter = ArrayAdapter(this, R.layout.spinner_item, bulletPreviews)
            bulletAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            resumeBulletSpinner.adapter = bulletAdapter
            resumeBulletSpinner.visibility = View.VISIBLE
            selectedFullBulletText.visibility = View.VISIBLE
            selectedFullBulletText.text = "Selected full bullet:\n• ${extractedBullets.first()}"
            bulletInput.visibility = View.VISIBLE
            bulletInput.hint = "Optional: paste/edit a bullet manually to override selected bullet"
            bulletSourceStatus.text =
                "Found resume bullets. Select a shortened preview below; the full selected bullet is shown underneath."
            resumeBulletSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedFullBulletText.text = "Selected full bullet:\n• ${extractedBullets[position]}"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        } else {
            resumeBulletSpinner.visibility = View.GONE
            selectedFullBulletText.visibility = View.GONE
            bulletInput.visibility = View.VISIBLE
            bulletSourceStatus.text = "No clear resume bullets detected. Paste a bullet manually."
        }

        rewriteButton.setOnClickListener {
            val manualBullet = bulletInput.text.toString().trim()
            val selectedBullet = if (extractedBullets.isNotEmpty()) {
                extractedBullets.getOrNull(resumeBulletSpinner.selectedItemPosition)?.trim() ?: ""
            } else {
                ""
            }
            val weakBullet = manualBullet.ifBlank { selectedBullet }

            if (weakBullet.isBlank()) {
                bulletInput.error = "Paste a resume bullet to rewrite."
                outputText.text = "Please paste a resume bullet to rewrite."
                return@setOnClickListener
            }

            if (weakBullet.length < 10) {
                bulletInput.error = "Enter a bullet with at least 10 characters."
                outputText.text = "Please enter or select a resume bullet with at least 10 characters."
                return@setOnClickListener
            }

            bulletInput.error = null
            val profession = targetRole.ifBlank { professionSpinner.selectedItem.toString() }
            outputText.text = buildRewriteOutput(weakBullet, profession)
        }
    }

    private fun extractCandidateBullets(resumeText: String): List<String> {
        val actionWords = listOf(
            "worked", "built", "developed", "analyzed", "managed", "created", "implemented",
            "improved", "designed", "coordinated", "led", "generated", "optimized",
            "handled", "supported", "assisted", "documented", "launched"
        )
        val seen = linkedSetOf<String>()

        resumeText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val startsLikeBullet = line.startsWith("-") || line.startsWith("•") || line.startsWith("*")
                val words = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val hasActionWord = actionWords.any { actionWord ->
                    line.contains(Regex("""\b${Regex.escape(actionWord)}\b""", RegexOption.IGNORE_CASE))
                }

                if (startsLikeBullet || (words.size in 5..35 && hasActionWord)) {
                    seen.add(line.removePrefix("-").removePrefix("•").removePrefix("*").trim())
                }
            }

        return seen.take(20)
    }

    private fun shortBulletPreview(bullet: String): String {
        return if (bullet.length <= 72) {
            bullet
        } else {
            "${bullet.take(69).trimEnd()}..."
        }
    }

    private fun buildRewriteOutput(input: String, profession: String): String {
        val profile = profileForProfession(profession)
        val cleanedInput = cleanBulletTask(input)
        val suggestedVerbs = profile.actionVerbs.distinct().joinToString("\n") { "• $it" }

        return """
            Original Bullet:
            • ${input.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim()}

            Improved Bullet:
            • ${profile.primaryVerb} $cleanedInput to improve ${profile.outcomePhrase}.

            Metric Version:
            • ${profile.metricVerb} $cleanedInput, contributing to measurable gains such as ${profile.metricPlaceholders}.

            Suggested Action Verbs:
            $suggestedVerbs

            Truth Warning:
            Only use metrics and responsibilities that are true. Replace placeholders like [X%], [number], and [hours] only with real details you can explain in an interview.
        """.trimIndent()
    }

    private fun cleanBulletTask(input: String): String {
        var cleaned = input
            .trim()
            .removePrefix("•")
            .removePrefix("-")
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
            "contributed to",
            "developed",
            "optimized",
            "analyzed",
            "automated",
            "documented",
            "streamlined",
            "managed",
            "prioritized",
            "improved",
            "increased",
            "reduced",
            "designed",
            "coordinated"
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

    private fun profileForProfession(profession: String): RewriteProfile {
        val roleLower = profession.lowercase()
        return when {
            roleLower.contains("software engineer") -> RewriteProfile("Developed", "Optimized", "reliability, scalability, and user experience", "[X% improvement], [number] users/features, or [hours] saved", listOf("Developed", "Implemented", "Debugged", "Tested", "Optimized"))
            roleLower.contains("data analyst") -> RewriteProfile("Analyzed", "Automated", "reporting accuracy, KPI visibility, and business insights", "[X% faster reporting], [number] dashboards, or [hours] saved", listOf("Analyzed", "Cleaned", "Visualized", "Reported", "Automated"))
            roleLower.contains("business analyst") -> RewriteProfile("Documented", "Streamlined", "stakeholder alignment, requirement clarity, and process efficiency", "[X% fewer clarification cycles], [number] stakeholders, or [hours] saved", listOf("Gathered", "Documented", "Mapped", "Validated", "Streamlined"))
            roleLower.contains("marketing") || roleLower.contains("digital marketing") -> RewriteProfile("Managed", "Optimized", "campaign performance, engagement, and lead generation", "[X% higher CTR], [number] leads, or [X% lower CPC]", listOf("Managed", "Launched", "Optimized", "Generated", "Converted"))
            roleLower.contains("product manager") -> RewriteProfile("Prioritized", "Improved", "product clarity, roadmap alignment, and user value", "[number] users, [X% adoption], or [number] features shipped", listOf("Prioritized", "Defined", "Launched", "Validated", "Improved"))
            roleLower.contains("finance analyst") -> RewriteProfile("Analyzed", "Improved", "forecasting accuracy, budgeting clarity, and financial decision-making", "[X% variance reduction], [amount] cost visibility, or [hours] saved", listOf("Analyzed", "Forecasted", "Modeled", "Reconciled", "Improved"))
            roleLower.contains("sales executive") -> RewriteProfile("Managed", "Increased", "pipeline quality, client engagement, and revenue opportunities", "[number] leads, [X% conversion], or [amount] pipeline value", listOf("Managed", "Prospected", "Negotiated", "Generated", "Increased"))
            roleLower.contains("operations analyst") -> RewriteProfile("Improved", "Reduced", "workflow efficiency, process consistency, and operational productivity", "[X% cost reduction], [hours] saved, or [number] processes improved", listOf("Improved", "Streamlined", "Reduced", "Coordinated", "Measured"))
            roleLower.contains("hr executive") -> RewriteProfile("Supported", "Improved", "candidate experience, onboarding quality, and HR operations", "[X% faster hiring], [number] candidates, or [hours] saved", listOf("Supported", "Recruited", "Onboarded", "Screened", "Improved"))
            roleLower.contains("ux") || roleLower.contains("ui") -> RewriteProfile("Designed", "Improved", "usability, user flows, and product experience", "[X% task completion], [number] screens, or [number] users tested", listOf("Designed", "Researched", "Prototyped", "Tested", "Improved"))
            roleLower.contains("project manager") -> RewriteProfile("Coordinated", "Improved", "delivery predictability, stakeholder alignment, and risk visibility", "[number] milestones, [X% on-time delivery], or [hours] saved", listOf("Coordinated", "Delivered", "Planned", "Mitigated", "Improved"))
            else -> RewriteProfile("Improved", "Strengthened", "clarity, execution, and measurable team outcomes", "[X% improvement], [number] outcomes, or [hours] saved", listOf("Improved", "Led", "Delivered", "Coordinated", "Strengthened"))
        }
    }

    private data class RewriteProfile(
        val primaryVerb: String,
        val metricVerb: String,
        val outcomePhrase: String,
        val metricPlaceholders: String,
        val actionVerbs: List<String>
    )
}
