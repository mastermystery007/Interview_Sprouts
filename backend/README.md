# InterviewSprouts Backend

Node.js + Express backend proxy for InterviewSprouts advanced resume analysis. The Android app should call this backend only; DeepSeek API keys stay server-side.

## Local setup

```bash
cd backend
npm install
cp .env.example .env
# edit .env and set DEEPSEEK_API_KEY
npm run dev
```

Health check:

```bash
curl http://localhost:3000/health
```

Analyze a resume:

```bash
curl -X POST http://localhost:3000/api/analyze-resume \
  -H "Content-Type: application/json" \
  -d '{
    "resumeText":"Education... Skills... Built SQL dashboard...",
    "targetRole":"Data Analyst",
    "experienceLevel":"Entry level",
    "jobSpecification":"SQL, dashboards, stakeholders",
    "requestedFeatures":["advancedReview","interviewQuestions"]
  }'
```


## Local phone backend testing

1. Run backend: `cd backend && npm start`
2. Find laptop Wi-Fi IPv4 address.
3. Confirm phone can open: `http://LAPTOP_IP:3000/health`
4. Set AiClient `BACKEND_BASE_URL` to: `http://LAPTOP_IP:3000/`
5. Rebuild and install the app.
6. Upload resume → analyze → first unlock → second unlock. The second unlock calls the backend.

Do not include secrets or API keys in the README.

## API

`POST /api/analyze-resume`

Request body:

- `resumeText` (required): resume content, limited to about 25,000 characters.
- `targetRole` (required): selected target role.
- `experienceLevel`: selected experience level.
- `jobSpecification`: optional job description.
- `requestedFeatures`: feature list requested by the Android client.

Response body:

```json
{
  "advancedReview": "...",
  "tailoredResumeSuggestions": "...",
  "interviewQuestions": "...",
  "bulletRewriteSuggestions": "...",
  "error": null
}
```

## DeepSeek configuration

Set these variables in the deployment environment:

- `DEEPSEEK_API_KEY`: required server-side API key.
- `DEEPSEEK_MODEL`: defaults to `deepseek-v4-flash`.
- `DEEPSEEK_BASE_URL`: defaults to `https://api.deepseek.com/chat/completions`.
- `ALLOWED_ORIGIN`: CORS origin; use your production domain instead of `*` when deployed.

## Deployment notes

- Deploy to a Node 18+ host such as Render, Railway, Fly.io, or a VPS.
- Configure environment variables in the host dashboard; do not commit `.env`.
- Point the Android placeholder base URL (`https://YOUR_BACKEND_URL/`) to the deployed backend URL.
- The server does not log full resume text; logs include only request metadata.
- Helmet, CORS, JSON size limits, and rate limiting are enabled for a safer MVP baseline.
