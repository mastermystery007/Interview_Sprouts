from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_exact(text: str, old: str, new: str, label: str, expected: int = 1) -> str:
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{label}: expected {expected} exact match(es), found {count}")
    return text.replace(old, new, expected)


def replace_regex(text: str, pattern: str, replacement: str, label: str, expected: int = 1) -> str:
    updated, count = re.subn(pattern, replacement, text, count=expected, flags=re.DOTALL)
    if count != expected:
        raise RuntimeError(f"{label}: expected {expected} regex match(es), found {count}")
    return updated


report_rel = "app/src/main/java/com/example/interviewsprouts/ResumeReportActivity.kt"
report = read(report_rel)

report = replace_exact(
    report,
    '''        findViewById<TextView>(R.id.textScoreValue).apply {
            text = scoreLabel
            textSize =
                if (scoreLabel.length > 12) {
                    20f
                } else {
                    24f
                }
        }''',
    '''        findViewById<TextView>(R.id.textScoreValue).apply {
            text = scoreLabel
            textSize = when {
                scoreLabel.length >= 17 -> 16f
                scoreLabel.length >= 13 -> 18f
                else -> 24f
            }
        }''',
    "score label sizing"
)

headings = '''        val headings = listOf(
            "Detailed Analysis",
            "Diamond Star Analysis",
            "Resume Improvement Suggestions",
            "Resume-Specific Interview Questions",
            "Top improvement",
            "Why this matters",
            "Overall fit",
            "JD status",
            "Keyword preview",
            "Missing role evidence",
            "Weak or vague bullets",
            "Quantified impact",
            "First recommended action",
            "Strengths",
            "Role signals found",
            "Evidence highlights",
            "Tool and skill evidence",
            "Keywords",
            "Role keywords found",
            "Role keywords to strengthen",
            "JD keywords found",
            "JD keywords to strengthen",
            "Suggested placement",
            "JD Match",
            "JD-specific gaps",
            "Where to add them",
            "Already evidenced",
            "Score Breakdown",
            "Relevant keyword coverage",
            "Quantified achievement evidence",
            "Action-oriented bullet writing",
            "Resume section clarity",
            "Target-role alignment",
            "Target-role and experience alignment",
            "Evidence quality",
            "Resume Structure",
            "Priority Fixes",
            "Top improvement:",
            "Main concern:",
            "Measurable impact:",
            "Section clarity:",
            "Main structure concern:",
            "Strengthen:",
            "Add:",
            "Rewrite:",
            "Status:",
            "Current analysis:",
            "For more tailored matching:",
            "Keywords found:",
            "Keywords not clearly evidenced:",
            "Target role:",
            "Achievement evidence:",
            "Weak or missing evidence:",
            "Measurable evidence:",
            "Matched JD evidence:",
            "Not clearly evidenced:",
            "Overall alignment",
            "Experience-level evidence",
            "Assessment"
        )
        headings.forEach'''

report = replace_regex(
    report,
    r"        val headings = listOf\(\n.*?\n        \)\n        headings\.forEach",
    headings,
    "report heading list"
)

signal_lists = '''        val positiveSignals = listOf(
            "Found keywords:",
            "Role Keywords Found:",
            "JD Keywords Found:",
            "Clearly evidenced",
            "Proof signal"
        )
        val gapSignals = listOf(
            "Missing keywords:",
            "Role Keywords Missing:",
            "JD Keywords Not Found:",
            "Weak bullets:",
            "Missing measurable impact:",
            "Main gap:",
            "Main hiring concern:",
            "Interview risk:",
            "Recruiter Red Flags:",
            "Not clearly evidenced"
        )'''

report = replace_regex(
    report,
    r"        val positiveSignals = listOf\(\n.*?\n        \)\n        val gapSignals = listOf\(\n.*?\n        \)",
    signal_lists,
    "positive and gap signal lists"
)

status_function = '''    private fun applyStatusLineColors(
        builder: SpannableStringBuilder,
        text: String
    ) {
        val positiveColor = Color.rgb(24, 128, 82)
        val warningColor = Color.rgb(190, 82, 32)

        val positivePattern = Regex(
            """(?im)(?:^|[—–:]\\s*)(Strong role fit|Good role fit|Excellent|Good)(?=\\s*$)"""
        )

        val warningPattern = Regex(
            """(?im)(?:^|[—–:]\\s*)(Needs Improvement|Lacking|Moderate role fit|Low role fit)(?=\\s*$)"""
        )

        positivePattern.findAll(text).forEach { match ->
            val status = match.groups[1] ?: return@forEach
            builder.setSpan(
                ForegroundColorSpan(positiveColor),
                status.range.first,
                status.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        warningPattern.findAll(text).forEach { match ->
            val status = match.groups[1] ?: return@forEach
            builder.setSpan(
                ForegroundColorSpan(warningColor),
                status.range.first,
                status.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applySpanToMatches'''

report = replace_regex(
    report,
    r"    private fun applyStatusLineColors\(\n.*?\n    \}\n\n    private fun applySpanToMatches",
    status_function,
    "status color helper"
)

report = replace_exact(
    report,
    '            "Strong evidence: ${bullets.take(2).joinToString("; ").ifBlank { "Add clearer role evidence from your resume." }}",',
    '            "Achievement evidence: ${bullets.take(2).joinToString("; ").ifBlank { "No achievement-focused evidence was detected yet." }}",',
    "advanced evidence wording"
)

report = replace_exact(
    report,
    '''        if (
            jobSpecification.isNotBlank() &&
            jdRoleMatchScore < 15
        ) {''',
    '''        if (
            jobSpecification.isNotBlank() &&
            jdRoleMatchScore <= 20
        ) {''',
    "JD score cap"
)

