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
            content: 'You are a resume review assistant. Return only valid JSON with keys advancedReview, tailoredResumeSuggestions, interviewQuestions, bulletRewriteSuggestions, error.'
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
      advancedReview: parsed.advancedReview || '',
      tailoredResumeSuggestions: parsed.tailoredResumeSuggestions || '',
      interviewQuestions: parsed.interviewQuestions || '',
      bulletRewriteSuggestions: parsed.bulletRewriteSuggestions || '',
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

Strict requirements:
- If a job description is present, provide tailored JD-based resume suggestions.
- Generate 8-10 detailed interview questions based only on actual resume content.
- Do not include generic questions such as tell me about yourself, why this role, strengths/weaknesses, teamwork/conflict, or first 30 days.
- Provide up to 3 bullet rewrites.
- Do not invent fake metrics, skills, responsibilities, tools, employers, or achievements.
- Use placeholders only when needed: [X%], [number], [hours].
- Every interview question must cite an actual short resume excerpt or detected skill/signal.

Return JSON exactly in this shape:
{
  "advancedReview": "...",
  "tailoredResumeSuggestions": "...",
  "interviewQuestions": "...",
  "bulletRewriteSuggestions": "...",
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
    return JSON.parse(content);
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
