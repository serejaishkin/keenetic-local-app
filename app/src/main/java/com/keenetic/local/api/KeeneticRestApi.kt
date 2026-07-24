package com.keenetic.local.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

// ===== Модели данных =====
data class AuthRequest(val login: String, val password: String)

data class SystemInfo(
    val cpuload: String? = null,
    val memory: String? = null,
    val uptime: String? = null,
    val version: String? = null,
    val hostname: String? = null
)

data class Client(
    val mac: String? = null,
    val name: String? = null,
    val ip: String? = null,
    val access: String? = null,
    val registered: String? = null,
    val active: String? = null
)

// /rci/show/ip/hotspot возвращает объект {"host": [...]}, а не голый массив -
// подтверждено реальным дампом с роутера.
data class HotspotResponse(val host: List<Client> = emptyList())

/**
 * Отображаемая модель интерфейса (после маппинга из сырого JSON-объекта,
 * см. [InterfaceMapper]).
 */
data class InterfaceInfo(
    val id: String = "",
    val displayName: String = "",
    val type: String? = null,
    val description: String? = null,
    val state: String? = null,      // административное состояние: "up" / "down"
    val link: String? = null,       // физическое состояние линка: "up" / "down"
    val connected: String? = null,
    val address: String? = null,
    val up: Boolean = false
)

/**
 * Точка доступа Wi-Fi (интерфейсы с type == "AccessPoint").
 */
data class WifiNetwork(
    val id: String,
    val ssid: String,
    val band: String,       // "2.4 ГГц" / "5 ГГц" / "—"
    val security: String,   // напр. "WPA2/WPA3-PSK" или "Открыто"
    val enabled: Boolean,
    val guest: Boolean
)

data class WifiAssoc(
    val mac: String? = null,
    val hostname: String? = null,
    val ip: String? = null,
    val rssi: String? = null,
    val txrate: String? = null,
    val rxrate: String? = null,
    val txbytes: Long? = null,
    val rxbytes: Long? = null,
    val ap: String? = null
)

data class IpPolicy(
    val name: String? = null,
    val description: String? = null
)

data class DhcpBinding(
    val mac: String? = null,
    val ip: String? = null,
    val hostname: String? = null,
    val active: Boolean = false
)

// ===== API интерфейс =====
interface KeeneticRestApi {

    @GET("auth")
    suspend fun auth(): Response<Void>

    @POST("auth")
    suspend fun login(
        @Body request: AuthRequest,
        @Header("Cookie") cookie: String
    ): Response<Void>

    @GET("rci/show/system")
    suspend fun getSystem(): Response<SystemInfo>

    @GET("rci/show/ip/hotspot")
    suspend fun getClients(): Response<HotspotResponse>

    // ВАЖНО: /rci/show/interface возвращает JSON-объект { "<id>": {...}, ... },
    // а не массив, поэтому Map<String, JsonObject>, а не List.
    @GET("rci/show/interface")
    suspend fun getInterfacesRaw(): Response<Map<String, JsonObject>>

    @POST("rci/interface/{name}")
    suspend fun setInterface(
        @Path("name") name: String,
        @Body body: Map<String, String>
    ): Response<Void>

    @POST("rci/ip/hotspot/host")
    suspend fun setClientAccess(@Body body: Map<String, String>): Response<Void>

    @POST("rci/system/reboot")
    suspend fun reboot(@Body body: Map<String, String> = emptyMap()): Response<Void>

    // Подтверждено официальной документацией Keenetic (RCI reference):
    // корневой эндпоинт, а не /rci/show/interface/{name}/assoc, как было раньше.
    @GET("rci/show/associations")
    suspend fun getAssociations(): Response<JsonElement>

    // ПРЕДПОЛОЖИТЕЛЬНЫЙ эндпоинт по аналогии с show/ip/hotspot,
    // show/ip/dhcp/bindings - не подтверждён HAR-дампом или документацией.
    // Обращение безопасно (GET, только чтение), но формат ответа не проверен -
    // код ниже обязан аккуратно обрабатывать неожиданную форму/ошибку.
    @GET("rci/show/ip/policy")
    suspend fun getIpPoliciesRaw(): Response<JsonElement>

    // Подтверждён сторонним open-source проектом мониторинга Keenetic
    // (keenetic-monitor на GitHub), не HAR-дампом с твоего роутера -
    // степень доверия ниже, чем у HAR-подтверждённых эндпоинтов.
    @GET("rci/show/ip/dhcp/bindings")
    suspend fun getDhcpBindings(): Response<JsonElement>

    // Реальный внутренний протокол Keenetic RCI: изменения настроек (в отличие
    // от чтения /rci/show/...) отправляются пакетом команд на корневой /rci/.
    // Формат подтверждён снятым HAR-дампом с настоящего роутера:
    //   - переименование клиента: {"known":{"host":{"name":..,"mac":..}}}
    //   - пароль/SSID Wi-Fi:      {"mws":{"wlan":{"id":"Home"|"Guest",...}}}
    // Обязательно завершать батч командой {"system":{"configuration":{"save":{}}}},
    // иначе изменения не переживут перезагрузку роутера.
    @POST("rci/")
    suspend fun executeRci(@Body commands: List<Map<String, Any>>): Response<JsonElement>

    @GET("rci/show/mws/wlan")
    suspend fun getMwsWlan(): Response<Map<String, JsonObject>>
}
