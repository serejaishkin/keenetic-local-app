package com.keenetic.local.api

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
    val rxrate: String? = null
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
    suspend fun getClients(): Response<List<Client>>

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
    suspend fun reboot(): Response<Void>

    @GET("rci/show/interface/{name}/assoc")
    suspend fun getWifiAssoc(@Path("name") name: String): Response<List<WifiAssoc>>
}
