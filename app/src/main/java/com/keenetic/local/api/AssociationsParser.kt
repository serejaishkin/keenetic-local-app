package com.keenetic.local.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Разбор ответов /rci/show/associations и /rci/show/ip/policy. Эти два
 * эндпоинта, в отличие от остальных в проекте, НЕ подтверждены реальным
 * HAR-дампом с роутера - только официальной документацией Keenetic и
 * общей схемой других show/-эндпоинтов. Реальная форма ответа (массив,
 * объект по ключу интерфейса, или объект с вложенным "station"/"host")
 * может отличаться в зависимости от прошивки, поэтому парсинг перебирает
 * несколько вероятных вариантов и не падает на неожиданной структуре -
 * в худшем случае просто вернёт пустой список.
 */
object AssociationsParser {

    fun parse(root: JsonElement?): List<WifiAssoc> {
        if (root == null || root.isJsonNull) return emptyList()
        val result = mutableListOf<WifiAssoc>()

        when {
            root.isJsonArray -> root.asJsonArray.forEach { el ->
                if (el.isJsonObject) result += toAssoc(el.asJsonObject)
            }
            root.isJsonObject -> {
                val obj = root.asJsonObject
                // Вариант: {"station": [...]}
                obj.get("station")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                }
                // Вариант: {"<interfaceId>": {"station": [...]}} или {"<interfaceId>": [...]}
                if (result.isEmpty()) {
                    obj.entrySet().forEach { (_, value) ->
                        when {
                            value.isJsonArray -> value.asJsonArray.forEach { el ->
                                if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                            }
                            value.isJsonObject -> {
                                val inner = value.asJsonObject
                                inner.get("station")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                                    if (el.isJsonObject) result += toAssoc(el.asJsonObject)
                                }
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private fun toAssoc(o: JsonObject): WifiAssoc = WifiAssoc(
        mac = str(o, "mac"),
        hostname = str(o, "hostname") ?: str(o, "name"),
        ip = str(o, "ip"),
        rssi = str(o, "rssi"),
        txrate = str(o, "txrate"),
        rxrate = str(o, "rxrate"),
        txbytes = long(o, "txbytes"),
        rxbytes = long(o, "rxbytes"),
        ap = str(o, "ap") ?: str(o, "interface")
    )

    fun parsePolicyNames(root: JsonElement?): List<IpPolicy> {
        if (root == null || root.isJsonNull) return emptyList()
        val result = mutableListOf<IpPolicy>()

        when {
            root.isJsonObject -> {
                // Вероятный вариант: объект вида {"<policyName>": {"description": "..."}}
                root.asJsonObject.entrySet().forEach { (key, value) ->
                    val desc = if (value.isJsonObject) str(value.asJsonObject, "description") else null
                    result += IpPolicy(name = key, description = desc?.takeIf { it.isNotBlank() })
                }
            }
            root.isJsonArray -> root.asJsonArray.forEach { el ->
                if (el.isJsonObject) {
                    val name = str(el.asJsonObject, "name")
                    val desc = str(el.asJsonObject, "description")
                    if (name != null) result += IpPolicy(name = name, description = desc?.takeIf { it.isNotBlank() })
                } else if (el.isJsonPrimitive) {
                    result += IpPolicy(name = el.asString)
                }
            }
        }
        return result
    }

    private fun str(o: JsonObject, field: String): String? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asString

    private fun long(o: JsonObject, field: String): Long? =
        o.get(field)?.takeIf { it.isJsonPrimitive }?.asLong
}
