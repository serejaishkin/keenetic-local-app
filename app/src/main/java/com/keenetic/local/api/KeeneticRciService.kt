package com.keenetic.local.api

import com.google.gson.JsonElement
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Retrofit service definition for KeeneticOS RCI (Remote Control Interface) REST API.
 *
 * Provides complete endpoints for:
 * 1. Base URL configuration and dynamic connection tuning (HTTP/HTTPS, ports, hosts, KeenDNS).
 * 2. Authentication and session handling (NDM challenge-response, HTTP Basic, Bearer tokens).
 * 3. Device lists and connected client telemetry (hotspot, DHCP bindings, ARP, Wi-Fi associations).
 * 4. System status, telemetry and metrics (uptime, CPU/RAM, components, KeenDNS, Cloud).
 * 5. Complete router settings and configurations:
 *    - Running and startup configuration (running-config, startup-config)
 *    - Network interfaces, VLANs, routing table, connection policies (PBR)
 *    - Wi-Fi 2.4/5GHz radios, Access Points, and Mesh Wi-Fi system (MWS)
 *    - Firewall, NAT, port forwarding, UPnP, and traffic control
 *    - Content and DNS filtering (AdGuard, NextDNS, Cloudflare, etc.)
 *    - User accounts and privileges
 *    - VPN servers and clients (WireGuard, OpenVPN, SSTP, IPsec)
 *    - USB, storage, media servers (SMB, FTP, DLNA, Transmission torrent)
 *    - Mobile cellular modems (4G/LTE/5G)
 *    - System diagnostics and logs (syslog, update status, NTCE)
 *    - Universal RCI batch command execution (/rci/) and path queries
 */
@JvmSuppressWildcards
interface KeeneticRciService {

    // ==========================================
    // 1. Authentication & Session Endpoints
    // ==========================================

    /**
     * Check authentication status with router.
     * When unauthorized (HTTP 401), headers X-NDM-Realm and X-NDM-Challenge are returned.
     */
    @GET("auth")
    suspend fun checkAuth(): Response<ResponseBody>

