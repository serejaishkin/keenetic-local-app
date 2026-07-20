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
        raw.entries
            // "Port" - это отдельные физические порты свитча (вложены и внутри
            // родительского GigabitEthernetX, и продублированы плоскими ключами
            // на верхнем уровне JSON) - пользователю неинтересны сами по себе.
            // "WifiStation" - интерфейс Wi-Fi клиентского режима (репитер),
            // обычно не используется и всегда down на большинстве роутеров.
            .filterNot { (_, obj) -> typeOf(obj) in setOf("Port", "WifiStation") }
            .map { (key, obj) -> toInterfaceInfo(key, obj) }
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
            up = up
        )
    }

    // ---- Wi-Fi ----

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
        // Явное поле диапазона (встречается на некоторых прошивках).
        str(obj, "band")?.let { return normalizeBand(it) }
        str(obj, "frequency")?.let { return normalizeBand(it) }

        // Реальные дампы Keenetic содержат подсказку в description,
        // например "5GHz Wi-Fi access point".
        str(obj, "description")?.let { desc ->
            if (desc.contains("5G", ignoreCase = true)) return "5 ГГц"
            if (desc.contains("2.4G", ignoreCase = true) || desc.contains("2,4G", ignoreCase = true)) return "2.4 ГГц"
        }

        // По каналу: >14 обычно означает диапазон 5 ГГц.
        obj.get("channel")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull()?.let { ch ->
            return if (ch > 14) "5 ГГц" else "2.4 ГГц"
        }

        // Фолбэк — по родительскому радио-модулю (для большинства моделей
        // Keenetic WifiMaster0 = 2.4 ГГц, WifiMaster1 = 5 ГГц; подтверждено
        // реальным дампом /rci/show/interface).
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

    /**
     * На реальном роутере шифрование - это плоское строковое поле "encryption"
     * ("wpa2", "wpa3", "" для отключённых/незащищённых сетей), без вложенного
     * объекта "security". Поле "auth-type" относится к 802.1x/RADIUS и не
     * определяет открытость сети, поэтому не используется здесь.
     */
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
