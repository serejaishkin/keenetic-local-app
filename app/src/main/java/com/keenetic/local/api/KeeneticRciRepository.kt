package com.keenetic.local.api

import android.util.Base64
import com.google.gson.JsonElement
import com.keenetic.local.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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

data class AuthResult(
    val success: Boolean,
    val errorMessage: String? = null
)

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
        // Клавиатура телефона часто добавляет пробел в конце или заглавную букву: пробуем и как введено, и без пробелов.
        val user = username.trim()
        val candidates = listOf(password, password.trim()).distinct()
        var last: AuthResult = AuthResult(false, "Ошибка авторизации")
        for (pw in candidates) {
            last = authenticateOnce(user, pw)
            if (last.success) return last
            if (last.errorMessage?.startsWith("Неверный логин") != true) return last
        }
        return last
    }

    private var lastNdw4Error: String? = null

    private suspend fun authenticateOnce(username: String, password: String): AuthResult {
        return try {
            rawCookies.clear() // свежая сессия: старый cookie даёт чужой challenge
            val service = getService()
            val initialAuth = service.checkAuth()
            AppLogger.logDebug("auth", "GET /auth -> HTTP ${initialAuth.code()}, host=$currentHost:$currentPort, https=$isHttps")
            if (initialAuth.code() == 200) {
                return AuthResult(true)
            }
            if (initialAuth.code() != 401) {
                return AuthResult(
                    false,
                    "Неожиданный ответ роутера на /auth: HTTP ${initialAuth.code()} ($currentHost:$currentPort). " +
                        "Проверьте IP, порт и HTTP/HTTPS."
                )
            }

            val realm = initialAuth.headers()["X-NDM-Realm"] ?: "Keenetic"
            val challenge = initialAuth.headers()["X-NDM-Challenge"] ?: ""
            val www = initialAuth.headers()["WWW-Authenticate"] ?: ""
            AppLogger.logDebug(
                "auth",
                "401 headers: realm=$realm challenge=${if (challenge.isEmpty()) "no" else "yes"} " +
                    "www=${www.take(80)} ndmData=${initialAuth.headers()["X-NDM-Data"] != null}"
            )

            // KeeneticOS 5.2+ NDW4 interactive auth (проверяем в первую очередь, если объявлен)
            if (www.contains("x-ndw4-interactive")) {
                AppLogger.logInfo("auth", "Starting NDW4 interactive auth")
                val ndw4 = authenticateNdw4(username, password)
                AppLogger.logInfo("auth", "NDW4 result: success=${ndw4.success}, error=${ndw4.errorMessage ?: "-"}")
                if (ndw4.success) return ndw4
                lastNdw4Error = ndw4.errorMessage
                // при явной ошибке проверки пароля не пытаемся legacy-фолбэки
                if (ndw4.errorMessage?.contains("signature verification", true) == true ||
                    ndw4.errorMessage?.contains("парол", true) == true) return ndw4
            }

            // 1. Calculate hash1 = MD5(username:realm:password)
            val hash1 = md5("$username:$realm:$password")

            // Modern KeeneticOS (3.x, 4.x, 5.x) uses SHA256(challenge + hash1)
            val loginResSha = service.login(mapOf("login" to username, "password" to sha256("$challenge$hash1")))
            AppLogger.logDebug("auth", "Legacy SHA256 login -> HTTP ${loginResSha.code()}")
            if (loginResSha.isSuccessful || loginResSha.code() == 200) {
                return AuthResult(true)
            }

            // Fallback for legacy NDMS 2.x: MD5(challenge + hash1)
            val loginResMd5 = service.login(mapOf("login" to username, "password" to md5("$challenge$hash1")))
            AppLogger.logDebug("auth", "Legacy MD5 login -> HTTP ${loginResMd5.code()}")
            if (loginResMd5.isSuccessful || loginResMd5.code() == 200) {
                return AuthResult(true)
            }

            // Fallback if challenge was empty
            if (challenge.isEmpty()) {
                val plainRes = service.login(mapOf("login" to username, "password" to password))
                if (plainRes.isSuccessful || plainRes.code() == 200) {
                    return AuthResult(true)
                }
            }

            val code = loginResSha.code()
            if (code == 401 || loginResMd5.code() == 401) {
                val extra = lastNdw4Error?.let { " NDW4: $it" } ?: ""
                AuthResult(
                    false,
                    "Неверный логин или пароль для $currentHost (HTTP $code, realm=$realm, " +
                        "challenge=${if (challenge.isEmpty()) "нет" else "есть"}).$extra " +
                        "Если данные точно верны — возможна временная блокировка входа, подождите 10 минут."
                )
            } else {
                AuthResult(false, "Ошибка сервера роутера (HTTP $code)${lastNdw4Error?.let { ". NDW4: $it" } ?: ""}")
            }
        } catch (e: Exception) {
            AppLogger.logError("authenticateOnce", e)
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

    /**
     * KeeneticOS 5.2+ NDW4 interactive authentication (Argon2id + HMAC-SHA3-512).
     * Performs network I/O and CPU-heavy Argon2 derivation off the main thread.
     */
    private suspend fun authenticateNdw4(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            rawCookies.clear()
            val base = currentBaseUrl.trimEnd('/')
            val json = "application/json; charset=utf-8".toMediaTypeOrNull()
            AppLogger.logDebug("auth", "NDW4 start: base=$base")

            fun postAuth(body: String): okhttp3.Response {
                val req = okhttp3.Request.Builder()
                    .url("$base/auth")
                    .post(okhttp3.RequestBody.create(json, body))
                    .header("Content-Type", "application/json")
                    .build()
                return okHttpClient.newCall(req).execute()
            }

            // GET /auth -> session cookie
            val getResp = okHttpClient.newCall(okhttp3.Request.Builder().url("$base/auth").get().build()).execute()
            val getCode = getResp.code
            val getHasData = getResp.header("X-NDM-Data") != null || getResp.header("x-ndm-data") != null
            getResp.close()
            AppLogger.logDebug("auth", "NDW4 GET /auth -> HTTP $getCode, hasXndmData=$getHasData, cookies=${rawCookies.keys}")

            val clientNonce = KeeneticNdw4Auth.generateClientNonceB64()
            val p1 = postAuth("""{"login":"${escJson(username)}","nonce":"${escJson(clientNonce)}"}""")
            val p1Code = p1.code
            val p1HasData = p1.header("X-NDM-Data") != null || p1.header("x-ndm-data") != null
            val p1Data = ndmDataJson(p1)
            p1.close()
            AppLogger.logDebug("auth", "NDW4 P1 -> HTTP $p1Code, hasXndmData=$p1HasData, dataKeys=${p1Data?.keySet()}")
            if (p1Code == 200) return@withContext AuthResult(true)
            if (p1Data == null) return@withContext AuthResult(false, "NDW4 фаза 1: нет данных (HTTP $p1Code)")

            val params = KeeneticNdw4Auth.Phase1Params(
                saltB64 = p1Data.get("salt")?.asString ?: return@withContext AuthResult(false, "NDW4: нет salt"),
                serverNonce = p1Data.get("nonce")?.asString ?: return@withContext AuthResult(false, "NDW4: нет nonce"),
                iter = p1Data.get("iter")?.asInt ?: 2,
                memcost = p1Data.get("memcost")?.asInt ?: 8
            )
            AppLogger.logDebug("auth", "NDW4 P1 params: iter=${params.iter}, memcost=${params.memcost}")

            val (proof, ctx) = KeeneticNdw4Auth.phase1Crypto(username, password, clientNonce, params)
            val proofB64 = KeeneticNdw4Auth.b64e(proof)

            val p2 = postAuth("""{"login":"${escJson(username)}","nonce":"${escJson(params.serverNonce)}","proof":"${escJson(proofB64)}"}""")
            val p2Code = p2.code
            val p2HasData = p2.header("X-NDM-Data") != null || p2.header("x-ndm-data") != null
            val p2Data = ndmDataJson(p2)
            p2.close()
            AppLogger.logDebug("auth", "NDW4 P2 -> HTTP $p2Code, hasXndmData=$p2HasData, dataKeys=${p2Data?.keySet()}")
            if (p2Code == 200) return@withContext AuthResult(true)
            val signature = p2Data?.get("signature")?.asString
                ?: return@withContext AuthResult(false, "NDW4 фаза 2: нет подписи сервера (HTTP $p2Code)")

            val sp = KeeneticNdw4Auth.phase2Verify(signature, ctx)
            val spB64 = KeeneticNdw4Auth.b64e(sp)

            val p3 = postAuth("""{"login":"${escJson(username)}","nonce":"${escJson(params.serverNonce)}","signature-proof":"${escJson(spB64)}"}""")
            val p3Code = p3.code
            val p3HasData = p3.header("X-NDM-Data") != null || p3.header("x-ndm-data") != null
            p3.close()
            AppLogger.logDebug("auth", "NDW4 P3 -> HTTP $p3Code, hasXndmData=$p3HasData, cookies=${rawCookies.keys}")
            if (p3Code == 200) AuthResult(true)
            else AuthResult(false, "NDW4 фаза 3: отклонено (HTTP $p3Code)")
        } catch (e: Exception) {
            AppLogger.logError("authenticateNdw4", e)
            AuthResult(false, "NDW4 ошибка: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun ndmDataJson(resp: okhttp3.Response): com.google.gson.JsonObject? {
        val h = resp.header("X-NDM-Data") ?: resp.header("x-ndm-data") ?: return null
        return try {
            val decoded = String(android.util.Base64.decode(h, android.util.Base64.DEFAULT), Charsets.UTF_8)
            com.google.gson.JsonParser.parseString(decoded).asJsonObject
        } catch (e: Exception) { null }
    }

    private fun escJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

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
     * Raw authenticated GET against the router (e.g. "/download/<uuid>/<path>").
     * Session cookies/auth headers are injected by the shared OkHttp client.
     */
    suspend fun downloadRaw(relativePath: String): okhttp3.Response {
        val url = currentBaseUrl.trimEnd('/') + "/" + relativePath.trimStart('/')
        val req = okhttp3.Request.Builder().url(url).get().build()
        return okHttpClient.newCall(req).execute()
    }

    /**
     * File upload handshake: RCI "put" allocates a bernuli port, then the file
     * is POSTed as multipart {port, Filedata} to /fui (same as web file-manager).
     * Returns the allocated port or null.
     */
    suspend fun putAllocatePort(remotePath: String, sizeBytes: Long): String? {
        return try {
            val resp = getService().executeRci(
                listOf(mapOf("put" to mapOf("filename" to remotePath, "size" to sizeBytes)))
            )
            if (!resp.isSuccessful) return null
            findPort(resp.body())
        } catch (e: Exception) {
            AppLogger.logError("putAllocatePort", e)
            null
        }
    }

    private fun findPort(el: JsonElement?): String? {
        if (el == null || el.isJsonNull || el.isJsonPrimitive) return null
        if (el.isJsonArray) {
            el.asJsonArray.forEach { findPort(it)?.let { return it } }
            return null
        }
        val obj = el.asJsonObject
        obj.get("port")?.let { portEl ->
            if (portEl.isJsonPrimitive) {
                val s = try { portEl.asString } catch (_: Exception) { "" }
                if (s.isNotBlank()) return s
            } else if (portEl.isJsonObject) {
                findPort(portEl)?.let { return it }
            }
        }
        obj.entrySet().forEach { (_, v) -> findPort(v)?.let { return it } }
        return null
    }

    /**
     * POSTs file bytes as multipart {port, Filedata} to /fui.
     */
    suspend fun uploadFui(port: String, fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        return try {
            val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("port", port)
                .addFormDataPart(
                    "Filedata", fileName,
                    okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                )
                .build()
            val req = okhttp3.Request.Builder()
                .url(currentBaseUrl.trimEnd('/') + "/fui")
                .post(body)
                .build()
            val resp = okHttpClient.newCall(req).execute()
            resp.use { it.isSuccessful }
        } catch (e: Exception) {
            AppLogger.logError("uploadFui", e)
            false
        }
    }

    /**
     * Execute batch RCI commands WITHOUT saving, validating every per-command
     * result. Returns (allOk, firstErrorText?). Use before sending the explicit
     * `system configuration save` so a rejected command never gets persisted
     * together with half-applied siblings.
     */
    suspend fun executeRciChecked(commands: List<Map<String, Any>>): Pair<Boolean, String?> {
        return try {
            val response = getService().executeRci(commands)
            if (!response.isSuccessful) {
                return Pair(false, "HTTP ${response.code()}")
            }
            val body = response.body() ?: return Pair(true, null)
            if (!body.isJsonArray) {
                return if (isRciError(body)) Pair(false, body.toString().take(300))
                else Pair(true, null)
            }
            val arr = body.asJsonArray
            for (i in 0 until arr.size()) {
                val el = arr.get(i)
                if (isRciError(el)) {
                    return Pair(false, "Команда ${i + 1}/${arr.size()}: ${el.toString().take(300)}")
                }
                if (el.isJsonObject) {
                    val s = el.toString()
                    if (s.contains("\"status\":\"error\"", ignoreCase = true)) {
                        return Pair(false, "Команда ${i + 1}/${arr.size()}: ${s.take(300)}")
                    }
                }
            }
            Pair(true, null)
        } catch (e: Exception) {
            AppLogger.logError("executeRciChecked", e)
            Pair(false, e.message)
        }
    }

    suspend fun saveConfiguration(): Pair<Boolean, String?> =
        executeRciChecked(
            listOf(mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>()))))
        )

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
        val normalizedPath = normalizeShowPath(path)
        return try {
            val service = getService()

            // 1. Direct GET /rci/show/<normalized path>
            val getRes = service.queryShow(normalizedPath)
            if (getRes.isSuccessful) {
                val body = getRes.body()
                if (body != null && !isRciError(body)) {
                    return body
                }
            }

            // 2. Fallback via POST /rci/. Build the command from the same normalized path.
            val cmd = fallbackCommand ?: pathToCommandMap(normalizedPath)
            val postRes = service.executeRci(listOf(mapOf("show" to cmd)))
            if (postRes.isSuccessful) {
                val pBody = postRes.body()
                if (pBody != null && !isRciError(pBody)) {
                    return extractFirstRciResult(pBody)
                }
            }

            null
        } catch (e: Exception) {
            AppLogger.logError("queryShow($normalizedPath)", e)
            null
        }
    }

    /**
     * Queries the HTTP status code of the direct GET /rci/show/<path> tree.
     * Returns 404 when the path is not supported by the running firmware.
     */
    suspend fun queryShowCode(path: String): Int {
        val normalizedPath = normalizeShowPath(path)
        return try {
            getService().queryShow(normalizedPath).code()
        } catch (e: Exception) {
            AppLogger.logError("queryShowCode($normalizedPath)", e)
            -1
        }
    }

    /**
     * Feature-support probe that mirrors what the web configurator does: fire the
     * direct GET /rci/show/<path> firstadian, and if it answers 404 (path not
     * found over that transport), retry the same tree through the POST /rci/ batch
     * ("{"show":{<command>}}"). Returns 404 only when BOTH transports reject the
     * path, so screens no longer show a false "feature is not supported" stub on
     * firmwares (e.g. KN-2311, fw 5.01.C.4.0-1) where the tree is only reachable
     * via the batch show used by the web UI.
     */
    suspend fun queryShowCodeWithFallback(path: String): Int {
        val normalizedPath = normalizeShowPath(path)
        return try {
            val service = getService()
            val getCode = service.queryShow(normalizedPath).code()
            if (getCode != 404) {
                return getCode
            }
            // GET returned 404. Try the POST batch transport before concluding
            // the path is unsupported (this is what the web configurator does).
            val cmd = pathToCommandMap(normalizedPath)
            val postRes = service.executeRci(listOf(mapOf("show" to cmd)))
            if (postRes.isSuccessful) {
                val body = postRes.body()
                if (body == null || !isRciError(body)) {
                    return postRes.code()
                }
            }
            getCode
        } catch (e: Exception) {
            AppLogger.logError("queryShowCodeWithFallback($normalizedPath)", e)
            -1
        }
    }

    /**
     * Query RCI show path returning raw text string (for non-JSON responses like "ip/rule").
     */
    suspend fun queryShowText(path: String): String? {
        val normalizedPath = normalizeShowPath(path)
        return try {
            // 1. Direct GET /rci/show/<path> as raw text.
            var text = getService().queryShowRaw(normalizedPath).let { res ->
                if (res.isSuccessful) res.body()?.string() else null
            }
            if (text != null) return text

            // 2. Fallback via POST /rci/ batch (same transport the web configurator
            //    uses; the direct GET answers 404 on KN-2311 / fw 5.01.C.4.0-1).
            val cmd = pathToCommandMap(normalizedPath)
            val postRes = getService().executeRci(listOf(mapOf("show" to cmd)))
            if (postRes.isSuccessful) {
                val pBody = postRes.body()
                if (pBody != null && !isRciError(pBody)) {
                    val first = extractFirstRciResult(pBody)
                    if (first != null) {
                        text = when {
                            first.isJsonPrimitive -> first.asString
                            first.isJsonArray -> first.asJsonArray.joinToString("\n") { it.asString }
                            else -> first.toString()
                        }
                        if (text.isNotBlank()) return text
                    }
                }
            }

            null
        } catch (e: Exception) {
            AppLogger.logError("queryShowText($normalizedPath)", e)
            null
        }
    }

    /**
     * Compatibility aliases left in older UI/parser code. These are normalized before
     * both GET /rci/show and POST /rci/ are attempted so the two transports use
     * exactly the same valid RCI tree.
     */
    private fun normalizeShowPath(path: String): String {
        val clean = path.trim().trim('/').replace(Regex("/+"), "/")
        return when (clean) {
            "ip/static" -> "sc/ip/static"
            "ip/access-list" -> "sc/interface/mac.access-list"
            "components/list" -> "components"
            // KN-2311 / KeeneticOS exposes the SMB service under its RCI tree
            // name "cifs" (the web configurator sends show cifs). The historical
            // "smb" alias in this app used to map onto nothing and made the SMB
            // loader/detail screens report the feature as unsupported.
            "smb" -> "cifs"
            "dlna" -> "dlna/status"
            "smb/dlna" -> "cifs/status"
            else -> clean
        }
    }

    /**
     * Reads the router system log. KeeneticOS exposes the syslog only through the
     * POST command `show log {"max-lines":N}` (the way the web configurator does);
     * there is no GET /rci/show/log tree, so a plain queryShow("log") produced
     * "not found" entries in the router log.
     */
    suspend fun queryLog(maxLines: Int = 300): JsonElement? {
        val command = mapOf("show" to mapOf("log" to mapOf("max-lines" to maxLines)))
        return try {
            val response = getService().executeRci(listOf(command))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && !isRciError(body)) {
                    extractFirstRciResult(body)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.logError("queryLog", e)
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

    fun isRciError(element: JsonElement?): Boolean {
        if (element == null) return false
        // Форма-список: [{"status":"error","code":...}] — так отвечает POST /rci/
        // на несуществующий путь (например show/interface/X/Y через GET).
        if (element.isJsonArray) {
            val arr = element.asJsonArray
            if (arr.size() == 0) return false
            val first = arr.get(0)
            if (first.isJsonObject) {
                val s = first.asJsonObject.get("status")
                if (s != null && s.isJsonPrimitive && s.asString.equals("error", ignoreCase = true)) {
                    return true
                }
                // Вложенная форма внутри элемента списка
                if (isRciError(first)) return true
            }
            return false
        }
        if (!element.isJsonObject) return false
        val obj = element.asJsonObject
        // Плоская форма: {"status":"error",...}
        obj.get("status")?.takeIf { it.isJsonPrimitive }?.let {
            if (it.asString.equals("error", ignoreCase = true)) return true
        }
        if (obj.has("status") && obj.get("status").isJsonArray) {
            val arr = obj.getAsJsonArray("status")
            if (arr.size() > 0 && arr.get(0).isJsonObject) {
                val sObj = arr.get(0).asJsonObject
                if (sObj.has("status") && sObj.get("status").asString.equals("error", ignoreCase = true)) {
                    return true
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
