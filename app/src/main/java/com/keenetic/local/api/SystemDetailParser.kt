package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object SystemDetailParser {

    fun parseEnvironment(root: JsonElement?): EnvironmentInfo {
        if (root == null || !root.isJsonObject) return EnvironmentInfo()
        val sys = findKey(root, "system") ?: return EnvironmentInfo()
        val env = sys.getAsJsonObject("environment")
        return EnvironmentInfo(
            temperature = env?.get("temperature")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
            fanSpeed = env?.get("fan-speed")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            voltageCpu = env?.get("voltage-cpu")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
            uptime = sys.get("uptime")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
        )
    }

    fun parseProduct(root: JsonElement?): ProductInfo {
        if (root == null || !root.isJsonObject) return ProductInfo()
        val prod = findKey(root, "product") ?: findKey(root, "ndm") ?: return ProductInfo()
        return ProductInfo(
            vendor = str(prod, "vendor") ?: "",
            model = str(prod, "model") ?: "",
            serialNumber = str(prod, "serial-number") ?: "",
            description = str(prod, "description") ?: ""
        )
    }

    fun parseNtp(root: JsonElement?): NtpStatus {
        if (root == null || !root.isJsonObject) return NtpStatus()
        val ntp = findKey(root, "ntp") ?: return NtpStatus()
        return NtpStatus(
            enabled = ntp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            server = str(ntp, "server") ?: "",
            lastSync = str(ntp, "last-sync") ?: "",
            mode = str(ntp, "mode") ?: ""
        )
    }

    fun parseBackup(root: JsonElement?): BackupStatus {
        if (root == null || !root.isJsonObject) return BackupStatus()
        val bak = findKey(root, "backup") ?: return BackupStatus()
        return BackupStatus(
            filename = str(bak, "filename") ?: "",
            size = bak.get("size")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
            date = str(bak, "date") ?: ""
        )
    }

    fun parseLed(root: JsonElement?): LedConfig {
        if (root == null || !root.isJsonObject) return LedConfig()
        val rootObj = root.asJsonObject
        val led = rootObj.getAsJsonObject("leds")?.getAsJsonObject("led") ?: rootObj
        val enabled = led.get("FN")?.takeIf { it.isJsonObject }?.let {
            it.asJsonObject.get("user_configurable")?.takeIf { p -> p.isJsonPrimitive }?.asBoolean
        } ?: led.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean
        return LedConfig(
            enabled = enabled ?: true,
            mode = "enabled"
        )
    }

    fun parseMode(root: JsonElement?): SystemMode {
        if (root == null || !root.isJsonObject) return SystemMode()
        val obj = root.asJsonObject
        val active = obj.get("active")?.takeIf { it.isJsonPrimitive }?.asString
        val selected = obj.get("selected")?.takeIf { it.isJsonPrimitive }?.asString
        val supported = obj.get("supported")?.takeIf { it.isJsonPrimitive }?.asString
        val hwControlled = obj.get("hw_controlled")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        val hwLocked = obj.get("hw_locked")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        if (active == null && selected == null) {
            val mode = findKey(root, "mode") ?: return SystemMode()
            return SystemMode(mode = str(mode, "mode") ?: "router")
        }
        return SystemMode(
            mode = active ?: selected ?: "router",
            supported = supported ?: "",
            hwControlled = hwControlled,
            hwLocked = hwLocked
        )
    }

    fun parseZram(root: JsonElement?): JsonObject? {
        if (root == null || !root.isJsonObject) return null
        return findKey(root, "zram")
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

    fun parseServiceFlags(root: JsonElement?): ServiceFlags {
        if (root == null || !root.isJsonObject) return ServiceFlags()
        val svc = findKey(root, "service")
            ?: (root as? JsonObject)?.takeIf { it.has("ftp") || it.has("ssh") || it.has("ntp") }
            ?: return ServiceFlags()
        return ServiceFlags(
            ftp = bool(svc, "ftp"),
            ssh = bool(svc, "ssh"),
            telnet = bool(svc, "telnet"),
            ntp = bool(svc, "ntp"),
            httpProxy = bool(svc, "http-proxy")
        )
    }

    private fun bool(o: JsonObject, field: String): Boolean =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
