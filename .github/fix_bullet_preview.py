from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

matcher_path = ROOT / "app/src/main/java/com/example/interviewsprouts/EvidenceAwareMatcher.kt"
report_path = ROOT / "app/src/main/java/com/example/interviewsprouts/ResumeReportActivity.kt"
test_path = ROOT / "app/src/test/java/com/example/interviewsprouts/EvidenceAwareMatcherTest.kt"

matcher = matcher_path.read_text(encoding="utf-8")
report = report_path.read_text(encoding="utf-8")
tests = test_path.read_text(encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


matcher = replace_once(
    matcher,
    """    fun buildBulletPreview(\n        resumeText: String,\n        relevantKeywords: List<String>\n    ): BulletPreview {\n""",
    """    fun buildBulletPreview(\n        resumeText: String,\n        relevantKeywords: List<String>\n    ): BulletPreview? {\n""",
    "make bullet preview nullable",
)

matcher = replace_once(
    matcher,
    """            .filter { it.length in 25..260 }\n            .filterNot { looksLikeHeading(it) }\n""",
    """            .filter { it.length in 20..260 }\n            .filterNot { looksLikeHeading(it) }\n            .filter { isLikelyBulletCandidate(it) }\n""",
    "filter preview candidates",
)

matcher = replace_once(
    matcher,
    """        if (selected == null) {\n            return BulletPreview(\n                original = \"No clear experience or project bullet was detected.\",\n                weakness = \"The resume needs at least one concrete achievement or responsibility line.\",\n                improvedStructure = \"Add: [Strong action verb] [specific task] using [tool or method], resulting in [truthful outcome or metric].\"\n            )\n        }\n""",
    """        if (selected == null) {\n            return null\n        }\n""",
    "hide preview when no safe bullet exists",
)

helper = r'''
    private fun isLikelyBulletCandidate(line: String): Boolean {
        val cleaned = line.trim()
        if (cleaned.isBlank()) {
            return false
        }

        val hasAction = containsActionVerb(cleaned)
        val startsWithResponsibility = Regex(
            """(?i)^\s*(worked on|responsible for|helped(?: with)?|handled|supported|maintained|collaborated|conducted|performed|assisted|owned)\b"""
        ).containsMatchIn(cleaned)

        val hasMonth = Regex(
            """(?i)\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\b"""
        ).containsMatchIn(cleaned)

        val hasYearOrDateRange = Regex(
            """(?i)\b(?:19|20)\d{2}\b|\b(?:19|20)\d{2}\s*[-–—/]\s*(?:\d{2,4}|present|current)\b"""
        ).containsMatchIn(cleaned)

        val hasMetadataSeparator =
            cleaned.contains('|') ||
                cleaned.contains('·') ||
                cleaned.contains('•') ||
                cleaned.contains('\t')

        val looksLikeContactOrLink =
            Regex("""(?i)\b[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}\b""").containsMatchIn(cleaned) ||
                Regex("""(?i)https?://|www\.|linkedin\.com|github\.com""").containsMatchIn(cleaned) ||
                Regex("""(?<!\w)(?:\+?\d[\d\s().-]{7,}\d)(?!\w)""").containsMatchIn(cleaned)

        if (looksLikeContactOrLink) {
            return false
        }

        if (
            !hasAction &&
            !startsWithResponsibility &&
            (
                hasMetadataSeparator ||
                    (hasMonth && hasYearOrDateRange) ||
                    hasYearOrDateRange
                )
        ) {
            return false
        }

        val wordCount = Regex("""[A-Za-z][A-Za-z0-9+#.&/-]*""")
            .findAll(cleaned)
            .count()

        if (wordCount < 4 && !hasAction && !startsWithResponsibility) {
            return false
        }

        val compactTitleOrCompanyLine =
            wordCount <= 8 &&
                !hasAction &&
                !startsWithResponsibility &&
                !containsOutcome(cleaned) &&
                !cleaned.endsWith('.') &&
                !cleaned.endsWith(';')

        if (compactTitleOrCompanyLine) {
            return false
        }

        return hasAction ||
            startsWithResponsibility ||
            wordCount >= 8
    }

'''

matcher = replace_once(
    matcher,
    """    private fun looksLikeHeading(line: String): Boolean {\n""",
    helper + """    private fun looksLikeHeading(line: String): Boolean {\n""",
    "insert safe bullet candidate filter",
)

report = replace_once(
    report,
    """        val freeBulletPreview =\n            EvidenceAwareMatcher.buildBulletPreview(\n                resumeText,\n                combinedKeywords\n            )\n\n        val bestSectionToImprove =""",
    """        val freeBulletPreview =\n            EvidenceAwareMatcher.buildBulletPreview(\n                resumeText,\n                combinedKeywords\n            )\n\n        val bulletPreviewSection =\n            freeBulletPreview?.let { preview ->\n                \"\"\"\nHighest-priority bullet\n\nOriginal\n\n${preview.original}\n\nWhy the bullet is weak\n\n${preview.weakness}\n\nImproved structure\n\n${preview.improvedStructure}\n                \"\"\".trimIndent()\n            }.orEmpty()\n\n        val bestSectionToImprove =""",
    "build optional bullet preview section",
)

report = replace_once(
    report,
    """$topGap\n\nHighest-priority bullet\n\nOriginal\n\n${freeBulletPreview.original}\n\nWhy the bullet is weak\n\n${freeBulletPreview.weakness}\n\nImproved structure\n\n${freeBulletPreview.improvedStructure}\n\nOverall fit\n""",
    """$topGap\n\n$bulletPreviewSection\n\nOverall fit\n""",
    "remove unsafe hardcoded preview",
)

report = replace_once(
    report,
    """            jdStatus,\n            freeBulletPreview.copyText\n""",
    """            jdStatus,\n            freeBulletPreview?.copyText ?: topImprovement\n""",
    "fallback copy action when preview is hidden",
)

# Existing test must account for the now-nullable preview.
tests = replace_once(
    tests,
    """import org.junit.Assert.assertEquals\nimport org.junit.Assert.assertTrue\n""",
    """import org.junit.Assert.assertEquals\nimport org.junit.Assert.assertNotNull\nimport org.junit.Assert.assertNull\nimport org.junit.Assert.assertTrue\n""",
    "add nullable assertions",
)

tests = replace_once(
    tests,
    """        assertTrue(preview.improvedStructure.contains(\"[truthful outcome or metric]\"))\n        assertTrue(preview.weakness.contains(\"responsibility\", ignoreCase = true))\n""",
    """        assertNotNull(preview)\n        assertTrue(preview!!.improvedStructure.contains(\"[truthful outcome or metric]\"))\n        assertTrue(preview.weakness.contains(\"responsibility\", ignoreCase = true))\n""",
    "update existing nullable preview test",
)

new_tests = r'''

    @Test
    fun companyLocationAndDateLineIsNotSelectedAsWeakBullet() {
        val preview = EvidenceAwareMatcher.buildBulletPreview(
            resumeText = """
Experience
Google | Bangalore, May 2024-25
Software Development Engineer
Responsible for Android application development and release support.
            """.trimIndent(),
            relevantKeywords = listOf("Android")
        )

        assertNotNull(preview)
        assertEquals(
            "Responsible for Android application development and release support.",
            preview!!.original
        )
    }

    @Test
    fun previewIsHiddenWhenResumeContainsOnlyEmployerMetadata() {
        val preview = EvidenceAwareMatcher.buildBulletPreview(
            resumeText = """
Experience
Google | Bangalore, May 2024-25
Software Development Engineer
            """.trimIndent(),
            relevantKeywords = listOf("Android")
        )

        assertNull(preview)
    }
'''

tests = replace_once(
    tests,
    "\n}\n",
    new_tests + "\n}\n",
    "append metadata regression tests",
)

matcher_path.write_text(matcher, encoding="utf-8")
report_path.write_text(report, encoding="utf-8")
test_path.write_text(tests, encoding="utf-8")

print("Safe bullet-preview filtering applied.")
