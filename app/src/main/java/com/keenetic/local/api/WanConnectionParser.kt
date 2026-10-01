package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Полное состояние Ethernet-подключения (WAN) как в веб-интерфейсе
 * («Подключения к интернету по Ethernet-кабелю») и приложении PACT.
 *
 * Источники (подтверждено живыми данными KN-2311, fw 5.1.5):
 * - state: узел интерфейса из `show interface` (напр. GigabitEthernet0/Vlan4):
 *   description, address/mask, mtu, mac, auth-type, connected/state, uptime,
 *   security-level, global/defaultgw/priority, ipv6.
 * - stanza: строки running-config секции `interface <id>`:
 *   schedule, order (по умолчанию 0), ip address dhcp/static, ip mtu,
 *   ip dhcp client hostname/dns-routes, ip [no] name-servers, mac address ...,
 *   ping-check profile ..., up / down.
 */
data class SwitchPortInfo(
    val label: String = "",
    val linkUp: Boolean = false,
    val speed: String = "",
    val duplex: String = "",
    val role: String = ""
)

data class WanConnection(
    val id: String = "",
    // --- state (show interface) ---
    val description: String = "",
    val connected: Boolean = false,
    val linkUp: Boolean = false,
    val address: String = "",
    val mask: String = "",
    val uptimeSec: Long = 0,
    val stateMtu: Int = 0,
    val stateMac: String = "",
    val authType: String = "none",
    val securityLevel: String = "",
    val globalPriority: Int = 0,
    val defaultGw: Boolean = false,
    val ipv6Up: Boolean = false,
    // --- config (running-config) ---
    val adminUp: Boolean = true,
    val schedule: String = "",
    val order: Int = 0,
    val ipMode: String = "dhcp", // dhcp | static | off
    val staticIp: String = "",
    val staticMask: String = "",
    val staticGateway: String = "",
    val hostname: String = "",
    val dhcpDnsRoutes: Boolean = true,
    val useNameServers: Boolean = true,
    val cfgMtu: Int = 0,
    val macMode: String = "factory", // factory | manual | random
    val macScope: String = "wan", // wan | lan (для factory)
    val macManual: String = "",
    val pingCheckProfile: String = "",
    val dyndns: String = "",
    val igmpUpstream: Boolean = false,
    // --- порты родительского свитча (только отображение) ---
    val switchPorts: List<SwitchPortInfo> = emptyList(),
    // --- редактируемые поля (значения по умолчанию = «как в вебе при пустом конфиге») ---
    val connType: String = "ALWAYS_ON", // ALWAYS_ON | ON_DEMAND
    val onDemandTimeout: Int = 0,
    val dnsManual: String = "", // IPv4 DNS 1 вручную
    val ipv6Mode: String = "off", // off | auto | manual
    val ipv6Address: String = "",
    val ipv6Prefix: String = "",
    val ipv6Gateway: String = "",
    val ipv6Dns: String = "",
    val ignoreDns6: Boolean = false,
    // PPPoE/PPTP/L2TP (auth-type из state; поля ниже — для включения)
    val authServer: String = "",
    val authLogin: String = "",
    val authPassword: String = "",
    val authMethod: String = "auto" // auto | pap | chap | mschap | mschap-v2
) {
    val effectiveMtu: Int get() = if (cfgMtu > 0) cfgMtu else stateMtu
    val providerName: String get() = description
    val scheduleLabel: String get() = schedule.ifBlank { "Работает постоянно" }
    fun orderLabel(): String = when (order) {
        -1 -> "Приоритет не выбран"
        1 -> "Резервное 1"
        2 -> "Резервное 2"
        else -> "Основное подключение"
    }
    fun ipModeLabel(): String = when (ipMode) {
        "static" -> "Ручная"
        "off" -> "Не используется"
        else -> "Автоматическая (DHCP)"
    }
    fun macModeLabel(): String = when (macMode) {
        "manual" -> "Вручную: $macManual"
        "random" -> "Случайный"
        else -> "По умолчанию (factory $macScope)"
    }
}

object WanConnectionParser {

