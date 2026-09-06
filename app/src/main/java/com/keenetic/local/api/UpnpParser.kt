package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object UpnpParser {

    fun parseRedirects(root: JsonElement?): List<UpnpRedirect> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<UpnpRedirect>()
        val upnp = findKey(root, "upnp") ?: return emptyList()
        for ((key, value) in upnp.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(UpnpRedirect(
                    name = key,
                    proto = str(o, "proto") ?: "",
                    externalPort = str(o, "external-port") ?: "",
                    internalIp = str(o, "internal-ip") ?: "",
                    internalPort = str(o, "internal-port") ?: "",
                    enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                ))
            }
        }
        return list
    }

    fun parsePinholes(root: JsonElement?): List<UpnpPinhole> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<UpnpPinhole>()
        val upnp = findKey(root, "upnp") ?: return emptyList()
        for ((key, value) in upnp.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(UpnpPinhole(
                    name = key,
                    proto = str(o, "proto") ?: "",
                    port = str(o, "port") ?: "",
                    internalIp = str(o, "internal-ip") ?: "",
                    enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
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
