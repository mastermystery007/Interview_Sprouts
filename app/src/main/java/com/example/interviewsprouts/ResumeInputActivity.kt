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
import java.util.zip.ZipInputStream

class ResumeInputActivity : AppCompatActivity() {
    private var extractedResumeText: String = ""

    private lateinit var textAttachedFile: TextView
    private lateinit var textExtractionStatus: TextView
    private lateinit var spinnerTargetRole: Spinner
    private lateinit var editResumeText: EditText

    private val professionList = listOf(
        "Software Engineer",
        "Android Developer",
        "iOS Developer",
        "Backend Software Engineer",
        "Frontend Software Engineer",
        "Full-Stack Developer",
        "Machine Learning Engineer",
        "AI Engineer",
        "Data Engineer",
        "Data Scientist",
        "Data Analyst",
        "DevOps Engineer",
        "Site Reliability Engineer (SRE)",
        "QA Automation Engineer / SDET",
        "Embedded Software Engineer",
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
        "General Job Applicant"
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

    private val documentPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No resume selected.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val fileName = getFileName(uri) ?: "Selected resume"
                textAttachedFile.text = "Attached file: $fileName"
                val text = extractTextFromDocument(uri, fileName)
                extractedResumeText = text
                editResumeText.setText(text)

                if (text.isBlank()) {
                    textExtractionStatus.text =
                        "No readable text found. Paste your resume text below or try another file."
                    Toast.makeText(this, "No readable text found.", Toast.LENGTH_LONG).show()
                } else {
                    textExtractionStatus.text =
                        "Resume text extracted. Review or edit it below before analysis."
                    Toast.makeText(this, "Resume attached and text extracted.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                extractedResumeText = ""
                textExtractionStatus.text =
                    "Could not read this file. Paste the resume text below or try another PDF/DOCX."
                Toast.makeText(this, "Could not read resume: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContentView(R.layout.activity_resume_input)

        textAttachedFile = findViewById(R.id.textAttachedFile)
        textExtractionStatus = findViewById(R.id.textExtractionStatus)
        spinnerTargetRole = findViewById(R.id.spinnerTargetRole)
        editResumeText = findViewById(R.id.editResumeText)

        val spinnerExperienceLevel = findViewById<Spinner>(R.id.spinnerExperienceLevel)
        val editJobSpecification = findViewById<EditText>(R.id.editJobSpecification)
        val btnAnalyzeResume = findViewById<Button>(R.id.btnAnalyzeResume)
        val btnUploadResume = findViewById<Button>(R.id.btnUploadPdf)

        val professionAdapter = ArrayAdapter(this, R.layout.spinner_item, professionList)
        professionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTargetRole.adapter = professionAdapter

        val experienceAdapter = ArrayAdapter(this, R.layout.spinner_item, experienceLevelList)
        experienceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerExperienceLevel.adapter = experienceAdapter

        btnUploadResume.setOnClickListener {
            documentPickerLauncher.launch(arrayOf(MIME_PDF, MIME_DOCX))
        }

        btnAnalyzeResume.setOnClickListener {
            val resumeText = editResumeText.text.toString().trim().ifBlank {
                extractedResumeText.trim()
            }
            val targetRole = spinnerTargetRole.selectedItem.toString()
            val experienceLevel = spinnerExperienceLevel.selectedItem.toString()
            val jobSpecification = editJobSpecification.text.toString().trim()

            if (resumeText.length < 50) {
                Toast.makeText(
                    this,
                    "Upload a readable PDF/DOCX or paste at least a few resume lines.",
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

    private fun extractTextFromDocument(uri: Uri, fileName: String): String {
        val mimeType = contentResolver.getType(uri).orEmpty()
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when {
            mimeType == MIME_PDF || extension == "pdf" -> extractTextFromPdf(uri)
            mimeType == MIME_DOCX || extension == "docx" -> extractTextFromDocx(uri)
            else -> throw IllegalArgumentException("Only PDF and DOCX files are supported.")
        }
    }

    private fun extractTextFromPdf(uri: Uri): String {
        contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                throw IllegalArgumentException("Unable to open PDF file.")
            }
            PDDocument.load(inputStream).use { document ->
                return normalizeExtractedResumeText(PDFTextStripper().getText(document))
            }
        }
    }

    private fun extractTextFromDocx(uri: Uri): String {
        contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                throw IllegalArgumentException("Unable to open DOCX file.")
            }
            ZipInputStream(inputStream.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return normalizeExtractedResumeText(docxXmlToText(xml))
                    }
                }
            }
        }
        throw IllegalArgumentException("DOCX document text was not found.")
    }

    private fun docxXmlToText(xml: String): String {
        return xml
            .replace(Regex("""(?i)</w:p\s*>"""), "\n")
            .replace(Regex("""(?i)<w:tab\s*/>"""), "\t")
            .replace(Regex("""(?i)<w:br\s*/>"""), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun normalizeExtractedResumeText(rawText: String): String {
        val normalized = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("""(?m)^\s*[●▪◦‣–-]\s+"""), "• ")
            .replace(Regex("""([A-Za-z]{2,})-\n([A-Za-z]{2,})""")) { match ->
                match.groupValues[1] + match.groupValues[2]
            }

        val sourceLines = normalized
            .lines()
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filterNot { it.matches(Regex("""^\d+$""")) }

        val rebuilt = mutableListOf<String>()
        for (rawLine in sourceLines) {
            val line = rawLine.trim()
            if (line.isBlank()) {
                if (rebuilt.lastOrNull()?.isNotBlank() == true) rebuilt.add("")
                continue
            }
            if (rebuilt.isEmpty() || rebuilt.last().isBlank()) {
                rebuilt.add(line)
                continue
            }

            val previous = rebuilt.last()
            val shouldMerge =
                !previous.endsWithSentencePunctuation() &&
                    !isLikelyResumeHeading(previous) &&
                    !isLikelyDateLine(previous) &&
                    !isLikelyResumeHeading(line) &&
                    !isBulletLine(line) &&
                    !isLikelyDateLine(line)

            if (shouldMerge) {
                rebuilt[rebuilt.lastIndex] = "$previous $line"
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            } else {
                rebuilt.add(line)
            }
        }

        return rebuilt
            .joinToString("\n")
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun String.endsWithSentencePunctuation(): Boolean {
        val cleaned = trim()
        return cleaned.endsWith('.') || cleaned.endsWith('!') ||
            cleaned.endsWith('?') || cleaned.endsWith(':')
    }

    private fun isLikelyResumeHeading(line: String): Boolean {
        val cleaned = line.trim().trimEnd(':')
        val commonHeadings = setOf(
            "Experience", "Work Experience", "Professional Experience", "Education",
            "Skills", "Technical Skills", "Projects", "Certifications", "Summary",
            "Profile", "Objective", "Employment", "Awards"
        )
        if (commonHeadings.any { it.equals(cleaned, ignoreCase = true) }) return true
        return cleaned.length <= 32 && cleaned.any { it.isLetter() } &&
            cleaned.uppercase() == cleaned
    }

    private fun isBulletLine(line: String): Boolean = line.trimStart().startsWith("•")

    private fun isLikelyDateLine(line: String): Boolean {
        val month =
            "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec|January|February|March|April|June|July|August|September|October|November|December)"
        val year = "(?:19|20)\\d{2}"
        return Regex(
            "(?i)^\\s*(?:$month\\s+)?$year\\s*[-–—]\\s*(?:(?:$month\\s+)?$year|present)\\s*$"
        ).containsMatchIn(line) ||
            Regex(
                "^\\s*\\d{1,2}/\\d{4}\\s*[-–—]\\s*(?:\\d{1,2}/\\d{4}|present)\\s*$",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(line)
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

    companion object {
        private const val MIME_PDF = "application/pdf"
        private const val MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
}
