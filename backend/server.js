import 'dotenv/config';
import cors from 'cors';
import express from 'express';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';

const app = express();
const PORT = process.env.PORT || 3000;
const DEEPSEEK_MODEL = process.env.DEEPSEEK_MODEL || 'deepseek-v4-flash';
const DEEPSEEK_BASE_URL = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com/chat/completions';
const MAX_RESUME_LENGTH = 25_000;

app.use(helmet());
app.use(cors({ origin: process.env.ALLOWED_ORIGIN || '*' }));
app.use(express.json({ limit: '512kb' }));
app.use(rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 60,
  standardHeaders: true,
  legacyHeaders: false
}));

app.get('/health', (_req, res) => {
  res.json({ ok: true });
});

app.post('/api/analyze-resume', async (req, res) => {
  const {
    resumeText,
    targetRole,
    experienceLevel = 'Not specified',
    jobSpecification = '',
    requestedFeatures = []
  } = req.body || {};

  if (!resumeText || typeof resumeText !== 'string' || !resumeText.trim()) {
    return res.status(400).json(emptyResponse('resumeText is required.'));
  }

  if (!targetRole || typeof targetRole !== 'string' || !targetRole.trim()) {
    return res.status(400).json(emptyResponse('targetRole is required.'));
  }

  if (resumeText.length > MAX_RESUME_LENGTH) {
    return res.status(413).json(emptyResponse(`resumeText must be ${MAX_RESUME_LENGTH} characters or fewer.`));
  }

  if (!process.env.DEEPSEEK_API_KEY) {
    return res.status(503).json(emptyResponse('AI backend is not configured.'));
  }

  console.info('Analyze resume request', {
    targetRole,
    experienceLevel,
    jobSpecificationProvided: Boolean(jobSpecification && jobSpecification.trim()),
    requestedFeatures
  });

  try {
    const aiResponse = await fetch(DEEPSEEK_BASE_URL, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${process.env.DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: DEEPSEEK_MODEL,
        temperature: 0.3,
        response_format: { type: 'json_object' },
        messages: [
          {
            role: 'system',
            content: 'You are a resume review assistant. Return only valid JSON with exactly these keys: advancedReview, tailoredResumeSuggestions, interviewQuestions, bulletRewriteSuggestions, error. Do not return markdown code fences. Do not include text outside JSON.'
          },
          {
            role: 'user',
            content: buildPrompt({ resumeText, targetRole, experienceLevel, jobSpecification })
          }
        ]
      })
    });

    if (!aiResponse.ok) {
      const safeError = `DeepSeek request failed with status ${aiResponse.status}.`;
      return res.status(502).json(emptyResponse(safeError));
    }

    const data = await aiResponse.json();
    const content = data?.choices?.[0]?.message?.content;
    const parsed = parseJsonContent(content);

    return res.json({
      advancedReview: toReadableString(parsed.advancedReview),
      tailoredResumeSuggestions: toReadableString(
        parsed.tailoredResumeSuggestions || combineOptimizedAndMissing(parsed)
      ),
      interviewQuestions: toReadableString(parsed.interviewQuestions),
      bulletRewriteSuggestions: toReadableString(parsed.bulletRewriteSuggestions),
      error: parsed.error || null
    });
  } catch (error) {
    console.error('Analyze resume failed', { message: error.message });
    return res.status(500).json(emptyResponse('AI analysis failed.'));
  }
});

