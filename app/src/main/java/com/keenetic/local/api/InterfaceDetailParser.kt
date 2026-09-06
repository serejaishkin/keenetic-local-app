package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object InterfaceDetailParser {

    fun parseTrafficCounters(root: JsonElement?): List<TrafficCounter> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<TrafficCounter>()
        for ((key, value) in root.asJsonObject.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(TrafficCounter(
                    id = key,
                    rxBytes = o.get("rx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
                    txBytes = o.get("tx-bytes")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
                ))
            }
        }
        return list
    }

    fun parseChannelUtilization(root: JsonElement?): List<ChannelUtilization> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<ChannelUtilization>()
        for ((key, value) in root.asJsonObject.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(ChannelUtilization(
                    channel = key.toIntOrNull() ?: 0,
                    load = o.get("load")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    flags = str(o, "flags") ?: ""
                ))
            }
        }
        return list
    }

    fun parseSpectrum(root: JsonElement?): List<SpectrumChannel> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<SpectrumChannel>()
        for ((key, value) in root.asJsonObject.entrySet()) {
            if (value.isJsonObject) {
                val points = mutableListOf<SpectrumPoint>()
                value.asJsonObject.getAsJsonArray("utilization")?.forEach { pt ->
                    if (pt.isJsonObject) {
                        points.add(SpectrumPoint(
                            load = pt.asJsonObject.get("load")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                            flags = str(pt.asJsonObject, "flags") ?: ""
                        ))
                    }
                }
                list.add(SpectrumChannel(
                    number = key.toIntOrNull() ?: 0,
                    utilization = points
                ))
            }
        }
        return list
    }

    fun parseCableDiagnostics(root: JsonElement?): List<CableDiagnosticResult> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<CableDiagnosticResult>()
        for ((key, value) in root.asJsonObject.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                o.getAsJsonArray("pair")?.forEach { p ->
                    if (p.isJsonObject) {
                        list.add(CableDiagnosticResult(
                            interfaceName = key,
                            pair = p.asJsonObject.get("pair")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                            length = p.asJsonObject.get("length")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                            status = str(p.asJsonObject, "status") ?: ""
                        ))
                    }
                }
            }
        }
        return list
    }

    fun parseWps(root: JsonElement?): WpsStatus {
        if (root == null || !root.isJsonObject) return WpsStatus()
        val wps = findKey(root, "wps") ?: return WpsStatus()
        return WpsStatus(
            enabled = wps.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            pin = str(wps, "pin") ?: "",
            state = str(wps, "state") ?: ""
        )
    }

    fun parseMws(root: JsonElement?): MwsStatus {
        if (root == null || !root.isJsonObject) return MwsStatus()
        val mws = findKey(root, "mws") ?: return MwsStatus()
        return MwsStatus(
            enabled = mws.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            role = str(mws, "role") ?: "",
            ssid = str(mws, "ssid") ?: "",
            channel = mws.get("channel")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
        )
    }

    fun parseMwsMembers(root: JsonElement?): List<MwsMember> {
        if (root == null || !root.isJsonObject) return emptyList()
        val list = mutableListOf<MwsMember>()
        for ((key, value) in root.asJsonObject.entrySet()) {
            if (value.isJsonObject) {
                val o = value.asJsonObject
                list.add(MwsMember(
                    name = key,
                    mac = str(o, "mac") ?: "",
                    ip = str(o, "ip") ?: "",
                    status = str(o, "status") ?: "",
                    firmware = str(o, "firmware") ?: ""
                ))
            }
        }
        return list
    }

    fun parseInternetDetailed(root: JsonElement?): InternetDetailedStatus {
        if (root == null || !root.isJsonObject) return InternetDetailedStatus()
        val isp = findKey(root, "isp") ?: findKey(root, "internet") ?: return InternetDetailedStatus()
        val addresses = mutableListOf<String>()
        isp.getAsJsonArray("address")?.forEach { a ->
            if (a.isJsonPrimitive) addresses.add(a.asString)
        }
        val dns = mutableListOf<String>()
        isp.getAsJsonArray("dns")?.forEach { d ->
            if (d.isJsonPrimitive) dns.add(d.asString)
        }
        return InternetDetailedStatus(
            connected = isp.get("connected")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            interfaceName = str(isp, "interface") ?: "",
            ip = addresses.firstOrNull() ?: "",
            mask = str(isp, "mask") ?: "",
            gateway = str(isp, "gateway") ?: "",
            dns = dns,
            speed = str(isp, "speed") ?: "",
            type = str(isp, "type") ?: ""
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
