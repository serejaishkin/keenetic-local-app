package com.keenetic.local.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {

    fun detectRouterGatewayIp(context: Context): String? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val linkProps: LinkProperties? = cm.getLinkProperties(activeNetwork)
                    if (linkProps != null) {
                        for (route in linkProps.routes) {
                            val gateway = route.gateway
                            if (route.isDefaultRoute && gateway != null && !gateway.isAnyLocalAddress) {
                                val host = gateway.hostAddress
                                if (!host.isNullOrBlank() && !host.startsWith("0.0.0.0") && !host.startsWith("::")) {
                                    return host
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.logError("detectRouterGatewayIp cm", e)
        }

        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wm?.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                val ip = dhcp.gateway
                val gatewayStr = String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
                if (gatewayStr != "0.0.0.0") {
                    return gatewayStr
                }
            }
        } catch (e: Exception) {
            AppLogger.logError("detectRouterGatewayIp wm", e)
        }

        try {
            BufferedReader(FileReader("/proc/net/route")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split("\\s+".toRegex())
                    if (tokens.size > 2 && tokens[1] == "00000000") {
                        val gwHex = tokens[2]
                        val gwLong = gwHex.toLong(16)
                        val b1 = (gwLong and 0xFF).toInt()
                        val b2 = (gwLong shr 8 and 0xFF).toInt()
                        val b3 = (gwLong shr 16 and 0xFF).toInt()
                        val b4 = (gwLong shr 24 and 0xFF).toInt()
                        val ip = "$b1.$b2.$b3.$b4"
                        if (ip != "0.0.0.0") return ip
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore proc route reading errors
        }

        return null
    }

    fun getSuggestedRouterIps(context: Context): List<String> {
        val detected = detectRouterGatewayIp(context)
        val defaults = listOf("192.168.1.1", "192.168.0.1", "172.16.1.1", "my.keenetic.net")
        return (listOfNotNull(detected) + defaults).distinct()
    }

    suspend fun testHostReachable(host: String, port: Int = 80, timeoutMs: Int = 1500): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
}
