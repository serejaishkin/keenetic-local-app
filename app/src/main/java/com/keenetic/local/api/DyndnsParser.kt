package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object DyndnsParser {

    fun parseStatus(root: JsonElement?): DyndnsStatus {
        if (root == null || !root.isJsonObject) return DyndnsStatus()
        val dyndns = findKey(root, "dyndns") ?: return DyndnsStatus()
        return DyndnsStatus(
            enabled = dyndns.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            provider = str(dyndns, "provider") ?: "",
            hostname = str(dyndns, "hostname") ?: "",
            lastUpdate = str(dyndns, "last-update") ?: ""
        )
    }

    fun parseProfiles(root: JsonElement?): List<DyndnsProfile> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<DyndnsProfile>()
        val dyndns = findKey(root, "dyndns") ?: return emptyList()
        for ((key, value) in dyndns.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(DyndnsProfile(
                    name = key,
                    hostname = str(o, "hostname") ?: "",
                    username = str(o, "username") ?: "",
                    enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                ))
            }
        }
        return list
    }

    fun parseUpdaters(root: JsonElement?): List<DyndnsUpdater> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<DyndnsUpdater>()
        val dyndns = findKey(root, "dyndns") ?: return emptyList()
        for ((key, value) in dyndns.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(DyndnsUpdater(
                    name = key,
                    hostname = str(o, "hostname") ?: "",
                    lastUpdate = str(o, "last-update") ?: "",
                    status = str(o, "status") ?: ""
                ))
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

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
