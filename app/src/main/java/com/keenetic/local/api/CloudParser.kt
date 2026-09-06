package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object CloudParser {

    fun parseStatus(root: JsonElement?): CloudStatus {
        if (root == null || !root.isJsonObject) return CloudStatus()
        val cloud = findKey(root, "cloud") ?: return CloudStatus()
        return CloudStatus(
            enabled = cloud.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            cloudType = str(cloud, "type") ?: "",
            address = str(cloud, "address") ?: ""
        )
    }

    fun parseNdmp(root: JsonElement?): CloudNdmp {
        if (root == null || !root.isJsonObject) return CloudNdmp()
        val ndmp = findKey(root, "ndmp") ?: return CloudNdmp()
        return CloudNdmp(
            enabled = ndmp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            status = str(ndmp, "status") ?: "",
            prepared = ndmp.get("prepared")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
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
