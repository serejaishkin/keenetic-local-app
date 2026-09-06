package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object ConntrackParser {

    fun parseConntrack(root: JsonElement?): List<ConntrackEntry> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<ConntrackEntry>()
        val conntrack = findKey(root, "conntrack") ?: return emptyList()
        conntrack.getAsJsonArray("entry")?.forEach { e ->
            if (e.isJsonObject) {
                val o = e.asJsonObject
                list.add(ConntrackEntry(
                    protocol = str(o, "protocol") ?: "",
                    srcIp = str(o, "src-ip") ?: "",
                    srcPort = str(o, "src-port") ?: "",
                    dstIp = str(o, "dst-ip") ?: "",
                    dstPort = str(o, "dst-port") ?: "",
                    state = str(o, "state") ?: "",
                    bytes = o.get("bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
                ))
            }
        }
        return list
    }

    fun parseNat(root: JsonElement?): List<NatEntry> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NatEntry>()
        val nat = findKey(root, "nat") ?: return emptyList()
        nat.getAsJsonArray("rule")?.forEach { r ->
            if (r.isJsonObject) {
                val o = r.asJsonObject
                list.add(NatEntry(
                    proto = str(o, "proto") ?: "",
                    srcIp = str(o, "src-ip") ?: "",
                    srcPort = str(o, "src-port") ?: "",
                    dstIp = str(o, "dst-ip") ?: "",
                    dstPort = str(o, "dst-port") ?: "",
                    interfaceName = str(o, "interface") ?: ""
                ))
            }
        }
        return list
    }

    fun parseArp(root: JsonElement?): List<ArpEntry> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<ArpEntry>()
        val arp = findKey(root, "arp") ?: return emptyList()
        arp.getAsJsonArray("entry")?.forEach { e ->
            if (e.isJsonObject) {
                val o = e.asJsonObject
                list.add(ArpEntry(
                    ip = str(o, "ip") ?: "",
                    mac = str(o, "mac") ?: "",
                    interfaceName = str(o, "interface") ?: "",
                    state = str(o, "state") ?: ""
                ))
            }
        }
        return list
    }

    fun parseNeighbours(root: JsonElement?): List<NeighbourEntry> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NeighbourEntry>()
        val neighbour = findKey(root, "neighbour") ?: return emptyList()
        neighbour.getAsJsonArray("entry")?.forEach { e ->
            if (e.isJsonObject) {
                val o = e.asJsonObject
                list.add(NeighbourEntry(
                    ip = str(o, "ip") ?: "",
                    mac = str(o, "mac") ?: "",
                    interfaceName = str(o, "interface") ?: "",
                    state = str(o, "state") ?: ""
                ))
            }
        }
        return list
    }

    fun parseIpRules(root: JsonElement?): List<IpRule> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<IpRule>()
        val rules = findKey(root, "rules") ?: findKey(root, "rule") ?: return emptyList()
        rules.getAsJsonArray("rule")?.forEach { r ->
            if (r.isJsonObject) {
                val o = r.asJsonObject
                list.add(IpRule(
                    priority = o.get("priority")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    from = str(o, "from") ?: "",
                    to = str(o, "to") ?: "",
                    lookup = str(o, "lookup") ?: ""
                ))
            }
        }
        return list
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
