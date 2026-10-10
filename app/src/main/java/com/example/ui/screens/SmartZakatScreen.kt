package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.PearlWhiteSubtle
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import com.example.util.PdfReportGenerator
import com.example.util.ZakatReportData
import java.text.NumberFormat
import java.util.Locale

/**
 * MODULE 9: SMART ZAKAT ENGINE & ENTERPRISE AUDIT DASHBOARD (VERSION 1.1)
 *
 * Luxury Pearl White and Platinum Gold Financial Dashboard.
 * - Dynamic live Nisab threshold calculation for Gold (~85g) & Silver (~595g) in USD & PKR.
 * - Dynamic input fields for Cash, Gold (in grams), Silver (in grams), Equities/Stocks,
 *   Cryptocurrencies, and Business Inventory minus Short-term Debts/Liabilities.
 * - Interactive Animated Jetpack Compose Canvas Donut & Asset Breakdown Proportion Chart.
 * - One-tap generation of publication-quality A4 PDF Zakat Certificate via PdfReportGenerator.kt,
 *   saved to Downloads and launched immediately via FileProvider with sharing support.
 */
@Composable
fun SmartZakatScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Currency Selection: "USD" or "PKR"
    var selectedCurrency by remember { mutableStateOf("USD") }
    val isPkr = selectedCurrency == "PKR"
    val currencySymbol = if (isPkr) "Rs " else "$"

    // Default Benchmark Metal Rates (USD vs PKR spot estimates)
    // 24K Gold: ~$85/g in USD, ~Rs 25,500/g in PKR
    // Fine Silver: ~$1.05/g in USD, ~Rs 320/g in PKR
    var goldRateUsd by remember { mutableDoubleStateOf(85.0) }
    var silverRateUsd by remember { mutableDoubleStateOf(1.05) }

    var goldRatePkr by remember { mutableDoubleStateOf(25500.0) }
    var silverRatePkr by remember { mutableDoubleStateOf(320.0) }

    val currentGoldRate = if (isPkr) goldRatePkr else goldRateUsd
    val currentSilverRate = if (isPkr) silverRatePkr else silverRateUsd

    // Nisab Standard: Silver (~595 grams) vs Gold (~85 grams)
    var useSilverNisab by remember { mutableStateOf(true) }

    val silverNisabThreshold = 595.0 * currentSilverRate
    val goldNisabThreshold = 85.0 * currentGoldRate
    val activeNisabThreshold = if (useSilverNisab) silverNisabThreshold else goldNisabThreshold

    // Asset & Liability State Fields
    var donorName by remember { mutableStateOf("Honorable Believer") }
    var cashInput by remember { mutableStateOf(if (isPkr) "2500000" else "15000") }
    var goldGramsInput by remember { mutableStateOf("120") }
    var silverGramsInput by remember { mutableStateOf("450") }
    var stocksInput by remember { mutableStateOf(if (isPkr) "1500000" else "12000") }
    var cryptoInput by remember { mutableStateOf(if (isPkr) "800000" else "4500") }
    var inventoryInput by remember { mutableStateOf(if (isPkr) "1200000" else "6000") }
    var liabilitiesInput by remember { mutableStateOf(if (isPkr) "450000" else "2200") }

    // Numeric parsing
    val cash = cashInput.toDoubleOrNull() ?: 0.0
    val goldGrams = goldGramsInput.toDoubleOrNull() ?: 0.0
    val silverGrams = silverGramsInput.toDoubleOrNull() ?: 0.0
    val stocks = stocksInput.toDoubleOrNull() ?: 0.0
    val crypto = cryptoInput.toDoubleOrNull() ?: 0.0
    val inventory = inventoryInput.toDoubleOrNull() ?: 0.0
    val liabilities = liabilitiesInput.toDoubleOrNull() ?: 0.0

    // Computed metal values
    val goldValue = goldGrams * currentGoldRate
    val silverValue = silverGrams * currentSilverRate

    // Reconciled wealth calculations
    val grossZakatable = cash + goldValue + silverValue + stocks + crypto + inventory
    val netZakatable = (grossZakatable - liabilities).coerceAtLeast(0.0)
    val isZakatObligatory = netZakatable >= activeNisabThreshold
    val zakatDue = if (isZakatObligatory) netZakatable * 0.025 else 0.0

    // Currency Formatter
    fun formatMoney(amount: Double): String {
        return if (isPkr) {
            "Rs " + String.format(Locale.US, "%,.0f", amount)
        } else {
            NumberFormat.getCurrencyInstance(Locale.US).format(amount)
        }
    }

    // Animated ratios for Canvas
    val animatedZakatRatio by animateFloatAsState(
        targetValue = if (netZakatable > 0 && isZakatObligatory) (zakatDue / netZakatable).toFloat() else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ZakatRatioAnim"
    )

    val animatedNetRatio by animateFloatAsState(
        targetValue = if (grossZakatable > 0) (netZakatable / grossZakatable).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "NetRatioAnim"
    )

    // Build Report Data Object for PDF Generation
    fun buildReportData(): ZakatReportData {
        return ZakatReportData(
            currency = selectedCurrency,
            currencySymbol = currencySymbol,
            cashAmount = cash,
            goldGrams = goldGrams,
            goldRate = currentGoldRate,
            goldValue = goldValue,
            silverGrams = silverGrams,
            silverRate = currentSilverRate,
            silverValue = silverValue,
            stocksValue = stocks,
            cryptoValue = crypto,
            inventoryValue = inventory,
            liabilitiesValue = liabilities,
            grossZakatableWealth = grossZakatable,
            netZakatableWealth = netZakatable,
            nisabBenchmark = if (useSilverNisab) "Silver (595g)" else "Gold (85g)",
            nisabThreshold = activeNisabThreshold,
            isEligible = isZakatObligatory,
            zakatDue = zakatDue,
            donorName = donorName.ifBlank { "Honorable Believer" }
        )
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ==========================================
            // TOP APP BAR WITH LUXURY ACTIONS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, PlatinumGold.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepRoyalEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Zakat Engine",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Financial Audit & Shariah Purification",
                            fontSize = 11.5.sp,
                            color = PlatinumGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val data = buildReportData()
                            PdfReportGenerator.shareZakatPdf(context, data)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, PlatinumGold.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Certificate",
                            tint = DeepRoyalEmerald
                        )
                    }

                    IconButton(
                        onClick = {
                            val data = buildReportData()
                            PdfReportGenerator.generateAndOpenZakatPdf(context, data)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldSurfaceLight)
                            .border(1.dp, RadiantEmerald, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download PDF",
                            tint = DeepRoyalEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // CURRENCY & NISAB SELECTOR CONTROLS
            // ==========================================
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                cornerRadius = 18.dp,
                backgroundColor = PureWhite
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = PlatinumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OPERATIONAL CURRENCY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Currency Switch (USD vs PKR)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PearlWhiteSubtle)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isPkr) DeepRoyalEmerald else Color.Transparent)
                                    .clickable { selectedCurrency = "USD" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "USD ($)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPkr) PureWhite else CharcoalPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPkr) DeepRoyalEmerald else Color.Transparent)
                                    .clickable { selectedCurrency = "PKR" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PKR (Rs)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPkr) PureWhite else CharcoalPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Nisab Standard Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NISAB BENCHMARK:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = useSilverNisab,
                                onClick = { useSilverNisab = true },
                                label = { Text("Silver (595g)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSurfaceLight,
                                    selectedLabelColor = DeepRoyalEmerald
                                )
                            )
                            FilterChip(
                                selected = !useSilverNisab,
                                onClick = { useSilverNisab = false },
                                label = { Text("Gold (85g)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSurfaceLight,
                                    selectedLabelColor = DeepRoyalEmerald
                                )
                            )
                        }
                    }

                    // Live Nisab Threshold Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldSurfaceLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Nisab Cutoff (${if (useSilverNisab) "595g Silver" else "85g Gold"}):",
                            fontSize = 11.sp,
                            color = DeepRoyalEmerald,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatMoney(activeNisabThreshold),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepRoyalEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // HERO FINANCIAL SUMMARY CARD WITH ANIMATED CANVAS DONUT & PROGRESS WHEEL
            // =========================================================================
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
                        text = "TOTAL ANNUAL ZAKAT PAYABLE (2.5%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formatMoney(zakatDue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        color = DeepRoyalEmerald
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isZakatObligatory) EmeraldSurfaceLight else Color(0xFFF1F5F9))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isZakatObligatory) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isZakatObligatory) RadiantEmerald else SlateMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isZakatObligatory) "Zakat is Obligatory (Fard - Exceeds Nisab)" else "Below Nisab Threshold (Exempt)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isZakatObligatory) RadiantEmerald else SlateMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Animated Canvas Donut Chart
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            val strokeWidth = 14.dp.toPx()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = (size.width - strokeWidth) / 2f

                            // Track circle
                            drawCircle(
                                color = Color(0xFFF1F5F9),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // 2.5% Zakat Arc
                            val sweepAngle = if (isZakatObligatory) {
                                (animatedZakatRatio * 360f).coerceAtLeast(18f)
                            } else {
                                0f
                            }

                            if (sweepAngle > 0f) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(RadiantEmerald, DeepRoyalEmerald, PlatinumGold)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "2.5%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepRoyalEmerald
                            )
                            Text(
                                text = "Lunar Rate",
                                fontSize = 9.sp,
                                color = SlateMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Summary Reconciled Wealth Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PearlWhiteSubtle)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Net Zakatable Wealth", fontSize = 10.5.sp, color = SlateMuted)
                            Text(
                                text = formatMoney(netZakatable),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Deductible Liabilities", fontSize = 10.5.sp, color = SlateMuted)
                            Text(
                                text = "- " + formatMoney(liabilities),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // ASSET BREAKDOWN CANVAS BAR & DISTRIBUTION CHART
            // =========================================================================
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 5.dp,
                cornerRadius = 20.dp,
                backgroundColor = PureWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PORTFOLIO ASSET ALLOCATION",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-Segment Canvas Proportion Bar
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                    ) {
                        val total = grossZakatable.coerceAtLeast(1.0)
                        val w = size.width
                        val h = size.height

                        val cashW = (cash / total).toFloat() * w
                        val goldW = (goldValue / total).toFloat() * w
                        val silverW = (silverValue / total).toFloat() * w
                        val stocksW = (stocks / total).toFloat() * w
                        val cryptoW = (crypto / total).toFloat() * w
                        val invW = (inventory / total).toFloat() * w

                        var currentX = 0f

                        // Cash segment (Emerald)
                        if (cashW > 0f) {
                            drawRect(color = DeepRoyalEmerald, topLeft = Offset(currentX, 0f), size = Size(cashW, h))
                            currentX += cashW
                        }
                        // Gold segment (Gold)
                        if (goldW > 0f) {
                            drawRect(color = PlatinumGold, topLeft = Offset(currentX, 0f), size = Size(goldW, h))
                            currentX += goldW
                        }
                        // Silver segment (Silver Slate)
                        if (silverW > 0f) {
                            drawRect(color = Color(0xFF94A3B8), topLeft = Offset(currentX, 0f), size = Size(silverW, h))
                            currentX += silverW
                        }
                        // Stocks segment (Sapphire Blue)
                        if (stocksW > 0f) {
                            drawRect(color = Color(0xFF2563EB), topLeft = Offset(currentX, 0f), size = Size(stocksW, h))
                            currentX += stocksW
                        }
                        // Crypto segment (Purple)
                        if (cryptoW > 0f) {
                            drawRect(color = Color(0xFF9333EA), topLeft = Offset(currentX, 0f), size = Size(cryptoW, h))
                            currentX += cryptoW
                        }
                        // Inventory segment (Amber)
                        if (invW > 0f) {
                            drawRect(color = Color(0xFFF59E0B), topLeft = Offset(currentX, 0f), size = Size(invW, h))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AllocationBadge(color = DeepRoyalEmerald, label = "Cash", amount = formatMoney(cash))
                            AllocationBadge(color = PlatinumGold, label = "Gold", amount = formatMoney(goldValue))
                            AllocationBadge(color = Color(0xFF94A3B8), label = "Silver", amount = formatMoney(silverValue))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AllocationBadge(color = Color(0xFF2563EB), label = "Stocks", amount = formatMoney(stocks))
                            AllocationBadge(color = Color(0xFF9333EA), label = "Crypto", amount = formatMoney(crypto))
                            AllocationBadge(color = Color(0xFFF59E0B), label = "Inventory", amount = formatMoney(inventory))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // DYNAMIC ASSET & LIABILITY INPUT FIELDS
            // =========================================================================
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                cornerRadius = 22.dp,
                backgroundColor = PureWhite
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENTER ASSETS & LIABILITIES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = selectedCurrency,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepRoyalEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Donor Name (for Official Certificate)
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text("Donor / Business Entity Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DeepRoyalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRoyalEmerald,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = PearlWhiteSubtle,
                            unfocusedContainerColor = PearlWhiteSubtle
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 2. Cash in Hand & Bank
                    ZakatInputField(
                        label = "Cash in Hand & Bank Accounts ($currencySymbol)",
                        value = cashInput,
                        icon = Icons.Default.AccountBalance,
                        onValueChange = { cashInput = it }
                    )

                    // 3. Gold in Grams (with computed value)
                    ZakatMetalInputField(
                        label = "Gold Holdings (Grams)",
                        gramsValue = goldGramsInput,
                        metalRate = currentGoldRate,
                        currencySymbol = currencySymbol,
                        computedValue = goldValue,
                        icon = Icons.Default.MonetizationOn,
                        accentColor = PlatinumGold,
                        onGramsChange = { goldGramsInput = it }
                    )

                    // 4. Silver in Grams (with computed value)
                    ZakatMetalInputField(
                        label = "Silver Holdings (Grams)",
                        gramsValue = silverGramsInput,
                        metalRate = currentSilverRate,
                        currencySymbol = currencySymbol,
                        computedValue = silverValue,
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF64748B),
                        onGramsChange = { silverGramsInput = it }
                    )

                    // 5. Public Equities / Stocks
                    ZakatInputField(
                        label = "Public Equities, Stocks & Mutual Funds ($currencySymbol)",
                        value = stocksInput,
                        icon = Icons.Default.TrendingUp,
                        onValueChange = { stocksInput = it }
                    )

                    // 6. Cryptocurrencies
                    ZakatInputField(
                        label = "Cryptocurrencies & Digital Assets ($currencySymbol)",
                        value = cryptoInput,
                        icon = Icons.Default.CurrencyBitcoin,
                        onValueChange = { cryptoInput = it }
                    )

                    // 7. Business Inventory
                    ZakatInputField(
                        label = "Business Inventory & Trade Stock ($currencySymbol)",
                        value = inventoryInput,
                        icon = Icons.Default.Inventory,
                        onValueChange = { inventoryInput = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 8. Deductible Liabilities
                    ZakatInputField(
                        label = "Less: Short-Term Debts & Due Liabilities ($currencySymbol)",
                        value = liabilitiesInput,
                        icon = Icons.Default.TrendingDown,
                        isLiability = true,
                        onValueChange = { liabilitiesInput = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // SCHOLARLY FIQH & SHARIAH WISDOM CITATION CARD
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFCE8)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF08A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHARIAH AUDIT STANDARD (AAOIFI NO. 9)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "“خُذْ مِنْ أَمْوَالِهِمْ صَدَقَةً تُطَهِّرُهُمْ وَتُزَكِّيهِم بِهَا”\n“Take from their wealth a charity to purify and sanctify them...” — Surah At-Tawbah (9:103)",
                        fontSize = 11.sp,
                        color = CharcoalPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // ACTION BUTTON: GENERATE & OPEN OFFICIAL PDF CERTIFICATE
            // =========================================================================
            Button(
                onClick = {
                    val reportData = buildReportData()
                    PdfReportGenerator.generateAndOpenZakatPdf(context, reportData)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("export_zakat_pdf_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepRoyalEmerald,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = PlatinumGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Official Zakat PDF Certificate",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Reusable dynamic text input field for standard currency values.
 */
@Composable
private fun ZakatInputField(
    label: String,
    value: String,
    icon: ImageVector,
    isLiability: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
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
            focusedContainerColor = PearlWhiteSubtle,
            unfocusedContainerColor = PearlWhiteSubtle,
            focusedTextColor = CharcoalPrimary,
            unfocusedTextColor = CharcoalPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Dynamic metal input field calculating total valuation from grams * current spot rate.
 */
@Composable
private fun ZakatMetalInputField(
    label: String,
    gramsValue: String,
    metalRate: Double,
    currencySymbol: String,
    computedValue: Double,
    icon: ImageVector,
    accentColor: Color,
    onGramsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = gramsValue,
            onValueChange = onGramsChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Text(
                    text = "grams",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepRoyalEmerald,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = PearlWhiteSubtle,
                unfocusedContainerColor = PearlWhiteSubtle,
                focusedTextColor = CharcoalPrimary,
                unfocusedTextColor = CharcoalPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rate: $currencySymbol${String.format(Locale.US, "%,.1f", metalRate)}/g",
                fontSize = 10.5.sp,
                color = SlateMuted
            )
            Text(
                text = "Value: $currencySymbol${String.format(Locale.US, "%,.2f", computedValue)}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepRoyalEmerald
            )
        }
    }
}

/**
 * Allocation legend badge for the Canvas Proportion Bar.
 */
@Composable
private fun AllocationBadge(
    color: Color,
    label: String,
    amount: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = SlateMuted, fontWeight = FontWeight.Medium)
            Text(text = amount, fontSize = 10.sp, color = CharcoalPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
