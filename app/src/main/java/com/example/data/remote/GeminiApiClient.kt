package com.example.data.remote

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// --- Gemini Request / Response DTOs using Moshi ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40,
    val maxOutputTokens: Int? = 1500
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

/**
 * Chat turn representation for multi-turn history.
 */
data class ChatMessageTurn(
    val text: String,
    val isFromUser: Boolean
)

/**
 * Result data returned by Gemini multi-turn chat.
 */
data class GeminiChatResult(
    val reply: String,
    val modelUsed: String,
    val roleTitle: String,
    val taskComplexity: String
)

/**
 * Specialized Chatbot Roles with dedicated system instructions and model routing.
 * - Complex Tasks: gemini-3.1-pro-preview (Deep reasoning, Fiqh, Usul al-Fiqh)
 * - General Tasks: gemini-3.5-flash (Spiritual companion, daily Quran & reflection)
 * - Fast Tasks: gemini-3.1-flash-lite (Instant Dua, prayer rules, rapid Hadith references)
 */
enum class GeminiChatbotRole(
    val title: String,
    val shortRole: String,
    val taskComplexity: String,
    val modelName: String,
    val displayModel: String,
    val systemPrompt: String
) {
    COMPLEX_JURIST(
        title = "Grand Mufti & Jurist",
        shortRole = "Fiqh & Deep Reasoning",
        taskComplexity = "Particularly Complex Tasks",
        modelName = "gemini-3.1-pro-preview",
        displayModel = "gemini-3.1-pro-preview",
        systemPrompt = """
            You are a Senior Islamic Jurist and Grand Mufti.
            Your purpose is addressing particularly complex tasks, advanced jurisprudence (Fiqh) across the classical Sunni schools (Hanafi, Maliki, Shafi'i, Hanbali), intricate inheritance distribution, contemporary bioethics, modern financial contracts, and reconciling subtle theological inquiries.
            Provide multi-step, deeply reasoned answers with precise Quranic citations (Surah:Ayah) and authentic Hadith with grading (Sahih al-Bukhari, Sahih Muslim, etc.).
            Maintain a dignified, objective, and scholarly demeanor.
        """.trimIndent()
    ),
    GENERAL_COMPANION(
        title = "Spiritual Companion 'Noor'",
        shortRole = "Daily Guidance & Reflection",
        taskComplexity = "General Tasks",
        modelName = "gemini-3.5-flash",
        displayModel = "gemini-3.5-flash",
        systemPrompt = """
            You are 'Noor', an authentic, warm, gentle, and empathetic Islamic AI Spiritual Companion.
            Your purpose is handling general daily tasks: explaining verses of the Holy Quran, sharing moral reflections (Tazkiyah), daily supplications (Duas), good character (Akhlaq), and encouraging advice.
            Always greet with 'As-salamu alaykum'. Keep answers clear, beautifully structured, comforting, and uplifting.
        """.trimIndent()
    ),
    FAST_FINDER(
        title = "Instant Hadith & Dua Finder",
        shortRole = "Fast Reference & Answers",
        taskComplexity = "Tasks that should happen fast",
        modelName = "gemini-3.1-flash-lite-preview",
        displayModel = "gemini-3.1-flash-lite",
        systemPrompt = """
            You are a high-speed Islamic Reference Assistant.
            Your purpose is fulfilling tasks that should happen fast: providing immediate authentic Duas (with Arabic text, transliteration, English meaning), prayer calculation rules, or rapid Hadith verification without unnecessary filler or lengthy preambles.
            Prioritize speed, accuracy, and conciseness.
        """.trimIndent()
    );

    companion object {
        fun fromModel(model: String): GeminiChatbotRole {
            return when {
                model.contains("pro", ignoreCase = true) -> COMPLEX_JURIST
                model.contains("lite", ignoreCase = true) -> FAST_FINDER
                else -> GENERAL_COMPANION
            }
        }

        fun autoDetectRole(prompt: String): GeminiChatbotRole {
            val lower = prompt.lowercase()
            return when {
                lower.contains("fiqh") || lower.contains("fatwa") || lower.contains("inheritance") ||
                lower.contains("mortgage") || lower.contains("bioethics") || lower.contains("interest") ||
                lower.contains("riba") || lower.contains("madhab") || lower.contains("hanafi") ||
                lower.contains("shafi") || lower.contains("maliki") || lower.contains("hanbali") ||
                lower.contains("theological") || lower.contains("usul") || lower.contains("in-depth") ||
                lower.contains("complex") || lower.contains("ruling on") -> COMPLEX_JURIST

                lower.contains("dua for") || lower.contains("hadith about") || lower.contains("quick") ||
                lower.contains("fast") || lower.contains("instant") || lower.contains("arabic text") ||
                lower.contains("transliteration") || lower.contains("prayer time rule") ||
                lower.contains("reference") -> FAST_FINDER

                else -> GENERAL_COMPANION
            }
        }
    }
}

