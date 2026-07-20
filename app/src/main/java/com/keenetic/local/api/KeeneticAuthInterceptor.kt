package com.keenetic.local.api

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

class KeeneticAuthInterceptor(
    private val login: String,
    private val password: String
) : Interceptor {

    private var sessionCookie: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Если уже есть cookie — используем её
        if (sessionCookie != null) {
            val newRequest = request.newBuilder()
                .addHeader("Cookie", sessionCookie!!)
                .build()
            return chain.proceed(newRequest)
        }

        // Иначе авторизуемся
        val authResponse = chain.proceed(
            request.newBuilder().url(request.url.newBuilder().pathSegments(listOf("auth")).build()).build()
        )

        if (authResponse.code == 401) {
            val realm = authResponse.header("X-NDM-Realm") ?: "Keenetic"
            val challenge = authResponse.header("X-NDM-Challenge") ?: return authResponse
            val cookie = authResponse.header("Set-Cookie")?.split(";")?.firstOrNull() ?: return authResponse

            val md5 = md5("$login:$realm:$password")
            val sha256 = sha256("$challenge$md5")

            val loginRequest = request.newBuilder()
                .url(request.url.newBuilder().pathSegments(listOf("auth")).build())
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), """{"login":"$login","password":"$sha256"}"""))
                .addHeader("Cookie", cookie)
                .build()

            val loginResponse = chain.proceed(loginRequest)
            if (loginResponse.isSuccessful) {
                sessionCookie = cookie
            }
            return loginResponse
        }

        return authResponse
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