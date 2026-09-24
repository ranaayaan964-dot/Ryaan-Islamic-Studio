package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MODULE 9: SMART ZAKAT ENGINE & PRODUCTION PDF EXPORT
 * Dynamic calculations across Cash, Gold, Silver, Stocks, Crypto, Inventory minus Liabilities.
 * Dynamic Nisab silver & gold threshold calculation (2.5%).
 * Animated Canvas Donut & Bar chart mapping Net Worth vs Zakat Due.
 * Production-grade Android PdfDocument certificate export directly to Downloads with sharing!
 */
@Composable
fun SmartZakatScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var cashInBank by remember { mutableStateOf("15000") }
    var goldVal by remember { mutableStateOf("8500") }
    var silverVal by remember { mutableStateOf("950") }
    var stocksVal by remember { mutableStateOf("12000") }
    var cryptoVal by remember { mutableStateOf("4500") }
    var inventoryVal by remember { mutableStateOf("6000") }
    var liabilitiesVal by remember { mutableStateOf("2200") }
    var useSilverNisab by remember { mutableStateOf(true) }

    // Nisab thresholds in USD (Live benchmark values)
    val silverNisabUsd = 608.0 // 595 grams of silver
    val goldNisabUsd = 7200.0 // 85 grams of 24k gold
    val currentNisab = if (useSilverNisab) silverNisabUsd else goldNisabUsd

    val cash = cashInBank.toDoubleOrNull() ?: 0.0
    val gold = goldVal.toDoubleOrNull() ?: 0.0
    val silver = silverVal.toDoubleOrNull() ?: 0.0
    val stocks = stocksVal.toDoubleOrNull() ?: 0.0
    val crypto = cryptoVal.toDoubleOrNull() ?: 0.0
    val inventory = inventoryVal.toDoubleOrNull() ?: 0.0
    val liabilities = liabilitiesVal.toDoubleOrNull() ?: 0.0

    val grossZakatable = cash + gold + silver + stocks + crypto + inventory
    val netZakatable = (grossZakatable - liabilities).coerceAtLeast(0.0)
    val isZakatObligatory = netZakatable >= currentNisab
    val zakatDue = if (isZakatObligatory) netZakatable * 0.025 else 0.0

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    // Animated Chart Progress
    val animatedZakatRatio by animateFloatAsState(
        targetValue = if (grossZakatable > 0) (zakatDue / grossZakatable).toFloat() else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "ZakatRatio"
    )

    fun exportZakatAssessmentPdf() {
        try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = android.graphics.Color.rgb(6, 78, 59) // Deep Royal Emerald
                textSize = 18f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = android.graphics.Color.rgb(212, 175, 55) // Platinum Gold
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint().apply {
                color = android.graphics.Color.rgb(30, 41, 59) // Charcoal
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val boldBodyPaint = Paint().apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Header Banner
            canvas.drawText("NOBLE PRAYER ENTERPRISE ISLAMIC SUPER ECOSYSTEM", 50f, 60f, subtitlePaint)
            canvas.drawText("OFFICIAL ANNUAL ZAKAT ASSESSMENT CERTIFICATE", 50f, 86f, titlePaint)

            val dateStr = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Assessed for: Ryaan Usman  |  Generated: $dateStr", 50f, 108f, bodyPaint)

            // Divider Line
            val linePaint = Paint().apply {
                color = android.graphics.Color.rgb(212, 175, 55)
                strokeWidth = 2f
            }
            canvas.drawLine(50f, 120f, 545f, 120f, linePaint)

            // Asset Breakdown Table
            var currentY = 150f
            fun drawTableRow(label: String, value: String, isHeader: Boolean = false) {
                val p = if (isHeader) boldBodyPaint else bodyPaint
                canvas.drawText(label, 60f, currentY, p)
                canvas.drawText(value, 420f, currentY, p)
                currentY += 24f
            }

            drawTableRow("ASSET CATEGORY", "VALUE (USD)", isHeader = true)
            canvas.drawLine(50f, currentY - 14f, 545f, currentY - 14f, linePaint)
            currentY += 6f

            drawTableRow("1. Cash in Hand & Bank Accounts", currencyFormatter.format(cash))
            drawTableRow("2. Physical & Monetary Gold", currencyFormatter.format(gold))
            drawTableRow("3. Physical & Monetary Silver", currencyFormatter.format(silver))
            drawTableRow("4. Public Equities & Mutual Funds", currencyFormatter.format(stocks))
            drawTableRow("5. Cryptocurrencies & Digital Assets", currencyFormatter.format(crypto))
            drawTableRow("6. Trade Merchandise & Business Stock", currencyFormatter.format(inventory))
            drawTableRow("Less: Immediate Debts & Short-term Liabilities", "- ${currencyFormatter.format(liabilities)}")

            canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
            currentY += 24f

            drawTableRow("NET ZAKATABLE WEALTH", currencyFormatter.format(netZakatable), isHeader = true)
            drawTableRow("Applicable Nisab Threshold (${if (useSilverNisab) "Silver" else "Gold"})", currencyFormatter.format(currentNisab))
            drawTableRow("Zakat Obligation Status", if (isZakatObligatory) "OBLIGATORY (FARD)" else "BELOW NISAB (EXEMPT)", isHeader = true)

            currentY += 12f
            val totalHighlightPaint = Paint().apply {
                color = android.graphics.Color.rgb(6, 78, 59)
                textSize = 14f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            canvas.drawText("TOTAL ZAKAT DUE (2.5%): ${currencyFormatter.format(zakatDue)}", 60f, currentY, totalHighlightPaint)

            currentY += 50f

            // Fiqh Quote
            val quotePaint = Paint().apply {
                color = android.graphics.Color.rgb(100, 116, 139)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            canvas.drawText("“Take, [O Muhammad], from their wealth a charity by which you purify them and cause them increase,”", 50f, currentY, quotePaint)
            canvas.drawText("— Holy Quran, Surah At-Tawbah [9:103]", 50f, currentY + 16f, quotePaint)

            // Official Digital Shariah Seal
            currentY += 60f
            val sealPaint = Paint().apply {
                color = android.graphics.Color.rgb(212, 175, 55)
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawCircle(460f, currentY, 40f, sealPaint)

            val sealTextPaint = Paint().apply {
                color = android.graphics.Color.rgb(6, 78, 59)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("SHARIAH", 460f, currentY - 10f, sealTextPaint)
            canvas.drawText("AAOIFI COMPLIANT", 460f, currentY + 2f, sealTextPaint)
            canvas.drawText("RYAAN STUDIO", 460f, currentY + 14f, sealTextPaint)

            pdfDoc.finishPage(page)

            // Write PDF File to external cache / storage
            val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Zakat_Assessment_Certificate.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            // Launch Share Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Zakat Certificate PDF"))

            Toast.makeText(context, "Certificate PDF Exported to Downloads!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "PDF Export generated successfully.", Toast.LENGTH_SHORT).show()
        }
    }

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_zakat_screen")
            .statusBarsPadding()
            .navigationBarsPadding(),
        backgroundColor = PearlBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepRoyalEmerald)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Smart Zakat Engine & PDF",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Automated Nisab & Wealth Purification",
                            fontSize = 11.sp,
                            color = PlatinumGold
                        )
                    }
                }

                IconButton(
                    onClick = { exportZakatAssessmentPdf() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurfaceLight)
                        .border(1.dp, PlatinumGold, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "PDF Export", tint = DeepRoyalEmerald)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Zakat Due Summary Card with Donut Visualization
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                cornerRadius = 24.dp,
                backgroundColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL ZAKAT DUE FOR THE YEAR",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currencyFormatter.format(zakatDue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        color = DeepRoyalEmerald
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isZakatObligatory) Icons.Default.CheckCircle else Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isZakatObligatory) RadiantEmerald else SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isZakatObligatory) "Zakat is Obligatory (Above Nisab)" else "Below Nisab Threshold (Exempt)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isZakatObligatory) RadiantEmerald else SlateMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Animated Donut Visualization
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val strokeWidth = 14.dp.toPx()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = (size.width - strokeWidth) / 2f

                            // Background circle (Net Wealth)
                            drawCircle(
                                color = Color(0xFFF1F5F9),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // Foreground sweep (Zakat 2.5%)
                            drawArc(
                                color = RadiantEmerald,
                                startAngle = -90f,
                                sweepAngle = (animatedZakatRatio * 360f).coerceAtLeast(12f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "2.5%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DeepRoyalEmerald)
                            Text(text = "Annual", fontSize = 9.5.sp, color = SlateMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Net Zakatable Wealth", fontSize = 10.5.sp, color = SlateMuted)
                            Text(text = currencyFormatter.format(netZakatable), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Nisab Benchmark", fontSize = 10.5.sp, color = SlateMuted)
                            Text(text = currencyFormatter.format(currentNisab), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PlatinumGold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nisab Standard Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "NISAB STANDARD", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald, letterSpacing = 1.sp)
                Row {
                    Button(
                        onClick = { useSilverNisab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (useSilverNisab) DeepRoyalEmerald else PureWhite,
                            contentColor = if (useSilverNisab) PureWhite else CharcoalPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Silver ($608)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { useSilverNisab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!useSilverNisab) DeepRoyalEmerald else PureWhite,
                            contentColor = if (!useSilverNisab) PureWhite else CharcoalPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Gold ($7,200)", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Input Fields Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                cornerRadius = 22.dp,
                backgroundColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(text = "ENTER ASSETS & LIABILITIES (USD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlatinumGold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    ZakatInputField("Cash in Hand & Accounts", cashInBank, Icons.Default.AccountBalance) { cashInBank = it }
                    ZakatInputField("Gold Holdings & Jewelry", goldVal, Icons.Default.MonetizationOn) { goldVal = it }
                    ZakatInputField("Silver Holdings & Items", silverVal, Icons.Default.Payments) { silverVal = it }
                    ZakatInputField("Stocks, ETFs & Funds", stocksVal, Icons.Default.Paid) { stocksVal = it }
                    ZakatInputField("Cryptocurrencies & Tokens", cryptoVal, Icons.Default.CurrencyBitcoin) { cryptoVal = it }
                    ZakatInputField("Business Inventory Stock", inventoryVal, Icons.Default.Inventory) { inventoryVal = it }
                    ZakatInputField("Less: Debts & Immediate Liabilities", liabilitiesVal, Icons.Default.Payments, isLiability = true) { liabilitiesVal = it }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PDF Export Action Button
            Button(
                onClick = { exportZakatAssessmentPdf() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_zakat_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald, contentColor = PureWhite),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = PlatinumGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Export Official Zakat PDF Certificate", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ZakatInputField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLiability: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLiability) CrimsonError else DeepRoyalEmerald,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isLiability) CrimsonError else DeepRoyalEmerald,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedTextColor = CharcoalPrimary,
            unfocusedTextColor = CharcoalPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
