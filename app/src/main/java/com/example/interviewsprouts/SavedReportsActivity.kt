package com.example.interviewsprouts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class SavedReportsActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_reports)

        container = findViewById(R.id.savedReportsContainer)
        emptyText = findViewById(R.id.textSavedReportsEmpty)

        findViewById<Button>(R.id.btnClearSavedReports).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear saved reports?")
                .setMessage("This removes all saved reports stored on this device.")
                .setPositiveButton("Clear") { _, _ ->
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(KEY_REPORTS).apply()
                    refreshReports()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        refreshReports()
    }

    private fun refreshReports() {
        container.removeAllViews()
        val reports = loadReportsSafely()
        if (reports.length() == 0) {
            emptyText.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        emptyText.visibility = View.GONE
        container.visibility = View.VISIBLE
        var displayPosition = 1

        for (
            index in reports.length() - 1
                downTo 0
        ) {
            val report =
                reports.optJSONObject(index)
                    ?: continue

            container.addView(
                createReportCard(
                    report,
                    displayPosition,
                    index
                )
            )

            displayPosition += 1
        }
        if (container.childCount == 0) emptyText.visibility = View.VISIBLE
    }

    private fun loadReportsSafely(): JSONArray {
        val rawJson = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_REPORTS, "[]") ?: "[]"
        return try { JSONArray(rawJson) } catch (_: Exception) { JSONArray() }
    }

    private fun createReportCard(
        report: JSONObject,
        displayPosition: Int,
        sourceIndex: Int
    ): View {
        val score =
            report.optInt("overall_score", 0)

        val rating = report
            .optString("rating_label")
            .ifBlank {
                ratingForScore(score)
            }

        val targetRole = report.optString(
            "target_role",
            "Target role not saved"
        )

        val rawTimestamp = report.optString(
            "timestamp",
            "Date not saved"
        )

        val formattedDate =
            formatSavedDate(rawTimestamp)

        val displayNumber =
            displayPosition
                .toString()
                .padStart(3, '0')

        val title =
            "Report $displayNumber — " +
                "$targetRole — " +
                "$score% — " +
                formattedDate

        val preview = report
            .optString(
                "basic_feedback",
                "Saved report preview unavailable."
            )
            .lineSequence()
            .map {
                it.trim()
                    .trimStart('•')
                    .trim()
            }
            .firstOrNull {
                it.isNotBlank() &&
                    !it.equals(
                        "Overview",
                        ignoreCase = true
                    )
            }
            ?: "Saved report preview unavailable."

        val card = TextView(this).apply {
            text = buildString {
                appendLine(title)

                appendLine(
                    "Experience: ${
                        report.optString(
                            "experience_level",
                            "Experience not saved"
                        )
                    }"
                )

                appendLine("Rating: $rating")

                report
                    .optString("jd_status")
                    .takeIf { it.isNotBlank() }
                    ?.let { appendLine(it) }

                appendLine()
                appendLine(preview)
                append("Tap to view full report")
            }

            setTextColor(0xFF111111.toInt())
            textSize = 15f
            setLineSpacing(
                dp(3).toFloat(),
                1f
            )
            setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
            )

            setBackgroundResource(
                R.drawable.bg_saved_report_card
            )

            isClickable = true
            isFocusable = true

            foreground = obtainStyledAttributes(
                intArrayOf(
                    android.R.attr
                        .selectableItemBackground
                )
            ).let { attributes ->
                val drawable =
                    attributes.getDrawable(0)

                attributes.recycle()
                drawable
            }

            setOnClickListener {
                startActivity(
                    Intent(
                        this@SavedReportsActivity,
                        SavedReportDetailActivity::class.java
                    ).putExtra(
                        "report_json",
                        report.toString()
                    )
                )
            }
        }

        val deleteButton = Button(this).apply {
            text = "Delete Report"
            setOnClickListener {
                AlertDialog.Builder(this@SavedReportsActivity)
                    .setTitle("Delete saved report?")
                    .setMessage("This removes only this saved report from this device.")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteSavedReportAt(sourceIndex)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                LinearLayout.LayoutParams
                    .WRAP_CONTENT
            )

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(card)
            addView(deleteButton)
        }

        cardContainer.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }

        return cardContainer
    }

    private fun deleteSavedReportAt(
        sourceIndex: Int
    ) {
        val reports =
            loadReportsSafely()

        val rebuilt =
            JSONArray()

        for (index in 0 until reports.length()) {
            if (index != sourceIndex) {
                reports.optJSONObject(index)?.let {
                    rebuilt.put(it)
                }
            }
        }

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).edit()
            .putString(
                KEY_REPORTS,
                rebuilt.toString()
            )
            .apply()

        refreshReports()
    }

    private fun formatSavedDate(
        rawTimestamp: String
    ): String {
        if (
            rawTimestamp.isBlank() ||
            rawTimestamp == "Date not saved"
        ) {
            return "Date not saved"
        }

        val inputFormat =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.US
            )

        val outputFormat =
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.US
            )

        return try {
            val parsed =
                inputFormat.parse(rawTimestamp)

            if (parsed == null) {
                rawTimestamp
            } else {
                outputFormat.format(parsed)
            }
        } catch (_: Exception) {
            rawTimestamp
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS_NAME = "saved_resume_reports"
        const val KEY_REPORTS = "reports_json"
        fun ratingForScore(score: Int): String = when {
            score >= 85 -> "Excellent"
            score >= 70 -> "Good"
            score >= 50 -> "Needs Improvement"
            else -> "Lacking"
        }
    }
}
