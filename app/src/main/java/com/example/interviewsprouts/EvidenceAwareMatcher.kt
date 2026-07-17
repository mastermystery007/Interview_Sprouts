package com.example.interviewsprouts

import kotlin.math.roundToInt

data class RequirementEvidence(
    val requirement: String,
    val importance: Int,
    val confidence: Double,
    val status: String,
    val evidenceLine: String?,
    val sectionLabel: String
)

data class BulletPreview(
    val original: String,
    val weakness: String,
    val improvedStructure: String
) {
    val copyText: String
        get() = """
Original
$original

Why it is weak
$weakness

Improved structure
$improvedStructure
        """.trimIndent()
}

data class EvidenceMatchResult(
    val requirements: List<RequirementEvidence>,
    val matchScore: Int,
    val topGap: RequirementEvidence?,
    val topStrength: RequirementEvidence?
) {
    val foundRequirements: List<String>
        get() = requirements
            .filter { it.confidence >= 0.60 }
            .map { it.requirement }

    val missingRequirements: List<String>
        get() = requirements
            .filter { it.confidence < 0.60 }
            .map { it.requirement }

    fun confidenceFor(requirement: String): Double {
        val normalized =
            EvidenceAwareMatcher.canonicalKey(requirement)

        return requirements
            .firstOrNull {
                EvidenceAwareMatcher.canonicalKey(
                    it.requirement
                ) == normalized
            }
            ?.confidence
            ?: 0.0
    }

    fun formatEvidenceMap(
        limit: Int = 10
    ): String {
        if (requirements.isEmpty()) {
            return "• No role or JD requirements were available for evidence mapping."
        }

        return requirements
            .sortedWith(
                compareByDescending<RequirementEvidence> {
                    it.importance
                }
                    .thenBy {
                        it.confidence
                    }
                    .thenBy {
                        it.requirement.lowercase()
                    }
            )
            .take(limit)
            .joinToString("\n\n") { item ->
                buildString {
                    append(
                        "• ${item.requirement} — ${item.status}"
                    )

                    if (!item.evidenceLine.isNullOrBlank()) {
                        append(
                            "\n  Evidence: “${item.evidenceLine}”"
                        )
                    } else {
                        append(
                            "\n  Evidence: none detected"
                        )
                    }
                }
            }
    }
}

object EvidenceAwareMatcher {
    private enum class ResumeSection(
        val label: String
    ) {
        SUMMARY("Summary"),
        SKILLS("Skills"),
        EXPERIENCE("Experience"),
        PROJECTS("Projects"),
        EDUCATION("Education"),
        OTHER("Other")
    }

    private data class ResumeEvidenceLine(
        val text: String,
        val section: ResumeSection,
        val hadBulletMarker: Boolean
    )

    private val actionVerbs = setOf(
        "achieved",
        "analyzed",
        "automated",
        "built",
        "coordinated",
        "created",
        "delivered",
        "designed",
        "developed",
        "deployed",
        "executed",
        "generated",
        "implemented",
        "improved",
        "increased",
        "launched",
        "led",
        "managed",
        "optimized",
        "reduced",
        "resolved",
        "scaled",
        "streamlined",
        "tested"
    )

    private val responsibilityStarters = setOf(
        "worked on",
        "responsible for",
        "helped",
        "helped with",
        "handled",
        "supported",
        "maintained",
        "collaborated",
        "conducted",
        "performed",
        "assisted",
        "owned"
    )

    private val outcomeWords = setOf(
        "improved",
        "reduced",
        "increased",
        "saved",
        "optimized",
        "grew",
        "accelerated",
        "streamlined",
        "delivered",
        "resolved",
        "launched",
        "converted",
        "cut",
        "raised",
        "resulted",
        "enabled"
    )

    private val synonymGroups = listOf(
        setOf("kubernetes", "k8s"),
        setOf(
            "amazon web services",
            "aws"
        ),
        setOf(
            "google cloud platform",
            "google cloud",
            "gcp"
        ),
        setOf(
            "microsoft azure",
            "azure"
        ),
        setOf(
            "continuous integration",
            "continuous delivery",
            "ci/cd",
            "cicd"
        ),
        setOf(
            "jetpack compose",
            "compose"
        ),
        setOf(
            "android sdk",
            "android"
        ),
        setOf(
            "postgresql",
            "postgres"
        ),
        setOf(
            "javascript",
            "js"
        ),
        setOf(
            "react.js",
            "reactjs",
            "react"
        ),
        setOf(
            "node.js",
            "nodejs"
        ),
        setOf(
            "machine learning",
            "ml"
        ),
        setOf(
            "artificial intelligence",
            "ai"
        ),
        setOf(
            "cybersecurity",
            "cyber security",
            "information security"
        ),
        setOf(
            "penetration testing",
            "pentesting",
            "pentest"
        ),
        setOf(
            "rest api",
            "restful api",
            "rest"
        ),
        setOf(
            "spring boot",
            "spring"
        ),
        setOf(".net", "dotnet"),
        setOf("c#", "c sharp"),
        setOf("c++", "c plus plus")
    )

