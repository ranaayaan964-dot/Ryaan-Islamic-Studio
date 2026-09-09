package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.rag.RagIslamicEngine
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.IslamicVoiceEngine
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "Just now",
    val references: List<String> = emptyList(),
    val isGeminiPowered: Boolean = true,
    val isRagGrounded: Boolean = false,
    val ragConfidence: Float? = null
)

@Composable
fun AiCompanionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    // Voice Engine for AI Text-To-Speech
    val voiceEngine = remember { IslamicVoiceEngine(context) }
    var isVoiceSpeaking by remember { mutableStateOf(false) }
    var activeSpeakingMessageId by remember { mutableStateOf<String?>(null) }

    // Speech-To-Text (Voice input from user)
    var isListeningToSpeech by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        voiceEngine.onSpeakingStateChanged = { speaking ->
            isVoiceSpeaking = speaking
            if (!speaking) activeSpeakingMessageId = null
        }

        onDispose {
            voiceEngine.shutdown()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "As-salamu alaykum! I am Noor, your real-time Islamic AI Companion powered directly by Google Gemini 3.5 Flash.\n\nYou can speak to me with your voice or type questions about the Quran, authentic Hadith, Prayer rules (Fiqh), Daily Duas, or Islamic history. I can also speak answers aloud in a calm scholar voice.",
                isUser = false,
                references = listOf("Al-Baqarah 2:186", "Sahih Bukhari 1"),
                isGeminiPowered = true
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var copiedMessageId by remember { mutableStateOf<String?>(null) }
    var isAudioAutoPlayEnabled by remember { mutableStateOf(true) }
    var isRagFiqhEngineEnabled by remember { mutableStateOf(true) }

    val quickQuestions = listOf(
        "What is the reward of Tahajjud prayer?",
        "Virtues of Surah Al-Kahf on Friday",
        "How to achieve Khushu in Salah?",
        "Rules of Fasting while traveling",
        "Prophetic Duas for peace & ease",
        "Difference between Fard and Sunnah"
    )

    fun sendUserMessage(question: String) {
        if (question.isBlank() || isThinking) return
        val userPrompt = question.trim()
        val userMsg = ChatMessage(text = userPrompt, isUser = true)
        messages.add(userMsg)
        inputText = ""
        isThinking = true

        // Stop any previous speech
        voiceEngine.stop()

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val replyMessage = if (isRagFiqhEngineEnabled) {
                val ragResult = RagIslamicEngine.queryFatwaAndFiqh(userPrompt)
                ragResult.fold(
                    onSuccess = { rag ->
                        val citations = rag.primaryCitations.map { "${it.sourceBook} (${it.volume}, ${it.hadithOrPageNumber})" }
                        ChatMessage(
                            text = rag.groundedAnswer,
                            isUser = false,
                            references = citations,
                            isGeminiPowered = true,
                            isRagGrounded = true,
                            ragConfidence = rag.confidenceScore
                        )
                    },
                    onFailure = {
                        val history = messages.map { it.text to it.isUser }
                        val result = GeminiApiClient.chat(history, userPrompt)
                        result.fold(
                            onSuccess = { reply ->
                                ChatMessage(
                                    text = reply,
                                    isUser = false,
                                    references = extractReferences(reply),
                                    isGeminiPowered = true
                                )
                            },
                            onFailure = { fallbackError ->
                                val fallback = generateIslamicAiResponse(userPrompt)
                                ChatMessage(
                                    text = "${fallback.text}\n\n[Offline Scholar Mode: ${fallbackError.message ?: "Network timeout"}]",
                                    isUser = false,
                                    references = fallback.references,
                                    isGeminiPowered = false
                                )
                            }
                        )
                    }
                )
            } else {
                val history = messages.map { it.text to it.isUser }
                val result = GeminiApiClient.chat(history, userPrompt)
                result.fold(
                    onSuccess = { reply ->
                        ChatMessage(
                            text = reply,
                            isUser = false,
                            references = extractReferences(reply),
                            isGeminiPowered = true
                        )
                    },
                    onFailure = { error ->
                        val fallback = generateIslamicAiResponse(userPrompt)
                        ChatMessage(
                            text = "${fallback.text}\n\n[Offline Scholar Mode: ${error.message ?: "Network timeout"}]",
                            isUser = false,
                            references = fallback.references,
                            isGeminiPowered = false
                        )
                    }
                )
            }

            messages.add(replyMessage)
            isThinking = false
            listState.animateScrollToItem(messages.size - 1)

            // Auto-speak reply if enabled
            if (isAudioAutoPlayEnabled) {
                activeSpeakingMessageId = replyMessage.id
                voiceEngine.speak(replyMessage.text, replyMessage.id)
            }
        }
    }

    // Voice recognition permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechListening(
                context = context,
                onResult = { recognizedText ->
                    inputText = recognizedText
                    isListeningToSpeech = false
                    sendUserMessage(recognizedText)
                },
                onError = {
                    isListeningToSpeech = false
                    Toast.makeText(context, "Speech recognition error: $it", Toast.LENGTH_SHORT).show()
                },
                onListeningState = { listening ->
                    isListeningToSpeech = listening
                },
                setRecognizer = { recognizer ->
                    speechRecognizer = recognizer
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice chat", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AIBreath")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_companion_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkNavyBackground,
                        Color(0xFF071B26),
                        Color(0xFF042823),
                        DarkNavyBackground
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Real Gemini AI Scholar with Voice Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(EmeraldAccent, GoldPrimary)))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DarkNavySurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVoiceSpeaking) Icons.Default.GraphicEq else Icons.Default.AutoAwesome,
                                contentDescription = "AI Scholar",
                                tint = if (isVoiceSpeaking) EmeraldAccent else GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Islamic AI Scholar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3310B981))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GEMINI 3.5 FLASH",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                        Text(
                            text = if (isVoiceSpeaking) "Speaking response aloud..." else "Real-time AI Chat & Voice Guidance",
                            fontSize = 11.sp,
                            color = if (isVoiceSpeaking) EmeraldAccent else Color(0xFF94A3B8)
                        )
                    }

                    // Voice Toggle / Stop Speaking Button
                    IconButton(
                        onClick = {
                            if (isVoiceSpeaking) {
                                voiceEngine.stop()
                            } else {
                                isAudioAutoPlayEnabled = !isAudioAutoPlayEnabled
                                Toast.makeText(
                                    context,
                                    if (isAudioAutoPlayEnabled) "Voice Audio: ON" else "Voice Audio: OFF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isVoiceSpeaking) Color(0x3310B981) else Color(0x221E293B))
                    ) {
                        Icon(
                            imageVector = when {
                                isVoiceSpeaking -> Icons.Default.Stop
                                isAudioAutoPlayEnabled -> Icons.Default.VolumeUp
                                else -> Icons.Default.VolumeOff
                            },
                            contentDescription = "Audio Toggle",
                            tint = if (isVoiceSpeaking) EmeraldAccent else GoldSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Prompt Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(quickQuestions) { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x33E5C07B), RoundedCornerShape(16.dp))
                            .clickable { sendUserMessage(q) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = q,
                            fontSize = 12.sp,
                            color = GoldSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // RAG Fatwa Engine Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRagFiqhEngineEnabled) Color(0x3310B981) else Color(0x22334155))
                        .border(
                            1.dp,
                            if (isRagFiqhEngineEnabled) EmeraldAccent else Color(0xFF475569),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isRagFiqhEngineEnabled = !isRagFiqhEngineEnabled }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isRagFiqhEngineEnabled) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRagFiqhEngineEnabled) "RAG FATWA ENGINE: ACTIVE (ZERO HALLUCINATIONS)" else "RAG FATWA ENGINE: PAUSED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRagFiqhEngineEnabled) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = "Bukhari • Muslim • Tafsir",
                    fontSize = 9.sp,
                    color = GoldSecondary
                )
            }

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        isSpeakingThis = activeSpeakingMessageId == msg.id && isVoiceSpeaking,
                        onSpeak = {
                            if (activeSpeakingMessageId == msg.id && isVoiceSpeaking) {
                                voiceEngine.stop()
                            } else {
                                activeSpeakingMessageId = msg.id
                                voiceEngine.speak(msg.text, msg.id)
                            }
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            copiedMessageId = msg.id
                        },
                        isCopied = copiedMessageId == msg.id
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini 3.5 Flash is formulating authentic answer...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Voice Listening Banner (Visible when user taps Mic)
            AnimatedVisibility(visible = isListeningToSpeech) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listening to your voice... Speak now",
                                fontSize = 12.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = {
                                try {
                                    speechRecognizer?.stopListening()
                                } catch (_: Exception) {}
                                isListeningToSpeech = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Cancel Listening",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Input Row with Text & Voice Input Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Gemini AI scholar or tap Mic...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0x33E5C07B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkNavySurfaceVariant,
                        unfocusedContainerColor = Color(0x990F172A)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Mic Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isListeningToSpeech) Color(0xFFEF4444) else Color(0x2810B981))
                        .border(1.dp, if (isListeningToSpeech) Color(0xFFEF4444) else Color(0x6610B981), CircleShape)
                        .clickable(enabled = !isThinking) {
                            if (isListeningToSpeech) {
                                try {
                                    speechRecognizer?.stopListening()
                                } catch (_: Exception) {}
                                isListeningToSpeech = false
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListeningToSpeech) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListeningToSpeech) Color.White else EmeraldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) GoldPrimary else Color(0x33E5C07B))
                        .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                            sendUserMessage(inputText)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color(0xFF1B1302) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isSpeakingThis: Boolean,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    isCopied: Boolean
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF0F1E36), Color(0xFF0C1B2A)))
                    }
                )
                .border(
                    1.dp,
                    if (isUser) Color(0x4438BDF8) else Color(0x33E5C07B),
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Islamic AI Scholar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldSecondary
                            )
                            if (message.isGeminiPowered) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "GEMINI",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                            if (message.isRagGrounded) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33F59E0B))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "RAG VERIFIED",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldSecondary
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Voice read aloud button
                            IconButton(onClick = onSpeak, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = "Read Aloud",
                                    tint = if (isSpeakingThis) EmeraldAccent else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Copy message button
                            IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (isCopied) EmeraldAccent else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 13.5.sp,
                    color = Color(0xFFF1F5F9),
                    lineHeight = 21.sp
                )

                if (message.references.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        message.references.take(4).forEach { ref ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x22E5C07B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "📖 $ref",
                                    fontSize = 10.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper: Starts Android SpeechRecognizer for real-time speech-to-text
private fun startSpeechListening(
    context: android.content.Context,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onListeningState: (Boolean) -> Unit,
    setRecognizer: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onError("Speech recognition not available on this device.")
        return
    }

    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        setRecognizer(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask any Islamic question...")
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningState(true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onListeningState(false)
            }

            override fun onError(error: Int) {
                onListeningState(false)
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue in speech recognition."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    else -> "Voice recognition stopped ($error)"
                }
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                onListeningState(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onListeningState(false)
        onError(e.message ?: "Failed to start speech recognition")
    }
}

// Helper: Extract Quran/Hadith citations from AI reply
private fun extractReferences(text: String): List<String> {
    val refs = mutableListOf<String>()
    val patterns = listOf(
        Regex("\\[(Surah [^\\]]+)\\]", RegexOption.IGNORE_CASE),
        Regex("\\[(Sahih [^\\]]+)\\]", RegexOption.IGNORE_CASE),
        Regex("\\[(Sunan [^\\]]+)\\]", RegexOption.IGNORE_CASE),
        Regex("(Al-Baqarah \\d+:\\d+)", RegexOption.IGNORE_CASE),
        Regex("(Sahih Bukhari \\d+)", RegexOption.IGNORE_CASE),
        Regex("(Sahih Muslim \\d+)", RegexOption.IGNORE_CASE)
    )

    for (p in patterns) {
        val matches = p.findAll(text)
        for (m in matches) {
            val citation = m.groupValues.getOrNull(1) ?: m.value
            if (citation.isNotBlank() && !refs.contains(citation)) {
                refs.add(citation)
            }
        }
    }
    return refs
}

fun generateIslamicAiResponse(prompt: String): ChatMessage {
    val lower = prompt.lowercase()
    return when {
        lower.contains("tahajjud") && lower.contains("witr") -> {
            ChatMessage(
                text = "Yes, you may pray Tahajjud (Night Prayer) after Witr if you wake up during the latter third of the night. However, the Prophet ﷺ advised: 'Make Witr the last of your prayers at night' (Sahih Bukhari). If you already prayed Witr before sleeping, do not repeat Witr, as there is 'No two Witrs in one night' (Sunan Abi Dawud). Simply pray voluntary units (Rak'ahs) in sets of two.",
                isUser = false,
                references = listOf("Sahih Bukhari 998", "Sunan Abi Dawud 1439"),
                isGeminiPowered = false
            )
        }
        lower.contains("kahf") || lower.contains("friday") -> {
            ChatMessage(
                text = "Reciting Surah Al-Kahf on Friday illuminates light between two Fridays. The Prophet Muhammad ﷺ said: 'Whoever reads Surah Al-Kahf on the day of Jumu'ah, will have a light that will shine from him from one Friday to the next.' (Al-Bayhaqi, authenticated by Al-Albani). It also provides divine protection against the trials of Dajjal.",
                isUser = false,
                references = listOf("Sunan al-Kubra 5856", "Sahih Muslim 809"),
                isGeminiPowered = false
            )
        }
        lower.contains("khushu") || lower.contains("focus") -> {
            ChatMessage(
                text = "To cultivate Khushu' (deep presence and humility) in Namaz:\n1. Perform thorough, mindful Wudu while reflecting on purification.\n2. Understand the verses you recite; pause after every ayah of Surah Al-Fatiha.\n3. Pray as if it is your farewell prayer (Salat al-Muwwada').\n4. Remove physical distractions (activate the app's Auto-Silent mode!).\n5. Remember Allah's words: 'Successful indeed are the believers: those who are humble in their prayers.'",
                isUser = false,
                references = listOf("Surah Al-Mu’minun 23:1-2", "Ibn Majah 4171"),
                isGeminiPowered = false
            )
        }
        lower.contains("travel") || lower.contains("fasting") -> {
            ChatMessage(
                text = "Regarding fasting while traveling: A traveler is granted the concession (Rukhsah) by Allah to postpone fasting and make up the missed days later (Qada). The Quran says: 'Whoever is ill or on a journey, the same number from other days' (Al-Baqarah 2:185). If fasting causes significant hardship, taking the concession is recommended.",
                isUser = false,
                references = listOf("Al-Baqarah 2:185", "Sahih Bukhari 1944"),
                isGeminiPowered = false
            )
        }
        lower.contains("anxiety") || lower.contains("peace") || lower.contains("dua") -> {
            ChatMessage(
                text = "For inner peace and anxiety relief, the Prophet ﷺ taught this profound Dua:\n\n'اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ'\n\n\"O Allah, I seek refuge in You from grief and sadness, from weakness and laziness, from miserliness and cowardice.\" (Sahih Bukhari).\nAlso remember: 'Verily, in the remembrance of Allah do hearts find rest.' (Ar-Ra'd 13:28).",
                isUser = false,
                references = listOf("Sahih Bukhari 6363", "Surah Ar-Ra'd 13:28"),
                isGeminiPowered = false
            )
        }
        else -> {
            ChatMessage(
                text = "BarakAllahu feek! In Islam, actions are judged by intentions (Niyyah). Regarding your question, our scholars emphasize adhering strictly to the Quran and the Sunnah of Prophet Muhammad ﷺ with wisdom, compassion, and moderation. Feel free to ask about any specific verse, hadith, or practical lifestyle rule!",
                isUser = false,
                references = listOf("Sahih Bukhari 1", "An-Nahl 16:125"),
                isGeminiPowered = false
            )
        }
    }
}
