from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, lambda _: replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 match, found {count}")
    return updated


report_path = "app/src/main/java/com/example/interviewsprouts/ResumeReportActivity.kt"
report = read(report_path)

save_block = '''        findViewById<Button>(R.id.btnSaveReport).setOnClickListener {
            saveReportLocally(targetRole, experienceLevel, report)
            Toast.makeText(this, "Report saved locally on this device.", Toast.LENGTH_SHORT).show()
        }'''

action_block = save_block + '''

        val shareableReport =
            buildShareableReport(
                targetRole,
                experienceLevel,
                report
            )

        findViewById<Button>(
            R.id.btnCopyTopImprovement
        ).setOnClickListener {
            ReportShareUtils.copyText(
                this,
                "Top resume improvement",
                report.copyableImprovement
            )

            Toast.makeText(
                this,
                "Top improvement copied.",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<Button>(
            R.id.btnShareReport
        ).setOnClickListener {
            ReportShareUtils.shareText(
                this,
                "Share resume report",
                "InterviewSprout report for $targetRole",
                shareableReport
            )
        }

        findViewById<Button>(
            R.id.btnExportReportPdf
        ).setOnClickListener {
            ReportShareUtils.exportPdf(
                this,
                "InterviewSprout_${targetRole}_${report.overallScore}",
                "InterviewSprout Resume Report",
                shareableReport
            )
        }'''

report = replace_once(report, save_block, action_block, "report actions")

share_helper = '''    private fun buildShareableReport(
        targetRole: String,
        experienceLevel: String,
        report: ResumeReportResult
    ): String {
        return """
InterviewSprout Resume Report

Target role: $targetRole
Experience: $experienceLevel
Overall score: ${report.overallScore}/100
Rating: ${scoreRatingLabel(report.overallScore)}
${report.jdStatus}

Quick Review

${report.basicFeedback}

Detailed Analysis

${report.fullReport}
        """.trimIndent()
    }

'''

report = replace_once(
    report,
    "    private fun startAdvancedLoadingAnimation(",
    share_helper + "    private fun startAdvancedLoadingAnimation(",
    "share helper"
)

old_keyword_block = '''        val roleKeywords = getKeywordsForRole(targetRole)
        val jdKeywords = extractSimpleKeywordsFromJobSpec(jobSpecification.lowercase())
        val combinedKeywords = (roleKeywords + jdKeywords).distinctBy { it.lowercase() }
        val resumeLower = resumeText.lowercase()
        val foundRoleKeywords = roleKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingRoleKeywords = roleKeywords.filterNot { resumeLower.contains(it.lowercase()) }
        val foundJdKeywords = jdKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingJdKeywords = jdKeywords.filterNot { resumeLower.contains(it.lowercase()) }
        val foundKeywords = combinedKeywords.filter { resumeLower.contains(it.lowercase()) }
        val missingKeywords = combinedKeywords.filterNot { resumeLower.contains(it.lowercase()) }'''

new_keyword_block = '''        val roleKeywords =
            getKeywordsForRole(targetRole)

        val jdKeywords =
            extractSimpleKeywordsFromJobSpec(
                jobSpecification.lowercase()
            )

        val evidenceMatch =
            EvidenceAwareMatcher.analyze(
                resumeText = resumeText,
                jobDescription = jobSpecification,
                roleKeywords = roleKeywords,
                jdKeywords = jdKeywords
            )

        val combinedKeywords =
            (roleKeywords + jdKeywords)
                .distinctBy {
                    it.lowercase()
                }

        val resumeLower =
            resumeText.lowercase()

        val foundRoleKeywords =
            roleKeywords.filter {
                evidenceMatch.confidenceFor(it) >= 0.60
            }

        val missingRoleKeywords =
            roleKeywords.filter {
                evidenceMatch.confidenceFor(it) < 0.60
            }

        val foundJdKeywords =
            jdKeywords.filter {
                evidenceMatch.confidenceFor(it) >= 0.60
            }

        val missingJdKeywords =
            jdKeywords.filter {
                evidenceMatch.confidenceFor(it) < 0.60
            }

        val foundKeywords =
            combinedKeywords.filter {
                evidenceMatch.confidenceFor(it) >= 0.60
            }

        val missingKeywords =
            combinedKeywords.filter {
                evidenceMatch.confidenceFor(it) < 0.60
            }'''

report = replace_once(report, old_keyword_block, new_keyword_block, "keyword evidence integration")

old_score = '''        val keywordMatchScore =
            calculateKeywordMatchScore(
                foundKeywords.size,
                combinedKeywords.size
            )'''
report = replace_once(
    report,
    old_score,
    '''        val keywordMatchScore =
            evidenceMatch.matchScore''',
    "evidence-aware score"
)