    fun analyze(
        resumeText: String,
        jobDescription: String,
        roleKeywords: List<String>,
        jdKeywords: List<String>
    ): EvidenceMatchResult {
        val resumeLines =
            parseResumeLines(resumeText)

        val requirements =
            linkedMapOf<String, Pair<String, Boolean>>()

        jdKeywords
            .filter { it.isNotBlank() }
            .forEach { requirement ->
                requirements.putIfAbsent(
                    canonicalKey(requirement),
                    requirement.trim() to true
                )
            }

        roleKeywords
            .filter { it.isNotBlank() }
            .forEach { requirement ->
                requirements.putIfAbsent(
                    canonicalKey(requirement),
                    requirement.trim() to false
                )
            }

        val evidenceItems =
            requirements.values.map {
                    (displayName, fromJd) ->
                evaluateRequirement(
                    requirement = displayName,
                    fromJd = fromJd,
                    resumeLines = resumeLines,
                    jobDescription = jobDescription
                )
            }

        val totalWeight =
            evidenceItems
                .sumOf { it.importance }
                .coerceAtLeast(1)

        val weightedScore =
            evidenceItems.sumOf {
                it.confidence * it.importance
            } / totalWeight

        val matchScore =
            if (evidenceItems.isEmpty()) {
                45
            } else {
                (weightedScore * 100.0)
                    .roundToInt()
                    .coerceIn(20, 95)
            }

        val topGap = evidenceItems
            .filter {
                it.confidence < 0.80
            }
            .maxWithOrNull(
                compareBy<RequirementEvidence> {
                    it.importance *
                        (1.0 - it.confidence)
                }
                    .thenBy {
                        it.importance
                    }
            )

        val topStrength = evidenceItems
            .filter {
                it.confidence >= 0.60
            }
            .maxWithOrNull(
                compareBy<RequirementEvidence> {
                    it.importance *
                        it.confidence
                }
                    .thenBy {
                        it.confidence
                    }
            )

        return EvidenceMatchResult(
            requirements = evidenceItems,
            matchScore = matchScore,
            topGap = topGap,
            topStrength = topStrength
        )
    }

    fun buildBulletPreview(
        resumeText: String,
        relevantKeywords: List<String>
    ): BulletPreview {
        val candidateLines =
            parseResumeLines(resumeText)
                .filter {
                    it.section ==
                        ResumeSection.EXPERIENCE ||
                        it.section ==
                        ResumeSection.PROJECTS ||
                        it.section ==
                        ResumeSection.OTHER
                }
                .filter {
                    it.text.length in 20..260
                }
                .filterNot {
                    looksLikeHeading(it.text)
                }
                .filter {
                    isLikelyBulletCandidate(it)
                }

        val selected = candidateLines
            .maxByOrNull { line ->
                bulletWeaknessScore(
                    line.text,
                    relevantKeywords
                )
            }
            ?.text

        if (selected == null) {
            return BulletPreview(
                original =
                    "No suitable experience or project bullet was detected.",
                weakness =
                    "Company names, job titles, locations, dates, and contact details are excluded from bullet feedback.",
                improvedStructure =
                    "Add a real bullet: [Strong action verb] [specific task] using [tool or method], resulting in [truthful outcome or metric]."
            )
        }

        val issues =
            mutableListOf<String>()

        val lower =
            selected.lowercase()

        if (startsWithResponsibilityLanguage(lower)) {
            issues +=
                "It begins with responsibility language instead of clear ownership."
        }

        if (!containsActionVerb(selected)) {
            issues +=
                "It does not begin with a strong action."
        }

        if (!containsOutcome(selected)) {
            issues +=
                "It does not show a verifiable result, scale, or outcome."
        }

        if (issues.isEmpty()) {
            issues +=
                "It can be stronger by connecting the action, method, and outcome more explicitly."
        }

        val cleaned = selected
            .replace(
                Regex(
                    """(?i)^\s*(worked on|responsible for|helped with|helped|handled|supported|maintained|collaborated|conducted|performed|assisted|owned)\s+"""
                ),
                ""
            )
            .trim()
            .trimEnd('.')

        return BulletPreview(
            original =
                shorten(selected, 180),
            weakness =
                issues.joinToString(" "),
            improvedStructure =
                "[Strong action verb] ${shorten(cleaned, 120)} using [tool or method], resulting in [truthful outcome or metric]."
        )
    }

