@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.data.quran.api.AyahData
import com.example.data.quran.api.QuranJsonApi
import com.example.data.quran.api.SurahFullData
import com.example.data.quran.api.SurahInfo
import com.example.data.quran.model.JuzCatalog
import com.example.data.quran.model.JuzInfo
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.repository.QuranRepository
import com.example.service.audio.QuranAudioController
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.audio.QuranPlaybackControlBar
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.launch

/**
 * Noble Quran Reader Module
 * Fetches Surah data dynamically from JSON API (api.alquran.cloud editions/quran-uthmani,en.sahih),
 * caches in memory for offline instant access, and presents a readable, typography-first format
 * with Uthmani Arabic script and English Sahih International translations.
 */
@Composable
fun QuranReaderScreen(
    onBack: () -> Unit = {},
    onOpenTafsir: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val quranRepository = remember { QuranRepository(context) }

    // Handle system back navigation
    BackHandler {
        onBack()
    }

    // State for selected Surah (Default: Surah 1 Al-Fatihah)
    var currentSurahNumber by remember { mutableIntStateOf(1) }
    var surahDataState by remember { mutableStateOf<SurahFullData?>(null) }
    var currentJuzNumber by remember { mutableIntStateOf(1) }
    var juzAyahs by remember { mutableStateOf<List<AyahData>>(emptyList()) }
    var isJuzMode by remember { mutableStateOf(false) }
    var isLoadingJuz by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var juzLoadAttempt by remember { mutableIntStateOf(0) }
    var surahLoadGeneration by remember { mutableIntStateOf(0) }

    // UI Preferences
    var arabicFontSize by remember { mutableFloatStateOf(26f) }
    var translationFontSize by remember { mutableFloatStateOf(14.5f) }
    var showTranslation by remember { mutableStateOf(true) }
    var showUrduTranslation by remember { mutableStateOf(true) }

    // Dialog & Sheet States
    var showSurahPickerSheet by remember { mutableStateOf(false) }
    var showJuzPickerSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var surahSearchQuery by remember { mutableStateOf("") }

    val isAudioPlaying by QuranAudioController.isPlaying.collectAsStateWithLifecycle()
    val activeAudioSurahId by QuranAudioController.activeSurahId.collectAsStateWithLifecycle()
    val activeAudioAyahNumber by QuranAudioController.activeAyahNumber.collectAsStateWithLifecycle()
    val activeQari by QuranAudioController.activeQari.collectAsStateWithLifecycle()
    val audioDurationMs by QuranAudioController.currentDurationMs.collectAsStateWithLifecycle()
    val audioPositionMs by QuranAudioController.currentPositionMs.collectAsStateWithLifecycle()

    var pendingQuickAyah by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Function to fetch Surah data from JSON API
    fun loadSurah(surahNum: Int) {
        surahLoadGeneration++
        val requestGeneration = surahLoadGeneration
        isLoading = true
        errorMessage = null
        scope.launch {
            val info = QuranJsonApi.ALL_114_SURAHS.first { it.number == surahNum }
            val localAyahs = quranRepository.getAyahsForSurah(surahNum)
            if (requestGeneration != surahLoadGeneration) return@launch
            if (localAyahs.size == info.numberOfAyahs && localAyahs.all {
                    it.textArabic.isNotBlank() && it.textUrduTranslation.isNotBlank()
                }
            ) {
                val cachedData = SurahFullData(
                    info = info,
                    ayahs = localAyahs.sortedBy { it.verseNumber }.map { ayah ->
                        AyahData(
                            numberInSurah = ayah.verseNumber,
                            globalNumber = ayah.globalVerseNumber,
                            arabicText = ayah.textArabic,
                            englishTranslation = ayah.textEnglishTranslation,
                            audioUrl = "https://everyayah.com/data/Alafasy_128kbps/${surahNum.toString().padStart(3, '0')}${ayah.verseNumber.toString().padStart(3, '0')}.mp3",
                            surahNumber = surahNum,
                            surahNameArabic = info.name,
                            surahNameEnglish = info.englishName,
                            juzNumber = JuzCatalog.getJuzForAyah(surahNum, ayah.verseNumber),
                            urduTranslation = ayah.textUrduTranslation
                        )
                    }
                )
                surahDataState = cachedData
                isLoading = false
                currentSurahNumber = surahNum
                listState.scrollToItem(0)
                return@launch
            }

            val result = QuranJsonApi.fetchSurah(surahNum)
            if (requestGeneration != surahLoadGeneration) return@launch
            val data = result.getOrElse { err ->
                surahDataState = QuranJsonApi.getOfflineFallback(surahNum)
                isLoading = false
                currentSurahNumber = surahNum
                errorMessage = "Network error: ${err.message}. Loaded offline fallback."
                return@launch
            }
            quranRepository.cacheFetchedSurah(data)
            surahDataState = data
            isLoading = false
            currentSurahNumber = surahNum
            val translatedAyahCount = data.ayahs.count {
                it.arabicText.isNotBlank() && it.urduTranslation.isNotBlank()
            }
            if (data.ayahs.size < data.info.numberOfAyahs || translatedAyahCount < data.info.numberOfAyahs) {
                errorMessage = "Complete translated text is unavailable (${translatedAyahCount} of ${data.info.numberOfAyahs} Ayahs). Connect to the internet or download this Surah."
            }
            listState.scrollToItem(0)
        }
    }

    // Initial load
    LaunchedEffect(currentSurahNumber) {
        loadSurah(currentSurahNumber)
    }

    LaunchedEffect(currentJuzNumber, isJuzMode, juzLoadAttempt) {
        if (!isJuzMode) return@LaunchedEffect
        isLoadingJuz = true
        errorMessage = null
        try {
            val entities = quranRepository.getOrFetchAyahsForJuz(currentJuzNumber)
            if (entities.isEmpty()) {
                juzAyahs = emptyList()
                errorMessage = "Unable to load complete Para $currentJuzNumber. Connect to the internet or download all its Surahs, then retry."
            } else {
                juzAyahs = entities.map { ayah ->
                    val info = QuranJsonApi.ALL_114_SURAHS.first { it.number == ayah.surahId }
                    AyahData(
                        numberInSurah = ayah.verseNumber,
                        globalNumber = ayah.globalVerseNumber,
                        arabicText = ayah.textArabic,
                        englishTranslation = ayah.textEnglishTranslation,
                        audioUrl = "https://everyayah.com/data/Alafasy_128kbps/${ayah.surahId.toString().padStart(3, '0')}${ayah.verseNumber.toString().padStart(3, '0')}.mp3",
                        surahNumber = ayah.surahId,
                        surahNameArabic = info.name,
                        surahNameEnglish = info.englishName,
                        juzNumber = currentJuzNumber,
                        urduTranslation = ayah.textUrduTranslation
                    )
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            juzAyahs = emptyList()
            errorMessage = "Unable to load Para $currentJuzNumber: ${e.message}"
        }
        isLoadingJuz = false
    }

    // Audio Playback Function (Streams via background Media3 service)
    fun playAyahAudio(ayah: AyahData) {
        if (activeAudioAyahNumber == ayah.numberInSurah && isAudioPlaying && activeAudioSurahId == ayah.surahNumber) {
            QuranAudioController.pauseAudio()
            return
        }

        try {
            val surahInfo = QuranJsonApi.ALL_114_SURAHS.first { it.number == ayah.surahNumber }
            QuranAudioController.playAyah(
                context = context,
                surahId = ayah.surahNumber,
                surahName = surahInfo.englishName,
                ayahNumber = ayah.numberInSurah,
                totalAyahs = surahInfo.numberOfAyahs,
                qariId = activeQari
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Audio error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentSurahNumber, surahDataState, pendingQuickAyah, errorMessage) {
        val target = pendingQuickAyah ?: return@LaunchedEffect
        val data = surahDataState ?: return@LaunchedEffect
        if (data.info.number != target.first || currentSurahNumber != target.first) return@LaunchedEffect
        val targetIndex = data.ayahs.indexOfFirst { it.numberInSurah == target.second }
        if (targetIndex < 0) {
            errorMessage = "Ayah ${target.second} is not available in this offline excerpt. Connect to the internet to load it."
            pendingQuickAyah = null
            return@LaunchedEffect
        }
        val targetAyah = data.ayahs[targetIndex]
        val headerItems = 1 +
            (if (target.first != 9) 1 else 0) +
            (if (errorMessage != null) 1 else 0)
        listState.scrollToItem((headerItems + targetIndex).coerceAtLeast(0))
        playAyahAudio(targetAyah)
        pendingQuickAyah = null
    }

    // Copy Verse to Clipboard
    fun copyAyah(ayah: AyahData, surah: SurahInfo) {
        val textToCopy = "${ayah.arabicText}\n\n\"${ayah.englishTranslation}\"\n[Quran ${surah.englishName} ${surah.number}:${ayah.numberInSurah}]"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ayah Text", textToCopy))
        Toast.makeText(context, "Ayah ${ayah.numberInSurah} copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Share Verse
    fun shareAyah(ayah: AyahData, surah: SurahInfo) {
        val shareText = "${ayah.arabicText}\n\n\"${ayah.englishTranslation}\"\n[Quran ${surah.englishName} ${surah.number}:${ayah.numberInSurah}]"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Ayah via"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PearlBackground, PureWhite, EmeraldSurfaceLight.copy(alpha = 0.25f))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("quran_reader_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                elevation = 6.dp,
                cornerRadius = 20.dp,
                backgroundColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                onBack()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmeraldSurfaceLight, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepRoyalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Tappable title opens the picker for the active reading mode.
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isJuzMode) showJuzPickerSheet = true else showSurahPickerSheet = true
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("surah_picker_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isJuzMode) {
                                        JuzCatalog.getJuzById(currentJuzNumber)?.let { "Para ${it.id}: ${it.nameEnglish}" } ?: "Loading Para..."
                                    } else {
                                        surahDataState?.info?.let { "${it.number}. ${it.englishName}" } ?: "Loading Surah..."
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRoyalEmerald
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▼",
                                    fontSize = 10.sp,
                                    color = PlatinumGold
                                )
                            }
                            Text(
                                text = if (isJuzMode) {
                                    JuzCatalog.getJuzById(currentJuzNumber)?.nameArabic ?: "30 Paras"
                                } else {
                                    surahDataState?.info?.let { "${it.name} • ${it.numberOfAyahs} Ayahs" } ?: "JSON API Reader"
                                },
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }

                    // Action Icons: Previous/Next Surah & Display Settings
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Previous Surah / Para
                        IconButton(
                            onClick = {
                                if (isJuzMode) currentJuzNumber = (currentJuzNumber - 1).coerceAtLeast(1)
                                else if (currentSurahNumber > 1) currentSurahNumber--
                            },
                            enabled = if (isJuzMode) currentJuzNumber > 1 else currentSurahNumber > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = if (isJuzMode) "Previous Para" else "Previous Surah",
                                tint = if (if (isJuzMode) currentJuzNumber > 1 else currentSurahNumber > 1) DeepRoyalEmerald else SlateMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Next Surah / Para
                        IconButton(
                            onClick = {
                                if (isJuzMode) currentJuzNumber = (currentJuzNumber + 1).coerceAtMost(30)
                                else if (currentSurahNumber < 114) currentSurahNumber++
                            },
                            enabled = if (isJuzMode) currentJuzNumber < 30 else currentSurahNumber < 114,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = if (isJuzMode) "Next Para" else "Next Surah",
                                tint = if (if (isJuzMode) currentJuzNumber < 30 else currentSurahNumber < 114) DeepRoyalEmerald else SlateMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Font Settings
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Display Settings",
                                tint = DeepRoyalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isJuzMode,
                    onClick = { isJuzMode = false },
                    label = { Text("Surahs (114)") }
                )
                FilterChip(
                    selected = isJuzMode,
                    onClick = { isJuzMode = true },
                    label = { Text("Paras / Juz (30)") }
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickJumps = listOf(
                    "Ayat al-Kursi" to { isJuzMode = false; pendingQuickAyah = 2 to 255; currentSurahNumber = 2 },
                    "Amanar-Rasool" to { isJuzMode = false; pendingQuickAyah = 2 to 285; currentSurahNumber = 2 },
                    "Ya-Sin" to { isJuzMode = false; currentSurahNumber = 36 },
                    "Ar-Rahman" to { isJuzMode = false; currentSurahNumber = 55 },
                    "Al-Mulk" to { isJuzMode = false; currentSurahNumber = 67 },
                    "Al-Kahf" to { isJuzMode = false; currentSurahNumber = 18 },
                    "Para 30" to { isJuzMode = true; currentJuzNumber = 30 },
                    "3 Quls" to { isJuzMode = false; currentSurahNumber = 112 }
                )
                items(quickJumps.size) { index ->
                    val (label, action) = quickJumps[index]
                    FilterChip(selected = false, onClick = action, label = { Text(label) })
                }
            }

            // Main Content Area
            if (if (isJuzMode) isLoadingJuz else isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = RadiantEmerald,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isJuzMode) "Fetching Para $currentJuzNumber from Quran JSON API..." else "Fetching Surah $currentSurahNumber from Quran JSON API...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Retrieving Uthmani script, Urdu & English translations",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }
                }
            } else {
                val currentData = surahDataState
                    ?: QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == currentSurahNumber }?.let {
                        SurahFullData(it, emptyList())
                    }
                if (currentData != null) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isJuzMode && errorMessage != null) {
                            item {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    color = SlateMuted,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (isJuzMode) {
                            val juz = JuzCatalog.getJuzById(currentJuzNumber)
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DeepRoyalEmerald,
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(Modifier.padding(18.dp)) {
                                        Text("Para ${juz?.id ?: currentJuzNumber}: ${juz?.nameEnglish.orEmpty()}",
                                            color = PlatinumGold, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                        Text(juz?.nameArabic.orEmpty(), color = PureWhite, fontSize = 22.sp,
                                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                        Text("${juz?.startSurahName} ${juz?.startVerse} — ${juz?.endSurahName} ${juz?.endVerse} • ${juz?.totalVersesInJuz} Ayahs",
                                            color = PureWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                            if (juzAyahs.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = errorMessage ?: "No downloaded Ayahs are available for this Para. Connect to the internet and retry.",
                                            color = SlateMuted,
                                            textAlign = TextAlign.Center
                                        )
                                        TextButton(onClick = { juzLoadAttempt++ }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            items(items = juzAyahs, key = { "juz_${currentJuzNumber}_${it.globalNumber}" }) { ayah ->
                                val info = QuranJsonApi.ALL_114_SURAHS.first { it.number == ayah.surahNumber }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (ayah.numberInSurah == 1 || ayah.surahNumber == juz?.startSurahId && ayah.numberInSurah == juz.startVerse) {
                                        SurahHeroBanner(
                                            info = info,
                                            isPlaying = isAudioPlaying && activeAudioSurahId == ayah.surahNumber,
                                            onTogglePlaySurah = {
                                                QuranAudioController.playSurahWithQari(context, ayah.surahNumber, info.englishName, activeQari)
                                            },
                                            onOpenTafsir = onOpenTafsir?.let { { it(ayah.surahNumber) } }
                                        )
                                        if (ayah.surahNumber != 1 && ayah.surahNumber != 9 && ayah.numberInSurah == 1) {
                                            BismillahOrnamentalBanner()
                                        }
                                    }
                                    AyahCardItem(
                                        ayah = ayah,
                                        surahInfo = info,
                                        isPlaying = isAudioPlaying && activeAudioSurahId == ayah.surahNumber &&
                                            activeAudioAyahNumber == ayah.numberInSurah,
                                        arabicFontSize = arabicFontSize,
                                        translationFontSize = translationFontSize,
                                        showTranslation = showTranslation,
                                        showUrduTranslation = showUrduTranslation,
                                        onPlayClick = { playAyahAudio(ayah) },
                                        onCopyClick = { copyAyah(ayah, info) },
                                        onShareClick = { shareAyah(ayah, info) }
                                    )
                                }
                            }
                            item {
                                SurahFooterNavigation(
                                    currentSurahNumber = currentJuzNumber,
                                    onPreviousSurah = { currentJuzNumber = (currentJuzNumber - 1).coerceAtLeast(1) },
                                    onNextSurah = { currentJuzNumber = (currentJuzNumber + 1).coerceAtMost(30) }
                                )
                            }
                        } else {
                        // Header 1: Surah Information Banner Card with Audio Stream Action
                        item {
                            val isSurahPlaying = isAudioPlaying && activeAudioSurahId == currentData.info.number
                            SurahHeroBanner(
                                info = currentData.info,
                                isPlaying = isSurahPlaying,
                                onTogglePlaySurah = {
                                    if (isSurahPlaying) {
                                        QuranAudioController.togglePlayPause(context)
                                    } else {
                                        QuranAudioController.playSurahWithQari(
                                            context = context,
                                            surahId = currentData.info.number,
                                            surahName = currentData.info.englishName,
                                            qariId = activeQari
                                        )
                                    }
                                },
                                onOpenTafsir = onOpenTafsir?.let { { it(currentData.info.number) } }
                            )
                        }

                        // Header 2: Bismillah Ornamental Banner (Except for Surah 9 At-Tawbah)
                        if (currentData.info.number != 1 && currentData.info.number != 9) {
                            item {
                                BismillahOrnamentalBanner()
                            }
                        }

                        // List of Ayahs
                        items(
                            items = currentData.ayahs,
                            key = { it.globalNumber }
                        ) { ayah ->
                            val isActivelyPlaying = isAudioPlaying && activeAudioSurahId == currentData.info.number &&
                                activeAudioAyahNumber == ayah.numberInSurah

                            AyahCardItem(
                                ayah = ayah,
                                surahInfo = currentData.info,
                                isPlaying = isActivelyPlaying,
                                arabicFontSize = arabicFontSize,
                                translationFontSize = translationFontSize,
                                showTranslation = showTranslation,
                                showUrduTranslation = showUrduTranslation,
                                onPlayClick = { playAyahAudio(ayah) },
                                onCopyClick = { copyAyah(ayah, currentData.info) },
                                onShareClick = { shareAyah(ayah, currentData.info) }
                            )
                        }

                        // Footer Navigation & Surah Completion
                        item {
                            SurahFooterNavigation(
                                currentSurahNumber = currentData.info.number,
                                onPreviousSurah = {
                                    if (currentSurahNumber > 1) currentSurahNumber--
                                },
                                onNextSurah = {
                                    if (currentSurahNumber < 114) currentSurahNumber++
                                }
                                }
                            )
                        }
                    }
                } else {
                    // Error state with retry
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Unable to load Surah data",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Please check your network connection.",
                                fontSize = 12.sp,
                                color = SlateMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { loadSurah(currentSurahNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Fetch")
                            }
                        }
                    }
                }
            }

            // DOCKED PLAYBACK CONTROL BAR (Features Play/Pause, Track Progress, Seeking & Qari info)
            if (isAudioPlaying || audioDurationMs > 0L || audioPositionMs > 0L) {
                QuranPlaybackControlBar()
            }
        }
    }

    // Modal Bottom Sheet: Surah Directory / Picker (114 Surahs)
    if (showSurahPickerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                showSurahPickerSheet = false
                surahSearchQuery = ""
            },
            sheetState = sheetState,
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Surah (اخْتَرْ السُّورَة)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = surahSearchQuery,
                    onValueChange = { surahSearchQuery = it },
                    placeholder = { Text("Search by name or number (e.g. Yaseen, 36, Mulk)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SlateMuted) },
                    trailingIcon = {
                        if (surahSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { surahSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = SlateMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RadiantEmerald,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredSurahs = remember(surahSearchQuery) {
                    if (surahSearchQuery.isBlank()) {
                        QuranJsonApi.ALL_114_SURAHS
                    } else {
                        val query = surahSearchQuery.trim().lowercase()
                        QuranJsonApi.ALL_114_SURAHS.filter {
                            it.number.toString() == query ||
                            it.englishName.lowercase().contains(query) ||
                            it.name.contains(query) ||
                            it.englishNameTranslation.lowercase().contains(query)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        val isSelected = surah.number == currentSurahNumber

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) EmeraldSurfaceLight else Color(0xFFF8FAFC))
                                .border(
                                    1.dp,
                                    if (isSelected) PlatinumGold else Color(0xFFE2E8F0),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    currentSurahNumber = surah.number
                                    showSurahPickerSheet = false
                                    surahSearchQuery = ""
                                }

                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) DeepRoyalEmerald else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${surah.number}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PureWhite else DeepRoyalEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = surah.englishName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepRoyalEmerald
                                    )
                                    Text(
                                        text = "${surah.englishNameTranslation} • ${surah.numberOfAyahs} Verses",
                                        fontSize = 11.sp,
                                        color = SlateMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = surah.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalPrimary
                                )
                                Text(
                                    text = surah.revelationType,
                                    fontSize = 10.5.sp,
                                    color = PlatinumGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJuzPickerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showJuzPickerSheet = false },
            sheetState = sheetState,
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("Select Para / Juz (۳۰ سپارے)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(JuzCatalog.allJuz, key = JuzInfo::id) { juz ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (juz.id == currentJuzNumber) EmeraldSurfaceLight else Color(0xFFF8FAFC))
                                .clickable {
                                    currentJuzNumber = juz.id
                                    isJuzMode = true
                                    showJuzPickerSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Para ${juz.id} • ${juz.nameEnglish}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                                Text("${juz.startSurahName} ${juz.startVerse} — ${juz.endSurahName} ${juz.endVerse}", fontSize = 11.sp, color = SlateMuted)
                            }
                            Text(juz.nameArabic, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                        }
                    }
                }
            }
        }
    }

    // Display & Font Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Display & Typography Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Arabic Font Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Arabic Script Size", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text("${arabicFontSize.toInt()} sp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                    }
                    Slider(
                        value = arabicFontSize,
                        onValueChange = { arabicFontSize = it },
                        valueRange = 20f..38f,
                        colors = SliderDefaults.colors(
                            thumbColor = DeepRoyalEmerald,
                            activeTrackColor = RadiantEmerald
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Translation Font Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Translation Size", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text("${translationFontSize.toInt()} sp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                    }
                    Slider(
                        value = translationFontSize,
                        onValueChange = { translationFontSize = it },
                        valueRange = 12f..22f,
                        colors = SliderDefaults.colors(
                            thumbColor = DeepRoyalEmerald,
                            activeTrackColor = RadiantEmerald
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Show Translation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show English Translation", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = showTranslation,
                            onCheckedChange = { showTranslation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = RadiantEmerald
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Urdu Translation (اردو)", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = showUrduTranslation,
                            onCheckedChange = { showUrduTranslation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = RadiantEmerald
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done", color = DeepRoyalEmerald, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Surah Hero Banner displaying Surah metadata and Arabic title.
 */
@Composable
fun SurahHeroBanner(
    info: SurahInfo,
    isPlaying: Boolean = false,
    onTogglePlaySurah: (() -> Unit)? = null,
    onOpenTafsir: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(DeepRoyalEmerald, DeepRoyalEmerald.copy(alpha = 0.92f))
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = info.name,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${info.number}. ${info.englishName} (${info.englishNameTranslation})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = "${info.numberOfAyahs} Verses",
                            fontSize = 10.5.sp,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = "${info.revelationType} Revelation",
                            fontSize = 10.5.sp,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (onOpenTafsir != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PlatinumGold,
                            modifier = Modifier.clickable(onClick = onOpenTafsir)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Tafsir",
                                    tint = DeepRoyalEmerald,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Tafsir",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRoyalEmerald
                                )
                            }
                        }
                    }
                }

                if (onTogglePlaySurah != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaying) RadiantEmerald else EmeraldSurfaceLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTogglePlaySurah() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Stream Audio",
                                tint = if (isPlaying) PureWhite else DeepRoyalEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "Streaming Recitation (Tap to Pause)" else "Stream Full Surah Recitation",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) PureWhite else DeepRoyalEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Traditional decorative Bismillah Calligraphy Banner displayed before Surah recitation.
 */
@Composable
fun BismillahOrnamentalBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = DeepRoyalEmerald,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = "شروع اللہ کا نام لے کر جو بڑا مہربان نہایت رحم والا ہے",
                    fontSize = 13.sp,
                    color = SlateMuted,
                    textAlign = TextAlign.Right
                )
            }
            Text(
                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful",
                fontSize = 11.5.sp,
                color = SlateMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual Ayah Card displaying Arabic calligraphy, English translation, and interactive controls.
 */
@Composable
fun AyahCardItem(
    ayah: AyahData,
    surahInfo: SurahInfo,
    isPlaying: Boolean,
    arabicFontSize: Float,
    translationFontSize: Float,
    showTranslation: Boolean,
    showUrduTranslation: Boolean = true,
    onPlayClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (isPlaying) RadiantEmerald.copy(alpha = 0.12f) else PureWhite,
        animationSpec = tween(300),
        label = "ayah_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) RadiantEmerald else PlatinumGold.copy(alpha = 0.28f),
        animationSpec = tween(300),
        label = "ayah_border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_item_${ayah.numberInSurah}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 5.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPlaying) 1.8.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Verse Header: Verse Badge & Quick Actions (Play, Copy, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verse Badge (e.g. ۝ 1:1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) RadiantEmerald else EmeraldSurfaceLight)
                            .border(1.dp, PlatinumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${ayah.numberInSurah}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) PureWhite else DeepRoyalEmerald
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Ayah ${ayah.numberInSurah} (Verse ${ayah.globalNumber})",
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }

                // Actions Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Play/Pause Button
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isPlaying) RadiantEmerald else EmeraldSurfaceLight,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Recite Ayah",
                            tint = if (isPlaying) PureWhite else DeepRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Copy Button
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Verse",
                            tint = SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Verse",
                            tint = SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text with Authentic Uthmani Calligraphy Formatting
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = "${ayah.arabicText} ﴿${ayah.numberInSurah}﴾",
                    fontSize = arabicFontSize.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) DeepRoyalEmerald else CharcoalPrimary,
                    textAlign = TextAlign.Right,
                    lineHeight = (arabicFontSize * 1.75).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showUrduTranslation && ayah.urduTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldSurfaceLight.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = ayah.urduTranslation,
                            fontSize = translationFontSize.sp,
                            color = CharcoalPrimary,
                            textAlign = TextAlign.Right,
                            lineHeight = (translationFontSize * 1.55).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // English Translation (if enabled)
            if (showTranslation) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = ayah.englishTranslation,
                            fontSize = translationFontSize.sp,
                            color = CharcoalPrimary.copy(alpha = 0.90f),
                            textAlign = TextAlign.Left,
                            lineHeight = (translationFontSize * 1.45).sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Footer providing easy navigation to next/previous Surahs at the end of the chapter.
 */
@Composable
fun SurahFooterNavigation(
    currentSurahNumber: Int,
    onPreviousSurah: () -> Unit,
    onNextSurah: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPreviousSurah,
                enabled = currentSurahNumber > 1,
                colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous", fontSize = 12.sp)
            }

            Text(
                text = "Surah $currentSurahNumber of 114",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateMuted
            )

            Button(
                onClick = onNextSurah,
                enabled = currentSurahNumber < 114,
                colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }
    }
}
