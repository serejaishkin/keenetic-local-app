package com.keenetic.local.api

import android.content.Context
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.security.KeystoreManager
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RouterRepository(context: Context) {

    private val dataStore = DataStoreManager(context)
    private val keystore = KeystoreManager()

    private var restApi: KeeneticRestApi? = null
    private var sshClient: KeeneticSshClient? = null

    suspend fun initClients(password: String) {
        val ip = dataStore.routerIp.first()
        val login = dataStore.routerLogin.first()
        val baseUrl = "http://$ip/"

        val client = OkHttpClient.Builder()
            .addInterceptor(KeeneticAuthInterceptor(login, password))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        restApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeeneticRestApi::class.java)

        sshClient = KeeneticSshClient(
            host = ip,
            login = login,
            password = password
        )
    }

    fun getRestApi(): KeeneticRestApi = restApi ?: throw IllegalStateException("Repository not initialized")
    fun getSshClient(): KeeneticSshClient = sshClient ?: throw IllegalStateException("Repository not initialized")

    suspend fun savePassword(password: String) {
        // В реальном приложении сохраняем через Keystore, но здесь просто инициализируем
        initClients(password)
    }

    suspend fun getRouterIp(): String = dataStore.routerIp.first()
    suspend fun getRouterLogin(): String = dataStore.routerLogin.first()
}