package com.keenetic.local.api

import com.google.gson.JsonObject

/**
 * Keenetic RCI не документирует JSON-схему `/rci/show/interface` официально,
 * и разные прошивки/модели отдают чуть разный набор полей. Поэтому весь
 * разбор здесь сделан "защитно" — пробуем несколько вероятных названий
 * полей и не падаем, если чего-то нет.
 *
 * Если после подключения реального роутера что-то по-прежнему не совпадает —
 * пришли сюда сырой JSON одного элемента (curl/SSH `show interface` с одной
 * точкой доступа), и маппинг легко уточнить под конкретную прошивку.
 */
object InterfaceMapper {

    /** Человекочитаемые имена для типовых системных интерфейсов Keenetic. */
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
        raw.map { (key, obj) -> toInterfaceInfo(key, obj) }
            .sortedWith(compareBy({ it.type != "AccessPoint" }, { it.id }))

    fun toWifiNetworks(raw: Map<String, JsonObject>): List<WifiNetwork> =
        raw.entries
            .filter { (_, obj) -> typeOf(obj).equals("AccessPoint", ignoreCase = true) }
            // Keenetic резервирует до 7 виртуальных AP на радиомодуль; реально
            // настроенные всегда имеют непустое description (см. реальный дамп
            // /rci/show/interface). Пустые слоты без description и без ssid
            // отфильтровываем, иначе в списке будет десяток пустых карточек.
            .filter { (_, obj) -> !str(obj, "description").isNullOrBlank() || !str(obj, "ssid").isNullOrBlank() }
            .map { (key, obj) -> toWifiNetwork(key, obj) }

    // ---- отдельный интерфейс ----

    private fun toInterfaceInfo(key: String, obj: JsonObject): InterfaceInfo {
        val id = str(obj, "id") ?: key
        val type = typeOf(obj)
        val description = str(obj, "description")
        val state = str(obj, "state")
        val link = str(obj, "link")
        val connected = str(obj, "connected")
        val address = str(obj, "address")
        val up = (link ?: state)?.equals("up", ignoreCase = true) == true

        val displayName = when {
            !description.isNullOrBlank() -> description
            type.equals("AccessPoint", ignoreCase = true) -> wifiDisplayName(id, obj)
            else -> KNOWN_NAMES[id] ?: id
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
            up = up
        )
    }

    // ---- Wi-Fi ----

    private fun toWifiNetwork(key: String, obj: JsonObject): WifiNetwork {
        val id = str(obj, "id") ?: key
        val ssid = str(obj, "ssid") ?: str(obj, "essid") ?: "(SSID неизвестен)"
        val state = str(obj, "state")
        val link = str(obj, "link")
        val enabled = (link ?: state)?.equals("up", ignoreCase = true) == true
        val guest = id.contains("Guest", ignoreCase = true) ||
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
        // Некоторые прошивки отдают явное поле диапазона.
        str(obj, "band")?.let { return normalizeBand(it) }
        str(obj, "frequency")?.let { return normalizeBand(it) }

        // Иначе — по каналу: >14 обычно означает диапазон 5 ГГц.
        obj.get("channel")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()?.let { ch ->
            return if (ch > 14) "5 ГГц" else "2.4 ГГц"
        }

        // Фолбэк — по родительскому радио-модулю (для большинства моделей
        // Keenetic WifiMaster0 = 2.4 ГГц, WifiMaster1 = 5 ГГц, но это не
        // гарантировано на 100% для всех моделей).
        return when {
            id.contains("WifiMaster0", ignoreCase = true) -> "2.4 ГГц"
            id.contains("WifiMaster1", ignoreCase = true) -> "5 ГГц"
            else -> "—"
        }
    }

    private fun normalizeBand(raw: String): String = when {
        raw.contains("2.4") -> "2.4 ГГц"
        raw.contains("5") -> "5 ГГц"
        else -> raw
    }

    private fun securityOf(obj: JsonObject): String {
        val security = obj.getAsJsonObject("security")
        val encTokens = mutableSetOf<String>()

        security?.get("encryption")?.let { collectTokens(it, encTokens) }
        security?.get("authentication")?.let { collectTokens(it, encTokens) }
        obj.get("authentication")?.let { collectTokens(it, encTokens) }
        obj.get("encryption")?.let { collectTokens(it, encTokens) }

        if (encTokens.isEmpty()) {
            // Явный признак открытой сети на некоторых прошивках.
            val openFlag = str(obj, "authentication")?.equals("none", ignoreCase = true) == true ||
                str(security, "encryption")?.equals("none", ignoreCase = true) == true
            return if (openFlag) "Открыто" else "—"
        }

        return encTokens.joinToString("/") { it.uppercase() }
    }

    private fun collectTokens(element: com.google.gson.JsonElement, into: MutableSet<String>) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { into.add(it.asString) }
            element.isJsonPrimitive -> into.add(element.asString)
        }
    }

    private fun typeOf(obj: JsonObject): String = str(obj, "type") ?: ""

    private fun str(obj: JsonObject?, field: String): String? =
        obj?.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
