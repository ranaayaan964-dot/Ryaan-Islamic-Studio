package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class GlobalDua(
    val id: String = UUID.randomUUID().toString(),
    val author: String,
    val cityAndCountry: String,
    val duaText: String,
    val category: String,
    var ameenCount: Int,
    val timestamp: String = "Just now",
    var hasUserSaidAmeen: Boolean = false
)

data class FloatingAmeenParticle(
    val id: String = UUID.randomUUID().toString(),
    val startX: Float,
    val label: String = "🤲 Ameen!"
)

/**
 * MODULE 5: GLOBAL DUA WALL & REAL-TIME HAPTIC UMMAH
 * Real-time community prayer feed where believers post prayer requests.
 * Features a heavy haptic "Ameen" button (HapticFeedbackType.LongPress) with real-time
 * ticking Ameen counters and Instagram Live-style floating heart/prayer particles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDuaWallScreen(
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("All") }
    var isNewDuaSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Floating particles state for Instagram Live effect
    val floatingParticles = remember { mutableStateListOf<FloatingAmeenParticle>() }

    // Live list of community Duas
    val duas = remember {
        mutableStateListOf(
            GlobalDua(
                author = "Brother Zaid",
                cityAndCountry = "Medina, Saudi Arabia",
                duaText = "Please make Dua for my elderly mother undergoing open-heart surgery tomorrow morning. Ya Allah grant her Shifa Kamila and tranquil health.",
                category = "Health & Healing",
                ameenCount = 1428
            ),
            GlobalDua(
                author = "Sister Maryam",
                cityAndCountry = "London, UK",
                duaText = "Ya Rabb, grant peace, safety, and victory to all oppressed Muslims in Palestine, Sudan, and across the globe. Protect their innocent children.",
                category = "Global Ummah",
                ameenCount = 5291
            ),
            GlobalDua(
                author = "Brother Bilal",
                cityAndCountry = "Lahore, Pakistan",
                duaText = "Seeking prayers for my final medical board examinations. May Allah grant barakah, sharp memory, and success to serve humanity.",
                category = "Studies & Career",
                ameenCount = 874
            ),
            GlobalDua(
                author = "Sister Aisha",
                cityAndCountry = "Toronto, Canada",
                duaText = "Ya Allah, grant righteous pious offspring to all couples yearning for children, and fill their households with Mawaddah and Rahmah.",
                category = "Family & Marriage",
                ameenCount = 2190
            ),
            GlobalDua(
                author = "Brother Omar",
                cityAndCountry = "Istanbul, Turkey",
                duaText = "O Allah, forgive our sins past and future, protect our hearts from arrogance, and grant us a peaceful ending in Sujood.",
                category = "Forgiveness",
                ameenCount = 3840
            )
        )
    }

    // Trigger floating particles animation
    fun spawnAmeenParticle() {
        val randomX = (100..260).random().toFloat()
        val particle = FloatingAmeenParticle(startX = randomX)
        floatingParticles.add(particle)

        scope.launch {
            delay(1800)
            floatingParticles.remove(particle)
        }
    }

    val categories = listOf("All", "Health & Healing", "Global Ummah", "Family & Marriage", "Studies & Career", "Forgiveness")

    val filteredDuas = if (selectedCategory == "All") duas else duas.filter { it.category == selectedCategory }

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("global_dua_wall_screen")
            .statusBarsPadding()
            .navigationBarsPadding(),
        backgroundColor = PearlBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepRoyalEmerald
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Global Dua Wall",
                                    fontSize = 17.sp,
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
                                        text = "LIVE UMMAH",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RadiantEmerald
                                    )
                                }
                            }
                            Text(
                                text = "United in prayer across all timezones",
                                fontSize = 11.sp,
                                color = PlatinumGold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PureWhite)
                            .border(1.dp, Color(0x25D4AF37), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = RadiantEmerald, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "14,892 Ameens Today", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DeepRoyalEmerald)
                        }
                    }
                }

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) DeepRoyalEmerald else PureWhite)
                                .border(1.dp, if (isSelected) DeepRoyalEmerald else Color(0x20D4AF37), RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else CharcoalSecondary
                            )
                        }
                    }
                }

                // Dua Cards Feed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
                ) {
                    items(filteredDuas, key = { it.id }) { dua ->
                        DuaFeedCard(
                            dua = dua,
                            onSayAmeen = {
                                // HEAVY HAPTIC FEEDBACK MANDATE
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                if (!dua.hasUserSaidAmeen) {
                                    dua.ameenCount += 1
                                    dua.hasUserSaidAmeen = true
                                } else {
                                    dua.ameenCount -= 1
                                    dua.hasUserSaidAmeen = false
                                }
                                spawnAmeenParticle()
                            }
                        )
                    }
                }
            }

            // Floating Instagram Live-style Ameen particles
            floatingParticles.forEach { particle ->
                FloatingAmeenBadge(particle = particle)
            }

            // FAB: Post a Prayer Request
            FloatingActionButton(
                onClick = { isNewDuaSheetOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("post_dua_fab"),
                containerColor = DeepRoyalEmerald,
                contentColor = PureWhite,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Post Dua", tint = PlatinumGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ask for Dua", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // New Dua Modal Sheet
        if (isNewDuaSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isNewDuaSheetOpen = false },
                sheetState = sheetState,
                containerColor = PureWhite
            ) {
                NewDuaInputSheet(
                    onPostDua = { newDua ->
                        duas.add(0, newDua)
                        isNewDuaSheetOpen = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        spawnAmeenParticle()
                    },
                    onCancel = { isNewDuaSheetOpen = false }
                )
            }
        }
    }
}

@Composable
fun DuaFeedCard(
    dua: GlobalDua,
    onSayAmeen: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp,
        cornerRadius = 20.dp,
        backgroundColor = PureWhite,
        borderColor = if (dua.hasUserSaidAmeen) RadiantEmerald.copy(alpha = 0.5f) else Color(0x22D4AF37)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Author & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldSurfaceLight)
                            .border(1.dp, PlatinumGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🤲", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = dua.author,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = dua.cityAndCountry, fontSize = 10.5.sp, color = SlateMuted)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = dua.category, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = CharcoalSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dua Text
            Text(
                text = dua.duaText,
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
                color = CharcoalPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: Live Ameen Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dua.timestamp, fontSize = 10.5.sp, color = SlateMuted)

                // Heavy Haptic Ameen Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (dua.hasUserSaidAmeen) EmeraldSurfaceLight else PureWhite)
                        .border(1.2.dp, if (dua.hasUserSaidAmeen) RadiantEmerald else PlatinumGold, RoundedCornerShape(16.dp))
                        .clickable { onSayAmeen() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (dua.hasUserSaidAmeen) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Ameen",
                            tint = if (dua.hasUserSaidAmeen) CrimsonError else PlatinumGold,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ameen (${dua.ameenCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dua.hasUserSaidAmeen) DeepRoyalEmerald else CharcoalPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingAmeenBadge(particle: FloatingAmeenParticle) {
    var offsetY by remember { mutableStateOf(0f) }
    var alphaVal by remember { mutableStateOf(1f) }

    LaunchedEffect(particle.id) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 1600) {
            val progress = (System.currentTimeMillis() - startTime) / 1600f
            offsetY = -progress * 280f
            alphaVal = (1f - progress).coerceIn(0f, 1f)
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .offset(x = particle.startX.dp, y = (480f + offsetY).dp)
            .alpha(alphaVal)
            .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x40064E3B))
            .clip(RoundedCornerShape(14.dp))
            .background(PureWhite)
            .border(1.5.dp, PlatinumGold, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "💚", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = particle.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
        }
    }
}

@Composable
fun NewDuaInputSheet(
    onPostDua: (GlobalDua) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var duaText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Health & Healing") }

    val categories = listOf("Health & Healing", "Global Ummah", "Family & Marriage", "Studies & Career", "Forgiveness")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "REQUEST DUA FROM THE GLOBAL UMMAH",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlatinumGold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Thousands of believers across the globe will say Ameen for your request.",
            fontSize = 11.5.sp,
            color = SlateMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name or 'Anonymous Brother/Sister'") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City, Country (e.g. Medina, Saudi Arabia)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = duaText,
            onValueChange = { duaText = it },
            label = { Text("Your Prayer Request (Dua)") },
            placeholder = { Text("Share what you need Allah's help with...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Select Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSel = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) DeepRoyalEmerald else Color(0xFFF1F5F9))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) PureWhite else CharcoalPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (duaText.isNotBlank()) {
                    onPostDua(
                        GlobalDua(
                            author = if (name.isBlank()) "Anonymous Believer" else name.trim(),
                            cityAndCountry = if (city.isBlank()) "Global Ummah" else city.trim(),
                            duaText = duaText.trim(),
                            category = selectedCategory,
                            ameenCount = 1,
                            hasUserSaidAmeen = true
                        )
                    )
                }
            },
            enabled = duaText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald, contentColor = PureWhite),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "Broadcast Prayer Request Globally", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
