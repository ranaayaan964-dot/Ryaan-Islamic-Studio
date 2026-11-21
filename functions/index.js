const { createHash } = require("node:crypto");
const { initializeApp, getApps } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getAppCheck } = require("firebase-admin/app-check");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { defineSecret } = require("firebase-functions/params");
const { onRequest } = require("firebase-functions/v2/https");
const {
  MAX_REQUEST_BYTES,
  MAX_TRANSCRIPTION_REQUEST_BYTES,
  validateRequestBody,
} = require("./validation");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");
const RATE_LIMIT_REQUESTS = 10;
const RATE_LIMIT_WINDOW_MS = 60_000;
const MAX_UPSTREAM_RESPONSE_BYTES = 2 * 1024 * 1024;
const UPSTREAM_TIMEOUT_MS = 60_000;

if (getApps().length === 0) {
  initializeApp();
}

const db = getFirestore();

function sendJson(res, status, body) {
  res.status(status).set("Content-Type", "application/json; charset=utf-8").send(body);
}

function getRequestBytes(req) {
  if (Buffer.isBuffer(req.rawBody)) {
    return req.rawBody.byteLength;
  }
  return Buffer.byteLength(JSON.stringify(req.body ?? null), "utf8");
}

async function applyRateLimit(uid) {
  const id = createHash("sha256").update(uid).digest("hex");
  const ref = db.collection("_aiScholarRateLimits").doc(id);
  const now = Date.now();

  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(ref);
    const record = snapshot.exists ? snapshot.data() : {};
    const windowStart = record.windowStart?.toMillis?.() ?? 0;
    const withinWindow = now - windowStart < RATE_LIMIT_WINDOW_MS;
    const count = withinWindow ? (record.count ?? 0) : 0;

    if (count >= RATE_LIMIT_REQUESTS) {
      return false;
    }
    transaction.set(ref, {
      windowStart: withinWindow ? record.windowStart : Timestamp.fromMillis(now),
      count: count + 1,
    });
    return true;
  });
}

