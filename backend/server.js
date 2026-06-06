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

This app supports many roles, including software, data, business analyst, recruiter, HR, sales, marketing, finance, operations, product, UX/design, and general business roles. Adapt the review and questions to the actual target role. Do not assume the role is technical unless the target role, job description, or resume clearly indicates it.

Required output limits:

* advancedReview: exactly 4 separate bullet lines maximum.
* tailoredResumeSuggestions: exactly 4 bullet lines maximum.
* interviewQuestions: exactly 4 questions, not 5, not 8, not 10.
* Questions only. No answers, no hints, no "Strong answer should mention", no "Based on", no "Why this may be asked", no "Follow-up probe", no "Answer", and no "Suggested answer".
* bulletRewriteSuggestions: null.
* Total visible AI report maximum: 12 items:
  4 review bullets + 4 suggestion bullets + 4 questions.

1. advancedReview

Return up to 4 bullets only.

Each bullet must:

* start with "• "
* be one sentence only
* cover one idea only
* not contain sub-bullets, numbered lists, or multiple semicolon-separated points
* not contain headings inside the field

Use this structure:
• Overall fit: ...
• Strongest evidence: ...
• Main gap: ...
• Impact evidence: ...

For "Impact evidence", adapt to the role:

* Software/Data/Product: systems built, users, latency, accuracy, reliability, dashboards, experiments, scale, adoption, measurable outcomes.
* Business Analyst/Operations: requirements, stakeholders, processes, KPIs, reporting, UAT, process improvements, cost/time savings.
* Recruiter/HR: sourcing volume, screening, hiring pipeline, time-to-fill, candidate experience, onboarding, HR operations, employee engagement.
* Sales/Marketing: leads, conversion, campaigns, revenue, CRM, funnel metrics, CTR/CPC/ROAS, client acquisition.
* Finance: financial modeling, forecasting, budgeting, variance analysis, reporting accuracy, cost/revenue impact.
* UX/Design: user research, usability findings, prototypes, design systems, accessibility, user flows, product impact.
* General roles: ownership, scope, stakeholders, deliverables, outcomes, tools, and measurable impact where evidenced.

If evidence is missing, say it is not clearly evidenced. Do not invent metrics.

2. tailoredResumeSuggestions

Visible Android title will be "Resume Improvement Suggestions". Do not include any section heading inside this field.

Return exactly 4 bullets maximum.

Rules:

* Mix resume improvements and JD-gap improvements in one list.
* If a job description is provided, include 1–2 bullets about JD requirements that are not clearly evidenced.
* If no job description is provided, use all 4 bullets for role-relevant resume improvements.
* Each bullet must start with "• ".
* Each bullet must be one sentence only.
* Do not include sub-bullets.
* Do not use headings like "Optimized Resume Points", "Missing JD-Based Points", or "Tailored Resume Suggestions".
* Do not invent experience, tools, frameworks, metrics, responsibilities, companies, achievements, or architecture.
* Do not append "only if true" to every bullet.

Make suggestions role-appropriate:

* Software/Data: clarify implementation, architecture, testing, systems, tools, scale, measurable outcomes.
* Business Analyst: clarify requirements gathering, stakeholder work, process mapping, UAT, KPIs, dashboards, business impact.
* Recruiter/HR: clarify sourcing channels, screening process, hiring funnel, ATS/HRIS tools, time-to-fill, candidate or employee outcomes.
* Sales/Marketing: clarify campaign ownership, CRM usage, funnel stage, lead/revenue impact, client segments, conversion metrics.
* Finance: clarify model/report ownership, assumptions, variance analysis, budgeting/forecasting impact, financial decision support.
* Operations: clarify process ownership, SOPs, vendor/supply chain work, cost/time/quality improvements.
* UX/Product: clarify research method, user problem, design decision, prototype/testing, product/user impact.

3. interviewQuestions

Generate exactly 4 difficult, standalone, resume-specific interview questions.

Questions must be hard to answer without real experience, but they must be appropriate for the target role.

Each question must reference at least one concrete resume/JD signal:

* project
* role responsibility
* tool
* skill
* metric
* stakeholder
* process
* campaign
* hiring pipeline
* financial model
* dashboard/report
* user research/design artifact
* system/product/process
* JD requirement

Do not ask generic questions such as:

* Tell me about yourself.
* Why this role?
* What are your strengths/weaknesses?
* What would you do in the first 30 days?
* Generic teamwork/conflict questions.
* Generic behavioral questions.

Do not label questions as follow-up, probe, or behavioral.

For technical roles, prefer questions about:

* implementation choices
* architecture/design tradeoffs
* debugging decisions
* performance/scalability/quality constraints
* exact contribution
* limitations and future improvements

For business analyst roles, prefer questions about:

* how requirements were gathered and validated
* stakeholder conflicts or tradeoffs tied to a specific project
* KPI/report/dashboard design decisions
* UAT or acceptance criteria
* process improvement impact
* how business value was measured

For recruiter/HR roles, prefer questions about:

* sourcing strategy and channel choice
* screening criteria and candidate quality
* funnel metrics and time-to-fill
* stakeholder or hiring manager alignment
* ATS/HRIS workflow decisions
* candidate or employee experience outcomes

For sales/marketing roles, prefer questions about:

* campaign/funnel decisions
* target audience or lead qualification
* CRM/process ownership
* conversion/revenue impact
* A/B testing or channel tradeoffs
* how success was measured

For finance roles, prefer questions about:

* model assumptions
* variance drivers
* forecast accuracy
* budget tradeoffs
* reporting decisions
* business recommendation impact

For operations roles, prefer questions about:

* process bottlenecks
* SOP or workflow design
* vendor/supply chain decisions
* quality/cost/time tradeoffs
* root-cause analysis
* measurable process improvement

For UX/product roles, prefer questions about:

* user research method
* design tradeoffs
* prioritization
* prototype/testing decisions
* product metric impact
* handling ambiguous user feedback

Return questions only in this exact format:
Q1. ...
Q2. ...
Q3. ...
Q4. ...

Do not include answers, hints, "Based on", "Strong answer should mention", "Why this may be asked", or "Follow-up probe".

4. bulletRewriteSuggestions

Return null.

Rules:

* Use only evidence present in the resume and job description.
* Do not say skills are "implicit", "likely", "assumed", or "probably present".
* If a JD skill is missing, say it is missing or not clearly evidenced.
* Do not invent tools, frameworks, skills, metrics, responsibilities, companies, achievements, or architecture.
* Do not use "AI-driven", "LLM-powered", "machine learning", or "automated" unless resume explicitly supports it.
* Do not invent exact metrics.
* Use placeholders only if needed: [X%], [number], [hours], [amount].
* Return valid JSON only.

Return JSON exactly in this Android-compatible shape:
{
"advancedReview": "• Overall fit: ...\n• Strongest evidence: ...\n• Main gap: ...\n• Impact evidence: ...",
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
