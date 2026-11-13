package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ProLicenseKeyEntity
import com.example.data.database.ProPlanTier
import com.example.data.database.UserEntity
import com.example.data.database.UserReportEntity
import com.example.data.repository.AdminRepository
import com.example.data.repository.ProSubscriptionRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun AdminDashboardScreen(
    onOpenUserApp: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AdminRepository(context) }
    val proRepository = remember(context) { ProSubscriptionRepository.getInstance(context) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val users by repository.observeAllUsers().collectAsStateWithLifecycle(emptyList())
    val messages by repository.observeAllMessages().collectAsStateWithLifecycle(emptyList())
    val reports by repository.observeAllReports().collectAsStateWithLifecycle(emptyList())
    var isAuthorized by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var targetAll by remember { mutableStateOf(true) }
    var messageType by remember { mutableStateOf("ANNOUNCEMENT") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var removeCandidate by remember { mutableStateOf<UserEntity?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var selectedProTier by remember { mutableStateOf(ProPlanTier.MONTHLY) }
    var buyerNote by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf<ProLicenseKeyEntity?>(null) }
    var keyFilter by remember { mutableStateOf("All Keys") }
    val proKeys by proRepository.observeKeys().collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(repository) {
        try {
            isAuthorized = repository.isAdminSession()
            if (!isAuthorized) {
                error = "This dashboard requires signing in with an authorized administrator email."
            } else {
                repository.syncUsersFromFirestore()
                repository.syncMessagesForAdmin()
                repository.syncReportsForAdmin()
                proRepository.syncKeysForAdmin()
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unable to load administrator data."
            actionMessage = error
        } finally {
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("AYAAN ADMIN COMMAND CENTER", style = MaterialTheme.typography.titleLarge)
                Text("Admin 2FA Verified")
                Text("Authorized: ${AdminRepository.AUTHORIZED_ADMIN_EMAILS.joinToString()}")
            }
            TextButton(onClick = onOpenUserApp) { Text("Open User App") }
            TextButton(onClick = {
                scope.launch {
                    try {
                        repository.syncUsersFromFirestore()
                        repository.syncMessagesForAdmin()
                        repository.syncReportsForAdmin()
                        proRepository.syncKeysForAdmin()
                        actionMessage = "Admin data refreshed."
                    } catch (e: Exception) {
                        actionMessage = e.localizedMessage ?: "Admin data refresh failed."
                    }
                }
            }) { Text("Refresh") }
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        if (isLoading) {
            Text("Verifying administrator access...")
        } else if (!isAuthorized) {
            Text(error ?: "Administrator access is unavailable.", color = MaterialTheme.colorScheme.error)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Users: ${users.size}")
                Text("Open reports: ${reports.count { it.status == "OPEN" }}")
                Text("Messages: ${messages.size}")
            }
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Users", "Send Message", "Reports", "History", "AI Pro Keys").forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
                }
            }
            actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search name, username, or email") },
                        singleLine = true
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users.filter {
                            val term = search.trim()
                            term.isBlank() || it.fullName.contains(term, true) ||
                                it.username.contains(term, true) || it.email.contains(term, true)
                        }, key = { it.id }) { user ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${user.username} • ${user.email}")
                                    Text("${user.role} • ${user.authProvider} • ${user.activitySummary}")
                                    Text("Last active: ${formatTimestamp(user.loginTimestamp)}")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val recipientKey = user.firebaseUid.ifBlank { user.username }
                                        Checkbox(
                                            checked = recipientKey in selectedUsers,
                                            onCheckedChange = { checked ->
                                                selectedUsers = if (checked) selectedUsers + recipientKey
                                                else selectedUsers - recipientKey
                                            }
                                        )
                                        TextButton(onClick = {
                                            selectedUsers = selectedUsers + recipientKey
                                            targetAll = false
                                            messageType = "WARNING"
                                            selectedTab = 1
                                        }) { Text("Warn / Message") }
                                        TextButton(onClick = { removeCandidate = user }) { Text("Remove User") }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(targetAll, { targetAll = true }, label = { Text("All Users") })
                        FilterChip(!targetAll, { targetAll = false }, label = { Text("Selected (${selectedUsers.size})") })
                    }
                    if (!targetAll) {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(users, key = { "recipient_${it.id}" }) { user ->
                                val recipientKey = user.firebaseUid.ifBlank { user.username }
                                Row(Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = recipientKey in selectedUsers,
                                        onCheckedChange = { checked ->
                                            selectedUsers = if (checked) selectedUsers + recipientKey
                                            else selectedUsers - recipientKey
                                        }
                                    )
                                    Text("${user.fullName} (@${user.username})")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WARNING", "ANNOUNCEMENT", "DIRECT_MESSAGE").forEach { type ->
                            FilterChip(messageType == type, { messageType = type }, label = { Text(type) })
                        }
                    }
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") })
                    OutlinedTextField(
                        body,
                        { body = it },
                        Modifier.fillMaxWidth().weight(1f),
                        label = { Text("Message") }
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    repository.sendAdminMessage(title, body, messageType, targetAll, selectedUsers.toList())
                                    title = ""
                                    body = ""
                                    actionMessage = "Message sent."
                                } catch (e: Exception) {
                                    actionMessage = e.localizedMessage ?: "Message could not be sent."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (targetAll) "Send to All Users" else "Send to ${selectedUsers.size} Selected") }
                }
                2 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reports, key = { it.id }) { report ->
                        ReportModerationCard(
                            report = report,
                            onReply = { status, reply ->
                                scope.launch {
                                    try {
                                        repository.replyAndResolveReport(report, status, reply)
                                        actionMessage = "Report updated and reply sent."
                                    } catch (e: Exception) {
                                        actionMessage = e.localizedMessage ?: "Report update failed."
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        repository.deleteUserReport(report.id)
                                        actionMessage = "Report deleted."
                                    } catch (e: Exception) {
                                        actionMessage = e.localizedMessage ?: "Report could not be deleted."
                                    }
                                }
                            },
                            onWarn = {
                                selectedUsers = setOf(report.reporterUid)
                                targetAll = false
                                messageType = "WARNING"
                                selectedTab = 1
                            }
                        )
                    }
                }
                3 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages, key = { it.id }) { message ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(message.title, style = MaterialTheme.typography.titleMedium)
                                Text(message.body)
                                Text(
                                    "${message.messageType} • ${if (message.targetAll) "All Users" else "Selected users"} • ${formatTimestamp(message.timestamp)}"
                                )
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            repository.deleteAdminMessage(message.id)
                                            actionMessage = "Message deleted."
                                        } catch (e: Exception) {
                                            actionMessage = e.localizedMessage ?: "Delete failed."
                                        }
                                    }
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
                4 -> ProKeysAdminPanel(
                    tiers = ProPlanTier.values().toList(),
                    selectedTier = selectedProTier,
                    onSelectTier = { selectedProTier = it },
                    buyerNote = buyerNote,
                    onBuyerNoteChange = { buyerNote = it },
                    generatedKey = generatedKey,
                    keys = proKeys,
                    keyFilter = keyFilter,
                    onSelectFilter = { keyFilter = it },
                    onGenerate = {
                        scope.launch {
                            proRepository.generateProKeyWithGroqAi(selectedProTier, buyerNote).fold(
                                onSuccess = { key ->
                                    generatedKey = key
                                    actionMessage = "Single-use Pro key generated and synced."
                                },
                                onFailure = { err ->
                                    actionMessage = err.localizedMessage ?: "Pro key generation failed."
                                }
                            )
                        }
                    },
                    onCopy = { key ->
                        clipboard.setText(AnnotatedString(key))
                        actionMessage = "Pro key copied."
                    },
                    onDelete = { key ->
                        scope.launch {
                            runCatching { proRepository.deleteProKey(key.licenseKey) }
                                .onSuccess { actionMessage = "Unused key deleted." }
                                .onFailure { actionMessage = it.localizedMessage ?: "Key could not be deleted." }
                        }
                    }
                )
            }
        }

        removeCandidate?.let { user ->
            AlertDialog(
                onDismissRequest = { removeCandidate = null },
                title = { Text("Remove ${user.fullName}?") },
                text = { Text("The account will be marked removed in Firestore and blocked from signing in.") },
                confirmButton = {
                    TextButton(onClick = {
                        removeCandidate = null
                        scope.launch {
                            try {
                                repository.removeUser(user)
                                actionMessage = "User marked removed."
                            } catch (e: Exception) {
                                actionMessage = e.localizedMessage ?: "User could not be removed."
                            }
                        }
                    }) { Text("Remove") }
                },
                dismissButton = { TextButton(onClick = { removeCandidate = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun ProKeysAdminPanel(
    tiers: List<ProPlanTier>,
    selectedTier: ProPlanTier,
    onSelectTier: (ProPlanTier) -> Unit,
    buyerNote: String,
    onBuyerNoteChange: (String) -> Unit,
    generatedKey: ProLicenseKeyEntity?,
    keys: List<ProLicenseKeyEntity>,
    keyFilter: String,
    onSelectFilter: (String) -> Unit,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (ProLicenseKeyEntity) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Create a single-use activation key", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tiers.forEach { tier ->
                FilterChip(
                    selected = selectedTier == tier,
                    onClick = { onSelectTier(tier) },
                    label = { Text("${tier.displayName.removeSuffix(" Pro Plan")} • ${tier.priceFormatted}") }
                )
            }
        }
        OutlinedTextField(
            value = buyerNote,
            onValueChange = onBuyerNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buyer name / optional note") },
            singleLine = true
        )
        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
            Text("Generate Single-Use Pro Key")
        }
        generatedKey?.let { key ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("New key: ${key.licenseKey}", style = MaterialTheme.typography.titleMedium)
                    Text(key.aiNote)
                    TextButton(onClick = { onCopy(key.licenseKey) }) { Text("Copy Key") }
                }
            }
        }
        Text("Generated keys", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All Keys", "Available", "Redeemed").forEach { filter ->
                FilterChip(
                    selected = keyFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter) }
                )
            }
        }
        val visibleKeys = keys.filter {
            when (keyFilter) {
                "Available" -> !it.isUsed
                "Redeemed" -> it.isUsed
                else -> true
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visibleKeys, key = { it.licenseKey }) { key ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(key.licenseKey, style = MaterialTheme.typography.titleMedium)
                        Text("${key.planTier} • ${key.durationDays} days • Rs. ${key.pricePkr}")
                        Text(if (key.isUsed) "USED & LOCKED" else "AVAILABLE (UNUSED)")
                        if (key.isUsed) {
                            Text("Used by: @${key.usedByUsername} (${key.usedByEmail})")
                            Text("Activated: ${formatTimestamp(key.activatedAt)} • Expires: ${formatTimestamp(key.expiresAt)}")
                        }
                        Row {
                            TextButton(onClick = { onCopy(key.licenseKey) }) { Text("Copy Key") }
                            if (!key.isUsed) TextButton(onClick = { onDelete(key) }) { Text("Delete / Revoke") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportModerationCard(
    report: UserReportEntity,
    onReply: (String, String) -> Unit,
    onDelete: () -> Unit,
    onWarn: () -> Unit
) {
    var reply by remember(report.id) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${report.subject} • ${report.status}", style = MaterialTheme.typography.titleMedium)
            Text("${report.reporterFullName} (@${report.reporterUsername}) • ${report.reporterEmail}")
            Text("${report.category} • ${formatTimestamp(report.timestamp)}")
            Text(report.description)
            if (report.adminReply.isNotBlank()) Text("Previous reply: ${report.adminReply}")
            OutlinedTextField(
                value = reply,
                onValueChange = { reply = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Admin reply") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onReply("RESOLVED", reply) }) { Text("Reply & Resolve") }
                TextButton(onClick = { onReply("IN_PROGRESS", reply) }) { Text("Reply") }
                TextButton(onClick = onWarn) { Text("Warn Reporter") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    if (timestamp <= 0L) "Not available" else DateFormat.getDateTimeInstance().format(Date(timestamp))
