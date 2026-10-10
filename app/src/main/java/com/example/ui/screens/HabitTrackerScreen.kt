package com.example.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary

data class IslamicBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 1.0f
)

@Composable
fun HabitTrackerScreen() {
    var streakCount by remember { mutableIntStateOf(14) }
    var userXp by remember { mutableIntStateOf(1450) }
    val level = remember(userXp) { (userXp / 300) + 1 }

    // Prayer Completion States
    val completedPrayers = remember {
        mutableStateMapOf(
            "Fajr" to true,
            "Dhuhr" to true,
            "Asr" to true,
            "Maghrib" to false,
            "Isha" to false
        )
    }

    var isFastingDone by remember { mutableStateOf(true) }
    var isCharityDone by remember { mutableStateOf(true) }
    var quranPagesRead by remember { mutableIntStateOf(4) }

    val badges = remember {
        listOf(
            IslamicBadge(
                id = "dawn_devotee",
                title = "Dawn Devotee",
                description = "Prayed Fajr on time 7 days consecutively",
                icon = "🌅",
                isUnlocked = true
            ),
            IslamicBadge(
                id = "steadfast_mumin",
                title = "Steadfast Mumin",
                description = "Completed all 5 prayers for 10 straight days",
                icon = "🛡️",
                isUnlocked = true
            ),
            IslamicBadge(
                id = "generous_heart",
                title = "Generous Heart",
                description = "Gave Sadaqah 5 times this week",
                icon = "💖",
                isUnlocked = true
            ),
            IslamicBadge(
                id = "tahajjud_seeker",
                title = "Tahajjud Seeker",
                description = "Awoke 30 mins before Fajr for Night Prayer",
                icon = "🌙",
                isUnlocked = true
            ),
            IslamicBadge(
                id = "quran_illuminator",
                title = "Quran Illuminator",
                description = "Recited 50 pages of the Noble Quran",
                icon = "📖",
                isUnlocked = false,
                progress = 0.65f
            ),
            IslamicBadge(
                id = "fasting_champion",
                title = "Sawm Champion",
                description = "Fasted Monday & Thursday Sunnah fasts",
                icon = "✨",
                isUnlocked = false,
                progress = 0.80f
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "FlameGlow")
    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Flame"
    )

    fun togglePrayer(prayer: String) {
        val current = completedPrayers[prayer] ?: false
        completedPrayers[prayer] = !current
        if (!current) {
            userXp += 25
        } else {
            userXp = (userXp - 25).coerceAtLeast(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("habit_tracker_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkNavyBackground,
                        Color(0xFF0F172A),
                        Color(0xFF03261F),
                        DarkNavyBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Streak & XP Hero Banner (Duolingo Style)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(24.dp)
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
                        // Flame Streak Counter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .scale(flamePulse)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFF97316), Color(0xFFEF4444), Color.Transparent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak Flame",
                                    tint = Color(0xFFFFEDD5),
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "$streakCount Days Streak!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFED7AA)
                                )
                                Text(
                                    text = "Your spiritual discipline is ablaze",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Level Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33E5C07B))
                                .border(1.dp, Color(0x66E5C07B), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LVL $level",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Devotee",
                                    fontSize = 9.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // XP Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Spiritual XP",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "$userXp / ${(level) * 300} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val levelProgress = ((userXp % 300) / 300f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = EmeraldAccent,
                        trackColor = Color(0x2210B981)
                    )
                }
            }

            // Daily 5 Prayers Checklist
            Text(
                text = "TODAY'S 5 MANDATORY PRAYERS",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = GoldSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prayerList = listOf(
                        Triple("Fajr", "الفجر", "04:35 AM"),
                        Triple("Dhuhr", "الظهر", "12:15 PM"),
                        Triple("Asr", "العصر", "03:45 PM"),
                        Triple("Maghrib", "المغرب", "06:36 PM"),
                        Triple("Isha", "العشاء", "07:55 PM")
                    )

                    prayerList.forEach { (name, arabic, time) ->
                        val isDone = completedPrayers[name] == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDone) Color(0x2210B981) else Color(0x151E293B))
                                .border(
                                    1.dp,
                                    if (isDone) Color(0x5510B981) else Color(0x22E5C07B),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { togglePrayer(name) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) EmeraldAccent else Color(0x2294A3B8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDone) Color.White else Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "$time • $arabic",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Text(
                                text = if (isDone) "+25 XP ✓" else "+25 XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) EmeraldAccent else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Daily Fasting, Charity & Quran Quests
            Text(
                text = "DAILY SPIRITUAL QUESTS",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = GoldSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fasting Quest
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isFastingDone) Color(0x2210B981) else Color(0x151E293B))
                            .border(1.dp, if (isFastingDone) Color(0x5510B981) else Color(0x22E5C07B), RoundedCornerShape(14.dp))
                            .clickable {
                                isFastingDone = !isFastingDone
                                userXp += if (isFastingDone) 50 else -50
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌙 ", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Daily Fasting (Sawm)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isFastingDone) "Fasting completed today" else "Tap to mark completed",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Text(
                            text = if (isFastingDone) "+50 XP ✓" else "+50 XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFastingDone) EmeraldAccent else Color(0xFF64748B)
                        )
                    }

                    // Charity Quest
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCharityDone) Color(0x2210B981) else Color(0x151E293B))
                            .border(1.dp, if (isCharityDone) Color(0x5510B981) else Color(0x22E5C07B), RoundedCornerShape(14.dp))
                            .clickable {
                                isCharityDone = !isCharityDone
                                userXp += if (isCharityDone) 30 else -30
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💖 ", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Sadaqah / Act of Kindness",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isCharityDone) "Charity logged today" else "Tap to log charity",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Text(
                            text = if (isCharityDone) "+30 XP ✓" else "+30 XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCharityDone) EmeraldAccent else Color(0xFF64748B)
                        )
                    }

                    // Quran Reading
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x151E293B))
                            .border(1.dp, Color(0x22E5C07B), RoundedCornerShape(14.dp))
                            .clickable {
                                quranPagesRead = (quranPagesRead + 1).coerceAtMost(10)
                                userXp += 15
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📖 ", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Quran Recitation Goal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$quranPagesRead / 5 Pages read (Tap to +1)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Text(
                            text = "+15 XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            // Digital Badges Showcase
            Text(
                text = "UNLOCKABLE DIGITAL BADGES",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = GoldSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(badges) { badge ->
                    GlassCard(
                        modifier = Modifier.width(170.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (badge.isUnlocked) Brush.linearGradient(listOf(GoldPrimary, GoldAccent))
                                        else Brush.linearGradient(listOf(Color(0x3364748B), Color(0x22334155)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badge.icon,
                                    fontSize = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = badge.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) Color.White else Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = badge.description,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (badge.isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "UNLOCKED ✓",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    progress = { badge.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = GoldPrimary,
                                    trackColor = Color(0x22E5C07B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
