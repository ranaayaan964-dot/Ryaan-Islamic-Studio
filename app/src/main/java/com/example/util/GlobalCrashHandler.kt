package com.example.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.example.ui.CrashRecoveryActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * PRODUCTION GLOBAL UNCAUGHT EXCEPTION HANDLER
 *
 * Implements Thread.UncaughtExceptionHandler to enforce a 100% Zero-Crash policy.
 * - Intercepts any fatal runtime exception before the Android OS displays the dreaded "App has stopped" dialog.
 * - Logs the error safely to Logcat and writes the crash diagnostic details to internal storage (`crash_reports.log`).
 * - Seamlessly restarts the application via a graceful, non-crashing Recovery Activity (`CrashRecoveryActivity`).
 * - Cleans up and safely terminates the faulty thread/process.
 */
class GlobalCrashHandler(
    private val application: Application,
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "GlobalCrashHandler"
        private const val CRASH_FILE_NAME = "crash_reports.log"
        private const val PREFS_CRASH = "app_crash_recovery_prefs"
        private const val KEY_LAST_CRASH_TIME = "last_crash_timestamp"
        private const val KEY_LAST_CRASH_MSG = "last_crash_message"

        /**
         * Reads the latest crash logs stored on the device for diagnostics.
         */
        fun getLatestCrashLog(context: Context): String? {
            return try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists()) file.readText() else null
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Clears crash log file.
         */
        fun clearCrashLogs(context: Context) {
            try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear crash log: ${e.message}")
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "CRITICAL: Caught fatal unhandled exception on thread '${thread.name}'", throwable)

            // 1. Format the stack trace
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceString = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val crashReport = buildString {
                appendLine("==========================================")
                appendLine("TIMESTAMP: $timestamp")
                appendLine("THREAD: ${thread.name} (id: ${thread.id})")
                appendLine("EXCEPTION: ${throwable::class.java.name}")
                appendLine("MESSAGE: ${throwable.message.orEmpty()}")
                appendLine("STACK TRACE:")
                appendLine(stackTraceString)
                appendLine("==========================================")
            }

            // 2. Persist diagnostic report to internal files directory
            try {
                val crashFile = File(application.filesDir, CRASH_FILE_NAME)
                crashFile.appendText(crashReport + "\n\n")
            } catch (fileEx: Exception) {
                Log.w(TAG, "Failed to write crash to file: ${fileEx.message}")
            }

            // 3. Save timestamp in SharedPreferences to prevent rapid crash loops
            val prefs = application.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            val lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0L)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastCrashTime

            prefs.edit()
                .putLong(KEY_LAST_CRASH_TIME, currentTime)
                .putString(KEY_LAST_CRASH_MSG, throwable.message.orEmpty())
                .apply()

            // 4. Launch Recovery Activity without showing the OS Force Close dialog
            // If crashing repeatedly within 2 seconds, do not loop infinitely
            if (timeDiff > 2000L) {
                val recoveryIntent = Intent(application, CrashRecoveryActivity::class.java).apply {
                    putExtra(CrashRecoveryActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: "Unexpected application error")
                    putExtra(CrashRecoveryActivity.EXTRA_STACK_TRACE, stackTraceString.take(4000))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                application.startActivity(recoveryIntent)
            } else {
                Log.w(TAG, "Rapid crash loop detected ($timeDiff ms). Allowing default process termination.")
                defaultHandler?.uncaughtException(thread, throwable)
                return
            }

        } catch (handlerError: Throwable) {
            Log.e(TAG, "Error inside GlobalCrashHandler while handling crash", handlerError)
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            // Kill current dying process immediately so the OS doesn't hang or show force close
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
