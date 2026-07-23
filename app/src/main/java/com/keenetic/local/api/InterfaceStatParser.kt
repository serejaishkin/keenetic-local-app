package com.keenetic.local.api

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Ответ команды `show interface <id> stat`, выполненной через SSH. Формат
 * подтверждён реальным выводом с роутера (не HAR, а прямой тест команды):
 * rxspeed/txspeed - байт в секунду, живая скорость на момент запроса.
 */
data class InterfaceStat(
    val rxbytes: Long? = null,
    val txbytes: Long? = null,
    val rxspeed: Long? = null,
    val txspeed: Long? = null,
    val rxerrors: Long? = null,
    val txerrors: Long? = null
)

object InterfaceStatParser {
    private val gson = Gson()

    /**
     * SSH-вывод команды может содержать служебные строки CLI до/после JSON
     * (например "(config)>" в начале, как видно на скриншоте) - вырезаем
     * содержимое между первой '{' и последней '}' перед парсингом.
     */
    fun parse(raw: String): InterfaceStat? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return try {
            gson.fromJson(raw.substring(start, end + 1), InterfaceStat::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}