    internal fun canonicalKey(
        value: String
    ): String {
        val normalized = normalize(value)

        val group = synonymGroups
            .firstOrNull { synonyms ->
                synonyms.any {
                    normalize(it) == normalized
                }
            }

        return normalize(
            group?.firstOrNull()
                ?: value
        )
    }

    private fun evaluateRequirement(
        requirement: String,
        fromJd: Boolean,
        resumeLines: List<ResumeEvidenceLine>,
        jobDescription: String
    ): RequirementEvidence {
        val variants =
            variantsFor(requirement)

        val candidates = resumeLines
            .filter { line ->
                variants.any { variant ->
                    containsVariant(
                        line.text,
                        variant
                    )
                }
            }
            .map { line ->
                line to confidenceForLine(line)
            }

        val best =
            candidates.maxByOrNull {
                it.second
            }

        val confidence =
            best?.second ?: 0.0

        val status = when {
            confidence >= 0.90 ->
                "Clearly evidenced"

            confidence >= 0.60 ->
                "Supported by work evidence"

            confidence > 0.0 ->
                "Mentioned only"

            else ->
                "Not evidenced"
        }

        return RequirementEvidence(
            requirement = requirement,
            importance = requirementImportance(
                requirement = requirement,
                fromJd = fromJd,
                jobDescription = jobDescription
            ),
            confidence = confidence,
            status = status,
            evidenceLine =
                best?.first?.text?.let {
                    shorten(it, 160)
                },
            sectionLabel =
                best?.first?.section?.label.orEmpty()
        )
    }

    private fun requirementImportance(
        requirement: String,
        fromJd: Boolean,
        jobDescription: String
    ): Int {
        if (!fromJd) {
            return 1
        }

        val variants =
            variantsFor(requirement)

        val matchingContext = jobDescription
            .replace("\r", "\n")
            .split(
                Regex("""[\n.!?;]+""")
            )
            .firstOrNull { line ->
                variants.any {
                    containsVariant(line, it)
                }
            }
            ?.lowercase()
            .orEmpty()

        return when {
            listOf(
                "required",
                "must have",
                "mandatory",
                "essential",
                "minimum qualification",
                "minimum requirement"
            ).any {
                matchingContext.contains(it)
            } -> 3

            listOf(
                "preferred",
                "nice to have",
                "bonus",
                "advantage"
            ).any {
                matchingContext.contains(it)
            } -> 2

            else -> 2
        }
    }

    private fun confidenceForLine(
        line: ResumeEvidenceLine
    ): Double {
        val base = when (line.section) {
            ResumeSection.SKILLS -> 0.30
            ResumeSection.SUMMARY -> 0.35
            ResumeSection.EDUCATION -> 0.45
            ResumeSection.EXPERIENCE,
            ResumeSection.PROJECTS -> 0.60
            ResumeSection.OTHER -> 0.40
        }

        val hasAction =
            containsActionVerb(line.text)

        val hasOutcome =
            containsOutcome(line.text)

        return when {
            hasOutcome &&
                isWorkEvidenceSection(
                    line.section
                ) -> 1.00

            hasAction &&
                isWorkEvidenceSection(
                    line.section
                ) -> 0.80

            hasOutcome ->
                maxOf(base, 0.75)

            else -> base
        }
    }

    private fun isWorkEvidenceSection(
        section: ResumeSection
    ): Boolean {
        return section ==
            ResumeSection.EXPERIENCE ||
            section ==
            ResumeSection.PROJECTS
    }

    private fun bulletWeaknessScore(
        line: String,
        relevantKeywords: List<String>
    ): Int {
        val lower =
            line.lowercase()

        var score = 0

        if (startsWithResponsibilityLanguage(lower)) {
            score += 5
        }

        if (!containsActionVerb(line)) {
            score += 3
        }

        if (!containsOutcome(line)) {
            score += 4
        }

        if (
            relevantKeywords.none {
                containsVariant(line, it)
            }
        ) {
            score += 1
        }

        if (line.length < 55) {
            score += 1
        }

        return score
    }

