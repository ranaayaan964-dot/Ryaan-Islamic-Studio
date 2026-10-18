package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.util.Locale
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

private data class OfflineProphetStory(val story: String, val moral: String)

private val offlineProphetStories = mapOf(
    "1" to OfflineProphetStory(
        story = "Allah created Adam (AS) and taught him knowledge. Adam and his wife lived in the Garden, but after they made a mistake, they turned back to Allah with sincere repentance. Allah accepted their repentance and sent them to live on earth, where Adam became the first prophet and taught his family to worship Allah.",
        moral = "When we make a mistake, we should admit it, ask Allah for forgiveness, and try to do better."
    ),
    "2" to OfflineProphetStory(
        story = "Prophet Nuh (AS) called his people to worship Allah alone for many years. When they refused, Allah told him to build an Ark. Nuh (AS) and the believers entered it, and pairs of animals were taken aboard. Allah saved them through the flood, and the Ark came to rest by His mercy.",
        moral = "Stay patient when doing what is right, and trust Allah even when a challenge feels difficult."
    ),
    "3" to OfflineProphetStory(
        story = "Prophet Ibrahim (AS) taught his people to worship Allah alone and not the idols they had made. He remained firm in faith when his people opposed him, and Allah protected him. Later, Ibrahim (AS) and his son Ismail (AS) raised the foundations of the Kaaba in Makkah as a place of worship for Allah.",
        moral = "Be brave in faith, think carefully about what is true, and obey Allah with sincerity."
    ),
    "4" to OfflineProphetStory(
        story = "Prophet Yusuf (AS) faced separation from his family and many unfair hardships. He stayed patient, honest, and mindful of Allah. Allah eventually gave him a position of trust in Egypt. When his brothers came to him in need, Yusuf (AS) forgave them and reunited with his family.",
        moral = "Patience, honesty, and forgiveness can turn a hard time into a chance to do good."
    ),
    "5" to OfflineProphetStory(
        story = "Prophet Ayyub (AS) endured a long period of hardship and called upon Allah with humility. He did not lose hope or turn away from his Lord. Allah answered his prayer, removed his suffering, and restored blessings to him.",
        moral = "Ask Allah for help, remain patient, and remember that hardship does not last forever."
    ),
    "6" to OfflineProphetStory(
        story = "Prophet Yunus (AS) left his people before receiving Allah's command to do so. He was swallowed by a great fish and called upon Allah from the darkness, admitting his mistake and praising Him. Allah answered his prayer, rescued him, and gave him another opportunity to guide his people.",
        moral = "Turn back to Allah in every difficulty and never lose hope in His mercy."
    ),
    "7" to OfflineProphetStory(
        story = "Prophet Musa (AS) was sent to call Pharaoh and his people to worship Allah and free the Children of Israel from oppression. When Musa (AS) and his people reached the sea, Allah made a path through it for them. They crossed safely, while Pharaoh's pursuit came to an end. Allah protected the believers.",
        moral = "Keep faith in Allah and stand up for justice, even when you feel outnumbered."
    ),
    "8" to OfflineProphetStory(
        story = "Prophet Dawud (AS) was given wisdom and prophethood. As a young believer, he faced Jalut with courage, and Allah granted him victory. Later, Dawud (AS) ruled with justice, remembered Allah often, and used his gifts in worship and service.",
        moral = "Use courage and ability to serve what is right, and treat people fairly."
    ),
    "9" to OfflineProphetStory(
        story = "Allah gave Prophet Sulaiman (AS) great gifts, wisdom, and authority. He understood that every blessing came from Allah. When he heard the warning of a small ant to its community, he smiled and thanked Allah. He used his authority with gratitude and care.",
        moral = "Be grateful for what you have, and treat even the smallest creatures with care."
    ),
    "10" to OfflineProphetStory(
        story = "The Quran tells of a man who passed a ruined town. Allah caused him to die for a hundred years and then brought him back to life, showing him how He restores what has died. The Quran does not name this man; some later traditions identify him as Uzair. The account teaches about Allah's power to resurrect.",
        moral = "Remember that Allah has power over all things, and speak carefully when a sacred text leaves a detail unnamed."
    ),
    "11" to OfflineProphetStory(
        story = "Prophet Ibrahim (AS) brought his young son Ismail (AS) and Hajar to the valley of Makkah, trusting Allah's care. When their water ran out, Hajar searched between Safa and Marwah for help. Allah provided the spring of Zamzam. Later, Ismail (AS) helped his father raise the Kaaba, and both prayed that Allah would accept their work.",
        moral = "Trust Allah, keep trying in hard moments, and work together in doing good."
    ),
    "12" to OfflineProphetStory(
        story = "Prophet Isa (AS) was born to Maryam (AS) by Allah's command. Allah made Isa (AS) a prophet and gave him signs, including healing the sick by Allah's permission. Isa (AS) called people to worship Allah and live faithfully. Muslims honor him as a messenger of Allah and honor his mother Maryam.",
        moral = "Be kind, speak truthfully, and remember that every good gift comes from Allah."
    ),
    "13" to OfflineProphetStory(
        story = "Prophet Muhammad (SAW) was born in Makkah and became known for honesty and trustworthiness. At the age of forty, he received the first revelation and called people to worship Allah alone, care for the vulnerable, and live with good character. After facing hardship in Makkah, he migrated to Madinah, where the Muslim community grew. He taught mercy, patience, and fairness until his death.",
        moral = "Follow the Prophet's example through honesty, mercy, patience, and care for others."
    )
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
                title = "Prophet Adam (AS)",
                description = "Learn about the first human and prophet, and how Allah created the beginning of humanity with mercy and guidance.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=uvhAUB2VxMg",
                colorAccent = RadiantEmerald,
                quizQuestion = "Who was the first human and prophet?",
                quizOptions = listOf("Prophet Adam (AS)", "Prophet Nuh (AS)", "Prophet Musa (AS)", "Prophet Ibrahim (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "2",
                title = "Prophet Nuh (AS)",
                description = "The inspiring story of the Ark, patience, and the promise of Allah's mercy in the face of hardship.",
                category = "Stories of the Prophets",
                duration = "12:15",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=NgBkdtrrKLg",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "What pairs were saved by Allah on the Ark?",
                quizOptions = listOf("A male and female pair of every creature", "Only birds", "Only animals", "No one"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "3",
                title = "Prophet Ibrahim (AS)",
                description = "The Kaaba, trust in Allah, and the courage to worship Him alone.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 5-12",
                youtubeUrl = "https://www.youtube.com/watch?v=-5HQ9iH0zQY",
                colorAccent = GoldenAmber,
                quizQuestion = "Who helped Prophet Ibrahim (AS) raise the foundations of the Kaaba?",
                quizOptions = listOf("Prophet Ismail (AS)", "Prophet Musa (AS)", "Prophet Yunus (AS)", "Prophet Isa (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "4",
                title = "Prophet Yusuf (AS)",
                description = "Patience through hardship, faithfulness, and forgiveness.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 5-12",
                youtubeUrl = "https://www.youtube.com/watch?v=19SbvfIKu_w",
                colorAccent = RadiantEmerald,
                quizQuestion = "Which quality did Prophet Yusuf (AS) show when he forgave his brothers?",
                quizOptions = listOf("Forgiveness", "Pride", "Impatience", "Jealousy"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "5",
                title = "Prophet Ayyub (AS)",
                description = "A story of patience, prayer, and trust in Allah during hardship.",
                category = "Stories of the Prophets",
                duration = "13:45",
                ageRating = "Ages 5-14",
                youtubeUrl = "https://www.youtube.com/watch?v=9ZIYQhfSe6s",
                colorAccent = GoldenAmber,
                quizQuestion = "Which quality did Prophet Ayyub (AS) show during hardship?",
                quizOptions = listOf("Patience", "Envy", "Anger", "Pride"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "6",
                title = "Prophet Yunus (AS) & The Whale",
                description = "A story of prayer, repentance, and Allah's mercy in hardship.",
                category = "Stories of the Prophets",
                duration = "12:00",
                ageRating = "Ages 5-14",
                youtubeUrl = "https://www.youtube.com/watch?v=4hKbaQ17rFs",
                colorAccent = GoldenAmber,
                quizQuestion = "What supplication did Prophet Yunus (AS) make in the darkness?",
                quizOptions = listOf("La ilaha illa Anta Subhanaka inni kuntu minaz-zalimin", "Alhamdulillah", "Astaghfirullah", "Bismillah"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "7",
                title = "Prophet Musa (AS)",
                description = "The sea, courage, and trust in Allah when facing oppression.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=s3dvuMlsuy8",
                colorAccent = RadiantEmerald,
                quizQuestion = "What miracle was Prophet Musa (AS) granted at the sea?",
                quizOptions = listOf("The sea parted", "A mountain moved", "It rained gold", "A river appeared"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "8",
                title = "Prophet Dawud (AS) vs Jalut",
                description = "The Quranic account of Talut's group, Jalut, and Dawud's victory by Allah's permission.",
                category = "Stories of the Prophets",
                duration = "11:00",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=D22G5RFef1M",
                colorAccent = Color(0xFF0284C7),
                quizQuestion = "Who defeated Jalut by Allah's permission?",
                quizOptions = listOf("Dawud", "Talut", "Musa", "Nuh"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "9",
                title = "Prophet Sulaiman (AS)",
                description = "A story of wisdom, gratitude, and compassion for Allah's creation.",
                category = "Stories of the Prophets",
                duration = "15:00",
                ageRating = "Ages 6-14",
                youtubeUrl = "https://www.youtube.com/watch?v=JbfEZMdgZ_A",
                colorAccent = Color(0xFFD97706),
                quizQuestion = "Who granted Prophet Sulaiman (AS) wisdom and blessings?",
                quizOptions = listOf("Allah", "A king", "A traveler", "A merchant"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "10",
                title = "Prophet Uzair (AS)",
                description = "A Quranic account about Allah bringing a man back to life after a century.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 5-14",
                youtubeUrl = "https://www.youtube.com/watch?v=m-78Vf2TByk",
                colorAccent = Color(0xFF0284C7),
                quizQuestion = "Does the Quran identify the unnamed man in 2:259 as Uzair?",
                quizOptions = listOf("No, the passage does not name him", "Yes, explicitly", "It names him as Musa", "It does not describe him"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "11",
                title = "Prophet Ismail (AS) & Zamzam",
                description = "The account of Ismail's trust in Allah and the Islamic tradition of Hajar and Zamzam.",
                category = "Stories of the Prophets",
                duration = "11:00",
                ageRating = "Ages 5-14",
                youtubeUrl = "https://www.youtube.com/watch?v=hkSkWt48U5Y",
                colorAccent = GoldenAmber,
                quizQuestion = "Who helped Prophet Ibrahim (AS) raise the Kaaba's foundations?",
                quizOptions = listOf("Prophet Ismail (AS)", "Prophet Dawud (AS)", "Prophet Isa (AS)", "Prophet Nuh (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "12",
                title = "Prophet Isa (AS)",
                description = "A gentle, child-friendly introduction to Prophet Isa (AS) and his message of faith and devotion.",
                category = "Stories of the Prophets",
                duration = "10:00",
                ageRating = "Ages 4-12",
                youtubeUrl = "https://www.youtube.com/watch?v=l7s8R9ugb98",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "Who was the mother of Prophet Isa (AS)?",
                quizOptions = listOf("Maryam (AS)", "Asiya (AS)", "Hajar (AS)", "Sarah (AS)"),
                correctIndex = 0
            ),
            KidsVideoEpisode(
                id = "13",
                title = "Prophet Muhammad (SAW)",
                description = "A beautiful introduction to the life, mercy, and character of the final Prophet Muhammad (SAW).",
                category = "Stories of the Prophets",
                duration = "14:20",
                ageRating = "Ages 4-14",
                youtubeUrl = "https://www.youtube.com/watch?v=WEEORXFj-lo",
                colorAccent = DeepRoyalEmerald,
                quizQuestion = "What was the Prophet Muhammad (SAW) known as before prophethood?",
                quizOptions = listOf("Al-Amin (The Trustworthy)", "The Merchant", "The Traveler", "The King"),
                correctIndex = 0
            )
        )
    }

    var selectedEpisode by remember { mutableStateOf(episodes[0]) }
    var readerEpisode by remember { mutableStateOf<KidsVideoEpisode?>(null) }
    var isQuizOpen by remember { mutableStateOf(false) }
    var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }
    var hasAnsweredCorrectly by remember { mutableStateOf(false) }
    var earnedStars by remember { mutableIntStateOf(15) }
    var ttsReady by remember { mutableStateOf(false) }
    val textToSpeech = remember(context) {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }.apply {
            language = Locale.ENGLISH
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun resetQuizState() {
        selectedQuizAnswer = null
        hasAnsweredCorrectly = false
    }

    LaunchedEffect(selectedEpisode.id) {
        resetQuizState()
    }

    fun openYouTubeVideo(episode: KidsVideoEpisode) {
        val videoId = Uri.parse(episode.youtubeUrl).getQueryParameter("v")
        if (!videoId.isNullOrBlank()) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            } catch (_: Exception) {
                // Fall back to the browser if the YouTube app is missing or rejects this video.
            }
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(episode.youtubeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No app is available to open this video.", Toast.LENGTH_SHORT).show()
        }
    }

    fun searchMoreEpisodes(episode: KidsVideoEpisode) {
        val searchUri = Uri.parse("https://www.youtube.com/results")
            .buildUpon()
            .appendQueryParameter("search_query", "${episode.title} Islamic Cartoon Urdu")
            .build()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Toast.makeText(context, "No app is available to search for episodes.", Toast.LENGTH_SHORT).show()
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
                            .clickable { openYouTubeVideo(selectedEpisode) },
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
                                onClick = { openYouTubeVideo(selectedEpisode) },
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
                                    resetQuizState()
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { readerEpisode = selectedEpisode },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Read offline", fontSize = 11.sp, color = DeepRoyalEmerald)
                            }
                            TextButton(
                                onClick = { searchMoreEpisodes(selectedEpisode) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Search episodes", fontSize = 11.sp, color = DeepRoyalEmerald)
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
                            resetQuizState()
                        },
                        onOpen = { openYouTubeVideo(ep) },
                        onSearch = { searchMoreEpisodes(ep) },
                        onRead = { readerEpisode = ep }
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
                            resetQuizState()
                        },
                        onOpen = { openYouTubeVideo(ep) },
                        onSearch = { searchMoreEpisodes(ep) },
                        onRead = { readerEpisode = ep }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        readerEpisode?.let { episode ->
            val storyContent = offlineProphetStories[episode.id]
            if (storyContent != null) {
                AlertDialog(
                    onDismissRequest = { readerEpisode = null },
                    title = {
                        Text(episode.title, color = DeepRoyalEmerald, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("THE STORY", color = PlatinumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(storyContent.story, color = CharcoalPrimary, fontSize = 14.sp, lineHeight = 21.sp)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text("A MORAL TO REMEMBER", color = PlatinumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(storyContent.moral, color = CharcoalPrimary, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (ttsReady) {
                                    textToSpeech.speak(
                                        "${storyContent.story} ${storyContent.moral}",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "offline_story_${episode.id}"
                                    )
                                } else {
                                    Toast.makeText(context, "Text-to-speech is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Listen", color = DeepRoyalEmerald)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { readerEpisode = null }) {
                            Text("Close", color = SlateMuted)
                        }
                    }
                )
            }
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
                                onClick = {
                                    isQuizOpen = false
                                    resetQuizState()
                                },
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
    onOpen: () -> Unit,
    onSearch: () -> Unit,
    onRead: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier
            .width(220.dp),
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
                    .clickable { onOpen() }
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
                    modifier = Modifier.clickable { onSelect() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRead, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text("Read offline", fontSize = 9.5.sp, color = DeepRoyalEmerald, maxLines = 1)
                    }
                    TextButton(onClick = onSearch, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text("Search more", fontSize = 9.5.sp, color = DeepRoyalEmerald, maxLines = 1)
                    }
                }
            }
        }
    }
}
