package com.keenetic.local

import android.app.Application
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.util.AppLogger

class KeeneticApp : Application() {
    companion object {
        lateinit var instance: KeeneticApp
            private set
    }

    lateinit var dataStoreManager: DataStoreManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLogger.init(this)
        dataStoreManager = DataStoreManager(this)
        AppLogger.logAction("Application created", "logFile=${AppLogger.getLogFile()?.absolutePath}")
    }
}
