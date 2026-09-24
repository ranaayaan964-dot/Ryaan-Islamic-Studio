package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ZakatReportData(
    val cashAmount: Double,
    val goldGrams: Double,
    val goldValue: Double,
    val silverGrams: Double,
    val silverValue: Double,
    val businessInventory: Double,
    val liabilities: Double,
    val netZakatWealth: Double,
    val nisabThreshold: Double,
    val isEligible: Boolean,
    val totalZakatPayable: Double,
    val currency: String = "USD"
)

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"

    fun generateAndOpenZakatPdf(context: Context, data: ZakatReportData) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(6, 78, 59) // Deep Emerald
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(212, 175, 55) // Gold
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val highlightPaint = Paint().apply {
            color = Color.rgb(240, 253, 244) // Light emerald fill
            style = Paint.Style.FILL
        }

        var y = 60f

        // Header
        canvas.drawText("RYAAN ISLAMIC STUDIO", 40f, y, subtitlePaint)
        y += 28f
        canvas.drawText("Official Zakat Al-Mal Assessment Report", 40f, y, titlePaint)
        y += 18f

        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a", Locale.getDefault())
        canvas.drawText("Assessment Date: ${dateFormat.format(Date())}", 40f, y, textPaint)
        y += 20f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        // Section: Wealth Breakdown
        canvas.drawText("ASSET & WEALTH BREAKDOWN (SUBJECT TO ZAKAT)", 40f, y, boldTextPaint)
        y += 20f

        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 50f, y, textPaint)
            val textWidth = boldTextPaint.measureText(value)
            canvas.drawText(value, 545f - textWidth, y, boldTextPaint)
            y += 18f
        }

        drawRow("Liquid Cash & Bank Balances:", "$${String.format(Locale.US, "%,.2f", data.cashAmount)}")
        drawRow("Gold Holdings (${String.format(Locale.US, "%.1f", data.goldGrams)}g @ Nisab 85g):", "$${String.format(Locale.US, "%,.2f", data.goldValue)}")
        drawRow("Silver Holdings (${String.format(Locale.US, "%.1f", data.silverGrams)}g @ Nisab 595g):", "$${String.format(Locale.US, "%,.2f", data.silverValue)}")
        drawRow("Business Merchandise / Inventory:", "$${String.format(Locale.US, "%,.2f", data.businessInventory)}")
        drawRow("Less: Short-Term Debts / Liabilities:", "-$${String.format(Locale.US, "%,.2f", data.liabilities)}")

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Net Zakatable Wealth
        drawRow("TOTAL NET ZAKATABLE WEALTH:", "$${String.format(Locale.US, "%,.2f", data.netZakatWealth)}")
        drawRow("NISAB THRESHOLD (Silver Benchmark):", "$${String.format(Locale.US, "%,.2f", data.nisabThreshold)}")
        drawRow("NISAB STATUS:", if (data.isEligible) "ELIGIBLE (Exceeds Nisab threshold)" else "BELOW NISAB (Exempt from Zakat)")

        y += 20f

        // Final Payable Banner
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 16f, 16f, highlightPaint)
        val bannerTitle = Paint().apply {
            color = Color.rgb(6, 78, 59)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bannerValue = Paint().apply {
            color = Color.rgb(6, 78, 59)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawText("TOTAL ZAKAT DUE (2.5% LUNAR OBLIGATION)", 60f, y + 30f, bannerTitle)
        val zakatDueStr = "$${String.format(Locale.US, "%,.2f", data.totalZakatPayable)} ${data.currency}"
        canvas.drawText(zakatDueStr, 60f, y + 64f, bannerValue)
        y += 110f

        // Fiqh note
        val fiqhPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        canvas.drawText("Note: Zakat is an obligatory pillar of Islam payable on net wealth held for one full lunar year (Hawl).", 40f, y, fiqhPaint)
        y += 14f
        canvas.drawText("Calculated in strict compliance with the classical consensus (Ijma) across the 4 Sunni Madhabs.", 40f, y, fiqhPaint)

        pdfDocument.finishPage(page)

        // Write to file in cacheDir or getExternalFilesDir
        val fileDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val pdfFile = File(fileDir, "Zakat_Assessment_Report_${System.currentTimeMillis()}.pdf")

        try {
            FileOutputStream(pdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            Log.d(TAG, "Zakat PDF report successfully saved: ${pdfFile.absolutePath}")

            // Trigger Intent.ACTION_VIEW using FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(intent, "Open Official Zakat Report")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or opening Zakat PDF report", e)
            pdfDocument.close()
        }
    }
}
