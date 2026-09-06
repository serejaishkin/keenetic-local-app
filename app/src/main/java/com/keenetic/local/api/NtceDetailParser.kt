package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object NtceDetailParser {

    fun parseApplications(root: JsonElement?): List<NtceApp> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NtceApp>()
        val apps = findKey(root, "applications") ?: findKey(root, "app") ?: return emptyList()
        for ((key, value) in apps.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(NtceApp(
                    name = key,
                    category = str(o, "category") ?: "",
                    priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                ))
            }
        }
        return list
    }

    fun parseHosts(root: JsonElement?): List<NtceHost> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NtceHost>()
        val hosts = findKey(root, "hosts") ?: findKey(root, "host") ?: return emptyList()
        for ((key, value) in hosts.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(NtceHost(
                    ip = key,
                    mac = str(o, "mac") ?: "",
                    hostname = str(o, "hostname") ?: "",
                    os = str(o, "os") ?: "",
                    priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                ))
            }
        }
        return list
    }

    fun parseOses(root: JsonElement?): List<NtceOs> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NtceOs>()
        val oses = findKey(root, "oses") ?: findKey(root, "os") ?: return emptyList()
        for ((key, value) in oses.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(NtceOs(
                    name = key,
                    hostsCount = o.get("hosts-count")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                ))
            }
        }
        return list
    }

    fun parseGroups(root: JsonElement?): List<NtceGroup> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NtceGroup>()
        val groups = findKey(root, "groups") ?: findKey(root, "group") ?: return emptyList()
        for ((key, value) in groups.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(NtceGroup(
                    name = key,
                    description = str(o, "description") ?: ""
                ))
            }
        }
        return list
    }

    fun parseFilterProfiles(root: JsonElement?): List<NtceFilterProfileFull> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NtceFilterProfileFull>()
        val profiles = findKey(root, "filter") ?: return emptyList()
        for ((key, value) in profiles.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(NtceFilterProfileFull(
                    id = key,
                    name = str(o, "name") ?: key,
                    priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
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
