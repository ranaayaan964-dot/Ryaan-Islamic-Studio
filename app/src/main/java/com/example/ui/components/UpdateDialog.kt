package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.theme.BorderGoldAccent
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PlatinumGoldSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted

/**
 * Highly Polished Un-cancellable In-App GitHub Update Dialog.
 *
 * Implements strict non-dismissible DialogProperties to enforce mandatory updates:
 * properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
 *
 * Exactly routes to the GitHub release page via:
 * val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubHtmlUrl))
 * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 * context.startActivity(intent)
 */
@Composable
fun UpdateDialog(
    newVersionName: String,
    downloadUrl: String,
    modifier: Modifier = Modifier,
    isMandatory: Boolean = true,
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Infinite breathing glow transition for the jewel badge
    val infiniteTransition = rememberInfiniteTransition(label = "update_glow_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    AlertDialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismissRequest?.invoke()
            }
            // Un-cancellable when mandatory: user gesture is ignored
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        ),
        modifier = modifier.testTag("update_required_dialog"),
        confirmButton = {
            // Prominent "Download Update" button with direct Intent routing
            Button(
                onClick = {
                    if (downloadUrl.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PlatinumGold.copy(alpha = 0.5f))
                    .testTag("download_update_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PlatinumGold, GoldenAmber, PlatinumGoldSecondary)
                            )
                        )
                        .border(
                            1.dp,
                            PureWhite.copy(alpha = 0.50f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download Update",
                            tint = CharcoalPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Download Update",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = CharcoalPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tag: Official Security / Feature Release Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DeepRoyalEmerald.copy(alpha = 0.10f))
                        .border(1.dp, DeepRoyalEmerald.copy(alpha = 0.30f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OFFICIAL GITHUB RELEASE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = DeepRoyalEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing Platinum Gold Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PlatinumGold.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PearlBackground, PureWhite)
                                )
                            )
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            PlatinumGold.copy(alpha = pulseBorderAlpha),
                                            DeepRoyalEmerald.copy(alpha = 0.4f),
                                            PlatinumGoldSecondary
                                        )
                                    )
                                ),
                                CircleShape
                            )
                            .shadow(6.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "New Version Available",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Exact Title requested: "New Version Available"
                Text(
                    text = "New Version Available",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = CharcoalPrimary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Exact Message requested:
                // "Version [tag_name] of Ryaan Islamic Studio is now available! Please update to get the latest features and bug fixes."
                Text(
                    text = "Version $newVersionName of Ryaan Islamic Studio is now available! Please update to get the latest features and bug fixes.",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = CharcoalSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Version Comparison Pill: Installed vs GitHub Latest
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PearlBackground,
                    border = BorderStroke(1.dp, BorderGoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Installed Version",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalPrimary
                            )
                        }

                        Text(
                            text = "➔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "GitHub Latest",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                            Text(
                                text = newVersionName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepRoyalEmerald
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = PureWhite,
        tonalElevation = 10.dp
    )
}
