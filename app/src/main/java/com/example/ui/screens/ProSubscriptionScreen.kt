package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.database.ProPlanTier
import com.example.data.repository.ProSubscriptionRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ProSubscriptionScreen(
    onNavigateBack: () -> Unit,
    onContinueWithFree: () -> Unit = onNavigateBack,
    isPostLoginStep: Boolean = false,
    onProActivated: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repository = remember(context) { ProSubscriptionRepository.getInstance(context) }
    val status by repository.proStatusFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableStateOf(ProPlanTier.MONTHLY) }
    var keyInput by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var activatedExpiry by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isPostLoginStep) {
                TextButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Text(" Back")
                }
            }
            Text("Ayaan Pro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (isPostLoginStep) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CONTINUE WITH FREE PLAN", fontWeight = FontWeight.Bold)
                    Text("Namaz timings, alarms, Quran reading, ads-supported access, and limited daily AI/Hifz uses.")
                    OutlinedButton(onClick = onContinueWithFree, modifier = Modifier.fillMaxWidth()) {
                        Text("Enter Free Version")
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AYAAN PRO PLAN • UNLIMITED & AD-FREE", fontWeight = FontWeight.Bold)
                    Text("No ads • Unlimited AI Scholar and Hifz • Offline Quran downloads • Zakat PDF certificates")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(" ${status.activePlanName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (status.isProActive && status.proExpiresAt > 0) {
                    Text("Premium access remaining: ${status.remainingDays} days")
                } else {
                    Text("Choose a plan or continue with the Free plan.")
                }
            }
        }

        Text("Choose Your Pro Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ProPlanTier.values().forEach { tier ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedPlan = tier },
                colors = CardDefaults.cardColors(
                    containerColor = if (tier == selectedPlan) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tier.displayName, fontWeight = FontWeight.Bold)
                        Text(tier.badgeLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(tier.priceFormatted, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Official Pro Key Purchase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Buy Pro Keys: ${BuildConfig.PRO_SUPPORT_EMAIL}")
                Text("Email support / backup: ${BuildConfig.GMAIL_SENDER_EMAIL}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(BuildConfig.PRO_SUPPORT_EMAIL))
                        Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy Email") }
                    TextButton(onClick = {
                        val mailIntent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:${BuildConfig.PRO_SUPPORT_EMAIL}")
                        ).putExtra(Intent.EXTRA_SUBJECT, "Buy Ayaan Islamic Studio Pro Key")
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                "Assalamu Alaikum,\n\nI would like to purchase the ${selectedPlan.displayName} (${selectedPlan.priceFormatted})."
                            )
                        runCatching { context.startActivity(mailIntent) }
                            .onFailure {
                                Toast.makeText(context, "No email app is available on this device.", Toast.LENGTH_LONG).show()
                            }
                    }) { Text("Email Now to Buy") }
                }
                Text("Contact / WhatsApp: Coming Soon")
            }
        }

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it.uppercase() },
            modifier = Modifier.fillMaxWidth().testTag("pro_key_input"),
            label = { Text("Enter your Pro activation key") },
            placeholder = { Text("AYAAN-MNTH-XXXX-XXXX-XXXX") },
            supportingText = { Text("Each Pro Key is single-use only and locks to your account.") },
            singleLine = true,
            enabled = !isActivating
        )
        Button(
            onClick = {
                isActivating = true
                notice = null
                scope.launch {
                    repository.redeemProKey(keyInput).fold(
                        onSuccess = { activated ->
                            keyInput = ""
                            notice = "${activated.aiNote}\nAccess is active for ${activated.durationDays} days."
                            isError = false
                            activatedExpiry = activated.expiresAt
                            repository.refreshCurrentProStatus()
                        },
                        onFailure = {
                            notice = it.localizedMessage ?: "The Pro key could not be activated."
                            isError = true
                        }
                    )
                    isActivating = false
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("activate_pro_key_button"),
            enabled = keyInput.isNotBlank() && !isActivating
        ) {
            if (isActivating) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Text(" Activating…")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text(" Activate Pro")
            }
        }

        notice?.let {
            Text(
                text = it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onContinueWithFree, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Free plan")
        }
    }
    if (!isError && activatedExpiry > 0L) {
        AlertDialog(
            onDismissRequest = {
                activatedExpiry = 0L
                onProActivated()
            },
            title = { Text("Pro Activated!") },
            text = {
                Text(
                    "${notice.orEmpty()}\n\nValid until ${
                        DateFormat.getDateInstance(DateFormat.LONG).format(Date(activatedExpiry))
                    }."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    activatedExpiry = 0L
                    onProActivated()
                }) { Text("Continue") }
            }
        )
    }
}
