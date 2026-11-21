package com.ayaan.islamicstudio.ui.components.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UnityRewardedManager {
    const val DEFAULT_REWARDED_PLACEMENT = "BP_Rewarded_Android"
    private const val FALLBACK_REWARDED_PLACEMENT = "Rewarded_Android"
    private const val RETRY_DELAY_MS = 10_000L
    private const val TAG = "UnityRewardedManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableIsRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = mutableIsRewardedReady.asStateFlow()

    @Volatile
    private var loadedPlacementId: String? = null
    private var requestedPlacementId: String = DEFAULT_REWARDED_PLACEMENT
    private var isLoading = false
    private var retryScheduled = false

    fun preloadRewarded(
        context: Context,
        placementId: String = DEFAULT_REWARDED_PLACEMENT
    ) {
        val appContext = context.applicationContext
        if (ProSubscriptionRepository.getInstance(appContext).proStatusFlow.value.isProActive) return
        if (!UnityAds.isInitialized()) return

        synchronized(this) {
            if (mutableIsRewardedReady.value || isLoading) return
            requestedPlacementId = placementId
            isLoading = true
        }
        loadPlacement(placementId, appContext)
    }

    fun showRewardedAd(
        activity: Activity,
        placementId: String = DEFAULT_REWARDED_PLACEMENT,
        onRewardEarned: () -> Unit,
        onAdDismissedOrFailed: (String?) -> Unit = {}
    ) {
        if (ProSubscriptionRepository.getInstance(activity).proStatusFlow.value.isProActive) {
            onRewardEarned()
            return
        }

        val loadedPlacement = loadedPlacementId
        if (!mutableIsRewardedReady.value || loadedPlacement == null) {
            preloadRewarded(activity, placementId)
            onAdDismissedOrFailed("Rewarded video is still loading. Please try again in a few seconds.")
            return
        }

        try {
            UnityAds.show(
                activity,
                loadedPlacement,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        resetReadyState()
                        Log.w(TAG, "Rewarded ad show failed ($error).")
                        preloadRewarded(activity)
                        onAdDismissedOrFailed(message)
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.i(TAG, "Rewarded ad started.")
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.i(TAG, "Rewarded ad clicked.")
                    }

                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        resetReadyState()
                        preloadRewarded(activity)
                        when (state) {
                            UnityAds.UnityAdsShowCompletionState.COMPLETED -> onRewardEarned()
                            UnityAds.UnityAdsShowCompletionState.SKIPPED ->
                                onAdDismissedOrFailed("Watch the full video to unlock your bonus reward.")
                            else -> onAdDismissedOrFailed(null)
                        }
                    }
                }
            )
        } catch (error: Exception) {
            resetReadyState()
            Log.w(TAG, "Rewarded ad could not be shown.", error)
            preloadRewarded(activity)
            onAdDismissedOrFailed(error.localizedMessage)
        }
    }

    private fun loadPlacement(placementId: String, context: Context) {
        try {
            UnityAds.load(placementId, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(loadedId: String) {
                    synchronized(this@UnityRewardedManager) {
                        isLoading = false
                        retryScheduled = false
                        loadedPlacementId = loadedId
                        mutableIsRewardedReady.value = true
                    }
                    Log.i(TAG, "Rewarded ad loaded.")
                }

                override fun onUnityAdsFailedToLoad(
                    failedId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    synchronized(this@UnityRewardedManager) { isLoading = false }
                    Log.w(TAG, "Rewarded ad load failed ($error).")
                    if (failedId == requestedPlacementId &&
                        requestedPlacementId == DEFAULT_REWARDED_PLACEMENT
                    ) {
                        startLoadIfIdle(FALLBACK_REWARDED_PLACEMENT, context)
                    } else {
                        scheduleRetry(context)
                    }
                }
            })
        } catch (error: Exception) {
            synchronized(this) { isLoading = false }
            Log.w(TAG, "Rewarded ad load could not be started.", error)
            if (placementId == DEFAULT_REWARDED_PLACEMENT) {
                startLoadIfIdle(FALLBACK_REWARDED_PLACEMENT, context)
            } else {
                scheduleRetry(context)
            }
        }
    }

    private fun startLoadIfIdle(placementId: String, context: Context) {
        synchronized(this) {
            if (mutableIsRewardedReady.value || isLoading) return
            isLoading = true
        }
        loadPlacement(placementId, context)
    }

    private fun scheduleRetry(context: Context) {
        synchronized(this) {
            if (retryScheduled) return
            retryScheduled = true
        }
        scope.launch {
            delay(RETRY_DELAY_MS)
            synchronized(this@UnityRewardedManager) { retryScheduled = false }
            preloadRewarded(context, requestedPlacementId)
        }
    }

    private fun resetReadyState() {
        synchronized(this) {
            loadedPlacementId = null
            mutableIsRewardedReady.value = false
        }
    }
}
