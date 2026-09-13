package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Parses the opkg configuration from `show/sc/opkg`:
 *   { "disk": { "disk": "storage:/", "no": false }, "initrc": { "path": "", "no": true } }
 */
object OpkgParser {

    fun parse(root: JsonElement?): OpkgConfig {
        if (root == null || !root.isJsonObject) return OpkgConfig()
        val o = root.asJsonObject
        val disk = o.get("disk")?.takeIf { it.isJsonObject }?.asJsonObject
        val initrc = o.get("initrc")?.takeIf { it.isJsonObject }?.asJsonObject
        return OpkgConfig(
            diskNo = disk?.get("no")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            diskId = disk?.get("disk")?.let(OpkgParser::str) ?: "",
            initrcPath = initrc?.get("path")?.let(OpkgParser::str) ?: "",
            initrcNo = initrc?.get("no")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
        )
    }

    fun str(el: JsonElement): String? = el.takeIf { it.isJsonPrimitive }?.asString
}