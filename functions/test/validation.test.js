const test = require("node:test");
const assert = require("node:assert/strict");
const {
  MAX_REQUEST_BYTES,
  MAX_TRANSCRIPTION_REQUEST_BYTES,
  MAX_AUDIO_BYTES,
  LIVE_MODELS,
  GROQ_TRANSCRIPTION_MODELS,
  validateRequestBody,
} = require("../validation");

function geminiPayload(text = "Explain patience.") {
  return {
    contents: [{ role: "user", parts: [{ text }] }],
    generationConfig: { temperature: 0.3, maxOutputTokens: 1024 },
  };
}

function groqPayload(model = "llama-3.3-70b-versatile") {
  return {
    model,
    messages: [
      { role: "system", content: "Answer safely." },
      { role: "user", content: "Explain patience." },
    ],
    temperature: 0.3,
    max_tokens: 1024,
  };
}

test("accepts Gemini native text payloads and preserves conversation/system instructions", () => {
  const payload = {
    ...geminiPayload(),
    systemInstruction: { parts: [{ text: "Answer accurately." }] },
    contents: [
      { role: "user", parts: [{ text: "First question." }] },
      { role: "model", parts: [{ text: "First answer." }] },
      { role: "user", parts: [{ text: "Follow-up." }] },
    ],
  };
  const request = validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash",
    payload,
  });
  assert.equal(request.payload, payload);
});

test("accepts every configured Gemini and Groq text model", () => {
  for (const model of [
    "gemini-1.5-flash",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-3.1-pro-preview",
    "gemini-3.1-flash-lite-preview",
    "gemini-3.5-flash",
  ]) {
    assert.doesNotThrow(() => validateRequestBody({
      provider: "gemini",
      model,
      payload: geminiPayload(),
    }));
  }
  for (const model of ["llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"]) {
    assert.doesNotThrow(() => validateRequestBody({
      provider: "groq",
      model,
      payload: groqPayload(model),
    }));
  }
});

test("accepts bounded Gemini image generation and image analysis payloads", () => {
  const imageGeneration = validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash-image",
    payload: {
      ...geminiPayload("A peaceful mosque at sunrise."),
      generationConfig: { responseModalities: ["TEXT", "IMAGE"], maxOutputTokens: 512 },
    },
  });
  assert.equal(imageGeneration.model, "gemini-2.5-flash-image");

  const imageAnalysis = validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash",
    payload: {
      contents: [{
        role: "user",
        parts: [
          { text: "Describe this image." },
          { inlineData: { mimeType: "image/png", data: "aGVsbG8=" } },
        ],
      }],
    },
  });
  assert.equal(imageAnalysis.payload.contents[0].parts.length, 2);
});

test("accepts bounded Gemini audio input for Hifz evaluation", () => {
  const request = validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash",
    payload: {
      contents: [{
        role: "user",
        parts: [
          { text: "Transcribe the recitation." },
          { inlineData: { mimeType: "audio/mp4", data: Buffer.from("sample audio").toString("base64") } },
        ],
      }],
    },
  });
  assert.equal(request.payload.contents[0].parts.length, 2);
});

test("rejects unlisted models and unsafe provider payload shapes", () => {
  const invalidRequests = [
    { provider: "other", model: "x", payload: geminiPayload() },
    { provider: "gemini", model: "unlisted-model", payload: geminiPayload() },
    { provider: "gemini", model: "gemini-2.5-flash", payload: { ...geminiPayload(), apiKey: "not allowed" } },
    { provider: "gemini", model: "gemini-2.5-flash", payload: { contents: [{ role: "function", parts: [{ text: "x" }] }] } },
    { provider: "gemini", model: "gemini-2.5-flash", payload: { contents: [{ role: "user", parts: [{ inlineData: { mimeType: "text/plain", data: "aGVsbG8=" } }] }] } },
    { provider: "groq", model: "llama-3.3-70b-versatile", payload: { ...groqPayload(), apiKey: "not allowed" } },
    { provider: "groq", model: "llama-3.3-70b-versatile", payload: { ...groqPayload(), max_tokens: 2049 } },
    { provider: "groq", model: "llama-3.3-70b-versatile", payload: { ...groqPayload(), model: "other-model" } },
  ];
  for (const body of invalidRequests) {
    assert.throws(() => validateRequestBody(body));
  }
});

