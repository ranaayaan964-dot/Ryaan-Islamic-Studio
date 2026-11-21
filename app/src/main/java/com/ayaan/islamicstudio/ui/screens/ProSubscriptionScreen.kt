package com.ayaan.islamicstudio.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayaan.islamicstudio.BuildConfig
import com.ayaan.islamicstudio.data.database.ProPlanTier
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.ayaan.islamicstudio.ui.components.ads.UnityRewardedManager
import com.google.firebase.auth.FirebaseAuth
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private fun Context.unwrapActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun ProSubscriptionScreen(
    onNavigateBack: () -> Unit,
    onContinueWithFree: () -> Unit = onNavigateBack,
    isPostLoginStep: Boolean = false,
    onProActivated: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val repository = remember(context) { ProSubscriptionRepository.getInstance(context) }
    val status by repository.proStatusFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val webViewState = remember { androidx.compose.runtime.mutableStateOf<WebView?>(null) }

    BackHandler {
        if (isPostLoginStep) onContinueWithFree() else onNavigateBack()
    }

    LaunchedEffect(repository) {
        UnityRewardedManager.preloadRewarded(context, "BP_Rewarded_Android")
        try {
            repository.refreshCurrentProStatus()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            Toast.makeText(
                context,
                error.localizedMessage ?: "Pro status could not be refreshed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewState.value?.apply {
                removeJavascriptInterface("AyaanProBridge")
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewState.value = null
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        factory = { viewContext ->
            WebView(viewContext).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#041B15"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = request.url.host != "ayaan.local"
                }
                addJavascriptInterface(
                    AyaanProBridge(
                        context = viewContext,
                        repository = repository,
                        scope = scope,
                        webView = this,
                        isPostLoginStep = isPostLoginStep,
                        onNavigateBack = onNavigateBack,
                        onContinueWithFree = onContinueWithFree,
                        onProActivated = onProActivated
                    ),
                    "AyaanProBridge"
                )
                val html = viewContext.assets.open("web/pro_portal.html")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                webViewState.value = this
                loadDataWithBaseURL("https://ayaan.local/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webViewState.value = webView
            val active = status.isProActive
            webView.post {
                webView.evaluateJavascript(
                    "window.renderProPortalState(${buildProStateJson(context, repository, isPostLoginStep, active)})",
                    null
                )
            }
        }
    )
}

class AyaanProBridge(
    private val context: Context,
    private val repository: ProSubscriptionRepository,
    private val scope: CoroutineScope,
    private val webView: WebView,
    private val isPostLoginStep: Boolean,
    private val onNavigateBack: () -> Unit,
    private val onContinueWithFree: () -> Unit,
    private val onProActivated: () -> Unit
) {
    @JavascriptInterface
    fun goBack() {
        webView.post {
            if (isPostLoginStep) onContinueWithFree() else onNavigateBack()
        }
    }

    @JavascriptInterface
    fun continueWithFree() {
        webView.post(onContinueWithFree)
    }

    @JavascriptInterface
    fun redeemProKey(rawKey: String) {
        val key = rawKey.trim().uppercase(Locale.ROOT)
        if (key.isBlank()) {
            dispatchRedeemResult(false, "Enter a Pro key to activate your membership.", false)
            return
        }
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) { repository.redeemProKey(key) }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                dispatchRedeemResult(
                    success = false,
                    message = error.localizedMessage ?: "The Pro key could not be activated.",
                    activated = false
                )
                return@launch
            }
            if (result.isSuccess) {
                try {
                    withContext(Dispatchers.IO) { repository.refreshCurrentProStatus() }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    dispatchRedeemResult(
                        success = true,
                        message = "Key activated, but account status could not be refreshed: " +
                            (error.localizedMessage ?: "please reopen the subscription screen."),
                        activated = true
                    )
                    return@launch
                }
            }
            dispatchRedeemResult(
                success = result.isSuccess,
                message = result.fold(
                    onSuccess = { "${it.aiNote}\nAccess is active for ${it.durationDays} days.".trim() },
                    onFailure = { it.localizedMessage ?: "The Pro key could not be activated." }
                ),
                activated = result.isSuccess
            )
        }
    }

    @JavascriptInterface
    fun watchRewardedAd() {
        webView.post {
            val activity = context.unwrapActivity()
            if (activity == null) {
                dispatchError("Rewarded video is unavailable on this screen.")
                return@post
            }
            UnityRewardedManager.showRewardedAd(
                activity = activity,
                placementId = "BP_Rewarded_Android",
                onRewardEarned = {
                    repository.grantRewardedAdBonus("ai_scholar", 2)
                    Toast.makeText(context, "Alhamdulillah! +2 Bonus Uses Unlocked!", Toast.LENGTH_SHORT).show()
                    refreshPortalState()
                },
                onAdDismissedOrFailed = { message ->
                    if (!message.isNullOrBlank()) dispatchError(message)
                }
            )
        }
    }

    @JavascriptInterface
    fun emailToBuy(tierId: String) {
        val tier = ProPlanTier.values().firstOrNull { it.tierId == tierId }
        if (tier == null) {
            dispatchError("Choose a valid Pro plan before contacting support.")
            return
        }
        webView.post {
            val intent = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:${BuildConfig.PRO_SUPPORT_EMAIL}")
            ).putExtra(Intent.EXTRA_SUBJECT, "Buy Ayaan Islamic Studio ${tier.displayName}")
                .putExtra(
                    Intent.EXTRA_TEXT,
                    "Assalamu Alaikum,\n\nI would like to purchase the ${tier.displayName} (${tier.priceFormatted}, ${tier.durationDays} days)."
                )
            try {
                context.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                dispatchError("No email app is available on this device.")
            }
        }
    }

    @JavascriptInterface
    fun copyText(value: String) {
        webView.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Ayaan Islamic Studio", value))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun requestStateRefresh() {
        refreshPortalState()
    }

    private fun refreshPortalState() {
        val json = buildProStateJson(context, repository, isPostLoginStep)
        webView.post {
            webView.evaluateJavascript("window.renderProPortalState($json)", null)
        }
    }

    private fun dispatchRedeemResult(success: Boolean, message: String, activated: Boolean) {
        val messageJson = JSONObject.quote(message)
        val stateJson = buildProStateJson(context, repository, isPostLoginStep)
        webView.post {
            webView.evaluateJavascript(
                "window.onProKeyRedeemResult($success,$messageJson,$stateJson)",
                null
            )
            if (activated) onProActivated()
        }
    }

    private fun dispatchError(message: String) {
        webView.post {
            webView.evaluateJavascript("window.onProBridgeError(${JSONObject.quote(message)})", null)
        }
    }
}

