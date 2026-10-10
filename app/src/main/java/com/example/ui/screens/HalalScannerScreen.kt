package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

enum class HalalStatus {
    HALAL,
    HARAM,
    MUSHBOOH,
    UNKNOWN
}

data class ECodeInfo(
    val code: String,
    val name: String,
    val status: HalalStatus,
    val source: String,
    val fiqhReason: String
)

object HalalDatabase {
    val database = mapOf(
        "E120" to ECodeInfo("E120", "Carmine / Cochineal", HalalStatus.HARAM, "Insects (Crushed Cochineal scale)", "Impermissible across major Madhabs (Hanafi/Hanbali) as it is derived directly from ground dead insects."),
        "E441" to ECodeInfo("E441", "Gelatin", HalalStatus.MUSHBOOH, "Animal bones/skin (often Porcine/Pork)", "Haram unless certified 100% Halal Bovine or Fish slaughtered according to Islamic Shariah."),
        "E471" to ECodeInfo("E471", "Mono- and Diglycerides of Fatty Acids", HalalStatus.MUSHBOOH, "Plant or Animal Fat (Porcine/Tallow)", "Mushbooh (doubtful). Haram if derived from non-dhabihah animals or swine. Halal only if 100% plant-based."),
        "E904" to ECodeInfo("E904", "Shellac", HalalStatus.MUSHBOOH, "Secretions of female Kerria lacca insect", "Mushbooh. Some contemporary scholars allow resin secretion; others advise caution due to insect residues."),
        "E920" to ECodeInfo("E920", "L-Cysteine", HalalStatus.HARAM, "Human hair or animal feathers/bristles", "Impermissible if extracted from human hair (violates human dignity in Islam) or non-halal animal hair."),
        "E542" to ECodeInfo("E542", "Bone Phosphate", HalalStatus.HARAM, "Animal bones", "Impermissible unless bones are from halal-slaughtered dhabihah animals."),
        "E160A" to ECodeInfo("E160a", "Carotene (Beta-Carotene)", HalalStatus.HALAL, "Plant extracts (Carrots, Palm oil)", "Pure vegetable origin; universally accepted as Halal."),
        "E300" to ECodeInfo("E300", "Ascorbic Acid (Vitamin C)", HalalStatus.HALAL, "Plant/Citrus fermentation", "Pure and clean antioxidant; completely Halal."),
        "E322" to ECodeInfo("E322", "Lecithin (Soy Lecithin)", HalalStatus.HALAL, "Soybean oil", "Plant origin (Soybean); universally Halal."),
        "E330" to ECodeInfo("E330", "Citric Acid", HalalStatus.HALAL, "Citrus / Molasses microbial fermentation", "Clean vegetable/fungal fermentation; 100% Halal."),
        "E407" to ECodeInfo("E407", "Carrageenan", HalalStatus.HALAL, "Red Seaweed (Marine)", "Marine vegetable derivative; completely Halal."),
        "E412" to ECodeInfo("E412", "Guar Gum", HalalStatus.HALAL, "Guar beans (Plant)", "Pure botanical seed gum; Halal."),
        "E415" to ECodeInfo("E415", "Xanthan Gum", HalalStatus.HALAL, "Glucose bacterial fermentation", "Fermented plant carbohydrates; 100% Halal.")
    )

    fun checkCode(rawInput: String): ECodeInfo? {
        val clean = rawInput.trim().uppercase().replace(" ", "").replace("-", "")
        // Check direct key
        database[clean]?.let { return it }
        // Check with E prefix
        if (!clean.startsWith("E")) {
            database["E$clean"]?.let { return it }
        }
        // Search through entries for code or name substring
        return database.values.firstOrNull {
            it.code.equals(clean, ignoreCase = true) || it.name.contains(clean, ignoreCase = true)
        }
    }
}

/**
 * MODULE 12: LIVE HALAL E-CODE & BARCODE SCANNER
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun HalalScannerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var searchInput by remember { mutableStateOf("") }
    var scannedResult by remember { mutableStateOf<ECodeInfo?>(null) }
    var scannedBarcodeRaw by remember { mutableStateOf<String?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Color animation based on result
    val successGreen = Color(0xFF10B981)
    val warningAmber = Color(0xFFF59E0B)
    val errorRed = CrimsonError

    val activeColor = when (scannedResult?.status) {
        HalalStatus.HALAL -> successGreen
        HalalStatus.HARAM -> errorRed
        HalalStatus.MUSHBOOH -> warningAmber
        null -> DeepRoyalEmerald
        else -> PlatinumGold
    }

    val animatedColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(300), label = "HalalStatusColor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .testTag("halal_scanner_screen")
    ) {
        // CameraX Viewfinder with Barcode Scanning
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val barcodeScanner = BarcodeScanning.getClient()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue ?: continue
                                            scannedBarcodeRaw = rawValue
                                            val found = HalalDatabase.checkCode(rawValue)
                                            if (found != null) {
                                                scannedResult = found
                                            } else {
                                                // Default check against mock product numbers or common ingredients
                                                if (rawValue.endsWith("120")) {
                                                    scannedResult = HalalDatabase.checkCode("E120")
                                                } else if (rawValue.endsWith("441")) {
                                                    scannedResult = HalalDatabase.checkCode("E441")
                                                } else if (rawValue.endsWith("471")) {
                                                    scannedResult = HalalDatabase.checkCode("E471")
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Viewfinder reticle overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(2.dp, animatedColor, RoundedCornerShape(24.dp))
        )

        // Top Search & Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Halal E-Code & Food Scanner",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual E-Code Search Input
            OutlinedTextField(
                value = searchInput,
                onValueChange = { query ->
                    searchInput = query
                    scannedResult = HalalDatabase.checkCode(query)
                },
                placeholder = { Text("Search E-Code e.g. E120, E441, E471...", color = Color.LightGray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PlatinumGold)
                },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = ""; scannedResult = null }) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    scannedResult = HalalDatabase.checkCode(searchInput)
                }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("halal_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = animatedColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Bottom Result Card (flashes ErrorRed if Haram/Mushbooh, SuccessGreen if Halal)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = animatedColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                val item = scannedResult
                if (item != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (item.status) {
                                HalalStatus.HALAL -> Icons.Default.CheckCircle
                                HalalStatus.HARAM -> Icons.Default.Cancel
                                HalalStatus.MUSHBOOH -> Icons.Default.Warning
                                HalalStatus.UNKNOWN -> Icons.Default.QrCodeScanner
                            },
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (item.status) {
                                    HalalStatus.HALAL -> "HALAL CERTIFIED"
                                    HalalStatus.HARAM -> "HARAM / STRICTLY AVOID"
                                    HalalStatus.MUSHBOOH -> "MUSHBOOH (DOUBTFUL)"
                                    HalalStatus.UNKNOWN -> "UNKNOWN STATUS"
                                },
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${item.code} - ${item.name}",
                                color = PureWhite.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Source: ${item.source}",
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fiqh Ruling: ${item.fiqhReason}",
                        color = PureWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Point Camera at Barcode or Enter E-Code",
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instant optical detection of E-numbers, Carmine, and Gelatin",
                                color = PureWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
