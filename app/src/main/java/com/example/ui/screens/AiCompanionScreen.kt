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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.ChatHistoryEntity
import com.example.data.remote.DualAiScholarEngine
import com.example.data.remote.GeminiChatbotRole
import com.example.data.remote.IslamicVoiceEngine
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multi-Turn Chat Interface using Gemini Models:
 * - Particularly Complex Tasks: gemini-3.1-pro-preview (Grand Mufti & Jurist)
 * - General Tasks: gemini-3.5-flash (Spiritual Companion 'Noor')
 * - Fast Tasks: gemini-3.1-flash-lite (Instant Hadith & Dua Finder)
 *
 * Maintains full multi-turn conversation history across turns,
 * displays messages in a scrollable thread, and enforces dedicated
 * role system instructions.
 */
@Composable
fun AiCompanionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val appDb = remember { AppDatabase.getInstance(context) }
    val chatHistory by appDb.chatDao().getAllChatHistoryFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val messages = remember { mutableStateListOf<ChatMessage>() }

    LaunchedEffect(chatHistory) {
        if (messages.isEmpty() && chatHistory.isNotEmpty()) {
            messages.clear()
            messages.addAll(chatHistory.map { ChatMessage(it.messageText, it.isFromUser) })
        }
    }

    // Active Chatbot Role State
    var selectedRole by remember { mutableStateOf(GeminiChatbotRole.GENERAL_COMPANION) }
    var isAutoDetectMode by remember { mutableStateOf(true) }
    var showRoleInfoDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Voice Engine for AI Text-To-Speech
    val voiceEngine = remember { IslamicVoiceEngine(context) }
    var isVoiceSpeaking by remember { mutableStateOf(false) }
    var activeSpeakingMessageId by remember { mutableStateOf<Long?>(null) }

    // Speech-To-Text (Voice input from user)
    var isListeningToSpeech by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var thinkingModelName by remember { mutableStateOf(selectedRole.displayModel) }

    // Ensure initial greeting exists in persistent Room database
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (appDb.chatDao().getMessageCount() == 0) {
                appDb.chatDao().insertChat(
                    ChatHistoryEntity(
                        messageText = "As-salamu alaykum wa Rahmatullah! I am your AI Islamic Companion. You can converse across multiple turns with specialized roles:\n\n" +
                                "• 🧠 Grand Mufti (gemini-3.1-pro-preview) for complex Fiqh rulings & deep reasoning\n" +
                                "• 🌟 Spiritual Companion (gemini-3.5-flash) for daily Quran reflection & guidance\n" +
                                "• ⚡ Fast Finder (gemini-3.1-flash-lite) for instant Hadith & Duas\n\n" +
                                "Ask any question to begin our conversation.",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis(),
                        scholarSource = "Gemini Companion (gemini-3.5-flash)",
                        isFatwa = false,
                        referencesJson = "Surah Al-Baqarah 2:186"
                    )
                )
            }
        }
    }

    fun stopVoiceInput() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListeningToSpeech = false
    }

    DisposableEffect(Unit) {
        voiceEngine.onSpeakingStateChanged = { speaking ->
            scope.launch(Dispatchers.Main.immediate) {
                isVoiceSpeaking = speaking
                if (!speaking) activeSpeakingMessageId = null
            }
        }

        onDispose {
            voiceEngine.shutdown()
            stopVoiceInput()
        }
    }

    // Scroll to latest item when history updates or when AI starts thinking
    LaunchedEffect(chatHistory.size, isThinking) {
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
                onListeningStateChange = { value ->
                    scope.launch(Dispatchers.Main.immediate) {
                        isListeningToSpeech = value
                    }
                },
                onTextRecognized = { recognizedText ->
                    scope.launch(Dispatchers.Main.immediate) {
                        inputText = recognizedText
                    }
                },
                setRecognizer = { speechRecognizer = it }
            )
        } else {
            Toast.makeText(context, "Microphone permission is required for voice queries", Toast.LENGTH_SHORT).show()
        }
    }

    // Role-dependent quick prompt suggestions
    val quickQuestions = when (selectedRole) {
        GeminiChatbotRole.COMPLEX_JURIST -> listOf(
            "Ruling on crypto trading according to classical Fiqh?",
            "How is inheritance calculated with daughters and siblings?",
            "Comparison of travel prayer (Qasr) rules among 4 Madhabs",
            "Fiqh ruling on mortgage vs Islamic home financing"
        )
        GeminiChatbotRole.FAST_FINDER -> listOf(
            "Dua for travel with Arabic and translation",
            "Authentic Hadith on consistency in good deeds",
            "Dua when waking up from sleep",
            "Quick prayer calculation rule for Fajr"
        )
        GeminiChatbotRole.GENERAL_COMPANION -> listOf(
            "How to maintain spiritual consistency after Ramadan?",
            "Reflection on Surah Ad-Duha and dealing with grief",
            "Etiquettes of making Dua that gets answered",
            "Advice on keeping Khushu in daily prayers"
        )
    }

    // Send user message through multi-turn Gemini pipeline
    fun sendUserMessage(question: String) {
        if (question.isBlank() || isThinking) return
        val userPrompt = question.trim()
        inputText = ""

        // Resolve active role (auto-detect if enabled)
        val activeRole = if (isAutoDetectMode) {
            GeminiChatbotRole.autoDetectRole(userPrompt).also {
                selectedRole = it
            }
        } else {
            selectedRole
        }

        messages.add(ChatMessage(userPrompt, true))
        thinkingModelName = activeRole.displayModel
        isThinking = true
        voiceEngine.stop()

        scope.launch {
            try {
                val scholarResult = DualAiScholarEngine.askScholar(
                    context = context,
                    userQuery = userPrompt,
                    isFatwaRequest = activeRole == GeminiChatbotRole.COMPLEX_JURIST
                )
                messages.add(ChatMessage(scholarResult.answer, false))
                try {
                    voiceEngine.speak(scholarResult.answer)
                } catch (_: Exception) {
                    // Never allow TTS callback or engine errors to crash the chat flow.
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Chat query error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            // Header Bar & Active Persona Card
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                elevation = 6.dp,
                cornerRadius = 20.dp,
                backgroundColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSurfaceLight)
                                    .border(1.5.dp, PlatinumGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (selectedRole) {
                                        GeminiChatbotRole.COMPLEX_JURIST -> Icons.Default.Psychology
                                        GeminiChatbotRole.FAST_FINDER -> Icons.Default.Bolt
                                        GeminiChatbotRole.GENERAL_COMPANION -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = selectedRole.title,
                                    tint = DeepRoyalEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedRole.title,
                                        fontSize = 14.sp,
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
                                            text = selectedRole.displayModel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RadiantEmerald
                                        )
                                    }
                                }
                                Text(
                                    text = "${selectedRole.shortRole} • ${selectedRole.taskComplexity}",
                                    fontSize = 10.5.sp,
                                    color = SlateMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showRoleInfoDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Role Details",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showClearConfirmDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = SlateMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Selector Tabs: Complex (3.1 Pro), General (3.5 Flash), Fast (3.1 Flash Lite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoleSelectorPill(
                            title = "Complex",
                            model = "3.1 Pro",
                            icon = Icons.Default.Psychology,
                            isSelected = selectedRole == GeminiChatbotRole.COMPLEX_JURIST && !isAutoDetectMode,
                            onClick = {
                                selectedRole = GeminiChatbotRole.COMPLEX_JURIST
                                isAutoDetectMode = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_selector_complex")
                        )

                        RoleSelectorPill(
                            title = "General",
                            model = "3.5 Flash",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = selectedRole == GeminiChatbotRole.GENERAL_COMPANION && !isAutoDetectMode,
                            onClick = {
                                selectedRole = GeminiChatbotRole.GENERAL_COMPANION
                                isAutoDetectMode = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_selector_general")
                        )

                        RoleSelectorPill(
                            title = "Fast",
                            model = "Flash-Lite",
                            icon = Icons.Default.Bolt,
                            isSelected = selectedRole == GeminiChatbotRole.FAST_FINDER && !isAutoDetectMode,
                            onClick = {
                                selectedRole = GeminiChatbotRole.FAST_FINDER
                                isAutoDetectMode = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_selector_fast")
                        )

                        RoleSelectorPill(
                            title = "Auto",
                            model = "Smart",
                            icon = Icons.Default.SmartToy,
                            isSelected = isAutoDetectMode,
                            onClick = {
                                isAutoDetectMode = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_selector_auto")
                        )
                    }
                }
            }

            // Scrollable Thread (LazyColumn)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages.size) { index ->
                    val chat = messages[index]
                    ChatBubbleItem(
                        chat = chat,
                        isSpeaking = false,
                        onSpeakToggle = {
                            if (chat.isUser) return@ChatBubbleItem
                            if (isVoiceSpeaking) {
                                voiceEngine.stop()
                            } else {
                                voiceEngine.speak(chat.text)
                            }
                        },
                        onCopyText = {
                            clipboardManager.setText(AnnotatedString(chat.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isThinking) {
                    item {
                        ThinkingBubble(modelName = thinkingModelName)
                    }
                }
            }

            // Quick Question Suggestions (Contextual to Active Role)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickQuestions) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                            .border(1.dp, PlatinumGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
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

            // Input Bar
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
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
                                stopVoiceInput()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isListeningToSpeech) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListeningToSpeech) CrimsonError else DeepRoyalEmerald
                        )
                    }

                    // Multi-turn Free-Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = when (selectedRole) {
                                    GeminiChatbotRole.COMPLEX_JURIST -> "Ask complex Fiqh, Usul, or bioethics..."
                                    GeminiChatbotRole.FAST_FINDER -> "Instant Dua or Hadith lookup..."
                                    GeminiChatbotRole.GENERAL_COMPANION -> "Ask Noor for spiritual guidance..."
                                },
                                fontSize = 12.5.sp,
                                color = SlateMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
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

                    // Send Button
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
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isThinking) PureWhite else SlateMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    // Role Info & System Instruction Dialog
    if (showRoleInfoDialog) {
        AlertDialog(
            onDismissRequest = { showRoleInfoDialog = false },
            title = {
                Text(
                    text = "${selectedRole.title} (${selectedRole.displayModel})",
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Task Category: ${selectedRole.taskComplexity}",
                        fontWeight = FontWeight.SemiBold,
                        color = RadiantEmerald,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active System Instruction:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = CharcoalPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldSurfaceLight)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = selectedRole.systemPrompt,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = CharcoalPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Complex tasks use gemini-3.1-pro-preview\n• General tasks use gemini-3.5-flash\n• Fast tasks use gemini-3.1-flash-lite",
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleInfoDialog = false }) {
                    Text("Close", color = DeepRoyalEmerald, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear your conversation history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            appDb.chatDao().clearHistory()
                        }
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = CrimsonError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = SlateMuted)
                }
            }
        )
    }
}

