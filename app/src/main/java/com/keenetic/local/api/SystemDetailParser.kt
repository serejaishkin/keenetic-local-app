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
        val led = findKey(root, "led") ?: return LedConfig()
        return LedConfig(
            enabled = led.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true,
            mode = str(led, "mode") ?: "enabled"
        )
    }

    fun parseMode(root: JsonElement?): SystemMode {
        if (root == null || !root.isJsonObject) return SystemMode()
        val mode = findKey(root, "mode") ?: return SystemMode()
        return SystemMode(
            mode = str(mode, "mode") ?: "router"
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

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString
}