    /**
     * Вырезать строки секции `interface <id>` из running-config.
     * Текст бывает трёх видов: plain CLI, JSON-массив строк, JSON-объект
     * {"message":[...]} (так отдаёт GET running-config на KN-2311).
     */
    fun extractStanza(rawText: String, ifaceId: String): List<String> {
        val lines = mutableListOf<String>()
        val t = rawText.trim()
        try {
            val gson = com.google.gson.Gson()
            if (t.startsWith("{")) {
                gson.fromJson(t, JsonObject::class.java)
                    .getAsJsonArray("message")?.forEach {
                        if (it.isJsonPrimitive) lines.add(it.asString)
                    }
            } else if (t.startsWith("[")) {
                gson.fromJson(t, JsonElement::class.java).asJsonArray.forEach {
                    if (it.isJsonPrimitive) lines.add(it.asString)
                }
            }
        } catch (e: Exception) {
            // не JSON — разберём построчно ниже
        }
        if (lines.isEmpty()) lines.addAll(t.lines())
        val out = mutableListOf<String>()
        var inside = false
        for (raw in lines) {
            val line = raw.trim()
            if (!inside) {
                if (line == "interface $ifaceId") inside = true
                continue
            }
            if (line == "!") break
            if (line.isNotBlank()) out.add(line)
        }
        return out
    }

