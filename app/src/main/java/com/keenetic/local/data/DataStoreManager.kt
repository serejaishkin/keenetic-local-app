package com.keenetic.local.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keenetic_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val ROUTER_IP = stringPreferencesKey("router_ip")
        val ROUTER_LOGIN = stringPreferencesKey("router_login")
        val LAST_SCREEN = stringPreferencesKey("last_screen")
    }

    val routerIp: Flow<String> = context.dataStore.data.map { it[ROUTER_IP] ?: "192.168.1.1" }
    val routerLogin: Flow<String> = context.dataStore.data.map { it[ROUTER_LOGIN] ?: "admin" }

    suspend fun saveRouterIp(ip: String) {
        context.dataStore.edit { it[ROUTER_IP] = ip }
    }

    suspend fun saveRouterLogin(login: String) {
        context.dataStore.edit { it[ROUTER_LOGIN] = login }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}