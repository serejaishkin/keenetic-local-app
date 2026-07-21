package com.keenetic.local.util

import android.util.Log

object AppLogger {
    private const val TAG = "KeeneticApp"

    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }

    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }

    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun logAction(action: String, details: String? = null) {
        val message = details?.let { "$action | $it" } ?: action
        i(message)
    }
}
