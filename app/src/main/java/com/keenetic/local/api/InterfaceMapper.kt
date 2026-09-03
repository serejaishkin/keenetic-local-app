package com.keenetic.local.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Парсер интерфейсов и Wi-Fi сетей роутеров Keenetic.
 * Поддерживает как объекты (Map<id, JsonObject>), так и массивы от /rci/show/interface.
 */
object InterfaceMapper {

    private val KNOWN_NAMES = mapOf(
        "GigabitEthernet0" to "Интернет (WAN)",
        "GigabitEthernet0/0" to "LAN 1",
        "GigabitEthernet0/1" to "LAN 2",
        "GigabitEthernet0/2" to "LAN 3",
        "GigabitEthernet0/3" to "LAN 4",
        "GigabitEthernet0/4" to "LAN 5",
        "Bridge0" to "Домашняя сеть (Bridge0)",
        "WifiMaster0" to "Wi-Fi модуль 2.4 ГГц",
        "WifiMaster1" to "Wi-Fi модуль 5 ГГц",
        "UsbLte0" to "USB-модем",
        "PPPoE0" to "PPPoE-подключение"
    )

    fun toInterfaceList(element: JsonElement?): List<RouterInterface> {
        if (element == null || element.isJsonNull) return emptyList()
        val rawMap = extractEntries(element)

        return rawMap.entries
            .filterNot { (_, obj) -> 
                val type = str(obj, "type") ?: ""
                type.equals("Port", ignoreCase = true) || type.equals("WifiStation", ignoreCase = true)
            }
            .map { (key, obj) ->
                val id = str(obj, "id") ?: key
                val type = str(obj, "type") ?: "Ethernet"
                val description = str(obj, "description") ?: ""
                val state = str(obj, "state") ?: "unknown"
                val link = str(obj, "link") ?: state
                val isUp = link.equals("up", ignoreCase = true) || state.equals("up", ignoreCase = true)
                val ip = str(obj, "address") ?: str(obj, "ip")
                val mask = str(obj, "mask")
                val uptime = longVal(obj, "uptime")
                val rxBytes = longVal(obj, "rxbytes")
                val txBytes = longVal(obj, "txbytes")
                val rxSpeed = longVal(obj, "rxspeed") / 1000
                val txSpeed = longVal(obj, "txspeed") / 1000

                val displayName = when {
                    description.isNotBlank() -> description
                    KNOWN_NAMES.containsKey(id) -> KNOWN_NAMES[id]!!
                    else -> str(obj, "interface-name")?.takeIf { it.isNotBlank() } ?: id
                }

                RouterInterface(
                    id = id,
                    name = displayName,
                    description = description.ifBlank { "$type интерфейс" },
                    type = type,
                    state = state,
                    isUp = isUp,
                    ip = ip,
                    mask = mask,
                    uptime = uptime,
                    rxBytes = rxBytes,
                    txBytes = txBytes,
                    rxSpeedKbps = rxSpeed,
                    txSpeedKbps = txSpeed
                )
            }
            .sortedWith(compareBy({ it.type != "Bridge" && it.type != "Ethernet" }, { it.id }))
    }

    fun toWifiNetworks(element: JsonElement?): List<WifiNetworkInfo> {
        if (element == null || element.isJsonNull) return emptyList()
        val rawMap = extractEntries(element)

        return rawMap.entries
            .filter { (_, obj) ->
                val type = str(obj, "type") ?: ""
                type.equals("AccessPoint", ignoreCase = true) ||
                        obj.has("ssid") ||
                        obj.has("essid")
            }
            .filter { (key, obj) ->
                val desc = str(obj, "description")
                val ssid = str(obj, "ssid") ?: str(obj, "essid")
                !desc.isNullOrBlank() || !ssid.isNullOrBlank()
            }
            .map { (key, obj) ->
                val id = str(obj, "id") ?: key
                val ssid = str(obj, "ssid") ?: str(obj, "essid") ?: "Keenetic-Wi-Fi"
                val band = bandOf(id, obj)
                val state = str(obj, "state")
                val link = str(obj, "link")
                val enabled = (link ?: state)?.equals("up", ignoreCase = true) == true
                val channel = intVal(obj, "channel").let { if (it > 0) it else if (band.contains("5")) 36 else 6 }
                val security = securityOf(obj)

                WifiNetworkInfo(
                    id = id,
                    ssid = ssid,
                    band = band,
                    enabled = enabled,
                    channel = channel,
                    security = security,
                    clientsCount = 0
                )
            }
            .distinctBy { it.ssid + it.band }
    }

    private fun extractEntries(element: JsonElement): Map<String, JsonObject> {
        val map = mutableMapOf<String, JsonObject>()
        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                for ((k, v) in obj.entrySet()) {
                    if (v.isJsonObject) {
                        map[k] = v.asJsonObject
                    }
                }
            }
            element.isJsonArray -> {
                element.asJsonArray.forEach { item ->
                    if (item.isJsonObject) {
                        val o = item.asJsonObject
                        val id = str(o, "id") ?: str(o, "name") ?: "if_${map.size}"
                        map[id] = o
                    }
                }
            }
        }
        return map
    }

    private fun bandOf(id: String, obj: JsonObject): String {
        str(obj, "band")?.let { return normalizeBand(it) }
        str(obj, "frequency")?.let { return normalizeBand(it) }

        val desc = str(obj, "description") ?: ""
        if (desc.contains("5G", ignoreCase = true)) return "5 ГГц"
        if (desc.contains("2.4G", ignoreCase = true) || desc.contains("2,4G", ignoreCase = true)) return "2.4 ГГц"

        val channel = intVal(obj, "channel")
        if (channel > 14) return "5 ГГц"
        if (channel in 1..14) return "2.4 ГГц"

        return when {
            id.contains("WifiMaster0", ignoreCase = true) -> "2.4 ГГц"
            id.contains("WifiMaster1", ignoreCase = true) -> "5 ГГц"
            else -> "2.4 + 5 ГГц"
        }
    }

    private fun normalizeBand(raw: String): String = when {
        raw.contains("2.4") || raw.contains("2,4") -> "2.4 ГГц"
        raw.contains("5") -> "5 ГГц"
        else -> raw
    }

    private fun securityOf(obj: JsonObject): String {
        val encryption = str(obj, "encryption") ?: str(obj, "security") ?: str(obj, "auth-type")
        return when {
            encryption.isNullOrBlank() -> "WPA2-PSK"
            encryption.contains("wpa3", ignoreCase = true) -> "WPA3-PSK"
            encryption.contains("wpa2", ignoreCase = true) -> "WPA2-PSK"
            encryption.equals("none", ignoreCase = true) || encryption.equals("open", ignoreCase = true) -> "Открытая"
            else -> encryption.uppercase()
        }
    }

    private fun str(obj: JsonObject?, field: String): String? {
        val el = obj?.get(field) ?: return null
        return if (el.isJsonPrimitive) el.asString else null
    }

    private fun longVal(obj: JsonObject?, field: String): Long {
        val el = obj?.get(field) ?: return 0L
        return runCatching { el.asLong }.getOrDefault(0L)
    }

    private fun intVal(obj: JsonObject?, field: String): Int {
        val el = obj?.get(field) ?: return 0
        return runCatching { el.asInt }.getOrDefault(0)
    }
}
