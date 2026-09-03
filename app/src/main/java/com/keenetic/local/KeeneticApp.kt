package com.keenetic.local

import android.app.Application
import com.keenetic.local.data.DataStoreManager

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
        dataStoreManager = DataStoreManager(this)
    }
}
