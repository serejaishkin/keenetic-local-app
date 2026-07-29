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

    fun parseNameServers(root: JsonElement?): List<DnsServerInfo> {
        val arr = findArrayKey(root, "name-server") ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            DnsServerInfo(
                address = o.get("address")?.takeIf { it.isJsonPrimitive }?.asString,
                interfaceName = o.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }
    }

    fun parseDohUpstream(root: JsonElement?): List<String> {
        val arr = findArrayKey(root, "upstream") ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            el.asJsonObject.get("url")?.takeIf { it.isJsonPrimitive }?.asString
        }
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
