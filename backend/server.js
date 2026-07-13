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
  return `Analyze this resume like a premium recruiter and resume consultant using only evidence present in the resume and job description.

Target role: ${targetRole}
Experience level: ${experienceLevel}
Job description provided: ${jobSpecification ? 'yes' : 'no'}

Return valid JSON only. No markdown code fences. Keep the output short, diagnostic, recruiter-like, bespoke, and mobile-friendly.

Role-specific evaluation lens:
- Software/Data: systems, tools, architecture, scale, debugging, metrics, deployment, reliability.
- Business/Ops: requirements, stakeholders, process, KPIs, UAT, workflow improvement, measurable savings.
- HR/Recruiting: sourcing, screening, funnel, ATS/HRIS, hiring-manager alignment, candidate experience.
- Sales/Marketing: funnel, CRM, ICP, campaigns, conversion, revenue, channel tradeoffs.
- Finance: models, assumptions, variance, forecast accuracy, budget/revenue impact.
- Product/UX: research, prioritization, tradeoffs, prototypes, usability, product metrics.
- Research/Academic: research question, methods, experiments, rigor, publications/projects, reproducibility.
- General roles: ownership, scope, stakeholders, tools/methods, deliverables, and outcomes.
Do not assume a technical role unless target role/JD/resume supports it.

Quality rules:
- Use only resume/JD evidence; do not fabricate missing skills, tools, numbers, companies, responsibilities, certifications, or achievements.
- If a JD requirement is not evidenced, say "not clearly evidenced".
- If JD is provided, every suggestion must be influenced by the JD.
- If JD includes must-have or required signals, prioritize those first.
- No generic advice, no long paragraphs, no filler, no markdown, no answers or hints.
- Total visible output must be exactly 12 items: 4 review bullets + 4 suggestion bullets + 4 questions.
- Always set bulletRewriteSuggestions to null.

1. advancedReview:
Return exactly 4 bullet lines, each starting with "• ". Each bullet must be one short diagnostic sentence.
Use exactly this structure and meaning:
• Fit thesis: judge the candidate's positioning for this JD/role in one sentence.
• Proof signal: strongest concrete resume evidence and why it matters.
• Gap severity: highest-impact missing or weak JD requirement and why it matters.
• Interview risk: the hardest concern an interviewer may test.

2. tailoredResumeSuggestions:
Return exactly 4 bullet lines, each starting with "• ". Each bullet must be one sentence, practical, JD-aware, section-specific, and evidence-safe.
Use this mental model without repeating it mechanically: change a specific section or bullet type by adding truthful evidence so it addresses a JD requirement or recruiter concern.
Prefer suggestions that move strongest JD evidence higher, add a missing JD tool/skill only if evidenced, rewrite a task-only bullet into action + tool + scope + outcome, add quantified results where supportable, clarify ownership/level, show domain/stakeholder/process evidence, add certification/education evidence only if present, or remove vague soft-skill claims.
Do not output a section heading inside this field.

3. interviewQuestions:
Return exactly 4 difficult questions in this format:
Q1. ...
Q2. ...
Q3. ...
Q4. ...
Each question must test a real resume claim or JD gap: ownership depth, tradeoff decisions, failure/debugging/constraint handling, measurable-result credibility, stakeholder/process judgment, tool/domain competence, or a role-specific JD scenario.
No generic behavioral questions, no answers, no hints, and no "follow-up" wording.

4. JSON shape:
Return exactly this Android-compatible shape:
{
"advancedReview": "• Fit thesis: ...\n• Proof signal: ...\n• Gap severity: ...\n• Interview risk: ...",
"tailoredResumeSuggestions": "• ...\n• ...\n• ...\n• ...",
"interviewQuestions": "Q1. ...\nQ2. ...\nQ3. ...\nQ4. ...",
"bulletRewriteSuggestions": null,
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
  return [optimized, missing].filter(Boolean).join('\n');
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
