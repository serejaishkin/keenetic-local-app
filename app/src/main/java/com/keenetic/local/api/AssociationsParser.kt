package com.keenetic.local.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Разбор ответов /rci/show/associations и /rci/show/ip/policy. Эти два
 * эндпоинта, в отличие от остальных в проекте, НЕ подтверждены реальным
 * HAR-дампом с роутера - только официальной документацией Keenetic и
 * общей схемой других show/-эндпоинтов. Реальная форма ответа может
 * отличаться в зависимости от прошивки, поэтому парсинг перебирает
 * несколько вероятных вариантов.
 */
object AssociationsParser {

    fun parse(root: JsonElement?): List<WifiAssoc> {
        if (root == null || root.isJsonNull) return emptyList()
        val result = mutableListOf<WifiAssoc>()

        when {
            root.isJsonArray -> root.asJsonArray.forEach { el ->
                if (el.isJsonObject) result += toAssoc(el.asJsonObject)
            }
            root.isJsonObject -> {
                val obj = root.asJsonObject
                obj.get("station")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                }
                if (result.isEmpty()) {
                    obj.entrySet().forEach { (_, value) ->
                        when {
                            value.isJsonArray -> value.asJsonArray.forEach { el ->
                                if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                            }
                            value.isJsonObject -> {
                                val inner = value.asJsonObject
                                inner.get("station")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                                    if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                                }
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private fun toAssoc(o: JsonObject): WifiAssoc = WifiAssoc(
        mac = str(o, "mac") ?: "",
        hostname = str(o, "hostname") ?: str(o, "name") ?: "",
        ip = str(o, "ip") ?: "",
        rssi = str(o, "rssi") ?: "",
        txrate = str(o, "txrate") ?: "",
        rxrate = str(o, "rxrate") ?: "",
        txbytes = long(o, "txbytes") ?: 0L,
        rxbytes = long(o, "rxbytes") ?: 0L,
        ap = str(o, "ap") ?: str(o, "interface") ?: ""
    )

    fun parsePolicyNames(root: JsonElement?): List<IpPolicy> {
        if (root == null || root.isJsonNull) return emptyList()
        val result = mutableListOf<IpPolicy>()

        when {
            root.isJsonObject -> {
                root.asJsonObject.entrySet().forEach { (key, value) ->
                    val desc = if (value.isJsonObject) str(value.asJsonObject, "description") else null
                    result += IpPolicy(name = key, description = desc?.takeIf { it.isNotBlank() })
                }
            }
            root.isJsonArray -> root.asJsonArray.forEach { el ->
                if (el.isJsonObject) {
                    val name = str(el.asJsonObject, "name")
                    val desc = str(el.asJsonObject, "description")
                    if (name != null) result += IpPolicy(name = name, description = desc?.takeIf { it.isNotBlank() })
                } else if (el.isJsonPrimitive) {
                    result += IpPolicy(name = el.asString)
                }
            }
        }
        return result
    }

    fun parseDhcpBindings(root: JsonElement?): List<DhcpBinding> {
        if (root == null || root.isJsonNull) return emptyList()
        val result = mutableListOf<DhcpBinding>()

        fun fromObj(o: JsonObject) {
            val mac = str(o, "mac") ?: ""
            val ip = str(o, "ip") ?: str(o, "address") ?: ""
            val hostname = str(o, "hostname") ?: str(o, "name") ?: ""
            val active = when {
                o.has("active") && o.get("active").isJsonPrimitive -> runCatching { o.get("active").asBoolean }.getOrDefault(false)
                str(o, "active")?.equals("true", ignoreCase = true) == true -> true
                str(o, "link")?.equals("up", ignoreCase = true) == true -> true
                o.has("expires") || o.has("dhcp") -> true
                else -> false
            }
            if (mac.isNotBlank() || ip.isNotBlank()) {
                result += DhcpBinding(mac = mac, ip = ip, hostname = hostname, active = active)
            }
        }

        fun fromArray(arr: JsonArray) {
            arr.forEach { el -> if (el.isJsonObject) fromObj(el.asJsonObject) }
        }

        when {
            root.isJsonArray -> fromArray(root.asJsonArray)
            root.isJsonObject -> {
                val obj = root.asJsonObject
                listOf("host", "binding", "lease", "leases").forEach { key ->
                    obj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.let { fromArray(it) }
                }
                // KeeneticOS 5.2: hotspot hosts contain online clients
                obj.get("hotspot")?.takeIf { it.isJsonObject }?.asJsonObject?.get("host")
                    ?.takeIf { it.isJsonArray }?.asJsonArray?.let { fromArray(it) }
                if (result.isEmpty()) {
                    obj.entrySet().forEach { (key, value) ->
                        if (value.isJsonObject) {
                            val o = value.asJsonObject
                            val mac = str(o, "mac") ?: key
                            val ip = str(o, "ip") ?: str(o, "address") ?: ""
                            val hostname = str(o, "hostname") ?: str(o, "name") ?: ""
                            if (mac.isNotBlank() || ip.isNotBlank()) {
                                result += DhcpBinding(mac = mac, ip = ip, hostname = hostname, active = false)
                            }
                        } else if (value.isJsonArray) {
                            fromArray(value.asJsonArray)
                        }
                    }
                }
            }
        }
        return result.distinctBy { "${it.mac}|${it.ip}" }
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString

    private fun long(o: JsonObject, field: String): Long? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asLong
}
