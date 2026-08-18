package com.keenetic.local.api

import com.google.gson.JsonObject

/**
 * Keenetic RCI не документирует JSON-схему `/rci/show/interface` официально,
 * и разные прошивки/модели отдают чуть разный набор полей. Поэтому весь
 * разбор здесь сделан "защитно" — пробуем несколько вероятных названий
 * полей и не падаем, если чего-то нет.
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

    fun toInterfaceList(raw: Map<String, JsonObject>): List<InterfaceInfo> =
        raw.entries
            .filterNot { (_, obj) -> typeOf(obj) in setOf("Port", "WifiStation") }
            .map { (key, obj) -> toInterfaceInfo(key, obj) }
            .sortedWith(compareBy({ it.type != "AccessPoint" }, { it.id }))

    fun toWifiNetworks(raw: Map<String, JsonObject>): List<WifiNetwork> =
        raw.entries
            .filter { (_, obj) -> typeOf(obj).equals("AccessPoint", ignoreCase = true) }
            .filter { (_, obj) -> !str(obj, "description").isNullOrBlank() || !str(obj, "ssid").isNullOrBlank() }
            .map { (key, obj) -> toWifiNetwork(key, obj) }

    /**
     * Преобразует плоские объекты type=Port из /rci/show/interface
     * в модели физических портов свитча.
     *
     * На разных моделях Keenetic номер порта может быть представлен как
     * port/number, поэтому используем несколько безопасных вариантов.
     */
    fun toSwitchPorts(raw: Map<String, JsonObject>): List<SwitchPort> =
        raw.entries
            .filter { (_, obj) -> typeOf(obj).equals("Port", ignoreCase = true) }
            .map { (key, obj) ->
                val id = str(obj, "id") ?: key
                val number = str(obj, "port")
                    ?: str(obj, "number")
                    ?: id.substringAfterLast('/').takeIf { it.isNotBlank() }
                    ?: id

                SwitchPort(
                    id = id,
                    label = portLabel(number),
                    link = str(obj, "link") ?: str(obj, "state"),
                    speed = str(obj, "speed") ?: str(obj, "rate"),
                    duplex = str(obj, "duplex"),
                    roleFor = str(obj, "for") ?: str(obj, "role") ?: str(obj, "interface")
                )
            }
            .sortedWith(compareBy({ portSortKey(it.label) }, { it.id }))

    private fun portLabel(raw: String): String {
        val n = raw.filter { it.isDigit() }.toIntOrNull()
        return if (n != null) n.toString() else raw
    }

    private fun portSortKey(label: String): Int =
        label.toIntOrNull() ?: Int.MAX_VALUE

    private fun toInterfaceInfo(key: String, obj: JsonObject): InterfaceInfo {
        val id = str(obj, "id") ?: key
        val type = typeOf(obj)
        val description = str(obj, "description")
        val state = str(obj, "state")
        val link = str(obj, "link")
        val connected = str(obj, "connected")
        val address = str(obj, "address")
        val mac = str(obj, "mac")
        val mask = str(obj, "mask")
        val up = (link ?: state)?.equals("up", ignoreCase = true) == true

        val displayName = when {
            !description.isNullOrBlank() -> description
            type.equals("AccessPoint", ignoreCase = true) -> wifiDisplayName(id, obj)
            else -> {
                val ifName = str(obj, "interface-name")
                KNOWN_NAMES[id] ?: (ifName?.takeIf { it != id && it.isNotBlank() }) ?: id
            }
        }

        return InterfaceInfo(
            id = id,
            displayName = displayName,
            type = type,
            description = description,
            state = state,
            link = link,
            connected = connected,
            address = address,
            up = up,
            mac = mac,
            mask = mask
        )
    }

    private fun toWifiNetwork(key: String, obj: JsonObject): WifiNetwork {
        val id = str(obj, "id") ?: key
        val ifName = str(obj, "interface-name")
        val ssid = str(obj, "ssid") ?: str(obj, "essid") ?: "(SSID не настроен)"
        val state = str(obj, "state")
        val link = str(obj, "link")
        val enabled = (link ?: state)?.equals("up", ignoreCase = true) == true
        val guest = (ifName?.contains("Guest", ignoreCase = true) == true) ||
            id.contains("Guest", ignoreCase = true) ||
            (str(obj, "description")?.contains("гост", ignoreCase = true) == true) ||
            (str(obj, "description")?.contains("guest", ignoreCase = true) == true)

        return WifiNetwork(
            id = id,
            ssid = ssid,
            band = bandOf(id, obj),
            security = securityOf(obj),
            enabled = enabled,
            guest = guest
        )
    }

    private fun wifiDisplayName(id: String, obj: JsonObject): String {
        val ssid = str(obj, "ssid") ?: str(obj, "essid")
        return when {
            !ssid.isNullOrBlank() -> ssid
            id.contains("Guest", ignoreCase = true) -> "Гостевая сеть"
            else -> "Wi-Fi сеть"
        }
    }

    private fun bandOf(id: String, obj: JsonObject): String {
        str(obj, "band")?.let { return normalizeBand(it) }
        str(obj, "frequency")?.let { return normalizeBand(it) }
        str(obj, "description")?.let { desc ->
            if (desc.contains("5G", ignoreCase = true)) return "5 ГГц"
            if (desc.contains("2.4G", ignoreCase = true) || desc.contains("2,4G", ignoreCase = true)) return "2.4 ГГц"
        }
        obj.get("channel")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()?.let { ch ->
            return if (ch > 14) "5 ГГц" else "2.4 ГГц"
        }
        return when {
            id.contains("WifiMaster0", ignoreCase = true) -> "2.4 ГГц"
            id.contains("WifiMaster1", ignoreCase = true) -> "5 ГГц"
            else -> "—"
        }
    }

    private fun normalizeBand(raw: String): String = when {
        raw.contains("2.4") || raw.contains("2,4") -> "2.4 ГГц"
        raw.contains("5") -> "5 ГГц"
        else -> raw
    }

    private fun securityOf(obj: JsonObject): String {
        val encryption = str(obj, "encryption")
        return when {
            encryption.isNullOrBlank() -> "Не защищено"
            else -> encryption.uppercase()
        }
    }

    private fun typeOf(obj: JsonObject): String = str(obj, "type") ?: ""

    private fun str(obj: JsonObject?, field: String): String? =
        obj?.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
