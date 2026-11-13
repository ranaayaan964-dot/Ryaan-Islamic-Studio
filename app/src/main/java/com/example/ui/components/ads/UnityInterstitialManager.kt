package com.example.ui.components.ads

import android.app.Activity
import android.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.example.data.repository.ProSubscriptionRepository
import java.util.concurrent.ConcurrentHashMap

object UnityInterstitialManager {
    private const val TAG = "UnityInterstitial"
    private val loadedPlacements = ConcurrentHashMap.newKeySet<String>()

    fun preloadInterstitial(activity: Activity, placementId: String = "BP_Interstitial_Android") {
        if (ProSubscriptionRepository.getInstance(activity).proStatusFlow.value.isProActive) return
        if (loadedPlacements.contains(placementId)) return

        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                loadedPlacements.add(placementId)
                Log.i(TAG, "Interstitial loaded: $placementId")
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                loadedPlacements.remove(placementId)
                Log.e(TAG, "Interstitial load failed: $message ($error)")
            }
        })
    }

    fun showInterstitial(
        activity: Activity,
        placementId: String = "BP_Interstitial_Android",
        onAdClosed: () -> Unit = {}
    ) {
        if (ProSubscriptionRepository.getInstance(activity).proStatusFlow.value.isProActive) {
            onAdClosed()
            return
        }
        if (!loadedPlacements.contains(placementId)) {
            preloadInterstitial(activity, placementId)
            onAdClosed()
            return
        }

        UnityAds.show(activity, placementId, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                loadedPlacements.remove(placementId)
                Log.e(TAG, "Interstitial show failed: $message ($error)")
                onAdClosed()
            }

            override fun onUnityAdsShowStart(placementId: String) {
                Log.i(TAG, "Interstitial started: $placementId")
            }

            override fun onUnityAdsShowClick(placementId: String) {
                Log.i(TAG, "Interstitial clicked: $placementId")
            }

            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                loadedPlacements.remove(placementId)
                Log.i(TAG, "Interstitial completed: $placementId ($state)")
                preloadInterstitial(activity, placementId)
                onAdClosed()
            }
        })
    }
}
