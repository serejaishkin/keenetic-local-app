package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object ConntrackParser {

    fun parseConntrack(root: JsonElement?): List<ConntrackEntry> {
        if (root == null) return emptyList()
        val list = mutableListOf<ConntrackEntry>()
        fun parseTextTable(text: String) {
            val re = Regex("""^(\S+)\s+\d+\s+src=(\S+)\s+dst=(\S+)\s+sport=(\S+)\s+dport=(\S+).*?packets=(\d+)\s+bytes=(\d+)""")
            text.lines().forEach { line ->
                val m = re.find(line.trim()) ?: return@forEach
                list.add(ConntrackEntry(
                    protocol = m.groupValues[1],
                    srcIp = m.groupValues[2],
                    srcPort = m.groupValues[4],
                    dstIp = m.groupValues[3],
                    dstPort = m.groupValues[5],
                    state = if ("[UNREPLIED]" in line) "UNREPLIED" else "ASSURED",
                    bytes = m.groupValues[7].toLongOrNull() ?: 0
                ))
            }
        }
        if (root.isJsonObject) {
            val obj = root.asJsonObject
            obj.get("ipv4")?.takeIf { it.isJsonPrimitive }?.asString?.let { parseTextTable(it) }
            obj.get("ipv6")?.takeIf { it.isJsonPrimitive }?.asString?.let { parseTextTable(it) }
            if (list.isNotEmpty()) return list
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
        return emptyList()
    }

    fun parseNat(root: JsonElement?): List<NatEntry> {
        if (root == null) return emptyList()
        val list = mutableListOf<NatEntry>()
        fun addEntry(o: JsonObject) {
            val proto = str(o, "proto") ?: str(o, "protocol") ?: ""
            val srcIp = str(o, "src-ip") ?: str(o, "src") ?: ""
            val srcPort = str(o, "src-port") ?: numStr(o, "sport") ?: ""
            val dstIp = str(o, "dst-ip") ?: str(o, "dst") ?: ""
            val dstPort = str(o, "dst-port") ?: numStr(o, "dport") ?: ""
            list.add(NatEntry(
                proto = proto,
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                interfaceName = str(o, "interface") ?: ""
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addEntry(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val nat = findKey(root, "nat") ?: return emptyList()
        nat.getAsJsonArray("rule")?.forEach { r ->
            if (r.isJsonObject) addEntry(r.asJsonObject)
        }
        return list
    }

    fun parseArp(root: JsonElement?): List<ArpEntry> {
        if (root == null) return emptyList()
        val list = mutableListOf<ArpEntry>()
        fun addEntry(o: JsonObject) {
            list.add(ArpEntry(
                ip = str(o, "ip") ?: "",
                mac = str(o, "mac") ?: "",
                interfaceName = str(o, "interface") ?: "",
                state = str(o, "state") ?: ""
            ))
        }
        if (root.isJsonArray) {
            root.asJsonArray.forEach { if (it.isJsonObject) addEntry(it.asJsonObject) }
            return list
        }
        if (!root.isJsonObject) return emptyList()
        val arp = findKey(root, "arp") ?: return emptyList()
        arp.getAsJsonArray("entry")?.forEach { e ->
            if (e.isJsonObject) addEntry(e.asJsonObject)
        }
        return list
    }

    fun parseNeighbours(root: JsonElement?): List<NeighbourEntry> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<NeighbourEntry>()
        fun addEntry(o: JsonObject) {
            list.add(NeighbourEntry(
                ip = str(o, "address") ?: str(o, "ip") ?: "",
                mac = str(o, "mac") ?: "",
                interfaceName = str(o, "interface") ?: "",
                state = if (o.get("expired")?.takeIf { it.isJsonPrimitive }?.asBoolean == true) "EXPIRED" else "REACHABLE"
            ))
        }
        val neighbour = findKey(root, "neighbour")
        if (neighbour != null) {
            neighbour.getAsJsonArray("entry")?.forEach { e ->
                if (e.isJsonObject) addEntry(e.asJsonObject)
            }
            if (list.isNotEmpty()) return list
        }
        root.asJsonObject.entrySet().forEach { (_, v) ->
            if (v.isJsonObject) {
                val o = v.asJsonObject
                if (o.has("mac") && (o.has("address") || o.has("ip"))) addEntry(o)
            } else if (v.isJsonArray) {
                v.asJsonArray.forEach { if (it.isJsonObject) addEntry(it.asJsonObject) }
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

    private fun numStr(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.let {
            try { it.asString } catch (_: Exception) { null }
        }
}
