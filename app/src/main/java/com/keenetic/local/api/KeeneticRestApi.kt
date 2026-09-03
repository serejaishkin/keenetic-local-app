package com.keenetic.local.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class AuthStatus(
    val authorized: Boolean,
    val user: String?,
    val realm: String?
)

data class SystemInfo(
    val hostname: String = "Keenetic",
    val model: String = "KN-1010",
    val title: String = "Titan",
    val osVersion: String = "4.2.1",
    val uptime: Long = 0,
    val uptimeFormatted: String = "0д 0ч 0м",
    val cpus: Int = 2,
    val memoryTotal: Long = 512 * 1024 * 1024,
    val memoryFree: Long = 256 * 1024 * 1024,
    val memoryUsagePercent: Int = 50,
    val cpuUsagePercent: Int = 12,
    val arch: String = "mips",
    val kernel: String = "4.9.227",
    val hwVersion: String = "1.0",
    val manufacturer: String = "Keenetic Limited",
    val memoryBuffers: Long = 0L,
    val memoryCached: Long = 0L,
    val clockTime: String = "",
    val domainName: String = "",
    val rawShowVersionJson: String = "",
    val rawShowSystemJson: String = ""
)

data class ConnectedClient(
    val mac: String,
    val ip: String,
    val hostname: String,
    val displayName: String,
    val interfaceName: String,
    val active: Boolean,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxSpeedKbps: Long = 0,
    val txSpeedKbps: Long = 0,
    val wifiSsid: String? = null,
    val wifiRssi: Int? = null,
    val wifiBand: String? = null,
    val isBlocked: Boolean = false,
    val schedule: String? = null,
    val isStaticIp: Boolean = false,
    val policy: String = "Основная",
    val policyId: String = "",
    val wifiBandPreference: String = "Авто",
    val speedLimitMbps: Int = 0
)

/**
 * Connection Policy (Политика подключений / PBR) from KeeneticOS.
 * Retrieved via RCI endpoint: GET /rci/show/ip/policy
 * or CLI command: show ip policy
 */
data class ConnectionPolicy(
    val id: String, // e.g. "", "Policy0", "Policy1" (empty string = default segment policy / conform)
    val name: String, // e.g. "Основная (по умолчанию)" or "Cloud VPN"
    val description: String = "",
    val mark: Int? = null,
    val table4: Int? = null
)

data class RouterInterface(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val state: String,
    val isUp: Boolean,
    val ip: String? = null,
    val mask: String? = null,
    val uptime: Long = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxSpeedKbps: Long = 0,
    val txSpeedKbps: Long = 0
)

data class WifiNetworkInfo(
    val id: String,
    val ssid: String,
    val band: String,
    val enabled: Boolean,
    val channel: Int,
    val security: String,
    val clientsCount: Int
)

data class WirelessClient(
    val mac: String,
    val ip: String? = null,
    val hostname: String = "",
    val displayName: String = "",
    val band: String = "2.4 GHz",
    val rssi: Int? = null,
    val txRateKbps: Long = 0,
    val rxRateKbps: Long = 0,
    val txBytes: Long = 0,
    val rxBytes: Long = 0,
    val ssid: String = "",
    val ap: String = "",
    val mode: String = "",
    val active: Boolean = true,
    val isBlocked: Boolean = false
)

data class WifiStationStatus(
    val id: String = "WifiMaster0/WifiStation0",
    val masterRadio: String = "WifiMaster0",
    val isUp: Boolean = false,
    val connectedSsid: String? = null,
    val ip: String? = null,
    val mac: String? = null,
    val rssi: Int? = null,
    val description: String? = null,
    val state: String = "down"
)

data class WifiSiteSurveyEntry(
    val ssid: String,
    val bssid: String = "",
    val channel: Int = 0,
    val rssi: Int = 0,
    val encryption: String = "",
    val band: String = "2.4 GHz"
)

data class PortForwardingRule(
    val id: String = "",
    val name: String,
    val proto: String,
    val srcPort: String,
    val dstIp: String,
    val dstPort: String,
    val interfaceName: String = "ISP",
    val enabled: Boolean = true
)

data class FirewallRule(
    val id: String = "",
    val action: String,
    val proto: String,
    val srcIp: String,
    val dstIp: String,
    val dstPort: String,
    val interfaceName: String,
    val enabled: Boolean = true,
    val comment: String = ""
)

data class StaticRoute(
    val id: String = "",
    val network: String,
    val mask: String,
    val gateway: String,
    val interfaceName: String,
    val auto: Boolean = false,
    val comment: String = ""
)

data class LanSegment(
    val id: String,
    val name: String,
    val ip: String,
    val mask: String,
    val dhcpEnabled: Boolean,
    val dhcpStart: String,
    val dhcpEnd: String,
    val isolateClients: Boolean
)

data class DhcpBinding(
    val mac: String,
    val ip: String,
    val hostname: String,
    val active: Boolean
)

data class RouterUserAccount(
    val name: String,
    val tags: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)

data class SystemLogEntry(
    val timestamp: String,
    val facility: String,
    val level: String,
    val message: String
)

data class UsbStorageDevice(
    val name: String,
    val label: String,
    val vendor: String,
    val model: String,
    val sizeBytes: Long,
    val freeBytes: Long,
    val filesystem: String,
    val mountPoint: String,
    val shareSmb: Boolean = true,
    val shareFtp: Boolean = false,
    val shareDlna: Boolean = false
)

data class FirmwareStatus(
    val title: String,
    val model: String,
    val channel: String,
    val updateAvailable: Boolean,
    val availableVersion: String,
    val changelog: String,
    val autoUpdate: Boolean
)

data class DiagnosticsResult(
    val tool: String,
    val target: String,
    val output: String,
    val success: Boolean,
    val durationMs: Long
)

data class MobileModemStatus(
    val connected: Boolean = false,
    val operator: String = "",
    val networkType: String = "",
    val signalStrengthPercent: Int = 0,
    val ip: String = "",
    val interfaceName: String = "UsbModem0",
    val description: String = ""
)

@JvmSuppressWildcards
interface KeeneticRestApi : KeeneticRciService

