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
        // root ожидается как единственный элемент из ответа executeRci -
        // ищем vpn-server на любом уровне вложенности show.sc.vpn-server
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
