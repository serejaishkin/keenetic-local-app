package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class VpnServerConfig(
    val enabled: Boolean = false,
    val interfaceName: String? = null,
    val poolStart: String? = null,
    val poolSize: String? = null,
    val nat: Boolean = false
)

/**
 * Разбор `{"show":{"sc":{"vpn-server":{...}}}}` - подтверждено реальным
 * HAR (REST, не SSH - в отличие от большинства других "show sc" команд
 * этот отдаёт чистый JSON через POST /rci/).
 */
object VpnServerParser {
    fun parse(root: JsonElement?): VpnServerConfig? {
        if (root == null || !root.isJsonObject) return null
        val vpn = findVpnServer(root.asJsonObject) ?: return null
        val config = vpn.getAsJsonObject("config")
        return VpnServerConfig(
            enabled = config?.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(vpn, "interface"),
            poolStart = vpn.getAsJsonObject("pool-range")?.let { str(it, "begin") },
            poolSize = vpn.getAsJsonObject("pool-range")?.let { str(it, "size") },
            nat = config?.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseToStatus(root: JsonElement): VpnServerStatus? {
        if (!root.isJsonObject) return null
        val vpn = findVpnServer(root.asJsonObject) ?: return null
        val config = vpn.getAsJsonObject("config")

        val enabled = config?.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        val iface = str(vpn, "interface") ?: ""

        // Detect VPN type from interface name or explicit field
        val vpnType = when {
            iface.contains("WireGuard", ignoreCase = true) -> "wireguard"
            iface.contains("SSTP", ignoreCase = true) -> "sstp"
            iface.contains("OpenVPN", ignoreCase = true) -> "openvpn"
            iface.contains("IPsec", ignoreCase = true) || iface.contains("L2TP", ignoreCase = true) -> "ipsec"
            vpn.get("wireguard")?.isJsonObject == true -> "wireguard"
            vpn.get("sstp")?.isJsonObject == true -> "sstp"
            vpn.get("openvpn")?.isJsonObject == true -> "openvpn"
            vpn.get("ipsec")?.isJsonObject == true || vpn.get("l2tp")?.isJsonObject == true -> "ipsec"
            else -> "unknown"
        }

        // Parse peers if WireGuard
        val peers = mutableListOf<VpnPeer>()
        vpn.get("wireguard")?.takeIf { it.isJsonObject }?.asJsonObject?.let { wg ->
            wg.get("peer")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { peerEl ->
                if (peerEl.isJsonObject) {
                    val p = peerEl.asJsonObject
                    peers.add(VpnPeer(
                        name = str(p, "name") ?: "",
                        publicKey = str(p, "public-key") ?: str(p, "publickey") ?: "",
                        endpoint = str(p, "endpoint") ?: "",
                        allowedIp = p.getAsJsonArray("allowed-ip")?.joinToString(", ") {
                            if (it.isJsonPrimitive) it.asString else ""
                        } ?: "",
                        bytesReceived = p.get("rx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                        bytesSent = p.get("tx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
                    ))
                }
            }
        }

        // Parse OpenVPN status
        var connectedClients = 0
        vpn.get("openvpn")?.takeIf { it.isJsonObject }?.asJsonObject?.let { ovpn ->
            ovpn.get("status")?.takeIf { it.isJsonObject }?.asJsonObject?.let { status ->
                connectedClients = status.get("connected")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            }
        }

        return VpnServerStatus(
            enabled = enabled,
            type = vpnType,
            interfaceName = iface,
            address = str(vpn, "address") ?: "",
            port = vpn.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            connectedClients = connectedClients,
            peers = peers
        )
    }

    private fun findVpnServer(obj: JsonObject): JsonObject? {
        obj.get("vpn-server")?.takeIf { it.isJsonObject }?.let { return it.asJsonObject }
        for ((_, v) in obj.entrySet()) {
            if (v.isJsonObject) {
                findVpnServer(v.asJsonObject)?.let { return it }
            }
        }
        return null
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