    /**
     * Check authentication status with an explicit Authorization header.
     */
    @GET("auth")
    suspend fun checkAuthWithHeader(
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    /**
     * Submit login authentication credentials (MD5/SHA256 challenge response or plain).
     */
    @POST("auth")
    suspend fun login(
        @Body credentials: Map<String, String>
    ): Response<ResponseBody>

    /**
     * Submit login authentication credentials with an explicit Authorization header.
     */
    @POST("auth")
    suspend fun loginWithHeader(
        @Header("Authorization") authHeader: String,
        @Body credentials: Map<String, String>
    ): Response<ResponseBody>

    /**
     * Terminate active router management session.
     */
    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>


    // ==========================================
    // 2. Device Lists & Connected Clients
    // ==========================================

    /**
     * Retrieve all registered and active client devices from Keenetic Hotspot.
     * Includes IP, MAC, hostname, rx/tx traffic, link speed, connection policy, and segment.
     */
    @GET("rci/show/ip/hotspot")
    suspend fun getHotspot(): Response<JsonElement>

    /**
     * Alias for [getHotspot] providing explicit naming for device list retrieval.
     */
    @GET("rci/show/ip/hotspot")
    suspend fun getHotspotDevices(): Response<JsonElement>

    /**
     * Hotspot summary statistics (count of active, registered, and blocked hosts).
     */
    @GET("rci/show/ip/hotspot/summary")
    suspend fun getHotspotSummary(): Response<JsonElement>

    /**
     * Retrieve active DHCP leases and static IP address reservations.
     */
    @GET("rci/show/ip/dhcp/bindings")
    suspend fun getDhcpBindings(): Response<JsonElement>

    /**
     * Retrieve the router's ARP (Address Resolution Protocol) cache table.
     */
    @GET("rci/show/ip/arp")
    suspend fun getArpTable(): Response<JsonElement>

    /**
     * Retrieve connected Wi-Fi wireless stations with RSSI, frequency band, PHY mode, and rates.
     */
    @GET("rci/show/associations")
    suspend fun getAssociations(): Response<JsonElement>

    /**
     * Retrieve connected Wi-Fi stations with explicit naming.
     */
    @GET("rci/show/associations")
    suspend fun getWirelessAssociations(): Response<JsonElement>

    /**
     * Retrieve IPv4/IPv6 neighbor discovery table.
     */
    @GET("rci/show/ip/neighbour")
    suspend fun getIpNeighbourRaw(): Response<JsonElement>

    @GET("rci/show/ip/neighbour")
    suspend fun getIpNeighbours(): Response<JsonElement>


    // ==========================================
    // 3. System Information & Status
    // ==========================================

    /**
     * Retrieve KeeneticOS release version, device model, architecture, and kernel info.
     */
    @GET("rci/show/version")
    suspend fun getVersion(): Response<JsonElement>

    /**
     * Retrieve system telemetry: CPU utilization, RAM usage, system uptime, and router clock.
     */
    @GET("rci/show/system")
    suspend fun getSystem(): Response<JsonElement>

    @GET("rci/show/system")
    suspend fun getSystemInfo(): Response<JsonElement>

    /**
     * Retrieve device identity: service tag, serial number, device model, and description.
     */
    @GET("rci/show/identification")
    suspend fun getIdentification(): Response<JsonElement>

    /**
     * Retrieve Keenetic Cloud connection status.
     */
    @GET("rci/show/cloud")
    suspend fun getCloudStatus(): Response<JsonElement>

    /**
     * Retrieve KeenDNS domain configuration, certificate status, and operational mode.
     */
    @GET("rci/show/ndns")
    suspend fun getNdnsStatus(): Response<JsonElement>

    /**
     * Retrieve global WAN Internet connectivity and gateway status.
     */
    @GET("rci/show/internet/status")
    suspend fun getInternetStatusRaw(): Response<JsonElement>

    @GET("rci/show/internet/status")
    suspend fun getInternetStatus(): Response<JsonElement>

    /**
     * Retrieve installed router software packages and components.
     */
    @GET("rci/show/components")
    suspend fun getComponents(): Response<JsonElement>

    /**
     * Retrieve active DNS name servers configured on router interfaces.
     */
    @GET("rci/show/ip/name-server")
    suspend fun getNameServers(): Response<JsonElement>

    /**
     * Retrieve firmware update check status and release channel details.
     */
    @GET("rci/show/system/update/status")
    suspend fun getSystemUpdateStatusRaw(): Response<JsonElement>

    @GET("rci/show/system/update/status")
    suspend fun getSystemUpdateStatus(): Response<JsonElement>

    /**
     * Retrieve router system event log (syslog).
     */
    @GET("rci/show/log")
    suspend fun getLogRaw(): Response<JsonElement>

    @GET("rci/show/log")
    suspend fun getSystemLogs(): Response<JsonElement>

    /**
     * NTCE traffic classification engine summary.
     */
    @GET("rci/show/ntce/summary")
    suspend fun getNtceSummaryRaw(): Response<JsonElement>

    /**
     * NTCE traffic classification engine status.
     */
    @GET("rci/show/ntce/status")
    suspend fun getNtceStatusRaw(): Response<JsonElement>


    // ==========================================
    // 4. Router Settings & Configuration (All Settings)
    // ==========================================

    /**
     * Retrieve full router running configuration (running-config) as raw text/CLI.
     */
    @GET("rci/show/running-config")
    suspend fun getRunningConfig(): Response<ResponseBody>

    /**
     * Retrieve full router startup configuration saved in NVRAM.
     */
    @GET("rci/show/startup-config")
    suspend fun getStartupConfig(): Response<ResponseBody>

    /**
     * Retrieve system configuration state and parameters.
     */
    @GET("rci/show/system/configuration")
    suspend fun getSystemConfiguration(): Response<JsonElement>


    // ==========================================
    // 5. Network Interfaces & Routing Settings
    // ==========================================

    /**
     * Retrieve configuration and state for all physical and virtual interfaces.
     */
    @GET("rci/show/interface")
    suspend fun getInterfaces(): Response<JsonElement>

    /**
     * Retrieve interface traffic throughput and packet statistics.
     */
    @GET("rci/show/interface/stat")
    suspend fun getInterfaceStatistics(): Response<JsonElement>

    /**
     * Retrieve settings and status for a specific network interface.
     */
    @GET("rci/show/interface/{name}")
    suspend fun getInterfaceDetails(
        @Path("name") name: String
    ): Response<JsonElement>

    /**
     * Retrieve IP routing table (connected, static, dynamic, and gateway routes).
     */
    @GET("rci/show/ip/route")
    suspend fun getRoutes(): Response<JsonElement>

    @GET("rci/show/ip/route")
    suspend fun getRoutingTable(): Response<JsonElement>

    /**
     * Retrieve static route configurations.
     */
    @GET("rci/show/ip/static")
    suspend fun getIpStaticRaw(): Response<JsonElement>

    @GET("rci/show/ip/static")
    suspend fun getStaticRoutes(): Response<JsonElement>

    /**
     * Retrieve Keenetic connection policies (PBR / policy-based routing tables).
     */
    @GET("rci/show/ip/policy")
    suspend fun getIpPolicyRaw(): Response<JsonElement>

    @GET("rci/show/ip/policy")
    suspend fun getConnectionPolicies(): Response<JsonElement>


    // ==========================================
    // 6. Wi-Fi & Mesh System Settings
    // ==========================================

    /**
     * 2.4 GHz Wi-Fi radio interface settings (channel, width, tx-power, state).
     */
    @GET("rci/show/interface/WifiMaster0")
    suspend fun getWifiMaster0(): Response<JsonElement>

    /**
     * 5 GHz Wi-Fi radio interface settings (channel, width, tx-power, state).
     */
    @GET("rci/show/interface/WifiMaster1")
    suspend fun getWifiMaster1(): Response<JsonElement>

    /**
     * Primary 2.4 GHz Access Point settings (SSID, security mode, WPA key, WPS).
     */
    @GET("rci/show/interface/WifiMaster0/AccessPoint0")
    suspend fun getWifiAccessPoint0(): Response<JsonElement>

    /**
     * Primary 5 GHz Access Point settings (SSID, security mode, WPA key, WPS).
     */
    @GET("rci/show/interface/WifiMaster1/AccessPoint0")
    suspend fun getWifiAccessPoint1(): Response<JsonElement>

    /**
     * Keenetic Mesh Wi-Fi System (MWS) node members and extenders.
     */
    @GET("rci/show/mws/member")
    suspend fun getMeshMembers(): Response<JsonElement>

    /**
     * Keenetic Mesh Wi-Fi System WLAN configurations.
     */
    @GET("rci/show/mws/wlan")
    suspend fun getMwsWlanRaw(): Response<JsonElement>

    @GET("rci/show/mws/wlan")
    suspend fun getMeshWlan(): Response<JsonElement>


    // ==========================================
    // 7. Security, Firewall, NAT & DNS Filters
    // ==========================================

    /**
     * NAT settings and port forwarding / port mapping rules.
     */
    @GET("rci/show/ip/nat")
    suspend fun getIpNatRaw(): Response<JsonElement>

    @GET("rci/show/ip/nat")
    suspend fun getNatRules(): Response<JsonElement>

    /**
     * Firewall filter rules.
     */
    @GET("rci/show/ip/firewall")
    suspend fun getFirewallRaw(): Response<JsonElement>

    @GET("rci/show/ip/firewall")
    suspend fun getFirewallRules(): Response<JsonElement>

    /**
     * IP Access Lists (ACLs).
     */
    @GET("rci/show/ip/access-list")
    suspend fun getAccessListRaw(): Response<JsonElement>

    @GET("rci/show/ip/access-list")
    suspend fun getAccessLists(): Response<JsonElement>

    /**
     * UPnP port redirection rules.
     */
    @GET("rci/show/upnp/redirect")
    suspend fun getUpnpRedirectRaw(): Response<JsonElement>

    @GET("rci/show/upnp/redirect")
    suspend fun getUpnpRedirects(): Response<JsonElement>

    /**
     * Traffic control / bandwidth shaping and QoS rules.
     */
    @GET("rci/show/ip/traffic-control")
    suspend fun getTrafficControl(): Response<JsonElement>

    /**
     * DNS proxy filter service presets (AdGuard, NextDNS, Cloudflare, CleanBrowsing, SafeDNS, etc.).
     */
    @GET("rci/show/dns-proxy/filter/presets")
    suspend fun getDnsFilterPresets(): Response<JsonElement>

    /**
     * Configured DNS filter profiles assigned to network segments or devices.
     */
    @GET("rci/show/dns-proxy/filter/profiles")
    suspend fun getDnsFilterProfiles(): Response<JsonElement>

    /**
     * DNS content filtering rules.
     */
    @GET("rci/show/ip/dns-filter")
    suspend fun getDnsFilters(): Response<JsonElement>

    /**
     * DHCP address pool settings and lease configuration.
     */
    @GET("rci/show/ip/dhcp/pool")
    suspend fun getDhcpPoolRaw(): Response<JsonElement>

    @GET("rci/show/ip/dhcp/pool")
    suspend fun getDhcpPool(): Response<JsonElement>


    // ==========================================
    // 8. User Accounts & Management Settings
    // ==========================================

    /**
     * Router user accounts and assigned access permissions.
     */
    @GET("rci/show/user")
    suspend fun getUsersRaw(): Response<JsonElement>

    @GET("rci/show/user")
    suspend fun getUsers(): Response<JsonElement>

    @GET("rci/show/users")
    suspend fun getUserAccounts(): Response<JsonElement>


    // ==========================================
    // 9. VPN Services Settings
    // ==========================================

    /**
     * WireGuard VPN interfaces, listen ports, and peer configurations.
     */
    @GET("rci/show/interface/Wireguard")
    suspend fun getWireguardSettings(): Response<JsonElement>

    /**
     * OpenVPN client/server interfaces and settings.
     */
    @GET("rci/show/interface/OpenVPN")
    suspend fun getOpenVpnSettings(): Response<JsonElement>

    /**
     * SSTP VPN client/server settings.
     */
    @GET("rci/show/interface/SSTP")
    suspend fun getSstpSettings(): Response<JsonElement>

    /**
     * Built-in VPN server status (IPsec, SSTP, OpenVPN, PPTP, Wireguard).
     */
    @GET("rci/show/vpn-server")
    suspend fun getVpnServerRaw(): Response<JsonElement>

    @GET("rci/show/vpn-server")
    suspend fun getVpnServerStatus(): Response<JsonElement>

    /**
     * IPsec and IKEv2 tunnel status.
     */
    @GET("rci/show/ipsec")
    suspend fun getIpsecStatus(): Response<JsonElement>


    // ==========================================
    // 10. USB, Storage & Network Media Services
    // ==========================================

    /**
     * Connected USB storage drives, partitions, and filesystems.
     */
    @GET("rci/show/media")
    suspend fun getMediaStorage(): Response<JsonElement>

    /**
     * Connected USB peripherals (disks, modems, hubs, printers).
     */
    @GET("rci/show/usb")
    suspend fun getUsbDevicesRaw(): Response<JsonElement>

    @GET("rci/show/usb")
    suspend fun getUsbDevices(): Response<JsonElement>

    /**
     * SMB / CIFS Windows file sharing service settings.
     */
    @GET("rci/show/smb")
    suspend fun getSmbSettings(): Response<JsonElement>

    /**
     * FTP server service settings.
     */
    @GET("rci/show/ftp")
    suspend fun getFtpSettings(): Response<JsonElement>

    /**
     * Transmission BitTorrent client settings and state.
     */
    @GET("rci/show/torrent")
    suspend fun getTorrentSettings(): Response<JsonElement>

    /**
     * DLNA / UPnP media server settings.
     */
    @GET("rci/show/dlna")
    suspend fun getDlnaSettings(): Response<JsonElement>


    // ==========================================
    // 11. Mobile / Cellular Broadband Modems
    // ==========================================

    /**
     * Mobile cellular modem status, network type (LTE/5G), and signal quality.
     */
    @GET("rci/show/mobile")
    suspend fun getMobileRaw(): Response<JsonElement>

    @GET("rci/show/mobile")
    suspend fun getMobileStatus(): Response<JsonElement>

    /**
     * Cellular SIM card status, operator, and PIN status.
     */
    @GET("rci/show/sim")
    suspend fun getSimRaw(): Response<JsonElement>

    @GET("rci/show/sim")
    suspend fun getSimStatus(): Response<JsonElement>

    /**
     * 4G/LTE USB Modem interface status.
     */
    @GET("rci/show/interface/UsbModem0")
    suspend fun getUsbModemInterface(): Response<JsonElement>


    // ==========================================
    // 12. Batch & Dynamic RCI Execution
    // ==========================================

    /**
     * Execute a batch of Keenetic RCI commands via POST /rci/
     * Example payload: [{"show": {"version": {}}}, {"show": {"interface": {}}}]
     */
    @POST("rci/")
    suspend fun executeRci(
        @Body commands: List<Map<String, Any>>
    ): Response<JsonElement>

    /**
     * Execute a batch of Keenetic RCI commands with an explicit Authorization header.
     */
    @POST("rci/")
    suspend fun executeRciWithHeader(
        @Header("Authorization") authHeader: String,
        @Body commands: List<Map<String, Any>>
    ): Response<JsonElement>

    /**
     * Direct query to any /rci/show/{path} (e.g. "version", "system", "interface", "ip/hotspot").
     */
    @GET("rci/show/{path}")
    suspend fun queryShow(
        @Path(value = "path", encoded = true) path: String
    ): Response<JsonElement>

    /**
     * Direct query to /rci/show/{path} with an explicit Authorization header.
     */
    @GET("rci/show/{path}")
    suspend fun queryShowWithHeader(
        @Header("Authorization") authHeader: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<JsonElement>

    /**
     * Generic query for any raw URL on the router.
     */
    @GET
    suspend fun getRaw(
        @Url url: String
    ): Response<JsonElement>

    /**
     * Generic query for any raw URL on the router with an explicit Authorization header.
     */
    @GET
    suspend fun getRawWithHeader(
        @Header("Authorization") authHeader: String,
        @Url url: String
    ): Response<JsonElement>

    /**
     * Generic path query compatibility method.
     */
    @GET
    suspend fun getRawPath(
        @Url path: String
    ): Response<JsonElement>


    // ==========================================
    // 13. Base URL Configuration & Client Factory
    // ==========================================

    companion object {
        const val DEFAULT_ROUTER_HOST = "192.168.1.1"
        const val DEFAULT_HTTP_PORT = 80
        const val DEFAULT_HTTPS_PORT = 443

        /**
         * Construct a normalized Keenetic RCI base URL string.
         *
         * @param host IP address (e.g. "192.168.1.1") or KeenDNS hostname (e.g. "myrouter.keenetic.pro")
         * @param port Network port (typically 80 for HTTP or 443 for HTTPS)
         * @param useHttps Whether to use secure HTTPS scheme
         * @return Normalized base URL ending with a trailing slash (e.g. "http://192.168.1.1:80/")
         */
        fun buildBaseUrl(
            host: String = DEFAULT_ROUTER_HOST,
            port: Int = DEFAULT_HTTP_PORT,
            useHttps: Boolean = false
        ): String {
            var cleanHost = host.trim()
            var isHttps = useHttps

            if (cleanHost.startsWith("https://", ignoreCase = true)) {
                isHttps = true
                cleanHost = cleanHost.substring(8)
            } else if (cleanHost.startsWith("http://", ignoreCase = true)) {
                isHttps = false
                cleanHost = cleanHost.substring(7)
            }

            if (cleanHost.contains("/")) {
                cleanHost = cleanHost.substringBefore("/")
            }

            var actualPort = port
            if (cleanHost.contains(":")) {
                val portStr = cleanHost.substringAfter(":")
                actualPort = portStr.toIntOrNull() ?: port
                cleanHost = cleanHost.substringBefore(":")
            }

            val scheme = if (isHttps) "https" else "http"
            return "$scheme://$cleanHost:$actualPort/"
        }

        /**
         * Ensures a base URL has scheme, valid host, and trailing slash.
         */
        fun normalizeBaseUrl(url: String): String {
            var cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
                cleanUrl = "http://$cleanUrl"
            }
            if (!cleanUrl.endsWith("/")) {
                cleanUrl += "/"
            }
            val parsed = cleanUrl.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid router base URL: $url")
            return parsed.toString()
        }

        /**
         * Quick factory method to create a [KeeneticRciService] instance with a base URL string.
         */
        fun create(
            baseUrl: String = "http://192.168.1.1:80/",
            authHeader: String? = null,
            cookieJar: CookieJar? = null
        ): KeeneticRciService {
            return Builder()
                .baseUrl(baseUrl)
                .authHeader(authHeader)
                .apply { if (cookieJar != null) cookieJar(cookieJar) }
                .build()
        }

        /**
         * Quick factory method to create a [KeeneticRciService] instance from host, port, and HTTPS mode.
         */
        fun create(
            host: String = DEFAULT_ROUTER_HOST,
            port: Int = DEFAULT_HTTP_PORT,
            useHttps: Boolean = false,
            authHeader: String? = null
        ): KeeneticRciService {
            return Builder()
                .host(host, port, useHttps)
                .authHeader(authHeader)
                .build()
        }

        /**
         * Returns a new [Builder] for configuring base URL, authentication, SSL, and timeouts.
         */
        fun builder(): Builder = Builder()
    }

    /**
     * Fluent builder for configuring and creating [KeeneticRciService] Retrofit clients.
     */
    class Builder {
        private var configuredBaseUrl: String = "http://192.168.1.1:80/"
        private var authHeader: String? = null
        private var connectTimeoutSec: Long = 12
        private var readTimeoutSec: Long = 18
        private var writeTimeoutSec: Long = 18
        private var trustAllSsl: Boolean = true
        private var enableLogging: Boolean = false
        private var customCookieJar: CookieJar? = null
        private var customOkHttpClient: OkHttpClient? = null

        /**
         * Sets the base URL from a full URL string.
         */
        fun baseUrl(url: String) = apply {
            this.configuredBaseUrl = normalizeBaseUrl(url)
        }

        /**
         * Sets the base URL by specifying router host/IP, port, and HTTPS mode.
         */
        fun host(
            host: String,
            port: Int = if (host.startsWith("https", ignoreCase = true)) DEFAULT_HTTPS_PORT else DEFAULT_HTTP_PORT,
            useHttps: Boolean = false
        ) = apply {
            this.configuredBaseUrl = buildBaseUrl(host, port, useHttps)
        }

        /**
         * Sets an explicit Authorization header (e.g. "Bearer <token>" or "Basic <base64>").
         */
        fun authHeader(header: String?) = apply {
            this.authHeader = header
        }

        /**
         * Configures HTTP Basic authentication credentials.
         */
        fun basicAuth(username: String, password: String) = apply {
            val creds = "$username:$password"
            val encoded = try {
                java.util.Base64.getEncoder().encodeToString(creds.toByteArray(Charsets.UTF_8))
            } catch (e: Throwable) {
                android.util.Base64.encodeToString(creds.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            }
            this.authHeader = "Basic $encoded"
        }

        /**
         * Configures Bearer token authorization header.
         */
        fun bearerToken(token: String) = apply {
            this.authHeader = "Bearer ${token.trim()}"
        }

        /**
         * Custom timeouts in seconds.
         */
        fun timeouts(connectSec: Long = 12, readSec: Long = 18, writeSec: Long = 18) = apply {
            this.connectTimeoutSec = connectSec
            this.readTimeoutSec = readSec
            this.writeTimeoutSec = writeSec
        }

        /**
         * Configures whether to trust all SSL certificates (vital for Keenetic local self-signed certs).
         */
        fun trustAllCertificates(enable: Boolean) = apply {
            this.trustAllSsl = enable
        }

        /**
         * Configures whether to enable basic HTTP request/response logging.
         */
        fun logging(enable: Boolean) = apply {
            this.enableLogging = enable
        }

        /**
         * Provides a custom CookieJar for session tracking (e.g. sys_auth cookies).
         */
        fun cookieJar(jar: CookieJar) = apply {
            this.customCookieJar = jar
        }

        /**
         * Directly override the underlying OkHttpClient.
         */
        fun okHttpClient(client: OkHttpClient) = apply {
            this.customOkHttpClient = client
        }

        /**
         * Build the configured [Retrofit] instance.
         */
        fun buildRetrofit(): Retrofit {
            val client = customOkHttpClient ?: createOkHttpClient()
            return Retrofit.Builder()
                .baseUrl(configuredBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        /**
         * Build and return the typed [KeeneticRciService] Retrofit client.
         */
        fun build(): KeeneticRciService {
            return buildRetrofit().create(KeeneticRciService::class.java)
        }

        private fun createOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)

            if (authHeader != null) {
                builder.addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("Authorization", authHeader!!)
                        .build()
                    chain.proceed(req)
                }
            }

            val jar = customCookieJar ?: DefaultSessionCookieJar()
            builder.cookieJar(jar)

            if (enableLogging) {
                builder.addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }

            if (trustAllSsl) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, trustAllCerts, SecureRandom())
                }
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            }

            return builder.build()
        }
    }

    /**
     * Default thread-safe session cookie jar for tracking Keenetic login authentication cookies.
     */
    class DefaultSessionCookieJar : CookieJar {
        private val cookiesStore = ConcurrentHashMap<String, MutableMap<String, Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val hostCookies = cookiesStore.computeIfAbsent(host) { ConcurrentHashMap() }
            cookies.forEach { cookie ->
                hostCookies[cookie.name] = cookie
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val hostCookies = cookiesStore[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            return hostCookies.values.filter { it.expiresAt > now }
        }

        fun clear() {
            cookiesStore.clear()
        }
    }
}

