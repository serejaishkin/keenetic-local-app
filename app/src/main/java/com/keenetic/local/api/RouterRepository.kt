package com.keenetic.local.api

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

        // The web configurator and the CLI use a few different names for the
        // same RCI show trees. Normalize compatibility aliases in one place so
        // all existing queryShow(...) callers reach the canonical RCI endpoint.
        val delegate = rciService ?: return
        rciService = Proxy.newProxyInstance(
            KeeneticRciService::class.java.classLoader,
            arrayOf(KeeneticRciService::class.java)
        ) { _, method, args ->
            val callArgs = args?.copyOf() ?: emptyArray()
            if (method.name == "queryShow" && callArgs.isNotEmpty()) {
                val originalPath = callArgs[0] as? String
                if (originalPath != null) {
                    callArgs[0] = normalizeShowPath(originalPath)
                }
            }
            method.invoke(delegate, *callArgs)
        } as KeeneticRciService

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        restApi = retrofit.create(KeeneticRestApi::class.java)
    }

    /** Reconfigures router base URL with host, port, and HTTPS mode. */
    fun initApi(host: String, port: String, useHttps: Boolean = false) {
        configureBaseUrl(host, port, useHttps)
    }

    /** Returns the typed [KeeneticRestApi] instance for KeeneticOS endpoints. */
    fun getRestApi(): KeeneticRestApi {
        if (restApi == null) {
            initApi("192.168.1.1", "80")
        }
        return restApi!!
    }

    /** Clears session cookies and any active authentication headers. */
    fun clearSession() {
        clearAuth()
    }

    private fun normalizeShowPath(path: String): String {
        val normalized = path.trim().trim('/').replace(Regex("/+"), "/")
        return when (normalized) {
            // Web configurator: show.sc.ip.static
            "ip/static" -> "sc/ip/static"

            // Web configurator: show.sc.interface.mac.access-list
            "ip/access-list" -> "sc/interface/mac.access-list"

            // Web configurator: show.components
            "components/list" -> "components"

            // The app historically used the plural form, while the RCI tree
            // used by the firewall page is show ip rule.
            "ip/rules" -> "ip/rule"

            // Canonical RCI status branches used by the system detail pages.
            // show ntp status -> /rci/show/ntp/status
            "ntp" -> "ntp/status"

            // show mws status -> /rci/show/mws/status
            "mws" -> "mws/status"

            // show mws member -> /rci/show/mws/member
            "mws/members" -> "mws/member"

            // CLI exposes "show ipv6 prefixes"; the RCI tree is plural too.
            "ipv6/prefix" -> "ipv6/prefixes"

            // CLI uses plural "routes", while the HTTP RCI endpoint is the
            // singular route resource.
            "ipv6/routes" -> "ipv6/route"

            else -> normalized
        }
    }
}
