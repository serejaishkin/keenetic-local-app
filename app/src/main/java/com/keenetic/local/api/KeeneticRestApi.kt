package com.keenetic.local.api

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

data class InterfaceInfo(
    val id: String? = null,
    val state: String? = null,
    val up: Boolean? = null,
    val link: String? = null,
    val connected: String? = null
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

    @GET("rci/show/interface")
    suspend fun getInterfaces(@Query("name") name: String? = null): Response<List<InterfaceInfo>>

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