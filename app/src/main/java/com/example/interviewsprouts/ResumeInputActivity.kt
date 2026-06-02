package com.example.interviewsprouts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class ResumeInputActivity : AppCompatActivity() {

    private lateinit var editResumeText: EditText

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No PDF selected.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val extractedText = extractTextFromPdf(uri)

                if (extractedText.isBlank()) {
                    Toast.makeText(
                        this,
                        "No readable text found. This may be a scanned PDF.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    editResumeText.setText(extractedText)
                    Toast.makeText(
                        this,
                        "PDF text extracted successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Could not read PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)

        setContentView(R.layout.activity_resume_input)

        editResumeText = findViewById(R.id.editResumeText)

        val editTargetRole = findViewById<EditText>(R.id.editTargetRole)
        val editExperienceLevel = findViewById<EditText>(R.id.editExperienceLevel)
        val btnAnalyzeResume = findViewById<Button>(R.id.btnAnalyzeResume)
        val btnUploadPdf = findViewById<Button>(R.id.btnUploadPdf)

        btnUploadPdf.setOnClickListener {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }

        btnAnalyzeResume.setOnClickListener {
            val resumeText = editResumeText.text.toString().trim()
            val targetRole = editTargetRole.text.toString().trim()
            val experienceLevel = editExperienceLevel.text.toString().trim()

            if (resumeText.length < 50) {
                Toast.makeText(
                    this,
                    "Please paste or upload more resume text before analyzing.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val finalTargetRole = if (targetRole.isBlank()) {
                "Software Engineer"
            } else {
                targetRole
            }

            val finalExperienceLevel = if (experienceLevel.isBlank()) {
                "Fresh Graduate"
            } else {
                experienceLevel
            }

            val intent = Intent(this, ResumeReportActivity::class.java)
            intent.putExtra("resume_text", resumeText)
            intent.putExtra("target_role", finalTargetRole)
            intent.putExtra("experience_level", finalExperienceLevel)
            startActivity(intent)
        }
    }

    private fun extractTextFromPdf(uri: Uri): String {
        contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                throw IllegalArgumentException("Unable to open PDF file.")
            }

            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                return stripper.getText(document).trim()
            }
        }
    }
}