report = replace_once(
    report,
    "        val missingRoleEvidence = detectMissingRoleEvidence(resumeText, targetRole, jobSpecification)",
    '''        val missingRoleEvidence =
            evidenceMatch.requirements
                .filter {
                    it.confidence < 0.60
                }
                .map {
                    "${it.requirement} — ${it.status}."
                }
                .take(6)''',
    "evidence-aware missing requirements"
)

report = regex_once(
    report,
    r'''        val topGap = when \{.*?        val bestSectionToImprove =''',
    '''        val topGap =
            evidenceMatch.topGap?.let {
                when {
                    it.confidence == 0.0 ->
                        "${it.requirement} is not evidenced in Skills, Experience, or Projects."

                    it.confidence < 0.60 ->
                        "${it.requirement} is mentioned, but it is not supported by a concrete work or project example."

                    else ->
                        "${it.requirement} has some evidence, but the result or scope is not yet clear."
                }
            }
                ?: "No major role-fit gap was detected."

        val topImprovement =
            evidenceMatch.topGap?.let {
                when {
                    it.confidence == 0.0 ->
                        "Add a truthful Experience or Projects bullet showing how you used ${it.requirement}."

                    it.confidence < 0.60 ->
                        "Move ${it.requirement} beyond the Skills section by connecting it to a specific task, tool, and result."

                    else ->
                        "Strengthen the ${it.requirement} evidence with ownership, scale, and a verifiable outcome."
                }
            }
                ?: when {
                    impactSignals.isEmpty() ->
                        "Add one truthful metric such as users, %, time saved, accuracy, revenue, cost, latency, scale, or efficiency."

                    responsibilityNoOutcome.isNotEmpty() ->
                        "Rewrite one responsibility-only bullet to show the result or outcome."

                    vaguePhrases.isNotEmpty() ->
                        "Replace vague wording with a specific task, tool, and result."

                    else ->
                        "Improve the most important bullet by adding context, tools used, and outcome."
                }

        val freeBulletPreview =
            EvidenceAwareMatcher.buildBulletPreview(
                resumeText,
                combinedKeywords
            )

        val bestSectionToImprove =''',
    "ranked gap and bullet preview"
)

old_basic = '''        val basicFeedback = """
Top improvement

$topImprovement

Why this matters

$topGap

Overall fit

${alignmentLabel(overallScore)}

JD status

$jdStatus
""".trimIndent()'''

new_basic = '''        val basicFeedback = """
Top improvement

$topImprovement

Why this matters

$topGap

Highest-priority bullet

Original

${freeBulletPreview.original}

Why the bullet is weak

${freeBulletPreview.weakness}

Improved structure

${freeBulletPreview.improvedStructure}

Overall fit

${alignmentLabel(overallScore)}

JD status

$jdStatus
""".trimIndent()'''

report = replace_once(report, old_basic, new_basic, "free bullet preview")

report = replace_once(
    report,
    '''        val keywords = """
Keywords

Role keywords found''',
    '''        val keywords = """
Keywords

Requirement evidence map

${evidenceMatch.formatEvidenceMap(12)}

Role keywords found''',
    "keyword evidence map"
)

report = replace_once(
    report,
    '''$jdMatchSection

Resume Structure''',
    '''Requirement evidence map

${evidenceMatch.formatEvidenceMap(10)}

$jdMatchSection

Resume Structure''',
    "full report evidence map"
)

report = replace_once(
    report,
    '            "Keyword preview",',
    '''            "Keyword preview",
            "Highest-priority bullet",
            "Original",
            "Why the bullet is weak",
            "Improved structure",
            "Requirement evidence map",''',
    "new headings"
)

report = replace_once(
    report,
    '''            bespoke,
            jdStatus
        )''',
    '''            bespoke,
            jdStatus,
            freeBulletPreview.copyText
        )''',
    "report result field"
)

report = replace_once(
    report,
    '''    val bespokeContent: String,
    val jdStatus: String
)''',
    '''    val bespokeContent: String,
    val jdStatus: String,
    val copyableImprovement: String
)''',
    "report result model"
)

write(report_path, report)

layout_path = "app/src/main/res/layout/activity_resume_report.xml"
layout = read(layout_path)
actions = '''        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:text="Use This Report"
            android:textColor="#111111"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/btnCopyTopImprovement"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:text="Copy Top Improvement" />

        <Button
            android:id="@+id/btnShareReport"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Share Report" />

        <Button
            android:id="@+id/btnExportReportPdf"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Export Report as PDF" />

'''
layout = replace_once(
    layout,
    '''        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:text="Save Report"''',
    actions + '''        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:text="Save Report"''',
    "report action layout"
)
write(layout_path, layout)

print("Report integration complete")
