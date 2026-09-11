package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object VpnDetailParser {

    fun parsePptpServer(root: JsonElement?): PptpServer {
        if (root == null || !root.isJsonObject) return PptpServer()
        val srv = findKey(root, "vpn-server") ?: findKey(root, "pptp-server") ?: findKey(root, "pptp") ?: return PptpServer()
        val cfg = srv.getAsJsonObject("config") ?: srv
        val poolRange = srv.getAsJsonObject("pool-range")
        return PptpServer(
            enabled = cfg.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(srv, "interface") ?: str(cfg, "interface") ?: "",
            poolStart = poolRange?.let { str(it, "begin") } ?: str(cfg, "pool-start") ?: str(srv, "pool-start") ?: "",
            poolSize = poolRange?.let { str(it, "size") } ?: str(cfg, "pool-size") ?: str(srv, "pool-size") ?: "",
            nat = cfg.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: srv.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            multiLogin = cfg.get("multi-login")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: srv.get("multi-login")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            encryption = cfg.get("encryption")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: cfg.get("isEncryptionEnabled")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: srv.get("encryption")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseOcServer(root: JsonElement?): OcServer {
        if (root == null || !root.isJsonObject) return OcServer()
        val srv = findKey(root, "oc-server") ?: findKey(root, "openconnect-server") ?: findKey(root, "openconnect") ?: return OcServer()
        val cfg = srv.getAsJsonObject("config") ?: srv
        val poolRange = srv.getAsJsonObject("pool-range")
        return OcServer(
            enabled = cfg.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(srv, "interface") ?: str(cfg, "interface") ?: "",
            poolStart = poolRange?.let { str(it, "begin") } ?: str(cfg, "pool-start") ?: str(srv, "pool-start") ?: "",
            poolSize = poolRange?.let { str(it, "size") } ?: str(cfg, "pool-size") ?: str(srv, "pool-size") ?: "",
            nat = cfg.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: srv.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            camouflage = cfg.get("camouflage")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: srv.get("camouflage")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseWireguardServer(root: JsonElement?): WireguardServerStatus {
        if (root == null || !root.isJsonObject) return WireguardServerStatus()
        val wg = findKey(root, "wireguard-server") ?: findKey(root, "wireguard") ?: return WireguardServerStatus()
        val peers = mutableListOf<WireguardPeerFull>()
        wg.getAsJsonArray("peer")?.forEach { p ->
            if (p.isJsonObject) {
                val o = p.asJsonObject
                peers.add(WireguardPeerFull(
                    name = str(o, "name") ?: "",
                    publicKey = str(o, "public-key") ?: "",
                    presharedKey = str(o, "preshared-key") ?: "",
                    endpoint = str(o, "endpoint") ?: "",
                    allowedIp = str(o, "allowed-ip") ?: "",
                    keepalive = o.get("keepalive")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    rxBytes = o.get("rx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                    txBytes = o.get("tx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                    latestHandshake = o.get("latest-handshake")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
                ))
            }
        }
        return WireguardServerStatus(
            enabled = wg.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            listenPort = wg.get("listen-port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            privateKey = str(wg, "private-key") ?: "",
            publicKey = str(wg, "public-key") ?: "",
            address = str(wg, "address") ?: "",
            peers = peers
        )
    }

    fun parseL2tpServer(root: JsonElement?): L2tpServer {
        if (root == null || !root.isJsonObject) return L2tpServer()
        val l2tp = findKey(root, "l2tp-server") ?: findKey(root, "l2tp") ?: return L2tpServer()
        return L2tpServer(
            enabled = l2tp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(l2tp, "interface") ?: "",
            poolStart = str(l2tp, "pool-start") ?: "",
            poolSize = str(l2tp, "pool-size") ?: "",
            nat = l2tp.get("nat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            encryption = l2tp.get("encryption")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseIkev2Server(root: JsonElement?): Ikev2Server {
        if (root == null || !root.isJsonObject) return Ikev2Server()
        val ikev2 = findKey(root, "ikev2-server") ?: findKey(root, "ikev2") ?: return Ikev2Server()
        return Ikev2Server(
            enabled = ikev2.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(ikev2, "interface") ?: "",
            poolStart = str(ikev2, "pool-start") ?: "",
            poolSize = str(ikev2, "pool-size") ?: ""
        )
    }

    fun parseSstpServer(root: JsonElement?): SstpServerFull {
        if (root == null || !root.isJsonObject) return SstpServerFull()
        val sstp = findKey(root, "sstp-server") ?: findKey(root, "sstp") ?: return SstpServerFull()
        return SstpServerFull(
            enabled = sstp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(sstp, "interface") ?: "",
            poolStart = str(sstp, "pool-start") ?: "",
            poolSize = str(sstp, "pool-size") ?: "",
            camouflage = sstp.get("camouflage")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseIpsec(root: JsonElement?): IpsecStatus {
        if (root == null || !root.isJsonObject) return IpsecStatus()
        val ipsec = findKey(root, "ipsec") ?: return IpsecStatus()
        val connections = mutableListOf<IpsecConnection>()
        ipsec.getAsJsonArray("connection")?.forEach { c ->
            if (c.isJsonObject) {
                val o = c.asJsonObject
                connections.add(IpsecConnection(
                    name = str(o, "name") ?: "",
                    status = str(o, "status") ?: "",
                    localAddress = str(o, "local-address") ?: "",
                    remoteAddress = str(o, "remote-address") ?: ""
                ))
            }
        }
        return IpsecStatus(
            enabled = ipsec.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            connections = connections
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
