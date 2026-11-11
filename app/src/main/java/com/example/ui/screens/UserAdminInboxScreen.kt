package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.database.UserReportEntity
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun UserAdminInboxScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AdminRepository(context) }
    val userDao = remember(context) { AppDatabase.getInstance(context).userDao() }
    val user by userDao.getCurrentUserFlow().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("App Bug") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val uid = user?.firebaseUid.orEmpty()
    val authenticatedUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val messages by repository.observeMessagesForUser(uid).collectAsStateWithLifecycle(emptyList())
    val reports by repository.observeReportsForUser(uid).collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(uid, authenticatedUid) {
        if (uid.isNotBlank() && authenticatedUid == uid) {
            try {
                repository.syncMessagesForCurrentUser()
                repository.syncReportsForCurrentUser()
            } catch (e: Exception) {
                notice = e.localizedMessage ?: "Could not sync inbox. Cached items remain available."
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Admin Messages & Alerts", style = MaterialTheme.typography.titleLarge)
                Text("Official Updates & Warnings from Admin Ayaan")
            }
            TextButton(onClick = onNavigateBack) { Text("Back") }
        }
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Messages") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Report Issue") })
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (user == null || uid.isBlank() || authenticatedUid != uid) {
            Text("A verified Firebase account is required to read messages or send a report.")
        } else if (selectedTab == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.id }) { message ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                when (message.messageType) {
                                    "WARNING" -> "OFFICIAL WARNING FROM ADMIN AYAAN"
                                    "DIRECT_MESSAGE" -> "Sent directly to you"
                                    else -> if (message.targetAll) "Global announcement" else "Admin message"
                                },
                                color = if (message.messageType == "WARNING") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Text(message.title, style = MaterialTheme.typography.titleMedium)
                            Text(message.body)
                            Text(DateFormat.getDateTimeInstance().format(Date(message.timestamp)))
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(listOf("App Bug", "Namaz Alarm Issue", "Quran / Audio Issue", "Account Help", "Suggestion")) { item ->
                        FilterChip(category == item, { category = item }, label = { Text(item) })
                    }
                }
                OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), label = { Text("Subject") })
                OutlinedTextField(
                    description,
                    { description = it },
                    Modifier.fillMaxWidth().weight(1f),
                    label = { Text("Problem description") }
                )
                Button(
                    onClick = {
                        val activeUser = user ?: return@Button
                        if (subject.isBlank() || description.isBlank()) {
                            notice = "Enter a subject and problem description."
                            return@Button
                        }
                        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != activeUser.firebaseUid) {
                            notice = "Sign in with a verified Firebase account before submitting a report."
                            return@Button
                        }
                        scope.launch {
                            try {
                                repository.submitUserReport(activeUser, category, subject, description)
                                subject = ""
                                description = ""
                                notice = "Report submitted."
                                repository.syncReportsForCurrentUser()
                            } catch (e: Exception) {
                                notice = "Report saved locally; cloud sync will retry when you reopen this screen. " +
                                    (e.localizedMessage ?: "Cloud sync failed.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit Report to Admin") }
                Text("Previous reports & replies", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reports, key = { it.id }) { report -> UserReportCard(report) }
                }
            }
        }
    }
}

@Composable
private fun UserReportCard(report: UserReportEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text("${report.subject} • ${report.status}", style = MaterialTheme.typography.titleSmall)
            Text("${report.category} • ${DateFormat.getDateTimeInstance().format(Date(report.timestamp))}")
            Text(report.description)
            if (report.adminReply.isNotBlank()) Text("Admin reply: ${report.adminReply}")
        }
    }
}