    private fun startsWithResponsibilityLanguage(
        lowerText: String
    ): Boolean {
        return responsibilityStarters.any {
            lowerText.startsWith(it)
        }
    }

    private fun isLikelyBulletCandidate(
        line: ResumeEvidenceLine
    ): Boolean {
        val cleaned =
            line.text.trim()

        if (cleaned.isBlank()) {
            return false
        }

        val hasAction =
            containsActionVerb(cleaned)

        val startsWithResponsibility =
            startsWithResponsibilityLanguage(
                cleaned.lowercase()
            )

        if (
            looksLikeEmployerOrResumeMetadata(
                cleaned,
                hasAction,
                startsWithResponsibility
            )
        ) {
            return false
        }

        val wordCount = wordCount(cleaned)

        if (wordCount < 4) {
            return false
        }

        if (line.hadBulletMarker) {
            return true
        }

        if (hasAction || startsWithResponsibility) {
            return true
        }

        return wordCount >= 8 &&
            (
                cleaned.endsWith('.') ||
                    cleaned.endsWith(';') ||
                    cleaned.contains(',')
                )
    }

    private fun looksLikeEmployerOrResumeMetadata(
        text: String,
        hasAction: Boolean,
        startsWithResponsibility: Boolean
    ): Boolean {
        val cleaned =
            text.trim()

        val hasOutcome =
            containsOutcome(cleaned)

        val looksLikeContactOrLink =
            Regex(
                """(?i)\b[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}\b"""
            ).containsMatchIn(cleaned) ||
                Regex(
                    """(?i)https?://|www\.|linkedin\.com|github\.com"""
                ).containsMatchIn(cleaned) ||
                Regex(
                    """(?<!\w)(?:\+?\d[\d\s().-]{7,}\d)(?!\w)"""
                ).containsMatchIn(cleaned)

        if (looksLikeContactOrLink) {
            return true
        }

        val hasMonth = Regex(
            """(?i)\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\b"""
        ).containsMatchIn(cleaned)

        val hasYear = Regex(
            """\b(?:19|20)\d{2}\b"""
        ).containsMatchIn(cleaned)

        val hasDateRange = Regex(
            """(?i)\b(?:19|20)\d{2}\s*[-–—/]\s*(?:\d{2,4}|present|current)\b"""
        ).containsMatchIn(cleaned) ||
            Regex(
                """(?i)\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\s+(?:19|20)\d{2}\s*[-–—]\s*(?:(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\s+)?(?:\d{2,4}|present|current)\b"""
            ).containsMatchIn(cleaned)

        val hasMetadataSeparator =
            cleaned.contains('|') ||
                cleaned.contains('·') ||
                cleaned.contains('•') ||
                cleaned.contains('\t')

        if (
            !hasAction &&
            !startsWithResponsibility &&
            !hasOutcome &&
            (
                hasMetadataSeparator ||
                    hasDateRange ||
                    (hasMonth && hasYear)
                )
        ) {
            return true
        }

        val words = wordCount(cleaned)

        val compactTitleOrCompanyLine =
            words <= 8 &&
                !hasAction &&
                !startsWithResponsibility &&
                !hasOutcome &&
                !cleaned.endsWith('.') &&
                !cleaned.endsWith(';')

        if (compactTitleOrCompanyLine) {
            return true
        }

        val locationAndDateLine =
            !hasAction &&
                !startsWithResponsibility &&
                (hasYear || hasMonth) &&
                cleaned.contains(',')

        return locationAndDateLine
    }

    private fun containsActionVerb(
        text: String
    ): Boolean {
        return actionVerbs.any { verb ->
            Regex(
                """(?i)(?<![a-z0-9])${Regex.escape(verb)}(?![a-z0-9])"""
            ).containsMatchIn(text)
        }
    }

    private fun containsOutcome(
        text: String
    ): Boolean {
        val lower =
            text.lowercase()

        val metricPattern = Regex(
            """(?i)(?:\b\d+(?:[.,]\d+)?\s*%|\b\d+(?:[.,]\d+)?\+?\s*(?:users?|customers?|clients?|hours?|days?|weeks?|months?|projects?|requests?|transactions?|ms|seconds?|minutes?)\b|[$₹€£]\s*\d+)"""
        )

        return metricPattern.containsMatchIn(text) ||
            outcomeWords.any { word ->
                Regex(
                    """(?i)(?<![a-z0-9])${Regex.escape(word)}(?![a-z0-9])"""
                ).containsMatchIn(lower)
            }
    }

