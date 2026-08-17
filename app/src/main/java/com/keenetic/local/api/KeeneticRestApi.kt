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
    val hostname: String? = null,
    // Для "Активные соединения" (card_system в веб-дашборде) - conntotal -
    // connfree. Подтверждено HAR (07.08) - это тот же самый эндпоинт
    // rci/show/system, просто эти два поля раньше не парсились.
    val conntotal: Int? = null,
    val connfree: Int? = null
)

/**
 * Модель/версия прошивки - ПОДТВЕРЖДЕНО HAR (07.08): отдельный эндпоинт
 * rci/show/version (НЕ show/system, где раньше "Версия ОС" искалась и была
 * всегда пустой - её там просто нет). title - тот самый "5.1.1", что видно
 * в вебе как версию, model - "KN-2311 (KN-2311)".
 */
data class VersionInfo(
    val title: String? = null,
    val model: String? = null,
    @com.google.gson.annotations.SerializedName("hw_id") val hwId: String? = null
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
    val up: Boolean = false,
    // ПОДТВЕРЖДЕНО HAR (07.08) - реальные поля "mac" и "mask" в ответе
    // /rci/show/interface для GigabitEthernet0/Vlan4 (WAN). "gateway" в
    // этом объекте НЕТ вообще (ни в этом, ни в show wans) - не выдумываю,
    // просто не показываем эту строку, пока не найдётся подтверждённый
    // источник.
    val mac: String? = null,
    val mask: String? = null
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

/**
 * Физический порт коммутатора ("Сетевые порты" на веб-дашборде).
 * ПОДТВЕРЖДЕНО HAR (07.08): реальный объект type=="Port" плоским ключом на
 * верхнем уровне /rci/show/interface (GigabitEthernet0/0..3 для LAN-портов
 * этой модели, GigabitEthernet1/0 - отдельный порт под WAN/SFP).
 */
data class SwitchPort(
    val id: String,
    val label: String,     // видимый номер порта, напр. "1"
    val link: String?,     // "up" / "down"
    val speed: String?,    // "1000" (Мбит/с) при link=up
    val duplex: String?,
    val roleFor: String?   // id VLAN/интерфейса, который использует порт (если есть)
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

    @GET("rci/show/version")
    suspend fun getVersion(): Response<VersionInfo>

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

    // Переадресация портов. RCI-путь "ip.static" подтверждён строкой в
    // main-553997B.js (B$="ip.static", show.sc.ip.static) - это ЧТЕНИЕ.
    // Схема ответа (массив/объект) и формат set-команды на создание правила
    // пока НЕ подтверждены HAR - см. заметку в PortForwardingScreen.kt.
    @GET("rci/show/ip/static")
    suspend fun getIpStaticRaw(): Response<JsonElement>

    // DNS-фильтры. RCI-пути подтверждены строками в main-553997B.js:
    // "show.dns-proxy.filter.presets" и "show.dns-proxy.filter.profiles"
    // (готовые пресеты вроде "Семейный"/"Без рекламы" и пользовательские
    // профили фильтрации). Чтение; set-команда назначения профиля
    // ("dns-proxy.filter.assign") пока не подтверждена HAR.
    @GET("rci/show/dns-proxy/filter/presets")
    suspend fun getDnsFilterPresets(): Response<JsonElement>

    @GET("rci/show/dns-proxy/filter/profiles")
    suspend fun getDnsFilterProfiles(): Response<JsonElement>

    // VPN-сервер (L2TP/IKEv2). RCI-путь подтверждён строкой в
    // main-553997B.js: "show.vpn-server". Это отдельный REST-путь через
    // /rci/, не то же самое, что SSH-команда `show vpn-server`, которая
    // ранее вернула пустой ответ (см. ROADMAP.md) - стоит попробовать этот
    // путь отдельно, механизмы разные (RCI HTTP vs CLI shell).
    @GET("rci/show/vpn-server")
    suspend fun getVpnServerRaw(): Response<JsonElement>

    // Ниже - полный список дополнительных ЧТЕНИЙ, у каждого RCI-путь
    // подтверждён буквальной строкой "show.xxx" в main-553997B.js (список
    // сверен со ВСЕМИ 248 show-путями, что отдаёт сама веб-морда - см.
    // API-REFERENCE.md). Ни одного угаданного пути ниже нет. Set-команды
    // по-прежнему не пишем без HAR.

    /** Маршрутизация (Управление -> Сеть -> Маршрутизация на сайте). */
    @GET("rci/show/ip/route")
    suspend fun getIpRouteRaw(): Response<JsonElement>

    /** Статус мобильного (LTE/3G) соединения - актуально для Hero 4G+. */
    @GET("rci/show/mobile")
    suspend fun getMobileRaw(): Response<JsonElement>

    /** Состояние SIM-карты. */
    @GET("rci/show/sim")
    suspend fun getSimRaw(): Response<JsonElement>

    /** ARP/соседи в локальной сети. */
    @GET("rci/show/ip/neighbour")
    suspend fun getIpNeighbourRaw(): Response<JsonElement>

    /** Список учётных записей администратора роутера. */
    @GET("rci/show/user")
    suspend fun getUsersRaw(): Response<JsonElement>

    /** Проверка наличия обновления прошивки. */
    @GET("rci/show/system/update/status")
    suspend fun getSystemUpdateStatusRaw(): Response<JsonElement>

    /** Диапазон(ы) пула DHCP. */
    @GET("rci/show/ip/dhcp/pool")
    suspend fun getDhcpPoolRaw(): Response<JsonElement>

    /** Проброшенные автоматически через UPnP порты (дополняет ручную переадресацию). */
    @GET("rci/show/upnp/redirect")
    suspend fun getUpnpRedirectRaw(): Response<JsonElement>

    /** Общий статус интернет-соединения (агрегирует активный WAN). */
    @GET("rci/show/internet/status")
    suspend fun getInternetStatusRaw(): Response<JsonElement>

    /**
     * Сводка IntelliQoS (Приоритеты подключений).
     *
     * ПРОВЕРЕНО НА РЕАЛЬНОМ РОУТЕРЕ (06.08): этот путь как простой GET не
     * работает - роутер отвечает 200 OK, но с телом
     * {"status":[{"status":"error","code":"7471107","ident":"Command::Root",
     * "message":"no input [http/rci ...]."}]}. То есть "нет входных данных" -
     * похоже, ntce.summary не читается как простой лист-эндпоинт, а требует
     * батч-POST на корневой /rci/ (как show wans/show associations), а не
     * GET на /rci/show/.... Оставляю метод как есть (не угадываю новый
     * формат), но если понадобится реально показывать IntelliQoS - нужен
     * HAR открытия раздела "Приоритеты подключений" на веб-морде.
     */
    @GET("rci/show/ntce/summary")
    suspend fun getNtceSummaryRaw(): Response<JsonElement>

    @GET("rci/show/ntce/status")
    suspend fun getNtceStatusRaw(): Response<JsonElement>

    // USB-накопители. ПОДТВЕРЖДЕНО HAR (07.08): show/usb - реальный путь
    // (найден в JS-бандле веб-морды и в списке путей APK/прошивки).
    // eject/power-cycle - пути подтверждены (APK: RouterApi.ejectUsb,
    // interface/usb/power-cycle), точный формат тела запроса не проверен -
    // отправляем как {"system":{"eject":{"name":deviceName}}} и
    // {"interface":{"usb":{"power-cycle":true},"name":port}} по аналогии
    // с остальными командами интерфейсов в проекте.
    @GET("rci/show/usb")
    suspend fun getUsbDevicesRaw(): Response<JsonElement>

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
