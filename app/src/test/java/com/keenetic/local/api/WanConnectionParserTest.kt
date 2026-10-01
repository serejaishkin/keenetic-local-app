package com.keenetic.local.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Проверено живыми данными KN-2311 (fw 5.1.5, провайдер PACT):
 * узел GigabitEthernet0/Vlan4 из `show interface` + секция running-config.
 */
class WanConnectionParserTest {

    private val gson = Gson()

    private val stateJson = """
        {"id":"GigabitEthernet0/Vlan4","index":4,"interface-name":"GigabitEthernet0/Vlan4",
        "type":"Vlan","description":"GigabitEthernet интерфейс","link":"up","connected":"yes",
        "state":"up","mtu":1500,"address":"10.41.0.50","mask":"255.255.255.0","uptime":74881,
        "global":true,"defaultgw":true,"priority":65470,"security-level":"public",
        "ipv6":{"addresses":[{"address":"fe80::20c:43ff:fe26:5997","prefix-length":64}],
        "defaultgw":false},"mac":"00:0c:43:26:59:97","auth-type":"none",
        "summary":{"layer":{"conf":"running","link":"running","ipv4":"running","ipv6":"disabled","ctrl":"running"}}}
    """.trimIndent()

    private val stanza = listOf(
        "description PACT",
        "dyndns nobind",
        "mac address factory wan",
        "security-level public",
        "ip address dhcp",
        "ip dhcp client hostname Keenetic",
        "ip dhcp client dns-routes",
        "ip mtu 1500",
        "ip global 65470",
        "ip no name-servers",
        "igmp upstream",
        "ping-check profile default",
        "up"
    )

    @Test
    fun testParseLiveVlan4() {
        val state = JsonParser.parseString(stateJson).asJsonObject
        val c = WanConnectionParser.parse("GigabitEthernet0/Vlan4", state, stanza)
        assertEquals("PACT", c.description)
        assertTrue(c.connected)
        assertTrue(c.linkUp)
        assertEquals("10.41.0.50", c.address)
        assertEquals("255.255.255.0", c.mask)
        assertEquals(1500, c.stateMtu)
        assertEquals(1500, c.effectiveMtu)
        assertEquals("00:0c:43:26:59:97", c.stateMac)
        assertEquals("none", c.authType)
        assertTrue(c.adminUp)
        assertEquals("", c.schedule)
        assertEquals("Работает постоянно", c.scheduleLabel)
        assertEquals(0, c.order)
        assertEquals("Основное подключение", c.orderLabel())
        assertEquals("dhcp", c.ipMode)
        assertEquals("Keenetic", c.hostname)
        assertTrue(c.dhcpDnsRoutes)
        assertFalse(c.useNameServers)
        assertEquals(1500, c.cfgMtu)
        assertEquals("factory", c.macMode)
        assertEquals("wan", c.macScope)
        assertEquals("default", c.pingCheckProfile)
        assertEquals("public", c.securityLevel)
        assertTrue(c.defaultGw)
        assertFalse(c.ipv6Up)
    }

    @Test
    fun testParseStaticAndManualMac() {
        val state = JsonParser.parseString(stateJson).asJsonObject
        val c = WanConnectionParser.parse(
            "GigabitEthernet0/Vlan4", state,
            listOf(
                "description TEST",
                "order 1",
                "schedule worktime",
                "ip address 192.168.10.5 255.255.255.0",
                "ip gateway 192.168.10.1",
                "mac address 00:11:22:33:44:55",
                "no ip dhcp client dns-routes",
                "down"
            )
        )
        assertEquals("static", c.ipMode)
        assertEquals("192.168.10.5", c.staticIp)
        assertEquals("255.255.255.0", c.staticMask)
        assertEquals("192.168.10.1", c.staticGateway)
        assertEquals("manual", c.macMode)
        assertEquals("00:11:22:33:44:55", c.macManual)
        assertEquals(1, c.order)
        assertEquals("Резервное 1", c.orderLabel())
        assertEquals("worktime", c.schedule)
        assertFalse(c.dhcpDnsRoutes)
        assertFalse(c.adminUp)
    }

    @Test
    fun testExtractStanzaFromMessageJson() {
        val raw = """{"message":["! $$$ Agent: http/rci","interface GigabitEthernet0/Vlan4",
            "    description PACT","    ip mtu 1500","    up","!",
            "interface GigabitEthernet1","    up","!"]}""".trimIndent()
            .replace("\n", "")
        val stanza = WanConnectionParser.extractStanza(raw, "GigabitEthernet0/Vlan4")
        assertEquals(listOf("description PACT", "ip mtu 1500", "up"), stanza)
    }

    @Test
    fun testExtractStanzaPlainCli() {
        val raw = "interface GigabitEthernet0/Vlan4\n    description PACT\n    up\n!\n"
        val stanza = WanConnectionParser.extractStanza(raw, "GigabitEthernet0/Vlan4")
        assertEquals(listOf("description PACT", "up"), stanza)
    }
}
