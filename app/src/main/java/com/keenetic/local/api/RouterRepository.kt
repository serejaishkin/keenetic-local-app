package com.keenetic.local.api

import com.google.gson.JsonElement
import com.keenetic.local.util.AppLogger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class AuthResult(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * High-level repository implementation extending [KeeneticRciRepository] to provide
 * router management features, typed Rest API access, and backward compatibility.
 */
class RouterRepository : KeeneticRciRepository() {
    private var restApi: KeeneticRestApi? = null

    init {
        rebuildRetrofit(currentBaseUrl)
    }

    override fun rebuildRetrofit(baseUrl: String) {
        super.rebuildRetrofit(baseUrl)
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        restApi = retrofit.create(KeeneticRestApi::class.java)
    }

    /**
     * Reconfigures router base URL with host, port, and HTTPS mode.
     */
    fun initApi(host: String, port: String, useHttps: Boolean = false) {
        configureBaseUrl(host, port, useHttps)
    }

    /**
     * Returns the typed [KeeneticRestApi] instance for KeeneticOS endpoints.
     */
    fun getRestApi(): KeeneticRestApi {
        if (restApi == null) {
            initApi("192.168.1.1", "80")
        }
        return restApi!!
    }

    /**
     * Clears session cookies and any active authentication headers.
     */
    fun clearSession() {
        clearAuth()
    }
}
