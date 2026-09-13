package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Torrent configuration lives in `sc/torrent`:
 *   { "rpc-port": { "port": 8090, "public": false }, "peer-port": 51413, "directory": "OPKG:" }
 * Running state comes from `torrent/status`: { "state": "disabled" }.
 */
object TorrentDetailParser {

    fun parseConfig(root: JsonElement?): TorrentConfig {
        if (root == null || !root.isJsonObject) return TorrentConfig()
        val o = root.asJsonObject
        val rpcPort = o.get("rpc-port")?.takeIf { it.isJsonObject }?.asJsonObject
        return TorrentConfig(
            rpcPort = rpcPort?.get("port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 8090,
            rpcPublic = rpcPort?.get("public")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            peerPort = o.get("peer-port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 51413,
            downloadDir = str(o, "directory") ?: "OPKG:"
        )
    }

    fun parseStatus(root: JsonElement?): TorrentStatusFull {
        if (root == null || !root.isJsonObject) return TorrentStatusFull()
        val torrent = findKey(root, "torrent") ?: return TorrentStatusFull()
        return TorrentStatusFull(
            enabled = torrent.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            state = str(torrent, "state") ?: "",
            rpcPort = torrent.get("rpc-port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            rpcPublic = torrent.get("rpc-public")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
            peerPort = torrent.get("peer-port")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            downloadDir = str(torrent, "download-dir") ?: "",
            activeTorrents = torrent.get("active-torrents")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            totalTorrents = torrent.get("total-torrents")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
            downloadSpeed = torrent.get("download-speed")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0,
            uploadSpeed = torrent.get("upload-speed")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0
        )
    }

    fun parseRunningState(root: JsonElement?): String {
        if (root == null || !root.isJsonObject) return ""
        return str(root.asJsonObject, "state") ?: ""
    }

    fun parseLocalAccount(root: JsonElement?): TorrentLocalAccount {
        if (root == null || !root.isJsonObject) return TorrentLocalAccount()
        val la = findKey(root, "local-account") ?: return TorrentLocalAccount()
        return TorrentLocalAccount(
            username = str(la, "username") ?: "",
            enabled = la.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
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