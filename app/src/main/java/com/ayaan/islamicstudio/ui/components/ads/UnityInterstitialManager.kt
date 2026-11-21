package com.ayaan.islamicstudio.ui.components.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object UnityInterstitialManager {
    private const val TAG = "UnityInterstitial"
    private const val PRIMARY_PLACEMENT = "BP_Interstitial_Android"
    private const val FALLBACK_PLACEMENT = "Interstitial_Android"
    private const val COOLDOWN_MS = 180_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val loadedPlacements = ConcurrentHashMap.newKeySet<String>()
    private val loadingPlacements = ConcurrentHashMap.newKeySet<String>()
    private val stateLock = Any()
    private var lastShowAt = 0L
    private var hasShown = false
    private var showInProgress = false

    fun preloadInterstitial(activity: Activity, placementId: String = PRIMARY_PLACEMENT) {
        val appContext = activity.applicationContext
        if (isProActive(appContext)) return

        scope.launch {
            UnityAdsController.isInitialized.first { it }
            if (isProActive(appContext)) return@launch
            loadPlacement(placementId)
        }
    }

    fun showInterstitial(
        activity: Activity,
        placementId: String = PRIMARY_PLACEMENT,
        onAdClosed: () -> Unit = {}
    ) {
        if (isProActive(activity)) {
            onAdClosed()
            return
        }

        val now = SystemClock.elapsedRealtime()
        val isBlocked = synchronized(stateLock) {
            if (
                showInProgress ||
                (hasShown && now - lastShowAt < COOLDOWN_MS)
            ) {
                true
            } else {
                false
            }
        }
        if (isBlocked) {
            onAdClosed()
            return
        }

        if (!UnityAdsController.isInitialized.value) {
            preloadInterstitial(activity, placementId)
            onAdClosed()
            return
        }

        val loadedPlacement = when {
            loadedPlacements.contains(placementId) -> placementId
            placementId == PRIMARY_PLACEMENT && loadedPlacements.contains(FALLBACK_PLACEMENT) ->
                FALLBACK_PLACEMENT
            else -> null
        }
        if (loadedPlacement == null) {
            preloadInterstitial(activity, placementId)
            onAdClosed()
            return
        }

        val blockedBeforeShow = synchronized(stateLock) {
            if (
                showInProgress ||
                (hasShown && SystemClock.elapsedRealtime() - lastShowAt < COOLDOWN_MS)
            ) {
                true
            } else {
                showInProgress = true
                hasShown = true
                lastShowAt = SystemClock.elapsedRealtime()
                false
            }
        }
        if (blockedBeforeShow) {
            onAdClosed()
            return
        }

        try {
            UnityAds.show(activity, loadedPlacement, object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    loadedPlacements.remove(loadedPlacement)
                    synchronized(stateLock) { showInProgress = false }
                    Log.w(TAG, "Interstitial show failed ($error).")
                    onAdClosed()
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.i(TAG, "Interstitial started.")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.i(TAG, "Interstitial clicked.")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    loadedPlacements.remove(loadedPlacement)
                    synchronized(stateLock) { showInProgress = false }
                    Log.i(TAG, "Interstitial completed ($state).")
                    preloadInterstitial(activity, placementId)
                    onAdClosed()
                }
            })
        } catch (_: Exception) {
            loadedPlacements.remove(loadedPlacement)
            synchronized(stateLock) { showInProgress = false }
            Log.w(TAG, "Interstitial could not be shown.")
            onAdClosed()
        }
    }

    private fun loadPlacement(requestedPlacement: String) {
        if (!UnityAdsController.isInitialized.value) return
        val placementToLoad = when {
            loadedPlacements.contains(requestedPlacement) -> return
            requestedPlacement == PRIMARY_PLACEMENT && loadedPlacements.contains(FALLBACK_PLACEMENT) -> return
            !loadingPlacements.add(requestedPlacement) -> return
            else -> requestedPlacement
        }
        if (placementToLoad == FALLBACK_PLACEMENT && requestedPlacement == PRIMARY_PLACEMENT) return

        try {
            UnityAds.load(placementToLoad, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    loadingPlacements.remove(requestedPlacement)
                    loadedPlacements.add(placementId)
                    Log.i(TAG, "Interstitial loaded.")
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    loadingPlacements.remove(requestedPlacement)
                    Log.w(TAG, "Interstitial load failed ($error).")
                    if (requestedPlacement == PRIMARY_PLACEMENT && placementId == PRIMARY_PLACEMENT) {
                        loadFallbackPlacement()
                    }
                }
            })
        } catch (_: Exception) {
            loadingPlacements.remove(requestedPlacement)
            Log.w(TAG, "Interstitial load could not be started.")
            if (requestedPlacement == PRIMARY_PLACEMENT) loadFallbackPlacement()
        }
    }

    private fun loadFallbackPlacement() {
        if (!UnityAdsController.isInitialized.value || !loadingPlacements.add(FALLBACK_PLACEMENT)) return
        try {
            UnityAds.load(FALLBACK_PLACEMENT, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    loadingPlacements.remove(FALLBACK_PLACEMENT)
                    loadedPlacements.add(placementId)
                    Log.i(TAG, "Interstitial fallback loaded.")
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    loadingPlacements.remove(FALLBACK_PLACEMENT)
                    Log.w(TAG, "Interstitial fallback load failed ($error).")
                }
            })
        } catch (_: Exception) {
            loadingPlacements.remove(FALLBACK_PLACEMENT)
            Log.w(TAG, "Interstitial fallback could not be started.")
        }
    }

    private fun isProActive(context: Context): Boolean =
        ProSubscriptionRepository.getInstance(context.applicationContext).proStatusFlow.value.isProActive
}
