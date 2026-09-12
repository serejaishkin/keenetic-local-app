package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object DeviceListParser {

    /**
     * Parses the detailed device list defensively. KeeneticOS versions may return
     * devices as an object map, an array, or nested under host/client/device keys.
     * LAN devices must not be dropped just because the response does not contain
     * the exact "devices" object used by another firmware version.
     */
    fun parseFull(root: JsonElement?): List<DeviceListEntryFull> {
        if (root == null || root.isJsonNull) return emptyList()

        val candidates = mutableListOf<Pair<String, JsonObject>>()

        fun addObject(key: String, value: JsonObject) {
            if (looksLikeDevice(value)) candidates += key to value
        }

        fun collectContainer(element: JsonElement?, fallbackPrefix: String) {
            when {
                element == null || element.isJsonNull -> Unit
                element.isJsonArray -> element.asJsonArray.forEachIndexed { index, item ->
                    if (item.isJsonObject) {
                        val o = item.asJsonObject
                        val key = str(o, "id") ?: str(o, "mac") ?: str(o, "name") ?: "${fallbackPrefix}_$index"
                        addObject(key, o)
                    }
                }
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                    if (value.isJsonObject) addObject(key, value.asJsonObject)
                    else if (value.isJsonArray) collectContainer(value, key)
                }
            }
        }

        if (root.isJsonArray) {
            collectContainer(root, "device")
        } else {
            val obj = root.asJsonObject
            val containerKeys = listOf("devices", "device", "clients", "client", "hosts", "host", "entries", "list")
            var foundContainer = false
            for (key in containerKeys) {
                val value = obj.get(key)
                if (value != null && !value.isJsonNull) {
                    foundContainer = true
                    collectContainer(value, key)
                }
            }

            // Some firmware returns the device map directly without a wrapper.
            if (!foundContainer) collectContainer(root, "device")

            // Also walk nested objects. This catches responses such as
            // {"ip":{"hotspot":{"host":{...}}}} without treating arbitrary
            // configuration objects as devices.
            fun walk(element: JsonElement?) {
                if (element == null || !element.isJsonObject) return
                val o = element.asJsonObject
                if (looksLikeDevice(o)) {
                    val key = str(o, "id") ?: str(o, "mac") ?: str(o, "name") ?: "device_${candidates.size}"
                    addObject(key, o)
                }
                o.entrySet().forEach { (_, value) ->
                    when {
                        value.isJsonObject -> walk(value)
                        value.isJsonArray -> value.asJsonArray.forEach { item -> if (item.isJsonObject) walk(item) }
                    }
                }
            }
            walk(root)
        }

        return candidates
            .map { (key, o) -> toDevice(key, o) }
            .filter { it.mac.isNotBlank() || it.ip.isNotBlank() || it.hostname.isNotBlank() }
            .distinctBy {
                when {
                    it.mac.isNotBlank() -> "mac:${it.mac.lowercase()}"
                    it.ip.isNotBlank() -> "ip:${it.ip}"
                    else -> "name:${it.name.lowercase()}"
                }
            }
            .sortedWith(compareByDescending<DeviceListEntryFull> { it.online }.thenBy { it.name.lowercase() })
    }

    private fun looksLikeDevice(o: JsonObject): Boolean {
        val mac = str(o, "mac") ?: str(o, "hwaddr") ?: str(o, "hardware-address")
        val ip = str(o, "ip") ?: str(o, "address") ?: str(o, "ipv4")
        val hostname = str(o, "hostname") ?: str(o, "host") ?: str(o, "name")
        val iface = str(o, "interface") ?: str(o, "iface") ?: str(o, "ifname") ?: str(o, "interface-name")
        val type = str(o, "type") ?: str(o, "connection") ?: str(o, "media")
        return !mac.isNullOrBlank() || !ip.isNullOrBlank() ||
            (!hostname.isNullOrBlank() && (!iface.isNullOrBlank() || !type.isNullOrBlank()))
    }

    private fun toDevice(key: String, o: JsonObject): DeviceListEntryFull {
        val mac = str(o, "mac") ?: str(o, "hwaddr") ?: str(o, "hardware-address") ?: ""
        val ip = str(o, "ip") ?: str(o, "address") ?: str(o, "ipv4") ?: ""
        val hostname = str(o, "hostname") ?: str(o, "host") ?: str(o, "name") ?: ""
        val ifaceRaw = str(o, "interface") ?: str(o, "iface") ?: str(o, "ifname") ?: str(o, "interface-name") ?: ""
        val typeRaw = str(o, "type") ?: str(o, "connection") ?: str(o, "media") ?: ""

        val interfaceName = normalizeInterface(ifaceRaw, typeRaw)
        val type = normalizeType(typeRaw, ifaceRaw)

        return DeviceListEntryFull(
            name = key.ifBlank { hostname.ifBlank { mac.ifBlank { "Устройство" } } },
            mac = mac,
            ip = ip,
            hostname = hostname,
            interfaceName = interfaceName,
            type = type,
            online = bool(o, "online") || bool(o, "active") || isUp(str(o, "state")) || isUp(str(o, "link")),
            policy = str(o, "policy") ?: str(o, "policy-name") ?: "",
            schedule = str(o, "schedule") ?: ""
        )
    }

    private fun normalizeInterface(raw: String, type: String): String {
        if (raw.isNotBlank()) {
            return when {
                raw.contains("GigabitEthernet", ignoreCase = true) -> raw
                raw.contains("Ethernet", ignoreCase = true) -> raw
                raw.contains("WifiMaster", ignoreCase = true) -> raw
                raw.contains("WiFi", ignoreCase = true) || raw.contains("Wi-Fi", ignoreCase = true) -> raw
                raw.contains("LAN", ignoreCase = true) -> raw
                else -> raw
            }
        }
        return when {
            type.contains("ethernet", ignoreCase = true) || type.contains("wired", ignoreCase = true) || type.contains("lan", ignoreCase = true) -> "LAN"
            type.contains("wifi", ignoreCase = true) || type.contains("wireless", ignoreCase = true) || type.contains("802.11", ignoreCase = true) -> "Wi-Fi"
            else -> ""
        }
    }

    private fun normalizeType(raw: String, iface: String): String {
        if (raw.isNotBlank()) return raw
        return when {
            iface.contains("Wifi", ignoreCase = true) || iface.contains("Wi-Fi", ignoreCase = true) -> "Wi-Fi"
            iface.contains("Ethernet", ignoreCase = true) || iface.contains("GigabitEthernet", ignoreCase = true) || iface.contains("LAN", ignoreCase = true) -> "Ethernet"
            else -> ""
        }
    }

    private fun isUp(value: String?): Boolean = when (value?.lowercase()) {
        "up", "online", "active", "connected", "running", "on" -> true
        else -> false
    }

    private fun bool(o: JsonObject, field: String): Boolean =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asBoolean }.getOrDefault(false) } ?: false

    fun parseMedia(root: JsonElement?): List<MediaStorage> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<MediaStorage>()
        fun parsePartitions(o: JsonObject): MutableList<MediaPartition> {
            val partitions = mutableListOf<MediaPartition>()
            fun addPart(po: JsonObject) {
                partitions.add(MediaPartition(
                    uuid = str(po, "uuid") ?: "",
                    label = str(po, "label") ?: "",
                    fstype = str(po, "fstype") ?: "",
                    size = po.get("total")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L)
                        ?: po.get("size")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L,
                    free = po.get("free")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L,
                    state = str(po, "state") ?: ""
                ))
            }
            val partEl = o.get("partition")
            when {
                partEl != null && partEl.isJsonArray ->
                    partEl.asJsonArray.forEach { if (it.isJsonObject) addPart(it.asJsonObject) }
                partEl != null && partEl.isJsonObject ->
                    partEl.asJsonObject.entrySet().forEach { (_, v) -> if (v.isJsonObject) addPart(v.asJsonObject) }
            }
            return partitions
        }
        fun addDrive(key: String, o: JsonObject) {
            val partitions = parsePartitions(o)
            val first = partitions.firstOrNull()
            list.add(MediaStorage(
                name = key,
                label = first?.label ?: str(o, "product") ?: key,
                mounted = str(o, "state") ?: "",
                fstype = first?.fstype ?: "",
                total = o.get("size")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L)
                    ?: partitions.sumOf { it.size },
                free = partitions.sumOf { it.free },
                partitions = partitions
            ))
        }
        val rootObj = root.asJsonObject
        val media = findKey(root, "media")
        if (media != null) {
            for ((key, value) in media.entrySet()) {
                if (value.isJsonObject) addDrive(key, value.asJsonObject)
            }
            if (list.isNotEmpty()) return list
        }
        for ((key, value) in rootObj.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                if (o.has("partition") || o.has("bus") || o.has("state")) addDrive(key, o)
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
