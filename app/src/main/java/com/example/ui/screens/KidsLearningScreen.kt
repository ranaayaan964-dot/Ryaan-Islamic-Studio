package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
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

data class KidsVideoEpisode(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val duration: String,
    val ageRating: String = "Ages 4-12",
    val youtubeUrl: String,
    val colorAccent: Color,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val correctIndex: Int
)

/**
 * MODULE 8: ISLAMIC KIDS CINEMA & LEARNING ACADEMY (VERSION 1.1 YOUTUBE UPGRADE)
 * Features real authentic YouTube streaming via Android Intent (preventing video player crashes),
 * rich video cards for Stories of the Prophets, Tajweed, and Sunnah morals,
 * plus an interactive gamified Islamic knowledge quiz with star rewards.
 */
@Composable
fun KidsLearningScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onNavigateBack()
    }

    val episodes = remember {
        listOf(
            KidsVideoEpisode(
                id = "1",
                title = "Prophet Muhammad (SAW) - Best of Creation",
                description = "The beautiful life, mercy, honesty, and character of our beloved Prophet Muhammad (peace be upon him).",
                category = "Stories of the Prophets",
                duration = "14:20",
                ageRating = "Ages 4-14",
                youtubeUrl = "https://www.youtube.com/watch?v=Fj2F7ePzV3o",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "What was the Prophet Muhammad (SAW) known as even before prophethood?",
                quizOptions = listOf("Al-Amin (The Trustworthy)", "The Merchant", "The Traveler", "The King"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "2",
                title = "The Ark of Prophet Nuh (AS)",
                description = "The inspiring story of Prophet Nuh building the great Ark with steadfast patience and trusting in Allah.",
                category = "Stories of the Prophets",
                duration = "12:15",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=e_M_66oJq-E",
                colorAccent = RadiantEmerald,
                quizQuestion = "What pairs did Prophet Nuh (AS) take onto the Ark by Allah's command?",
                quizOptions = listOf("Only birds", "A male and female pair of every creature", "Only domestic sheep", "None"),
                correctIndex = 1
            ),
            KidsVideoEpisode(
                id = "3",
                title = "Prophet Ibrahim (AS) & The Holy Kaaba",
                description = "How Prophet Ibrahim and his son Ismail (peace be upon them) built the foundations of the sacred Kaaba in Makkah.",
                category = "Stories of the Prophets",
                duration = "13:45",
                ageRating = "Ages 5-14",
                youtubeUrl = "https://www.youtube.com/watch?v=9jR4rX-C4g4",
                colorAccent = GoldenAmber,
                quizQuestion = "Who helped Prophet Ibrahim (AS) raise the foundations of the Kaaba?",
                quizOptions = listOf("Prophet Ismail (AS)", "Prophet Musa (AS)", "Prophet Yunus (AS)", "Prophet Isa (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "4",
                title = "Prophet Yunus (AS) in the Whale",
                description = "The supplication of Dhun-Noon inside the darkness of the belly of the whale that relieves all distress.",
                category = "Stories of the Prophets",
                duration = "11:10",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=F0vj8K-d-1I",
                colorAccent = Color(0xFF0284C7),
                quizQuestion = "What powerful Dua did Prophet Yunus (AS) make inside the belly of the whale?",
                quizOptions = listOf(
                    "La ilaha illa Anta Subhanaka inni kuntu minaz-zalimin",
                    "Rabbi zidni 'ilma",
                    "Hasbunallahu wa ni'mal wakeel",
                    "Rabbana atina fid-dunya hasanah"
                ),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "5",
                title = "Tajweed Fun: The Letters of Qalqalah",
                description = "Learn how to make the echoing bouncing sound on the 5 letters of Qalqalah (Qutb Jadd) with cartoon animations.",
                category = "Tajweed & Arabic Alphabet",
                duration = "08:30",
                ageRating = "Ages 5-12",
                youtubeUrl = "https://www.youtube.com/watch?v=kYv_8kQ_Wxo",
                colorAccent = Color(0xFF8B5CF6),
                quizQuestion = "Which mnemonic phrase gathers all 5 Qalqalah letters (ق ط ب ج د)?",
                quizOptions = listOf("Qutb Jadd (ق ط ب ج د)", "Hayyun Qayyum", "Al-Fatihah", "Makharij"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "6",
                title = "5 Daily Sunnah Manners for Muslim Children",
                description = "Manners of eating with the right hand, smiling, saying Bismillah, greeting with Salam, and showing respect to elders.",
                category = "Daily Islamic Morals",
                duration = "07:25",
                ageRating = "Ages 4-10",
                youtubeUrl = "https://www.youtube.com/watch?v=8lJmS5H_6pE",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "What should every Muslim child say before starting to eat or drink?",
                quizOptions = listOf("Bismillah (In the name of Allah)", "Alhamdulillah", "Astaghfirullah", "Allahu Akbar"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "7",
                title = "Prophet Musa (AS) & The Parting of the Sea",
                description = "The miracle granted to Prophet Musa by Allah when standing before Pharaoh and the miraculous dry path through the sea.",
                category = "Stories of the Prophets",
                duration = "15:00",
                ageRating = "Ages 6-14",
                youtubeUrl = "https://www.youtube.com/watch?v=15tGZfL-7kU",
                colorAccent = Color(0xFFD97706),
                quizQuestion = "What miracle was Prophet Musa (AS) granted with his staff at the Red Sea?",
                quizOptions = listOf("The sea parted into towering walls of water", "It rained jewels", "A golden ship appeared", "None"),
                correctIndex = 0
            )
        )
    }

    var selectedEpisode by remember { mutableStateOf(episodes[0]) }
    var isQuizOpen by remember { mutableStateOf(false) }
    var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }
    var hasAnsweredCorrectly by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(15) }

    fun openYouTubeVideo(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Opening YouTube video...", Toast.LENGTH_SHORT).show()
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open browser for video.", Toast.LENGTH_SHORT).show()
            }
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
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x150F172A))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Islamic Kids Cinema",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Prophet Stories, Tajweed & Sunnah",
                            fontSize = 11.5.sp,
                            color = PlatinumGold
                        )
                    }
                }

                // Stars Trophy Counter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldenSun.copy(alpha = 0.35f))
                        .border(1.dp, PlatinumGold, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PlatinumGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$earnedStars Stars",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepRoyalEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Featured Hero Card (YouTube Cinema Showcase)
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                cornerRadius = 24.dp,
                backgroundColor = PureWhite,
                borderColor = PlatinumGold
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Visual YouTube Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        selectedEpisode.colorAccent,
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .clickable { openYouTubeVideo(selectedEpisode.youtubeUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        // YouTube Red Play Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000))
                                .border(2.dp, PureWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play on YouTube",
                                tint = PureWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Top Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartDisplay,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0000),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "YouTube HD",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepRoyalEmerald.copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedEpisode.ageRating,
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bottom Duration
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedEpisode.duration,
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Episode Details & Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = selectedEpisode.category.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedEpisode.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedEpisode.description,
                            fontSize = 12.sp,
                            color = SlateMuted,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { openYouTubeVideo(selectedEpisode.youtubeUrl) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFCC0000),
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch on YouTube",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    selectedQuizAnswer = null
                                    hasAnsweredCorrectly = false
                                    isQuizOpen = true
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepRoyalEmerald,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = PlatinumGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Take Quiz",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Carousel 1: Stories of the Prophets (Qisas al-Anbiya)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORIES OF THE PROPHETS",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Tap to Watch",
                    fontSize = 11.sp,
                    color = DeepRoyalEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(episodes.filter { it.category == "Stories of the Prophets" }) { ep ->
                    KidsYouTubeEpisodeCard(
                        episode = ep,
                        isSelected = ep.id == selectedEpisode.id,
                        onSelect = {
                            selectedEpisode = ep
                        },
                        onOpen = {
                            selectedEpisode = ep
                            openYouTubeVideo(ep.youtubeUrl)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Carousel 2: Tajweed & Islamic Morals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAJWEED & SUNNAH MANNERS",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Tap to Watch",
                    fontSize = 11.sp,
                    color = DeepRoyalEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(episodes.filter { it.category != "Stories of the Prophets" }) { ep ->
                    KidsYouTubeEpisodeCard(
                        episode = ep,
                        isSelected = ep.id == selectedEpisode.id,
                        onSelect = {
                            selectedEpisode = ep
                        },
                        onOpen = {
                            selectedEpisode = ep
                            openYouTubeVideo(ep.youtubeUrl)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Gamified Quiz Modal Overlay
        if (isQuizOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = PlatinumGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STORY CHALLENGE QUIZ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlatinumGold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            IconButton(
                                onClick = { isQuizOpen = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = SlateMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedEpisode.quizQuestion,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Options
                        selectedEpisode.quizOptions.forEachIndexed { idx, option ->
                            val isSelected = selectedQuizAnswer == idx
                            val isCorrect = idx == selectedEpisode.correctIndex
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
                                        Text("Continue Learning")
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
fun KidsYouTubeEpisodeCard(
    episode: KidsVideoEpisode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier
            .width(220.dp)
            .clickable { onOpen() },
        elevation = 5.dp,
        cornerRadius = 18.dp,
        backgroundColor = PureWhite,
        borderColor = if (isSelected) PlatinumGold else Color(0x150F172A)
    ) {
        Column {
            // Visual YouTube Thumbnail Mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
                // Play button icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xDDCC0000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch on YouTube",
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // YouTube watermark badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "YouTube",
                        fontSize = 8.5.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.duration,
                        fontSize = 9.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = episode.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = episode.ageRating,
                        fontSize = 10.sp,
                        color = SlateMuted
                    )
                    Text(
                        text = "Watch ▶",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCC0000)
                    )
                }
            }
        }
    }
}
