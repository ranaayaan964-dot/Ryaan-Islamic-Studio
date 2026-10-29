package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite

@Composable
fun TermsAgreementScreen(onAgree: () -> Unit) {
    var accepted by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PearlBackground, PureWhite, MaterialTheme.colorScheme.surface)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(PureWhite, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                color = DeepRoyalEmerald,
                fontSize = 23.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ayaan Islamic Studio",
                color = DeepRoyalEmerald,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Terms of Use & Shariah Notice",
                color = PlatinumGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = """
                    Please read before continuing.

                    • This app provides Quran text, translations, prayer-time tools, and educational content. Translations and explanatory materials are not the Quran itself.

                    • For religious rulings, personal worship questions, or matters requiring scholarly judgment, consult a qualified and trusted scholar. App content is informational and is not a fatwa.

                    • Prayer times and location-based features are estimates and may differ from your local mosque or authority. Confirm important timings locally.

                    • Use the app respectfully and lawfully. Protect your account credentials and do not share access to your account.

                    • By continuing, you acknowledge that network features may process account and app data according to the service providers' privacy terms.
                """.trimIndent(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text(
                    text = "I have read and agree to these terms and understand this Shariah notice.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAgree,
                enabled = accepted,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepRoyalEmerald,
                    contentColor = PureWhite
                )
            ) {
                Text(
                    text = "AGREE & ENTER AYAAN ISLAMIC STUDIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
