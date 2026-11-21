package com.ayaan.islamicstudio.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val WILMS_YOUTUBE_URL =
    "https://www.youtube.com/channel/UCS__GvfRK9icDQR98s_RsGg?sub_confirmation=1"
private const val WILMS_WEBSITE_URL = "https://wilmsresearchcentre.netlify.app/"
private const val WILMS_WHATSAPP_URL = "https://wa.me/923056319464"

@Composable
fun WilmsAiPromotionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val contentMaxHeight = (screenHeight * 0.58f).coerceIn(220.dp, 480.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WILMS AI — Learn, Create, Grow") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = contentMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Explore AI, digital skills, and research training from the World Institute of " +
                        "Linguistics, Literature & Mind Sciences."
                )
                Text(
                    "Courses include Generative AI, NLP, Python, cybersecurity, research and thesis " +
                        "tools, graphic design, video editing, web design, and freelancing."
                )
                Text(
                    "Learn with live evening sessions, recorded lessons, notes, templates, and " +
                        "certification opportunities."
                )
                Text("WhatsApp enrollment: 03056319464")
                Text("Director & Founder: R.A. Ghafoor, MA English, M.Phil English, PhD English Scholar")
                TextButton(
                    onClick = {
                        openWilmsLink(context, WILMS_WEBSITE_URL)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Visit WILMS AI website")
                }
                TextButton(
                    onClick = {
                        openWilmsLink(context, WILMS_WHATSAPP_URL)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Contact on WhatsApp")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openWilmsLink(context, WILMS_YOUTUBE_URL)
                    onDismiss()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Subscribe on YouTube")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue to app")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun openWilmsLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: Exception) {
        Toast.makeText(context, "Could not open the link: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
