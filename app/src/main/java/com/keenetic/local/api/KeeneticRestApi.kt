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
    val txSpeedKbps: Long = 0,
    val order: Int = 0,
    val channel: Int = 0,
    val channelWidth: Int = 0,
    val autoRescan: String = "",
    val powerPercent: Int = 100,
    val country: String = "",
    val standard: String = "",
    val txBurst: Boolean = false,
    val beamforming: Boolean = false,
    val qam256: Boolean = false,
    val downlinkOfdma: Boolean = false,
    val uplinkOfdma: Boolean = false,
    val downlinkMumimo: Boolean = false,
    val uplinkMumimo: Boolean = false,
    val targetWaketime: Boolean = false,
    val atfDisabled: Boolean = true,
    val atfInbound: Boolean = false,
    val bandSteeringEnabled: Boolean = false,
    val preferBand: String = "no-priority",
    val ssidHidden: Boolean = false,
    val wpsEnabled: Boolean = false,
    val ftEnabled: Boolean = false,
    val mdid: String = "",
    val iappKey: String = "",
    val rrmEnabled: Boolean = false,
    val peerIsolation: Boolean = false,
    val macAccessMode: String = "none"
)

data class IntelliQosCategory(
    val id: Int = 0,
    val name: String = "",
    val priority: Int = 5
)

data class IntelliQosConfig(
    val classifyEnabled: Boolean = false,
    val qosEnabled: Boolean = false,
    val categories: List<IntelliQosCategory> = emptyList()
)

data class MobileTraffic(
    val enable: Boolean = false,
    val limit: Long = 0,
    val unit: String = "MB",
    val multiplier: Long = 1048576,
    val dayOfMonth: Int = 1,
    val cycleResetEnabled: Boolean = false,
    val threshold: Int = 90,
    val smsWarningEnabled: Boolean = false,
    val smsLimitEnabled: Boolean = false,
    val smsPhone: String = "",
    val smsMessage: String = "",
    val disconnect: Boolean = false
)

fun unitMultiplier(unit: String): Long = when (unit.uppercase()) {
    "B" -> 1L
    "KB" -> 1024L
    "GB" -> 1073741824L
    "TB" -> 1099511627776L
    else -> 1048576L
}

fun intelliQosCategoryName(id: Int): String = when (id) {
    0 -> "Без категории"
    2048 -> "Общие"
    2049 -> "Обмен файлами (P2P)"
    2050 -> "Игры"
    2051 -> "Туннели"
    2052 -> "Работа и бизнес"
    2053 -> "Интернет-магазины"
    2054 -> "Конференц-связь и телефония"
    2055 -> "Мессенджеры"
    2056 -> "Потоковое видео"
    2057 -> "Мобильные приложения"
    2058 -> "Удалённое управление"
    2059 -> "Почта"
    2060 -> "Сетевые сервисы"
    2061 -> "Системные обновления"
    2062 -> "Интернет-серфинг"
    2063 -> "Другое"
    2111 -> "Криптовалюты"
    2121 -> "Социальные сети"
    2202 -> "Онлайн-игры"
    2211 -> "Интернет рекламы"
    2301 -> "Стриминг"
    2333 -> "Облачные сервисы"
    else -> "Категория $id"
}

fun intelliQosPriorityName(priority: Int): String = when (priority) {
    1 -> "Наивысший"
    2 -> "Критический"
    3 -> "Высокий"
    4 -> "Повышенный"
    5 -> "Нормальный (по умолчанию)"
    6 -> "Низкий"
    7 -> "Минимальный"
    else -> "Уровень $priority"
}

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
    val comment: String = "",
    val index: String = "",
    val type: String = "network",
    val prefix: String = "",
    val reject: Boolean = false,
    val enabled: Boolean = true
)

data class DnsRouteData(
    val id: String = "",
    val index: String = "",
    val group: String = "",
    val gateway: String = "",
    val interfaceName: String = "",
    val reject: Boolean = false,
    val enabled: Boolean = true
)

data class FqdnGroup(
    val name: String = "",
    val description: String = "",
    val domains: List<String> = emptyList()
)

