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
            content: 'You are a resume review assistant. Return only valid JSON with keys advancedReview, tailoredResumeSuggestions, interviewQuestions, bulletRewriteSuggestions, error. Do not return markdown code fences.'
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

Return a concise mobile-friendly JSON answer with these sections:

1. advancedReview
- Section label: Advanced AI Review
- Concise fit assessment.
- Strong evidence from resume.
- Weak or missing evidence.
- No assumptions.

2. tailoredResumeSuggestions
- Include the labels: Optimized Resume Points and Missing JD-Based Points.
- Optimized Resume Points: 5-8 optimized points the user can add or improve, based only on existing resume evidence.
- Focus on truthful clarity, role alignment, responsibilities, tools already evidenced, and concrete outcomes already evidenced; do not focus on metric bullet versions.
- Do not invent experience.
- If suggesting a skill/tool not in resume, phrase it as: "Add this only if true" or "Build a small project before adding this."
- Missing JD-Based Points when job description is present: matched JD keywords with resume evidence, missing JD keywords or weak evidence, where to add them (Skills / Experience / Projects / Summary), and always include "only if true".
- If job description is absent: say JD-specific suggestions require a pasted job description.

3. interviewQuestions
- Section label: Resume-Specific Interview Questions.
- Generate exactly 8-10 questions.
- Every question must reference an actual resume excerpt, project, skill, tool, metric, or JD requirement.
- No generic questions.
- Do not ask generic questions such as:
  * Tell me about yourself.
  * Why this role?
  * What are your strengths/weaknesses?
  * What would you do in the first 30 days?
  * Tell me about a conflict/teamwork situation, unless it directly references a resume item.
  * Describe feedback you received, unless it directly references a resume item.
- Every question must refer to:
  * a resume excerpt, OR
  * a detected skill/tool/project, OR
  * measurable evidence, OR
  * a JD requirement.
- Each question should use this format:
  Q1. ...
  Based on: "..."
  Why this may be asked: ...
  Strong answer should mention:
  • ...
  • ...
  Follow-up probe: ...

4. bulletRewriteSuggestions
- Optional Resume Point Rewrites only.
- Keep this short and do not focus mainly on metric bullet versions.

Rules:
1. Do not say skills are "implicit", "likely", or "assumed".
2. If a JD skill is missing, say it is missing/not clearly evidenced and should be added only if true.
3. Do not invent tools, frameworks, skills, metrics, responsibilities, companies, achievements, or architecture.
4. Do not use "AI-driven", "LLM-powered", "machine learning", or "automated" unless resume explicitly supports it.
5. Do not invent exact metrics.
6. Use placeholders only if needed: [X%], [number], [hours], [amount].
7. Do not return markdown code fences.
8. Return valid JSON only.

Return JSON exactly in this Android-compatible shape:
{
  "advancedReview": "Advanced AI Review\\n...",
  "tailoredResumeSuggestions": "Optimized Resume Points\\n...\\n\\nMissing JD-Based Points\\n...",
  "interviewQuestions": "Resume-Specific Interview Questions\\nQ1. ...",
  "bulletRewriteSuggestions": "Optional Resume Point Rewrites\\n...",
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
