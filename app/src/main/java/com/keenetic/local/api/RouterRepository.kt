package com.keenetic.local.api

import android.content.Context
import android.util.Log
import com.keenetic.local.data.DataStoreManager
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RouterRepository(context: Context) {

    private val dataStore = DataStoreManager(context)
    private var restApi: KeeneticRestApi? = null
    private var sshClient: KeeneticSshClient? = null

    suspend fun initClients(password: String): String {
        val ip = dataStore.routerIp.first()
        val login = dataStore.routerLogin.first()
        val baseUrl = "http://$ip/"

        Log.d("KeeneticRepo", "Init: url=$baseUrl, login=$login")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(KeeneticAuthInterceptor(login, password))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
            .build()

        restApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeeneticRestApi::class.java)

        sshClient = KeeneticSshClient(host = ip, login = login, password = password)

        // Тест: GET /auth ожидаемо вернёт 401 — это НЕ ошибка
        return try {
            val test = restApi!!.auth()
            // Логируем заголовки ответа для отладки авторизации
            try {
                val headers = test.headers()
                for (name in headers.names()) {
                    Log.d("KeeneticRepo", "auth header: $name=${headers[name]}")
                }
            } catch (ignored: Exception) {
            }

            when (test.code()) {
                401 -> "OK (auth required)"
                200 -> "OK (no auth)"
                else -> "OK (HTTP ${test.code()})"
            }
        } catch (e: java.net.UnknownHostException) {
            "FAIL: Unknown host"
        } catch (e: java.net.ConnectException) {
            "FAIL: Connection refused"
        } catch (e: java.net.SocketTimeoutException) {
            "FAIL: Timeout"
        } catch (e: Exception) {
            "FAIL: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    fun getRestApi(): KeeneticRestApi = restApi ?: throw IllegalStateException("Not initialized")
    fun getSshClient(): KeeneticSshClient = sshClient ?: throw IllegalStateException("Not initialized")
}
