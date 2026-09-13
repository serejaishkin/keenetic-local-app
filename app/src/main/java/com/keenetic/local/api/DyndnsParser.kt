package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Actual router response for `show dyndns` is a JSON ARRAY of profile entries:
 *   [ { "type":"", "profile":"_WEBADMIN", "status":"unknown", "status6":"unknown",
 *       "regtime":"Thu Jan  1 03:00:00 1970", "domain":"", "message":"", "message6":"" } ]
 * And `show dyndns/updaters` returns:
 *   { "updater": [ { "type":"regru", "url":"...", "api":"..." }, ... ] }
 */
object DyndnsParser {

    fun parseStatus(root: JsonElement?): DyndnsStatus {
        val entry = firstEntry(root) ?: return DyndnsStatus()
        return DyndnsStatus(
            profileName = str(entry, "profile") ?: "",
            provider = str(entry, "type") ?: "",
            hostname = str(entry, "domain") ?: "",
            regtime = str(entry, "regtime") ?: "",
            status = str(entry, "status") ?: "",
            status6 = str(entry, "status6") ?: "",
            message = str(entry, "message") ?: "",
            message6 = str(entry, "message6") ?: ""
        )
    }

    /**
     * `sc/dyndns` returns: { "profile": { "_WEBADMIN": { "send-address": true } } }
     */
    fun parseSc(root: JsonElement?): Boolean {
        if (root == null || !root.isJsonObject) return true
        val profile = root.asJsonObject.get("profile")?.takeIf { it.isJsonObject }?.asJsonObject ?: return true
        for ((_, value) in profile.entrySet()) {
            if (value.isJsonObject) {
                val sa = value.asJsonObject.get("send-address")
                if (sa?.isJsonPrimitive == true) return sa.asBoolean
            }
        }
        return true
    }

    fun parseProfiles(root: JsonElement?): List<DyndnsProfile> {
        if (root == null || !root.isJsonArray) return emptyList()
        val list = mutableListOf<DyndnsProfile>()
        for (item in root.asJsonArray) {
            if (!item.isJsonObject) continue
            val o = item.asJsonObject
            list.add(DyndnsProfile(
                name = str(o, "profile") ?: "",
                hostname = str(o, "domain") ?: "",
                provider = str(o, "type") ?: ""
            ))
        }
        return list
    }

    fun parseUpdaters(root: JsonElement?): List<DyndnsUpdater> {
        if (root == null || !root.isJsonObject) return emptyList()
        val updaters = root.asJsonObject.get("updater")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        val list = mutableListOf<DyndnsUpdater>()
        for (item in updaters) {
            if (!item.isJsonObject) continue
            val o = item.asJsonObject
            list.add(DyndnsUpdater(
                type = str(o, "type") ?: "",
                url = str(o, "url") ?: "",
                api = str(o, "api") ?: ""
            ))
        }
        return list
    }

    private fun firstEntry(root: JsonElement?): JsonObject? {
        if (root == null) return null
        if (root.isJsonArray && root.asJsonArray.size() > 0 && root.asJsonArray.get(0).isJsonObject) {
            return root.asJsonArray.get(0).asJsonObject
        }
        if (root.isJsonObject) return root.asJsonObject
        return null
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}