package com.ayaan.islamicstudio.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.ayaan.islamicstudio.ui.components.ads.UnityRewardedManager

private fun Context.unwrapActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun ProUpgradeDialog(
    message: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    featureKey: String? = null,
    onWatchRewardedAd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayaan Pro") },
        text = { Text(message) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("👑 Upgrade to Pro")
                }
                if (onWatchRewardedAd != null) {
                    OutlinedButton(
                        onClick = {
                            val activity = context.unwrapActivity()
                            if (activity == null) {
                                Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show()
                            } else {
                                UnityRewardedManager.showRewardedAd(
                                    activity = activity,
                                    onRewardEarned = {
                                        featureKey?.let {
                                            ProSubscriptionRepository.getInstance(context)
                                                .grantRewardedAdBonus(it, 2)
                                        }
                                        Toast.makeText(
                                            context,
                                            "Alhamdulillah! +2 Bonus Uses Unlocked!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onWatchRewardedAd.invoke()
                                        onDismiss()
                                    },
                                    onAdDismissedOrFailed = { message ->
                                        if (!message.isNullOrBlank()) {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD4AF37)
                        )
                    ) {
                        Text("🎬 Watch Short Video Ad (+2 Bonus Uses)")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
