const MAX_REQUEST_BYTES = 5 * 1024 * 1024;
const MAX_TRANSCRIPTION_REQUEST_BYTES = 4 * 1024 * 1024 + 4096;
const MAX_AUDIO_BYTES = 3 * 1024 * 1024;
const MAX_INLINE_MEDIA_BYTES = 3 * 1024 * 1024;
const MAX_OUTPUT_TOKENS = 2048;
const MAX_PROMPT_CHARS = 32_000;
const MAX_CONTENTS = 13;
const MAX_PARTS = 8;
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const ALLOWED_AUDIO_TYPES = new Set(["audio/m4a", "audio/mp4", "audio/wav", "audio/webm", "audio/mpeg"]);

const MODELS = Object.freeze({
  gemini: Object.freeze({
    text: new Set([
      "gemini-1.5-flash",
      "gemini-2.0-flash",
      "gemini-2.0-flash-lite",
      "gemini-2.5-flash",
      "gemini-2.5-flash-lite",
      "gemini-3.1-pro-preview",
      "gemini-3.1-flash-lite-preview",
      "gemini-3.5-flash",
    ]),
    image: new Set(["gemini-2.5-flash-image"]),
  }),
  groq: Object.freeze({
    text: new Set([
      "llama-3.3-70b-versatile",
      "llama-3.1-8b-instant",
      "gemma2-9b-it",
    ]),
  }),
});
const LIVE_MODELS = new Set(["gemini-2.0-flash-exp"]);
const GROQ_TRANSCRIPTION_MODELS = new Set(["whisper-large-v3"]);
const AUDIO_TYPES = new Map([
  ["audio/m4a", "m4a"],
  ["audio/mp4", "mp4"],
  ["audio/wav", "wav"],
  ["audio/webm", "webm"],
  ["audio/mpeg", "mp3"],
]);

