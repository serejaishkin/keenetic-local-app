package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object SshSnmpParser {

    fun parseSsh(root: JsonElement?): SshSettings {
        if (root == null || !root.isJsonObject) return SshSettings()
        val ssh = findKey(root, "ssh") ?: return SshSettings()
        return SshSettings(
            enabled = ssh.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            port = ssh.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 22,
            sftpEnabled = ssh.get("sftp")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseSshFingerprint(root: JsonElement?): SshFingerprint {
        if (root == null || !root.isJsonObject) return SshFingerprint()
        val ssh = findKey(root, "ssh") ?: return SshFingerprint()
        val fp = ssh.getAsJsonObject("fingerprint") ?: return SshFingerprint()
        return SshFingerprint(
            md5 = str(fp, "md5") ?: "",
            sha256 = str(fp, "sha256") ?: ""
        )
    }

    fun parseSnmp(root: JsonElement?): SnmpView {
        if (root == null || !root.isJsonObject) return SnmpView()
        val snmp = findKey(root, "snmp") ?: return SnmpView()
        return SnmpView(
            community = str(snmp, "community") ?: "",
            enabled = snmp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseFtp(root: JsonElement?): FtpSettings {
        // The web configurator reads `show sc ip ftp` as an array and uses `port`
        // plus the `permissive` anonymous-access flag.
        val ftp = when {
            root == null -> null
            root.isJsonArray -> root.asJsonArray.firstOrNull {
                it.isJsonObject && (it.asJsonObject.has("port") || it.asJsonObject.has("permissive"))
            }?.asJsonObject
            root.isJsonObject -> findKey(root, "ftp") ?: root.asJsonObject.takeIf { it.has("port") || it.has("permissive") }
            else -> null
        } ?: return FtpSettings()
        val port = ftp.get("port")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let {
            when {
                it.isNumber -> it.asInt
                it.isString -> it.asString.trim().toIntOrNull()
                else -> null
            }
        } ?: 21
        return FtpSettings(
            enabled = ftp.get("enabled")?.takeIf { it.isJsonPrimitive }?.asBoolean
                ?: ftp.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean
                ?: false,
            port = port,
            anonymousAccess = ftp.get("permissive")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseTelnet(root: JsonElement?): TelnetSettings {
        if (root == null || !root.isJsonObject) return TelnetSettings()
        val telnet = findKey(root, "telnet") ?: return TelnetSettings()
        return TelnetSettings(
            enabled = telnet.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            port = telnet.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 23
        )
    }

    fun parseHttpProxy(root: JsonElement?): HttpProxySettings {
        if (root == null || !root.isJsonObject) return HttpProxySettings()
        val proxy = findKey(root, "http-proxy") ?: return HttpProxySettings()
        return HttpProxySettings(
            enabled = proxy.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            port = proxy.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 3128
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
