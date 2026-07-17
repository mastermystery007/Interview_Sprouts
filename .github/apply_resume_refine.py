from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

TEXT_SUFFIXES = {
    ".kt", ".kts", ".xml", ".md", ".js", ".json", ".txt", ".name"
}

SKIP_PARTS = {
    ".git", ".gradle", "build", "node_modules", ".github"
}

INPUT_OLD = (
    "Privacy: Diamond Star analysis sends resume text and JD to our backend only after the second unlock."
)
INPUT_NEW = (
    "AI processing notice: Basic and Gold analysis run on your device. If you unlock Diamond Star Analysis, "
    "your resume text and optional job description are sent through our server to DeepSeek, an external large "
    "language model, for AI analysis. Do not include sensitive information you do not want processed. Saved "
    "reports remain on this device."
)

LOCKED_OLD = (
    "Get tailored resume improvements and resume-specific interview questions.\\n\\n"
    "Your resume text and job description are sent to the backend only after you choose to view this analysis."
)
LOCKED_NEW = (
    "Diamond Star uses an external AI model.\\n\\n"
    "After you continue, your resume text and optional job description will be sent through our server to "
    "DeepSeek for analysis. Remove personal or sensitive information you do not want processed by the AI provider."
)

DIALOG_OLD = '''.setMessage(
                    "A short ad opens tailored resume suggestions " +
                        "and resume-specific interview questions."
                )'''
DIALOG_NEW = '''.setMessage(
                    "A short ad opens tailored resume suggestions and " +
                        "resume-specific interview questions.\\n\\n" +
                        "After the ad, your resume text and optional job description " +
                        "will be sent through our server to DeepSeek, an external " +
                        "large language model, for analysis."
                )'''

REPLACEMENTS = [
    (INPUT_OLD, INPUT_NEW),
    (LOCKED_OLD, LOCKED_NEW),
    (DIALOG_OLD, DIALOG_NEW),
    ("InterviewSprout Resume Report", "Resume Refine Report"),
    ("InterviewSprout_Report", "Resume_Refine_Report"),
    ("InterviewSprout_", "Resume_Refine_"),
    ("Interview Sprouts", "Resume Refine"),
    ("Interview Sprout", "Resume Refine"),
    ("InterviewSprouts", "Resume Refine"),
    ("InterviewSprout", "Resume Refine"),
    ("interview-sprouts-backend", "resume-refine-backend"),
    ("Backend proxy for Resume Refine resume AI analysis.", "Backend proxy for Resume Refine AI resume analysis."),
]

changed = []
for path in ROOT.rglob("*"):
    if not path.is_file():
        continue
    relative = path.relative_to(ROOT)
    if any(part in SKIP_PARTS for part in relative.parts):
        continue
    if path.suffix.lower() not in TEXT_SUFFIXES and path.name != ".name":
        continue

    try:
        original = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue

    updated = original
    for old, new in REPLACEMENTS:
        updated = updated.replace(old, new)

    if updated != original:
        path.write_text(updated, encoding="utf-8")
        changed.append(str(relative))

required_paths = {
    "app/src/main/res/values/strings.xml",
    "app/src/main/res/layout/activity_main.xml",
    "app/src/main/res/layout/activity_resume_input.xml",
    "app/src/main/java/com/example/interviewsprouts/ResumeReportActivity.kt",
    "app/src/main/java/com/example/interviewsprouts/SavedReportDetailActivity.kt",
    "app/src/main/java/com/example/interviewsprouts/ReportShareUtils.kt",
}

missing = sorted(required_paths - set(changed))
if missing:
    raise SystemExit(f"Expected files were not changed: {missing}")

for path in ROOT.rglob("*"):
    if not path.is_file():
        continue
    relative = path.relative_to(ROOT)
    if any(part in SKIP_PARTS for part in relative.parts):
        continue
    if path.suffix.lower() not in TEXT_SUFFIXES and path.name != ".name":
        continue
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue

    forbidden = [
        "InterviewSprout",
        "Interview Sprout",
        "Interview Sprouts",
        "sent to the backend only",
        "sent to our backend only",
    ]
    matches = [token for token in forbidden if token in text]
    if matches:
        raise SystemExit(f"Obsolete user-facing text remains in {relative}: {matches}")

print("Changed files:")
for item in sorted(changed):
    print(item)

# Trigger build diagnostics for the transformed branch.
