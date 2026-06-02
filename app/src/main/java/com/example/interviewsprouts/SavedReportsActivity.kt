package com.example.interviewsprouts

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class SavedReportsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_reports)

        val reportsText = findViewById<TextView>(R.id.textSavedReportsList)
        val clearButton = findViewById<Button>(R.id.btnClearSavedReports)

        fun refreshReports() {
            reportsText.text = loadSavedReportsText()
        }

        clearButton.setOnClickListener {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_REPORTS)
                .apply()
            refreshReports()
        }

        refreshReports()
    }

    private fun loadSavedReportsText(): String {
        val rawJson = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_REPORTS, "[]") ?: "[]"
        val reports = JSONArray(rawJson)
        if (reports.length() == 0) return "No saved reports yet."

        val builder = StringBuilder()
        for (index in reports.length() - 1 downTo 0) {
            val report = reports.getJSONObject(index)
            builder.append("Report ${reports.length() - index}\n")
            builder.append("Saved: ${report.optString("timestamp")}\n")
            builder.append("Target Role: ${report.optString("target_role")}\n")
            builder.append("Experience: ${report.optString("experience_level")}\n")
            builder.append("Overall Score: ${report.optInt("overall_score")}/100\n\n")
            builder.append("Basic Feedback:\n${report.optString("basic_feedback")}\n\n")
            builder.append("Full Report:\n${report.optString("full_report")}\n")
            if (index != 0) builder.append("\n------------------------------\n\n")
        }
        return builder.toString()
    }

    companion object {
        const val PREFS_NAME = "saved_resume_reports"
        const val KEY_REPORTS = "reports_json"
    }
}
