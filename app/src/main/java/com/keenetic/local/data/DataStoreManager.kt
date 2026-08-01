package com.keenetic.local.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keenetic.local.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keenetic_settings")

data class SavedService(
    val name: String,
    val host: String,
    val port: String,
    val username: String = "",
    val password: String = ""
)

class DataStoreManager(private val context: Context) {
    private val keystore = KeystoreManager()

    companion object {
        val ROUTER_IP = stringPreferencesKey("router_ip")
        val ROUTER_LOGIN = stringPreferencesKey("router_login")
        val LAST_SCREEN = stringPreferencesKey("last_screen")
        val ENCRYPTED_PASSWORD = stringPreferencesKey("encrypted_password")
        val AUTO_LOGIN = booleanPreferencesKey("auto_login")
        val WEB_SERVICES = stringPreferencesKey("web_services")
    }

    val routerIp: Flow<String> = context.dataStore.data.map { it[ROUTER_IP] ?: "192.168.1.1" }
    val routerLogin: Flow<String> = context.dataStore.data.map { it[ROUTER_LOGIN] ?: "admin" }
    val encryptedPassword: Flow<String?> = context.dataStore.data.map { it[ENCRYPTED_PASSWORD] }
    val autoLogin: Flow<Boolean> = context.dataStore.data.map { it[AUTO_LOGIN] ?: false }
    val webServices: Flow<List<SavedService>> = context.dataStore.data.map { prefs ->
        val raw = prefs[WEB_SERVICES] ?: "[]"
        runCatching {
            Gson().fromJson<List<SavedService>>(raw, object : TypeToken<List<SavedService>>() {}.type)
                ?.map { service ->
                    val decryptedPassword = service.password.takeIf { it.isNotBlank() }?.let { keystore.decrypt(it) }.orEmpty()
                    service.copy(password = decryptedPassword)
                }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun saveRouterIp(ip: String) {
        context.dataStore.edit { it[ROUTER_IP] = ip }
    }

    suspend fun saveRouterLogin(login: String) {
        context.dataStore.edit { it[ROUTER_LOGIN] = login }
    }

    suspend fun saveEncryptedPassword(encrypted: String) {
        context.dataStore.edit { it[ENCRYPTED_PASSWORD] = encrypted }
    }

    suspend fun setAutoLogin(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_LOGIN] = enabled }
    }

    suspend fun saveWebServices(services: List<SavedService>) {
        val encryptedServices = services.map { service ->
            val encryptedPassword = service.password.takeIf { it.isNotBlank() }?.let { keystore.encrypt(it) }.orEmpty()
            service.copy(password = encryptedPassword)
        }
        context.dataStore.edit { it[WEB_SERVICES] = Gson().toJson(encryptedServices) }
    }

    /** Очищает всё, кроме роутера/логина, - их удобно оставить для следующего входа. */
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(ENCRYPTED_PASSWORD)
            it.remove(AUTO_LOGIN)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
