package com.example.interviewsprouts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

object ReportShareUtils {
    fun copyText(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(context: Context, chooserTitle: String, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun exportPdf(context: Context, fileName: String, title: String, text: String) {
        val directory = File(context.cacheDir, "shared_reports").apply { mkdirs() }
        val safeName = fileName
            .replace(Regex("""[^A-Za-z0-9._-]+"""), "_")
            .trim('_')
            .ifBlank { "Resume_Refine_Report" }
        val file = File(directory, "$safeName.pdf")
        val document = PdfDocument()
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = 11f
            typeface = Typeface.DEFAULT
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val maxTextWidth = pageWidth - margin * 2
        val bodyLineHeight = 16f
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = margin

        fun startPage() {
            page?.let { document.finishPage(it) }
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            y = margin
            page?.canvas?.drawText(title, margin, y, titlePaint)
            y += 30f
        }

        startPage()
        text.replace("\r\n", "\n").replace("\r", "\n").lines().forEach { rawLine ->
            wrapLine(rawLine, bodyPaint, maxTextWidth).forEach { line ->
                if (y > pageHeight - margin) startPage()
                page?.canvas?.drawText(line, margin, y, bodyPaint)
                y += bodyLineHeight
            }
        }
        page?.let { document.finishPage(it) }
        file.outputStream().use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export report as PDF"))
    }

    private fun wrapLine(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val indent = text.takeWhile { it == ' ' || it == '\t' }.replace("\t", "    ")
        val words = text.trim().split(Regex("""\s+"""))
        val lines = mutableListOf<String>()
        var current = indent
        words.forEach { word ->
            val candidate = if (current.trim().isEmpty()) indent + word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.trim().isNotEmpty()) lines += current
                current = indent + word
            }
        }
        if (current.trim().isNotEmpty()) lines += current
        return lines.ifEmpty { listOf("") }
    }
}
