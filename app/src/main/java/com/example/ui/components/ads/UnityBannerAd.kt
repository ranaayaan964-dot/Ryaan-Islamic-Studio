package com.example.ui.components.ads

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

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
    placementId: String = "BP_Banner_Android"
) {
    val activity = LocalContext.current.unwrapActivity()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, Color(0xFFDDBA63).copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (activity != null) AndroidView(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            factory = {
                BannerView(activity, placementId, UnityBannerSize(320, 50)).apply {
                    listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerView: BannerView) {
                            Log.i("UnityBannerAd", "Banner loaded: $placementId")
                        }

                        override fun onBannerFailedToLoad(
                            bannerView: BannerView,
                            errorInfo: BannerErrorInfo
                        ) {
                            Log.e("UnityBannerAd", "Banner failed: ${errorInfo.errorMessage}")
                        }

                        override fun onBannerClick(bannerView: BannerView) {
                            Log.i("UnityBannerAd", "Banner clicked: $placementId")
                        }

                        override fun onBannerShown(bannerView: BannerView) {
                            Log.i("UnityBannerAd", "Banner shown: $placementId")
                        }

                        override fun onBannerLeftApplication(bannerView: BannerView) {
                            Log.i("UnityBannerAd", "Banner left application: $placementId")
                        }
                    }
                    load()
                }
            },
            onRelease = { bannerView -> bannerView.destroy() }
        )
    }
}
