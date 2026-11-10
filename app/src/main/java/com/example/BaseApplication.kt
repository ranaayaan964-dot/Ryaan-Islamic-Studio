package com.example

import android.app.Application
import android.util.Log
import com.example.util.GlobalCrashHandler
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds

/**
 * BaseApplication
 *
 * Core application entrypoint.
 * Automatically installs the [GlobalCrashHandler] to prevent fatal force-closes
 * and ensure 100% resilient operation across all screens and background services.
 */
class BaseApplication : Application() {

    companion object {
        private const val TAG = "BaseApplication"
    }

    override fun onCreate() {
        super.onCreate()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashHandler = GlobalCrashHandler(this, previousHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        Log.i(TAG, "GlobalCrashHandler registered successfully. Zero-Crash policy active. Previous handler: ${previousHandler?.javaClass?.name ?: "none"}")

        UnityAds.initialize(
            applicationContext,
            BuildConfig.UNITY_GAME_ID.ifBlank { "800424750" },
            false,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.i("UnityAdsInit", "Unity Ads SDK initialized successfully (Live Mode)")
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.e("UnityAdsInit", "Unity Ads initialization failed: $message (Error: $error)")
                }
            }
        )
    }
}
