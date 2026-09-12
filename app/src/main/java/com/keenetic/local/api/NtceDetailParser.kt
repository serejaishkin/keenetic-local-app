package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object NtceDetailParser {

    fun parseApplications(root: JsonElement?): List<NtceApp> {
        if (root == null) return emptyList()
        val list = mutableListOf<NtceApp>()
        fun addApp(o: JsonObject) {
            list.add(NtceApp(
                name = str(o, "short") ?: str(o, "long") ?: "",
                category = str(o, "group-long") ?: str(o, "category") ?: "",
                priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addApp(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val obj = root.asJsonObject
        obj.get("application")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
            if (it.isJsonObject) addApp(it.asJsonObject)
        }
        if (list.isNotEmpty()) return list
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
        if (root == null) return emptyList()
        val list = mutableListOf<NtceHost>()
        fun addHost(o: JsonObject) {
            list.add(NtceHost(
                ip = str(o, "ip") ?: "",
                mac = str(o, "mac") ?: "",
                hostname = str(o, "hostname") ?: str(o, "name") ?: "",
                os = str(o, "os") ?: "",
                priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addHost(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val obj = root.asJsonObject
        obj.get("host")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
            if (it.isJsonObject) addHost(it.asJsonObject)
        }
        if (list.isNotEmpty()) return list
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
        if (root == null) return emptyList()
        val list = mutableListOf<NtceOs>()
        fun addOs(o: JsonObject) {
            list.add(NtceOs(
                name = str(o, "long") ?: str(o, "short") ?: "",
                hostsCount = o.get("hosts-count")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addOs(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val obj = root.asJsonObject
        obj.get("os")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
            if (it.isJsonObject) addOs(it.asJsonObject)
        }
        if (list.isNotEmpty()) return list
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
        if (root == null) return emptyList()
        val list = mutableListOf<NtceGroup>()
        fun addGroup(o: JsonObject) {
            list.add(NtceGroup(
                name = str(o, "short") ?: str(o, "long") ?: "",
                description = str(o, "long") ?: str(o, "groupset-long-id") ?: ""
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addGroup(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val obj = root.asJsonObject
        obj.get("group")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
            if (it.isJsonObject) addGroup(it.asJsonObject)
        }
        if (list.isNotEmpty()) return list
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
