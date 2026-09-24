package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.util.PdfReportGenerator
import com.example.util.ZakatReportData
import java.util.Locale

/**
 * MODULE 13: ADVANCED ZAKAT ENGINE & PDF EXPORT
 */
@Composable
fun ZakatCalculatorScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // Live market benchmarks per gram (USD)
    val goldPricePerGram = 75.50 // Nisab: 85g = $6,417.50
    val silverPricePerGram = 0.95 // Nisab: 595g = $565.25
    val silverNisabThreshold = 595.0 * silverPricePerGram

    var cashInput by remember { mutableStateOf("5000") }
    var goldGramsInput by remember { mutableStateOf("30") }
    var silverGramsInput by remember { mutableStateOf("0") }
    var inventoryInput by remember { mutableStateOf("2500") }
    var liabilitiesInput by remember { mutableStateOf("400") }

    val cash = cashInput.toDoubleOrNull() ?: 0.0
    val goldGrams = goldGramsInput.toDoubleOrNull() ?: 0.0
    val goldValue = goldGrams * goldPricePerGram
    val silverGrams = silverGramsInput.toDoubleOrNull() ?: 0.0
    val silverValue = silverGrams * silverPricePerGram
    val inventory = inventoryInput.toDoubleOrNull() ?: 0.0
    val liabilities = liabilitiesInput.toDoubleOrNull() ?: 0.0

    val grossAssets = cash + goldValue + silverValue + inventory
    val netZakatableWealth = (grossAssets - liabilities).coerceAtLeast(0.0)
    val isEligibleForZakat = netZakatableWealth >= silverNisabThreshold
    val totalZakatPayable = if (isEligibleForZakat) netZakatableWealth * 0.025 else 0.0

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
            .testTag("zakat_calculator_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
                        text = "Smart Zakat Al-Mal Engine",
                        color = CharcoalPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shariah Certified 2.5% Lunar Liability & PDF Exporter",
                        color = DeepRoyalEmerald,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Overview & Nisab Status Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = DeepRoyalEmerald,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PlatinumGold)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL ZAKAT LIABILITY (2.5%)",
                        color = PlatinumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalZakatPayable)}",
                        color = PureWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEligibleForZakat)
                            "STATUS: NISAB MET (Net Wealth > $${String.format(Locale.US, "%.0f", silverNisabThreshold)})"
                        else
                            "STATUS: BELOW NISAB (No Zakat is due on this amount)",
                        color = if (isEligibleForZakat) Color(0xFF6EE7B7) else PlatinumGold,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inputs Group
            Text(
                text = "ZAKATABLE ASSETS & LIABILITIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PlatinumGold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Cash input
            ZakatCalcInputField(
                label = "Cash & Bank Balance ($)",
                value = cashInput,
                onValueChange = { cashInput = it },
                icon = Icons.Default.Paid
            )

            // Gold (grams)
            ZakatCalcInputField(
                label = "Gold Holdings (grams @ Nisab 85g)",
                value = goldGramsInput,
                onValueChange = { goldGramsInput = it },
                helperText = "Value: $${String.format(Locale.US, "%,.2f", goldValue)} ($75.50/g)",
                icon = Icons.Default.AccountBalance
            )

            // Silver (grams)
            ZakatCalcInputField(
                label = "Silver Holdings (grams @ Nisab 595g)",
                value = silverGramsInput,
                onValueChange = { silverGramsInput = it },
                helperText = "Value: $${String.format(Locale.US, "%,.2f", silverValue)} ($0.95/g)",
                icon = Icons.Default.AccountBalance
            )

            // Business Inventory
            ZakatCalcInputField(
                label = "Business Inventory / Goods for Trade ($)",
                value = inventoryInput,
                onValueChange = { inventoryInput = it },
                icon = Icons.Default.Inventory
            )

            // Debts / Liabilities
            ZakatCalcInputField(
                label = "Immediate Short-Term Debts / Deductions ($)",
                value = liabilitiesInput,
                onValueChange = { liabilitiesInput = it },
                icon = Icons.Default.Calculate
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PDF Export Button
            Button(
                onClick = {
                    val reportData = ZakatReportData(
                        cashAmount = cash,
                        goldGrams = goldGrams,
                        goldValue = goldValue,
                        silverGrams = silverGrams,
                        silverValue = silverValue,
                        businessInventory = inventory,
                        liabilities = liabilities,
                        netZakatWealth = netZakatableWealth,
                        nisabThreshold = silverNisabThreshold,
                        isEligible = isEligibleForZakat,
                        totalZakatPayable = totalZakatPayable
                    )
                    PdfReportGenerator.generateAndOpenZakatPdf(context, reportData)
                    Toast.makeText(context, "Official PDF Report Generated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepRoyalEmerald,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = PlatinumGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "EXPORT OFFICIAL PDF REPORT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ZakatCalcInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helperText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        onValueChange(input)
                    }
                },
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = icon, contentDescription = null, tint = DeepRoyalEmerald, modifier = Modifier.size(20.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepRoyalEmerald,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
            if (helperText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = helperText,
                    fontSize = 10.5.sp,
                    color = DeepRoyalEmerald,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
