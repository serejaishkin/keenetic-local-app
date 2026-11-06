package com.keenetic.local.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Снимок ответов RCI: набор read-only `show`-путей, ответы собираются в один JSON
 * (вместо ручного снятия HAR). Секретные поля маскируются как `***`.
 * Путь записывается как в `show`: "skydns/userinfo" = GET /rci/show/skydns/userinfo
 * (с запасным POST через KeeneticRciRepository.queryShow).
 */
object RciCapture {

    /** Только чтение. Отсутствующий ответ тоже полезен: значит, компонента нет или формат другой. */
    val DEFAULT_PATHS: List<String> = listOf(
        "version", "system", "clock/date", "last-change",
        "interface", "internet/status", "ip/hotspot", "ip/policy", "ip/static", "ip/route",
        "ip/dhcp/bindings", "ip/dhcp/pool", "ipv6/addresses", "ipv6/prefixes", "ipv6/route",
        "upnp/redirect", "associations", "mws", "mws/wlan", "site-survey", "ping-check", "schedule",
        "dns-proxy", "dns-proxy/filter/presets", "dns-proxy/filter/profiles",
        "skydns", "safedns", "nextdns",
        "skydns/userinfo", "safedns/userinfo",
        "rc/skydns", "rc/safedns", "rc/nextdns",
        "skydns/profiles/profiles", "safedns/profiles/profiles", "nextdns/profiles/profiles",
        "ndss", "ndns", "acme", "crypto",
        "vpn-server", "sstp-server", "oc-server",
        "ntce/summary", "ntce/status", "tags", "authentication/token", "rc/ntce/upstream/rate-limit",
        "usb", "media", "torrent", "cifs", "afp", "ntp/status", "eula", "dpn"
    )

    private val SECRET_KEY = Regex("(?i)(pass|secret|private|psk|token|cookie|auth-key|^key$|-key$|^sid$)")

    data class Item(val path: String, val ok: Boolean, val bytes: Int, val body: JsonElement?)

    /** "show ip hotspot" / "/ip/hotspot" / "ip hotspot" -> "ip/hotspot". */
    fun normalizePath(line: String): String =
        line.trim().removePrefix("show ").trim().trim('/').replace(Regex("\\s+"), "/")

    fun mask(e: JsonElement?): JsonElement? {
        if (e == null) return null
        return when {
            e.isJsonObject -> JsonObject().also { o ->
                for ((k, v) in e.asJsonObject.entrySet()) {
                    val secret = SECRET_KEY.containsMatchIn(k) && v.isJsonPrimitive &&
                        v.asString.isNotEmpty() && v.asString != "false" && v.asString != "true"
                    o.add(k, if (secret) JsonPrimitive("***") else mask(v))
                }
            }
            e.isJsonArray -> JsonArray().also { a -> e.asJsonArray.forEach { a.add(mask(it)) } }
            else -> e
        }
    }

    fun buildReport(items: List<Item>, masked: Boolean, routerInfo: String): String {
        val root = JsonObject()
        val meta = JsonObject()
        meta.addProperty("created", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        meta.addProperty("router", routerInfo)
        meta.addProperty("masked", masked)
        meta.addProperty("note", "ответы на read-only show-пути; ok=false: пути/компонента нет или ответ с ошибкой")
        root.add("meta", meta)
        val res = JsonObject()
        for (it in items) {
            val o = JsonObject()
            o.addProperty("ok", it.ok)
            o.add("response", if (masked) mask(it.body) else it.body)
            res.add("show/" + it.path, o)
        }
        root.add("results", res)
        return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root)
    }
}
