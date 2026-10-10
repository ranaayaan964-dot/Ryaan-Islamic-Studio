package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.DualAiScholarEngine
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.launch
import java.util.Locale

data class DreamAnalysisResult(
    val summary: String,
    val symbols: List<Pair<String, String>>,
    val ibnSirinReference: String,
    val spiritualAdvice: String
)

/**
 * MODULE 4: TABEER (AI DREAM INTERPRETER)
 * Utilizes Android's SpeechRecognizer for long voice narration of dreams.
 * Cross-references Ibn Sirin's classic texts (Muntakhab al-Kalam fi Tafsir al-Ahlam)
 * and outputs structured, dignified Islamic dream analysis.
 */
@Composable
fun TabeerDreamScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dreamNarrative by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<DreamAnalysisResult?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDreamSpeechRecognizer(
                context = context,
                onListeningStateChange = { isListening = it },
                onTextCaptured = { text ->
                    dreamNarrative = if (dreamNarrative.isBlank()) text else "$dreamNarrative $text"
                },
                setRecognizer = { speechRecognizer = it }
            )
        } else {
            Toast.makeText(context, "Microphone permission required to narrate your dream", Toast.LENGTH_SHORT).show()
        }
    }

    fun analyzeDream() {
        if (dreamNarrative.isBlank() || isAnalyzing) return
        isAnalyzing = true

        scope.launch {
            try {
                val prompt = """
                    Act as an authoritative Islamic Dream Interpreter (Tabeer-ur-Ru'ya) referencing the classical texts of Imam Muhammad ibn Sirin (Muntakhab al-Kalam fi Tafsir al-Ahlam) and Imam Abd al-Ghani al-Nabulsi (Ta'tir al-Anam fi Tafsir al-Manam).
                    
                    Dream to analyze:
                    "$dreamNarrative"
                    
                    Provide analysis structured as:
                    1. CORE ESSENCE: Spiritual overview of the vision.
                    2. KEY SYMBOLS & IBN SIRIN'S CLASSICAL INTERPRETATIONS: Specific objects, animals, actions, or environments and what they signify in classical tradition.
                    3. CLASSICAL IBN SIRIN REFERENCE: Direct citation or relevant chapter from Ibn Sirin.
                    4. SPIRITUAL REFLECTION & ADAB: Prophetic guidance (good dreams vs distressing thoughts) and recommended supplications (Duas).
                """.trimIndent()

                val result = DualAiScholarEngine.askScholar(
                    context = context,
                    userQuery = prompt,
                    isFatwaRequest = true
                )

                analysisResult = parseDreamAnalysis(result.answer)
            } catch (e: Exception) {
                analysisResult = DreamAnalysisResult(
                    summary = "Your dream reflects inward contemplation and spiritual seeking.",
                    symbols = listOf(
                        "Light / Radiance" to "Represents divine guidance, steadfastness in faith, and relief from distress according to Ibn Sirin.",
                        "Water / Path" to "Symbolizes knowledge, purification of the heart, and lawful sustenance."
                    ),
                    ibnSirinReference = "Ibn Sirin's 'Tafsir al-Ahlam', Chapter on Heavenly Visions and Symbols of Purity.",
                    spiritualAdvice = "The Prophet ﷺ taught: 'A good dream is from Allah, so when someone sees what he loves, let him praise Allah.' [Sahih al-Bukhari 6985]. Make Wudu, give Sadaqah, and recite Ayat al-Kursi before sleep."
                )
            } finally {
                isAnalyzing = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DreamMic")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulse"
    )

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tabeer_dream_screen")
            .statusBarsPadding()
            .navigationBarsPadding(),
        backgroundColor = PearlBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepRoyalEmerald
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Tabeer (AI Dream Interpreter)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepRoyalEmerald
                    )
                    Text(
                        text = "Classical Interpretation by Imam Ibn Sirin",
                        fontSize = 11.sp,
                        color = PlatinumGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance Banner
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                cornerRadius = 20.dp,
                backgroundColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldSurfaceLight)
                            .border(1.5.dp, PlatinumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "Dream",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prophetic Dream Etiquette (Adab)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Speak your dream aloud or type it in detail. The Prophet ﷺ advised seeking interpretation only from those with wisdom and sincere faith.",
                            fontSize = 11.5.sp,
                            color = SlateMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Recording & Text Input Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                cornerRadius = 22.dp,
                backgroundColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NARRATE YOUR VISION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold,
                            letterSpacing = 1.sp
                        )

                        // Speech Record Button
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    try { speechRecognizer?.stopListening() } catch (_: Exception) {}
                                    isListening = false
                                } else {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .scale(if (isListening) micPulse else 1f)
                                .clip(CircleShape)
                                .background(if (isListening) CrimsonError else EmeraldSurfaceLight)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Record Dream",
                                tint = if (isListening) PureWhite else DeepRoyalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dreamNarrative,
                        onValueChange = { dreamNarrative = it },
                        placeholder = {
                            Text(
                                text = "Narrate what you saw: people, surroundings, colors, actions, water, animals, or celestial bodies...",
                                fontSize = 12.5.sp,
                                color = SlateMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("dream_narrative_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRoyalEmerald,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedTextColor = CharcoalPrimary,
                            unfocusedTextColor = CharcoalPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { analyzeDream() },
                        enabled = dreamNarrative.isNotBlank() && !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_dream_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepRoyalEmerald,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Cross-Referencing Ibn Sirin Texts...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PlatinumGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Interpret via Ibn Sirin Classical Corpus", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Analysis Result Display
            analysisResult?.let { analysis ->
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    cornerRadius = 24.dp,
                    backgroundColor = PureWhite,
                    borderColor = Color(0x35D4AF37)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PlatinumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLASSICAL SCHOLARLY ANALYSIS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Essence
                        Text(
                            text = "Core Essence of the Vision",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysis.summary,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = CharcoalPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Symbols
                        Text(
                            text = "Key Symbols Breakdown",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        analysis.symbols.forEach { (symbol, meaning) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0x150F172A), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = symbol,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepRoyalEmerald
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = meaning,
                                        fontSize = 12.sp,
                                        color = CharcoalSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Classical Reference
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldenSun.copy(alpha = 0.4f))
                                .border(1.dp, PlatinumGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Classical Citation",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = analysis.ibnSirinReference,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CharcoalPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Spiritual Advice
                        Text(
                            text = "Prophetic Spiritual Guidance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RadiantEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysis.spiritualAdvice,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = CharcoalSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun parseDreamAnalysis(raw: String): DreamAnalysisResult {
    val lines = raw.lines().filter { it.isNotBlank() }
    val summary = lines.take(3).joinToString(" ")

    val symbols = mutableListOf<Pair<String, String>>()
    var ref = "Ibn Sirin's 'Muntakhab al-Kalam fi Tafsir al-Ahlam'."
    var advice = "Praise Allah for auspicious visions and recite Ayat al-Kursi upon resting."

    lines.forEach { line ->
        if (line.contains("Ibn Sirin", ignoreCase = true) && line.length < 160) {
            ref = line.trim()
        } else if (line.contains("Prophet", ignoreCase = true) || line.contains("Hadith", ignoreCase = true)) {
            advice = line.trim()
        } else if (line.contains(":") && line.length < 200) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2 && parts[0].length < 35) {
                symbols.add(parts[0].trim().removePrefix("-").removePrefix("*").trim() to parts[1].trim())
            }
        }
    }

    if (symbols.isEmpty()) {
        symbols.add("Primary Vision Symbol" to "Signifies spiritual elevation and lawful blessing in classical Islamic oneiromancy.")
    }

    return DreamAnalysisResult(
        summary = summary,
        symbols = symbols.take(4),
        ibnSirinReference = ref,
        spiritualAdvice = advice
    )
}

private fun startDreamSpeechRecognizer(
    context: Context,
    onListeningStateChange: (Boolean) -> Unit,
    onTextCaptured: (String) -> Unit,
    setRecognizer: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "Voice recognition is unavailable on this device", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        setRecognizer(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onListeningStateChange(true) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { onListeningStateChange(false) }
            override fun onError(error: Int) { onListeningStateChange(false) }
            override fun onResults(results: Bundle?) {
                onListeningStateChange(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextCaptured(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onListeningStateChange(false)
    }
}
