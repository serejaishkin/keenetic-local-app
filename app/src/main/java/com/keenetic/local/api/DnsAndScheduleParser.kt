package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class ScheduleInfo(
    val name: String,
    val description: String? = null,
    val actionsCount: Int = 0
)

data class DnsServerInfo(
    val address: String? = null,
    val interfaceName: String? = null
)

data class DohServerInfo(
    val url: String,
    val interfaceName: String? = null
)

data class DotServerInfo(
    val address: String? = null,
    val fqdn: String? = null,
    val interfaceName: String? = null
)

/**
 * Разбор `show sc schedule` и `show sc ip name-server` /
 * `show sc dns-proxy https upstream` - подтверждено реальным HAR.
 */
object DnsAndScheduleParser {

    fun parseSchedules(root: JsonElement?): List<ScheduleInfo> {
        val sc = findKey(root, "schedule") ?: return emptyList()
        return sc.entrySet().mapNotNull { (name, value) ->
            if (!value.isJsonObject) return@mapNotNull null
            val o = value.asJsonObject
            ScheduleInfo(
                name = name,
                description = o.get("description")?.takeIf { it.isJsonPrimitive }?.asString,
                actionsCount = o.getAsJsonArray("action")?.size() ?: 0
            )
        }
    }

    /**
     * Plain DNS-серверы. Реальная структура show/ip/name-server:
     *   "name-server": { "server": [ { "address": "...", "service": "Dhcp::...", "interface": "..." } ] }
     * Ручные `ip name-server` в show/sc/ip/name-server:
     *   "name-server": [ { "address": "...", "domain": "", "interface": "..." } ]
     * Учитываем оба варианта.
     */
    fun parseNameServers(root: JsonElement?): List<DnsServerInfo> {
        val arr = when {
            root?.isJsonArray == true -> root.asJsonArray.toList()
            else -> {
                val ns = findKey(root, "name-server")
                ns?.get("server")?.takeIf { it.isJsonArray }?.asJsonArray?.toList()
                    ?: findArrayKey(root, "name-server")
                    ?: return emptyList()
            }
        }
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            DnsServerInfo(
                address = o.get("address")?.takeIf { it.isJsonPrimitive }?.asString,
                interfaceName = o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }.distinctBy { it.address to it.interfaceName }
    }

    fun parseDohUpstream(root: JsonElement?): List<String> {
        return parseDohServers(root).map {
            val intf = it.interfaceName
            if (intf.isNullOrBlank()) it.url else "${it.url} ($intf)"
        }
    }

    fun parseDohServers(root: JsonElement?): List<DohServerInfo> {
        val arr = when {
            root?.isJsonArray == true -> root.asJsonArray.toList()
            else -> findArrayKey(root, "upstream") ?: return emptyList()
        }
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val url = o.get("url")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            DohServerInfo(
                url = url,
                interfaceName = o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }.distinctBy { it.url to it.interfaceName }
    }

    fun parseDotUpstream(root: JsonElement?): List<String> {
        return parseDotServers(root).map {
            val intf = it.interfaceName
            val label = if (it.fqdn.isNullOrBlank()) it.address ?: "" else it.fqdn!!
            if (intf.isNullOrBlank()) label else "$label ($intf)"
        }
    }

    fun parseDotServers(root: JsonElement?): List<DotServerInfo> {
        val arr = when {
            root?.isJsonArray == true -> root.asJsonArray.toList()
            else -> findArrayKey(root, "upstream") ?: return emptyList()
        }
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val fqdn = o.get("fqdn")?.takeIf { it.isJsonPrimitive }?.asString
            val address = o.get("address")?.takeIf { it.isJsonPrimitive }?.asString
            if (fqdn.isNullOrBlank() && address.isNullOrBlank()) return@mapNotNull null
            DotServerInfo(
                address = address,
                fqdn = fqdn,
                interfaceName = o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }.distinctBy { it.address to it.fqdn to it.interfaceName }
    }

    /** Ручные plain DNS (show/sc/ip/name-server) - массив {address, domain, interface}. */
    fun parseScNameServers(root: JsonElement?): List<DnsServerInfo> {
        val arr = when {
            root?.isJsonArray == true -> root.asJsonArray.toList()
            else -> findArrayKey(root, "name-server") ?: return emptyList()
        }
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            DnsServerInfo(
                address = o.get("address")?.takeIf { it.isJsonPrimitive }?.asString,
                interfaceName = o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }
    }

