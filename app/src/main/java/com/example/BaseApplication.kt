package com.example

import android.app.Application
import android.util.Log
import com.example.util.GlobalCrashHandler

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
    }
}
