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
            content: 'You are a resume review assistant. Return only valid JSON with exactly these keys: advancedReview, tailoredResumeSuggestions, interviewQuestions, bulletRewriteSuggestions, error. Do not return markdown code fences. Do not include text outside JSON. Strictly obey all count limits. The final visible report must contain at most 12 content items total. Return exactly 4 interview questions only, with no answers or hints. Always set bulletRewriteSuggestions to null. Use null instead of blank strings.'
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
    const parsed = parseJsonContent(content) || {};

    return res.json({
      advancedReview: nullableReadableString(parsed.advancedReview),
      tailoredResumeSuggestions: nullableReadableString(
        parsed.tailoredResumeSuggestions || combineOptimizedAndMissing(parsed)
      ),
      interviewQuestions: nullableReadableString(parsed.interviewQuestions),
      bulletRewriteSuggestions: null,
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

Required output limits:

* Advanced AI Review: maximum 4 bullets.
* Optimized Resume Points + Missing JD-Based Points: maximum 4 bullets total across both headings.
* Resume-Specific Interview Questions: exactly 4 questions, not 5, not 8, not 10.
* Questions only. No answers, no hints, no “Strong answer should mention”, no “Based on”, no “Why this may be asked”, no “Follow-up probe”, no “Answer”, and no “Suggested answer”.
* Optional Resume Point Rewrites must be null and must not be displayed in Android.
* Total visible AI report maximum: 12 items:
  4 review bullets + 4 suggestion/JD bullets + 4 questions.

Interview question requirements:

Generate exactly 4 difficult, resume-specific interview questions.

The questions should be challenging and should test whether the candidate can deeply explain their actual work.

Each question must reference:
* a concrete resume project, skill, tool, responsibility, metric, or JD requirement.

Prefer questions that ask about:
* technical/design tradeoffs
* debugging decisions
* implementation details
* measurable impact
* ownership and exact contribution
* limitations or failure cases
* why a specific tool/approach was used
* how the candidate would improve the work now

Do not ask generic questions such as:
* Tell me about yourself.
* Why this role?
* What are your strengths/weaknesses?
* What would you do in the first 30 days?
* Generic teamwork/conflict questions.
* Generic behavioral questions.

Return questions only in this exact format:

Q1. ...
Q2. ...
Q3. ...
Q4. ...

Do not include any answer guidance.

Use this JSON shape:

{
"advancedReview": "• ...\n• ...\n• ...\n• ...",
"tailoredResumeSuggestions": "Optimized Resume Points\n• ...\n• ...\n\nMissing JD-Based Points\n• ...\n• ...",
"interviewQuestions": "Q1. ...\nQ2. ...\nQ3. ...\nQ4. ...",
"bulletRewriteSuggestions": null,
"error": null
}

Rules:

* Use only evidence present in the resume and job description.
* Do not invent tools, frameworks, metrics, responsibilities, companies, achievements, or architecture.
* If a JD skill is missing, say it is not clearly evidenced.
* Do not repeat “only if true” after every bullet.
* Do not use “AI-driven”, “LLM-powered”, “machine learning”, or “automated” unless resume explicitly supports it.
* Use placeholders only if needed: [X%], [number], [hours], [amount].
* Return valid JSON only.

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
      tailoredResumeSuggestions: null,
      interviewQuestions: null,
      bulletRewriteSuggestions: null,
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

function nullableReadableString(value) {
  const text = toReadableString(value);
  return text && text.trim() ? text.trim() : null;
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
    advancedReview: null,
    tailoredResumeSuggestions: null,
    interviewQuestions: null,
    bulletRewriteSuggestions: null,
    error
  };
}

app.listen(PORT, () => {
  console.info(`InterviewSprouts backend listening on port ${PORT}`);
});