private fun buildProStateJson(
    context: Context,
    repository: ProSubscriptionRepository,
    isPostLoginStep: Boolean,
    forceProActive: Boolean = repository.proStatusFlow.value.isProActive
): String {
    val status = repository.proStatusFlow.value
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val aiScholarUsed = if (forceProActive) 0 else 5 - repository.remainingUses("ai_scholar", 5)
    val hifzUsed = if (forceProActive) 0 else 3 - repository.remainingUses("ai_hifz", 3)
    val scanUsed = if (forceProActive) 0 else 2 - repository.remainingUses("scan_dream", 2)
    val expiry = status.proExpiresAt.takeIf { it > 0L }?.let {
        DateFormat.getDateInstance(DateFormat.LONG).format(Date(it))
    }.orEmpty()
    return JSONObject()
        .put("isProActive", forceProActive)
        .put("activePlanName", status.activePlanName)
        .put("remainingDays", status.remainingDays)
        .put("expiresAtFormatted", expiry)
        .put("activatedKey", status.activatedKey)
        .put("supportEmail", BuildConfig.PRO_SUPPORT_EMAIL)
        .put("aiScholarUsed", aiScholarUsed.coerceAtLeast(0))
        .put("hifzUsed", hifzUsed.coerceAtLeast(0))
        .put("scanUsed", scanUsed.coerceAtLeast(0))
        .put("isPostLoginStep", isPostLoginStep)
        .put("userEmail", firebaseUser?.email.orEmpty())
        .put("userName", firebaseUser?.displayName.orEmpty())
        .put("packageName", context.packageName)
        .toString()
}
