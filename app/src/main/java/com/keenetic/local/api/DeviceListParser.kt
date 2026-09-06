package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object DeviceListParser {

    fun parseFull(root: JsonElement?): List<DeviceListEntryFull> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<DeviceListEntryFull>()
        val devices = findKey(root, "devices") ?: findKey(root, "device") ?: return emptyList()
        for ((key, value) in devices.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(DeviceListEntryFull(
                    name = key,
                    mac = str(o, "mac") ?: "",
                    ip = str(o, "ip") ?: "",
                    hostname = str(o, "hostname") ?: "",
                    interfaceName = str(o, "interface") ?: "",
                    type = str(o, "type") ?: "",
                    online = o.get("online")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
                    policy = str(o, "policy") ?: "",
                    schedule = str(o, "schedule") ?: ""
                ))
            }
        }
        return list
    }

    fun parseMedia(root: JsonElement?): List<MediaStorage> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<MediaStorage>()
        val media = findKey(root, "media") ?: return emptyList()
        for ((key, value) in media.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                val partitions = mutableListOf<MediaPartition>()
                o.getAsJsonArray("partition")?.forEach { p ->
                    if (p.isJsonObject) {
                        val po = p.asJsonObject
                        partitions.add(MediaPartition(
                            uuid = str(po, "uuid") ?: "",
                            label = str(po, "label") ?: "",
                            fstype = str(po, "fstype") ?: "",
                            size = po.get("size")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                            free = po.get("free")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                            state = str(po, "state") ?: ""
                        ))
                    }
                }
                list.add(MediaStorage(
                    name = key,
                    label = str(o, "label") ?: "",
                    mounted = str(o, "mounted") ?: "",
                    fstype = str(o, "fstype") ?: "",
                    total = o.get("total")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                    free = o.get("free")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                    partitions = partitions
                ))
            }
        }
        return list
    }

    fun parseComponents(root: JsonElement?): List<ComponentInfo> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<ComponentInfo>()
        val components = findKey(root, "components") ?: findKey(root, "component") ?: return emptyList()
        for ((key, value) in components.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(ComponentInfo(
                    name = key,
                    title = str(o, "title") ?: key,
                    installed = o.get("installed")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
                    available = o.get("available")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
                    version = str(o, "version") ?: "",
                    description = str(o, "description") ?: ""
                ))
            }
        }
        return list
    }

    fun parseObjectGroup(root: JsonElement?): List<ObjectGroupFqdn> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<ObjectGroupFqdn>()
        val groups = findKey(root, "object-group") ?: findKey(root, "fqdn") ?: return emptyList()
        for ((key, value) in groups.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                val members = mutableListOf<String>()
                o.getAsJsonArray("member")?.forEach { m ->
                    if (m.isJsonPrimitive) members.add(m.asString)
                    else if (m.isJsonObject) {
                        m.asJsonObject.get("host")?.takeIf { it.isJsonPrimitive }?.asString?.let { members.add(it) }
                    }
                }
                list.add(ObjectGroupFqdn(
                    name = key,
                    members = members
                ))
            }
        }
        return list
    }

    fun parseMonitor(root: JsonElement?): MonitorStatus {
        if (root == null || !root.isJsonObject) return MonitorStatus()
        val monitor = findKey(root, "monitor") ?: return MonitorStatus()
        return MonitorStatus(
            active = monitor.get("active")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(monitor, "interface") ?: "",
            filter = str(monitor, "filter") ?: ""
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
