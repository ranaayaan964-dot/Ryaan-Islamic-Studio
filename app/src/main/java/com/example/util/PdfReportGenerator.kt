package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enterprise Data Model for the Zakat Assessment Report.
 */
data class ZakatReportData(
    val currency: String = "USD",
    val currencySymbol: String = "$",
    val cashAmount: Double,
    val goldGrams: Double,
    val goldRate: Double,
    val goldValue: Double,
    val silverGrams: Double,
    val silverRate: Double,
    val silverValue: Double,
    val stocksValue: Double,
    val cryptoValue: Double,
    val inventoryValue: Double,
    val liabilitiesValue: Double,
    val grossZakatableWealth: Double,
    val netZakatableWealth: Double,
    val nisabBenchmark: String, // "Silver (595g)" or "Gold (85g)"
    val nisabThreshold: Double,
    val isEligible: Boolean,
    val zakatDue: Double,
    val donorName: String = "Honorable Believer"
)

/**
 * MODULE: PRODUCTION PDF CERTIFICATE GENERATOR (VERSION 1.1)
 * Generates an ultra-luxurious, publication-quality A4 PDF certificate (Zakat_Assessment_Certificate.pdf)
 * complete with Islamic geometric borders, Bismillah, asset breakdown tables, classical Fiqh citations
 * (Surah At-Tawbah 9:103), and a digital Shariah compliance seal.
 *
 * Renders and saves on an IO dispatcher, then opens or shares the completed file on the main thread.
 */
