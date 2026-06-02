package com.example.interviewsprouts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class ResumeInputActivity : AppCompatActivity() {

    private lateinit var editResumeText: EditText
    private lateinit var textAttachedFile: TextView
    private lateinit var textExtractionStatus: TextView

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No PDF selected.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val fileName = getFileName(uri) ?: "Selected resume PDF"
                textAttachedFile.text = "Attached file: $fileName"

                val extractedText = extractTextFromPdf(uri)

                if (extractedText.isBlank()) {
                    textExtractionStatus.text =
                        "No readable text found. This may be a scanned/image PDF."
                    Toast.makeText(
                        this,
                        "No readable text found. Try pasting resume text manually.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    editResumeText.setText(extractedText)
                    textExtractionStatus.text =
                        "Text extracted successfully. You can preview/edit it before analysis."
                    Toast.makeText(
                        this,
                        "PDF text extracted successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                textExtractionStatus.text = "Could not read PDF. Try another file or paste text."
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
        textAttachedFile = findViewById(R.id.textAttachedFile)
        textExtractionStatus = findViewById(R.id.textExtractionStatus)

        val editTargetRole = findViewById<EditText>(R.id.editTargetRole)
        val editExperienceLevel = findViewById<EditText>(R.id.editExperienceLevel)
        val editJobSpecification = findViewById<EditText>(R.id.editJobSpecification)

        val btnAnalyzeResume = findViewById<Button>(R.id.btnAnalyzeResume)
        val btnUploadPdf = findViewById<Button>(R.id.btnUploadPdf)
        val btnToggleExtractedText = findViewById<Button>(R.id.btnToggleExtractedText)

        btnUploadPdf.setOnClickListener {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }

        btnToggleExtractedText.setOnClickListener {
            if (editResumeText.visibility == View.VISIBLE) {
                editResumeText.visibility = View.GONE
                btnToggleExtractedText.text = "Preview / Edit Resume Text"
            } else {
                editResumeText.visibility = View.VISIBLE
                btnToggleExtractedText.text = "Hide Resume Text"
            }
        }

        btnAnalyzeResume.setOnClickListener {
            val resumeText = editResumeText.text.toString().trim()
            val targetRole = editTargetRole.text.toString().trim()
            val experienceLevel = editExperienceLevel.text.toString().trim()
            val jobSpecification = editJobSpecification.text.toString().trim()

            if (resumeText.length < 50) {
                Toast.makeText(
                    this,
                    "Please paste or upload more resume text before analyzing.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val finalTargetRole = if (targetRole.isBlank()) {
                "General Job Applicant"
            } else {
                targetRole
            }

            val finalExperienceLevel = if (experienceLevel.isBlank()) {
                "Not specified"
            } else {
                experienceLevel
            }

            val intent = Intent(this, ResumeReportActivity::class.java)
            intent.putExtra("resume_text", resumeText)
            intent.putExtra("target_role", finalTargetRole)
            intent.putExtra("experience_level", finalExperienceLevel)
            intent.putExtra("job_specification", jobSpecification)
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

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }
}