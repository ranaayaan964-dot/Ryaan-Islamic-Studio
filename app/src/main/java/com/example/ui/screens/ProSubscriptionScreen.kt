package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import java.util.Locale

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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isPostLoginStep) {
                TextButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Text(" Back")
                }
            }
            Text(
                "AYAAN ISLAMIC STUDIO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "  A MORE FOCUSED EXPERIENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Your practice,\nwithout limits.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Choose the plan that fits your journey. Keep the essentials free, or unlock the complete Ayaan experience with Pro.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (status.isProActive && status.proExpiresAt > 0) {
                            "PRO ACTIVE · ${status.remainingDays} days remaining"
                        } else {
                            "FREE PLAN · Upgrade whenever you’re ready"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (isPostLoginStep) {
            OutlinedButton(onClick = onContinueWithFree, modifier = Modifier.fillMaxWidth()) {
                Text("Continue with Free plan")
            }
        }

        Text("Choose your experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanFeatureCard(
                modifier = Modifier.weight(1f),
                title = "Free",
                subtitle = "The essentials",
                features = listOf(
                    "Prayer times & alarms",
                    "Quran reading",
                    "Limited daily AI & Hifz",
                    "Ad-supported access"
                ),
                highlighted = false
            )
            PlanFeatureCard(
                modifier = Modifier.weight(1f),
                title = "Ayaan Pro",
                subtitle = "Everything, unlocked",
                features = listOf(
                    "Unlimited AI Scholar & Hifz",
                    "Ad-free experience",
                    "Offline Quran downloads",
                    "Zakat PDF certificates"
                ),
                highlighted = true
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select your Pro plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("· Secure key activation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProPlanTier.values().forEach { tier ->
            val isSelected = tier == selectedPlan
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPlan = tier },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(tier.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(tier.badgeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tier.priceFormatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isSelected) {
                            Text("SELECTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Get your official Pro key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Contact our team to purchase the ${selectedPlan.displayName} (${selectedPlan.priceFormatted}). Your key will be linked to your account when activated.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Purchases: ${BuildConfig.PRO_SUPPORT_EMAIL}", style = MaterialTheme.typography.bodyMedium)
                Text("Email support / backup: ${BuildConfig.GMAIL_SENDER_EMAIL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(BuildConfig.PRO_SUPPORT_EMAIL))
                        Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy Email") }
                    Button(onClick = {
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
                    }) { Text("Email to buy") }
                }
                Text("WhatsApp contact coming soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Already have a Pro key?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it.uppercase(Locale.ROOT) },
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
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Activating…")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text("  Activate Pro")
            }
        }

        notice?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        OutlinedButton(onClick = onContinueWithFree, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Free plan")
        }
        Spacer(Modifier.height(8.dp))
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

@Composable
private fun PlanFeatureCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    features: List<String>,
    highlighted: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp).padding(top = 1.dp),
                        tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(feature, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