    fun parse(ifaceId: String, state: JsonObject?, stanza: List<String>): WanConnection {
        var c = WanConnection(id = ifaceId)
        if (state != null) {
            c = c.copy(
                description = str(state, "description") ?: "",
                connected = str(state, "connected") == "yes",
                linkUp = str(state, "link") == "up" || str(state, "state") == "up",
                address = str(state, "address") ?: "",
                mask = str(state, "mask") ?: "",
                uptimeSec = state.get("uptime")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                stateMtu = state.get("mtu")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                stateMac = str(state, "mac") ?: "",
                authType = str(state, "auth-type") ?: "none",
                securityLevel = str(state, "security-level") ?: "",
                globalPriority = state.get("global")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                defaultGw = state.get("defaultgw")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
                ipv6Up = state.getAsJsonObject("summary")
                    ?.getAsJsonObject("layer")?.get("ipv6")?.takeIf { it.isJsonPrimitive }
                    ?.asString == "running"
            )
        }
        var adminUp = true
        var schedule = ""
        var order = 0
        var ipMode = "dhcp"
        var staticIp = ""
        var staticMask = ""
        var staticGw = ""
        var hostname = ""
        var dnsRoutes = true
        var nameServers = true
        var cfgMtu = 0
        var macMode = "factory"
        var macScope = "wan"
        var macManual = ""
        var pingProfile = ""
        var dyndns = ""
        var igmp = false
        var pendingConnType = "ALWAYS_ON"
        var pendingTimeout = 0
        var pendingIpv6 = "off"
        var pendingIpv6Addr = ""
        var pendingIpv6Prefix = ""
        var pendingIpv6Gw = ""
        var pendingIpv6Dns = ""
        for (rawLine in stanza) {
            val line = rawLine.trim()
            when {
                line == "up" -> adminUp = true
                line == "down" || line == "no up" -> adminUp = false
                line.startsWith("description ") -> {
                    val d = line.substringAfter("description ").trim().trim('"')
                    if (d.isNotBlank()) c = c.copy(description = d)
                }
                line.startsWith("schedule ") -> schedule = line.substringAfter("schedule ").trim()
                line == "no schedule" -> schedule = ""
                line.startsWith("order ") -> order = line.substringAfter("order ").trim().toIntOrNull() ?: order
                line == "ip address dhcp" -> ipMode = "dhcp"
                line.startsWith("ip address ") -> {
                    ipMode = "static"
                    val rest = line.substringAfter("ip address ").trim()
                    // Формы: "A.B.C.D/M" или "A.B.C.D 255.255.255.0".
                    if ("/" in rest) {
                        staticIp = rest.substringBefore("/")
                        val bits = rest.substringAfter("/").toIntOrNull() ?: 0
                        staticMask = prefixToMask(bits)
                    } else {
                        val parts = rest.split(Regex("\\s+"))
                        staticIp = parts.getOrElse(0) { "" }
                        staticMask = parts.getOrElse(1) { "" }
                    }
                }
                line == "no ip address" -> ipMode = "off"
                line.startsWith("ip dhcp client hostname ") ->
                    hostname = line.substringAfter("ip dhcp client hostname ").trim()
                line == "ip dhcp client dns-routes" -> dnsRoutes = true
                line == "no ip dhcp client dns-routes" -> dnsRoutes = false
                line == "ip name-servers" -> nameServers = true
                line == "ip no name-servers" -> nameServers = false
                line.startsWith("ip mtu ") ->
                    cfgMtu = line.substringAfter("ip mtu ").trim().toIntOrNull() ?: 0
                line.startsWith("mac address factory ") -> {
                    macMode = "factory"
                    macScope = line.substringAfter("mac address factory ").trim().ifBlank { "wan" }
                }
                line == "mac address random" -> macMode = "random"
                line.startsWith("mac address ") -> {
                    val v = line.substringAfter("mac address ").trim()
                    if (v.contains(":")) {
                        macMode = "manual"
                        macManual = v
                    }
                }
                line.startsWith("ping-check profile ") ->
                    pingProfile = line.substringAfter("ping-check profile ").trim()
                line.startsWith("dyndns ") -> dyndns = line.substringAfter("dyndns ").trim()
                line == "igmp upstream" -> igmp = true
                line.startsWith("ip gateway ") ->
                    staticGw = line.substringAfter("ip gateway ").trim()
                line == "connection always-on" -> {
                    /* connType по умолчанию ALWAYS_ON */
                }
                line.startsWith("connection on-demand") -> {
                    // connType/таймаут разберём ниже через мутабельные переменные
                    pendingConnType = "ON_DEMAND"
                    pendingTimeout =
                        line.substringAfter("connection on-demand").trim().toIntOrNull() ?: 0
                }
                line == "ipv6 address auto" -> pendingIpv6 = "auto"
                line.startsWith("ipv6 address ") -> {
                    pendingIpv6 = "manual"
                    pendingIpv6Addr = line.substringAfter("ipv6 address ").trim()
                }
                line == "no ipv6 address" -> pendingIpv6 = "off"
                line.startsWith("ipv6 prefix ") ->
                    pendingIpv6Prefix = line.substringAfter("ipv6 prefix ").trim()
                line.startsWith("ipv6 gateway ") ->
                    pendingIpv6Gw = line.substringAfter("ipv6 gateway ").trim()
                line.startsWith("ipv6 name-server ") ->
                    pendingIpv6Dns = line.substringAfter("ipv6 name-server ").trim()
            }
        }
        return c.copy(
            adminUp = adminUp, schedule = schedule, order = order, ipMode = ipMode,
            staticIp = staticIp, staticMask = staticMask, staticGateway = staticGw,
            hostname = hostname, dhcpDnsRoutes = dnsRoutes, useNameServers = nameServers,
            cfgMtu = cfgMtu, macMode = macMode, macScope = macScope, macManual = macManual,
            pingCheckProfile = pingProfile, dyndns = dyndns, igmpUpstream = igmp,
            connType = pendingConnType, onDemandTimeout = pendingTimeout,
            ipv6Mode = pendingIpv6, ipv6Address = pendingIpv6Addr,
            ipv6Prefix = pendingIpv6Prefix, ipv6Gateway = pendingIpv6Gw,
            ipv6Dns = pendingIpv6Dns
        )
    }

    fun parseSwitchPorts(parentState: JsonObject?): List<SwitchPortInfo> {
        val ports = parentState?.getAsJsonObject("port") ?: return emptyList()
        return ports.entrySet().mapNotNull { (key, el) ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val roles = o.getAsJsonArray("role")?.mapNotNull {
                if (it.isJsonObject) it.asJsonObject.get("role")?.takeIf { r -> r.isJsonPrimitive }?.asString else null
            } ?: emptyList()
            SwitchPortInfo(
                label = str(o, "label") ?: key,
                linkUp = str(o, "link") == "up",
                speed = str(o, "speed") ?: "",
                duplex = str(o, "duplex") ?: "",
                role = roles.joinToString(",")
            )
        }.sortedBy { it.label.toIntOrNull() ?: 99 }
    }

    private fun prefixToMask(bits: Int): String {
        if (bits <= 0 || bits > 32) return ""
        val m = if (bits == 32) -1 else (-1 shl (32 - bits))
        return listOf(24, 16, 8, 0).joinToString(".") { ((m shr it) and 0xFF).toString() }
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