    private fun variantsFor(
        requirement: String
    ): Set<String> {
        val normalized =
            normalize(requirement)

        val group = synonymGroups
            .firstOrNull { synonyms ->
                synonyms.any {
                    normalize(it) == normalized
                }
            }

        return buildSet {
            add(requirement)
            group?.forEach {
                add(it)
            }
        }
    }

    private fun parseResumeLines(
        resumeText: String
    ): List<ResumeEvidenceLine> {
        var section =
            ResumeSection.OTHER

        val parsed =
            mutableListOf<ResumeEvidenceLine>()

        resumeText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .forEach { rawLine ->
                val rawTrimmed =
                    rawLine.trim()

                if (rawTrimmed.isBlank()) {
                    return@forEach
                }

                val hadBulletMarker =
                    rawTrimmed.firstOrNull() in
                        setOf(
                            '•',
                            '-',
                            '–',
                            '—',
                            '*',
                            '▪',
                            '◦',
                            '‣'
                        )

                val line = rawTrimmed
                    .trimStart(
                        '•',
                        '-',
                        '–',
                        '—',
                        '*',
                        '▪',
                        '◦',
                        '‣'
                    )
                    .trim()
                    .replace(
                        Regex("""\s+"""),
                        " "
                    )

                val detected =
                    detectHeading(line)

                if (detected != null) {
                    section = detected
                } else {
                    parsed += ResumeEvidenceLine(
                        text = line,
                        section = section,
                        hadBulletMarker =
                            hadBulletMarker
                    )
                }
            }

        return parsed
    }

    private fun detectHeading(
        line: String
    ): ResumeSection? {
        val cleaned =
            normalize(
                line.trimEnd(':')
            )

        return when {
            cleaned in setOf(
                "summary",
                "profile",
                "professional summary",
                "objective",
                "about"
            ) -> ResumeSection.SUMMARY

            cleaned in setOf(
                "skills",
                "technical skills",
                "core skills",
                "technologies",
                "tools",
                "tech stack"
            ) -> ResumeSection.SKILLS

            cleaned in setOf(
                "experience",
                "work experience",
                "professional experience",
                "employment",
                "work history",
                "internship",
                "internships"
            ) -> ResumeSection.EXPERIENCE

            cleaned in setOf(
                "projects",
                "project experience",
                "academic projects",
                "personal projects"
            ) -> ResumeSection.PROJECTS

            cleaned in setOf(
                "education",
                "academic background",
                "qualifications"
            ) -> ResumeSection.EDUCATION

            else -> null
        }
    }

    private fun looksLikeHeading(
        line: String
    ): Boolean {
        val cleaned =
            line.trim().trimEnd(':')

        return detectHeading(cleaned) != null ||
            (
                cleaned.length <= 36 &&
                    cleaned.any {
                        it.isLetter()
                    } &&
                    cleaned.uppercase() == cleaned
                )
    }

    private fun containsVariant(
        text: String,
        variant: String
    ): Boolean {
        val normalizedText =
            normalize(text)

        val normalizedVariant =
            normalize(variant)

        if (normalizedVariant.isBlank()) {
            return false
        }

        return Regex(
            """(?<![a-z0-9])${Regex.escape(normalizedVariant)}(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(normalizedText)
    }

    private fun normalize(
        value: String
    ): String {
        return value
            .lowercase()
            .replace("&", " and ")
            .replace(
                Regex("""[^a-z0-9+#./]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }

    private fun wordCount(
        value: String
    ): Int {
        return Regex(
            """[A-Za-z][A-Za-z0-9+#.&/-]*"""
        )
            .findAll(value)
            .count()
    }

    private fun shorten(
        value: String,
        maxLength: Int
    ): String {
        if (value.length <= maxLength) {
            return value
        }

        val candidate =
            value.take(maxLength - 1)

        val lastSpace =
            candidate.lastIndexOf(' ')

        return if (
            lastSpace > maxLength / 2
        ) {
            candidate
                .take(lastSpace)
                .trimEnd() + "…"
        } else {
            candidate.trimEnd() + "…"
        }
    }
}
