package com.keenetic.local.api

import android.util.Log
import com.keenetic.local.data.DataStoreManager
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class RouterRepository(private val context: Context) {
    private val dataStore = DataStoreManager(context)
    private var restApi: KeeneticRestApi? = null
    private var currentCookie: String? = null

    suspend fun login(password: String): String {
        val ip = dataStore.routerIp.first()
        val login = dataStore.routerLogin.first() ?: "admin"
        val baseUrl = "http://$ip/"

        // 1. Temp client для challenge
        val tempClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val tempRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(tempClient)
            .build()

        val tempApi = tempRetrofit.create(KeeneticRestApi::class.java)

        val authResp = try { tempApi.auth().execute() } 
            catch (e: Exception) { return "FAIL: GET /auth ${e.message}" }

        if (authResp.code() == 200) return "OK (no password)"

        val wwwAuth = authResp.headers()["WWW-Authenticate"] ?: ""
        val realm = authResp.headers()["X-NDM-Realm"] 
            ?: extractRealm(wwwAuth) 
            ?: "Keenetic KN-2311"
        val challenge = authResp.headers()["X-NDM-Challenge"] ?: return "FAIL: no challenge"

        val setCookie = authResp.headers()["Set-Cookie"] ?: return "FAIL: no Set-Cookie"
        val cookie = setCookie.split(";")[0].trim()

        Log.d("KeeneticAuth", "Realm='$realm' Challenge='$challenge' Cookie='$cookie'")

        // 2. Хэши (точно как в JS)
        val md5 = md5("$login:$realm:$password")
        val response = sha256("$challenge$md5")

        val loginResp = tempApi.login(AuthRequest(login, response), cookie).execute()
        if (!loginResp.isSuccessful) {
            return "FAIL: POST /auth ${loginResp.code()}"
        }

        currentCookie = cookie

        // 3. Постоянный клиент
        val permanentClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Cookie", cookie)
                    .build()
                chain.proceed(req)
            }
            .build()

        restApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(permanentClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeeneticRestApi::class.java)

        return "OK"
    }

    fun getApi(): KeeneticRestApi = restApi ?: throw IllegalStateException("Not logged in")

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
