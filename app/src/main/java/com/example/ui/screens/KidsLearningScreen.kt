package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CrimsonError
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

data class KidsVideoEpisode(
    val id: String,
    val title: String,
    val category: String,
    val duration: String,
    val ageRating: String = "Ages 4-12",
    val videoUrl: String,
    val colorAccent: Color,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val correctIndex: Int
)

/**
 * MODULE 8: ISLAMIC KIDS E-LEARNING & 4K MEDIA STREAMING
 * Netflix-style horizontal carousels with animated Prophet Stories & Tajweed cartoons.
 * ExoPlayer media streaming container with Picture-in-Picture (PiP) support
 * and an interactive Gamified Quiz Overlay with star rewards!
 */
@OptIn(UnstableApi::class)
@Composable
fun KidsLearningScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val episodes = remember {
        listOf(
            KidsVideoEpisode(
                id = "1",
                title = "Prophet Ibrahim (AS) & The Holy Kaaba",
                category = "Stories of the Prophets",
                duration = "12:40",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "Who built the Holy Kaaba with Prophet Ibrahim (AS)?",
                quizOptions = listOf("Prophet Ismail (AS)", "Prophet Musa (AS)", "Prophet Nuh (AS)", "Prophet Adam (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "2",
                title = "The Ark of Prophet Nuh (AS)",
                category = "Stories of the Prophets",
                duration = "14:15",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                colorAccent = RadiantEmerald,
                quizQuestion = "What pairs did Prophet Nuh (AS) take onto the Ark?",
                quizOptions = listOf("Only birds", "A male and female of every creature", "Only domestic sheep", "None"),
                correctIndex = 1
            ),
            KidsVideoEpisode(
                id = "3",
                title = "Tajweed Fun: The Letters of Qalqalah",
                category = "Tajweed & Arabic Alphabet",
                duration = "08:30",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                colorAccent = GoldenAmber,
                quizQuestion = "Which phrase helps us remember all 5 Qalqalah letters?",
                quizOptions = listOf("Qutb Jadd (ق ط ب ج د)", "Hayyun Qayyum", "Al-Fatihah", "Makharij"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "4",
                title = "The Manners of Eating (Sunnah for Kids)",
                category = "Daily Islamic Morals",
                duration = "06:50",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "What should we say before starting to eat?",
                quizOptions = listOf("Alhamdulillah", "Bismillah", "Astaghfirullah", "Allahu Akbar"),
                correctIndex = 1
            ),
            KidsVideoEpisode(
                id = "5",
                title = "Prophet Yunus (AS) in the Whale",
                category = "Stories of the Prophets",
                duration = "11:20",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                colorAccent = RadiantEmerald,
                quizQuestion = "What Dua did Prophet Yunus make inside the whale?",
                quizOptions = listOf("La ilaha illa Anta Subhanaka inni kuntu minaz-zalimin", "Rabbi zidni 'ilma", "Hasbunallahu wa ni'mal wakeel", "Rabbana atina fid-dunya"),
                correctIndex = 0
            )
        )
    }

    var currentPlayingEpisode by remember { mutableStateOf(episodes[0]) }
    var isPlayingVideo by remember { mutableStateOf(false) }
    var isQuizOpen by remember { mutableStateOf(false) }
    var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }
    var hasAnsweredCorrectly by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(12) }

    // ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    DisposableEffect(currentPlayingEpisode) {
        val mediaItem = MediaItem.fromUri(currentPlayingEpisode.videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Function to trigger Android Picture-in-Picture
    fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = context as? Activity ?: return
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } else {
            Toast.makeText(context, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("kids_learning_screen")
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
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepRoyalEmerald)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Islamic Kids Cinema & Academy",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Prophet Stories, Tajweed & Virtues",
                            fontSize = 11.sp,
                            color = PlatinumGold
                        )
                    }
                }

                // Stars Trophy Counter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldenSun.copy(alpha = 0.4f))
                        .border(1.dp, PlatinumGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = PlatinumGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$earnedStars Stars", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = DeepRoyalEmerald)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Video Player Container
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                cornerRadius = 24.dp,
                backgroundColor = Color.Black
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // ExoPlayer View
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // PiP Button in Player Corner
                    IconButton(
                        onClick = { enterPiP() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "PiP", tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Playing Details & Take Quiz Button
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPlayingEpisode.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "${currentPlayingEpisode.category} • ${currentPlayingEpisode.duration} • ${currentPlayingEpisode.ageRating}",
                            fontSize = 11.sp,
                            color = SlateMuted
                        )
                    }

                    Button(
                        onClick = {
                            selectedQuizAnswer = null
                            hasAnsweredCorrectly = false
                            isQuizOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = PlatinumGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Play Quiz", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 1: Stories of the Prophets (Qisas al-Anbiya)
            Text(
                text = "STORIES OF THE PROPHETS (QISAS AL-ANBIYA)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlatinumGold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(episodes.filter { it.category == "Stories of the Prophets" }) { ep ->
                    KidsEpisodeCard(
                        episode = ep,
                        isSelected = ep.id == currentPlayingEpisode.id,
                        onSelect = {
                            currentPlayingEpisode = ep
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Row 2: Tajweed & Islamic Morals
            Text(
                text = "TAJWEED & PROPHETIC MORALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlatinumGold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(episodes.filter { it.category != "Stories of the Prophets" }) { ep ->
                    KidsEpisodeCard(
                        episode = ep,
                        isSelected = ep.id == currentPlayingEpisode.id,
                        onSelect = {
                            currentPlayingEpisode = ep
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                        }
                    )
                }
            }
        }

        // Gamified Quiz Modal Overlay
        if (isQuizOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 16.dp,
                    cornerRadius = 24.dp,
                    backgroundColor = PureWhite,
                    borderColor = PlatinumGold
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STORY CHALLENGE QUIZ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumGold,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { isQuizOpen = false }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentPlayingEpisode.quizQuestion,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Options
                        currentPlayingEpisode.quizOptions.forEachIndexed { idx, option ->
                            val isSelected = selectedQuizAnswer == idx
                            val isCorrect = idx == currentPlayingEpisode.correctIndex
                            val optionBg = when {
                                selectedQuizAnswer != null && isCorrect -> Color(0xFFF0FDF4)
                                selectedQuizAnswer == idx && !isCorrect -> Color(0xFFFEF2F2)
                                isSelected -> EmeraldSurfaceLight
                                else -> Color(0xFFF8FAFC)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(optionBg)
                                    .border(
                                        1.dp,
                                        if (selectedQuizAnswer != null && isCorrect) RadiantEmerald else Color(0xFFE2E8F0),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (selectedQuizAnswer == null) {
                                            selectedQuizAnswer = idx
                                            if (isCorrect) {
                                                hasAnsweredCorrectly = true
                                                earnedStars += 3
                                            }
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = CharcoalPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedQuizAnswer != null) {
                            if (hasAnsweredCorrectly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🌟 MASHALLAH! CORRECT! (+3 STARS)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RadiantEmerald
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { isQuizOpen = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald)
                                    ) {
                                        Text("Continue Watching")
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Try again next time! Keep learning!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonError
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { isQuizOpen = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateMuted)
                                    ) {
                                        Text("Close")
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

@Composable
fun KidsEpisodeCard(
    episode: KidsVideoEpisode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier
            .width(210.dp)
            .clickable { onSelect() },
        elevation = 5.dp,
        cornerRadius = 18.dp,
        backgroundColor = PureWhite,
        borderColor = if (isSelected) PlatinumGold else Color(0x150F172A)
    ) {
        Column {
            // Thumbnail Mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                episode.colorAccent,
                                DeepRoyalEmerald
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = DeepRoyalEmerald, modifier = Modifier.size(24.dp))
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = episode.duration, fontSize = 9.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = episode.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.ageRating,
                    fontSize = 10.sp,
                    color = SlateMuted
                )
            }
        }
    }
}