function isRecord(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function validateBase64(data, label) {
  if (
    typeof data !== "string" ||
    data.length === 0 ||
    data.length % 4 !== 0 ||
    !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(data)
  ) {
    throw new Error(`${label} must be valid base64`);
  }
  return Buffer.from(data, "base64");
}

function validateTextParts(parts, label) {
  if (!Array.isArray(parts) || parts.length === 0 || parts.length > MAX_PARTS) {
    throw new Error(`${label} must contain between 1 and ${MAX_PARTS} parts`);
  }
  for (const part of parts) {
    if (!isRecord(part)) {
      throw new Error(`${label} contains an invalid part`);
    }
    if (Object.keys(part).length === 1 && typeof part.text === "string" && part.text.length <= MAX_PROMPT_CHARS) {
      continue;
    }
    throw new Error(`${label} may contain only bounded text parts`);
  }
}

function validateGeminiPayload(model, payload) {
  if (
    !isRecord(payload) ||
    Object.keys(payload).some((key) => !["contents", "systemInstruction", "generationConfig"].includes(key))
  ) {
    throw new Error("Gemini payload contains unsupported fields");
  }
  if (!Array.isArray(payload.contents) || payload.contents.length === 0 || payload.contents.length > MAX_CONTENTS) {
    throw new Error(`Gemini contents must contain between 1 and ${MAX_CONTENTS} turns`);
  }

  let totalInlineBytes = 0;
  let totalTextChars = 0;
  for (const content of payload.contents) {
    if (
      !isRecord(content) ||
      Object.keys(content).some((key) => !["role", "parts"].includes(key)) ||
      !["user", "model"].includes(content.role) ||
      !Array.isArray(content.parts) ||
      content.parts.length === 0 ||
      content.parts.length > MAX_PARTS
    ) {
      throw new Error("Gemini contents contain an invalid turn");
    }
    for (const part of content.parts) {
      if (!isRecord(part)) {
        throw new Error("Gemini contents contain an invalid part");
      }
      if (Object.keys(part).length === 1 && typeof part.text === "string" && part.text.length <= MAX_PROMPT_CHARS) {
        totalTextChars += part.text.length;
        continue;
      }
      if (
        Object.keys(part).length !== 1 ||
        !isRecord(part.inlineData) ||
        Object.keys(part.inlineData).some((key) => !["mimeType", "data"].includes(key)) ||
        typeof part.inlineData.mimeType !== "string" ||
        (!ALLOWED_IMAGE_TYPES.has(part.inlineData.mimeType) && !ALLOWED_AUDIO_TYPES.has(part.inlineData.mimeType))
      ) {
        throw new Error("Gemini inline media is invalid or unsupported");
      }
      const bytes = validateBase64(part.inlineData.data, "Gemini inline media");
      totalInlineBytes += bytes.length;
      if (totalInlineBytes > MAX_INLINE_MEDIA_BYTES) {
        throw new Error(`Gemini inline media must be no larger than ${MAX_INLINE_MEDIA_BYTES} bytes`);
      }
    }
  }

  if (payload.systemInstruction !== undefined) {
    if (
      !isRecord(payload.systemInstruction) ||
      Object.keys(payload.systemInstruction).some((key) => key !== "parts")
    ) {
      throw new Error("Gemini systemInstruction is invalid");
    }
    validateTextParts(payload.systemInstruction.parts, "Gemini systemInstruction");
    totalTextChars += payload.systemInstruction.parts.reduce((total, part) => total + part.text.length, 0);
  }
  if (totalTextChars === 0 || totalTextChars > MAX_PROMPT_CHARS) {
    throw new Error(`Gemini prompt content must total at most ${MAX_PROMPT_CHARS} characters`);
  }

  const generationConfig = payload.generationConfig ?? {};
  const allowedConfigKeys = new Set(["temperature", "topP", "topK", "maxOutputTokens", "responseModalities"]);
  if (!isRecord(generationConfig) || Object.keys(generationConfig).some((key) => !allowedConfigKeys.has(key))) {
    throw new Error("Gemini generationConfig contains unsupported fields");
  }
  if (generationConfig.maxOutputTokens !== undefined &&
      (!Number.isInteger(generationConfig.maxOutputTokens) ||
        generationConfig.maxOutputTokens < 1 ||
        generationConfig.maxOutputTokens > MAX_OUTPUT_TOKENS)) {
    throw new Error(`maxOutputTokens must be an integer between 1 and ${MAX_OUTPUT_TOKENS}`);
  }
  if (generationConfig.temperature !== undefined &&
      (typeof generationConfig.temperature !== "number" || generationConfig.temperature < 0 || generationConfig.temperature > 2)) {
    throw new Error("temperature must be between 0 and 2");
  }
  if (generationConfig.topP !== undefined &&
      (typeof generationConfig.topP !== "number" || generationConfig.topP < 0 || generationConfig.topP > 1)) {
    throw new Error("topP must be between 0 and 1");
  }
  if (generationConfig.topK !== undefined &&
      (!Number.isInteger(generationConfig.topK) || generationConfig.topK < 1 || generationConfig.topK > 100)) {
    throw new Error("topK must be an integer between 1 and 100");
  }
  if (generationConfig.responseModalities !== undefined &&
      (!Array.isArray(generationConfig.responseModalities) ||
        generationConfig.responseModalities.length === 0 ||
        generationConfig.responseModalities.some((value) => !["TEXT", "IMAGE", "AUDIO"].includes(value)))) {
    throw new Error("responseModalities contains an unsupported modality");
  }

  const generationType = generationConfig.responseModalities?.includes("IMAGE") ? "image" : "text";
  if (!MODELS.gemini[generationType].has(model)) {
    throw new Error("model is not allowed for this Gemini request type");
  }
  return payload;
}

function validateGroqPayload(model, payload) {
  if (
    !isRecord(payload) ||
    Object.keys(payload).some((key) => !["model", "messages", "temperature", "max_tokens"].includes(key)) ||
    payload.model !== model
  ) {
    throw new Error("Groq payload is invalid");
  }
  if (!Array.isArray(payload.messages) || payload.messages.length === 0 || payload.messages.length > MAX_CONTENTS) {
    throw new Error(`Groq messages must contain between 1 and ${MAX_CONTENTS} entries`);
  }
  let totalChars = 0;
  for (const message of payload.messages) {
    if (
      !isRecord(message) ||
      Object.keys(message).some((key) => !["role", "content"].includes(key)) ||
      !["system", "user", "assistant"].includes(message.role) ||
      typeof message.content !== "string"
    ) {
      throw new Error("Groq message is invalid");
    }
    totalChars += message.content.length;
  }
  if (totalChars === 0 || totalChars > MAX_PROMPT_CHARS) {
    throw new Error(`Groq message content must total at most ${MAX_PROMPT_CHARS} characters`);
  }
  if (payload.temperature !== undefined &&
      (typeof payload.temperature !== "number" || payload.temperature < 0 || payload.temperature > 2)) {
    throw new Error("temperature must be between 0 and 2");
  }
  if (payload.max_tokens !== undefined &&
      (!Number.isInteger(payload.max_tokens) || payload.max_tokens < 1 || payload.max_tokens > MAX_OUTPUT_TOKENS)) {
    throw new Error(`max_tokens must be an integer between 1 and ${MAX_OUTPUT_TOKENS}`);
  }
  return payload;
}

function validateRequestBody(body) {
  if (!isRecord(body) || Object.keys(body).some((key) => !["action", "provider", "model", "payload"].includes(key))) {
    throw new Error("request body must contain only action, provider, model, and payload");
  }

  const action = body.action ??
    (body.provider === "groq" && body.model === "whisper-large-v3" ? "transcribe" : "generate");
  if (!["generate", "liveToken", "transcribe"].includes(action)) {
    throw new Error("action must be generate, liveToken, or transcribe");
  }
  if (typeof body.provider !== "string" || !Object.hasOwn(MODELS, body.provider)) {
    throw new Error("provider must be gemini or groq");
  }
  if (typeof body.model !== "string") {
    throw new Error("model must be a string");
  }

  if (action === "liveToken") {
    if (body.provider !== "gemini" || !LIVE_MODELS.has(body.model)) {
      throw new Error("model is not allowed for Gemini Live");
    }
    if (!isRecord(body.payload) || Object.keys(body.payload).length !== 0) {
      throw new Error("liveToken payload must be an empty object");
    }
    return { action, provider: body.provider, model: body.model };
  }

  if (action === "transcribe") {
    if (body.provider !== "groq" || !GROQ_TRANSCRIPTION_MODELS.has(body.model)) {
      throw new Error("transcribe requires Groq whisper-large-v3");
    }
    if (
      !isRecord(body.payload) ||
      Object.keys(body.payload).some((key) => !["audioBase64", "mimeType", "fileName", "language"].includes(key))
    ) {
      throw new Error("transcription payload must contain audioBase64, mimeType, fileName, and language");
    }
    const extension = AUDIO_TYPES.get(body.payload.mimeType);
    if (!extension) {
      throw new Error("audio mimeType is not supported");
    }
    if (
      typeof body.payload.fileName !== "string" ||
      body.payload.fileName.length > 100 ||
      !/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(body.payload.fileName) ||
      !body.payload.fileName.toLowerCase().endsWith(`.${extension}`)
    ) {
      throw new Error(`fileName must be a safe filename ending in .${extension}`);
    }
    if (body.payload.language !== "ar") {
      throw new Error("language must be ar");
    }
    const audio = validateBase64(body.payload.audioBase64, "audioBase64");
    if (audio.length === 0 || audio.length > MAX_AUDIO_BYTES) {
      throw new Error(`audio must be no larger than ${MAX_AUDIO_BYTES} bytes`);
    }
    return {
      action,
      provider: body.provider,
      model: body.model,
      audio,
      mimeType: body.payload.mimeType,
      fileName: body.payload.fileName,
      language: body.payload.language,
    };
  }

  if (body.provider === "gemini") {
    return {
      action,
      provider: body.provider,
      model: body.model,
      payload: validateGeminiPayload(body.model, body.payload),
    };
  }
  return {
    action,
    provider: body.provider,
    model: body.model,
    payload: validateGroqPayload(body.model, body.payload),
  };
}

module.exports = {
  MAX_REQUEST_BYTES,
  MAX_TRANSCRIPTION_REQUEST_BYTES,
  MAX_AUDIO_BYTES,
  MAX_OUTPUT_TOKENS,
  MODELS,
  LIVE_MODELS,
  GROQ_TRANSCRIPTION_MODELS,
  validateRequestBody,
};
