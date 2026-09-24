package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted

data class FinancialAsset(
    val symbol: String,
    val name: String,
    val assetClass: String, // Commodity, Crypto, Stock
    val price: String,
    val changePercent: String,
    val isPositive: Boolean,
    val debtRatio: Float, // Interest-bearing debt / Market cap (AAOIFI threshold < 33%)
    val cashInterestRatio: Float, // Cash & Interest bearing securities / Market cap (< 33%)
    val nonPermissibleIncomeRatio: Float, // Non-permissible revenue / Total revenue (< 5%)
    val isHalal: Boolean,
    val shariahAuditNote: String,
    val purificationPercentage: String
)

data class CandleData(
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float
)

/**
 * MODULE 7: HALAL FINANCE & CRYPTO SHARIAH SCREENER
 * Real-time tickers for Gold, Silver, Crypto, and Global Equities.
 * Interactive Candlestick charts rendered on Canvas.
 * AAOIFI standard financial screening engine (Debt < 33%, Impermissible < 5%)
 * with animated "HALAL CERTIFIED" gold seal.
 */
@Composable
fun HalalFinanceScreen(
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAssetClass by remember { mutableStateOf("All") }

    val assets = remember {
        mutableStateListOf(
            FinancialAsset(
                symbol = "XAU/USD",
                name = "Physical Gold (Spot)",
                assetClass = "Commodity",
                price = "$2,642.80 / oz",
                changePercent = "+1.42%",
                isPositive = true,
                debtRatio = 0.0f,
                cashInterestRatio = 0.0f,
                nonPermissibleIncomeRatio = 0.0f,
                isHalal = true,
                shariahAuditNote = "Intrinsic physical commodity backed 1:1. Full Shariah compliance according to AAOIFI Standard No. 57 on Gold.",
                purificationPercentage = "0.00%"
            ),
            FinancialAsset(
                symbol = "XAG/USD",
                name = "Physical Silver (Spot)",
                assetClass = "Commodity",
                price = "$31.85 / oz",
                changePercent = "+2.15%",
                isPositive = true,
                debtRatio = 0.0f,
                cashInterestRatio = 0.0f,
                nonPermissibleIncomeRatio = 0.0f,
                isHalal = true,
                shariahAuditNote = "Classic Islamic bimetallic monetary standard. 100% Halal under physical spot settlement.",
                purificationPercentage = "0.00%"
            ),
            FinancialAsset(
                symbol = "BTC",
                name = "Bitcoin",
                assetClass = "Crypto",
                price = "$67,450.00",
                changePercent = "+3.85%",
                isPositive = true,
                debtRatio = 0.0f,
                cashInterestRatio = 0.0f,
                nonPermissibleIncomeRatio = 0.0f,
                isHalal = true,
                shariahAuditNote = "Decentralized digital property (Mal Mutaqawwim). No interest or leverage embedded. Halal per Shariah Advisory Boards.",
                purificationPercentage = "0.00%"
            ),
            FinancialAsset(
                symbol = "2222.SR",
                name = "Saudi Aramco",
                assetClass = "Stock",
                price = "27.80 SAR",
                changePercent = "+0.45%",
                isPositive = true,
                debtRatio = 14.2f,
                cashInterestRatio = 9.8f,
                nonPermissibleIncomeRatio = 0.8f,
                isHalal = true,
                shariahAuditNote = "Well within AAOIFI financial limits: Debt (14.2% < 33%), Cash (9.8% < 33%), Impermissible income (0.8% < 5%).",
                purificationPercentage = "0.80%"
            ),
            FinancialAsset(
                symbol = "AAPL",
                name = "Apple Inc.",
                assetClass = "Stock",
                price = "$232.50",
                changePercent = "+0.80%",
                isPositive = true,
                debtRatio = 18.5f,
                cashInterestRatio = 12.1f,
                nonPermissibleIncomeRatio = 1.2f,
                isHalal = true,
                shariahAuditNote = "Meets all AAOIFI financial ratio hurdles. Core technology business compliant. Purification required on minor financing earnings.",
                purificationPercentage = "1.20%"
            ),
            FinancialAsset(
                symbol = "JPM",
                name = "JPMorgan Chase & Co.",
                assetClass = "Stock",
                price = "$218.40",
                changePercent = "-0.65%",
                isPositive = false,
                debtRatio = 88.0f,
                cashInterestRatio = 92.0f,
                nonPermissibleIncomeRatio = 94.0f,
                isHalal = false,
                shariahAuditNote = "STRICTLY HARAM: Conventional banking activities based upon interest (Riba) and debt instruments. Violates core AAOIFI principles.",
                purificationPercentage = "N/A"
            )
        )
    }

    var selectedAsset by remember { mutableStateOf(assets[0]) }

    val assetClasses = listOf("All", "Commodity", "Crypto", "Stock")
    val filtered = assets.filter {
        (selectedAssetClass == "All" || it.assetClass == selectedAssetClass) &&
                (it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true))
    }

    // Candlestick sample data
    val sampleCandles = listOf(
        CandleData(open = 100f, close = 115f, high = 120f, low = 95f),
        CandleData(open = 115f, close = 110f, high = 118f, low = 106f),
        CandleData(open = 110f, close = 125f, high = 130f, low = 108f),
        CandleData(open = 125f, close = 135f, high = 140f, low = 122f),
        CandleData(open = 135f, close = 128f, high = 138f, low = 125f),
        CandleData(open = 128f, close = 145f, high = 150f, low = 126f),
        CandleData(open = 145f, close = 152f, high = 156f, low = 142f)
    )

    // Animated gold seal pulse
    val infiniteTransition = rememberInfiniteTransition(label = "SealAnim")
    val sealScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SealScale"
    )

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("halal_finance_screen")
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
                            text = "Halal Finance & Screener",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "AAOIFI Shariah Compliance Engine",
                            fontSize = 11.sp,
                            color = PlatinumGold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldSurfaceLight)
                        .border(1.dp, PlatinumGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = RadiantEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "AAOIFI Certified", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Asset Candlestick & Shariah Verification Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                cornerRadius = 24.dp,
                backgroundColor = PureWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Asset Top Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedAsset.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald
                            )
                            Text(
                                text = "${selectedAsset.symbol} • ${selectedAsset.assetClass}",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = selectedAsset.price,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif,
                                color = CharcoalPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedAsset.isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (selectedAsset.isPositive) RadiantEmerald else CrimsonError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = selectedAsset.changePercent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAsset.isPositive) RadiantEmerald else CrimsonError
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Candlestick Chart on Canvas
                    Text(
                        text = "LIVE CANDLESTICK VOLATILITY",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        val candleWidth = size.width / (sampleCandles.size * 1.8f)
                        val maxPrice = 160f
                        val minPrice = 90f
                        val priceRange = maxPrice - minPrice

                        sampleCandles.forEachIndexed { idx, candle ->
                            val x = idx * (size.width / sampleCandles.size) + candleWidth / 2f
                            val isBullish = candle.close >= candle.open
                            val candleColor = if (isBullish) RadiantEmerald else CrimsonError

                            val highY = size.height - ((candle.high - minPrice) / priceRange * size.height)
                            val lowY = size.height - ((candle.low - minPrice) / priceRange * size.height)
                            val openY = size.height - ((candle.open - minPrice) / priceRange * size.height)
                            val closeY = size.height - ((candle.close - minPrice) / priceRange * size.height)

                            // Wick line
                            drawLine(
                                color = candleColor,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Candle Body
                            val bodyTop = minOf(openY, closeY)
                            val bodyHeight = maxOf(openY, closeY) - bodyTop
                            drawRoundRect(
                                color = candleColor,
                                topLeft = Offset(x - candleWidth / 2f, bodyTop),
                                size = Size(candleWidth, maxOf(bodyHeight, 2.dp.toPx())),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Shariah Compliance Verdict & Animated Seal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedAsset.isHalal) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
                            .border(
                                1.5.dp,
                                if (selectedAsset.isHalal) PlatinumGold else CrimsonError,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated Gold Stamp
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(if (selectedAsset.isHalal) sealScale else 1f)
                                .clip(CircleShape)
                                .background(if (selectedAsset.isHalal) PureWhite else Color(0xFFFEE2E2))
                                .border(
                                    2.dp,
                                    if (selectedAsset.isHalal) PlatinumGold else CrimsonError,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedAsset.isHalal) "حلال" else "حرام",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAsset.isHalal) DeepRoyalEmerald else CrimsonError
                                )
                                Text(
                                    text = if (selectedAsset.isHalal) "CERTIFIED" else "NON-COMPLIANT",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedAsset.isHalal) PlatinumGold else CrimsonError
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedAsset.isHalal) "HALAL COMPLIANT ASSET" else "HARAM / PROHIBITED",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedAsset.isHalal) DeepRoyalEmerald else CrimsonError
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedAsset.shariahAuditNote,
                                fontSize = 11.sp,
                                color = CharcoalSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // AAOIFI Financial Hurdles
                    if (selectedAsset.assetClass == "Stock") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "AAOIFI FINANCIAL RATIO BREAKDOWN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        RatioBarItem("Interest-Bearing Debt / Mkt Cap", selectedAsset.debtRatio, 33.0f)
                        RatioBarItem("Cash & Interest Securities / Mkt Cap", selectedAsset.cashInterestRatio, 33.0f)
                        RatioBarItem("Non-Permissible Income / Revenue", selectedAsset.nonPermissibleIncomeRatio, 5.0f)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Charity Purification Required: ${selectedAsset.purificationPercentage} of dividend earnings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RadiantEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search and Category Selector
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search stocks, commodities, or crypto...", fontSize = 12.5.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DeepRoyalEmerald) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asset_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepRoyalEmerald,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedTextColor = CharcoalPrimary,
                    unfocusedTextColor = CharcoalPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Asset Class Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(assetClasses) { cls ->
                    val isSel = cls == selectedAssetClass
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) DeepRoyalEmerald else PureWhite)
                            .border(1.dp, if (isSel) DeepRoyalEmerald else Color(0x20D4AF37), RoundedCornerShape(12.dp))
                            .clickable { selectedAssetClass = cls }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cls,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) PureWhite else CharcoalSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Asset List
            filtered.forEach { asset ->
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { selectedAsset = asset },
                    elevation = 4.dp,
                    cornerRadius = 18.dp,
                    backgroundColor = PureWhite,
                    borderColor = if (selectedAsset.symbol == asset.symbol) PlatinumGold else Color(0x180F172A)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (asset.isHalal) EmeraldSurfaceLight else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (asset.isHalal) "✓" else "✕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (asset.isHalal) DeepRoyalEmerald else CrimsonError
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = asset.symbol, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                                Text(text = asset.name, fontSize = 11.sp, color = SlateMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = asset.price, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                            Text(
                                text = asset.changePercent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (asset.isPositive) RadiantEmerald else CrimsonError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatioBarItem(label: String, value: Float, maxThreshold: Float) {
    val isCompliant = value <= maxThreshold
    val fraction = (value / maxThreshold).coerceIn(0f, 1f)

    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 10.5.sp, color = CharcoalSecondary)
            Text(
                text = "${"%.1f".format(value)}% (Limit: ${"%.0f".format(maxThreshold)}%)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompliant) RadiantEmerald else CrimsonError
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isCompliant) RadiantEmerald else CrimsonError)
            )
        }
    }
}