report = replace_exact(
    report,
    '''        if (measurableImpactScore < 20) {
            cappedScore =
                minOf(cappedScore, 78)
        }''',
    '''        if (measurableImpactScore < 40) {
            cappedScore =
                minOf(cappedScore, 78)
        }''',
    "measurable impact cap"
)

report = replace_exact(
    report,
    '        val hasImpactUnitNearNumber = Regex("\\\\b\\\\d+(?:[.,]\\\\d+)?\\\\+?\\\\s*(?:$unitPattern)\\\\b|\\\\b(?:$impactUnits)\\\\b\\\\W{0,24}\\\\b\\\\d+(?:[.,]\\\\d+)?\\\\+?\\\\b", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)',
    '''        val hasImpactUnitNearNumber = Regex(
            "\\\\b\\\\d+(?:[.,]\\\\d+)?\\\\+?\\\\s*(?:$unitPattern)\\\\b|" +
                "\\\\b(?:$unitPattern)\\\\b\\\\W{0,24}" +
                "\\\\b\\\\d+(?:[.,]\\\\d+)?\\\\+?\\\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(cleaned)''',
    "impact unit regex"
)

for function_name in (
    "calculateProjectExperienceDepthScore",
    "rankGapSeverity",
    "detectRecruiterRedFlags",
    "diagnoseBulletQuality",
):
    occurrences = report.count(function_name)
    if occurrences == 1:
        report, removed = re.subn(
            rf"\n    private fun {function_name}\([\s\S]*?\n    \}}\n",
            "\n",
            report,
            count=1,
        )
        if removed != 1:
            raise RuntimeError(f"Could not remove unused function {function_name}")
    elif occurrences > 1:
        print(f"Keeping {function_name}: found {occurrences} references")

assert '"Strong evidence"' not in report
assert '"Strong evidence:' not in report
assert "jdRoleMatchScore < 15" not in report
assert "measurableImpactScore < 20" not in report
assert "$impactUnits" not in report
assert "scoreLabel.length >= 17 -> 16f" in report
assert "Achievement evidence:" in report
write(report_rel, report)

layout_report_rel = "app/src/main/res/layout/activity_resume_report.xml"
layout_report = read(layout_report_rel)
layout_report = replace_regex(
    layout_report,
    r'(<TextView\s+android:id="@\+id/textScoreValue"[\s\S]*?android:textSize=")20sp(")',
    r'\g<1>16sp\g<2>',
    "score label XML size"
)
assert 'android:id="@+id/textScoreValue"' in layout_report
write(layout_report_rel, layout_report)

input_activity_rel = "app/src/main/java/com/example/interviewsprouts/ResumeInputActivity.kt"
input_activity = read(input_activity_rel)
input_activity = replace_exact(input_activity, '"Android Engineer"', '"Android Developer"', "Android role name")
input_activity = replace_exact(input_activity, '"Security Engineer"', '"Cybersecurity Analyst"', "cybersecurity role name")
write(input_activity_rel, input_activity)

input_layout_rel = "app/src/main/res/layout/activity_resume_input.xml"
input_layout = read(input_layout_rel)
input_layout = replace_exact(
    input_layout,
    'android:text="Job description"',
    'android:text="Paste JD for tailored matching (optional)"',
    "JD field title"
)
input_layout = replace_exact(
    input_layout,
    'android:gravity="top"',
    'android:gravity="top|start"',
    "JD field gravity"
)
input_layout = replace_exact(
    input_layout,
    'android:hint="Optional: paste JD for tailored matching"',
    'android:hint="Paste the job description here to get more accurate role matching, missing-skill checks, and JD-specific interview questions."',
    "JD field hint"
)
input_layout = replace_regex(
    input_layout,
    r'''\n\s*<TextView\n\s*android:layout_width="match_parent"\n\s*android:layout_height="wrap_content"\n\s*android:layout_marginTop="10dp"\n\s*android:text="Add a job description for custom suggestions, missing-skill checks, sharper role matching, and JD-specific interview questions\."\n\s*android:textColor="#64748B"\n\s*android:textSize="12sp"\s*/>''',
    "",
    "external JD explanation"
)
assert "*" not in "Paste JD for tailored matching (optional)"
assert "Optional: paste JD for tailored matching" not in input_layout
assert "Add a job description for custom suggestions" not in input_layout
write(input_layout_rel, input_layout)

# Restore accidental Android Studio planning metadata to its earlier single-entry state.
planning_rel = ".idea/planningMode.xml"
write(
    planning_rel,
    '''<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="PlanningModeManager">
    <option name="approvalStates">
      <map>
        <entry key="20260705-125315-73f0deeb-b764-4ce3-91fa-e5c310aede86" value="true" />
      </map>
    </option>
  </component>
</project>
'''
)

# Ensure role names are consistent anywhere else under app/src.
for path in (ROOT / "app" / "src").rglob("*"):
    if path.is_file() and path.suffix in {".kt", ".xml", ".json", ".txt"}:
        text = path.read_text(encoding="utf-8")
        updated = text.replace("Android Engineer", "Android Developer").replace("Security Engineer", "Cybersecurity Analyst")
        if updated != text:
            path.write_text(updated, encoding="utf-8")

for path in (ROOT / "app" / "src").rglob("*"):
    if path.is_file() and path.suffix in {".kt", ".xml", ".json", ".txt"}:
        text = path.read_text(encoding="utf-8")
        if "Android Engineer" in text or "Security Engineer" in text:
            raise RuntimeError(f"Old role name remains in {path}")

# Remove the one-time automation from the final commit.
(ROOT / ".github" / "workflows" / "apply_remaining_fixes.yml").unlink(missing_ok=True)
Path(__file__).unlink(missing_ok=True)

print("Remaining fixes applied successfully.")