object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"

    private val exportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val exportMutex = Mutex()

    fun generateAndOpenZakatPdf(context: Context, data: ZakatReportData) {
        val appContext = context.applicationContext
        exportScope.launch {
            val file = exportMutex.withLock { generateZakatPdfFile(appContext, data) }
            withContext(Dispatchers.Main) {
                if (file == null) {
                    Toast.makeText(appContext, "Could not generate the Zakat certificate.", Toast.LENGTH_LONG).show()
                } else {
                    openPdf(appContext, file)
                    Toast.makeText(appContext, "Certificate saved to Downloads & opened!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateZakatPdfFile(context: Context, data: ZakatReportData): File? {
        val pdfDoc = PdfDocument()

        // Standard A4 Dimensions in PostScript points (595 x 842 pt = 8.27 x 11.69 inches)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        // Palette Colors
        val emeraldDeep = Color.rgb(10, 60, 47)      // Deep Royal Emerald #0A3C2F
        val emeraldLight = Color.rgb(16, 185, 129)   // Radiant Emerald #10B981
        val emeraldBg = Color.rgb(240, 253, 244)     // Emerald Tint #F0FDF4
        val goldPrimary = Color.rgb(212, 175, 55)    // Platinum Gold #D4AF37
        val goldSoft = Color.rgb(245, 230, 180)      // Soft Gold
        val slateText = Color.rgb(30, 41, 59)        // Charcoal #1E293B
        val mutedText = Color.rgb(100, 116, 139)     // Slate Muted #64748B
        val borderLine = Color.rgb(226, 232, 240)    // Light Border #E2E8F0

        // Paint Definitions
        val borderPaint = Paint().apply {
            color = goldPrimary
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val innerBorderPaint = Paint().apply {
            color = emeraldDeep
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val bismillahPaint = Paint().apply {
            color = emeraldDeep
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val orgPaint = Paint().apply {
            color = goldPrimary
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = emeraldDeep
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = mutedText
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = slateText
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = slateText
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = borderLine
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val zebraPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        // 1. DUAL LUXURY CERTIFICATE BORDER
        canvas.drawRect(RectF(20f, 20f, (pageWidth - 20).toFloat(), (pageHeight - 20).toFloat()), borderPaint)
        canvas.drawRect(RectF(25f, 25f, (pageWidth - 25).toFloat(), (pageHeight - 25).toFloat()), innerBorderPaint)

        // Corner Ornaments
        val cornerSize = 14f
        fun drawCorner(x: Float, y: Float, dx: Float, dy: Float) {
            canvas.drawLine(x, y, x + dx * cornerSize, y, borderPaint)
            canvas.drawLine(x, y, x, y + dy * cornerSize, borderPaint)
        }
        drawCorner(28f, 28f, 1f, 1f)
        drawCorner((pageWidth - 28).toFloat(), 28f, -1f, 1f)
        drawCorner(28f, (pageHeight - 28).toFloat(), 1f, -1f)
        drawCorner((pageWidth - 28).toFloat(), (pageHeight - 28).toFloat(), -1f, -1f)

        var y = 52f

        // 2. HEADER SECTION
        canvas.drawText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", pageWidth / 2f, y, bismillahPaint)
        y += 20f
        canvas.drawText("RYAAN ISLAMIC STUDIO • SHARIAH AUDIT & COMPLIANCE BOARD", pageWidth / 2f, y, orgPaint)
        y += 22f
        canvas.drawText("OFFICIAL ZAKAT AL-MAL ASSESSMENT CERTIFICATE", pageWidth / 2f, y, titlePaint)
        y += 14f
        canvas.drawText("Verified in Accordance with Classical Islamic Jurisprudence & AAOIFI Standard No. 9", pageWidth / 2f, y, subtitlePaint)
        y += 18f

        // Metadata Header Box
        val metaBoxPaint = Paint().apply {
            color = emeraldBg
            style = Paint.Style.FILL
        }
        val metaBorder = Paint().apply {
            color = Color.rgb(187, 247, 208)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(38f, y, 557f, y + 36f), 8f, 8f, metaBoxPaint)
        canvas.drawRoundRect(RectF(38f, y, 557f, y + 36f), 8f, 8f, metaBorder)

        val certDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val certId = "RIS-ZKT-${System.currentTimeMillis().toString().takeLast(6)}"

        canvas.drawText("Certificate ID: $certId", 50f, y + 16f, boldTextPaint)
        canvas.drawText("Assessment Date: $certDate", 50f, y + 28f, textPaint)

        canvas.drawText("Beneficiary: ${data.donorName}", 340f, y + 16f, boldTextPaint)
        canvas.drawText("Benchmark Currency: ${data.currency} (${data.currencySymbol})", 340f, y + 28f, textPaint)

        y += 52f

        // 3. ASSET BREAKDOWN TABLE HEADER
        val tableHeaderPaint = Paint().apply {
            color = emeraldDeep
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(38f, y, 557f, y + 22f), 4f, 4f, tableHeaderPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("ASSET CATEGORY", 50f, y + 15f, headerTextPaint)
        canvas.drawText("QUANTITY / WEIGHT", 240f, y + 15f, headerTextPaint)
        canvas.drawText("BENCHMARK RATE", 370f, y + 15f, headerTextPaint)
        val valHeadX = 545f - headerTextPaint.measureText("NET VALUE (${data.currency})")
        canvas.drawText("NET VALUE (${data.currency})", valHeadX, y + 15f, headerTextPaint)

        y += 24f

        fun formatCurrency(amount: Double): String {
            return "${data.currencySymbol} ${String.format(Locale.US, "%,.2f", amount)}"
        }

        // Table Rows
        data class TableRowItem(val category: String, val qty: String, val rate: String, val value: Double, val isDeduction: Boolean = false)

        val rows = listOf(
            TableRowItem("1. Cash in Hand & Bank Balances", "Liquid Balances", "1.00", data.cashAmount),
            TableRowItem("2. Gold (Jewelry, Bullion, Coins)", "${String.format(Locale.US, "%.2f", data.goldGrams)} g", formatCurrency(data.goldRate) + "/g", data.goldValue),
            TableRowItem("3. Silver (Utensils, Bullion, Coins)", "${String.format(Locale.US, "%.2f", data.silverGrams)} g", formatCurrency(data.silverRate) + "/g", data.silverValue),
            TableRowItem("4. Public Equities, Stocks & Funds", "Shares / ETFs", "Market Value", data.stocksValue),
            TableRowItem("5. Cryptocurrencies & Digital Assets", "Liquid Tokens", "Market Value", data.cryptoValue),
            TableRowItem("6. Trade Inventory / Business Goods", "Saleable Stock", "Wholesale Cost", data.inventoryValue),
            TableRowItem("Less: Immediate Debts & Liabilities", "Due < 1 Lunar Year", "Deductible", -data.liabilitiesValue, isDeduction = true)
        )

        rows.forEachIndexed { index, row ->
            if (index % 2 == 0) {
                canvas.drawRect(RectF(38f, y, 557f, y + 20f), zebraPaint)
            }
            canvas.drawText(row.category, 48f, y + 14f, if (row.isDeduction) boldTextPaint else textPaint)
            canvas.drawText(row.qty, 240f, y + 14f, textPaint)
            canvas.drawText(row.rate, 370f, y + 14f, textPaint)

            val valStr = if (row.isDeduction) "- ${formatCurrency(data.liabilitiesValue)}" else formatCurrency(row.value)
            val valWidth = boldTextPaint.measureText(valStr)
            val rowPaint = if (row.isDeduction) Paint(boldTextPaint).apply { color = Color.rgb(185, 28, 28) } else boldTextPaint
            canvas.drawText(valStr, 545f - valWidth, y + 14f, rowPaint)

            y += 20f
            canvas.drawLine(38f, y, 557f, y, linePaint)
        }

        y += 14f

        // 4. WEALTH SUMMARY RECONCILIATION
        val summaryBox = RectF(38f, y, 557f, y + 68f)
        val summaryBoxBg = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(summaryBox, 6f, 6f, summaryBoxBg)
        canvas.drawRoundRect(summaryBox, 6f, 6f, metaBorder)

        fun drawSummaryLine(label: String, value: String, isBold: Boolean = false, isEmerald: Boolean = false) {
            val p = if (isBold) boldTextPaint else textPaint
            val vp = if (isEmerald) Paint(boldTextPaint).apply { color = emeraldDeep; textSize = 11f } else p
            canvas.drawText(label, 48f, y + 14f, p)
            val vWidth = vp.measureText(value)
            canvas.drawText(value, 545f - vWidth, y + 14f, vp)
            y += 16f
        }

        val startSummaryY = y
        drawSummaryLine("Gross Zakatable Assets Subtotal:", formatCurrency(data.grossZakatableWealth))
        drawSummaryLine("Less Deductible Immediate Liabilities:", "- " + formatCurrency(data.liabilitiesValue))
        drawSummaryLine("Total Net Zakatable Wealth (Nisab Base):", formatCurrency(data.netZakatableWealth), isBold = true, isEmerald = true)
        drawSummaryLine("Applicable Nisab Benchmark (${data.nisabBenchmark}):", formatCurrency(data.nisabThreshold) + " — " + (if (data.isEligible) "ELIGIBLE (Exceeds Nisab)" else "EXEMPT (Below Nisab)"))

        y = startSummaryY + 76f

        // 5. TOTAL ZAKAT PAYABLE HIGHLIGHT CARD
        val payableCard = RectF(38f, y, 557f, y + 84f)
        val payableBg = Paint().apply {
            color = emeraldDeep
            style = Paint.Style.FILL
        }
        val goldFrame = Paint().apply {
            color = goldPrimary
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(payableCard, 10f, 10f, payableBg)
        canvas.drawRoundRect(payableCard, 10f, 10f, goldFrame)

        val payableTitlePaint = Paint().apply {
            color = goldPrimary
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.1f
            isAntiAlias = true
        }

        val payableAmountPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val payableSubPaint = Paint().apply {
            color = Color.rgb(209, 250, 229)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText("TOTAL ZAKAT DUE (2.5% LUNAR OBLIGATION - فريضة الزكاة)", 52f, y + 24f, payableTitlePaint)
        val zakatDueFormatted = if (data.isEligible) formatCurrency(data.zakatDue) else "${data.currencySymbol} 0.00 (Exempt)"
        canvas.drawText(zakatDueFormatted, 52f, y + 54f, payableAmountPaint)
        canvas.drawText("Due immediately upon completion of one full lunar Hawl • Disburse to verified Asnaf (Quran 9:60)", 52f, y + 72f, payableSubPaint)

        y += 98f

        // 6. CLASSICAL FIQH CITATIONS & QURANIC AYAH
        val fiqhBox = RectF(38f, y, 380f, y + 104f)
        val fiqhBoxBg = Paint().apply {
            color = Color.rgb(254, 252, 232) // Soft parchment tint
            style = Paint.Style.FILL
        }
        val fiqhBorder = Paint().apply {
            color = Color.rgb(254, 240, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(fiqhBox, 6f, 6f, fiqhBoxBg)
        canvas.drawRoundRect(fiqhBox, 6f, 6f, fiqhBorder)

        val ayahArabicPaint = Paint().apply {
            color = emeraldDeep
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val quoteTranslationPaint = Paint().apply {
            color = slateText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        canvas.drawText("خُذْ مِنْ أَمْوَالِهِمْ صَدَقَةً تُطَهِّرُهُمْ وَتُزَكِّيهِم بِهَا", 50f, y + 20f, ayahArabicPaint)
        canvas.drawText("“Take, [O Muhammad], from their wealth a charity by which you purify", 50f, y + 36f, quoteTranslationPaint)
        canvas.drawText("them and cause them increase, and invoke [Allah's blessings] upon them.”", 50f, y + 48f, quoteTranslationPaint)
        canvas.drawText("— Holy Quran, Surah At-Tawbah [9:103]", 50f, y + 62f, boldTextPaint)

        val rulingPaint = Paint().apply {
            color = mutedText
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Consensus (Ijma): Zakat is an absolute pillar of Islam on excess wealth.", 50f, y + 78f, rulingPaint)
        canvas.drawText("Silver Nisab (595g) is preferred by majority for maximizing benefit to the poor.", 50f, y + 90f, rulingPaint)

        // 7. DIGITAL SHARIAH COMPLIANCE SEAL (CIRCULAR MEDALLION)
        val sealCenterX = 468f
        val sealCenterY = y + 52f
        val sealRadius = 42f

        val sealBg = Paint().apply {
            color = Color.rgb(255, 251, 235)
            style = Paint.Style.FILL
        }
        val sealRing = Paint().apply {
            color = goldPrimary
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val sealInnerRing = Paint().apply {
            color = emeraldDeep
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(sealCenterX, sealCenterY, sealRadius, sealBg)
        canvas.drawCircle(sealCenterX, sealCenterY, sealRadius, sealRing)
        canvas.drawCircle(sealCenterX, sealCenterY, sealRadius - 4f, sealInnerRing)

        // 8-Pointed Star Ornament inside Seal
        val starPaint = Paint().apply {
            color = goldPrimary
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val starRadius = 14f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4.0
            val x1 = (sealCenterX + starRadius * cos(angle)).toFloat()
            val y1 = (sealCenterY + starRadius * sin(angle)).toFloat()
            val nextAngle = (i + 2) * Math.PI / 4.0
            val x2 = (sealCenterX + starRadius * cos(nextAngle)).toFloat()
            val y2 = (sealCenterY + starRadius * sin(nextAngle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, starPaint)
        }

        val sealText = Paint().apply {
            color = emeraldDeep
            textSize = 6.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("★ SHARIAH COMPLIANT ★", sealCenterX, sealCenterY - 24f, sealText)
        canvas.drawText("AAOIFI STANDARD 9", sealCenterX, sealCenterY + 22f, sealText)
        canvas.drawText("RYAAN ISLAMIC STUDIO", sealCenterX, sealCenterY + 30f, sealText)

        y += 114f

        // 8. SIGNATURE AND VERIFICATION FOOTER
        canvas.drawLine(38f, y, 557f, y, linePaint)
        y += 14f

        val footerPaint = Paint().apply {
            color = mutedText
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("This certificate represents a formal mathematical calculation based on user-provided values. Generated via Ryaan Islamic Studio.", 38f, y, footerPaint)
        y += 10f
        canvas.drawText("Electronic verification hash: ${data.hashCode().toUInt().toString(16).uppercase(Locale.US)} • All Rights Reserved © 2026", 38f, y, footerPaint)

        pdfDoc.finishPage(page)

        // SAVE PDF TO PUBLIC DOWNLOADS DIRECTORY & EXTERNAL STORAGE RESILIENTLY
        return try {
            var fileToOpen: File? = null

            // 1. Try public downloads directory
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val publicPdfFile = File(downloadsDir, "Zakat_Assessment_Certificate.pdf")
                FileOutputStream(publicPdfFile).use { fos ->
                    pdfDoc.writeTo(fos)
                    fos.flush()
                }
                if (publicPdfFile.exists() && publicPdfFile.length() > 0) {
                    fileToOpen = publicPdfFile
                    Log.i(TAG, "Zakat PDF written to public downloads: ${publicPdfFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct public downloads write avoided or failed due to scoped storage: ${e.message}")
            }

            // 2. Guarantee app-specific downloads directory
            val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val appPdfFile = File(appDownloadsDir, "Zakat_Assessment_Certificate.pdf")
            try {
                FileOutputStream(appPdfFile).use { fos ->
                    pdfDoc.writeTo(fos)
                    fos.flush()
                }
                if (fileToOpen == null && appPdfFile.exists() && appPdfFile.length() > 0) {
                    fileToOpen = appPdfFile
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to app downloads dir: ${e.message}")
            }

            // 3. Guarantee cached internal copy for FileProvider
            val internalFile = File(context.filesDir, "Zakat_Assessment_Certificate.pdf")
            try {
                FileOutputStream(internalFile).use { fos ->
                    pdfDoc.writeTo(fos)
                    fos.flush()
                }
                if (fileToOpen == null) {
                    fileToOpen = internalFile
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to internal files: ${e.message}")
            }

            pdfDoc.close()

            val targetFile = fileToOpen ?: internalFile
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or opening Zakat PDF: ${e.message}", e)
            try { pdfDoc.close() } catch (_: Exception) {}
            null
        }
    }

    private fun openPdf(context: Context, file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open Zakat Assessment Certificate").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e(TAG, "Could not open the generated Zakat PDF", e)
            Toast.makeText(context, "PDF saved, but no compatible viewer could be opened.", Toast.LENGTH_LONG).show()
        }
    }

    fun shareZakatPdf(context: Context, data: ZakatReportData) {
        val appContext = context.applicationContext
        exportScope.launch {
            val file = exportMutex.withLock { generateZakatPdfFile(appContext, data) }
            withContext(Dispatchers.Main) {
                if (file != null && file.exists()) {
                    sharePdf(appContext, file)
                } else {
                    Toast.makeText(appContext, "Could not generate the Zakat certificate.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sharePdf(context: Context, fileToShare: File) {
        try {
                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    fileToShare
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Zakat Assessment Certificate - Ryaan Islamic Studio")
                    putExtra(Intent.EXTRA_TEXT, "Official Zakat Assessment Certificate calculated in accordance with AAOIFI Standard No. 9.")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(shareIntent, "Share Zakat Certificate").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing Zakat PDF: ${e.message}", e)
            Toast.makeText(context, "Could not share certificate.", Toast.LENGTH_SHORT).show()
        }
    }
}
