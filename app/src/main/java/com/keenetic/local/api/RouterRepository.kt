package com.keenetic.local.api

import android.content.Context
import android.util.Log
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.security.KeystoreManager
import com.keenetic.local.util.AppLogger
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Отвечает за аутентификацию на роутере (challenge/response как в веб-морде
 * Keenetic) и за выдачу готовых клиентов (REST/SSH) остальному приложению.
 */
class RouterRepository(private val context: Context) {

    private val dataStore = DataStoreManager(context)
    private val keystore = KeystoreManager()

    private var restApi: KeeneticRestApi? = null
    private var sshClient: KeeneticSshClient? = null
    private var currentCookie: String? = null

    /**
     * Полный цикл входа: challenge -> md5/sha256 -> POST /auth -> постоянный
     * клиент с куки на все последующие запросы.
     */
    suspend fun login(password: String): String {
        val ip = dataStore.routerIp.first()
        val login = dataStore.routerLogin.first()
        val baseUrl = "http://$ip/"

        AppLogger.logAction("Login start", "ip=$ip login=$login")

        val tempClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val tempApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(tempClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeeneticRestApi::class.java)

        val authResp = try {
            tempApi.auth()
        } catch (e: Exception) {
            AppLogger.e("Login auth request failed", throwable = e)
            return "FAIL: GET /auth ${e.message}"
        }

        if (authResp.code() == 200) {
            AppLogger.i("Login succeeded without password")
            // Роутер без пароля - пропускаем challenge, но клиент всё равно нужен.
            buildPermanentClient(baseUrl, cookie = null)
            return "OK (no password)"
        }

        val wwwAuth = authResp.headers()["WWW-Authenticate"] ?: ""
        val realm = authResp.headers()["X-NDM-Realm"]
            ?: extractRealm(wwwAuth)
            ?: "Keenetic KN-2311"
        val challenge = authResp.headers()["X-NDM-Challenge"] ?: return "FAIL: no challenge"
        val setCookie = authResp.headers()["Set-Cookie"] ?: return "FAIL: no Set-Cookie"
        val cookie = setCookie.split(";")[0].trim()

        Log.d("KeeneticAuth", "Realm='$realm' Challenge='$challenge' Cookie='$cookie'")
        AppLogger.d("Auth challenge received: realm=$realm challenge=$challenge")

        val md5 = md5("$login:$realm:$password")
        val response = sha256("$challenge$md5")

        val loginResp = tempApi.login(AuthRequest(login, response), cookie)
        if (!loginResp.isSuccessful) {
            AppLogger.w("Login request failed with HTTP ${loginResp.code()}")
            return "FAIL: POST /auth ${loginResp.code()}"
        }

        currentCookie = cookie
        buildPermanentClient(baseUrl, cookie)
        AppLogger.i("Login completed successfully")

        savePassword(password)

        return "OK"
    }

    private fun buildPermanentClient(baseUrl: String, cookie: String?) {
        val permanentClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (cookie != null) {
                    addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .addHeader("Cookie", cookie)
                            .build()
                        chain.proceed(req)
                    }
                }
            }
            .build()

        restApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(permanentClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeeneticRestApi::class.java)
    }

    fun getRestApi(): KeeneticRestApi = restApi ?: throw IllegalStateException("Not logged in")

    suspend fun getSshClient(): KeeneticSshClient {
        sshClient?.let { return it }
        val ip = dataStore.routerIp.first()
        val login = dataStore.routerLogin.first()
        val password = readSavedPassword() ?: throw IllegalStateException("No saved password for SSH")
        return KeeneticSshClient(host = ip, login = login, password = password).also { sshClient = it }
    }

    /** Шифрует и сохраняет пароль в DataStore через AndroidKeyStore. */
    suspend fun savePassword(password: String) {
        val encrypted = keystore.encrypt(password)
        dataStore.saveEncryptedPassword(encrypted)
        AppLogger.d("Saved encrypted password for future auto-login")
    }

    suspend fun readSavedPassword(): String? {
        val encrypted = dataStore.encryptedPassword.first() ?: return null
        return try {
            keystore.decrypt(encrypted)
        } catch (e: Exception) {
            AppLogger.e("Failed to decrypt saved password", throwable = e)
            null
        }
    }

    suspend fun hasSavedCredentials(): Boolean = readSavedPassword() != null

    /** Пытается войти сохранённым паролем (для автовхода при старте приложения). */
    suspend fun tryAutoLogin(): String {
        val password = readSavedPassword() ?: return "FAIL: no saved password"
        return login(password)
    }

    fun clearSession() {
        restApi = null
        sshClient = null
        currentCookie = null
        AppLogger.logAction("Session cleared")
    }

    private fun md5(str: String): String = digest("MD5", str)
    private fun sha256(str: String): String = digest("SHA-256", str)

    private fun digest(algo: String, input: String): String {
        val digest = MessageDigest.getInstance(algo)
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun extractRealm(www: String): String? {
        val regex = """realm="([^"]+)"""".toRegex()
        return regex.find(www)?.groupValues?.get(1)
    }
}
