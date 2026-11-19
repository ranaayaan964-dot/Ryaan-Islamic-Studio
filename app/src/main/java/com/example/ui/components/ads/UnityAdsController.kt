package com.example.ui.components.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UnityAdsController {
    private const val TAG = "UnityAdsController"
    private const val RETRY_DELAY_MS = 10_000L
    private const val DEFAULT_GAME_ID = "800424750"

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableIsInitialized = MutableStateFlow(false)
    private val mutableInitError = MutableStateFlow<String?>(null)

    val isInitialized: StateFlow<Boolean> = mutableIsInitialized.asStateFlow()
    val initError: StateFlow<String?> = mutableInitError.asStateFlow()

    private var appContext: Context? = null
    private var gameId = BuildConfig.UNITY_GAME_ID.trim().ifBlank { DEFAULT_GAME_ID }
    private var initializationInProgress = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var retryJob: Job? = null

    fun initialize(context: Context, gameId: String = BuildConfig.UNITY_GAME_ID) {
        synchronized(lock) {
            if (mutableIsInitialized.value || initializationInProgress) return
            appContext = context.applicationContext
            this.gameId = gameId.trim().ifBlank { DEFAULT_GAME_ID }
            registerNetworkCallbackLocked()
        }

        if (this.gameId.isBlank()) {
            failInitialization("Unity Ads game ID is not configured.")
            return
        }
        startInitialization()
    }

    private fun startInitialization() {
        val context: Context
        val id: String
        synchronized(lock) {
            if (mutableIsInitialized.value || initializationInProgress) return
            context = appContext ?: return
            id = gameId
            initializationInProgress = true
            retryJob?.cancel()
            retryJob = null
        }

        try {
            UnityAds.initialize(
                context,
                id,
                false,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        synchronized(lock) {
                            initializationInProgress = false
                            retryJob?.cancel()
                            retryJob = null
                        }
                        mutableInitError.value = null
                        mutableIsInitialized.value = true
                        Log.i(TAG, "Unity Ads initialized.")
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        failInitialization("Initialization failed (${error ?: "unknown"}).")
                    }
                }
            )
        } catch (_: Exception) {
            failInitialization("Initialization could not be started.")
        }
    }

    private fun failInitialization(error: String) {
        synchronized(lock) { initializationInProgress = false }
        mutableIsInitialized.value = false
        mutableInitError.value = error
        Log.w(TAG, error)
        if (gameId.isNotBlank()) scheduleRetryIfConnected()
    }

    private fun registerNetworkCallbackLocked() {
        if (networkCallback != null) return
        val manager = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (mutableInitError.value != null && !mutableIsInitialized.value) {
                    scheduleRetryIfConnected()
                }
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(request, callback)
            networkCallback = callback
        } catch (_: Exception) {
            Log.w(TAG, "Unity Ads network retry monitoring is unavailable.")
        }
    }

    private fun scheduleRetryIfConnected() {
        synchronized(lock) {
            if (retryJob?.isActive == true || mutableIsInitialized.value) return
            retryJob = scope.launch {
                delay(RETRY_DELAY_MS)
                if (!mutableIsInitialized.value && mutableInitError.value != null && hasNetworkConnection()) {
                    startInitialization()
                }
            }
        }
    }

    private fun hasNetworkConnection(): Boolean {
        val context = appContext ?: return false
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = manager.activeNetwork ?: return false
                val capabilities = manager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                manager.activeNetworkInfo?.isConnected == true
            }
        } catch (_: SecurityException) {
            false
        }
    }
}
