package com.keenetic.local.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Ответ команды `show wans` по SSH - подтверждено реальным выводом с
 * роутера. В отличие от нашей прежней эвристики (искать интерфейс с
 * непустым address), это официальный список активных WAN-подключений,
 * включая резервные (wbk).
 */
data class WanEntry(
    val id: String? = null,
    val ip: String? = null,
    @SerializedName("es") val speed: String? = null,
    @SerializedName("ed") val duplex: String? = null,
    @SerializedName("ea") val enabledFlag: String? = null,
    @SerializedName("ut") val uptime: String? = null
) {
    val enabled: Boolean get() = enabledFlag == "on"
}

data class WansResponse(
    val wan: WanEntry? = null,
    val wbk: List<WanEntry> = emptyList()
)

object WansParser {
    private val gson = Gson()

    fun parse(raw: String): WansResponse? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return try {
            gson.fromJson(raw.substring(start, end + 1), WansResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