function buildGeminiRequest(model, payload) {
  return {
    url: `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  };
}

function buildGroqRequest(payload) {
  return {
    url: "https://api.groq.com/openai/v1/chat/completions",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  };
}

function buildTranscriptionRequest(request) {
  const form = new FormData();
  form.append("model", request.model);
  form.append("language", request.language);
  form.append("response_format", "json");
  form.append(
    "file",
    new Blob([request.audio], { type: request.mimeType }),
    request.fileName,
  );
  return {
    url: "https://api.groq.com/openai/v1/audio/transcriptions",
    method: "POST",
    headers: {},
    body: form,
  };
}

function buildLiveTokenRequest(model) {
  const now = Date.now();
  return {
    url: "https://generativelanguage.googleapis.com/v1beta/auth_tokens",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      uses: 1,
      expireTime: new Date(now + 30 * 60_000).toISOString(),
      newSessionExpireTime: new Date(now + 60_000).toISOString(),
      liveConnectConstraints: {
        model: `models/${model}`,
        config: {
          generationConfig: {
            responseModalities: ["AUDIO", "TEXT"],
            maxOutputTokens: 2048,
          },
        },
      },
    }),
  };
}

async function readJsonResponse(response) {
  const chunks = [];
  let totalBytes = 0;
  for await (const chunk of response.body) {
    totalBytes += chunk.byteLength;
    if (totalBytes > MAX_UPSTREAM_RESPONSE_BYTES) {
      throw new Error("provider response too large");
    }
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

async function sendUpstream(upstream, apiKey, provider) {
  if (provider === "gemini") {
    upstream.headers["x-goog-api-key"] = apiKey;
  } else {
    upstream.headers.Authorization = `Bearer ${apiKey}`;
  }
  const response = await fetch(upstream.url, {
    method: upstream.method,
    headers: upstream.headers,
    body: upstream.body,
    signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
  });
  if (!response.ok) {
    return { status: response.status };
  }
  return { status: response.status, body: await readJsonResponse(response) };
}

exports.aiScholarProxy = onRequest(
  {
    secrets: [GEMINI_API_KEY, GROQ_API_KEY],
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.set("Allow", "POST");
      return sendJson(res, 405, { error: "Method not allowed" });
    }

    const contentType = (req.get("content-type") || "").split(";")[0].trim().toLowerCase();
    if (contentType !== "application/json") {
      return sendJson(res, 415, { error: "Content-Type must be application/json" });
    }
    const requestLimit = req.body?.action === "transcribe" ||
      (req.body?.provider === "groq" && req.body?.model === "whisper-large-v3")
      ? MAX_TRANSCRIPTION_REQUEST_BYTES
      : MAX_REQUEST_BYTES;
    if (getRequestBytes(req) > requestLimit) {
      return sendJson(res, 413, { error: "Request body is too large" });
    }

    const authorization = req.get("authorization") || "";
    const match = /^Bearer ([^\s]+)$/.exec(authorization);
    if (!match) {
      return sendJson(res, 401, { error: "A Firebase ID token is required" });
    }

    let decodedToken;
    try {
      decodedToken = await getAuth().verifyIdToken(match[1]);
    } catch {
      return sendJson(res, 401, { error: "Firebase ID token is invalid or expired" });
    }

    const appCheckToken = req.get("x-firebase-appcheck") || "";
    if (!appCheckToken) {
      return sendJson(res, 401, { error: "Firebase App Check token is required" });
    }
    try {
      await getAppCheck().verifyToken(appCheckToken);
    } catch {
      return sendJson(res, 401, { error: "Firebase App Check token is invalid" });
    }

    let request;
    try {
      request = validateRequestBody(req.body);
    } catch (error) {
      return sendJson(res, 400, { error: error.message });
    }

    try {
      if (!(await applyRateLimit(decodedToken.uid))) {
        return sendJson(res, 429, { error: "Rate limit exceeded; try again shortly" });
      }
    } catch {
      return sendJson(res, 503, { error: "Request could not be processed" });
    }

    const apiKey = request.provider === "gemini" ? GEMINI_API_KEY.value() : GROQ_API_KEY.value();
    if (!apiKey) {
      return sendJson(res, 503, { error: "AI provider is not configured" });
    }

    if (request.action === "liveToken") {
      try {
        const upstream = await sendUpstream(buildLiveTokenRequest(request.model), apiKey, "gemini");
        if (upstream.status < 200 || upstream.status >= 300) {
          return sendJson(res, 502, {
            error: "Gemini Live token provisioning failed",
            providerStatus: upstream.status,
          });
        }
        if (typeof upstream.body?.name !== "string" || !upstream.body.name.startsWith("auth_tokens/")) {
          return sendJson(res, 502, { error: "Gemini returned an invalid Live token response" });
        }
        return sendJson(res, 200, {
          token: upstream.body.name,
          expireTime: upstream.body.expireTime,
          newSessionExpireTime: upstream.body.newSessionExpireTime,
        });
      } catch {
        return sendJson(res, 502, { error: "Gemini Live token provisioning failed" });
      }
    }

    if (request.action === "transcribe") {
      try {
        const upstream = await sendUpstream(
          buildTranscriptionRequest(request),
          apiKey,
          "groq",
        );
        if (upstream.status < 200 || upstream.status >= 300) {
          return sendJson(res, 502, {
            error: "Groq transcription request failed",
            providerStatus: upstream.status,
          });
        }
        if (typeof upstream.body?.text !== "string") {
          return sendJson(res, 502, { error: "Groq returned an invalid transcription response" });
        }
        return sendJson(res, 200, { text: upstream.body.text });
      } catch {
        return sendJson(res, 502, { error: "Groq transcription request failed" });
      }
    }

    try {
      const upstream = await sendUpstream(
        request.provider === "gemini"
          ? buildGeminiRequest(request.model, request.payload)
            : buildGroqRequest(request.payload),
        apiKey,
        request.provider,
      );
      if (upstream.status < 200 || upstream.status >= 300) {
        return sendJson(res, 502, {
          error: "AI provider request failed",
          provider: request.provider,
          providerStatus: upstream.status,
        });
      }
      return sendJson(res, 200, upstream.body);
    } catch {
      return sendJson(res, 502, { error: "AI provider did not return a valid response" });
    }
  },
);