    fun parseDnsFilterPresets(root: JsonElement?): List<DnsFilterPreset> {
        if (root == null) return emptyList()
        val list = mutableListOf<DnsFilterPreset>()

        fun extractPresets(obj: JsonObject) {
            for ((key, value) in obj.entrySet()) {
                if (value.isJsonObject) {
                    val o = value.asJsonObject
                    val name = o.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: o.get("title")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: key
                    val desc = o.get("description")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                    val provider = o.get("provider")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                    val enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false)
                        ?: o.get("active")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false

                    // Determine provider type from key/name
                    val type = when {
                        key.contains("adguard", ignoreCase = true) || name.contains("AdGuard", ignoreCase = true) -> "adguard"
                        key.contains("nextdns", ignoreCase = true) || name.contains("NextDNS", ignoreCase = true) -> "nextdns"
                        key.contains("cloudflare", ignoreCase = true) || name.contains("Cloudflare", ignoreCase = true) -> "cloudflare"
                        key.contains("safe", ignoreCase = true) || name.contains("SafeDNS", ignoreCase = true) -> "safe"
                        else -> "custom"
                    }

                    list.add(DnsFilterPreset(
                        id = key,
                        name = name,
                        provider = provider,
                        description = desc,
                        type = type,
                        enabled = enabled
                    ))
                } else if (value.isJsonArray) {
                    value.asJsonArray.forEach { el ->
                        if (el.isJsonObject) extractPresets(el.asJsonObject)
                    }
                }
            }
        }

        when {
            root.isJsonObject -> {
                val obj = root.asJsonObject
                // Try common response structures
                obj.get("preset")?.takeIf { it.isJsonObject }?.let { extractPresets(it.asJsonObject) }
                obj.get("presets")?.takeIf { it.isJsonObject }?.let { extractPresets(it.asJsonObject) }
                if (list.isEmpty()) extractPresets(obj)
            }
            root.isJsonArray -> {
                root.asJsonArray.forEach { el ->
                    if (el.isJsonObject) extractPresets(el.asJsonObject)
                }
            }
        }

        return list
    }

    fun parseDnsFilterProfiles(root: JsonElement?): List<DnsFilterProfile> {
        if (root == null) return emptyList()
        val list = mutableListOf<DnsFilterProfile>()

        fun extractProfiles(obj: JsonObject) {
            for ((key, value) in obj.entrySet()) {
                if (value.isJsonObject) {
                    val o = value.asJsonObject
                    val name = o.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: o.get("title")?.takeIf { it.isJsonPrimitive }?.asString ?: key
                    val desc = o.get("description")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                    val preset = o.get("preset")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                    val enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true)
                        ?: o.get("active")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true

                    val assigned = mutableListOf<String>()
                    o.get("assign")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                        if (el.isJsonPrimitive) assigned.add(el.asString)
                        else if (el.isJsonObject) {
                            el.asJsonObject.get("interface")?.takeIf { it.isJsonPrimitive }?.asString?.let { assigned.add(it) }
                        }
                    }
                    o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString?.let { assigned.add(it) }

                    list.add(DnsFilterProfile(
                        id = key,
                        name = name,
                        presetId = preset,
                        description = desc,
                        enabled = enabled,
                        assignedTo = assigned
                    ))
                } else if (value.isJsonArray) {
                    value.asJsonArray.forEach { el ->
                        if (el.isJsonObject) extractProfiles(el.asJsonObject)
                    }
                }
            }
        }

        when {
            root.isJsonObject -> {
                val obj = root.asJsonObject
                obj.get("profile")?.takeIf { it.isJsonObject }?.let { extractProfiles(it.asJsonObject) }
                obj.get("profiles")?.takeIf { it.isJsonObject }?.let { extractProfiles(it.asJsonObject) }
                if (list.isEmpty()) extractProfiles(obj)
            }
            root.isJsonArray -> {
                root.asJsonArray.forEach { el ->
                    if (el.isJsonObject) extractProfiles(el.asJsonObject)
                }
            }
        }

        return list
    }

    private fun findKey(element: JsonElement?, key: String): JsonObject? {
        if (element == null || !element.isJsonObject) return null
        val obj = element.asJsonObject
        obj.get(key)?.takeIf { it.isJsonObject }?.let { return it.asJsonObject }
        for ((_, v) in obj.entrySet()) {
            findKey(v, key)?.let { return it }
        }
        return null
    }

    private fun findArrayKey(element: JsonElement?, key: String): List<JsonElement>? {
        if (element == null) return null
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            obj.get(key)?.takeIf { it.isJsonArray }?.let { return it.asJsonArray.toList() }
            for ((_, v) in obj.entrySet()) {
                findArrayKey(v, key)?.let { return it }
            }
        }
        return null
    }
}
