package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.model.BookmarkEntity
import com.example.data.quran.model.JuzCatalog
import com.example.data.quran.model.JuzInfo
import com.example.data.quran.model.SurahEntity
import com.example.data.quran.repository.HifzRepeatMode
import com.example.data.quran.repository.LastReadInfo
import com.example.data.quran.repository.QuranReadingPreferences
import com.example.data.quran.repository.QuranRepository
import com.example.service.audio.QariCatalog
import com.example.service.audio.QariId
import com.example.service.audio.QuranAudioController
import com.example.service.audio.QuranDownloadManager
import com.example.ui.components.GlassCard
import com.example.ui.components.quran.JuzListView
import com.example.ui.components.quran.QariSelectionDialog
import com.example.ui.components.quran.QuranBookmarksDialog
import com.example.ui.components.quran.QuranDisplaySettingsDialog
import com.example.ui.components.quran.QuranKhatamTrackerHeader
import com.example.util.TajweedColorRules
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LightEmeraldBackground
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun QuranTafsirScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalThemeIsDark.current
    val repository = remember { QuranRepository(context) }
    val preferences = remember { QuranReadingPreferences(context) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        QuranAudioController.bindService(context)
        onDispose {
            QuranAudioController.unbindService(context)
        }
    }

    // Audio states from Controller
    val isPlaying by QuranAudioController.isPlaying.collectAsState()
    val currentPositionMs by QuranAudioController.currentPositionMs.collectAsState()
    val currentDurationMs by QuranAudioController.currentDurationMs.collectAsState()
    val activeSurahId by QuranAudioController.activeSurahId.collectAsState()
    val activeSurahName by QuranAudioController.activeSurahName.collectAsState()
    val activeQari by QuranAudioController.activeQari.collectAsState()
    val isOfflineAudio by QuranAudioController.isOfflineAudio.collectAsState()
    val playbackSpeed by QuranAudioController.playbackSpeed.collectAsState()
    val repeatMode by QuranAudioController.repeatMode.collectAsState()
    val loopingAyah by QuranAudioController.loopingAyah.collectAsState()
    val sleepTimerSeconds by QuranAudioController.sleepTimerSeconds.collectAsState()

    // Reading preferences
    val arabicFontSize by preferences.arabicFontSizeFlow.collectAsState()
    val showEnglish by preferences.showEnglishFlow.collectAsState()
    val showUrdu by preferences.showUrduFlow.collectAsState()
    val tajweedEnabled by preferences.tajweedEnabledFlow.collectAsState()
    val dailyGoal by preferences.dailyGoalFlow.collectAsState()
    val todayVersesRead by preferences.todayVersesReadFlow.collectAsState()
    val khatamVersesCompleted by preferences.khatamVersesCompletedFlow.collectAsState()
    val selectedQariId by preferences.selectedQariFlow.collectAsState()
    val lastReadInfo by preferences.lastReadFlow.collectAsState()

    // Database state
    val surahs by repository.getAllSurahsFlow().collectAsState(initial = emptyList())
    val bookmarks by repository.getAllBookmarksFlow().collectAsState(initial = emptyList())
    val downloadProgressMap by QuranDownloadManager.downloadProgressMap.collectAsState()

    var selectedSurah by remember { mutableStateOf<SurahEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTafsirAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var selectedWordByWordAyah by remember { mutableStateOf<AyahEntity?>(null) }

    // Tab 0 = Surahs (114), Tab 1 = Juz (30)
    var currentTab by remember { mutableIntStateOf(0) }
    val readerListState = rememberLazyListState()

    // Dialog flags
    var showQariDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showTajweedLegendDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val currentSurahId = selectedSurah?.id ?: 1
    val ayahList by remember(currentSurahId) {
        repository.getAyahsForSurahFlow(currentSurahId)
    }.collectAsState(initial = emptyList())

    LaunchedEffect(surahs) {
        if (selectedSurah == null && surahs.isNotEmpty()) {
            selectedSurah = surahs.firstOrNull()
        }
    }

    val filteredSurahs = remember(surahs, searchQuery) {
        if (searchQuery.isBlank()) surahs else {
            surahs.filter {
                it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                it.nameTranslation.contains(searchQuery, ignoreCase = true) ||
                it.nameArabic.contains(searchQuery) ||
                it.id.toString() == searchQuery.trim()
            }
        }
    }

    val currentQariProfile = remember(activeQari) {
        QariCatalog.getQariById(activeQari)
    }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                DarkNavyBackground,
                Color(0xFF0D1C34),
                Color(0xFF042628),
                DarkNavyBackground
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LightEmeraldBackground,
                Color(0xFFF1F8F5),
                Color(0xFFFFFFFF),
                LightEmeraldBackground
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_tafsir_screen")
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x33FFFFFF) else Color(0x18000000))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x33E5C07B) else Color(0x18065F46)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "The Noble Quran",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "114 Surahs • Multi-Qari • Urdu & English",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Action Icons (Tajweed, Sleep Timer, Bookmarks & Settings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tajweed Legend Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (tajweedEnabled) Color(0x220284C7) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { showTajweedLegendDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Tajweed Guide",
                            tint = if (tajweedEnabled) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Sleep Timer Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (sleepTimerSeconds != null) Color(0x22A855F7) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { showSleepTimerDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerSeconds != null) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Bookmarks Modal Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (bookmarks.isNotEmpty()) GoldPrimary.copy(alpha = 0.2f) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { showBookmarksDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = if (bookmarks.isNotEmpty()) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Display Settings Modal Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Qari Selector Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x24065F46) else Color(0x12065F46))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { showQariDialog = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currentQariProfile.flag, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reciter: ${currentQariProfile.nameEnglish}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Change Qari",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Daily Reading Goal & Khatam Tracker Card
            QuranKhatamTrackerHeader(
                todayVersesRead = todayVersesRead,
                dailyGoal = dailyGoal,
                khatamVersesCompleted = khatamVersesCompleted,
                onOpenTajweedLegend = { showTajweedLegendDialog = true },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Surahs (114) vs Juz / Para (30) Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF13233F) else Color(0xFFE6EEF5))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (currentTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { currentTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (currentTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Surahs (114)",
                            fontSize = 12.sp,
                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (currentTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { currentTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewList,
                            contentDescription = null,
                            tint = if (currentTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Juz / Paras (30)",
                            fontSize = 12.sp,
                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("quran_search_input"),
                placeholder = {
                    Text(
                        if (currentTab == 0) "Search Surah name or number (e.g. Kahf, 18)..."
                        else "Search Juz number or name (e.g. 30, Amma, Alif Lam)...",
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color(0x33E5C07B) else Color(0x28065F46)
                )
            )

            if (currentTab == 1) {
                // Juz / Para Navigation View
                JuzListView(
                    searchQuery = searchQuery,
                    onSelectJuz = { juz ->
                        val targetSurah = surahs.firstOrNull { it.id == juz.startSurahId }
                        if (targetSurah != null) {
                            selectedSurah = targetSurah
                            currentTab = 0
                            searchQuery = ""
                            scope.launch {
                                preferences.saveLastRead(
                                    targetSurah.id,
                                    juz.startVerse,
                                    targetSurah.nameEnglish,
                                    targetSurah.nameArabic
                                )
                                if (juz.startVerse > 1) {
                                    readerListState.animateScrollToItem(juz.startVerse.coerceAtLeast(0))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Resume Last Read Hero Banner (if available)
                if (lastReadInfo != null && searchQuery.isEmpty()) {
                    val lastRead = lastReadInfo!!
                    ResumeReadingHero(
                        lastRead = lastRead,
                        onResume = {
                            val targetSurah = surahs.firstOrNull { it.id == lastRead.surahId }
                            if (targetSurah != null) {
                                selectedSurah = targetSurah
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Horizontal Surah Quick Chips
                if (searchQuery.isEmpty() && surahs.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(surahs.take(10)) { surah ->
                            val isSelected = surah.id == selectedSurah?.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                                    .clickable {
                                        selectedSurah = surah
                                        scope.launch {
                                            preferences.saveLastRead(surah.id, 1, surah.nameEnglish, surah.nameArabic)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${surah.id}. ${surah.nameEnglish}",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Main Content: Surah Banner + Ayahs List
                LazyColumn(
                    state = readerListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                // If search query active, list matching Surahs
                if (searchQuery.isNotBlank()) {
                    items(filteredSurahs) { surah ->
                        SurahListItem(
                            surah = surah,
                            isSelected = surah.id == selectedSurah?.id,
                            onClick = {
                                selectedSurah = surah
                                searchQuery = ""
                                scope.launch {
                                    preferences.saveLastRead(surah.id, 1, surah.nameEnglish, surah.nameArabic)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // Top section: Selected Surah Header Banner with Audio & Offline Download
                    selectedSurah?.let { surah ->
                        item {
                            val downloadKey = "${activeQari.name}_${surah.id}"
                            val downloadProgress = downloadProgressMap[downloadKey]
                            val isDownloaded = QuranDownloadManager.isSurahDownloaded(context, activeQari, surah.id)

                            SurahHeaderBanner(
                                surah = surah,
                                isPlaying = isPlaying && activeSurahId == surah.id,
                                activeQari = currentQariProfile,
                                isDownloaded = isDownloaded,
                                downloadProgress = downloadProgress,
                                onPlayAudio = {
                                    QuranAudioController.playSurahWithQari(
                                        context = context,
                                        surahId = surah.id,
                                        surahName = surah.nameEnglish,
                                        qariId = activeQari
                                    )
                                },
                                onTogglePlay = {
                                    QuranAudioController.togglePlayPause(context)
                                },
                                onDownload = {
                                    QuranDownloadManager.startDownload(context, activeQari, surah.id)
                                },
                                onDeleteDownload = {
                                    QuranDownloadManager.deleteDownloadedSurah(context, activeQari, surah.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Ayahs Section for Selected Surah
                    selectedSurah?.let { surah ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ayahs & Commentary",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${ayahList.size} of ${surah.totalVerses} loaded",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (ayahList.isEmpty()) {
                            item {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Loading Ayahs from Room Database...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(ayahList, key = { it.globalVerseNumber }) { ayah ->
                                val isCurrentAyahSync = isPlaying &&
                                        activeSurahId == ayah.surahId &&
                                        currentPositionMs in ayah.audioStartTimeMs..ayah.audioEndTimeMs

                                val isBookmarked = bookmarks.any { it.surahId == ayah.surahId && it.verseNumber == ayah.verseNumber }
                                val isLoopingThisAyah = loopingAyah?.surahId == ayah.surahId && loopingAyah?.verseNumber == ayah.verseNumber

                                AyahCardItem(
                                    ayah = ayah,
                                    isHighlighted = isCurrentAyahSync,
                                    isBookmarked = isBookmarked,
                                    isLooping = isLoopingThisAyah,
                                    arabicFontSize = arabicFontSize,
                                    showEnglish = showEnglish,
                                    showUrdu = showUrdu,
                                    showTajweed = tajweedEnabled,
                                    onOpenTafsir = { selectedTafsirAyah = ayah },
                                    onOpenWordByWord = { selectedWordByWordAyah = ayah },
                                    onPlayFromHere = {
                                        selectedSurah?.let { s ->
                                            QuranAudioController.playSurahWithQari(
                                                context = context,
                                                surahId = s.id,
                                                surahName = s.nameEnglish,
                                                qariId = activeQari,
                                                startPositionMs = ayah.audioStartTimeMs
                                            )
                                            scope.launch {
                                                preferences.saveLastRead(s.id, ayah.verseNumber, s.nameEnglish, s.nameArabic)
                                                preferences.incrementVersesRead()
                                            }
                                        }
                                    },
                                    onToggleBookmark = {
                                        selectedSurah?.let { s ->
                                            scope.launch {
                                                repository.toggleBookmark(
                                                    surahId = ayah.surahId,
                                                    verseNumber = ayah.verseNumber,
                                                    surahNameEnglish = s.nameEnglish,
                                                    surahNameArabic = s.nameArabic,
                                                    textArabic = ayah.textArabic,
                                                    textEnglish = ayah.textEnglishTranslation,
                                                    textUrdu = ayah.textUrduTranslation
                                                )
                                            }
                                        }
                                    },
                                    onLoopAyah = {
                                        selectedSurah?.let { s ->
                                            val nextMode = when (repeatMode) {
                                                HifzRepeatMode.OFF -> HifzRepeatMode.REPEAT_3X
                                                HifzRepeatMode.REPEAT_3X -> HifzRepeatMode.REPEAT_5X
                                                HifzRepeatMode.REPEAT_5X -> HifzRepeatMode.REPEAT_INFINITE
                                                else -> HifzRepeatMode.OFF
                                            }
                                            QuranAudioController.playAyahLoop(
                                                context = context,
                                                surahId = s.id,
                                                surahName = s.nameEnglish,
                                                qariId = activeQari,
                                                ayah = ayah,
                                                repeatMode = nextMode
                                            )
                                        }
                                    },
                                    onCopyAyah = {
                                        copyVerseToClipboard(context, ayah, selectedSurah?.nameEnglish ?: "Quran")
                                    },
                                    onShareAyah = {
                                        shareVerse(context, ayah, selectedSurah?.nameEnglish ?: "Quran")
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            }

            // Bottom Sticky Audio Dock Player
            if (isPlaying || currentPositionMs > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                DockedAudioPlayer(
                    surahName = activeSurahName,
                    currentPosMs = currentPositionMs,
                    totalDurMs = currentDurationMs,
                    isPlaying = isPlaying,
                    qariName = currentQariProfile.nameEnglish,
                    isOffline = isOfflineAudio,
                    playbackSpeed = playbackSpeed,
                    repeatMode = repeatMode,
                    sleepTimerRemainingSeconds = sleepTimerSeconds,
                    onTogglePlay = { QuranAudioController.togglePlayPause(context) },
                    onStop = { QuranAudioController.stopAudio() },
                    onSeek = { QuranAudioController.seekTo(it) },
                    onCycleSpeed = {
                        val nextSpeed = when (playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 0.75f
                            else -> 1.0f
                        }
                        QuranAudioController.setPlaybackSpeed(nextSpeed)
                    },
                    onCycleRepeat = {
                        val nextMode = when (repeatMode) {
                            HifzRepeatMode.OFF -> HifzRepeatMode.REPEAT_1X
                            HifzRepeatMode.REPEAT_1X -> HifzRepeatMode.REPEAT_3X
                            HifzRepeatMode.REPEAT_3X -> HifzRepeatMode.REPEAT_5X
                            HifzRepeatMode.REPEAT_5X -> HifzRepeatMode.REPEAT_INFINITE
                            HifzRepeatMode.REPEAT_INFINITE -> HifzRepeatMode.OFF
                        }
                        QuranAudioController.setRepeatMode(nextMode)
                    },
                    onOpenSleepTimer = { showSleepTimerDialog = true }
                )
            }
        }

        // Tafsir Ibn Kathir Dialog
        selectedTafsirAyah?.let { ayah ->
            TafsirDialog(
                ayah = ayah,
                onDismiss = { selectedTafsirAyah = null }
            )
        }

        // Word By Word Dialog
        selectedWordByWordAyah?.let { ayah ->
            WordByWordDialog(
                ayah = ayah,
                surahNameEnglish = selectedSurah?.nameEnglish ?: "Surah",
                onDismiss = { selectedWordByWordAyah = null }
            )
        }

        // Tajweed Legend Dialog
        if (showTajweedLegendDialog) {
            TajweedLegendDialog(onDismiss = { showTajweedLegendDialog = false })
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentRemainingSeconds = sleepTimerSeconds,
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        // Qari Selection Dialog
        if (showQariDialog) {
            QariSelectionDialog(
                currentQariId = activeQari,
                onSelectQari = { qari ->
                    preferences.setQari(qari.id)
                    if (isPlaying) {
                        selectedSurah?.let { s ->
                            QuranAudioController.playSurahWithQari(
                                context = context,
                                surahId = s.id,
                                surahName = s.nameEnglish,
                                qariId = qari.id,
                                startPositionMs = currentPositionMs
                            )
                        }
                    }
                },
                onDismiss = { showQariDialog = false }
            )
        }

        // Display Settings Dialog
        if (showSettingsDialog) {
            QuranDisplaySettingsDialog(
                arabicFontSize = arabicFontSize,
                showEnglish = showEnglish,
                showUrdu = showUrdu,
                showTajweed = tajweedEnabled,
                dailyGoal = dailyGoal,
                onFontSizeChange = { size ->
                    preferences.setArabicFontSize(size)
                },
                onToggleEnglish = { enabled ->
                    preferences.setShowEnglish(enabled)
                },
                onToggleUrdu = { enabled ->
                    preferences.setShowUrdu(enabled)
                },
                onToggleTajweed = { enabled ->
                    preferences.setTajweedEnabled(enabled)
                },
                onDailyGoalChange = { goal ->
                    preferences.setDailyGoal(goal)
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Bookmarks Dialog
        if (showBookmarksDialog) {
            QuranBookmarksDialog(
                bookmarks = bookmarks,
                onSelectBookmark = { bookmark ->
                    val targetSurah = surahs.firstOrNull { it.id == bookmark.surahId }
                    if (targetSurah != null) {
                        selectedSurah = targetSurah
                    }
                },
                onDeleteBookmark = { bookmark ->
                    scope.launch {
                        repository.deleteBookmark(bookmark.surahId, bookmark.verseNumber)
                    }
                },
                onDismiss = { showBookmarksDialog = false }
            )
        }
    }
}

@Composable
private fun ResumeReadingHero(
    lastRead: LastReadInfo,
    onResume: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    if (isDark) listOf(Color(0xFF0F2E28), Color(0xFF1B3B30))
                    else listOf(Color(0xFFE6F4EA), Color(0xFFF1F8F5))
                )
            )
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { onResume() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Resume Last Reading",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Surah ${lastRead.surahNameEnglish} : Ayah ${lastRead.verseNumber}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Resume",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SurahHeaderBanner(
    surah: SurahEntity,
    isPlaying: Boolean,
    activeQari: com.example.service.audio.QariProfile,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onPlayAudio: () -> Unit,
    onTogglePlay: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33E5C07B) else Color(0x18065F46)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${surah.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = surah.nameEnglish,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${surah.nameTranslation} • ${surah.revelationType} • ${surah.totalVerses} Ayahs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = surah.nameArabic,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Controls Bar with Multi-Qari & Offline Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x22E5C07B) else Color(0x14065F46))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                if (isPlaying) onTogglePlay() else onPlayAudio()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Reciter: ${activeQari.nameEnglish}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPlaying) "Playing live audio • Word sync active" else "Tap play to stream or download",
                            fontSize = 11.sp,
                            color = if (isPlaying) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Download Button / Status
                when {
                    downloadProgress != null && downloadProgress in 0.0f..0.99f -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    isDownloaded -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuccessGreen.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownloadDone,
                                        contentDescription = "Downloaded",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Offline", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onDeleteDownload, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Offline Audio",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                                .clickable { onDownload() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download MP3",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Download",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahCardItem(
    ayah: AyahEntity,
    isHighlighted: Boolean,
    isBookmarked: Boolean,
    isLooping: Boolean,
    arabicFontSize: Float,
    showEnglish: Boolean,
    showUrdu: Boolean,
    showTajweed: Boolean,
    onOpenTafsir: () -> Unit,
    onOpenWordByWord: () -> Unit,
    onPlayFromHere: () -> Unit,
    onToggleBookmark: () -> Unit,
    onLoopAyah: () -> Unit,
    onCopyAyah: () -> Unit,
    onShareAyah: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val highlightBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFF10B981) else Color.Transparent,
        label = "highlightBorder"
    )
    val highlightBgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0x2610B981) else Color.Transparent,
        label = "highlightBg"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isHighlighted) 1.5.dp else 0.dp, highlightBorderColor, RoundedCornerShape(18.dp))
            .background(highlightBgColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Verse Number header with Play, Loop, Bookmark & Tafsir buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x33E5C07B) else Color(0x18065F46))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ayah ${ayah.verseNumber}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play audio from this verse
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onPlayFromHere() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Verse",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Play", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Hifz Repeat Loop Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLooping) EmeraldAccent.copy(alpha = 0.25f) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onLoopAyah() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Hifz Loop",
                                tint = if (isLooping) EmeraldAccent else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isLooping) "Looping" else "Loop",
                                fontSize = 10.5.sp,
                                color = if (isLooping) EmeraldAccent else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Bookmark Star Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBookmarked) GoldPrimary.copy(alpha = 0.25f) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onToggleBookmark() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tafsir Ibn Kathir button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0x22E5C07B) else Color(0x18065F46))
                            .clickable { onOpenTafsir() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tafsir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Tafsir", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text (Right-to-left layout with optional Tajweed Color Rules)
            val baseColor = MaterialTheme.colorScheme.onSurface
            if (showTajweed) {
                val tajweedAnnotated = remember(ayah.textArabic, isDark, baseColor) {
                    TajweedColorRules.buildTajweedAnnotatedString(
                        arabicText = ayah.textArabic,
                        isDark = isDark,
                        isEnabled = true,
                        defaultColor = baseColor
                    )
                }
                Text(
                    text = tajweedAnnotated,
                    fontSize = arabicFontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right,
                    lineHeight = (arabicFontSize * 1.6f).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = ayah.textArabic,
                    fontSize = arabicFontSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = baseColor,
                    textAlign = TextAlign.Right,
                    lineHeight = (arabicFontSize * 1.6f).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // English Translation (if enabled)
            if (showEnglish && ayah.textEnglishTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ayah.textEnglishTranslation,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            // Urdu Translation (if enabled)
            if (showUrdu && ayah.textUrduTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x1C10B981) else Color(0x1010B981))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = ayah.textUrduTranslation,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                        textAlign = TextAlign.Right,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isHighlighted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reciting now • Word-by-word active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action strip: Word-by-Word Analysis, Copy, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word-by-Word Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x2238BDF8) else Color(0x180284C7))
                        .clickable { onOpenWordByWord() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Word by Word",
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Word by Word",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy Ayah
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onCopyAyah() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Ayah",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Share Ayah
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onShareAyah() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Ayah",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahListItem(
    surah: SurahEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0x22E5C07B) else Color(0x18065F46),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0x22FFFFFF) else Color(0x14000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${surah.id}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = surah.nameEnglish,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${surah.nameTranslation} • ${surah.totalVerses} Ayahs",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = surah.nameArabic,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DockedAudioPlayer(
    surahName: String,
    currentPosMs: Long,
    totalDurMs: Long,
    isPlaying: Boolean,
    qariName: String,
    isOffline: Boolean,
    playbackSpeed: Float,
    repeatMode: HifzRepeatMode,
    sleepTimerRemainingSeconds: Int?,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenSleepTimer: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val progress = if (totalDurMs > 0) (currentPosMs.toFloat() / totalDurMs).coerceIn(0f, 1f) else 0f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quran_docked_player"),
        shape = RoundedCornerShape(18.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Surah $surahName",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOffline) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SuccessGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Offline MP3",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$qariName • Word Sync",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Playback Speed Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            .clickable { onCycleSpeed() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Repeat Mode Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (repeatMode != HifzRepeatMode.OFF) EmeraldAccent.copy(alpha = 0.25f)
                                else if (isDark) Color(0x22FFFFFF) else Color(0x14000000)
                            )
                            .clickable { onCycleRepeat() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != HifzRepeatMode.OFF) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = repeatMode.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (repeatMode != HifzRepeatMode.OFF) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Sleep Timer Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (sleepTimerRemainingSeconds != null && sleepTimerRemainingSeconds > 0) GoldPrimary.copy(alpha = 0.25f)
                                else if (isDark) Color(0x22FFFFFF) else Color(0x14000000)
                            )
                            .clickable { onOpenSleepTimer() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingSeconds != null && sleepTimerRemainingSeconds > 0) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            if (sleepTimerRemainingSeconds != null && sleepTimerRemainingSeconds > 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                                val mins = sleepTimerRemainingSeconds / 60
                                val secs = sleepTimerRemainingSeconds % 60
                                Text(
                                    text = String.format(java.util.Locale.US, "%d:%02d", mins, secs),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${formatAudioTime(currentPosMs)} / ${formatAudioTime(totalDurMs)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onStop, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(17.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val targetMs = (newProgress * totalDurMs).toLong()
                    onSeek(targetMs)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (isDark) Color(0x33E5C07B) else Color(0x28065F46)
                )
            )
        }
    }
}

@Composable
private fun TafsirDialog(
    ayah: AyahEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tafsir Ibn Kathir",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Surah ${ayah.surahId}, Ayah ${ayah.verseNumber}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = ayah.textArabic,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Authentic Commentary:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ayah.tafsirIbnKathir,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Close Tafsir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

private fun copyVerseToClipboard(context: Context, ayah: AyahEntity, surahName: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = "${ayah.textArabic}\n\n\"${ayah.textEnglishTranslation}\"\n\n— Quran, Surah $surahName (${ayah.surahId}:${ayah.verseNumber})"
    val clip = ClipData.newPlainText("Quran Ayah", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Ayah copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareVerse(context: Context, ayah: AyahEntity, surahName: String) {
    val text = "${ayah.textArabic}\n\n\"${ayah.textEnglishTranslation}\"\n\n— Quran, Surah $surahName (${ayah.surahId}:${ayah.verseNumber})"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Noble Quran Verse"))
}
