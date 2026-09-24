package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DuaItem
import com.example.data.repository.FirestoreRepository
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald

/**
 * MODULE 14: GLOBAL DUA WALL & REAL-TIME HAPTIC UMMAH
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDuaScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val firestoreRepo = remember { FirestoreRepository() }

    // Real-time stream from Firestore
    val realTimeDuas by firestoreRepo.getPrayerRequestsFlow().collectAsState(initial = emptyList())

    // Initial default Duas if Firestore has not populated yet
    val initialDuas = remember {
        listOf(
            DuaItem(
                id = "init_1",
                userName = "Sister Fatima",
                userLocation = "Lahore, Pakistan",
                content = "Please make Dua for my elderly mother undergoing open heart surgery tomorrow. May Allah grant her complete Shifa.",
                category = "Health",
                ameenCount = 1420
            ),
            DuaItem(
                id = "init_2",
                userName = "Brother Tariq",
                userLocation = "London, UK",
                content = "Struggling to find Halal livelihood and providing for family. Dua requested for Barakah in Rizq.",
                category = "Guidance",
                ameenCount = 985
            ),
            DuaItem(
                id = "init_3",
                userName = "Ummah Youth",
                userLocation = "Toronto, Canada",
                content = "May Allah protect our youth, keep us firm on the 5 daily prayers, and shield our hearts from fitnah.",
                category = "Forgiveness",
                ameenCount = 3120
            )
        )
    }

    // Local instant Ameen tracker (zero latency)
    val localAmeenOverrides = remember { mutableStateMapOf<String, Long>() }
    val localDuasSubmitted = remember { mutableStateMapOf<String, DuaItem>() }

    // Merge real-time items with initial defaults and local submissions
    val displayList = remember(realTimeDuas, localDuasSubmitted) {
        val merged = (localDuasSubmitted.values + realTimeDuas).toMutableList()
        if (merged.isEmpty()) {
            merged.addAll(initialDuas)
        }
        merged.distinctBy { it.id }
    }

    var showSubmitSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var newDuaText by remember { mutableStateOf("") }
    var newDuaName by remember { mutableStateOf("Anonymous Believer") }
    var selectedCategory by remember { mutableStateOf("Health") }
    val categories = listOf("Health", "Family", "Forgiveness", "Guidance", "Ummah")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PearlBackground, PureWhite, DeepRoyalEmerald.copy(alpha = 0.06f))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("global_dua_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "Global Ummah Dua Wall",
                        color = CharcoalPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-Time Shared Supplications with Haptic Ameen",
                        color = DeepRoyalEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Real-time LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList, key = { it.id }) { dua ->
                    val currentCount = localAmeenOverrides[dua.id] ?: dua.ameenCount
                    DuaCard(
                        dua = dua,
                        ameenCount = currentCount,
                        onAmeenClicked = {
                            // MODULE 14: Ameen Counter Logic
                            // 1. LongPress haptic feedback
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 2. Instantly increment local count for zero latency
                            localAmeenOverrides[dua.id] = currentCount + 1

                            // 3. Update Firestore in background using FieldValue.increment(1)
                            firestoreRepo.incrementAmeen(dua.id)
                        }
                    )
                }
            }
        }

        // Floating Action Button to Submit Dua
        FloatingActionButton(
            onClick = { showSubmitSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("submit_dua_fab"),
            containerColor = DeepRoyalEmerald,
            contentColor = PureWhite,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Dua", tint = PlatinumGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Make Dua", fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Sheet for Dua Submission
        if (showSubmitSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubmitSheet = false },
                sheetState = sheetState,
                containerColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "SUBMIT DUA TO THE GLOBAL UMMAH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newDuaName,
                        onValueChange = { newDuaName = it },
                        label = { Text("Your Name or Moniker") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newDuaText,
                        onValueChange = { newDuaText = it },
                        label = { Text("Write your heartfelt Dua...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SELECT CATEGORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepRoyalEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedCategory == cat) DeepRoyalEmerald else PearlBackground,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (selectedCategory == cat) PureWhite else CharcoalPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (newDuaText.isNotBlank()) {
                                val newId = "user_${System.currentTimeMillis()}"
                                val newDua = DuaItem(
                                    id = newId,
                                    userName = newDuaName.ifBlank { "Anonymous Believer" },
                                    userLocation = "Worldwide",
                                    content = newDuaText.trim(),
                                    category = selectedCategory,
                                    ameenCount = 1
                                )
                                localDuasSubmitted[newId] = newDua
                                firestoreRepo.submitDua(newDua)
                                Toast.makeText(context, "Dua shared with the global Ummah", Toast.LENGTH_SHORT).show()
                                newDuaText = ""
                                showSubmitSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = PlatinumGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Dua to Ummah", fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: DuaItem,
    ameenCount: Long,
    onAmeenClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // User header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepRoyalEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dua.userName.firstOrNull()?.uppercase() ?: "U",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = dua.userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PlatinumGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = dua.userLocation, fontSize = 11.sp, color = DeepRoyalEmerald)
                        }
                    }
                }

                // Category Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeepRoyalEmerald.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = dua.category,
                        color = DeepRoyalEmerald,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dua Content
            Text(
                text = dua.content,
                fontSize = 14.sp,
                color = CharcoalPrimary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ameen Button with Real-Time Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAmeenClicked,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepRoyalEmerald.copy(alpha = 0.08f),
                        contentColor = DeepRoyalEmerald
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "Ameen",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ameen ($ameenCount)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
