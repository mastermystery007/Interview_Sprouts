package com.example.interviewsprouts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class ResumeInputActivity : AppCompatActivity() {

    private var extractedResumeText: String = ""

    private lateinit var textAttachedFile: TextView
    private lateinit var textExtractionStatus: TextView
    private lateinit var spinnerTargetRole: Spinner

    private val professionList = listOf(
        "Software Engineer",
        "Android Developer",
        "iOS Developer",
        "Backend Developer",
        "Frontend Developer",
        "Full-Stack Developer",
        "Machine Learning Engineer",
        "AI Engineer",
        "Data Engineer",
        "Data Scientist",
        "Data Analyst",
        "DevOps Engineer",
        "Site Reliability Engineer (SRE)",
        "QA Automation Engineer / SDET",
        "Embedded Systems Engineer",
        "Cybersecurity Analyst",
        "Game Developer",
        "Business Analyst",
        "Product Manager",
        "Project Manager",
        "UX/UI Designer",
        "Finance Analyst",
        "Operations Analyst",
        "Sales Executive",
        "Business Development Executive",
        "Marketing Executive",
        "Digital Marketing Specialist",
        "HR Executive",
        "Recruiter / Talent Acquisition Specialist",
        "Research Assistant / Researcher",
        "Other / General"
    )

    private val experienceLevelList = listOf(
        "Student",
        "Internship Applicant",
        "Fresh Graduate",
        "0-1 Years",
        "1-2 Years",
        "2-4 Years",
        "4-7 Years",
        "7+ Years",
        "Career Switcher"
    )

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No PDF selected.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val fileName = getFileName(uri) ?: "Selected resume PDF"
                textAttachedFile.text = "Attached file: $fileName"

                val text = extractTextFromPdf(uri)
                extractedResumeText = text

                if (text.isBlank()) {
                    textExtractionStatus.text =
                        "No readable text found. This may be a scanned/image PDF."
                    Toast.makeText(
                        this,
                        "No readable text found. Try another PDF.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    textExtractionStatus.text =
                        "Resume text extracted successfully. Ready for analysis."
                    Toast.makeText(
                        this,
                        "PDF attached and text extracted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                extractedResumeText = ""
                textExtractionStatus.text = "Could not read PDF. Try another file."
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

        textAttachedFile = findViewById(R.id.textAttachedFile)
        textExtractionStatus = findViewById(R.id.textExtractionStatus)
        spinnerTargetRole = findViewById(R.id.spinnerTargetRole)

        val spinnerExperienceLevel = findViewById<Spinner>(R.id.spinnerExperienceLevel)
        val editJobSpecification = findViewById<EditText>(R.id.editJobSpecification)
        val btnAnalyzeResume = findViewById<Button>(R.id.btnAnalyzeResume)
        val btnUploadPdf = findViewById<Button>(R.id.btnUploadPdf)

        val professionAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            professionList
        )

        professionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTargetRole.adapter = professionAdapter

        val experienceAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            experienceLevelList
        )

        experienceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerExperienceLevel.adapter = experienceAdapter

        btnUploadPdf.setOnClickListener {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }

        btnAnalyzeResume.setOnClickListener {
            val resumeText = extractedResumeText.trim()
            val targetRole = spinnerTargetRole.selectedItem.toString()
            val experienceLevel = spinnerExperienceLevel.selectedItem.toString()
            val jobSpecification = editJobSpecification.text.toString().trim()

            if (resumeText.length < 50) {
                Toast.makeText(
                    this,
                    "Please upload a readable resume PDF before analyzing.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ResumeReportActivity::class.java)
            intent.putExtra("resume_text", resumeText)
            intent.putExtra("target_role", targetRole)
            intent.putExtra("experience_level", experienceLevel)
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