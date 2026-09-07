package com.keenetic.local.api

import com.google.gson.JsonElement
import com.keenetic.local.util.AppLogger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Proxy

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

        // The web configurator uses a few RCI paths that are not exposed by the
        // generic show/path names used by the app. Normalize them here so every
        // existing queryShow(...) caller gets the real Keenetic RCI path.
        val delegate = rciService ?: return
        rciService = Proxy.newProxyInstance(
            KeeneticRciService::class.java.classLoader,
            arrayOf(KeeneticRciService::class.java)
        ) { _, method, args ->
            if (method.name == "queryShow" && args != null && args.isNotEmpty()) {
                val originalPath = args[0] as? String
                if (originalPath != null) {
                    args[0] = normalizeShowPath(originalPath)
                }
            }
            method.invoke(delegate, *(args ?: emptyArray()))
        } as KeeneticRciService

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

    private fun normalizeShowPath(path: String): String {
        val normalized = path.trim().trim('/')
        return when (normalized) {
            // Web configurator: show.sc.ip.static
            "ip/static" -> "sc/ip/static"

            // Web configurator: show.sc.interface.mac.access-list
            "ip/access-list" -> "sc/interface/mac.access-list"

            // Web configurator: show.components
            "components/list" -> "components"

            else -> normalized
        }
    }
}
