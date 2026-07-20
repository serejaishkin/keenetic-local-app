package com.keenetic.local.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object AutoDiscovery {

    data class DiscoveredRouter(
        val ip: String,
        val hostname: String? = null,
        val reachable: Boolean = false
    )

    suspend fun discover(baseIp: String = "192.168.1"): List<DiscoveredRouter> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredRouter>()
        val jobs = (1..254).map { i ->
            val ip = "$baseIp.$i"
            try {
                val addr = InetAddress.getByName(ip)
                if (addr.isReachable(800)) {
                    // Проверяем, отвечает ли на /rci/show/system
                    val url = URL("http://$ip/rci/show/system")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 1500
                    conn.readTimeout = 1500
                    conn.requestMethod = "GET"
                    val isKeenetic = conn.responseCode in 200..401 // 401 тоже ок, значит auth включен
                    conn.disconnect()
                    if (isKeenetic) {
                        DiscoveredRouter(ip, addr.hostName, true)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
        jobs.filterNotNull()
    }

    suspend fun quickCheck(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/rci/show/system")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            val ok = conn.responseCode in 200..401
            conn.disconnect()
            ok
        } catch (e: Exception) {
            false
        }
    }
}