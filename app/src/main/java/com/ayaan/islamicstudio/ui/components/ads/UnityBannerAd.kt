package com.ayaan.islamicstudio.ui.components.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

private const val PRIMARY_BANNER_PLACEMENT = "BP_Banner_Android"
private const val FALLBACK_BANNER_PLACEMENT = "Banner_Android"
private const val TAG = "UnityBannerAd"

private fun Context.unwrapActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext ?: return null
    }
    return current as? Activity
}

@Composable
fun UnityBannerAd(
    modifier: Modifier = Modifier,
    placementId: String = PRIMARY_BANNER_PLACEMENT
) {
    val context = LocalContext.current
    val activity = context.unwrapActivity()
    val proStatus by ProSubscriptionRepository.getInstance(context).proStatusFlow
        .collectAsStateWithLifecycle()
    val adsInitialized by UnityAdsController.isInitialized.collectAsStateWithLifecycle()
    var activePlacement by remember(placementId) { mutableStateOf(placementId) }
    var triedFallback by remember(placementId) { mutableStateOf(false) }

    if (!proStatus.isProActive && adsInitialized && activity != null &&
        !activity.isFinishing && !activity.isDestroyed
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .border(1.dp, Color(0xFFDDBA63).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            key(activePlacement) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = {
                        BannerView(activity, activePlacement, UnityBannerSize(320, 50)).apply {
                            listener = object : BannerView.IListener {
                                override fun onBannerLoaded(bannerView: BannerView) {
                                    Log.i(TAG, "Banner loaded.")
                                }

                                override fun onBannerFailedToLoad(
                                    bannerView: BannerView,
                                    errorInfo: BannerErrorInfo
                                ) {
                                    if (
                                        activePlacement == PRIMARY_BANNER_PLACEMENT &&
                                        placementId == PRIMARY_BANNER_PLACEMENT &&
                                        !triedFallback
                                    ) {
                                        triedFallback = true
                                        activePlacement = FALLBACK_BANNER_PLACEMENT
                                        Log.w(TAG, "Primary banner placement failed; trying fallback.")
                                    } else {
                                        Log.w(TAG, "Banner failed to load.")
                                    }
                                }

                                override fun onBannerClick(bannerView: BannerView) {
                                    Log.i(TAG, "Banner clicked.")
                                }

                                override fun onBannerShown(bannerView: BannerView) {
                                    Log.i(TAG, "Banner shown.")
                                }

                                override fun onBannerLeftApplication(bannerView: BannerView) {
                                    Log.i(TAG, "Banner opened another app.")
                                }
                            }
                            load()
                        }
                    },
                    onRelease = { bannerView ->
                        runCatching {
                            bannerView.listener = null
                            bannerView.destroy()
                        }.onFailure {
                            Log.w(TAG, "Banner cleanup failed.")
                        }
                    }
                )
            }
        }
    }
}
