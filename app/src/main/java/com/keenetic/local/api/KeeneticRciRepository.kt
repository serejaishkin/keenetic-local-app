package com.keenetic.local.api

import android.util.Base64
import com.google.gson.JsonElement
import com.keenetic.local.util.AppLogger
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Data repository class handling RCI (Remote Control Interface) REST API requests
 * to the Keenetic router, including dynamic base URL configuration and authentication header support.
 */
open class KeeneticRciRepository(
    initialHost: String = "192.168.1.1",
    initialPort: String = "80",
    initialHttps: Boolean = false
) {
    // Current router network configuration
    var currentHost: String = initialHost
        protected set

    var currentPort: String = initialPort
        protected set

    var isHttps: Boolean = initialHttps
        protected set

    var currentBaseUrl: String = buildBaseUrl(initialHost, initialPort, initialHttps)
        protected set

    // Authentication header state (e.g. "Bearer <token>" or "Basic <base64>")
    private val authHeaderRef = AtomicReference<String?>(null)

    // Thread-safe map for session cookies (e.g., sys_auth)
    protected val rawCookies = ConcurrentHashMap<String, String>()

    // Active Retrofit service instances
    protected var rciService: KeeneticRciService? = null

    // CookieJar for session tracking
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { cookie ->
                rawCookies[cookie.name] = cookie.value
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return rawCookies.map { (name, value) ->
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(url.host)
                    .path("/")
                    .build()
            }
        }
    }

    /**
     * Interceptor providing authentication header support and session cookie injection/extraction.
     */
    private val authAndCookieInterceptor = Interceptor { chain ->
        val original = chain.request()
        val reqBuilder = original.newBuilder()

        // 1. Inject Authorization header if configured
        val authHeader = authHeaderRef.get()
        if (!authHeader.isNullOrBlank()) {
            reqBuilder.header("Authorization", authHeader)
        }

        // 2. Inject session cookies if present
        if (rawCookies.isNotEmpty()) {
            val cookieHeaderVal = rawCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            reqBuilder.header("Cookie", cookieHeaderVal)
        }

        val response = chain.proceed(reqBuilder.build())

        // 3. Extract any Set-Cookie headers regardless of HTTP status (200, 401, etc.)
        val setCookies = response.headers("Set-Cookie")
        for (header in setCookies) {
            val parts = header.split(";")
            if (parts.isNotEmpty()) {
                val pair = parts[0].trim()
                val eqIdx = pair.indexOf('=')
                if (eqIdx > 0) {
                    val name = pair.substring(0, eqIdx).trim()
                    val value = pair.substring(eqIdx + 1).trim()
                    rawCookies[name] = value
                }
            }
        }

        response
    }

    protected val okHttpClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(cookieJar)
            .addInterceptor(authAndCookieInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(18, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    init {
        rebuildRetrofit(currentBaseUrl)
    }

    // ==========================================
    // Base URL Configuration
    // ==========================================

    /**
     * Set base URL directly from a full URL string (e.g. "http://192.168.1.1:80/" or "https://router:443/").
     */
    @Synchronized
    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        val parsed = cleanUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid base URL: $url")
        currentHost = parsed.host
        currentPort = parsed.port.toString()
        isHttps = parsed.isHttps
        currentBaseUrl = cleanUrl
        rebuildRetrofit(cleanUrl)
    }

    /**
     * Configure base URL by specifying host, port, and HTTPS protocol preference.
     */
    @Synchronized
    fun configureBaseUrl(host: String, port: String = "80", useHttps: Boolean = false) {
        var cleanHost = host.trim()
        var cleanPort = port.trim()
        var https = useHttps

        if (cleanHost.startsWith("https://", ignoreCase = true)) {
            https = true
            cleanHost = cleanHost.substring(8)
        } else if (cleanHost.startsWith("http://", ignoreCase = true)) {
            https = false
            cleanHost = cleanHost.substring(7)
        }

        if (cleanHost.contains("/")) {
            cleanHost = cleanHost.substringBefore("/")
        }

        if (cleanHost.contains(":")) {
            cleanPort = cleanHost.substringAfter(":")
            cleanHost = cleanHost.substringBefore(":")
        }

        if (cleanPort.isEmpty()) {
            cleanPort = if (https) "443" else "80"
        }

        currentHost = cleanHost
        currentPort = cleanPort
        isHttps = https

        val baseUrl = buildBaseUrl(cleanHost, cleanPort, https)
        currentBaseUrl = baseUrl
        rebuildRetrofit(baseUrl)
    }

    private fun buildBaseUrl(host: String, port: String, useHttps: Boolean): String {
        val scheme = if (useHttps) "https" else "http"
        return "$scheme://$host:$port/"
    }

    @Synchronized
    open fun rebuildRetrofit(baseUrl: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        rciService = retrofit.create(KeeneticRciService::class.java)
    }

    fun getService(): KeeneticRciService {
        if (rciService == null) {
            rebuildRetrofit(currentBaseUrl)
        }
        return rciService!!
    }

    // ==========================================
    // Authentication Header Support
    // ==========================================

    /**
     * Configure raw Authorization header value (e.g. "Bearer <token>", "Basic <base64>", or custom auth key).
     */
    fun setAuthHeader(header: String?) {
        authHeaderRef.set(header)
    }

    /**
     * Retrieve the current Authorization header, if any.
     */
    fun getAuthHeader(): String? = authHeaderRef.get()

    /**
     * Configure Bearer Token authorization header: "Bearer <token>".
     */
    fun setBearerToken(token: String?) {
        if (token.isNullOrBlank()) {
            authHeaderRef.set(null)
        } else {
            authHeaderRef.set("Bearer ${token.trim()}")
        }
    }

    /**
     * Configure HTTP Basic authentication header: "Basic base64(username:password)".
     */
    fun setBasicAuth(username: String, password: String) {
        val credentials = "$username:$password"
        val encoded = try {
            java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        } catch (e: Throwable) {
            android.util.Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        }
        authHeaderRef.set("Basic $encoded")
    }

    /**
     * Clear all authentication headers and stored session cookies.
     */
    fun clearAuth() {
        authHeaderRef.set(null)
        rawCookies.clear()
    }

    // ==========================================
    // Keenetic Challenge-Response Authentication
    // ==========================================

    /**
     * Authenticates with Keenetic router using the standard challenge-response mechanism.
     * 1. Initiates GET /auth to receive realm and challenge
     * 2. Computes SHA256(challenge + MD5(user:realm:password))
     * 3. Posts computed hash to POST /auth
     */
    suspend fun authenticate(username: String, password: String): AuthResult {
        return try {
            val service = getService()
            val initialAuth = service.checkAuth()
            if (initialAuth.code() == 200) {
                return AuthResult(true)
            }

            val realm = initialAuth.headers()["X-NDM-Realm"] ?: "Keenetic"
            val challenge = initialAuth.headers()["X-NDM-Challenge"] ?: ""

            // 1. Calculate hash1 = MD5(username:realm:password)
            val hash1 = md5("$username:$realm:$password")

            // Modern KeeneticOS (3.x, 4.x, 5.x) uses SHA256(challenge + hash1)
            val sha256Response = sha256("$challenge$hash1")
            val loginPayloadSha = mapOf(
                "login" to username,
                "password" to sha256Response
            )

            val loginResSha = service.login(loginPayloadSha)
            if (loginResSha.isSuccessful || loginResSha.code() == 200) {
                return AuthResult(true)
            }

            // Fallback for legacy NDMS 2.x: MD5(challenge + hash1)
            val md5Response = md5("$challenge$hash1")
            val loginPayloadMd5 = mapOf(
                "login" to username,
                "password" to md5Response
            )
            val loginResMd5 = service.login(loginPayloadMd5)
            if (loginResMd5.isSuccessful || loginResMd5.code() == 200) {
                return AuthResult(true)
            }

            // Fallback if challenge was empty
            if (challenge.isEmpty()) {
                val plainPayload = mapOf("login" to username, "password" to password)
                val plainRes = service.login(plainPayload)
                if (plainRes.isSuccessful || plainRes.code() == 200) {
                    return AuthResult(true)
                }
            }

            val code = loginResSha.code()
            if (code == 401 || loginResMd5.code() == 401) {
                AuthResult(false, "Неверный логин или пароль для $currentHost")
            } else {
                AuthResult(false, "Ошибка сервера роутера (HTTP $code)")
            }
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("Failed to connect", ignoreCase = true) == true ->
                    "Не удалось подключиться к $currentHost:$currentPort. Проверьте IP и подключение к Wi-Fi роутера."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Таймаут соединения с $currentHost:$currentPort. Убедитесь, что устройство в одной сети с роутером."
                e.message?.contains("SSL", ignoreCase = true) == true ->
                    "Ошибка SSL соединения. Попробуйте переключить HTTP/HTTPS."
                else ->
                    "Ошибка: ${e.localizedMessage ?: e.message ?: "Не удалось связаться с роутером"}"
            }
            AuthResult(false, msg)
        }
    }

    // ==========================================
    // RCI Request Handling
    // ==========================================

    /**
     * Execute batch RCI commands via POST /rci/
     */
    suspend fun executeRci(commands: List<Map<String, Any>>): Response<JsonElement> {
        return getService().executeRci(commands)
    }

    /**
     * Execute batch RCI commands with automatic non-volatile configuration save
     * {"system": {"configuration": {"save": {}}}}
     */
    suspend fun executeRciWithSave(commands: List<Map<String, Any>>): Boolean {
        return try {
            val saveCmd = mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
            val fullBatch = commands + saveCmd
            val response = getService().executeRci(fullBatch)
            if (response.isSuccessful) {
                val body = response.body()
                body == null || !isRciError(body)
            } else {
                false
            }
        } catch (e: Exception) {
            AppLogger.logError("executeRciWithSave", e)
            false
        }
    }

    /**
     * Universal query method for Keenetic RCI show paths (e.g. "interface", "ip/hotspot").
     * First attempts direct GET /rci/show/$path; if rejected or unavailable, falls back to POST /rci/.
     */
    suspend fun queryShow(path: String, fallbackCommand: Map<String, Any>? = null): JsonElement? {
        return try {
            val service = getService()

            // 1. Direct GET /rci/show/$path
            val getRes = service.queryShow(path)
            if (getRes.isSuccessful) {
                val body = getRes.body()
                if (body != null && !isRciError(body)) {
                    return body
                }
            }

            // 2. Fallback via POST /rci/ batch
            val cmd = fallbackCommand ?: pathToCommandMap(path)
            val postRes = service.executeRci(listOf(mapOf("show" to cmd)))
            if (postRes.isSuccessful) {
                val pBody = postRes.body()
                if (pBody != null && !isRciError(pBody)) {
                    return extractFirstRciResult(pBody)
                }
            }

            null
        } catch (e: Exception) {
            AppLogger.logError("queryShow($path)", e)
            null
        }
    }

    /**
     * Queries running configuration section (show.sc.*).
     */
    suspend fun querySc(section: String, subSection: String? = null): JsonElement? {
        val inner = if (subSection != null) {
            mapOf(section to mapOf(subSection to emptyMap<String, Any>()))
        } else {
            mapOf(section to emptyMap<String, Any>())
        }
        val command = mapOf("show" to mapOf("sc" to inner))
        return try {
            val response = getService().executeRci(listOf(command))
            if (response.isSuccessful) {
                response.body()?.let { extractFirstRciResult(it) }
            } else null
        } catch (e: Exception) {
            AppLogger.logError("querySc($section)", e)
            null
        }
    }

    fun isRciError(element: JsonElement): Boolean {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("status") && obj.get("status").isJsonArray) {
                val arr = obj.getAsJsonArray("status")
                if (arr.size() > 0 && arr.get(0).isJsonObject) {
                    val sObj = arr.get(0).asJsonObject
                    if (sObj.has("status") && sObj.get("status").asString.equals("error", ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun extractFirstRciResult(element: JsonElement): JsonElement {
        if (element.isJsonArray && element.asJsonArray.size() > 0) {
            val first = element.asJsonArray.get(0)
            if (first.isJsonObject) {
                val obj = first.asJsonObject
                if (obj.has("show")) {
                    var inner = obj.get("show")
                    while (inner.isJsonObject && inner.asJsonObject.size() == 1) {
                        val entry = inner.asJsonObject.entrySet().first()
                        if (entry.value.isJsonObject || entry.value.isJsonArray) {
                            inner = entry.value
                        } else {
                            break
                        }
                    }
                    return inner
                }
            }
            return first
        }
        return element
    }

    private fun pathToCommandMap(path: String): Map<String, Any> {
        val segments = path.split("/").filter { it.isNotBlank() }
        var current: MutableMap<String, Any> = mutableMapOf()
        val root = current
        for (i in segments.indices) {
            val seg = segments[i]
            if (i == segments.size - 1) {
                current[seg] = emptyMap<String, Any>()
            } else {
                val next = mutableMapOf<String, Any>()
                current[seg] = next
                current = next
            }
        }
        return root
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
