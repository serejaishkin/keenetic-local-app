package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class SiteSurveyResult(
    val essid: String? = null,
    val address: String? = null,
    val encryption: String? = null,
    val channel: Int? = null,
    val rssi: Int? = null,
    val quality: Int? = null
)

/**
 * Разбор `{"show":{"site-survey":{"name":"WifiMasterX"}}}` - подтверждено
 * реальным HAR, отдаёт чистый JSON через REST /rci/.
 */
object SiteSurveyParser {
    fun parse(root: JsonElement?): List<SiteSurveyResult> {
        if (root == null) return emptyList()
        val siteSurvey = findKey(root, "site-survey") ?: return emptyList()
        val cells = siteSurvey.getAsJsonArray("ap_cell") ?: return emptyList()
        return cells.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            SiteSurveyResult(
                essid = str(o, "essid"),
                address = str(o, "address"),
                encryption = str(o, "encryption"),
                channel = o.get("channel")?.takeIf { it.isJsonPrimitive }?.asInt,
                rssi = o.get("rssi")?.takeIf { it.isJsonPrimitive }?.asInt,
                quality = o.get("quality")?.takeIf { it.isJsonPrimitive }?.asInt
            )
        }.filter { !it.essid.isNullOrBlank() }
            .distinctBy { it.essid }
            .sortedByDescending { it.rssi ?: -999 }
    }

    private fun findKey(element: JsonElement, key: String): JsonObject? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            obj.get(key)?.takeIf { it.isJsonObject }?.let { return it.asJsonObject }
            for ((_, v) in obj.entrySet()) {
                findKey(v, key)?.let { return it }
            }
        }
        return null
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
