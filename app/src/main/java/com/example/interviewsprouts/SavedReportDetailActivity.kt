package com.example.interviewsprouts

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SavedReportDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_report_detail)
        findViewById<TextView>(R.id.btnBackSavedReportDetail).setOnClickListener { finish() }

        val meta = findViewById<TextView>(R.id.textSavedReportDetailMeta)
        val basic = findViewById<TextView>(R.id.textSavedReportDetailBasic)
        val full = findViewById<TextView>(R.id.textSavedReportDetailFull)
        val note = findViewById<TextView>(R.id.textSavedReportDetailNote)
        val report = try {
            JSONObject(intent.getStringExtra("report_json") ?: "")
        } catch (_: Exception) {
            null
        }

        if (report == null) {
            meta.text = "Could not open this saved report."
            basic.text = ""
            full.text = ""
            findViewById<Button>(R.id.btnCopySavedReport).isEnabled = false
            findViewById<Button>(R.id.btnShareSavedReport).isEnabled = false
            findViewById<Button>(R.id.btnExportSavedReportPdf).isEnabled = false
            return
        }

        val score = report.optInt("overall_score", 0)
        val rating = report.optString("rating_label").ifBlank {
            SavedReportsActivity.ratingForScore(score)
        }
        val targetRole = report.optString("target_role", "Target role not saved")
        val metaText = buildString {
            appendLine("Saved: ${report.optString("timestamp", "Date not saved")}")
            appendLine("Target role: $targetRole")
            appendLine("Experience level: ${report.optString("experience_level", "Experience not saved")}")
            appendLine("Overall score: $score/100")
            appendLine("Rating: $rating")
            append(report.optString("jd_status", "JD status not saved."))
        }
        val basicText =
            "Quick Review\n\n${report.optString("basic_feedback", "Basic feedback not available.")}"
        val fullText =
            "Detailed Analysis\n\n${report.optString("full_report", "Detailed report not available.")}"
        val shareableText = """
Resume Refine Report

$metaText

$basicText

$fullText
        """.trimIndent()

        meta.text = metaText
        basic.text = basicText
        full.text = fullText
        note.text = report.optString(
            "local_only_note",
            "Saved reports are stored locally on this device only."
        )

        findViewById<Button>(R.id.btnCopySavedReport).setOnClickListener {
            ReportShareUtils.copyText(this, "Resume Refine report", shareableText)
            Toast.makeText(this, "Report copied.", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnShareSavedReport).setOnClickListener {
            ReportShareUtils.shareText(
                this,
                "Share saved report",
                "Resume Refine report for $targetRole",
                shareableText
            )
        }
        findViewById<Button>(R.id.btnExportSavedReportPdf).setOnClickListener {
            ReportShareUtils.exportPdf(
                this,
                "Resume_Refine_${targetRole}_$score",
                "Resume Refine Report",
                shareableText
            )
        }
    }
}