/**
 * Real Gemini AI Client for Multi-Turn Islamic Scholar Chat.
 * Dynamically routes to:
 * - gemini-3.1-pro-preview for particularly complex tasks
 * - gemini-3.5-flash for general tasks
 * - gemini-3.1-flash-lite for tasks that should happen fast
 */
object GeminiApiClient {
    private const val TAG = "GeminiApiClient"

    fun getResolvedGeminiKey(): String {
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return buildKey.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" && !it.contains("YOUR_") }.orEmpty()
    }

    /**
     * Executes a true multi-turn chat with conversation history and role-specific system instructions.
     */
    suspend fun multiTurnChat(
        conversationHistory: List<ChatMessageTurn>,
        prompt: String,
        role: GeminiChatbotRole
    ): Result<GeminiChatResult> = withContext(Dispatchers.IO) {
        try {
            val recentHistory = conversationHistory.takeLast(12)
            val fullPrompt = buildString {
                if (recentHistory.isNotEmpty()) {
                    appendLine("Conversation context:")
                    recentHistory.forEach { turn ->
                        appendLine("${if (turn.isFromUser) "User" else "Assistant"}: ${turn.text}")
                    }
                    appendLine()
                }
                append("Current user message: ")
                append(prompt)
            }
            val routed = DualAiScholarEngine.askScholarWithSource(fullPrompt, role.systemPrompt)
            Result.success(
                GeminiChatResult(
                    reply = routed.answer,
                    modelUsed = routed.source,
                    roleTitle = role.title,
                    taskComplexity = role.taskComplexity
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI provider router failed for Gemini chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Backward-compatible simple chat for legacy components.
     */
    suspend fun chat(
        conversationHistory: List<Pair<String, Boolean>>,
        prompt: String,
        modelName: String = "gemini-3.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        val turns = conversationHistory.map { ChatMessageTurn(it.first, it.second) }
        val role = GeminiChatbotRole.fromModel(modelName)
        val result = multiTurnChat(turns, prompt, role)
        result.map { it.reply }
    }

    private fun generateScholarlySimulation(prompt: String, role: GeminiChatbotRole): String {
        val lower = prompt.lowercase()
        return when (role) {
            GeminiChatbotRole.COMPLEX_JURIST -> {
                when {
                    lower.contains("crypto") || lower.contains("bitcoin") ->
                        "**Ruling on Cryptocurrency & Digital Assets (Fiqh Council Review)**\n\n" +
                        "1. **Core Shariah Principle:** Contemporary jurists distinguish between crypto-assets with speculative/gambling features (*Gharar Fahish*) and utility-backed digital mediums of exchange.\n" +
                        "2. **Hanafi & Shafi'i Considerations:** A currency (*Thamaniyyah*) must possess customary acceptance (*Istilah*) and tangible utility without deception. If used for legitimate services and devoid of interest (*Riba*), many contemporary scholars permit it with caution.\n" +
                        "3. **Conclusion:** Permissible provided it is not utilized for illicit transactions or margin-trading contracts involving interest. *(See OIC Islamic Fiqh Academy Resolution No. 219)*."

                    lower.contains("zakat") ->
                        "**Fiqh Analysis of Zakat Calculation & Nisab**\n\n" +
                        "• **Gold Nisab:** 85 grams of fine gold (24 karat).\n" +
                        "• **Silver Nisab:** 595 grams of pure silver.\n" +
                        "• **Hawl Requirement:** One full lunar year of unencumbered possession above the threshold.\n" +
                        "• **Rate:** 2.5% (1/40th) of net Zakatable wealth as decreed in Surah At-Tawbah (9:60)."

                    else ->
                        "**Scholarly Jurisprudential Assessment (${role.displayModel})**\n\n" +
                        "In addressing: *\"$prompt\"*\n\n" +
                        "1. **Evidence from the Noble Quran:** Allah Almighty commands: *\"Ask the people of knowledge if you do not know.\"* [Surah An-Nahl 16:43].\n" +
                        "2. **Classical Consensus:** The mainstream consensus upholds that rulings must derive from primary sources (Quran & Sunnah) before secondary consensus (*Ijma*) and analogical deduction (*Qiyas*).\n" +
                        "3. **Practical Counsel:** Strive to adhere to the most cautious and spiritually protective interpretation (*Al-Ihtiyat*)."
                }
            }

            GeminiChatbotRole.FAST_FINDER -> {
                when {
                    lower.contains("travel") ->
                        "**Dua for Travel (Safar):**\n\n" +
                        "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ\n\n" +
                        "*Subhana-lladhi sakh-khara lana hadha wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun.*\n\n" +
                        "**Meaning:** \"Glory be to Him Who has subjected this to us, whereas we could not have otherwise subdued it, and indeed to our Lord we shall return.\" [Surah Az-Zukhruf 43:13-14 | Sahih Muslim 1342]."

                    lower.contains("sleep") || lower.contains("waking") ->
                        "**Dua upon Waking Up:**\n\n" +
                        "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ\n\n" +
                        "*Al-hamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur.*\n\n" +
                        "**Source:** Sahih al-Bukhari 6312."

                    else ->
                        "**Instant Hadith & Dua Lookup (${role.displayModel}):**\n\n" +
                        "• **Prophetic Teaching:** The Messenger of Allah (ﷺ) said: *\"The most beloved deeds to Allah are those that are consistent, even if they are few.\"* [Sahih al-Bukhari 6464, Sahih Muslim 783].\n" +
                        "• **Key Takeaway:** Sincerity (*Ikhlas*) and consistency outweigh volume in worship."
                }
            }

            GeminiChatbotRole.GENERAL_COMPANION -> {
                "As-salamu alaykum wa Rahmatullah!\n\n" +
                "Thank you for sharing your reflection: *\"$prompt\"*.\n\n" +
                "The Holy Quran reminds us in Surah Al-Baqarah (2:186): *\"And when My servants ask you concerning Me, indeed I am near. I respond to the invocation of the supplicant when he calls upon Me.\"*\n\n" +
                "May Allah bless your heart with tranquility (*Sakinah*), steadfast faith (*Iman*), and grant ease in all your endeavors. Please feel free to ask follow-up questions!"
            }
        }
    }
}

/**
 * Real-time Islamic AI Voice Synthesis Engine using Android TextToSpeech.
 * Provides voice reading of AI responses and Quranic/Arabic verses with pitch & speech rate tuning.
 */
class IslamicVoiceEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isSpeaking = false
        private set

    var onSpeakingStateChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.apply {
                    // Try Arabic/UK/US high-quality serene voice
                    val preferredLocales = listOf(
                        Locale("ar"),
                        Locale("en", "GB"),
                        Locale("en", "US"),
                        Locale.getDefault()
                    )
                    for (loc in preferredLocales) {
                        if (isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
                            language = loc
                            break
                        }
                    }
                    setPitch(0.96f) // Soothing, calm scholar tone
                    setSpeechRate(0.92f) // Gentle, clear articulation
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        onSpeakingStateChanged?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "ai_voice_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) return

        try {
            // Clean markdown stars/bullets for clean audio reading
            val cleanSpeech = text
                .replace(Regex("[#*`_~]"), "")
                .replace(Regex("📖\\s*"), "")
                .replace(Regex("\\n+"), ". ")
                .trim()

            if (cleanSpeech.isBlank()) return

            stop()
            tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (_: Exception) {
            isSpeaking = false
            onSpeakingStateChanged?.invoke(false)
        }
    }

    fun stop() {
        try {
            if (isSpeaking) {
                tts?.stop()
                isSpeaking = false
                onSpeakingStateChanged?.invoke(false)
            }
        } catch (_: Exception) {
            isSpeaking = false
            onSpeakingStateChanged?.invoke(false)
        }
    }

    fun shutdown() {
        try {
            stop()
        } catch (_: Exception) {}
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