function buildPrompt({ resumeText, targetRole, experienceLevel, jobSpecification }) {
  return `Analyze this resume for the target role using only evidence present in the resume.

Target role: ${targetRole}
Experience level: ${experienceLevel}
Job description provided: ${jobSpecification ? 'yes' : 'no'}

Return a concise mobile-friendly JSON answer.

Content limits:

* advancedReview: maximum 4 bullets.
* tailoredResumeSuggestions: exactly 4 bullets total.
* interviewQuestions: exactly 4 questions.
* bulletRewriteSuggestions: return an empty string unless there is a very clear rewrite opportunity.
* Avoid long paragraphs.
* Do not repeat the same warning after every bullet.

1. advancedReview

Give a concise fit assessment using only resume evidence.

Include:

* 1 bullet on overall fit.
* 1–2 bullets on strongest evidence.
* 1–2 bullets on weak or missing evidence.

Do not invent assumptions. Do not say a skill is present unless it is clearly evidenced.

2. tailoredResumeSuggestions

Return exactly 4 bullets total.

Start this section with this sentence once:
"Add suggested skills or tools only if they are true."

Then include:

* 2 bullets under "Optimized Resume Points" based on existing resume evidence.
* 2 bullets under "Missing JD-Based Points" if a job description is provided.
* If no job description is provided, use those 2 bullets for role-relevant missing points.

Rules for this section:

* Focus on clarity, role alignment, responsibilities, tools already evidenced, and concrete outcomes already evidenced.
* Do not invent experience, tools, frameworks, metrics, responsibilities, companies, achievements, or architecture.
* If a JD skill is missing, say it is "not clearly evidenced".
* If suggesting a missing technical skill, say: "Add this only if true, or build a small project before adding it."
* For non-technical skills, do not say "build a small project"; instead say where it could be evidenced, such as Skills, Experience, Projects, or Summary.
* Do not append "only if true" to every bullet.
* Do not focus mainly on metric rewrites.

3. interviewQuestions

Generate exactly 4 focused questions.

Every question must reference:

* an actual resume excerpt, OR
* a detected skill/tool/project, OR
* measurable evidence, OR
* a JD requirement.

Do not ask generic questions such as:

* Tell me about yourself.
* Why this role?
* What are your strengths/weaknesses?
* What would you do in the first 30 days?
* Tell me about a conflict/teamwork situation, unless it directly references a resume item.
* Describe feedback you received, unless it directly references a resume item.

Use this compact format:

Q1. [question]
Based on: "[resume/JD evidence]"
Strong answer should mention: [2 short points]

4. bulletRewriteSuggestions

Return an empty string unless there is a very clear rewrite opportunity. If used, maximum 2 bullets.

Rules:

1. Use only evidence present in the resume and job description.
2. Do not say skills are "implicit", "likely", "assumed", or "probably present".
3. If a JD skill is missing, say it is missing or not clearly evidenced.
4. Do not invent tools, frameworks, skills, metrics, responsibilities, companies, achievements, or architecture.
5. Do not use "AI-driven", "LLM-powered", "machine learning", or "automated" unless the resume explicitly supports it.
6. Do not invent exact metrics.
7. Use placeholders only if needed: [X%], [number], [hours], [amount].
8. Return valid JSON only.
9. Keep the response concise enough for a mobile screen.

Return JSON exactly in this Android-compatible shape:
{
"advancedReview": "...",
"tailoredResumeSuggestions": "Add suggested skills or tools only if they are true.\n\nOptimized Resume Points\n• ...\n• ...\n\nMissing JD-Based Points\n• ...\n• ...",
"interviewQuestions": "Q1. ...\nBased on: \"...\"\nStrong answer should mention: ...\n\nQ2. ...",
"bulletRewriteSuggestions": "",
"error": null
}

Job description:
${jobSpecification || 'Not provided'}

Resume text:
${resumeText}`;
}

function parseJsonContent(content) {
  if (!content || typeof content !== 'string') {
    return emptyResponse('No AI content returned.');
  }

  try {
    return JSON.parse(stripCodeFences(content).trim());
  } catch (_error) {
    return {
      advancedReview: content,
      tailoredResumeSuggestions: '',
      interviewQuestions: '',
      bulletRewriteSuggestions: '',
      error: null
    };
  }
}

function combineOptimizedAndMissing(parsed) {
  const optimized = toReadableString(parsed.optimizedResumePoints);
  const missing = toReadableString(parsed.missingJobDescriptionPoints);
  return [
    optimized ? `Optimized Resume Points\n${optimized}` : '',
    missing ? `Missing JD-Based Points\n${missing}` : ''
  ].filter(Boolean).join('\n\n');
}

function toReadableString(value) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'string') return stripCodeFences(value).trim();
  if (Array.isArray(value)) {
    return value.map((item, index) => {
      if (typeof item === 'string') return `• ${stripCodeFences(item).trim()}`;
      return `• ${stringifyObject(item, index)}`;
    }).join('\n');
  }
  if (typeof value === 'object') return stringifyObject(value);
  return String(value);
}

function stringifyObject(value) {
  return Object.entries(value)
    .map(([key, item]) => {
      if (Array.isArray(item)) {
        return `${formatLabel(key)}:\n${item.map((entry) => `• ${toReadableString(entry)}`).join('\n')}`;
      }
      if (item && typeof item === 'object') {
        return `${formatLabel(key)}:\n${stringifyObject(item)}`;
      }
      return `${formatLabel(key)}: ${toReadableString(item)}`;
    })
    .join('\n');
}

function formatLabel(key) {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/[_-]+/g, ' ')
    .replace(/^./, (char) => char.toUpperCase());
}

function stripCodeFences(value) {
  return value.replace(/```(?:json)?/gi, '').replace(/```/g, '');
}

function emptyResponse(error) {
  return {
    advancedReview: '',
    tailoredResumeSuggestions: '',
    interviewQuestions: '',
    bulletRewriteSuggestions: '',
    error
  };
}

app.listen(PORT, () => {
  console.info(`InterviewSprouts backend listening on port ${PORT}`);
});
