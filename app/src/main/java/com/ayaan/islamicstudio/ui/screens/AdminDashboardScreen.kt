package com.ayaan.islamicstudio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayaan.islamicstudio.data.database.AdminMessageEntity
import com.ayaan.islamicstudio.data.database.ProLicenseKeyEntity
import com.ayaan.islamicstudio.data.database.ProPlanTier
import com.ayaan.islamicstudio.data.database.UserEntity
import com.ayaan.islamicstudio.data.database.UserReportEntity
import com.ayaan.islamicstudio.data.repository.AdminRepository
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.ayaan.islamicstudio.ui.theme.DeepRoyalEmerald
import com.ayaan.islamicstudio.ui.theme.LocalGlassTheme
import com.ayaan.islamicstudio.ui.theme.PlatinumGold
import com.ayaan.islamicstudio.ui.components.AppVersionManagementDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val AdminTabs = listOf(
    "Overview",
    "All Users",
    "Pro Keys Vault",
    "Broadcast & Warnings",
    "User Reports",
    "App Version"
)

@Composable
fun AdminDashboardScreen(
    onOpenUserApp: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val glassColors = LocalGlassTheme.current
    val repository = remember(context) { AdminRepository(context) }
    val proRepository = remember(context) { ProSubscriptionRepository.getInstance(context) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val users by repository.observeAllUsers().collectAsStateWithLifecycle(emptyList())
    val messages by repository.observeAllMessages().collectAsStateWithLifecycle(emptyList())
    val reports by repository.observeAllReports().collectAsStateWithLifecycle(emptyList())
    val proKeys by proRepository.observeKeys().collectAsStateWithLifecycle(emptyList())
    val syncSummary by repository.userSyncSummary.collectAsStateWithLifecycle()
    val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()

    var isAuthorized by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSyncingUsers by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var targetAll by remember { mutableStateOf(true) }
    var messageType by remember { mutableStateOf("ANNOUNCEMENT") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var userFilter by remember { mutableStateOf("All") }
    var reportFilter by remember { mutableStateOf("All") }
    var showHistory by remember { mutableStateOf(false) }
    var removeCandidate by remember { mutableStateOf<UserEntity?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var selectedProTier by remember { mutableStateOf(ProPlanTier.MONTHLY) }
    var buyerNote by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf<ProLicenseKeyEntity?>(null) }
    var keyFilter by remember { mutableStateOf("All Keys") }
    var showVersionDialog by remember { mutableStateOf(false) }
    BackHandler {
        if (selectedTab > 0) {
            selectedTab = 0
        } else {
            onOpenUserApp()
        }
    }

    suspend fun refreshData() {
        repository.syncUsersFromFirestore()
        repository.syncMessagesForAdmin()
        repository.syncReportsForAdmin()
        proRepository.syncKeysForAdmin()
    }

    LaunchedEffect(repository) {
        try {
            isAuthorized = repository.isAdminSession()
            if (isAuthorized) {
                refreshData()
            } else {
                actionMessage = "This dashboard requires an authorized Firebase administrator account."
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            actionMessage = error.localizedMessage ?: "Unable to load administrator data."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(glassColors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = glassColors.background
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PlatinumGold)
                        Spacer(Modifier.height(12.dp))
                        Text("Verifying administrator access…", color = glassColors.textPrimary)
                    }
                }
            } else if (!isAuthorized) {
                AdminSurfaceCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Administrator access is unavailable. Sign in with an authorized admin account.",
                        color = glassColors.textPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                AdminHeader(
                    firebaseEmail = firebaseEmail,
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        if (!isRefreshing) scope.launch {
                            isRefreshing = true
                            try {
                                refreshData()
                                actionMessage = "All administrator data refreshed."
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                actionMessage = error.localizedMessage ?: "Admin data refresh failed."
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    onOpenUserApp = onOpenUserApp,
                    onLogout = onLogout
                )

                if (actionMessage != null) {
                    AdminSurfaceCard(Modifier.fillMaxWidth()) {
                        Text(
                            actionMessage.orEmpty(),
                            color = if (actionMessage.orEmpty().contains("failed", true)) Color(0xFFB91C1C)
                            else glassColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = glassColors.background,
                    contentColor = PlatinumGold
                ) {
                    AdminTabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    label,
                                    maxLines = 1,
                                    color = if (selectedTab == index) adminAccentColor() else glassColors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> OverviewSection(
                        users = users,
                        proKeys = proKeys,
                        reports = reports,
                        onJump = { selectedTab = it }
                    )
                        1 -> UsersSection(
                        users = users,
                        search = search,
                        onSearchChange = { search = it },
                        filter = userFilter,
                        onFilterChange = { userFilter = it },
                        selectedUsers = selectedUsers,
                        onSelectedUsersChange = { selectedUsers = it },
                        syncSummary = "${syncSummary.active} active • ${syncSummary.removed} removed",
                        isSyncing = isSyncingUsers,
                        onSync = {
                            if (!isSyncingUsers) scope.launch {
                                isSyncingUsers = true
                                try {
                                    repository.syncUsersFromFirestore()
                                    actionMessage = "All registered users synced."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "User sync failed."
                                } finally {
                                    isSyncingUsers = false
                                }
                                }
                        },
                        onWarn = { user ->
                            selectedUsers = setOf(user.firebaseUid.ifBlank { user.username })
                            targetAll = false
                            messageType = "WARNING"
                            selectedTab = 3
                            showHistory = false
                        },
                        onRemove = { removeCandidate = it }
                    )
                        2 -> ProKeysSection(
                        tiers = ProPlanTier.values().toList(),
                        selectedTier = selectedProTier,
                        onSelectTier = { selectedProTier = it },
                        buyerNote = buyerNote,
                        onBuyerNoteChange = { buyerNote = it },
                        generatedKey = generatedKey,
                        keys = proKeys,
                        keyFilter = keyFilter,
                        onSelectFilter = { keyFilter = it },
                        isGenerating = isGenerating,
                        onGenerate = {
                            if (!isGenerating) scope.launch {
                                isGenerating = true
                                proRepository.generateProKeyWithGroqAi(selectedProTier, buyerNote).fold(
                                    onSuccess = {
                                        generatedKey = it
                                        actionMessage = "Single-use Pro key generated and synced."
                                    },
                                    onFailure = {
                                        actionMessage = it.localizedMessage ?: "Pro key generation failed."
                                    }
                                )
                                isGenerating = false
                                }
                        },
                        onCopy = { key ->
                            clipboard.setText(AnnotatedString(key))
                            actionMessage = "Pro key copied."
                        },
                        onCopyMessage = { key ->
                            clipboard.setText(AnnotatedString(fullKeyMessage(key)))
                            actionMessage = "Full WhatsApp / email message copied."
                        },
                        onDelete = { key ->
                            scope.launch {
                                try {
                                    proRepository.deleteProKey(key.licenseKey)
                                    actionMessage = "Unused key revoked."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "Key could not be revoked."
                                }
                            }
                        }
                    )
                        3 -> MessagesSection(
                        users = users,
                        messages = messages,
                        showHistory = showHistory,
                        onShowHistory = { showHistory = it },
                        targetAll = targetAll,
                        onTargetAllChange = { targetAll = it },
                        selectedUsers = selectedUsers,
                        onSelectedUsersChange = { selectedUsers = it },
                        messageType = messageType,
                        onMessageTypeChange = { messageType = it },
                        title = title,
                        onTitleChange = { title = it },
                        body = body,
                        onBodyChange = { body = it },
                        onSend = {
                            scope.launch {
                                try {
                                    repository.sendAdminMessage(
                                        title, body, messageType, targetAll, selectedUsers.toList()
                                    )
                                    title = ""
                                    body = ""
                                    actionMessage = "Message sent successfully."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "Message could not be sent."
                                }
                                }
                        },
                        onDelete = { message ->
                            scope.launch {
                                try {
                                    repository.deleteAdminMessage(message.id)
                                    actionMessage = "Message deleted."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "Message could not be deleted."
                                }
                                }
                            }
                    )
                        else -> ReportsSection(
                        reports = reports,
                        filter = reportFilter,
                        onFilterChange = { reportFilter = it },
                        onReply = { report, status, reply ->
                            scope.launch {
                                try {
                                    repository.replyAndResolveReport(report, status, reply)
                                    actionMessage = if (status == "RESOLVED") "Report resolved and reply sent."
                                    else "Reply sent; report marked in progress."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "Report update failed."
                                }
                                }
                        },
                        onWarn = { report ->
                            selectedUsers = setOf(report.reporterUid)
                            targetAll = false
                            messageType = "WARNING"
                            selectedTab = 3
                            showHistory = false
                        },
                        onDelete = { report ->
                            scope.launch {
                                try {
                                    repository.deleteUserReport(report.id)
                                    actionMessage = "Report deleted."
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    actionMessage = error.localizedMessage ?: "Report could not be deleted."
                                }
                                }
                            }
                    )
                }
            }
        }
    }

    removeCandidate?.let { user ->
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            containerColor = glassColors.surface,
            titleContentColor = glassColors.textPrimary,
            textContentColor = glassColors.textSecondary,
            title = { Text("Remove ${user.fullName}?") },
            text = { Text("This account will be marked removed in Firestore and blocked from signing in.") },
            confirmButton = {
                TextButton(onClick = {
                    removeCandidate = null
                    scope.launch {
                        try {
                            repository.removeUser(user)
                            actionMessage = "User marked removed."
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            actionMessage = error.localizedMessage ?: "User could not be removed."
                        }
                    }
                }) { Text("Remove", color = Color(0xFFB91C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { removeCandidate = null }) {
                    Text("Cancel", color = glassColors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AdminHeader(
    firebaseEmail: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenUserApp: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepRoyalEmerald),
        border = BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.8f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "ADMIN COMMAND CENTER",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "Verified Admin • $firebaseEmail",
                color = PlatinumGold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderAction(
                    label = "Refresh Data",
                    icon = {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                color = PlatinumGold,
                                strokeWidth = 2.dp
                            )
                        } else Icon(Icons.Outlined.Refresh, null, tint = PlatinumGold, modifier = Modifier.size(17.dp))
                    },
                    onClick = onRefresh,
                    modifier = Modifier.weight(1.25f)
                )
                HeaderAction(
                    label = "User App",
                    icon = { Icon(Icons.Outlined.Home, null, tint = PlatinumGold, modifier = Modifier.size(17.dp)) },
                    onClick = onOpenUserApp,
                    modifier = Modifier.weight(1f)
                )
                HeaderAction(
                    label = "Logout",
                    icon = { Icon(Icons.Outlined.Logout, null, tint = PlatinumGold, modifier = Modifier.size(17.dp)) },
                    onClick = onLogout,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun HeaderAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            icon()
            Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun OverviewSection(
    users: List<UserEntity>,
    proKeys: List<ProLicenseKeyEntity>,
    reports: List<UserReportEntity>,
    onJump: (Int) -> Unit
) {
    val now = System.currentTimeMillis()
    val metrics = listOf(
        "Total Users" to users.size,
        "Pro Members" to users.count { !it.role.equals("ADMIN", true) && it.proExpiresAt > now },
        "Free Users" to users.count { !it.role.equals("ADMIN", true) && it.proExpiresAt <= now },
        "Available Pro Keys" to proKeys.count { !it.isUsed },
        "Redeemed Keys" to proKeys.count { it.isUsed },
        "Open Reports" to reports.count { it.status == "OPEN" }
    )
    val glassColors = LocalGlassTheme.current
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(354.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                gridItems(metrics) { (label, value) ->
                    AdminSurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = glassColors.textSecondary, fontSize = 12.sp)
                            Text(
                                value.toString(),
                                color = if (label.contains("Pro")) {
                                    if (glassColors.isDark) PlatinumGold else DeepRoyalEmerald
                                } else glassColors.textPrimary,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "QUICK ACTIONS",
                color = adminAccentColor(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Manage Users" to 1,
                    "Pro Keys Vault" to 2,
                    "Broadcast / Messages" to 3,
                    "Moderate Reports" to 4
                ).forEach { (label, tab) ->
                    AdminSurfaceCard(
                        Modifier.fillMaxWidth().clickable { onJump(tab) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = glassColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "OPEN  ›",
                                color = if (glassColors.isDark) PlatinumGold else DeepRoyalEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersSection(
    users: List<UserEntity>,
    search: String,
    onSearchChange: (String) -> Unit,
    filter: String,
    onFilterChange: (String) -> Unit,
    selectedUsers: Set<String>,
    onSelectedUsersChange: (Set<String>) -> Unit,
    syncSummary: String,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onWarn: (UserEntity) -> Unit,
    onRemove: (UserEntity) -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val visibleUsers = users.filter { user ->
        val term = search.trim().removePrefix("@")
        val matchesSearch = term.isBlank() || user.fullName.contains(term, true) ||
            user.username.contains(term, true) || user.email.contains(term, true)
        val isPro = user.proExpiresAt > System.currentTimeMillis()
        val matchesFilter = when (filter) {
            "Pro" -> isPro
            "Free" -> !isPro
            "Admins" -> user.role.equals("ADMIN", true)
            else -> true
        }
        matchesSearch && matchesFilter
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search by name, @username, or email..."
        )
        AdminChipRow(listOf("All", "Pro", "Free", "Admins"), filter, onFilterChange)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(syncSummary, color = glassColors.textSecondary, fontSize = 12.sp)
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                colors = adminButtonColors(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Sync All Users Now", fontSize = 12.sp)
            }
        }
        if (visibleUsers.isEmpty()) {
            EmptyState(
                text = if (users.isEmpty()) "No users have loaded yet. Sync to fetch all registered users."
                else "No users match this search or filter."
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleUsers, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        isSelected = (user.firebaseUid.ifBlank { user.username }) in selectedUsers,
                        onSelect = { selected ->
                            val key = user.firebaseUid.ifBlank { user.username }
                            onSelectedUsersChange(if (selected) selectedUsers + key else selectedUsers - key)
                        },
                        onWarn = { onWarn(user) },
                        onRemove = { onRemove(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserEntity,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onWarn: () -> Unit,
    onRemove: () -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val name = user.fullName.ifBlank { user.username.ifBlank { user.email } }
    val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }
        .joinToString("").uppercase().ifBlank { "U" }
    val isPro = user.proExpiresAt > System.currentTimeMillis()
    AdminSurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(DeepRoyalEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = glassColors.textPrimary, fontWeight = FontWeight.Bold)
                    Text("@${user.username}", color = glassColors.textSecondary, fontSize = 12.sp)
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelect,
                    colors = CheckboxDefaults.colors(
                        checkedColor = DeepRoyalEmerald,
                        uncheckedColor = glassColors.textSecondary,
                        checkmarkColor = Color.White
                    )
                )
            }
            Text(user.email, color = glassColors.textSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(user.role.uppercase(), if (user.role.equals("ADMIN", true)) PlatinumGold else glassColors.chipBackground)
                StatusBadge(
                    if (isPro) "PRO • ${formatTimestamp(user.proExpiresAt)}" else "FREE",
                    if (isPro) DeepRoyalEmerald else glassColors.chipBackground
                )
                StatusBadge(
                    if (user.authProvider.contains("google", true)) "Google" else "Email",
                    glassColors.chipBackground
                )
            }
            Text(user.activitySummary.ifBlank { "Registered User" }, color = glassColors.textSecondary, fontSize = 12.sp)
            Text("Last active: ${formatTimestamp(user.loginTimestamp)}", color = glassColors.textMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onWarn, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                    Text("Warn / Direct Message", color = adminAccentColor(), fontSize = 12.sp)
                }
                TextButton(
                    onClick = onRemove,
                    enabled = !user.role.equals("ADMIN", true),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        "Remove User",
                        color = if (user.role.equals("ADMIN", true)) glassColors.textMuted else Color(0xFFB91C1C),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProKeysSection(
    tiers: List<ProPlanTier>,
    selectedTier: ProPlanTier,
    onSelectTier: (ProPlanTier) -> Unit,
    buyerNote: String,
    onBuyerNoteChange: (String) -> Unit,
    generatedKey: ProLicenseKeyEntity?,
    keys: List<ProLicenseKeyEntity>,
    keyFilter: String,
    onSelectFilter: (String) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onCopyMessage: (ProLicenseKeyEntity) -> Unit,
    onDelete: (ProLicenseKeyEntity) -> Unit
) {
    val glassColors = LocalGlassTheme.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSurfaceCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Generate New Single-Use Pro Key", color = glassColors.textPrimary, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    tiers.forEach { tier ->
                        val durationLabel = when (tier) {
                            ProPlanTier.WEEKLY -> "Weekly (Rs. 1,000 • 7d)"
                            ProPlanTier.MONTHLY -> "Monthly (Rs. 3,000 • 30d)"
                            ProPlanTier.YEARLY -> "Yearly (Rs. 25,000 • 365d)"
                        }
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { onSelectTier(tier) },
                            label = { Text(durationLabel, fontSize = 12.sp) },
                            colors = adminChipColors()
                        )
                    }
                }
                AdminTextField(
                    value = buyerNote,
                    onValueChange = onBuyerNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Buyer name / optional note",
                    singleLine = true
                )
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminButtonColors()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Generate Single-Use Pro Key")
                }
                generatedKey?.let { key ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = glassColors.chipBackground),
                        border = BorderStroke(1.dp, PlatinumGold)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("NEW KEY", color = PlatinumGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                key.licenseKey,
                                color = glassColors.textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(key.aiNote, color = glassColors.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onCopy(key.licenseKey) }) {
                                    Text("1-Tap Copy Key", color = adminAccentColor(), fontSize = 12.sp)
                                }
                                TextButton(onClick = { onCopyMessage(key) }) {
                                    Text("Copy Full Message", color = adminAccentColor(), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Generated Keys Ledger", color = adminAccentColor(), fontWeight = FontWeight.Bold)
        AdminChipRow(
            listOf("All Keys", "Available", "Redeemed"),
            keyFilter,
            onSelectFilter
        )
        val visibleKeys = keys.filter {
            when (keyFilter) {
                "Available" -> !it.isUsed
                "Redeemed" -> it.isUsed
                else -> true
            }
        }
        if (visibleKeys.isEmpty()) {
            EmptyState(if (keys.isEmpty()) "No Pro keys have been generated yet." else "No keys match this filter.")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleKeys, key = { it.licenseKey }) { key ->
                    ProKeyCard(key, onCopy, onDelete)
                }
            }
        }
    }
}

@Composable
private fun ProKeyCard(
    key: ProLicenseKeyEntity,
    onCopy: (String) -> Unit,
    onDelete: (ProLicenseKeyEntity) -> Unit
) {
    val glassColors = LocalGlassTheme.current
    AdminSurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    key.licenseKey,
                    Modifier.weight(1f),
                    color = glassColors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(if (key.isUsed) "USED & LOCKED" else "AVAILABLE", if (key.isUsed) Color(0xFFB91C1C) else DeepRoyalEmerald)
            }
            Text("${key.planTier} • ${key.durationDays} days • Rs. ${key.pricePkr}", color = glassColors.textSecondary, fontSize = 12.sp)
            if (key.isUsed) {
                Text("Redeemed by @${key.usedByUsername} • ${key.usedByEmail}", color = glassColors.textPrimary, fontSize = 12.sp)
                Text(
                    "Activated: ${formatTimestamp(key.activatedAt)} • Expires: ${formatTimestamp(key.expiresAt)}",
                    color = glassColors.textSecondary,
                    fontSize = 11.sp
                )
            }
            Row {
                TextButton(onClick = { onCopy(key.licenseKey) }) {
                    Text("Copy", color = adminAccentColor(), fontSize = 12.sp)
                }
                if (!key.isUsed) {
                    TextButton(onClick = { onDelete(key) }) {
                        Text("Revoke / Delete", color = Color(0xFFB91C1C), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesSection(
    users: List<UserEntity>,
    messages: List<AdminMessageEntity>,
    showHistory: Boolean,
    onShowHistory: (Boolean) -> Unit,
    targetAll: Boolean,
    onTargetAllChange: (Boolean) -> Unit,
    selectedUsers: Set<String>,
    onSelectedUsersChange: (Set<String>) -> Unit,
    messageType: String,
    onMessageTypeChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
    onDelete: (AdminMessageEntity) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminChipRow(
            listOf("Compose Message", "Sent History (${messages.size})"),
            if (showHistory) "Sent History (${messages.size})" else "Compose Message",
            { onShowHistory(it.startsWith("Sent")) }
        )
        if (showHistory) {
            if (messages.isEmpty()) {
                EmptyState("No messages have been sent yet.")
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages, key = { it.id }) { message ->
                        MessageHistoryCard(message, onDelete)
                    }
                }
            }
        } else {
            AdminChipRow(
                listOf("All Users", "Selected Users (${selectedUsers.size})"),
                if (targetAll) "All Users" else "Selected Users (${selectedUsers.size})",
                { onTargetAllChange(it == "All Users") }
            )
            if (!targetAll) {
                AdminSurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp)
                    ) {
                        users.forEach { user ->
                            val key = user.firebaseUid.ifBlank { user.username }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = key in selectedUsers,
                                    onCheckedChange = { checked ->
                                        onSelectedUsersChange(if (checked) selectedUsers + key else selectedUsers - key)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepRoyalEmerald)
                                )
                                Text(
                                    "${user.fullName} (@${user.username})",
                                    color = LocalGlassTheme.current.textPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            AdminChipRow(
                listOf("ANNOUNCEMENT", "WARNING", "DIRECT_MESSAGE"),
                messageType,
                onMessageTypeChange
            )
            AdminTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Message title",
                singleLine = true
            )
            AdminTextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = "Write your announcement or message…",
                minLines = 4
            )
            Button(
                onClick = onSend,
                enabled = title.isNotBlank() && body.isNotBlank() && (targetAll || selectedUsers.isNotEmpty()),
                modifier = Modifier.fillMaxWidth(),
                colors = adminButtonColors()
            ) {
                Text(if (targetAll) "Send to All Users" else "Send to ${selectedUsers.size} Selected")
            }
        }
    }
}

@Composable
private fun MessageHistoryCard(message: AdminMessageEntity, onDelete: (AdminMessageEntity) -> Unit) {
    val glassColors = LocalGlassTheme.current
    AdminSurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.title, Modifier.weight(1f), color = glassColors.textPrimary, fontWeight = FontWeight.Bold)
                StatusBadge(message.messageType, PlatinumGold)
            }
            Text(message.body, color = glassColors.textSecondary)
            Text(
                "${if (message.targetAll) "All users" else "${message.targetUsersCsv.split(',').size} selected user(s)"} • ${formatTimestamp(message.timestamp)}",
                color = glassColors.textMuted,
                fontSize = 11.sp
            )
            TextButton(onClick = { onDelete(message) }) {
                Text("Delete", color = Color(0xFFB91C1C))
            }
        }
    }
}

@Composable
private fun ReportsSection(
    reports: List<UserReportEntity>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onReply: (UserReportEntity, String, String) -> Unit,
    onWarn: (UserReportEntity) -> Unit,
    onDelete: (UserReportEntity) -> Unit
) {
    val visibleReports = reports.filter { filter == "All" || it.status == filter }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminChipRow(listOf("All", "OPEN", "IN_PROGRESS", "RESOLVED"), filter, onFilterChange)
        if (visibleReports.isEmpty()) {
            EmptyState(if (reports.isEmpty()) "No user reports are available." else "No reports match this status.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleReports, key = { it.id }) { report ->
                    ReportModerationCard(report, onReply, onWarn, onDelete)
                }
            }
        }
    }
}

@Composable
private fun ReportModerationCard(
    report: UserReportEntity,
    onReply: (UserReportEntity, String, String) -> Unit,
    onWarn: (UserReportEntity) -> Unit,
    onDelete: (UserReportEntity) -> Unit
) {
    val glassColors = LocalGlassTheme.current
    var reply by remember(report.id) { mutableStateOf("") }
    AdminSurfaceCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(report.subject, Modifier.weight(1f), color = glassColors.textPrimary, fontWeight = FontWeight.Bold)
                StatusBadge(report.status, if (report.status == "RESOLVED") DeepRoyalEmerald else PlatinumGold)
            }
            Text(
                "${report.reporterFullName} (@${report.reporterUsername}) • ${report.reporterEmail}",
                color = glassColors.textSecondary,
                fontSize = 12.sp
            )
            Text("${report.category} • ${formatTimestamp(report.timestamp)}", color = glassColors.textMuted, fontSize = 11.sp)
            Text(report.description, color = glassColors.textPrimary)
            if (report.adminReply.isNotBlank()) {
                Text("Previous admin reply: ${report.adminReply}", color = glassColors.textSecondary, fontSize = 12.sp)
            }
            AdminTextField(
                value = reply,
                onValueChange = { reply = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Admin reply",
                minLines = 2
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onReply(report, "RESOLVED", reply) },
                    enabled = reply.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = adminButtonColors()
                ) { Text("Reply & Resolve") }
                Button(
                    onClick = { onReply(report, "IN_PROGRESS", reply) },
                    enabled = reply.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlatinumGold,
                        contentColor = Color(0xFF1F2937)
                    )
                ) { Text("Reply (In Progress)") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onWarn(report) }, modifier = Modifier.weight(1f)) {
                        Text("Warn Reporter", color = adminAccentColor(), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onDelete(report) }, modifier = Modifier.weight(1f)) {
                        Text("Delete", color = Color(0xFFB91C1C), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassTheme.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = glassColors.surface),
        border = BorderStroke(1.dp, if (glassColors.isDark) PlatinumGold.copy(alpha = 0.35f) else DeepRoyalEmerald.copy(alpha = 0.2f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    val glassColors = LocalGlassTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = glassColors.textMuted) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = glassColors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = glassColors.textPrimary,
            unfocusedTextColor = glassColors.textPrimary,
            focusedContainerColor = glassColors.surface,
            unfocusedContainerColor = glassColors.surface,
            focusedBorderColor = if (glassColors.isDark) PlatinumGold else DeepRoyalEmerald,
            unfocusedBorderColor = glassColors.textMuted,
            focusedLabelColor = if (glassColors.isDark) PlatinumGold else DeepRoyalEmerald,
            unfocusedLabelColor = glassColors.textSecondary,
            cursorColor = if (glassColors.isDark) PlatinumGold else DeepRoyalEmerald
        )
    )
}

@Composable
private fun AdminChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, maxLines = 1, fontSize = 12.sp) },
                colors = adminChipColors()
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, backgroundColor: Color) {
    val glassColors = LocalGlassTheme.current
    val textColor = when (backgroundColor) {
        PlatinumGold -> Color(0xFF1F2937)
        glassColors.chipBackground -> glassColors.textPrimary
        else -> Color.White
    }
    Box(
        Modifier.clip(CircleShape).background(backgroundColor).padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EmptyState(text: String) {
    val glassColors = LocalGlassTheme.current
    AdminSurfaceCard(Modifier.fillMaxWidth()) {
        Text(
            text,
            color = glassColors.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(18.dp)
        )
    }
}

@Composable
private fun adminChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = LocalGlassTheme.current.surface,
    labelColor = LocalGlassTheme.current.textSecondary,
    selectedContainerColor = DeepRoyalEmerald,
    selectedLabelColor = Color.White
)

@Composable
private fun adminButtonColors() = ButtonDefaults.buttonColors(
    containerColor = DeepRoyalEmerald,
    contentColor = Color.White,
    disabledContainerColor = DeepRoyalEmerald.copy(alpha = 0.45f),
    disabledContentColor = Color.White.copy(alpha = 0.8f)
)

@Composable
private fun adminAccentColor(): Color =
    if (LocalGlassTheme.current.isDark) PlatinumGold else DeepRoyalEmerald

private fun fullKeyMessage(key: ProLicenseKeyEntity): String =
    "Assalamu Alaikum! Your ${key.planTier} access for Ayaan Islamic Studio is ready.\n\n" +
        "Your single-use activation key: ${key.licenseKey}\n\n${key.aiNote}\n\n" +
        "Open the app and enter this key in Pro Subscription to activate."

private fun formatTimestamp(timestamp: Long): String =
    if (timestamp <= 0L) "Not available" else DateFormat.getDateTimeInstance().format(Date(timestamp))