test("rejects excessive Gemini inline media and malformed base64", () => {
  assert.throws(() => validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash",
    payload: {
      contents: [{
        role: "user",
        parts: [{ inlineData: { mimeType: "image/jpeg", data: Buffer.alloc(3 * 1024 * 1024 + 1).toString("base64") } }],
      }],
    },
  }));
  assert.throws(() => validateRequestBody({
    provider: "gemini",
    model: "gemini-2.5-flash",
    payload: {
      contents: [{
        role: "user",
        parts: [{ inlineData: { mimeType: "image/jpeg", data: "not-base64" } }],
      }],
    },
  }));
});

test("validates and bounds Groq Whisper transcription requests", () => {
  assert.ok(GROQ_TRANSCRIPTION_MODELS.has("whisper-large-v3"));
  const request = validateRequestBody({
    provider: "groq",
    model: "whisper-large-v3",
    payload: {
      audioBase64: Buffer.from("sample audio").toString("base64"),
      mimeType: "audio/m4a",
      fileName: "recitation.m4a",
      language: "ar",
    },
  });
  assert.equal(request.action, "transcribe");
  assert.equal(request.fileName, "recitation.m4a");
  assert.equal(request.audio.toString(), "sample audio");
  assert.equal(request.language, "ar");
  assert.equal(MAX_TRANSCRIPTION_REQUEST_BYTES, 4 * 1024 * 1024 + 4096);
  assert.equal(MAX_AUDIO_BYTES, 3 * 1024 * 1024);
  assert.throws(() => validateRequestBody({
    provider: "groq",
    model: "whisper-large-v3",
    payload: {
      audioBase64: "not base64",
      mimeType: "audio/m4a",
      fileName: "recitation.m4a",
      language: "ar",
    },
  }));
  assert.throws(() => validateRequestBody({
    action: "transcribe",
    provider: "groq",
    model: "whisper-large-v3",
    payload: {
      audioBase64: Buffer.alloc(MAX_AUDIO_BYTES + 1).toString("base64"),
      mimeType: "audio/m4a",
      fileName: "recitation.m4a",
      language: "ar",
    },
  }));
  for (const payload of [
    { audioBase64: "YQ==", mimeType: "audio/m4a", fileName: "../evil.m4a", language: "ar" },
    { audioBase64: "YQ==", mimeType: "audio/m4a", fileName: "recitation.m4a", language: "en" },
  ]) {
    assert.throws(() => validateRequestBody({
      provider: "groq",
      model: "whisper-large-v3",
      payload,
    }));
  }
});

test("accepts only configured Gemini Live ephemeral token models and empty payloads", () => {
  for (const model of LIVE_MODELS) {
    assert.equal(validateRequestBody({
      action: "liveToken",
      provider: "gemini",
      model,
      payload: {},
    }).action, "liveToken");
  }
  assert.throws(() => validateRequestBody({
    action: "liveToken",
    provider: "gemini",
    model: "gemini-3.1-pro-preview",
    payload: {},
  }));
  assert.throws(() => validateRequestBody({
    action: "liveToken",
    provider: "gemini",
    model: "gemini-2.0-flash-exp",
    payload: { systemInstruction: "not trusted" },
  }));
});

test("request size limits accommodate encoded Hifz media but remain bounded", () => {
  assert.equal(MAX_REQUEST_BYTES, 5 * 1024 * 1024);
  assert.equal(MAX_TRANSCRIPTION_REQUEST_BYTES, 4 * 1024 * 1024 + 4096);
});
