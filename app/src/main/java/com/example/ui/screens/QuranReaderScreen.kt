package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.data.quran.database.QuranDatabase
import com.example.data.quran.model.AyahEntity
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * MODULE 15: OFFLINE QURAN PAGING ENGINE & WORD-SYNC
 */
@OptIn(UnstableApi::class)
@Composable
fun QuranReaderScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val quranDb = remember { QuranDatabase.getDatabase(context) }

    // MODULE 15: Paging 3 collectAsLazyPagingItems()
    val pager = remember {
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            quranDb.quranDao().getAyahs()
        }
    }
    val lazyAyahItems = pager.flow.collectAsLazyPagingItems()

    // MODULE 15: Media3 ExoPlayer Audio Synchronization
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingAyahIndex by remember { mutableIntStateOf(-1) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    currentPlayingAyahIndex = -1
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Playback position polling loop
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            currentPlaybackPositionMs = exoPlayer.currentPosition
            delay(100L)
        }
    }

    fun playAyahAudio(ayah: AyahEntity) {
        if (currentPlayingAyahIndex == ayah.globalVerseNumber && isPlaying) {
            exoPlayer.pause()
            return
        }

        // Format reciter URL (Alafasy standard 128kbps)
        val surahStr = ayah.surahId.toString().padStart(3, '0')
        val ayahStr = ayah.verseNumber.toString().padStart(3, '0')
        val audioUrl = "https://everyayah.com/data/Alafasy_128kbps/$surahStr$ayahStr.mp3"

        currentPlayingAyahIndex = ayah.globalVerseNumber
        val mediaItem = MediaItem.fromUri(Uri.parse(audioUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PearlBackground, PureWhite, DeepRoyalEmerald.copy(alpha = 0.05f))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("quran_reader_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        exoPlayer.stop()
                        onBack()
                    },
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
                        text = "The Noble Quran (القرآن الكريم)",
                        color = CharcoalPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete 6,236 Ayahs • Paging 3 & Live Audio Sync",
                        color = DeepRoyalEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Paged LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    count = lazyAyahItems.itemCount,
                    key = { index -> lazyAyahItems[index]?.globalVerseNumber ?: index }
                ) { index ->
                    val ayah = lazyAyahItems[index]
                    if (ayah != null) {
                        val isCurrentAyahActive = currentPlayingAyahIndex == ayah.globalVerseNumber

                        // MODULE 15: Radiant Emerald background highlight when actively recited
                        val cardBgColor by animateColorAsState(
                            targetValue = if (isCurrentAyahActive) RadiantEmerald.copy(alpha = 0.14f) else PureWhite,
                            animationSpec = tween(300),
                            label = "AyahCardBg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isCurrentAyahActive) RadiantEmerald else PlatinumGold.copy(alpha = 0.25f),
                            animationSpec = tween(300),
                            label = "AyahBorder"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playAyahAudio(ayah) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentAyahActive) 6.dp else 2.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isCurrentAyahActive) 2.dp else 1.dp,
                                color = borderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                // Surah & Verse badge + Audio button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DeepRoyalEmerald.copy(alpha = 0.10f)
                                    ) {
                                        Text(
                                            text = "Surah ${ayah.surahId}:${ayah.verseNumber} (Ayah ${ayah.globalVerseNumber})",
                                            color = DeepRoyalEmerald,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { playAyahAudio(ayah) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isCurrentAyahActive && isPlaying) RadiantEmerald else DeepRoyalEmerald.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentAyahActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Ayah",
                                            tint = if (isCurrentAyahActive && isPlaying) PureWhite else DeepRoyalEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Arabic Ayah Text with Uthmani Font Style
                                Text(
                                    text = ayah.textArabic,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentAyahActive) DeepRoyalEmerald else CharcoalPrimary,
                                    textAlign = TextAlign.End,
                                    lineHeight = 42.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // English Translation
                                Text(
                                    text = ayah.textEnglishTranslation,
                                    fontSize = 13.5.sp,
                                    color = CharcoalPrimary.copy(alpha = 0.85f),
                                    lineHeight = 20.sp
                                )

                                if (ayah.textUrduTranslation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = ayah.textUrduTranslation,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = DeepRoyalEmerald,
                                        textAlign = TextAlign.End,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.fillMaxWidth()
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
