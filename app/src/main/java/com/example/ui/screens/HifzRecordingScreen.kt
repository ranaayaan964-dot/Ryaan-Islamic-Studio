package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.repository.QuranRepository
import com.example.data.quran.seed.QuranSeedData
import com.example.data.remote.GroqHifzClient
import com.example.data.remote.GroqHifzResult
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.util.AudioRecorderHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * FULL 30 JUZ QURAN HIFZ STUDIO WITH REAL-TIME GROQ VOICE DETECTION
 * - 114 Surah Selection UI with Searchable Bottom Sheet
 * - Exact Arabic Target Text loaded from Room Database (QuranDatabase)
 * - Real Groq Whisper audio detection (whisper-large-v3, language=ar)
 * - Groq Llama-3 Tajweed evaluation (llama3-70b-8192)
 * - Interactive AI Spoken Feedback via Android Text-To-Speech (TTS)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzRecordingScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val quranRepository = remember { QuranRepository(context) }

    // All 114 Surahs catalog
    val allSurahs = remember { QuranSeedData.getSurahList() }
    var selectedSurah by remember { mutableStateOf(allSurahs[0]) } // Surah Al-Fatihah default

    // Surah Ayah Range state
    var startVerse by remember { mutableIntStateOf(1) }
    var endVerse by remember { mutableIntStateOf(selectedSurah.totalVerses.coerceAtMost(7)) }

    // Room Database Target Text State
    var surahAyahs by remember { mutableStateOf<List<AyahEntity>>(emptyList()) }
    var isLoadingAyahs by remember { mutableStateOf(false) }
    var targetArabicText by remember { mutableStateOf("") }

    // Surah Picker Sheet State
    var showSurahPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var surahSearchQuery by remember { mutableStateOf("") }

    // Audio recording & Evaluation states
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required for recitation check", Toast.LENGTH_SHORT).show()
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var hifzResult by remember { mutableStateOf<GroqHifzResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }

    // Android TextToSpeech (TTS) Engine
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.ENGLISH
                isTtsReady = true
            }
        }
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
        textToSpeech = ttsInstance

        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Function to trigger spoken feedback
    fun speakFeedback(text: String) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HifzFeedbackUtterance")
        }
    }

    // Load Ayahs from Room Database when selected Surah changes
    LaunchedEffect(selectedSurah.id) {
        isLoadingAyahs = true
        startVerse = 1
        endVerse = selectedSurah.totalVerses.coerceAtMost(7)
        val ayahs = quranRepository.getOrFetchAyahsForSurah(selectedSurah.id)
        surahAyahs = ayahs
        isLoadingAyahs = false
    }

    // Update target Arabic text whenever surahAyahs or verse range changes
    LaunchedEffect(surahAyahs, startVerse, endVerse) {
        if (surahAyahs.isNotEmpty()) {
            val filtered = surahAyahs.filter { it.verseNumber in startVerse..endVerse }
            targetArabicText = filtered.joinToString(separator = " ۝ ") { it.textArabic }
        } else {
            targetArabicText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        }
    }

    // Pulsing animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
    val recScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PearlBackground, PureWhite, DeepRoyalEmerald.copy(alpha = 0.08f))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("hifz_recording_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(DeepRoyalEmerald.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepRoyalEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "30 Juz Hifz Studio",
                            color = CharcoalPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Groq Whisper & Llama-3 AI Voice Detection",
                            color = DeepRoyalEmerald,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // AI Voice TTS Indicator / Toggle
                if (hifzResult != null) {
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                textToSpeech?.stop()
                                isSpeaking = false
                            } else {
                                speakFeedback(hifzResult?.feedback.orEmpty())
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isSpeaking) RadiantEmerald else PlatinumGold.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Read Aloud",
                            tint = if (isSpeaking) PureWhite else DeepRoyalEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 114 Surah Selector Banner (Clickable Card)
            Surface(
                onClick = { showSurahPicker = true },
                shape = RoundedCornerShape(16.dp),
                color = PureWhite,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_selector_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DeepRoyalEmerald.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${selectedSurah.id}",
                                    color = DeepRoyalEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Surah ${selectedSurah.nameEnglish} (${selectedSurah.nameTranslation})",
                                color = CharcoalPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedSurah.revelationType} • ${selectedSurah.totalVerses} Ayahs",
                                color = CharcoalPrimary.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedSurah.nameArabic,
                            color = DeepRoyalEmerald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Surah",
                            tint = PlatinumGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ayah Range Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECITATION RANGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold
                )
                Text(
                    text = "Ayahs $startVerse to $endVerse of ${selectedSurah.totalVerses}",
                    fontSize = 12.sp,
                    color = CharcoalPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val ranges = listOf(
                    1 to selectedSurah.totalVerses.coerceAtMost(3),
                    1 to selectedSurah.totalVerses.coerceAtMost(7),
                    1 to selectedSurah.totalVerses.coerceAtMost(10),
                    1 to selectedSurah.totalVerses
                ).distinctBy { it.second }

                items(ranges) { range ->
                    val isSelected = startVerse == range.first && endVerse == range.second
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DeepRoyalEmerald else PureWhite,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) DeepRoyalEmerald else PlatinumGold.copy(alpha = 0.4f)
                        ),
                        onClick = {
                            startVerse = range.first
                            endVerse = range.second
                            hifzResult = null
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = if (range.second == selectedSurah.totalVerses) "Full Surah (1-${range.second})" else "Ayahs ${range.first}-${range.second}",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureWhite else CharcoalPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Room Database Target Text Display Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TARGET QURANIC ARABIC TEXT (ROOM DATABASE)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingAyahs) {
                        CircularProgressIndicator(
                            color = PlatinumGold,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loading verses from Room Database...",
                            fontSize = 11.sp,
                            color = CharcoalPrimary.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = targetArabicText,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press and hold below to recite. Groq Whisper will transcribe in real-time, and AI will speak your Tajweed feedback.",
                        fontSize = 11.5.sp,
                        color = CharcoalPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hold-to-Record Recitation Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .scale(if (isRecording) recScale else 1f)
                    .testTag("hold_to_record_button")
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(CrimsonError.copy(alpha = 0.25f))
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) CrimsonError else DeepRoyalEmerald)
                        .shadow(12.dp, CircleShape)
                        .pointerInput(hasAudioPermission, targetArabicText) {
                            detectTapGestures(
                                onPress = {
                                    if (!hasAudioPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@detectTapGestures
                                    }

                                    isRecording = true
                                    hifzResult = null
                                    errorMessage = null
                                    try {
                                        recordedFile = audioRecorder.startRecording()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }

                                    // Wait until finger is lifted
                                    tryAwaitRelease()

                                    // User released
                                    isRecording = false
                                    val savedFile = try {
                                        audioRecorder.stopRecording()
                                    } catch (stopEx: Exception) {
                                        Log.w("HifzScreen", "Error stopping recording: ${stopEx.message}")
                                        null
                                    }

                                    if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
                                        recordedFile = savedFile
                                        isEvaluating = true
                                        coroutineScope.launch {
                                            try {
                                                val evalRes = GroqHifzClient.evaluateRecitation(
                                                    audioFile = savedFile,
                                                    targetText = targetArabicText,
                                                    surahName = selectedSurah.nameEnglish
                                                )
                                                evalRes.fold(
                                                    onSuccess = { res ->
                                                        hifzResult = res
                                                        // Trigger Android Text-To-Speech (TTS) safely
                                                        try {
                                                            speakFeedback(res.feedback)
                                                        } catch (ttsEx: Exception) {
                                                            Log.w("HifzScreen", "TTS error: ${ttsEx.message}")
                                                        }
                                                    },
                                                    onFailure = { err ->
                                                        errorMessage = "Notice: ${err.localizedMessage ?: "Tajweed evaluation temporarily offline"}"
                                                    }
                                                )
                                            } catch (t: Throwable) {
                                                Log.e("HifzScreen", "Unexpected error in recitation evaluation", t)
                                                errorMessage = "Tajweed verification safely completed. Please try again."
                                            } finally {
                                                isEvaluating = false
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Recording too brief. Hold and recite clearly.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Recite",
                            tint = PureWhite,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRecording) "RELEASE" else "HOLD",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRecording) "Listening with Groq Whisper... Release when finished" else "Hold to Record Recitation",
                color = if (isRecording) CrimsonError else DeepRoyalEmerald,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Evaluating Progress Indicator
            AnimatedVisibility(visible = isEvaluating) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = PlatinumGold,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transcribing with Whisper & Analyzing with Llama-3...",
                        fontSize = 12.5.sp,
                        color = DeepRoyalEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error Message
            if (errorMessage != null) {
                Surface(
                    color = CrimsonError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = CrimsonError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // AI Hifz Evaluation Output & Spoken Teacher Corrections
            if (hifzResult != null && !isEvaluating) {
                val result = hifzResult!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PlatinumGold.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Title & TTS Play Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PlatinumGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI TAJWEED EVALUATION",
                                    color = DeepRoyalEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Interactive Spoken Feedback Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSpeaking) RadiantEmerald else PlatinumGold.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSpeaking) RadiantEmerald else PlatinumGold
                                ),
                                modifier = Modifier.clickable {
                                    if (isSpeaking) {
                                        textToSpeech?.stop()
                                        isSpeaking = false
                                    } else {
                                        speakFeedback(result.feedback)
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                        contentDescription = "Speak Corrections",
                                        tint = if (isSpeaking) PureWhite else DeepRoyalEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "Speaking..." else "Listen",
                                        color = if (isSpeaking) PureWhite else DeepRoyalEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // What User Actually Recited (Groq Whisper Arabic Transcription)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CharcoalPrimary.copy(alpha = 0.04f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "GROQ WHISPER DETECTED RECITATION:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalPrimary.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.transcript,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalPrimary,
                                    lineHeight = 28.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Llama-3 AI Teacher Feedback
                        Text(
                            text = "TEACHER CORRECTIONS & TAJWEED FEEDBACK:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result.feedback,
                            color = CharcoalPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Searchable Bottom Sheet for all 114 Surahs
        if (showSurahPicker) {
            ModalBottomSheet(
                onDismissRequest = { showSurahPicker = false },
                sheetState = sheetState,
                containerColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select from 114 Surahs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        IconButton(onClick = { showSurahPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search field
                    OutlinedTextField(
                        value = surahSearchQuery,
                        onValueChange = { surahSearchQuery = it },
                        placeholder = { Text("Search Surah name or number...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRoyalEmerald,
                            unfocusedBorderColor = PlatinumGold.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredSurahs = allSurahs.filter {
                        val q = surahSearchQuery.trim()
                        q.isEmpty() ||
                                it.id.toString() == q ||
                                it.nameEnglish.contains(q, ignoreCase = true) ||
                                it.nameArabic.contains(q, ignoreCase = true) ||
                                it.nameTranslation.contains(q, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            val isSelected = surah.id == selectedSurah.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DeepRoyalEmerald.copy(alpha = 0.12f) else PureWhite,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) DeepRoyalEmerald else PlatinumGold.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSurah = surah
                                        hifzResult = null
                                        errorMessage = null
                                        showSurahPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) DeepRoyalEmerald else CharcoalPrimary.copy(alpha = 0.08f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${surah.id}",
                                                    color = if (isSelected) PureWhite else CharcoalPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = surah.nameEnglish,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CharcoalPrimary
                                            )
                                            Text(
                                                text = "${surah.revelationType} • ${surah.totalVerses} Ayahs",
                                                fontSize = 11.sp,
                                                color = CharcoalPrimary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = surah.nameArabic,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepRoyalEmerald
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = DeepRoyalEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
