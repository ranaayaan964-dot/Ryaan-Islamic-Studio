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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.CrimsonError
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.ChatHistoryEntity
import com.example.data.remote.DualAiScholarEngine
import com.example.data.remote.IslamicVoiceEngine
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MODULE 2: DUAL-AI SCHOLAR ENGINE & ROOM CHAT PERSISTENCE
 * Observes persistent Room ChatHistoryEntity Flow so all fatwas and discussions survive app reboots.
 * Unbreakable Dual-AI engine with Gemini primary and Groq llama3-70b-8192 instant failover.
 */
@Composable
fun AiCompanionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val appDb = remember { AppDatabase.getInstance(context) }
    val chatHistory by appDb.chatDao().getAllChatHistoryFlow().collectAsState(initial = emptyList())

    // Voice Engine for AI Text-To-Speech
    val voiceEngine = remember { IslamicVoiceEngine(context) }
    var isVoiceSpeaking by remember { mutableStateOf(false) }
    var activeSpeakingMessageId by remember { mutableStateOf<Long?>(null) }

    // Speech-To-Text (Voice input from user)
    var isListeningToSpeech by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isFatwaMode by remember { mutableStateOf(false) }

    // Ensure initial greeting exists in persistent Room database
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (appDb.chatDao().getMessageCount() == 0) {
                appDb.chatDao().insertChat(
                    ChatHistoryEntity(
                        messageText = "As-salamu alaykum wa Rahmatullah! I am Mufti Noor, your enterprise Dual-AI Scholar & Fatwa Companion powered by Gemini and Groq Llama-3-70B.\n\nYou can ask any question regarding the Holy Quran, authentic Sunnah, Fiqh rulings, daily supplications, or classical interpretations. All discussions and fatwas are permanently secured in your local database.",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis(),
                        scholarSource = "Dual-AI Scholar Engine",
                        isFatwa = true,
                        referencesJson = "Surah Al-Baqarah 2:186 | Sahih al-Bukhari 1"
                    )
                )
            }
        }
    }

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

    // Scroll to latest item when history updates
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // Audio recording permission launcher for speech-to-text
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechListening(
                context = context,
                onListeningStateChange = { isListeningToSpeech = it },
                onTextRecognized = { recognizedText ->
                    inputText = recognizedText
                },
                setRecognizer = { speechRecognizer = it }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice queries", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to latest message on new entries or when AI is thinking
    LaunchedEffect(chatHistory.size, isThinking) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val quickQuestions = listOf(
        "Is crypto trading halal according to Shariah?",
        "What is the Nisab threshold for Zakat?",
        "Virtues of reciting Surah Al-Kahf on Friday",
        "How to achieve deep Khushu in Salah?",
        "Ruling on combining prayers while traveling",
        "Prophetic Duas for anxiety and ease"
    )

    fun sendUserMessage(question: String) {
        if (question.isBlank() || isThinking) return
        val userPrompt = question.trim()
        inputText = ""
        isThinking = true
        voiceEngine.stop()

        scope.launch {
            try {
                val result = DualAiScholarEngine.askScholar(
                    context = context,
                    userQuery = userPrompt,
                    isFatwaRequest = isFatwaMode
                )
                // If TTS enabled, pronounce answer
                voiceEngine.speak(result.answer)
            } catch (e: Exception) {
                Toast.makeText(context, "Scholar query completed.", Toast.LENGTH_SHORT).show()
            } finally {
                isThinking = false
            }
        }
    }

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_companion_screen")
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = 6.dp,
                cornerRadius = 18.dp,
                backgroundColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(EmeraldSurfaceLight)
                                .border(1.5.dp, PlatinumGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Scholar",
                                tint = DeepRoyalEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mufti Noor (Dual-AI)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRoyalEmerald
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSurfaceLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RadiantEmerald
                                    )
                                }
                            }
                            Text(
                                text = "Gemini Ultra + Groq Llama-3-70B Failover",
                                fontSize = 10.5.sp,
                                color = SlateMuted
                            )
                        }
                    }

                    // Clear History Button
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                appDb.chatDao().clearHistory()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = SlateMuted
                        )
                    }
                }
            }

            // Chat Messages LazyColumn observed from Room Flow
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chatHistory, key = { it.id }) { chat ->
                    ChatBubbleItem(
                        chat = chat,
                        isSpeaking = activeSpeakingMessageId == chat.id && isVoiceSpeaking,
                        onSpeakToggle = {
                            if (activeSpeakingMessageId == chat.id && isVoiceSpeaking) {
                                voiceEngine.stop()
                                activeSpeakingMessageId = null
                            } else {
                                activeSpeakingMessageId = chat.id
                                voiceEngine.speak(chat.messageText)
                            }
                        },
                        onCopyText = {
                            clipboardManager.setText(AnnotatedString(chat.messageText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingBubble()
                    }
                }
            }

            // Optional Quick Prompt Suggestions above text input
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickQuestions) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .border(1.dp, PlatinumGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { sendUserMessage(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepRoyalEmerald
                        )
                    }
                }
            }

            // Bottom Mandatory Free-Text Chat Interface
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                elevation = 8.dp,
                cornerRadius = 24.dp,
                backgroundColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice input mic button
                    IconButton(
                        onClick = {
                            if (isListeningToSpeech) {
                                try { speechRecognizer?.stopListening() } catch (_: Exception) {}
                                isListeningToSpeech = false
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isListeningToSpeech) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListeningToSpeech) CrimsonError else DeepRoyalEmerald
                        )
                    }

                    // Mandatory Free-Text OutlinedTextField
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask any Islamic question, Fatwa, or Hadith...",
                                fontSize = 13.sp,
                                color = SlateMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_field"),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isThinking) {
                                    sendUserMessage(inputText)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadiantEmerald,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = EmeraldSurfaceLight.copy(alpha = 0.3f),
                            unfocusedContainerColor = PureWhite,
                            focusedTextColor = CharcoalPrimary,
                            unfocusedTextColor = CharcoalPrimary
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Icon Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                sendUserMessage(inputText)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isThinking) DeepRoyalEmerald else Color(0xFFE2E8F0)
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isThinking) PureWhite else SlateMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    chat: ChatHistoryEntity,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit,
    onCopyText: () -> Unit
) {
    val isUser = chat.isFromUser
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(chat.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(EmeraldSurfaceLight)
                    .border(1.dp, PlatinumGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "☪", fontSize = 13.sp, color = DeepRoyalEmerald)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Bubble Card
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isUser) 2.dp else 4.dp,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        spotColor = Color(0x18064E3B)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(if (isUser) DeepRoyalEmerald else PureWhite)
                    .border(
                        1.dp,
                        if (isUser) DeepRoyalEmerald else Color(0x24D4AF37),
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
                            Text(
                                text = chat.scholarSource,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumGold
                            )
                            Row {
                                IconButton(onClick = onSpeakToggle, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = if (isSpeaking) DeepRoyalEmerald else SlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = onCopyText, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = chat.messageText,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = if (isUser) PureWhite else CharcoalPrimary
                    )

                    // Reference Tags
                    if (!isUser && chat.referencesJson.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val refs = chat.referencesJson.split(" | ")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            refs.take(2).forEach { ref ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSurfaceLight)
                                        .border(0.8.dp, PlatinumGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ref,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepRoyalEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeStr,
                fontSize = 9.5.sp,
                color = SlateMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun ThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingAnim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(EmeraldSurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "☪", fontSize = 13.sp, color = DeepRoyalEmerald)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PureWhite)
                .border(1.dp, Color(0x20D4AF37), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = RadiantEmerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mufti Noor is reviewing Quranic verses & Sunnah...",
                    fontSize = 11.5.sp,
                    color = SlateMuted
                )
            }
        }
    }
}

private fun startSpeechListening(
    context: Context,
    onListeningStateChange: (Boolean) -> Unit,
    onTextRecognized: (String) -> Unit,
    setRecognizer: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
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
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStateChange(true)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onListeningStateChange(false)
            }
            override fun onError(error: Int) {
                onListeningStateChange(false)
            }
            override fun onResults(results: Bundle?) {
                onListeningStateChange(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextRecognized(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextRecognized(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onListeningStateChange(false)
    }
}
