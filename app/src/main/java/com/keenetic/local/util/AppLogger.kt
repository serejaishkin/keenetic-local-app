package com.keenetic.local.util

import android.util.Log

object AppLogger {
    private const val TAG = "KeeneticApp"

    fun logAction(action: String, details: String = "") {
        Log.d(TAG, if (details.isNotEmpty()) "$action: $details" else action)
    }

    fun logInfo(tag: String, message: String) {
        Log.i(TAG, "[$tag] $message")
    }

    fun logDebug(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
    }

    fun logError(action: String, throwable: Throwable? = null) {
        Log.e(TAG, "Error in $action", throwable)
    }
}
