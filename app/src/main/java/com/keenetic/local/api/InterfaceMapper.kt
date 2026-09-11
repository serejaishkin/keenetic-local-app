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
                    txSpeedKbps = txSpeed,
                    order = intVal(obj, "order"),
                    channel = intVal(obj, "channel"),
                    channelWidth = runCatching { nested(obj, "channel", "width")?.asInt ?: 0 }.getOrDefault(0),
                    autoRescan = elementStr(nested(obj, "channel", "auto-rescan", "interval")) ?: "",
                    powerPercent = intVal(obj, "power").takeIf { it > 0 } ?: 100,
                    country = elementStr(nested(obj, "country-code", "code")) ?: "",
                    standard = elementStr(nested(obj, "compatibility", "0", "annex")) ?: "",
                    txBurst = boolVal(obj, "tx-burst"),
                    beamforming = boolVal(nested(obj, "beamforming", "explicit"), null),
                    qam256 = boolVal(obj, "vht"),
                    downlinkOfdma = boolVal(obj, "downlink-ofdma"),
                    uplinkOfdma = boolVal(obj, "uplink-ofdma"),
                    downlinkMumimo = boolVal(obj, "downlink-mumimo"),
                    uplinkMumimo = boolVal(obj, "uplink-mumimo"),
                    targetWaketime = boolVal(obj, "target-waketime"),
                    atfDisabled = boolVal(nested(obj, "atf", "disable"), null).let { disabled -> if (nested(obj, "atf") == null) true else disabled },
                    atfInbound = boolVal(nested(obj, "atf", "inbound"), null),
                    bandSteeringEnabled = boolVal(nested(obj, "band-steering", "enable"), null),
                    preferBand = elementStr(nested(obj, "band-steering", "prefer-band")) ?: "no-priority",
                    ssidHidden = boolVal(nested(obj, "ssid", "hide"), null),
                    wpsEnabled = boolVal(nested(obj, "wps", "enable"), null),
                    ftEnabled = boolVal(nested(obj, "ft", "enable"), null),
                    mdid = elementStr(nested(obj, "ft", "mdid")) ?: "",
                    iappKey = elementStr(nested(obj, "ft", "iapp", "key")) ?: "",
                    rrmEnabled = boolVal(nested(obj, "rrm", "enable"), null),
                    peerIsolation = boolVal(obj, "peer-isolation"),
                    macAccessMode = elementStr(obj.get("mac-access")) ?: "none"
                )
            }
            .sortedWith(compareBy({ it.type != "Bridge" && it.type != "Ethernet" }, { it.id }))
    }

    fun toVpnConnections(element: JsonElement?): List<VpnConnection> {
        if (element == null || element.isJsonNull) return emptyList()
        val rawMap = extractEntries(element)

        val vpnTypes = setOf(
            "proxy", "wireguard", "openvpn", "pptp", "l2tp", "sstp",
            "ike", "openconnect", "zerotier", "gre", "ipip", "eoip",
            "ppp", "pppoe", "ipv6to4", "6to4", "tunnel", "listener"
        )

        return rawMap.entries
            .filter { (key, obj) ->
                val type = (str(obj, "type") ?: "").lowercase()
                val lowerKey = key.lowercase()
                vpnTypes.contains(type) ||
                        lowerKey.startsWith("proxy") ||
                        lowerKey.startsWith("wireguard") ||
                        lowerKey.startsWith("wg") ||
                        lowerKey.startsWith("awg") ||
                        lowerKey.startsWith("openvpn") ||
                        lowerKey.startsWith("sstp") ||
                        lowerKey.startsWith("l2tp") ||
                        lowerKey.startsWith("pptp")
            }
            .map { (key, obj) ->
                val id = str(obj, "id") ?: key
                val rawType = str(obj, "type") ?: when {
                    key.startsWith("Proxy", ignoreCase = true) -> "Proxy"
                    key.startsWith("Wireguard", ignoreCase = true) || key.startsWith("wg", ignoreCase = true) -> "Wireguard"
                    key.startsWith("OpenVPN", ignoreCase = true) -> "OpenVPN"
                    key.startsWith("Sstp", ignoreCase = true) -> "SSTP"
                    key.startsWith("L2tp", ignoreCase = true) -> "L2TP"
                    key.startsWith("Pptp", ignoreCase = true) -> "PPTP"
                    else -> "VPN"
                }
                val description = str(obj, "description") ?: ""
                val state = str(obj, "state") ?: "unknown"
                val link = str(obj, "link") ?: state
                val isUp = link.equals("up", ignoreCase = true) || state.equals("up", ignoreCase = true)
                val ip = str(obj, "address") ?: str(obj, "ip")
                val rxBytes = longVal(obj, "rxbytes")
                val txBytes = longVal(obj, "txbytes")

                val proxyObj = when {
                    obj.has("proxy") && obj.get("proxy").isJsonObject -> obj.getAsJsonObject("proxy")
                    obj.has("sc") && obj.getAsJsonObject("sc").has("proxy") -> obj.getAsJsonObject("sc").getAsJsonObject("proxy")
                    else -> null
                }

                var proto: String? = null
                var upstream: String? = null

                if (proxyObj != null) {
                    proto = when {
                        proxyObj.has("protocol") && proxyObj.get("protocol").isJsonObject ->
                            str(proxyObj.getAsJsonObject("protocol"), "proto")
                        else -> str(proxyObj, "protocol")
                    }
                    upstream = when {
                        proxyObj.has("upstream") && proxyObj.get("upstream").isJsonObject ->
                            str(proxyObj.getAsJsonObject("upstream"), "server")
                                ?: str(proxyObj.getAsJsonObject("upstream"), "address")
                        else -> str(proxyObj, "upstream")
                    }
                }

                if (proto == null && rawType.equals("Proxy", ignoreCase = true)) {
                    proto = "SOCKS5"
                }

                if (upstream == null && obj.has("server")) {
                    upstream = str(obj, "server")
                }

                val displayName = when {
                    description.isNotBlank() -> description
                    rawType.equals("Proxy", ignoreCase = true) -> "$id ($rawType)"
                    else -> str(obj, "interface-name")?.takeIf { it.isNotBlank() } ?: id
                }

                VpnConnection(
                    id = id,
                    name = displayName,
                    type = rawType,
                    isUp = isUp,
                    state = state,
                    ip = ip,
                    protocol = proto,
                    upstream = upstream,
                    rxBytes = rxBytes,
                    txBytes = txBytes
                )
            }
            .sortedWith(compareBy({ !it.isUp }, { it.id }))
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

    private fun elementStr(el: JsonElement?): String? =
        el?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let { p ->
            when {
                p.isString -> p.asString
                p.isBoolean -> p.asBoolean.toString()
                p.isNumber -> p.asString
                else -> null
            }
        }

    private fun nested(obj: JsonElement?, vararg path: String): JsonElement? {
        var cur: JsonElement? = obj
        for (p in path) {
            val o = cur as? JsonObject ?: return null
            cur = o.get(p)
        }
        return cur
    }

    private fun boolVal(el: JsonElement?, field: String? = null): Boolean {
        val target = if (field != null) (el as? JsonObject)?.get(field) else el
        return elementStr(target)?.let { it == "true" || it == "yes" || it == "1" } ?: false
    }

    private fun longVal(obj: JsonObject?, field: String): Long {
        val el = obj?.get(field) ?: return 0L
        return runCatching { el.asLong }.getOrDefault(0L)
    }

    private fun intVal(obj: JsonObject?, field: String): Int {
        val el = obj?.get(field) ?: return 0
        return runCatching { el.asInt }.getOrDefault(0)
    }

    fun toMobileTraffic(element: JsonElement?): MobileTraffic {
        if (element == null || element.isJsonNull) return MobileTraffic()
        val obj = when {
            element.isJsonObject && element.asJsonObject.has("interface") ->
                firstEntry(element.asJsonObject.getAsJsonObject("interface"))
            element.isJsonObject -> element.asJsonObject
            else -> null
        } ?: return MobileTraffic()
        val tc = obj.getAsJsonObject("traffic-counter") ?: return MobileTraffic()

        val actionArr = runCatching { tc.getAsJsonArray("action") }.getOrNull()
        var smsWarning = false
        var smsLimit = false
        var disconnect = false
        var smsPhone = ""
        var smsMessage = ""
        if (actionArr != null) {
            for (el in actionArr) {
                if (!el.isJsonObject) continue
                val act = el.asJsonObject
                val trigger = str(act, "trigger") ?: ""
                val sms = runCatching { act.getAsJsonObject("sms-alert") }.getOrNull()
                val phoneList = sms?.get("phone")?.takeIf { it.isJsonArray }?.asJsonArray
                val phone = phoneList?.firstOrNull()?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                val message = str(sms, "message") ?: ""
                if (phone.isNotBlank()) {
                    smsPhone = phone
                    smsMessage = message
                }
                when (trigger) {
                    "threshold" -> smsWarning = true
                    "limit" -> {
                        smsLimit = true
                        disconnect = act.getOrNull("disconnect")?.asBoolean == true
                    }
                }
            }
        }

        val monthly = runCatching { tc.getAsJsonObject("monthly") }.getOrNull()
        val multiplier = runCatching { tc.get("multiplier").asLong }.getOrDefault(1048576L)

        return MobileTraffic(
            enable = tc.getOrNull("enable")?.asBoolean == true,
            limit = runCatching { tc.get("limit").asLong }.getOrDefault(0L),
            unit = str(tc, "unit") ?: "MB",
            multiplier = multiplier,
            dayOfMonth = (runCatching { monthly?.get("day-of-month")?.asInt }.getOrDefault(1)) ?: 1,
            cycleResetEnabled = monthly != null,
            threshold = runCatching { tc.get("threshold").asInt }.getOrDefault(90),
            smsWarningEnabled = smsWarning,
            smsLimitEnabled = smsLimit,
            smsPhone = smsPhone,
            smsMessage = smsMessage,
            disconnect = disconnect
        )
    }

    private fun JsonObject.getOrNull(field: String): JsonElement? =
        if (has(field)) get(field) else null

    private fun firstEntry(obj: JsonObject): JsonObject? =
        obj.entrySet().firstOrNull()?.value?.takeIf { it.isJsonObject }?.asJsonObject

    fun toSwitchPorts(element: JsonElement?): List<SwitchPort> {
        if (element == null || element.isJsonNull) return emptyList()
        val rawMap = extractEntries(element)
        return rawMap.entries
            .filter { (_, obj) ->
                val type = str(obj, "type") ?: ""
                type.equals("Port", ignoreCase = true) || type.equals("SwitchPort", ignoreCase = true)
            }
            .map { (key, obj) ->
                SwitchPort(
                    id = str(obj, "id") ?: key,
                    name = str(obj, "description") ?: key,
                    state = str(obj, "state") ?: str(obj, "link") ?: "down",
                    speed = str(obj, "speed") ?: ""
                )
            }
    }
}
