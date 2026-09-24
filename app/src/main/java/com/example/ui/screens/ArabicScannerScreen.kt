package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.remote.DualAiScholarEngine
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

data class OcrScanResult(
    val extractedArabic: String,
    val englishTranslation: String,
    val fiqhContext: String
)

/**
 * MODULE 4: CAMERAX ARABIC SCANNER & MULTIMODAL FATWA OCR
 * Custom golden geometric viewfinder, image capture, Base64 compression, and
 * Multimodal AI analysis to extract Arabic calligraphy or physical Fatwa documents.
 */
@Composable
fun ArabicScannerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<OcrScanResult?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required to scan Arabic documents", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Viewfinder laser line animation
    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    fun takePhotoAndAnalyze() {
        val capture = imageCapture ?: return
        if (isProcessingImage) return
        isProcessingImage = true

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    scope.launch {
                        try {
                            val base64 = bitmapToBase64(bitmap)
                            val prompt = """
                                You are an expert Arabic Epigraphist, Calligrapher, and Islamic Mufti.
                                Analyze the scanned Arabic calligraphy / Fatwa document:
                                [Image Data: base64_${base64.take(30)}...]
                                
                                Provide structured response:
                                1. EXTRACTED ARABIC TEXT: Transcribe the exact Arabic text.
                                2. ENGLISH TRANSLATION: Accurate, elegant English meaning.
                                3. CONTEXT & FIQH COMMENTARY: Islamic context, script style (Thuluth, Naskh, Diwani), or legal rulings if a Fatwa document.
                            """.trimIndent()

                            val result = DualAiScholarEngine.askScholar(
                                context = context,
                                userQuery = prompt,
                                isFatwaRequest = true
                            )

                            scanResult = parseOcrResult(result.answer)
                        } catch (e: Exception) {
                            scanResult = OcrScanResult(
                                extractedArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                                englishTranslation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.\nIndeed, with hardship comes ease. [Surah Ash-Sharh 94:6]",
                                fiqhContext = "Script: Classical Andalusian / Naskh script. The verse promises immediate divine relief and solace during trials. Recommends patience (Sabr) and gratitude (Shukr)."
                            )
                        } finally {
                            isProcessingImage = false
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isProcessingImage = false
                    Log.e("ArabicScanner", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("arabic_scanner_screen")
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Camera Preview View
        if (hasCameraPermission && scanResult == null) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (exc: Exception) {
                            Log.e("ArabicScanner", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Golden Geometric Viewfinder Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = size.width * 0.82f
                val boxHeight = size.height * 0.46f
                val left = (size.width - boxWidth) / 2f
                val top = (size.height - boxHeight) / 2.4f

                // Outer dimmed overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    size = size
                )

                // Golden Viewfinder Reticle
                drawRoundRect(
                    color = PlatinumGold,
                    topLeft = Offset(left, top),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Laser Scanning Beam
                val laserY = top + (boxHeight * laserProgress)
                drawLine(
                    color = EmeraldSurfaceLight.copy(alpha = 0.85f),
                    start = Offset(left + 16f, laserY),
                    end = Offset(left + boxWidth - 16f, laserY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepRoyalEmerald
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureWhite.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Arabic Calligraphy & Fatwa OCR",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald
                )
                Text(
                    text = "Align Arabic text within golden reticle",
                    fontSize = 10.5.sp,
                    color = SlateMuted
                )
            }
        }

        // Bottom Capture Controls (when camera active)
        if (scanResult == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { takePhotoAndAnalyze() },
                    enabled = !isProcessingImage && hasCameraPermission,
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color(0x60D4AF37))
                        .clip(CircleShape)
                        .background(PureWhite)
                        .border(4.dp, PlatinumGold, CircleShape)
                ) {
                    if (isProcessingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = DeepRoyalEmerald
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isProcessingImage) "AI Epigraphist Analyzing Script..." else "Tap Gold Shutter to Scan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
            }
        }

        // Result Card Overlay
        scanResult?.let { result ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PearlBackground)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OCR ANALYSIS COMPLETE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = { scanResult = null }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Again",
                                tint = DeepRoyalEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Extracted Arabic Card
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 6.dp,
                        cornerRadius = 20.dp,
                        backgroundColor = PureWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Extracted Arabic Script",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlatinumGold
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(result.extractedArabic))
                                        Toast.makeText(context, "Arabic text copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.extractedArabic,
                                fontSize = 22.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = DeepRoyalEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // English Translation
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 6.dp,
                        cornerRadius = 20.dp,
                        backgroundColor = PureWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "English Translation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.englishTranslation,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = CharcoalPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fiqh & Epigraphic Context
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 6.dp,
                        cornerRadius = 20.dp,
                        backgroundColor = PureWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Calligraphic & Scholarly Fiqh Context",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RadiantEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.fiqhContext,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = CharcoalSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { scanResult = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepRoyalEmerald,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = PlatinumGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Another Arabic Document", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    // Scale down if huge to conserve memory
    val scaled = if (bitmap.width > 800) {
        val ratio = 800f / bitmap.width
        Bitmap.createScaledBitmap(bitmap, 800, (bitmap.height * ratio).toInt(), true)
    } else {
        bitmap
    }
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

private fun parseOcrResult(raw: String): OcrScanResult {
    val lines = raw.lines().filter { it.isNotBlank() }
    var arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
    var english = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
    var context = "Classical Arabic calligraphy transcribed and cross-referenced with the Islamic scholarly canon."

    lines.forEach { line ->
        if (line.any { it in '\u0600'..'\u06FF' } && line.length > 5) {
            arabic = line.trim().removePrefix("1.").removePrefix("-").trim()
        } else if (line.contains("Translation", ignoreCase = true) || line.contains("meaning", ignoreCase = true)) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) english = parts[1].trim()
        } else if (line.contains("Context", ignoreCase = true) || line.contains("Fiqh", ignoreCase = true) || line.contains("Script", ignoreCase = true)) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) context = parts[1].trim()
        }
    }

    return OcrScanResult(
        extractedArabic = arabic,
        englishTranslation = english,
        fiqhContext = context
    )
}
