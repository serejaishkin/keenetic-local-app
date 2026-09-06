package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object Ipv6DetailParser {

    fun parseAddresses(root: JsonElement?): List<Ipv6Address> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6Address>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("address")?.forEach { a ->
            if (a.isJsonObject) {
                val o = a.asJsonObject
                list.add(Ipv6Address(
                    address = str(o, "address") ?: "",
                    prefix = o.get("prefix")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    interfaceName = str(o, "interface") ?: "",
                    flags = str(o, "flags") ?: ""
                ))
            }
        }
        return list
    }

    fun parsePrefixes(root: JsonElement?): List<Ipv6Prefix> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6Prefix>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("prefix")?.forEach { p ->
            if (p.isJsonObject) {
                val o = p.asJsonObject
                list.add(Ipv6Prefix(
                    prefix = str(o, "prefix") ?: "",
                    interfaceName = str(o, "interface") ?: "",
                    preferred = o.get("preferred")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    valid = o.get("valid")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                ))
            }
        }
        return list
    }

    fun parseRoutes(root: JsonElement?): List<Ipv6Route> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6Route>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("route")?.forEach { r ->
            if (r.isJsonObject) {
                val o = r.asJsonObject
                list.add(Ipv6Route(
                    network = str(o, "network") ?: "",
                    prefix = o.get("prefix")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    gateway = str(o, "gateway") ?: "",
                    interfaceName = str(o, "interface") ?: "",
                    metric = o.get("metric")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                ))
            }
        }
        return list
    }

    fun parseSubnets(root: JsonElement?): List<Ipv6Subnet> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6Subnet>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("subnet")?.forEach { s ->
            if (s.isJsonObject) {
                val o = s.asJsonObject
                list.add(Ipv6Subnet(
                    network = str(o, "network") ?: "",
                    prefix = o.get("prefix")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    interfaceName = str(o, "interface") ?: ""
                ))
            }
        }
        return list
    }

    fun parseDhcpBindings(root: JsonElement?): List<Ipv6DhcpBinding> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6DhcpBinding>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("binding")?.forEach { b ->
            if (b.isJsonObject) {
                val o = b.asJsonObject
                list.add(Ipv6DhcpBinding(
                    duid = str(o, "duid") ?: "",
                    iaId = str(o, "ia-id") ?: "",
                    address = str(o, "address") ?: "",
                    hostname = str(o, "hostname") ?: ""
                ))
            }
        }
        return list
    }

    fun parseConntrack(root: JsonElement?): List<Ipv6Conntrack> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<Ipv6Conntrack>()
        val ipv6 = findKey(root, "ipv6") ?: return emptyList()
        ipv6.getAsJsonArray("conntrack")?.forEach { c ->
            if (c.isJsonObject) {
                val o = c.asJsonObject
                list.add(Ipv6Conntrack(
                    protocol = str(o, "protocol") ?: "",
                    srcIp = str(o, "src-ip") ?: "",
                    dstIp = str(o, "dst-ip") ?: "",
                    state = str(o, "state") ?: ""
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
