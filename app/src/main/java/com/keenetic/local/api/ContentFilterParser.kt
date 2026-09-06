package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object ContentFilterParser {

    fun parseNextdns(root: JsonElement?): NextdnsProfileFull {
        if (root == null || !root.isJsonObject) return NextdnsProfileFull()
        val nd = findKey(root, "nextdns") ?: return NextdnsProfileFull()
        return NextdnsProfileFull(
            id = str(nd, "id") ?: "",
            name = str(nd, "name") ?: "",
            enabled = nd.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            configured = nd.get("configured")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseSafedns(root: JsonElement?): SafednsProfileFull {
        if (root == null || !root.isJsonObject) return SafednsProfileFull()
        val sd = findKey(root, "safedns") ?: return SafednsProfileFull()
        return SafednsProfileFull(
            id = str(sd, "id") ?: "",
            name = str(sd, "name") ?: "",
            enabled = sd.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            configured = sd.get("configured")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseSkydns(root: JsonElement?): SkydnsProfileFull {
        if (root == null || !root.isJsonObject) return SkydnsProfileFull()
        val sky = findKey(root, "skydns") ?: return SkydnsProfileFull()
        return SkydnsProfileFull(
            id = str(sky, "id") ?: "",
            name = str(sky, "name") ?: "",
            enabled = sky.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            configured = sky.get("configured")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
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
