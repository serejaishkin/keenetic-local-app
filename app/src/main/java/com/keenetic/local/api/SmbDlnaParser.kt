package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object SmbDlnaParser {

    fun parseSmb(root: JsonElement?): SmbConfig {
        val o = configObject(root, "smb") ?: return SmbConfig()
        return SmbConfig(
            enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            name = str(o, "name") ?: "",
            workgroup = str(o, "workgroup") ?: "",
            description = str(o, "description") ?: ""
        )
    }

    fun parseDlna(root: JsonElement?): DlnaConfig {
        val o = configObject(root, "dlna") ?: return DlnaConfig()
        return DlnaConfig(
            enabled = o.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            name = str(o, "name") ?: "",
            port = o.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            type = str(o, "type") ?: ""
        )
    }

    private fun configObject(root: JsonElement?, key: String): JsonObject? {
        if (root == null || !root.isJsonObject) return null
        val obj = root.asJsonObject
        obj.get(key)?.takeIf { it.isJsonObject }?.let { return it.asJsonObject }
        if (obj.has("enable")) return obj
        for ((_, v) in obj.entrySet()) {
            configObject(v, key)?.takeIf { it.has("enable") }?.let { return it }
        }
        return null
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}