data class RouterRouteEntry(
    val id: String = "",
    val destination: String = "",
    val gateway: String = "",
    val interfaceName: String = "",
    val isStatic: Boolean = false,
    val isRejecting: Boolean = false
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
    val executionTimeMs: Long
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

data class DnsFilterPreset(
    val id: String,
    val name: String,
    val provider: String = "",
    val description: String = "",
    val type: String = "",  // "adguard", "nextdns", "cloudflare", "safe", "custom"
    val enabled: Boolean = false,
    val profilesCount: Int = 0
)

data class DnsFilterProfile(
    val id: String,
    val name: String,
    val presetId: String = "",
    val presetName: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val assignedTo: List<String> = emptyList()  // interfaces or segments
)

data class VpnServerStatus(
    val enabled: Boolean = false,
    val type: String = "",  // "wireguard", "openvpn", "sstp", "ipsec", "l2tp"
    val interfaceName: String = "",
    val address: String = "",
    val port: Int = 0,
    val connectedClients: Int = 0,
    val totalBytesIn: Long = 0,
    val totalBytesOut: Long = 0,
    val peers: List<VpnPeer> = emptyList()
)

data class VpnPeer(
    val name: String = "",
    val publicKey: String = "",
    val endpoint: String = "",
    val allowedIp: String = "",
    val latestHandshake: String = "",
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0
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
    val name: String = "",
    val description: String? = null
)

data class SwitchPort(
    val id: String = "",
    val name: String = "",
    val state: String = "down",
    val speed: String = ""
)

data class NetworkHint(
    val gateway: String? = null,
    val currentIp: String? = null,
    val suggestedRouterIps: List<String> = emptyList()
)

data class SavedService(
    val name: String = "",
    val host: String = "",
    val port: String = "80",
    val username: String = "",
    val password: String = ""
)

// ===== Фаза 1: Новые модели =====

data class EnvironmentInfo(
    val temperature: Double = 0.0,
    val fanSpeed: Int = 0,
    val voltageCpu: Double = 0.0,
    val uptime: Long = 0
)

data class ProductInfo(
    val vendor: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val description: String = ""
)

data class NtpStatus(
    val enabled: Boolean = false,
    val server: String = "",
    val lastSync: String = "",
    val mode: String = ""
)

data class BackupStatus(
    val filename: String = "",
    val size: Long = 0,
    val date: String = ""
)

data class LedConfig(
    val enabled: Boolean = true,
    val mode: String = "enabled"
)

data class SystemMode(
    val mode: String = "router",
    val supported: String = "",
    val hwControlled: Boolean = false,
    val hwLocked: Boolean = false
)

data class IfaceStat(
    val id: String = "",
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBps: Long = 0,
    val txBps: Long = 0,
    val rxPackets: Long = 0,
    val txPackets: Long = 0,
    val rxErrors: Long = 0,
    val txErrors: Long = 0,
    val rxDropped: Long = 0,
    val txDropped: Long = 0
)

data class TrafficCounter(
    val id: String = "",
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val direction: String = ""
)

data class ChannelUtilization(
    val channel: Int = 0,
    val load: Int = 0,
    val flags: String = ""
)

data class SpectrumChannel(
    val number: Int = 0,
    val utilization: List<SpectrumPoint> = emptyList()
)

data class SpectrumPoint(
    val load: Int = 0,
    val flags: String = ""
)

data class CableDiagnosticResult(
    val interfaceName: String = "",
    val pair: Int = 0,
    val length: Int = 0,
    val status: String = ""
)

data class WpsStatus(
    val enabled: Boolean = false,
    val pin: String = "",
    val state: String = ""
)

data class MwsStatus(
    val enabled: Boolean = false,
    val role: String = "",
    val ssid: String = "",
    val channel: Int = 0
)

data class MwsMember(
    val name: String = "",
    val mac: String = "",
    val ip: String = "",
    val status: String = "",
    val firmware: String = ""
)

data class ConntrackEntry(
    val protocol: String = "",
    val srcIp: String = "",
    val srcPort: String = "",
    val dstIp: String = "",
    val dstPort: String = "",
    val state: String = "",
    val bytes: Long = 0
)

data class NatEntry(
    val proto: String = "",
    val srcIp: String = "",
    val srcPort: String = "",
    val dstIp: String = "",
    val dstPort: String = "",
    val interfaceName: String = ""
)

data class ArpEntry(
    val ip: String = "",
    val mac: String = "",
    val interfaceName: String = "",
    val state: String = ""
)

data class NeighbourEntry(
    val ip: String = "",
    val mac: String = "",
    val interfaceName: String = "",
    val state: String = ""
)

data class IpRule(
    val priority: Int = 0,
    val from: String = "",
    val to: String = "",
    val lookup: String = ""
)

data class WireguardServerStatus(
    val enabled: Boolean = false,
    val listenPort: Int = 0,
    val privateKey: String = "",
    val publicKey: String = "",
    val address: String = "",
    val peers: List<WireguardPeerFull> = emptyList()
)

data class WireguardPeerFull(
    val name: String = "",
    val publicKey: String = "",
    val presharedKey: String = "",
    val endpoint: String = "",
    val allowedIp: String = "",
    val keepalive: Int = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val latestHandshake: Long = 0
)

data class PptpServer(
    val enabled: Boolean = false,
    val interfaceName: String = "",
    val poolStart: String = "",
    val poolSize: String = "",
    val nat: Boolean = false,
    val multiLogin: Boolean = false,
    val encryption: Boolean = false
)

data class OcServer(
    val enabled: Boolean = false,
    val interfaceName: String = "",
    val poolStart: String = "",
    val poolSize: String = "",
    val nat: Boolean = false,
    val camouflage: Boolean = false
)

data class L2tpServer(
    val enabled: Boolean = false,
    val interfaceName: String = "",
    val poolStart: String = "",
    val poolSize: String = "",
    val nat: Boolean = false,
    val encryption: Boolean = false
)

data class Ikev2Server(
    val enabled: Boolean = false,
    val interfaceName: String = "",
    val poolStart: String = "",
    val poolSize: String = ""
)

data class SstpServerFull(
    val enabled: Boolean = false,
    val interfaceName: String = "",
    val poolStart: String = "",
    val poolSize: String = "",
    val camouflage: Boolean = false
)

data class IpsecStatus(
    val enabled: Boolean = false,
    val connections: List<IpsecConnection> = emptyList()
)

data class IpsecConnection(
    val name: String = "",
    val status: String = "",
    val localAddress: String = "",
    val remoteAddress: String = ""
)

data class NtceApp(
    val name: String = "",
    val category: String = "",
    val priority: Int = 0
)

data class NtceHost(
    val ip: String = "",
    val mac: String = "",
    val hostname: String = "",
    val os: String = "",
    val priority: Int = 0
)

data class NtceOs(
    val name: String = "",
    val hostsCount: Int = 0
)

data class NtceGroup(
    val name: String = "",
    val description: String = ""
)

data class NtceFilterProfileFull(
    val id: String = "",
    val name: String = "",
    val priority: Int = 0,
    val enabled: Boolean = true
)

data class DyndnsStatus(
    val enabled: Boolean = false,
    val provider: String = "",
    val hostname: String = "",
    val lastUpdate: String = ""
)

data class DyndnsProfile(
    val name: String = "",
    val hostname: String = "",
    val username: String = "",
    val enabled: Boolean = true
)

data class DyndnsUpdater(
    val name: String = "",
    val hostname: String = "",
    val lastUpdate: String = "",
    val status: String = ""
)

data class UpnpRedirect(
    val name: String = "",
    val proto: String = "",
    val externalPort: String = "",
    val internalIp: String = "",
    val internalPort: String = "",
    val enabled: Boolean = true
)

data class UpnpPinhole(
    val name: String = "",
    val proto: String = "",
    val port: String = "",
    val internalIp: String = "",
    val enabled: Boolean = true
)

data class TorrentStatusFull(
    val enabled: Boolean = false,
    val rpcPort: Int = 0,
    val rpcPublic: Boolean = false,
    val peerPort: Int = 0,
    val downloadDir: String = "",
    val activeTorrents: Int = 0,
    val totalTorrents: Int = 0,
    val downloadSpeed: Long = 0,
    val uploadSpeed: Long = 0
)

data class TorrentLocalAccount(
    val username: String = "",
    val enabled: Boolean = false
)

data class CloudStatus(
    val enabled: Boolean = false,
    val cloudType: String = "",
    val address: String = ""
)

data class CloudNdmp(
    val enabled: Boolean = false,
    val status: String = "",
    val prepared: Boolean = false
)

data class SshSettings(
    val enabled: Boolean = false,
    val port: Int = 22,
    val sftpEnabled: Boolean = false
)

data class SshFingerprint(
    val md5: String = "",
    val sha256: String = ""
)

data class SnmpView(
    val community: String = "",
    val enabled: Boolean = false
)

data class FtpSettings(
    val enabled: Boolean = false,
    val port: Int = 21,
    val anonymousAccess: Boolean = false
)

data class TelnetSettings(
    val enabled: Boolean = false,
    val port: Int = 23
)

data class HttpProxySettings(
    val enabled: Boolean = false,
    val port: Int = 3128
)

data class ServiceFlags(
    val ftp: Boolean = false,
    val ssh: Boolean = false,
    val telnet: Boolean = false,
    val ntp: Boolean = false,
    val httpProxy: Boolean = false
)

data class Ipv6Address(
    val address: String = "",
    val prefix: Int = 0,
    val interfaceName: String = "",
    val flags: String = ""
)

data class Ipv6Prefix(
    val prefix: String = "",
    val interfaceName: String = "",
    val preferred: Int = 0,
    val valid: Int = 0
)

data class Ipv6Route(
    val network: String = "",
    val prefix: Int = 0,
    val gateway: String = "",
    val interfaceName: String = "",
    val metric: Int = 0
)

data class Ipv6Subnet(
    val network: String = "",
    val prefix: Int = 0,
    val interfaceName: String = ""
)

data class Ipv6DhcpBinding(
    val duid: String = "",
    val iaId: String = "",
    val address: String = "",
    val hostname: String = ""
)

data class Ipv6Conntrack(
    val protocol: String = "",
    val srcIp: String = "",
    val dstIp: String = "",
    val state: String = ""
)

data class MediaStorage(
    val name: String = "",
    val label: String = "",
    val mounted: String = "",
    val fstype: String = "",
    val total: Long = 0,
    val free: Long = 0,
    val partitions: List<MediaPartition> = emptyList()
)

data class MediaPartition(
    val uuid: String = "",
    val label: String = "",
    val fstype: String = "",
    val size: Long = 0,
    val free: Long = 0,
    val state: String = ""
)

data class VpnConnection(
    val id: String,
    val name: String,
    val type: String,
    val isUp: Boolean,
    val state: String,
    val ip: String? = null,
    val protocol: String? = null,
    val upstream: String? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0
)

data class ComponentInfo(
    val name: String = "",
    val title: String = "",
    val installed: Boolean = false,
    val available: Boolean = false,
    val version: String = "",
    val description: String = ""
)

data class DeviceListEntryFull(
    val name: String = "",
    val mac: String = "",
    val ip: String = "",
    val hostname: String = "",
    val interfaceName: String = "",
    val type: String = "",
    val online: Boolean = false,
    val policy: String = "",
    val schedule: String = ""
)

data class InternetDetailedStatus(
    val connected: Boolean = false,
    val interfaceName: String = "",
    val ip: String = "",
    val mask: String = "",
    val gateway: String = "",
    val dns: List<String> = emptyList(),
    val speed: String = "",
    val type: String = ""
)

data class ObjectGroupFqdn(
    val name: String = "",
    val members: List<String> = emptyList()
)

data class MonitorStatus(
    val active: Boolean = false,
    val interfaceName: String = "",
    val filter: String = ""
)

@JvmSuppressWildcards
interface KeeneticRestApi : KeeneticRciService

