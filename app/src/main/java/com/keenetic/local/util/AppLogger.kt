package com.keenetic.local.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "KeeneticApp"
    private const val MAX_FILE_BYTES = 2L * 1024L * 1024L
    private const val LOG_FILE_NAME = "keenetic-local.log"

    @Volatile
    private var logFile: File? = null
    private val fileLock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, LOG_FILE_NAME)
        synchronized(fileLock) {
            logFile = file
            try {
                dir.mkdirs()
                if (!file.exists()) file.createNewFile()
            } catch (_: Exception) {
                // ignore: logcat still works
            }
        }
    }

    fun getLogFile(): File? = logFile

    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val file = logFile ?: return
        val ts = synchronized(dateFormat) { dateFormat.format(Date()) }
        val sb = StringBuilder()
            .append(ts).append(' ')
            .append(level).append('/').append(tag).append(": ")
            .append(message)
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.append('\n').append(sw.toString())
        }
        sb.append('\n')
        synchronized(fileLock) {
            try {
                if (file.exists() && file.length() > MAX_FILE_BYTES) {
                    val backup = File(file.parentFile, "$LOG_FILE_NAME.1")
                    if (backup.exists()) backup.delete()
                    file.renameTo(backup)
                    file.createNewFile()
                }
                file.appendText(sb.toString())
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun logAction(action: String, details: String = "") {
        val msg = if (details.isNotEmpty()) "$action: $details" else action
        Log.d(TAG, msg)
        writeToFile("D", TAG, msg)
    }

    fun logInfo(tag: String, message: String) {
        val msg = "[$tag] $message"
        Log.i(TAG, msg)
        writeToFile("I", TAG, msg)
    }

    fun logDebug(tag: String, message: String) {
        val msg = "[$tag] $message"
        Log.d(TAG, msg)
        writeToFile("D", TAG, msg)
    }

    fun logError(action: String, throwable: Throwable? = null) {
        val msg = "Error in $action"
        Log.e(TAG, msg, throwable)
        writeToFile("E", TAG, msg, throwable)
    }

    fun d(message: String) {
        Log.d(TAG, message)
        writeToFile("D", TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        writeToFile("W", TAG, message, throwable)
    }
}
