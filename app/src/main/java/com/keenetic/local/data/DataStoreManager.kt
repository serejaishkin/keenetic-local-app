package com.keenetic.local.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val ROUTER_IP = stringPreferencesKey("router_ip")
        val ROUTER_PORT = stringPreferencesKey("router_port")
        val ROUTER_USERNAME = stringPreferencesKey("router_username")
        val AUTO_LOGIN = booleanPreferencesKey("auto_login")
        val USE_HTTPS = booleanPreferencesKey("use_https")
        val REFRESH_INTERVAL = stringPreferencesKey("refresh_interval")
    }

    val routerIp: Flow<String> = context.dataStore.data.map { it[ROUTER_IP] ?: "192.168.1.1" }
    val routerPort: Flow<String> = context.dataStore.data.map { it[ROUTER_PORT] ?: "80" }
    val routerUsername: Flow<String> = context.dataStore.data.map { it[ROUTER_USERNAME] ?: "admin" }
    val autoLogin: Flow<Boolean> = context.dataStore.data.map { it[AUTO_LOGIN] ?: false }
    val useHttps: Flow<Boolean> = context.dataStore.data.map { it[USE_HTTPS] ?: false }
    val refreshInterval: Flow<String> = context.dataStore.data.map { it[REFRESH_INTERVAL] ?: "5" }

    suspend fun saveSettings(ip: String, port: String, username: String, autoLogin: Boolean, useHttps: Boolean = false) {
        context.dataStore.edit {
            it[ROUTER_IP] = ip
            it[ROUTER_PORT] = port
            it[ROUTER_USERNAME] = username
            it[AUTO_LOGIN] = autoLogin
            it[USE_HTTPS] = useHttps
        }
    }
}