@Composable
fun RoleSelectorPill(
    title: String,
    model: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) DeepRoyalEmerald else EmeraldSurfaceLight.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isSelected) PlatinumGold else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PureWhite else DeepRoyalEmerald,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PureWhite else DeepRoyalEmerald
                )
                Text(
                    text = model,
                    fontSize = 8.sp,
                    color = if (isSelected) PlatinumGold else SlateMuted
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    chat: ChatMessage,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit,
    onCopyText: () -> Unit
) {
    val isUser = chat.isUser
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(EmeraldSurfaceLight)
                    .border(1.dp, PlatinumGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "☪", fontSize = 12.sp, color = DeepRoyalEmerald)
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
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        spotColor = Color(0x18064E3B)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) DeepRoyalEmerald else PureWhite)
                    .border(
                        1.dp,
                        if (isUser) DeepRoyalEmerald else Color(0x24D4AF37),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Noor AI",
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(onClick = onCopyText, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = chat.text,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = if (isUser) PureWhite else CharcoalPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = if (isUser) PureWhite.copy(alpha = 0.7f) else SlateMuted,
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start)
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingBubble(modelName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EmeraldSurfaceLight)
                .border(1.dp, PlatinumGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "☪", fontSize = 12.sp, color = DeepRoyalEmerald)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PureWhite)
                .border(1.dp, PlatinumGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(13.dp)
                        .scale(pulseScale),
                    strokeWidth = 2.dp,
                    color = RadiantEmerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini ($modelName) is reasoning...",
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
                try {
                    recognizer.cancel()
                } catch (_: Exception) {}
                try {
                    recognizer.destroy()
                } catch (_: Exception) {}
            }
            override fun onResults(results: Bundle?) {
                onListeningStateChange(false)
                try {
                    recognizer.destroy()
                } catch (_: Exception) {}
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
    } catch (_: Exception) {
        onListeningStateChange(false)
    }
}
