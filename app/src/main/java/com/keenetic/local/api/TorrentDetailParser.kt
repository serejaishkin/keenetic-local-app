package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object TorrentDetailParser {

    fun parseStatus(root: JsonElement?): TorrentStatusFull {
        if (root == null || !root.isJsonObject) return TorrentStatusFull()
        val torrent = findKey(root, "torrent") ?: return TorrentStatusFull()
        return TorrentStatusFull(
            enabled = torrent.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false,
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
