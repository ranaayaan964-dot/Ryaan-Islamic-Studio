package com.ayaan.islamicstudio

import android.app.Application
import android.util.Log
import com.ayaan.islamicstudio.ui.components.ads.UnityAdsController
import com.ayaan.islamicstudio.ui.components.ads.UnityRewardedManager
import com.ayaan.islamicstudio.util.GlobalCrashHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

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
        FirebaseApp.initializeApp(this)
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashHandler = GlobalCrashHandler(this, previousHandler)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        Log.i(TAG, "GlobalCrashHandler registered successfully. Zero-Crash policy active. Previous handler: ${previousHandler?.javaClass?.name ?: "none"}")

        UnityAdsController.initialize(this) {
            UnityRewardedManager.preloadRewarded(this)
        }
    }
}
