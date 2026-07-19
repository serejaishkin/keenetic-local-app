package com.keenetic.local.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.security.MessageDigest

class KeeneticAuthInterceptor(
    private val login: String,
    private val password: String
) : Interceptor {

    private var sessionCookie: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // НЕ перехватываем /auth — иначе рекурсия!
        if (path == "/auth") {
            return chain.proceed(request)
        }

        // Если уже авторизованы — просто добавляем cookie
        if (sessionCookie != null) {
            val newRequest = request.newBuilder()
                .header("Cookie", sessionCookie!!)
                .build()
            return chain.proceed(newRequest)
        }

        // Шаг 1: Получаем challenge
        val authUrl = request.url.newBuilder().encodedPath("/auth").build()
        val authRequest = request.newBuilder().url(authUrl).build()
        val authResponse = chain.proceed(authRequest)

        if (authResponse.code != 401) {
            // Роутер без пароля — пропускаем оригинальный запрос
            return chain.proceed(request)
        }

        // Шаг 2: Парсим заголовки
        val realm = authResponse.header("X-NDM-Realm")
            ?: extractRealm(authResponse.header("WWW-Authenticate") ?: "")
            ?: "Keenetic"
        val challenge = authResponse.header("X-NDM-Challenge") ?: run {
            Log.e("KeeneticAuth", "No X-NDM-Challenge")
            return authResponse
        }
        val setCookie = authResponse.header("Set-Cookie") ?: run {
            Log.e("KeeneticAuth", "No Set-Cookie")
            return authResponse
        }

        // Берём "имя=значение" до первой ";"
        val cookie = setCookie.split(";").firstOrNull()?.trim() ?: run {
            Log.e("KeeneticAuth", "Cannot parse cookie")
            return authResponse
        }

        Log.d("KeeneticAuth", "realm=$realm, challenge=$challenge, cookie=$cookie")

        // Шаг 3: Считаем хеш
        val md5Hash = md5("$login:$realm:$password")
        val sha256Hash = sha256("$challenge$md5Hash")

        Log.d("KeeneticAuth", "md5=$md5Hash, sha256=$sha256Hash")

        // Шаг 4: POST /auth
        val jsonBody = """{"login":"$login","password":"$sha256Hash"}"""
        val loginRequest = request.newBuilder()
            .url(authUrl)
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Cookie", cookie)
            .build()

        val loginResponse = chain.proceed(loginRequest)
        Log.d("KeeneticAuth", "POST /auth response: ${loginResponse.code}")

        if (!loginResponse.isSuccessful) {
            Log.e("KeeneticAuth", "Login failed")
            return loginResponse
        }

        // Шаг 5: Сохраняем cookie и ПОВТОРЯЕМ оригинальный запрос
        sessionCookie = cookie
        Log.d("KeeneticAuth", "Success! Retrying ${request.url}")

        val finalRequest = request.newBuilder()
            .header("Cookie", cookie)
            .build()
        return chain.proceed(finalRequest)
    }

    private fun extractRealm(wwwAuth: String): String? {
        val regex = """realm="([^"]+)"""".toRegex()
        return regex.find(wwwAuth)?.groupValues?.get(1)
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
