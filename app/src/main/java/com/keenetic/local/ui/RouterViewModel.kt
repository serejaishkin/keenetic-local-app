package com.keenetic.local.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.keenetic.local.KeeneticApp
import com.keenetic.local.api.*
import com.keenetic.local.discovery.AutoDiscovery
import com.keenetic.local.security.EncryptedStorage
import com.keenetic.local.ssh.KeeneticSshService
import com.keenetic.local.ssh.SshExecutionResult
import com.keenetic.local.util.AppLogger
import com.keenetic.local.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.keenetic.local.ui.screens.common.ApiCallState

class RouterViewModel : ViewModel() {
    private val repository = RouterRepository()
    private val encryptedStorage = EncryptedStorage(KeeneticApp.instance)
    private val dataStore = KeeneticApp.instance.dataStoreManager
    private val sshService = KeeneticSshService()

    enum class RebootMethod(val title: String, val description: String) {
        RCI("Keenetic RCI (REST API)", "Основной метод: вызов JSON RCI API через HTTP/HTTPS"),
        SSH("SSH Терминал (JSch)", "Вторичный метод: безопасное прямое соединение через JSch SSH (порт 22)")
    }

    private val _selectedRebootMethod = MutableStateFlow(RebootMethod.RCI)
    val selectedRebootMethod: StateFlow<RebootMethod> = _selectedRebootMethod.asStateFlow()

    private val _sshPort = MutableStateFlow("22")
    val sshPort: StateFlow<String> = _sshPort.asStateFlow()

    fun setSelectedRebootMethod(method: RebootMethod) {
        _selectedRebootMethod.value = method
    }

    fun setSshPort(port: String) {
        _sshPort.value = port
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isCheckingAutoLogin = MutableStateFlow(false)
    val isCheckingAutoLogin: StateFlow<Boolean> = _isCheckingAutoLogin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _detectedGatewayIp = MutableStateFlow<String?>(null)
    val detectedGatewayIp: StateFlow<String?> = _detectedGatewayIp.asStateFlow()

    private val _suggestedIps = MutableStateFlow<List<String>>(
        listOf("192.168.1.1", "192.168.0.1", "172.16.1.1", "my.keenetic.net")
    )
    val suggestedIps: StateFlow<List<String>> = _suggestedIps.asStateFlow()

    private val _savedIp = MutableStateFlow("192.168.1.1")
    val savedIp: StateFlow<String> = _savedIp.asStateFlow()

    private val _savedPort = MutableStateFlow("80")
    val savedPort: StateFlow<String> = _savedPort.asStateFlow()

    private val _savedUsername = MutableStateFlow("admin")
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    private val _savedUseHttps = MutableStateFlow(false)
    val savedUseHttps: StateFlow<Boolean> = _savedUseHttps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredRouters = MutableStateFlow<List<AutoDiscovery.DiscoveredRouter>>(emptyList())
    val discoveredRouters: StateFlow<List<AutoDiscovery.DiscoveredRouter>> = _discoveredRouters.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Int>>(listOf(12, 15, 14, 16, 13, 14, 18, 15, 13, 14))
    val cpuHistory: StateFlow<List<Int>> = _cpuHistory.asStateFlow()

    private val _ramHistory = MutableStateFlow<List<Int>>(listOf(38, 38, 39, 39, 39, 40, 39, 40, 39, 39))
    val ramHistory: StateFlow<List<Int>> = _ramHistory.asStateFlow()

    private val _isLivePolling = MutableStateFlow<Boolean>(true)
    val isLivePolling: StateFlow<Boolean> = _isLivePolling.asStateFlow()

    private val _pollingIntervalSeconds = MutableStateFlow<Int>(3)
    val pollingIntervalSeconds: StateFlow<Int> = _pollingIntervalSeconds.asStateFlow()

    private val _lastTelemetryTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastTelemetryTimestamp: StateFlow<Long> = _lastTelemetryTimestamp.asStateFlow()

    private var livePollingJob: Job? = null

    private val _clients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val clients: StateFlow<List<ConnectedClient>> = _clients.asStateFlow()

    private val _interfaces = MutableStateFlow<List<RouterInterface>>(emptyList())
    val interfaces: StateFlow<List<RouterInterface>> = _interfaces.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetworkInfo>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetworkInfo>> = _wifiNetworks.asStateFlow()

    private val _wirelessClients = MutableStateFlow<List<WirelessClient>>(emptyList())
    val wirelessClients: StateFlow<List<WirelessClient>> = _wirelessClients.asStateFlow()

    private val _wifiStationStatus = MutableStateFlow(WifiStationStatus())
    val wifiStationStatus: StateFlow<WifiStationStatus> = _wifiStationStatus.asStateFlow()

    private val _wifiScanResults = MutableStateFlow<List<WifiSiteSurveyEntry>>(emptyList())
    val wifiScanResults: StateFlow<List<WifiSiteSurveyEntry>> = _wifiScanResults.asStateFlow()

    private val _isWifiScanning = MutableStateFlow(false)
    val isWifiScanning: StateFlow<Boolean> = _isWifiScanning.asStateFlow()

    private val _isWifiLoading = MutableStateFlow(false)
    val isWifiLoading: StateFlow<Boolean> = _isWifiLoading.asStateFlow()

    private val _wifiActionMessage = MutableStateFlow<String?>(null)
    val wifiActionMessage: StateFlow<String?> = _wifiActionMessage.asStateFlow()

    private val _portForwardingRules = MutableStateFlow<List<PortForwardingRule>>(emptyList())
    val portForwardingRules: StateFlow<List<PortForwardingRule>> = _portForwardingRules.asStateFlow()

    private val _firewallRules = MutableStateFlow<List<FirewallRule>>(emptyList())
    val firewallRules: StateFlow<List<FirewallRule>> = _firewallRules.asStateFlow()

    private val _staticRoutes = MutableStateFlow<List<StaticRoute>>(emptyList())
    val staticRoutes: StateFlow<List<StaticRoute>> = _staticRoutes.asStateFlow()

    private val _lanSegments = MutableStateFlow<List<LanSegment>>(emptyList())
    val lanSegments: StateFlow<List<LanSegment>> = _lanSegments.asStateFlow()

    private val _connectionPolicies = MutableStateFlow<List<ConnectionPolicy>>(
        listOf(ConnectionPolicy("", "Основная (по умолчанию)", "Следовать политике сегмента сети (conform)"))
    )
    val connectionPolicies: StateFlow<List<ConnectionPolicy>> = _connectionPolicies.asStateFlow()

    private val _dhcpBindings = MutableStateFlow<List<DhcpBinding>>(emptyList())
    val dhcpBindings: StateFlow<List<DhcpBinding>> = _dhcpBindings.asStateFlow()

    private val _userAccounts = MutableStateFlow<List<RouterUserAccount>>(emptyList())
    val userAccounts: StateFlow<List<RouterUserAccount>> = _userAccounts.asStateFlow()

    private val _usbStorageList = MutableStateFlow<List<UsbStorageDevice>>(emptyList())
    val usbStorageList: StateFlow<List<UsbStorageDevice>> = _usbStorageList.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLogEntry>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogEntry>> = _systemLogs.asStateFlow()

    private val _mobileModemStatus = MutableStateFlow<MobileModemStatus>(MobileModemStatus())
    val mobileModemStatus: StateFlow<MobileModemStatus> = _mobileModemStatus.asStateFlow()

    private val _firmwareStatus = MutableStateFlow<FirmwareStatus?>(null)
    val firmwareStatus: StateFlow<FirmwareStatus?> = _firmwareStatus.asStateFlow()

    private val _diagnosticsResult = MutableStateFlow<DiagnosticsResult?>(null)
    val diagnosticsResult: StateFlow<DiagnosticsResult?> = _diagnosticsResult.asStateFlow()

    private val _dnsFilterPresets = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val dnsFilterPresets: StateFlow<ApiCallState> = _dnsFilterPresets.asStateFlow()

    private val _dnsFilterProfiles = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val dnsFilterProfiles: StateFlow<ApiCallState> = _dnsFilterProfiles.asStateFlow()

    private val _dnsFilterPresetList = MutableStateFlow<List<DnsFilterPreset>>(emptyList())
    val dnsFilterPresetList: StateFlow<List<DnsFilterPreset>> = _dnsFilterPresetList.asStateFlow()

    private val _dnsFilterProfileList = MutableStateFlow<List<DnsFilterProfile>>(emptyList())
    val dnsFilterProfileList: StateFlow<List<DnsFilterProfile>> = _dnsFilterProfileList.asStateFlow()

    private val _vpnServerRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val vpnServerRaw: StateFlow<ApiCallState> = _vpnServerRaw.asStateFlow()

    private val _vpnServerStatus = MutableStateFlow<VpnServerStatus?>(null)
    val vpnServerStatus: StateFlow<VpnServerStatus?> = _vpnServerStatus.asStateFlow()

    private val _isRebooting = MutableStateFlow(false)
    val isRebooting: StateFlow<Boolean> = _isRebooting.asStateFlow()

    private val _rebootMessage = MutableStateFlow<String?>(null)
    val rebootMessage: StateFlow<String?> = _rebootMessage.asStateFlow()

    private val _rawRunningConfig = MutableStateFlow<String>("")
    val rawRunningConfig: StateFlow<String> = _rawRunningConfig.asStateFlow()

    private val _parsedConfig = MutableStateFlow<KeeneticParsedConfig?>(null)
    val parsedConfig: StateFlow<KeeneticParsedConfig?> = _parsedConfig.asStateFlow()

    private val _configLoading = MutableStateFlow<Boolean>(false)
    val configLoading: StateFlow<Boolean> = _configLoading.asStateFlow()

    private val _routerIp = MutableStateFlow("")
    val routerIp: StateFlow<String> = _routerIp.asStateFlow()

    private val _routerLogin = MutableStateFlow("admin")
    val routerLogin: StateFlow<String> = _routerLogin.asStateFlow()

    private val _autoLoginEnabled = MutableStateFlow(false)
    val autoLoginEnabled: StateFlow<Boolean> = _autoLoginEnabled.asStateFlow()

    private val _networkHint = MutableStateFlow(NetworkHint())
    val networkHint: StateFlow<NetworkHint> = _networkHint.asStateFlow()

    private val _sshOutput = MutableStateFlow("")
    val sshOutput: StateFlow<String> = _sshOutput.asStateFlow()

    private val _savedServices = MutableStateFlow<List<SavedService>>(emptyList())
    val savedServices: StateFlow<List<SavedService>> = _savedServices.asStateFlow()

    private val _usbDevicesRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val usbDevicesRaw: StateFlow<ApiCallState> = _usbDevicesRaw.asStateFlow()

    private val _nameServers = MutableStateFlow<List<DnsServerInfo>>(emptyList())
    val nameServers: StateFlow<List<DnsServerInfo>> = _nameServers.asStateFlow()

    private val _dohUpstream = MutableStateFlow<List<String>>(emptyList())
    val dohUpstream: StateFlow<List<String>> = _dohUpstream.asStateFlow()

    private val _autoUpdateEnabled = MutableStateFlow<Boolean?>(null)
    val autoUpdateEnabled: StateFlow<Boolean?> = _autoUpdateEnabled.asStateFlow()

    private val _systemUpdateStatusRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val systemUpdateStatusRaw: StateFlow<ApiCallState> = _systemUpdateStatusRaw.asStateFlow()

    private val _usersRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val usersRaw: StateFlow<ApiCallState> = _usersRaw.asStateFlow()

    private val _dhcpPoolRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val dhcpPoolRaw: StateFlow<ApiCallState> = _dhcpPoolRaw.asStateFlow()

    private val _ntceSummaryRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val ntceSummaryRaw: StateFlow<ApiCallState> = _ntceSummaryRaw.asStateFlow()

    private val _vpnServer = MutableStateFlow<VpnServerConfig?>(null)
    val vpnServer: StateFlow<VpnServerConfig?> = _vpnServer.asStateFlow()

    private val _cliExecutionResult = MutableStateFlow<String?>(null)
    val cliExecutionResult: StateFlow<String?> = _cliExecutionResult.asStateFlow()

    private val _isExecutingCli = MutableStateFlow<Boolean>(false)
    val isExecutingCli: StateFlow<Boolean> = _isExecutingCli.asStateFlow()

    private val _saveConfigMessage = MutableStateFlow<String?>(null)
    val saveConfigMessage: StateFlow<String?> = _saveConfigMessage.asStateFlow()

    init {
        detectNetworkGateway()
        loadSavedSettingsAndAutoLogin()
    }

    fun detectNetworkGateway() {
        viewModelScope.launch(Dispatchers.IO) {
            val gateway = NetworkUtils.detectRouterGatewayIp(KeeneticApp.instance)
            if (!gateway.isNullOrBlank()) {
                _detectedGatewayIp.value = gateway
            }
            val suggested = NetworkUtils.getSuggestedRouterIps(KeeneticApp.instance)
            _suggestedIps.value = suggested
        }
    }

    fun scanNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val gw = _detectedGatewayIp.value ?: _savedIp.value
                val base = if (gw.contains(".")) gw.substringBeforeLast(".") else "192.168.1"
                val found = AutoDiscovery.discover(base)
                _discoveredRouters.value = found
            } catch (e: Exception) {
                AppLogger.logError("scanNetwork", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun loadSavedSettingsAndAutoLogin() {
        viewModelScope.launch {
            dataStore.routerIp.collect { ip ->
                if (ip.isNotBlank()) _savedIp.value = ip
            }
        }
        viewModelScope.launch {
            dataStore.routerPort.collect { port ->
                if (port.isNotBlank()) _savedPort.value = port
            }
        }
        viewModelScope.launch {
            dataStore.routerUsername.collect { user ->
                if (user.isNotBlank()) _savedUsername.value = user
            }
        }
        viewModelScope.launch {
            dataStore.useHttps.collect { https ->
                _savedUseHttps.value = https
            }
        }
        viewModelScope.launch {
            dataStore.autoLogin.collect { auto ->
                if (auto && !_isLoggedIn.value) {
                    val savedPass = encryptedStorage.getPassword()
                    if (!savedPass.isNullOrBlank()) {
                        val host = _savedIp.value
                        val port = _savedPort.value
                        val user = _savedUsername.value
                        val https = _savedUseHttps.value
                        login(host, port, user, savedPass, https)
                    }
                }
            }
        }
    }

    fun loadDemoData() {
        val sampleShowVersionJson = """
{
  "model": "KN-1811",
  "device": "Keenetic Titan (KN-1811)",
  "title": "Titan (KN-1811)",
  "release": "5.1.1",
  "arch": "aarch64",
  "kernel": "5.10.160",
  "ndmversion": "5.01.A.1.0-0",
  "hw_version": "rev.A",
  "hw_id": "KN-1811-A",
  "manufacturer": "Keenetic Limited"
}
        """.trimIndent()

        val sampleShowSystemJson = """
{
  "hostname": "Keenetic-Titan",
  "domainname": "local",
  "cpus": 4,
  "cpuload": 14,
  "cpu_freq": "1350 MHz",
  "uptime": 259200,
  "memory": "204800/524288",
  "memtotal": 524288,
  "memfree": 319488,
  "membuffers": 32768,
  "memcached": 98304,
  "clock": {
    "date": "2026-09-03",
    "time": "16:32:00",
    "timezone": "MSK+03"
  }
}
        """.trimIndent()

        _systemInfo.value = SystemInfo(
            hostname = "Keenetic-Titan",
            model = "KN-1811",
            title = "Titan (KN-1811)",
            osVersion = "5.1.1",
            uptime = 259200,
            uptimeFormatted = "3д 14ч 32м 00с",
            cpus = 4,
            memoryTotal = 512 * 1024 * 1024,
            memoryFree = 312 * 1024 * 1024,
            memoryUsagePercent = 39,
            cpuUsagePercent = 14,
            arch = "aarch64",
            kernel = "5.10.160",
            hwVersion = "rev.A",
            manufacturer = "Keenetic Limited",
            memoryBuffers = 32 * 1024 * 1024,
            memoryCached = 96 * 1024 * 1024,
            clockTime = "2026-09-03 16:32:00 MSK",
            domainName = "local",
            rawShowVersionJson = sampleShowVersionJson,
            rawShowSystemJson = sampleShowSystemJson
        )

        _clients.value = listOf(
            ConnectedClient(
                mac = "a4:83:e7:21:54:19",
                ip = "192.168.1.101",
                hostname = "iPhone-15-Pro",
                displayName = "iPhone 15 Pro (Сергей)",
                interfaceName = "WifiMaster0/AccessPoint0",
                active = true,
                rxSpeedKbps = 14200,
                txSpeedKbps = 850,
                wifiSsid = "Keenetic-Home-5G",
                wifiRssi = -52,
                wifiBand = "5 GHz"
            ),
            ConnectedClient(
                mac = "3c:22:fb:49:10:aa",
                ip = "192.168.1.102",
                hostname = "MacBook-Pro-M3",
                displayName = "MacBook Pro M3 Max",
                interfaceName = "WifiMaster0/AccessPoint0",
                active = true,
                rxSpeedKbps = 48500,
                txSpeedKbps = 3200,
                wifiSsid = "Keenetic-Home-5G",
                wifiRssi = -46,
                wifiBand = "5 GHz"
            ),
            ConnectedClient(
                mac = "ec:71:db:9a:11:02",
                ip = "192.168.1.110",
                hostname = "LG-OLED-TV",
                displayName = "LG OLED C3 65\"",
                interfaceName = "GigabitEthernet0/2",
                active = true,
                rxSpeedKbps = 24000,
                txSpeedKbps = 120
            ),
            ConnectedClient(
                mac = "44:91:60:88:fe:90",
                ip = "192.168.1.120",
                hostname = "PlayStation-5",
                displayName = "Sony PlayStation 5",
                interfaceName = "GigabitEthernet0/1",
                active = true,
                rxSpeedKbps = 185000,
                txSpeedKbps = 4500
            )
        )

        _interfaces.value = listOf(
            RouterInterface(
                id = "GigabitEthernet0/Vlan2",
                name = "ISP (Провайдер)",
                description = "Основное подключение Gigabit Ethernet",
                type = "Ethernet",
                state = "up",
                isUp = true,
                ip = "185.120.44.18",
                mask = "255.255.255.0",
                uptime = 259200,
                rxBytes = 148590120194L,
                txBytes = 32410291044L,
                rxSpeedKbps = 24500,
                txSpeedKbps = 2100
            ),
            RouterInterface(
                id = "Bridge0",
                name = "Home (Домашняя сеть)",
                description = "Основной LAN сегмент 192.168.1.1/24",
                type = "Bridge",
                state = "up",
                isUp = true,
                ip = "192.168.1.1",
                mask = "255.255.255.0",
                uptime = 259200,
                rxBytes = 32410291044L,
                txBytes = 148590120194L,
                rxSpeedKbps = 2100,
                txSpeedKbps = 24500
            ),
            RouterInterface(
                id = "Wireguard0",
                name = "WireGuard VPN",
                description = "Туннель до сервера Amsterdam",
                type = "WireGuard",
                state = "up",
                isUp = true,
                ip = "10.8.0.2",
                mask = "255.255.255.0",
                uptime = 124500,
                rxBytes = 1890120194L,
                txBytes = 410291044L,
                rxSpeedKbps = 1240,
                txSpeedKbps = 180
            )
        )

        _wifiNetworks.value = listOf(
            WifiNetworkInfo("WifiMaster0/AccessPoint0", "Keenetic-Home-5G", "5 GHz (Wi-Fi 6 AX)", true, 36, "WPA2/WPA3-PSK", 2),
            WifiNetworkInfo("WifiMaster1/AccessPoint0", "Keenetic-Home-2.4G", "2.4 GHz (Wi-Fi 4 N)", true, 6, "WPA2-PSK", 2),
            WifiNetworkInfo("WifiMaster0/AccessPoint1", "Keenetic-Guest", "5 GHz", false, 36, "WPA2-PSK", 0)
        )

        _wirelessClients.value = listOf(
            WirelessClient(
                mac = "a4:83:e7:21:54:19",
                ip = "192.168.1.101",
                hostname = "iPhone-15-Pro",
                displayName = "iPhone 15 Pro (Сергей)",
                band = "5 GHz",
                rssi = -52,
                txRateKbps = 1201000,
                rxRateKbps = 1080000,
                ssid = "Keenetic-Home-5G",
                ap = "WifiMaster0/AccessPoint0",
                mode = "802.11ax (Wi-Fi 6)",
                active = true
            ),
            WirelessClient(
                mac = "3c:22:fb:49:10:aa",
                ip = "192.168.1.102",
                hostname = "MacBook-Pro-M3",
                displayName = "MacBook Pro M3 Max",
                band = "5 GHz",
                rssi = -46,
                txRateKbps = 2402000,
                rxRateKbps = 2402000,
                ssid = "Keenetic-Home-5G",
                ap = "WifiMaster0/AccessPoint0",
                mode = "802.11ax (Wi-Fi 6)",
                active = true
            ),
            WirelessClient(
                mac = "50:ec:50:88:14:bb",
                ip = "192.168.1.115",
                hostname = "Roborock-S8",
                displayName = "Робот-пылесос Roborock S8",
                band = "2.4 GHz",
                rssi = -64,
                txRateKbps = 72000,
                rxRateKbps = 72000,
                ssid = "Keenetic-Home-2.4G",
                ap = "WifiMaster1/AccessPoint0",
                mode = "802.11n (Wi-Fi 4)",
                active = true
            ),
            WirelessClient(
                mac = "74:ac:b9:2d:48:fe",
                ip = "192.168.1.118",
                hostname = "Aqara-Hub-M2",
                displayName = "Шлюз умного дома Aqara M2",
                band = "2.4 GHz",
                rssi = -58,
                txRateKbps = 65000,
                rxRateKbps = 65000,
                ssid = "Keenetic-Home-2.4G",
                ap = "WifiMaster1/AccessPoint0",
                mode = "802.11n (Wi-Fi 4)",
                active = true
            )
        )

        _wifiStationStatus.value = WifiStationStatus(
            id = "WifiMaster0/WifiStation0",
            masterRadio = "WifiMaster0",
            isUp = false,
            connectedSsid = null,
            ip = null,
            mac = "50:ff:20:00:1a:02",
            rssi = null,
            state = "down"
        )

        _portForwardingRules.value = listOf(
            PortForwardingRule("1", "Plex Media Server", "TCP", "32400", "192.168.1.102", "32400", "ISP", true),
            PortForwardingRule("2", "SSH Server", "TCP", "2222", "192.168.1.102", "22", "ISP", true),
            PortForwardingRule("3", "Minecraft Server", "TCP/UDP", "25565", "192.168.1.102", "25565", "ISP", false)
        )

        _firewallRules.value = listOf(
            FirewallRule("1", "permit", "TCP", "any", "192.168.1.102", "32400", "ISP", true, "Разрешить Plex"),
            FirewallRule("2", "permit", "TCP", "any", "192.168.1.102", "22", "ISP", true, "Внешний доступ по SSH"),
            FirewallRule("3", "deny", "IP", "any", "192.168.1.0/24", "any", "ISP", true, "Блокировать остальной входящий трафик")
        )

        _staticRoutes.value = listOf(
            StaticRoute("1", "10.8.0.0", "255.255.255.0", "10.8.0.1", "Wireguard0", false, "Маршрут к ресурсам офиса"),
            StaticRoute("2", "192.168.2.0", "255.255.255.0", "192.168.1.254", "Bridge0", false, "Гостевая подсеть")
        )

        _lanSegments.value = listOf(
            LanSegment("Home", "Домашняя сеть", "192.168.1.1", "255.255.255.0", true, "192.168.1.33", "192.168.1.199", false),
            LanSegment("Guest", "Гостевая сеть", "192.168.2.1", "255.255.255.0", true, "192.168.2.10", "192.168.2.99", true)
        )

        _userAccounts.value = listOf(
            RouterUserAccount(name = "admin", tags = listOf("admin", "http", "cli", "ssh"), permissions = listOf("Полный доступ")),
            RouterUserAccount(name = "family", tags = listOf("smb", "media"), permissions = listOf("Чтение/запись SMB")),
            RouterUserAccount(name = "vpn_client", tags = listOf("vpn", "wireguard"), permissions = listOf("VPN доступ"))
        )

        _usbStorageList.value = listOf(
            UsbStorageDevice(
                name = "Samsung Portable SSD T7",
                label = "MediaStorage",
                vendor = "Samsung",
                model = "T7 Shield",
                sizeBytes = 500107862016L,
                freeBytes = 320420102144L,
                filesystem = "exFAT",
                mountPoint = "/tmp/mnt/MediaStorage",
                shareSmb = true,
                shareFtp = true,
                shareDlna = true
            ),
            UsbStorageDevice(
                name = "Kingston DataTraveler 3.0",
                label = "BackupFlash",
                vendor = "Kingston",
                model = "DT50",
                sizeBytes = 64172818432L,
                freeBytes = 48119283712L,
                filesystem = "NTFS",
                mountPoint = "/tmp/mnt/BackupFlash",
                shareSmb = true,
                shareFtp = false,
                shareDlna = false
            )
        )

        _systemLogs.value = listOf(
            SystemLogEntry("09:42:15", "ndm", "info", "Core::SystemServer: HTTP authentication succeeded for user 'admin'."),
            SystemLogEntry("09:40:02", "ndm", "notice", "Dhcp::Server: assigned IP 192.168.1.105 to iPhone-14-Pro (ac:bc:32:89:11:22)."),
            SystemLogEntry("09:38:22", "wificore", "info", "WifiMaster0/AccessPoint0: STA 3c:22:fb:a4:12:90 802.11k/v fast BSS transition to 5 GHz (RSSI -58 dBm)."),
            SystemLogEntry("09:35:10", "wireguard", "info", "Wireguard0: handshake completed with peer 'Amsterdam-NL' (endpoint 185.220.101.5:51820)."),
            SystemLogEntry("09:30:00", "ndm", "notice", "Service: KeeneticOS Cloud Agent connected to KeenDNS portal.")
        )

        _mobileModemStatus.value = MobileModemStatus(
            connected = true,
            operator = "MegaFon",
            networkType = "LTE (4G)",
            signalStrengthPercent = 85,
            ip = "10.145.22.84",
            interfaceName = "UsbModem0",
            description = "Huawei E3372 4G Dongle"
        )

        _firmwareStatus.value = FirmwareStatus(
            title = "KeeneticOS 5.1.1",
            model = "KN-1811 (Titan)",
            channel = "Release",
            updateAvailable = true,
            availableVersion = "5.2.0",
            changelog = "• Оптимизирована работа Wi-Fi Mesh и Fast Roaming (802.11k/r/v)\n• Обновлены компоненты ядра WireGuard и аппаратного NAT\n• Повышена стабильность передачи файлов по SMBv3 на внешние накопители USB 3.0",
            autoUpdate = true
        )

        _isDemoMode.value = true
        _isLoggedIn.value = true
    }

    fun exitDemoMode() {
        _isDemoMode.value = false
        _isLoggedIn.value = false
    }

    fun login(host: String, port: String, user: String, pass: String, useHttps: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.initApi(host, port, useHttps)
                val authResult = repository.authenticate(user, pass)
                if (authResult.success) {
                    _isLoggedIn.value = true
                    _isDemoMode.value = false
                    encryptedStorage.savePassword(pass)
                    dataStore.saveSettings(host, port, user, autoLogin = true, useHttps = useHttps)
                    refreshAll()
                } else {
                    _error.value = authResult.errorMessage ?: "Ошибка авторизации. Проверьте логин и пароль."
                }
            } catch (e: Exception) {
                _error.value = "Не удалось подключиться к $host: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAll() {
        if (_isDemoMode.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadSystemInfo()
                loadConnectionPolicies()
                loadClients()
                loadInterfaces()
                loadDhcpBindings()
                loadPortForwardingRules()
                loadFirewallRules()
                loadLanSegments()
                loadStaticRoutes()
                loadUsers()
                loadUsbDevices()
                loadFirmwareStatus()
                loadSystemLogs()
                loadMobileStatus()
            } catch (e: Exception) {
                AppLogger.logError("refreshAll", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSystemInfo() {
        viewModelScope.launch {
            if (_isDemoMode.value) {
                val current = _systemInfo.value ?: SystemInfo()
                val nextCpu = (current.cpuUsagePercent + (-3..4).random()).coerceIn(8, 55)
                val nextRam = (current.memoryUsagePercent + (-1..1).random()).coerceIn(35, 45)
                val nextUptime = current.uptime + _pollingIntervalSeconds.value
                val days = nextUptime / 86400
                val hours = (nextUptime % 86400) / 3600
                val mins = (nextUptime % 3600) / 60
                val secs = nextUptime % 60
                val uptimeFormatted = "${days}д ${hours}ч ${mins}м ${secs}с"

                _cpuHistory.value = (_cpuHistory.value + nextCpu).takeLast(25)
                _ramHistory.value = (_ramHistory.value + nextRam).takeLast(25)
                _lastTelemetryTimestamp.value = System.currentTimeMillis()

                _systemInfo.value = current.copy(
                    cpuUsagePercent = nextCpu,
                    memoryUsagePercent = nextRam,
                    uptime = nextUptime,
                    uptimeFormatted = uptimeFormatted
                )
                return@launch
            }

            try {
                val sysRes = repository.queryShow("system")
                val verRes = repository.queryShow("version")

                val sysObj = sysRes?.takeIf { it.isJsonObject }?.asJsonObject
                val verObj = verRes?.takeIf { it.isJsonObject }?.asJsonObject

                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                val rawVersion = verRes?.let { gson.toJson(it) } ?: ""
                val rawSystem = sysRes?.let { gson.toJson(it) } ?: ""

                val hostname = sysObj?.get("hostname")?.takeIf { it.isJsonPrimitive }?.asString ?: "Keenetic"
                val domainName = sysObj?.get("domainname")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                val uptime = sysObj?.get("uptime")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L

                val days = uptime / 86400
                val hours = (uptime % 86400) / 3600
                val mins = (uptime % 3600) / 60
                val secs = uptime % 60
                val uptimeFormatted = if (uptime > 0) "${days}д ${hours}ч ${mins}м ${secs}с" else "0д 0ч 0м 0с"

                val cpuLoad = sysObj?.get("cpuload")?.takeIf { it.isJsonPrimitive }?.runCatching {
                    asString.trimEnd('%').toFloatOrNull()?.toInt() ?: 12
                }?.getOrDefault(12) ?: 12

                var memTotal = 512L * 1024 * 1024
                var memFree = 256L * 1024 * 1024
                val memBuffers = sysObj?.get("membuffers")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong * 1024 }?.getOrDefault(0L) ?: 0L
                val memCached = sysObj?.get("memcached")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong * 1024 }?.getOrDefault(0L) ?: 0L
                var memUsage = 40

                val memRaw = sysObj?.get("memory")?.takeIf { it.isJsonPrimitive }?.asString
                if (!memRaw.isNullOrBlank()) {
                    if (memRaw.contains("/")) {
                        val used = memRaw.substringBefore("/").trim().toLongOrNull() ?: 0L
                        val total = memRaw.substringAfter("/").trim().toLongOrNull() ?: 0L
                        if (total > 0) {
                            memTotal = total * 1024
                            memFree = (total - used).coerceAtLeast(0) * 1024
                            memUsage = ((used * 100) / total).toInt().coerceIn(1, 99)
                        }
                    } else if (memRaw.contains("%")) {
                        memUsage = memRaw.trimEnd('%').toIntOrNull()?.coerceIn(1, 99) ?: 40
                    }
                } else if (sysObj?.has("memtotal") == true) {
                    val total = sysObj.get("memtotal").asLong * 1024
                    val free = (sysObj.get("memfree")?.asLong ?: (total / 2048)) * 1024
                    memTotal = total
                    memFree = free
                    memUsage = if (total > 0) (((total - free) * 100) / total).toInt().coerceIn(1, 99) else 40
                }

                val cpus = sysObj?.get("cpus")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(2) ?: 2
                val clockObj = sysObj?.get("clock")?.takeIf { it.isJsonObject }?.asJsonObject
                val clockTime = if (clockObj != null) {
                    val d = clockObj.get("date")?.asString ?: ""
                    val t = clockObj.get("time")?.asString ?: ""
                    val tz = clockObj.get("timezone")?.asString ?: ""
                    "$d $t $tz".trim()
                } else ""

                val model = verObj?.get("model")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: verObj?.get("device")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: "Keenetic"
                val osVersion = verObj?.get("title")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: verObj?.get("release")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: "KeeneticOS"
                val arch = verObj?.get("arch")?.takeIf { it.isJsonPrimitive }?.asString ?: "mips"
                val kernel = verObj?.get("kernel")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: verObj?.get("ndmversion")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: "4.9.x"
                val hwVersion = verObj?.get("hw_version")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: verObj?.get("hw_id")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: "rev.A"
                val manufacturer = verObj?.get("manufacturer")?.takeIf { it.isJsonPrimitive }?.asString ?: "Keenetic Limited"
                val title = "$model ($osVersion)"

                _systemInfo.value = SystemInfo(
                    hostname = hostname,
                    model = model,
                    title = title,
                    osVersion = osVersion,
                    uptime = uptime,
                    uptimeFormatted = uptimeFormatted,
                    cpus = cpus,
                    memoryTotal = memTotal,
                    memoryFree = memFree,
                    memoryBuffers = memBuffers,
                    memoryCached = memCached,
                    memoryUsagePercent = memUsage,
                    cpuUsagePercent = cpuLoad,
                    arch = arch,
                    kernel = kernel,
                    hwVersion = hwVersion,
                    manufacturer = manufacturer,
                    clockTime = clockTime,
                    domainName = domainName,
                    rawShowVersionJson = rawVersion,
                    rawShowSystemJson = rawSystem
                )

                _cpuHistory.value = (_cpuHistory.value + cpuLoad).takeLast(25)
                _ramHistory.value = (_ramHistory.value + memUsage).takeLast(25)
                _lastTelemetryTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                AppLogger.logError("loadSystemInfo", e)
            }
        }
    }

    fun startLivePolling() {
        _isLivePolling.value = true
        livePollingJob?.cancel()
        livePollingJob = viewModelScope.launch {
            while (isActive && _isLivePolling.value) {
                loadSystemInfo()
                delay((_pollingIntervalSeconds.value * 1000L).coerceAtLeast(1000L))
            }
        }
    }

    fun stopLivePolling() {
        _isLivePolling.value = false
        livePollingJob?.cancel()
        livePollingJob = null
    }

    fun toggleLivePolling() {
        if (_isLivePolling.value) {
            stopLivePolling()
        } else {
            startLivePolling()
        }
    }

    fun setPollingInterval(seconds: Int) {
        _pollingIntervalSeconds.value = seconds.coerceIn(1, 60)
        if (_isLivePolling.value) {
            startLivePolling()
        }
    }

    /**
     * Loads connection policies directly from Keenetic router RCI endpoint: GET /rci/show/ip/policy
     * or show ip policy CLI via SSH.
     */
    private suspend fun loadConnectionPoliciesInternal() {
        try {
            val res = repository.queryShow("ip/policy")
            val parsed = parseConnectionPolicies(res)
            if (parsed.isNotEmpty()) {
                _connectionPolicies.value = parsed
                AppLogger.logInfo("loadConnectionPolicies", "Loaded ${parsed.size} policies from router via RCI")
            }
        } catch (e: Exception) {
            AppLogger.logError("loadConnectionPoliciesInternal", e)
        }
    }

    fun loadConnectionPolicies() {
        viewModelScope.launch {
            loadConnectionPoliciesInternal()
        }
    }

    /**
     * Parses KeeneticOS RCI response from /rci/show/ip/policy or show.sc.ip.policy.
     */
    fun parseConnectionPolicies(res: JsonElement?): List<ConnectionPolicy> {
        val result = mutableListOf<ConnectionPolicy>()
        // Default policy is always available in KeeneticOS: conform to segment policy
        result.add(
            ConnectionPolicy(
                id = "",
                name = "Основная (по умолчанию)",
                description = "Следовать политике сегмента сети (conform)"
            )
        )

        if (res == null) return result

        try {
            if (res.isJsonObject) {
                val rootObj = res.asJsonObject
                val policyElem = when {
                    rootObj.has("policy") -> rootObj.get("policy")
                    rootObj.has("ip") && rootObj.getAsJsonObject("ip").has("policy") ->
                        rootObj.getAsJsonObject("ip").get("policy")
                    else -> rootObj
                }

                if (policyElem.isJsonObject) {
                    val pObj = policyElem.asJsonObject
                    for ((key, value) in pObj.entrySet()) {
                        if (key.equals("status", ignoreCase = true) ||
                            key.equals("prompt", ignoreCase = true) ||
                            key.equals("message", ignoreCase = true)) continue

                        if (value.isJsonObject) {
                            val inner = value.asJsonObject
                            val desc = inner.get("description")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                            val name = inner.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim() ?: key
                            val mark = inner.get("mark")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                            val table4 = inner.get("table4")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                            val displayName = if (!desc.isNullOrBlank()) desc else name
                            if (!key.equals("Main", ignoreCase = true) && !key.equals("default", ignoreCase = true)) {
                                result.add(
                                    ConnectionPolicy(
                                        id = key,
                                        name = displayName,
                                        description = desc ?: "",
                                        mark = mark,
                                        table4 = table4
                                    )
                                )
                            }
                        } else if (value.isJsonPrimitive) {
                            val nameStr = value.asString.trim()
                            if (!key.equals("Main", ignoreCase = true) && !key.equals("default", ignoreCase = true)) {
                                result.add(
                                    ConnectionPolicy(
                                        id = key,
                                        name = if (nameStr.isNotBlank()) nameStr else key,
                                        description = ""
                                    )
                                )
                            }
                        }
                    }
                } else if (policyElem.isJsonArray) {
                    policyElem.asJsonArray.forEach { item ->
                        if (item.isJsonObject) {
                            val itemObj = item.asJsonObject
                            val id = itemObj.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                                ?: itemObj.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                                ?: ""
                            val desc = itemObj.get("description")?.takeIf { it.isJsonPrimitive }?.asString?.trim() ?: ""
                            val mark = itemObj.get("mark")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                            val table4 = itemObj.get("table4")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                            val displayName = if (desc.isNotBlank()) desc else (if (id.isNotBlank()) id else "Политика")
                            if (id.isNotBlank() && !id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                                result.add(
                                    ConnectionPolicy(
                                        id = id,
                                        name = displayName,
                                        description = desc,
                                        mark = mark,
                                        table4 = table4
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (res.isJsonArray) {
                res.asJsonArray.forEach { item ->
                    if (item.isJsonObject) {
                        val itemObj = item.asJsonObject
                        val id = itemObj.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                            ?: itemObj.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                            ?: ""
                        val desc = itemObj.get("description")?.takeIf { it.isJsonPrimitive }?.asString?.trim() ?: ""
                        val mark = itemObj.get("mark")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                        val table4 = itemObj.get("table4")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                        val displayName = if (desc.isNotBlank()) desc else (if (id.isNotBlank()) id else "Политика")
                        if (id.isNotBlank() && !id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                            result.add(
                                ConnectionPolicy(
                                    id = id,
                                    name = displayName,
                                    description = desc,
                                    mark = mark,
                                    table4 = table4
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.logError("parseConnectionPolicies", e)
        }

        return result.distinctBy { it.id }
    }

    fun loadClients() {
        viewModelScope.launch {
            try {
                // Ensure connection policies from Keenetic router are loaded first
                loadConnectionPoliciesInternal()

                val res = repository.queryShow("ip/hotspot")
                val assocRes = repository.queryShow("associations")

                val assocMap = mutableMapOf<String, JsonObject>()
                if (assocRes != null) {
                    if (assocRes.isJsonArray) {
                        assocRes.asJsonArray.forEach {
                            if (it.isJsonObject) {
                                val mac = it.asJsonObject.get("mac")?.asString?.lowercase()
                                if (mac != null) assocMap[mac] = it.asJsonObject
                            }
                        }
                    } else if (assocRes.isJsonObject) {
                        for ((_, v) in assocRes.asJsonObject.entrySet()) {
                            if (v.isJsonArray) {
                                v.asJsonArray.forEach {
                                    if (it.isJsonObject) {
                                        val mac = it.asJsonObject.get("mac")?.asString?.lowercase()
                                        if (mac != null) assocMap[mac] = it.asJsonObject
                                    }
                                }
                            }
                        }
                    }
                }

                val list = mutableListOf<ConnectedClient>()
                if (res != null) {
                    val hosts = when {
                        res.isJsonObject && res.asJsonObject.has("host") && res.asJsonObject.get("host").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("host")
                        res.isJsonArray -> res.asJsonArray
                        else -> null
                    }

                    hosts?.forEach { el ->
                        if (el.isJsonObject) {
                            val obj = el.asJsonObject
                            val mac = obj.get("mac")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                            val ip = obj.get("ip")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                            val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                            val hostname = obj.get("hostname")?.takeIf { it.isJsonPrimitive }?.asString ?: (name ?: mac)
                            val displayName = if (!name.isNullOrBlank()) name else hostname
                            val iface = obj.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
                                ?: obj.get("via")?.takeIf { it.isJsonPrimitive }?.asString
                                ?: ""

                            val activeRaw = obj.get("active")
                            val linkRaw = obj.get("link")?.takeIf { it.isJsonPrimitive }?.asString
                            val isActive = when {
                                activeRaw != null && activeRaw.isJsonPrimitive -> {
                                    val str = activeRaw.asString
                                    str.equals("true", ignoreCase = true) || str.equals("yes", ignoreCase = true) || str == "1"
                                }
                                linkRaw != null -> linkRaw.equals("up", ignoreCase = true)
                                else -> ip.isNotBlank()
                            }

                            val access = obj.get("access")?.takeIf { it.isJsonPrimitive }?.asString
                            val isBlocked = access?.equals("deny", ignoreCase = true) == true

                            // Connection policy assigned to this host in KeeneticOS
                            val policyRaw = when {
                                obj.has("policy") && obj.get("policy").isJsonPrimitive -> obj.get("policy").asString.trim()
                                obj.has("policy") && obj.get("policy").isJsonObject -> {
                                    val p = obj.getAsJsonObject("policy")
                                    p.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                                        ?: p.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                                        ?: ""
                                }
                                else -> ""
                            }

                            val matchedPolicy = _connectionPolicies.value.find {
                                it.id.equals(policyRaw, ignoreCase = true) || it.name.equals(policyRaw, ignoreCase = true)
                            }
                            val policyDisplayName = matchedPolicy?.name ?: if (policyRaw.isNotBlank()) policyRaw else "Основная"
                            val policyId = matchedPolicy?.id ?: policyRaw

                            val staticRaw = obj.get("static") ?: obj.get("fixed")
                            val isStatic = staticRaw?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
                            val speedLimit = obj.get("speed-limit")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt / 1024 }?.getOrDefault(0) ?: 0

                            val rxBytes = obj.get("rxbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                            val txBytes = obj.get("txbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                            val rxSpeed = obj.get("rxspeed")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong / 1000 }?.getOrDefault(0L) ?: 0L
                            val txSpeed = obj.get("txspeed")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong / 1000 }?.getOrDefault(0L) ?: 0L

                            val assocObj = assocMap[mac.lowercase()]
                            val wifiRssi = assocObj?.get("rssi")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                            val wifiSsid = assocObj?.get("ssid")?.takeIf { it.isJsonPrimitive }?.asString
                                ?: if (iface.contains("WifiMaster", ignoreCase = true)) "Wi-Fi" else null
                            val wifiBand = if (iface.contains("WifiMaster1") || assocObj?.get("ap")?.asString?.contains("WifiMaster1") == true) "5 GHz"
                                else if (iface.contains("WifiMaster0") || assocObj?.get("ap")?.asString?.contains("WifiMaster0") == true) "2.4 GHz"
                                else null

                            if (mac.isNotBlank() || ip.isNotBlank()) {
                                list.add(
                                    ConnectedClient(
                                        mac = mac,
                                        ip = ip,
                                        hostname = hostname,
                                        displayName = displayName,
                                        interfaceName = iface,
                                        active = isActive,
                                        rxBytes = rxBytes,
                                        txBytes = txBytes,
                                        rxSpeedKbps = rxSpeed,
                                        txSpeedKbps = txSpeed,
                                        wifiSsid = wifiSsid,
                                        wifiRssi = wifiRssi,
                                        wifiBand = wifiBand,
                                        isBlocked = isBlocked,
                                        isStaticIp = isStatic,
                                        policy = policyDisplayName,
                                        policyId = policyId,
                                        speedLimitMbps = speedLimit
                                    )
                                )
                            }
                        }
                    }
                }

                _clients.value = list.sortedWith(compareByDescending<ConnectedClient> { it.active }.thenBy { it.displayName })
            } catch (e: Exception) {
                AppLogger.logError("loadClients", e)
            }
        }
    }

    fun loadInterfaces() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface")
                if (res != null) {
                    val ifaceList = InterfaceMapper.toInterfaceList(res)
                    val wifiList = InterfaceMapper.toWifiNetworks(res)

                    val updatedWifi = wifiList.map { wifi ->
                        val count = _clients.value.count { client ->
                            client.active && (client.wifiSsid == wifi.ssid || client.interfaceName.contains(wifi.id))
                        }
                        wifi.copy(clientsCount = count)
                    }

                    _interfaces.value = ifaceList
                    _wifiNetworks.value = updatedWifi
                }
            } catch (e: Exception) {
                AppLogger.logError("loadInterfaces", e)
            }
        }
    }

    fun loadDhcpBindings() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/dhcp/bindings") ?: repository.queryShow("ip/dhcp")
                if (res != null) {
                    val list = mutableListOf<DhcpBinding>()
                    val arr = when {
                        res.isJsonArray -> res.asJsonArray
                        res.isJsonObject && res.asJsonObject.has("binding") && res.asJsonObject.get("binding").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("binding")
                        else -> null
                    }
                    arr?.forEach {
                        if (it.isJsonObject) {
                            val o = it.asJsonObject
                            val mac = o.get("mac")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val ip = o.get("ip")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val hostname = o.get("name")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                ?: o.get("hostname")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val active = o.get("active")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true
                            if (mac.isNotBlank() && ip.isNotBlank()) {
                                list.add(DhcpBinding(mac, ip, hostname, active))
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        _dhcpBindings.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDhcpBindings", e)
            }
        }
    }

    fun loadPortForwardingRules() {
        if (_isDemoMode.value && _portForwardingRules.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/static") ?: repository.queryShow("ip/nat")
                if (res != null) {
                    val list = mutableListOf<PortForwardingRule>()
                    fun parseNat(o: com.google.gson.JsonObject, defaultIdx: Int) {
                        val name = o.get("name")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("comment")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: "Правило #${defaultIdx + 1}"
                        val proto = o.get("proto")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("protocol")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "TCP"
                        val port = o.get("port")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("src-port")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val toAddress = o.get("to-address")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("dst")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("ip")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val toPort = o.get("to-port")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("dst-port")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: port
                        val iface = o.get("interface")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "ISP"
                        val enabled = o.get("enable")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true)
                            ?: o.get("active")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true

                        if (port.isNotBlank() || toAddress.isNotBlank()) {
                            list.add(
                                PortForwardingRule(
                                    id = (list.size + 1).toString(),
                                    name = name,
                                    proto = proto.uppercase(),
                                    srcPort = if (port.isNotBlank()) port else toPort,
                                    dstIp = if (toAddress.isNotBlank()) toAddress else "192.168.1.2",
                                    dstPort = toPort,
                                    interfaceName = iface,
                                    enabled = enabled
                                )
                            )
                        }
                    }

                    if (res.isJsonArray) {
                        res.asJsonArray.forEachIndexed { idx, it -> if (it.isJsonObject) parseNat(it.asJsonObject, idx) }
                    } else if (res.isJsonObject) {
                        val root = res.asJsonObject
                        if (root.has("rule") && root.get("rule").isJsonArray) {
                            root.getAsJsonArray("rule").forEachIndexed { idx, it -> if (it.isJsonObject) parseNat(it.asJsonObject, idx) }
                        } else if (root.has("static") && root.get("static").isJsonArray) {
                            root.getAsJsonArray("static").forEachIndexed { idx, it -> if (it.isJsonObject) parseNat(it.asJsonObject, idx) }
                        } else {
                            var counter = 0
                            root.entrySet().forEach { (_, v) ->
                                if (v.isJsonObject) {
                                    parseNat(v.asJsonObject, counter++)
                                } else if (v.isJsonArray) {
                                    v.asJsonArray.forEach { el -> if (el.isJsonObject) parseNat(el.asJsonObject, counter++) }
                                }
                            }
                        }
                    }
                    if (list.isNotEmpty() || !_isDemoMode.value) {
                        _portForwardingRules.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadPortForwardingRules", e)
            }
        }
    }

    fun loadFirewallRules() {
        if (_isDemoMode.value && _firewallRules.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/access-list") ?: repository.queryShow("ip/firewall")
                if (res != null) {
                    val list = mutableListOf<FirewallRule>()
                    fun parseRule(o: com.google.gson.JsonObject, defaultIface: String = "ISP") {
                        val action = o.get("action")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "permit"
                        val proto = o.get("proto")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("protocol")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "IP"
                        val src = o.get("src")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("source")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "any"
                        val dst = o.get("dst")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("destination")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "any"
                        val dstPort = o.get("dst-port")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("port")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "any"
                        val iface = o.get("interface")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: defaultIface
                        val comment = o.get("comment")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val enabled = o.get("enable")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true

                        list.add(
                            FirewallRule(
                                id = (list.size + 1).toString(),
                                action = action,
                                proto = proto.uppercase(),
                                srcIp = src,
                                dstIp = dst,
                                dstPort = dstPort,
                                interfaceName = iface,
                                enabled = enabled,
                                comment = comment
                            )
                        )
                    }

                    if (res.isJsonArray) {
                        res.asJsonArray.forEach { if (it.isJsonObject) parseRule(it.asJsonObject) }
                    } else if (res.isJsonObject) {
                        val root = res.asJsonObject
                        if (root.has("rule") && root.get("rule").isJsonArray) {
                            root.getAsJsonArray("rule").forEach { if (it.isJsonObject) parseRule(it.asJsonObject) }
                        } else if (root.has("access-list") && root.get("access-list").isJsonArray) {
                            root.getAsJsonArray("access-list").forEach { if (it.isJsonObject) parseRule(it.asJsonObject) }
                        } else {
                            root.entrySet().forEach { (aclName, el) ->
                                if (el.isJsonArray) {
                                    el.asJsonArray.forEach { if (it.isJsonObject) parseRule(it.asJsonObject, aclName) }
                                } else if (el.isJsonObject && el.asJsonObject.has("rule") && el.asJsonObject.get("rule").isJsonArray) {
                                    el.asJsonObject.getAsJsonArray("rule").forEach { if (it.isJsonObject) parseRule(it.asJsonObject, aclName) }
                                }
                            }
                        }
                    }
                    if (list.isNotEmpty() || !_isDemoMode.value) {
                        _firewallRules.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadFirewallRules", e)
            }
        }
    }

    fun loadLanSegments() {
        if (_isDemoMode.value && _lanSegments.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val ifaceRes = repository.queryShow("interface")
                val poolRes = repository.queryShow("ip/dhcp/pool")

                val list = mutableListOf<LanSegment>()
                if (ifaceRes != null && ifaceRes.isJsonObject) {
                    val root = ifaceRes.asJsonObject
                    for ((id, el) in root.entrySet()) {
                        if (el.isJsonObject) {
                            val o = el.asJsonObject
                            val type = o.get("type")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val desc = o.get("description")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                ?: o.get("name")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: id
                            val ip = o.get("address")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                ?: o.getAsJsonObject("ip")?.get("address")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val mask = o.get("mask")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                ?: o.getAsJsonObject("ip")?.get("mask")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "255.255.255.0"
                            val isolate = o.get("isolate")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false)
                                ?: desc.contains("Гост", ignoreCase = true) || id.contains("Guest", ignoreCase = true)

                            // LAN bridge or Home network segments
                            if (type.equals("Bridge", ignoreCase = true) || id.startsWith("Bridge", ignoreCase = true) || id.startsWith("Home", ignoreCase = true) || ip.isNotBlank()) {
                                val effectiveIp = if (ip.isNotBlank()) ip else "192.168.1.1"
                                val ipPrefix = effectiveIp.substringBeforeLast(".")
                                list.add(
                                    LanSegment(
                                        id = id,
                                        name = desc,
                                        ip = effectiveIp,
                                        mask = mask,
                                        dhcpEnabled = true,
                                        dhcpStart = "$ipPrefix.33",
                                        dhcpEnd = "$ipPrefix.199",
                                        isolateClients = isolate
                                    )
                                )
                            }
                        }
                    }
                }
                if (list.isNotEmpty() || !_isDemoMode.value) {
                    _lanSegments.value = list
                }
            } catch (e: Exception) {
                AppLogger.logError("loadLanSegments", e)
            }
        }
    }

    fun loadStaticRoutes() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/route")
                if (res != null) {
                    val list = mutableListOf<StaticRoute>()
                    val arr = when {
                        res.isJsonArray -> res.asJsonArray
                        res.isJsonObject && res.asJsonObject.has("route") && res.asJsonObject.get("route").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("route")
                        res.isJsonObject -> {
                            val jsonArr = com.google.gson.JsonArray()
                            res.asJsonObject.entrySet().forEach { (_, v) ->
                                if (v.isJsonObject) jsonArr.add(v)
                                else if (v.isJsonArray) v.asJsonArray.forEach { jsonArr.add(it) }
                            }
                            jsonArr
                        }
                        else -> null
                    }
                    arr?.forEach {
                        if (it.isJsonObject) {
                            val o = it.asJsonObject
                            val dest = o.get("destination")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                ?: o.get("network")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val mask = o.get("mask")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "255.255.255.0"
                            val gw = o.get("gateway")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val iface = o.get("interface")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                            val auto = o.get("auto")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
                            val comment = o.get("comment")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""

                            if (dest.isNotBlank()) {
                                list.add(StaticRoute(id = "${dest}_$iface", network = dest, mask = mask, gateway = gw, interfaceName = iface, auto = auto, comment = comment))
                            }
                        }
                    }
                    if (list.isNotEmpty() || !_isDemoMode.value) {
                        _staticRoutes.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadStaticRoutes", e)
            }
        }
    }

    fun loadUsers() {
        if (_isDemoMode.value && _userAccounts.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val res = repository.queryShow("user")
                if (res != null) {
                    val list = mutableListOf<RouterUserAccount>()
                    if (res.isJsonArray) {
                        res.asJsonArray.forEach { el ->
                            if (el.isJsonObject) {
                                val o = el.asJsonObject
                                val name = o.get("name")?.asString ?: ""
                                val tags = mutableListOf<String>()
                                o.get("tag")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
                                    tags.add(it.asString)
                                }
                                if (name.isNotBlank()) {
                                    val isSuper = tags.contains("admin")
                                    list.add(RouterUserAccount(name, tags, if (isSuper) listOf("Полный доступ") else listOf("Ограниченный доступ")))
                                }
                            }
                        }
                    } else if (res.isJsonObject) {
                        for ((name, v) in res.asJsonObject.entrySet()) {
                            val tags = mutableListOf<String>()
                            if (v.isJsonObject && v.asJsonObject.has("tag")) {
                                v.asJsonObject.getAsJsonArray("tag").forEach { tags.add(it.asString) }
                            }
                            val isSuper = tags.contains("admin")
                            list.add(RouterUserAccount(name, tags, if (isSuper) listOf("Полный доступ") else listOf("Ограниченный доступ")))
                        }
                    }
                    if (list.isNotEmpty()) {
                        _userAccounts.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadUsers", e)
            }
        }
    }

    fun loadUsbDevices() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("usb") ?: repository.queryShow("media")
                if (res != null) {
                    val list = mutableListOf<UsbStorageDevice>()
                    fun parseUsb(o: com.google.gson.JsonObject) {
                        val name = o.get("name")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "USB Drive"
                        val label = o.get("label")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: name
                        val vendor = o.get("vendor")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "Generic"
                        val model = o.get("model")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val size = o.get("size")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                        val free = o.get("free")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: (size / 2)
                        val fs = o.get("filesystem")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "NTFS"
                        val mount = o.get("mount")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "/tmp/mnt/$label"

                        list.add(
                            UsbStorageDevice(
                                name = name,
                                label = label,
                                vendor = vendor,
                                model = model,
                                sizeBytes = size,
                                freeBytes = free,
                                filesystem = fs,
                                mountPoint = mount,
                                shareSmb = true
                            )
                        )
                    }

                    if (res.isJsonArray) {
                        res.asJsonArray.forEach { if (it.isJsonObject) parseUsb(it.asJsonObject) }
                    } else if (res.isJsonObject) {
                        val obj = res.asJsonObject
                        if (obj.has("device") && obj.get("device").isJsonArray) {
                            obj.getAsJsonArray("device").forEach { if (it.isJsonObject) parseUsb(it.asJsonObject) }
                        } else if (obj.has("media") && obj.get("media").isJsonArray) {
                            obj.getAsJsonArray("media").forEach { if (it.isJsonObject) parseUsb(it.asJsonObject) }
                        } else {
                            obj.entrySet().forEach { (_, v) ->
                                if (v.isJsonObject) parseUsb(v.asJsonObject)
                                else if (v.isJsonArray) v.asJsonArray.forEach { if (it.isJsonObject) parseUsb(it.asJsonObject) }
                            }
                        }
                    }
                    if (list.isNotEmpty() || !_isDemoMode.value) {
                        _usbStorageList.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadUsbDevices", e)
            }
        }
    }

    fun loadFirmwareStatus() {
        viewModelScope.launch {
            try {
                val verRes = repository.queryShow("version")
                val updateRes = repository.queryShow("system/update/status") ?: repository.queryShow("components/list")

                val verObj = verRes?.takeIf { it.isJsonObject }?.asJsonObject
                val updObj = updateRes?.takeIf { it.isJsonObject }?.asJsonObject

                val title = verObj?.get("title")?.takeIf { it.isJsonPrimitive }?.asString ?: "KeeneticOS 5.1"
                val model = verObj?.get("model")?.takeIf { it.isJsonPrimitive }?.asString ?: "Keenetic"
                val channel = updObj?.get("channel")?.takeIf { it.isJsonPrimitive }?.asString ?: "Release"
                val updateAvailable = updObj?.get("update-available")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
                val availableVer = updObj?.get("available-version")?.takeIf { it.isJsonPrimitive }?.asString ?: title

                _firmwareStatus.value = FirmwareStatus(
                    title = "KeeneticOS $title",
                    model = model,
                    channel = channel,
                    updateAvailable = updateAvailable,
                    availableVersion = availableVer,
                    changelog = "• Актуальная версия прошивки KeeneticOS для $model\n• Все системы функционируют в штатном режиме",
                    autoUpdate = true
                )
            } catch (e: Exception) {
                AppLogger.logError("loadFirmwareStatus", e)
            }
        }
    }

    fun loadSystemLogs() {
        if (_isDemoMode.value && _systemLogs.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val res = repository.queryShow("log")
                if (res != null) {
                    val list = mutableListOf<SystemLogEntry>()
                    val arr = when {
                        res.isJsonArray -> res.asJsonArray
                        res.isJsonObject && res.asJsonObject.has("log") && res.asJsonObject.get("log").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("log")
                        res.isJsonObject && res.asJsonObject.has("entry") && res.asJsonObject.get("entry").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("entry")
                        else -> null
                    }

                    if (arr != null) {
                        arr.forEach { el ->
                            if (el.isJsonObject) {
                                val o = el.asJsonObject
                                val time = o.get("time")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                    ?: o.get("timestamp")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                                val facility = o.get("facility")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                    ?: o.get("ident")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "ndm"
                                val level = o.get("level")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                    ?: o.get("priority")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "info"
                                val msg = o.get("message")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                    ?: o.get("msg")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""

                                if (msg.isNotBlank()) {
                                    list.add(SystemLogEntry(time, facility, level, msg))
                                }
                            } else if (el.isJsonPrimitive) {
                                val line = el.asString
                                list.add(SystemLogEntry("", "ndm", "info", line))
                            }
                        }
                    } else if (res.isJsonObject && res.asJsonObject.has("log") && res.asJsonObject.get("log").isJsonPrimitive) {
                        val text = res.asJsonObject.get("log").asString
                        text.lines().filter { it.isNotBlank() }.forEach { line ->
                            list.add(SystemLogEntry("", "ndm", "info", line))
                        }
                    }

                    if (list.isNotEmpty()) {
                        _systemLogs.value = list
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSystemLogs", e)
            }
        }
    }

    fun loadMobileStatus() {
        if (_isDemoMode.value && _mobileModemStatus.value.connected) return
        viewModelScope.launch {
            try {
                val mobileRes = repository.queryShow("mobile") ?: repository.queryShow("sim") ?: repository.queryShow("interface")
                var modemConnected = false
                var operator = ""
                var networkType = "4G/LTE"
                var signal = 0
                var ip = ""
                var ifaceName = "UsbModem0"
                var desc = ""

                if (mobileRes != null && mobileRes.isJsonObject) {
                    val root = mobileRes.asJsonObject
                    // Check if root has UsbModem or Mobile section
                    val modemObj = when {
                        root.has("modem") && root.get("modem").isJsonObject -> root.getAsJsonObject("modem")
                        root.has("mobile") && root.get("mobile").isJsonObject -> root.getAsJsonObject("mobile")
                        root.has("UsbModem0") && root.get("UsbModem0").isJsonObject -> root.getAsJsonObject("UsbModem0")
                        else -> null
                    }

                    if (modemObj != null) {
                        modemConnected = modemObj.get("connected")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true)
                            ?: modemObj.get("up")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true
                        operator = modemObj.get("operator")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: modemObj.get("spn")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        networkType = modemObj.get("type")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: modemObj.get("act")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "LTE (4G)"
                        signal = modemObj.get("signal")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(75)
                            ?: modemObj.get("rssi")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(75) ?: 75
                        ip = modemObj.get("address")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: modemObj.get("ip")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        desc = modemObj.get("description")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: modemObj.get("model")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "USB LTE Модем"
                    } else {
                        // Check if any interface is a modem (UsbModem, CdcEther, Qmi, etc.)
                        for ((name, el) in root.entrySet()) {
                            if (name.contains("modem", ignoreCase = true) || name.contains("cdc", ignoreCase = true) || name.contains("qmi", ignoreCase = true)) {
                                if (el.isJsonObject) {
                                    val o = el.asJsonObject
                                    modemConnected = o.get("up")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
                                    ip = o.get("address")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                                    desc = o.get("description")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: name
                                    ifaceName = name
                                    break
                                }
                            }
                        }
                    }
                }

                _mobileModemStatus.value = MobileModemStatus(
                    connected = modemConnected,
                    operator = operator,
                    networkType = networkType,
                    signalStrengthPercent = signal,
                    ip = ip,
                    interfaceName = ifaceName,
                    description = desc
                )
            } catch (e: Exception) {
                AppLogger.logError("loadMobileStatus", e)
            }
        }
    }

    fun createUserAccount(username: String, pass: String, isSuperuser: Boolean, allowSmb: Boolean, allowVpn: Boolean) {
        val tags = mutableListOf<String>()
        if (isSuperuser) tags.add("admin")
        if (allowSmb) tags.add("smb")
        if (allowVpn) tags.add("vpn")
        val newAcc = RouterUserAccount(username, tags, if (isSuperuser) listOf("Полный доступ") else listOf("Хранилище/VPN"))
        _userAccounts.value = _userAccounts.value.filter { it.name != username } + newAcc
        viewModelScope.launch {
            try {
                val userObj = mutableMapOf<String, Any>("name" to username)
                if (pass.isNotBlank()) userObj["password"] = pass
                if (tags.isNotEmpty()) userObj["tag"] = tags
                val cmd = mapOf("user" to userObj)
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("createUserAccount", e)
            }
        }
    }

    fun deleteUserAccount(username: String) {
        _userAccounts.value = _userAccounts.value.filter { it.name != username }
        viewModelScope.launch {
            try {
                val cmd = mapOf("no" to mapOf("user" to username))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("deleteUserAccount", e)
            }
        }
    }

    fun ejectUsbDevice(name: String) {
        _usbStorageList.value = _usbStorageList.value.filter { it.name != name }
    }

    fun setFirmwareChannel(channel: String) {
        _firmwareStatus.value = _firmwareStatus.value?.copy(channel = channel)
    }

    fun startFirmwareUpdate() {
        _firmwareStatus.value = _firmwareStatus.value?.copy(updateAvailable = false)
    }

    fun runDiagnostics(tool: String, target: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val host = _savedIp.value
                val port = _sshPort.value.toIntOrNull() ?: 22
                val username = _savedUsername.value
                val password = encryptedStorage.getPassword() ?: ""

                if (password.isBlank()) {
                    _diagnosticsResult.value = DiagnosticsResult(
                        tool, target,
                        "Ошибка: пароль не сохранён. Войдите в приложение для сохранения учётных данных.",
                        success = false, executionTimeMs = 0
                    )
                    _isLoading.value = false
                    return@launch
                }

                val startTime = System.currentTimeMillis()
                val result = when (tool.lowercase()) {
                    "ping" -> sshService.pingViaSsh(host, port, username, password, target)
                    "traceroute" -> sshService.tracerouteViaSsh(host, port, username, password, target)
                    "dns" -> sshService.dnsLookupViaSsh(host, port, username, password, target)
                    else -> sshService.executeCommand(host, port, username, password, target)
                }
                val elapsed = System.currentTimeMillis() - startTime

                val output = if (result.output.isNotBlank()) result.output else {
                    if (result.error.isNotBlank()) "Ошибка: ${result.error}" else "Нет вывода"
                }

                _diagnosticsResult.value = DiagnosticsResult(
                    tool = tool,
                    target = target,
                    output = output,
                    success = result.success,
                    executionTimeMs = elapsed
                )
            } catch (e: Exception) {
                AppLogger.logError("runDiagnostics", e)
                _diagnosticsResult.value = DiagnosticsResult(
                    tool, target,
                    "Исключение: ${e.message ?: e.javaClass.simpleName}",
                    success = false, executionTimeMs = 0
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rebootRouter(
        method: RebootMethod = _selectedRebootMethod.value,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (method == RebootMethod.SSH) {
            val portInt = _sshPort.value.toIntOrNull() ?: 22
            rebootRouterViaSsh(port = portInt, onComplete = onComplete)
            return
        }

        viewModelScope.launch {
            _isRebooting.value = true
            try {
                val cmd = listOf(mapOf("system" to mapOf("reboot" to emptyMap<String, Any>())))
                val res = repository.getRestApi().executeRci(cmd)
                if (res.isSuccessful) {
                    val msg = "Команда перезагрузки успешно принята интернет-центром Keenetic через RCI REST API"
                    _rebootMessage.value = msg
                    onComplete(true, msg)
                } else {
                    val msg = "Ошибка перезагрузки через RCI: код HTTP ${res.code()}"
                    _rebootMessage.value = msg
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                AppLogger.logError("rebootRouter", e)
                val msg = "Ошибка: ${e.message ?: "Не удалось отправить команду перезагрузки"}"
                _rebootMessage.value = msg
                onComplete(false, msg)
            } finally {
                _isRebooting.value = false
            }
        }
    }

    /**
     * Executes reboot via SSH using the JSch library as a secondary management method.
     */
    fun rebootRouterViaSsh(
        host: String = _savedIp.value,
        port: Int = _sshPort.value.toIntOrNull() ?: 22,
        username: String = _savedUsername.value,
        password: String = encryptedStorage.getPassword() ?: "",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isRebooting.value = true
            try {
                if (password.isBlank()) {
                    val msg = "Ошибка: пароль пользователя $username не сохранен в защищенном хранилище"
                    _rebootMessage.value = msg
                    onComplete(false, msg)
                    return@launch
                }

                AppLogger.logInfo("RouterViewModel", "Отправка перезагрузки роутера через JSch SSH на $username@$host:$port")
                val result = sshService.rebootRouter(
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )

                if (result.success) {
                    val msg = result.output.ifBlank {
                        "Команда 'system reboot' успешно передана интернет-центру Keenetic через JSch SSH (порт $port)"
                    }
                    _rebootMessage.value = msg
                    onComplete(true, msg)
                } else {
                    val msg = result.error ?: "Не удалось выполнить SSH перезагрузку"
                    _rebootMessage.value = msg
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                AppLogger.logError("rebootRouterViaSsh", e)
                val msg = "Исключение при SSH перезагрузке: ${e.message}"
                _rebootMessage.value = msg
                onComplete(false, msg)
            } finally {
                _isRebooting.value = false
            }
        }
    }

    fun clearRebootMessage() {
        _rebootMessage.value = null
    }

    fun toggleClientBlock(client: ConnectedClient) {
        val newBlocked = !client.isBlocked
        _clients.value = _clients.value.map {
            if (it.mac == client.mac) it.copy(isBlocked = newBlocked) else it
        }
        viewModelScope.launch {
            try {
                val accessVal = if (newBlocked) "deny" else "permit"
                val cmd = mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to client.mac, "access" to accessVal))))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("toggleClientBlock", e)
            }
        }
    }

    fun updateClientFullSettings(
        mac: String,
        newName: String,
        ip: String,
        isStatic: Boolean,
        policy: String,
        policyId: String = "",
        wifiBandPreference: String,
        speedLimitMbps: Int
    ) {
        val resolvedPolicy = _connectionPolicies.value.find {
            (policyId.isNotBlank() && it.id.equals(policyId, ignoreCase = true)) ||
            (policy.isNotBlank() && (it.name.equals(policy, ignoreCase = true) || it.id.equals(policy, ignoreCase = true)))
        }
        val targetPolicyId = resolvedPolicy?.id ?: policyId
        val targetPolicyName = resolvedPolicy?.name ?: if (policy.isNotBlank()) policy else "Основная"

        _clients.value = _clients.value.map {
            if (it.mac.equals(mac, ignoreCase = true)) {
                it.copy(
                    displayName = if (newName.isNotBlank()) newName else it.displayName,
                    ip = if (ip.isNotBlank()) ip else it.ip,
                    isStaticIp = isStatic,
                    policy = targetPolicyName,
                    policyId = targetPolicyId,
                    wifiBandPreference = wifiBandPreference,
                    speedLimitMbps = speedLimitMbps
                )
            } else it
        }
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()
                if (newName.isNotBlank()) {
                    cmds.add(mapOf("known" to mapOf("host" to mapOf("name" to newName, "mac" to mac))))
                }
                if (isStatic && ip.isNotBlank()) {
                    cmds.add(mapOf("known" to mapOf("host" to mapOf("mac" to mac, "ip" to ip))))
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "ip" to ip, "permit" to true)))))
                } else if (!isStatic) {
                    cmds.add(mapOf("no" to mapOf("known" to mapOf("host" to mapOf("mac" to mac, "ip" to true)))))
                }
                if (targetPolicyId.isNotBlank() && !targetPolicyId.equals("Main", ignoreCase = true) && !targetPolicyId.equals("default", ignoreCase = true)) {
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "policy" to targetPolicyId)))))
                } else {
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "conform" to true)))))
                    cmds.add(mapOf("no" to mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "policy" to true))))))
                }
                if (speedLimitMbps > 0) {
                    val rate = speedLimitMbps * 1024
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "speed-limit" to rate)))))
                } else {
                    cmds.add(mapOf("no" to mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "speed-limit" to true))))))
                }
                if (cmds.isNotEmpty()) {
                    repository.executeRciWithSave(cmds)
                }
            } catch (e: Exception) {
                AppLogger.logError("updateClientFullSettings", e)
            }
        }
    }

    fun bindStaticIp(mac: String, ip: String, isStatic: Boolean) {
        _clients.value = _clients.value.map {
            if (it.mac.equals(mac, ignoreCase = true)) it.copy(isStaticIp = isStatic, ip = ip) else it
        }
        viewModelScope.launch {
            try {
                if (isStatic) {
                    val cmd1 = mapOf("known" to mapOf("host" to mapOf("mac" to mac, "ip" to ip)))
                    val cmd2 = mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "ip" to ip, "permit" to true))))
                    repository.executeRciWithSave(listOf(cmd1, cmd2))
                } else {
                    val cmd = mapOf("no" to mapOf("known" to mapOf("host" to mapOf("mac" to mac, "ip" to true))))
                    repository.executeRciWithSave(listOf(cmd))
                }
            } catch (e: Exception) {
                AppLogger.logError("bindStaticIp", e)
            }
        }
    }

    fun setClientWifiBandPreference(mac: String, band: String) {
        _clients.value = _clients.value.map {
            if (it.mac.equals(mac, ignoreCase = true)) it.copy(wifiBandPreference = band) else it
        }
    }

    fun setClientSpeedLimit(mac: String, limitMbps: Int) {
        _clients.value = _clients.value.map {
            if (it.mac.equals(mac, ignoreCase = true)) it.copy(speedLimitMbps = limitMbps) else it
        }
        viewModelScope.launch {
            try {
                val cmd = if (limitMbps > 0) {
                    mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "speed-limit" to (limitMbps * 1024)))))
                } else {
                    mapOf("no" to mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "speed-limit" to true)))))
                }
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setClientSpeedLimit", e)
            }
        }
    }

    fun deleteKnownDevice(mac: String) {
        _clients.value = _clients.value.filter { !it.mac.equals(mac, ignoreCase = true) }
        viewModelScope.launch {
            try {
                val cmd = mapOf("no" to mapOf("known" to mapOf("host" to mapOf("mac" to mac))))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("deleteKnownDevice", e)
            }
        }
    }

    fun updateLanSegment(id: String, ip: String, mask: String, dhcpStart: String, dhcpEnd: String, isolate: Boolean) {
        _lanSegments.value = _lanSegments.value.map {
            if (it.id == id || it.name == id) {
                it.copy(ip = ip, mask = mask, dhcpStart = dhcpStart, dhcpEnd = dhcpEnd, isolateClients = isolate)
            } else it
        }
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()
                cmds.add(mapOf("interface" to mapOf("ip" to mapOf("address" to listOf(mapOf("address" to ip, "mask" to mask))), "name" to id)))
                cmds.add(mapOf("ip" to mapOf("dhcp" to mapOf("pool" to mapOf("name" to id, "range" to mapOf("start" to dhcpStart, "end" to dhcpEnd))))))
                if (isolate) {
                    cmds.add(mapOf("interface" to mapOf("isolate" to true, "name" to id)))
                } else {
                    cmds.add(mapOf("no" to mapOf("interface" to mapOf("isolate" to true, "name" to id))))
                }
                repository.executeRciWithSave(cmds)
            } catch (e: Exception) {
                AppLogger.logError("updateLanSegment", e)
            }
        }
    }

    fun reconnectInterface(interfaceId: String) {
        viewModelScope.launch {
            try {
                val downCmd = mapOf("interface" to mapOf("down" to true, "name" to interfaceId))
                val upCmd = mapOf("interface" to mapOf("up" to true, "name" to interfaceId))
                repository.executeRciWithSave(listOf(downCmd, upCmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("reconnectInterface", e)
            }
        }
    }

    fun toggleUsbService(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = when (serviceName.lowercase()) {
                    "smb", "cifs" -> if (enabled) mapOf("cifs" to mapOf("enable" to true)) else mapOf("no" to mapOf("cifs" to true))
                    "dlna", "media" -> if (enabled) mapOf("media" to mapOf("enable" to true)) else mapOf("no" to mapOf("media" to true))
                    "ftp" -> if (enabled) mapOf("ftp" to mapOf("enable" to true)) else mapOf("no" to mapOf("ftp" to true))
                    else -> emptyMap()
                }
                if (cmd.isNotEmpty()) {
                    repository.executeRciWithSave(listOf(cmd))
                }
            } catch (e: Exception) {
                AppLogger.logError("toggleUsbService", e)
            }
        }
    }

    fun setModemMode(mode: String) {
        _mobileModemStatus.value = _mobileModemStatus.value.copy(networkType = mode)
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf("usb" to mapOf("modem" to mapOf("mode" to mode.lowercase())), "name" to "UsbModem0"))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setModemMode", e)
            }
        }
    }

    fun renameDevice(mac: String, newName: String) {
        _clients.value = _clients.value.map {
            if (it.mac == mac) it.copy(displayName = newName) else it
        }
        viewModelScope.launch {
            try {
                val cmd = mapOf("known" to mapOf("host" to mapOf("name" to newName, "mac" to mac)))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("renameDevice", e)
            }
        }
    }

    fun setClientPolicy(mac: String, policyNameOrId: String) {
        val resolved = _connectionPolicies.value.find {
            it.id.equals(policyNameOrId, ignoreCase = true) || it.name.equals(policyNameOrId, ignoreCase = true)
        }
        val targetId = resolved?.id ?: policyNameOrId
        val targetName = resolved?.name ?: policyNameOrId

        _clients.value = _clients.value.map {
            if (it.mac.equals(mac, ignoreCase = true)) it.copy(policy = targetName, policyId = targetId) else it
        }
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()
                if (targetId.isNotBlank() && !targetId.equals("Main", ignoreCase = true) && !targetId.equals("default", ignoreCase = true)) {
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "policy" to targetId)))))
                } else {
                    cmds.add(mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "conform" to true)))))
                    cmds.add(mapOf("no" to mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "policy" to true))))))
                }
                repository.executeRciWithSave(cmds)
            } catch (e: Exception) {
                AppLogger.logError("setClientPolicy", e)
            }
        }
    }

    fun setClientSchedule(mac: String, scheduleName: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "schedule" to scheduleName))))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setClientSchedule", e)
            }
        }
    }

    fun clearWifiMessage() {
        _wifiActionMessage.value = null
    }

    fun loadWifiData() {
        viewModelScope.launch {
            _isWifiLoading.value = true
            try {
                val wifiShow = repository.queryShow("wifi")
                val assocShow = repository.queryShow("associations")
                val hotspotShow = repository.queryShow("ip/hotspot")
                val ifaceShow = repository.queryShow("interface")

                val hotspotMap = mutableMapOf<String, JsonObject>()
                val hosts = when {
                    hotspotShow?.isJsonObject == true && hotspotShow.asJsonObject.has("host") && hotspotShow.asJsonObject.get("host").isJsonArray ->
                        hotspotShow.asJsonObject.getAsJsonArray("host")
                    hotspotShow?.isJsonArray == true -> hotspotShow.asJsonArray
                    else -> null
                }
                hosts?.forEach { el ->
                    if (el.isJsonObject) {
                        val mac = el.asJsonObject.get("mac")?.takeIf { it.isJsonPrimitive }?.asString?.lowercase()
                        if (mac != null) hotspotMap[mac] = el.asJsonObject
                    }
                }

                val rawAssocList = mutableListOf<JsonObject>()
                fun collectAssocs(element: JsonElement?) {
                    if (element == null) return
                    when {
                        element.isJsonArray -> element.asJsonArray.forEach { if (it.isJsonObject) rawAssocList.add(it.asJsonObject) }
                        element.isJsonObject -> {
                            val obj = element.asJsonObject
                            for ((k, v) in obj.entrySet()) {
                                if (v.isJsonArray) {
                                    v.asJsonArray.forEach { if (it.isJsonObject) rawAssocList.add(it.asJsonObject) }
                                } else if (v.isJsonObject && (v.asJsonObject.has("mac") || v.asJsonObject.has("rssi"))) {
                                    rawAssocList.add(v.asJsonObject)
                                } else if (k == "associations" || k == "station" || k == "stations" || k == "client" || k == "clients") {
                                    collectAssocs(v)
                                }
                            }
                        }
                    }
                }

                collectAssocs(assocShow)
                collectAssocs(wifiShow)

                val parsedClients = mutableListOf<WirelessClient>()
                val seenMacs = mutableSetOf<String>()

                rawAssocList.forEach { assocObj ->
                    val mac = assocObj.get("mac")?.takeIf { it.isJsonPrimitive }?.asString?.lowercase() ?: return@forEach
                    if (seenMacs.add(mac)) {
                        val hs = hotspotMap[mac]
                        val name = hs?.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                        val hostname = hs?.get("hostname")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: assocObj.get("hostname")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: (name ?: mac)
                        val displayName = if (!name.isNullOrBlank()) name else hostname
                        val ip = hs?.get("ip")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: assocObj.get("ip")?.takeIf { it.isJsonPrimitive }?.asString

                        val ap = assocObj.get("ap")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: hs?.get("interface")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: ""
                        val ssid = assocObj.get("ssid")?.takeIf { it.isJsonPrimitive }?.asString ?: "Keenetic-Wi-Fi"
                        val rssi = assocObj.get("rssi")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                        val txRate = assocObj.get("txrate")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                        val rxRate = assocObj.get("rxrate")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                        val txBytes = assocObj.get("txbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L)
                            ?: hs?.get("txbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                        val rxBytes = assocObj.get("rxbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L)
                            ?: hs?.get("rxbytes")?.takeIf { it.isJsonPrimitive }?.runCatching { asLong }?.getOrDefault(0L) ?: 0L
                        val mode = assocObj.get("mode")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: assocObj.get("standard")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: if (ap.contains("WifiMaster0")) "802.11ax" else "802.11n"

                        val band = when {
                            assocObj.get("band")?.takeIf { it.isJsonPrimitive }?.asString?.contains("5") == true -> "5 GHz"
                            assocObj.get("band")?.takeIf { it.isJsonPrimitive }?.asString?.contains("2.4") == true -> "2.4 GHz"
                            ap.contains("WifiMaster0") || ap.contains("5G", ignoreCase = true) -> "5 GHz"
                            ap.contains("WifiMaster1") || ap.contains("2.4G", ignoreCase = true) -> "2.4 GHz"
                            else -> "2.4 GHz"
                        }

                        val access = hs?.get("access")?.takeIf { it.isJsonPrimitive }?.asString
                        val isBlocked = access?.equals("deny", ignoreCase = true) == true

                        parsedClients.add(
                            WirelessClient(
                                mac = mac,
                                ip = ip,
                                hostname = hostname,
                                displayName = displayName,
                                band = band,
                                rssi = rssi,
                                txRateKbps = txRate,
                                rxRateKbps = rxRate,
                                txBytes = txBytes,
                                rxBytes = rxBytes,
                                ssid = ssid,
                                ap = ap,
                                mode = mode,
                                active = true,
                                isBlocked = isBlocked
                            )
                        )
                    }
                }

                if (parsedClients.isEmpty() && _clients.value.isNotEmpty()) {
                    _clients.value.filter { it.wifiSsid != null || it.interfaceName.contains("WifiMaster") || it.interfaceName.contains("AccessPoint") }
                        .forEach { c ->
                            parsedClients.add(
                                WirelessClient(
                                    mac = c.mac,
                                    ip = c.ip,
                                    hostname = c.hostname,
                                    displayName = c.displayName,
                                    band = c.wifiBand ?: if (c.interfaceName.contains("WifiMaster0")) "5 GHz" else "2.4 GHz",
                                    rssi = c.wifiRssi,
                                    txRateKbps = c.txSpeedKbps,
                                    rxRateKbps = c.rxSpeedKbps,
                                    txBytes = c.txBytes,
                                    rxBytes = c.rxBytes,
                                    ssid = c.wifiSsid ?: "Keenetic-Home",
                                    ap = c.interfaceName,
                                    mode = if (c.wifiBand?.contains("5") == true) "802.11ax" else "802.11n",
                                    active = c.active,
                                    isBlocked = c.isBlocked
                                )
                            )
                        }
                }

                if (parsedClients.isNotEmpty()) {
                    _wirelessClients.value = parsedClients.sortedWith(compareBy<WirelessClient> { it.band }.thenBy { -(it.rssi ?: -999) })
                }

                // 2. Parse WifiStation interface
                val st0 = repository.queryShow("interface/WifiMaster0/WifiStation0")
                    ?: repository.queryShow("interface/WifiStation0")
                val st1 = repository.queryShow("interface/WifiMaster1/WifiStation0")
                val activeStationJson = when {
                    st0 != null && st0.isJsonObject && st0.asJsonObject.get("state")?.asString?.equals("up", ignoreCase = true) == true -> st0.asJsonObject
                    st1 != null && st1.isJsonObject && st1.asJsonObject.get("state")?.asString?.equals("up", ignoreCase = true) == true -> st1.asJsonObject
                    st0 != null && st0.isJsonObject -> st0.asJsonObject
                    st1 != null && st1.isJsonObject -> st1.asJsonObject
                    else -> null
                }

                if (activeStationJson != null) {
                    val sId = activeStationJson.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: "WifiMaster0/WifiStation0"
                    val isUp = activeStationJson.get("state")?.takeIf { it.isJsonPrimitive }?.asString?.equals("up", ignoreCase = true) == true ||
                            activeStationJson.get("link")?.takeIf { it.isJsonPrimitive }?.asString?.equals("up", ignoreCase = true) == true
                    val ssid = activeStationJson.get("ssid")?.takeIf { it.isJsonPrimitive }?.asString
                    val ip = activeStationJson.get("ip")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: activeStationJson.getAsJsonObject("address")?.get("ip")?.takeIf { it.isJsonPrimitive }?.asString
                    val mac = activeStationJson.get("mac")?.takeIf { it.isJsonPrimitive }?.asString
                    val rssi = activeStationJson.get("rssi")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
                    val masterRadio = if (sId.contains("WifiMaster1")) "WifiMaster1" else "WifiMaster0"

                    _wifiStationStatus.value = WifiStationStatus(
                        id = sId,
                        masterRadio = masterRadio,
                        isUp = isUp,
                        connectedSsid = ssid,
                        ip = ip,
                        mac = mac,
                        rssi = rssi,
                        description = activeStationJson.get("description")?.takeIf { it.isJsonPrimitive }?.asString,
                        state = if (isUp) "up" else "down"
                    )
                }

                // 3. Update wifi networks list
                if (ifaceShow != null) {
                    val wifiList = InterfaceMapper.toWifiNetworks(ifaceShow)
                    if (wifiList.isNotEmpty()) {
                        val currentClients = _wirelessClients.value
                        val updatedWifi = wifiList.map { wifi ->
                            val count = currentClients.count { it.band == wifi.band || it.ssid == wifi.ssid }
                            wifi.copy(clientsCount = count)
                        }
                        _wifiNetworks.value = updatedWifi
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadWifiData", e)
            } finally {
                _isWifiLoading.value = false
            }
        }
    }

    fun toggleWifiBand(band: String, enabled: Boolean) {
        val is24G = band.contains("2.4") || band.contains("Master1", ignoreCase = true)
        val masterRadio = if (is24G) "WifiMaster1" else "WifiMaster0"
        val apName = "$masterRadio/AccessPoint0"
        val label = if (is24G) "2.4 ГГц" else "5 ГГц"

        _wifiNetworks.value = _wifiNetworks.value.map { net ->
            if (net.band.contains(if (is24G) "2.4" else "5")) {
                net.copy(enabled = enabled)
            } else net
        }

        viewModelScope.launch {
            try {
                val commands = listOf(
                    mapOf("interface" to mapOf("up" to enabled, "name" to masterRadio)),
                    mapOf("interface" to mapOf("up" to enabled, "name" to apName))
                )
                val success = repository.executeRciWithSave(commands)
                _wifiActionMessage.value = if (success) {
                    "Диапазон $label ${if (enabled) "включён" else "выключен"}"
                } else {
                    "Команда отправлена на $label"
                }
                loadWifiData()
            } catch (e: Exception) {
                AppLogger.logError("toggleWifiBand", e)
                _wifiActionMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun updateWifiNetworkConfig(
        band: String,
        newSsid: String,
        newPassword: String,
        channel: Int?,
        txPowerPercent: Int?
    ) {
        val is24G = band.contains("2.4") || band.contains("Master1", ignoreCase = true)
        val masterRadio = if (is24G) "WifiMaster1" else "WifiMaster0"
        val apName = "$masterRadio/AccessPoint0"
        val label = if (is24G) "2.4 ГГц" else "5 ГГц"

        _wifiNetworks.value = _wifiNetworks.value.map { net ->
            if (net.band.contains(if (is24G) "2.4" else "5")) {
                net.copy(
                    ssid = if (newSsid.isNotBlank()) newSsid else net.ssid,
                    channel = channel ?: net.channel
                )
            } else net
        }

        viewModelScope.launch {
            try {
                val commands = mutableListOf<Map<String, Any>>()
                if (newSsid.isNotBlank()) {
                    commands.add(mapOf("interface" to mapOf("name" to apName, "ssid" to newSsid)))
                }
                if (newPassword.isNotBlank()) {
                    commands.add(mapOf("interface" to mapOf("name" to apName, "wpa-psk" to newPassword)))
                }
                if (channel != null && channel > 0) {
                    commands.add(mapOf("interface" to mapOf("name" to masterRadio, "channel" to channel)))
                }
                if (txPowerPercent != null) {
                    commands.add(mapOf("interface" to mapOf("name" to masterRadio, "tx-power" to txPowerPercent)))
                }
                if (commands.isNotEmpty()) {
                    val success = repository.executeRciWithSave(commands)
                    _wifiActionMessage.value = if (success) "Параметры Wi-Fi $label сохранены" else "Настройки применены"
                    loadWifiData()
                }
            } catch (e: Exception) {
                AppLogger.logError("updateWifiNetworkConfig", e)
                _wifiActionMessage.value = "Ошибка сохранения Wi-Fi: ${e.message}"
            }
        }
    }

    fun toggleWifiStation(stationId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf("up" to enabled, "name" to stationId))
                val success = repository.executeRciWithSave(listOf(cmd))
                _wifiStationStatus.value = _wifiStationStatus.value.copy(isUp = enabled, state = if (enabled) "up" else "down")
                _wifiActionMessage.value = "WifiStation ${if (enabled) "включён" else "выключен"}"
                loadWifiData()
            } catch (e: Exception) {
                AppLogger.logError("toggleWifiStation", e)
                _wifiActionMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun connectWifiStation(masterRadio: String, ssid: String, password: String) {
        viewModelScope.launch {
            _isWifiLoading.value = true
            try {
                val stationId = "$masterRadio/WifiStation0"
                val cmds = listOf(
                    mapOf("interface" to mapOf("ip" to mapOf("address" to mapOf("no" to true, "dhcp" to true)), "name" to stationId)),
                    mapOf("interface" to mapOf("description" to ssid, "name" to stationId)),
                    mapOf("interface" to mapOf("ssid" to ssid, "name" to stationId)),
                    mapOf("interface" to mapOf(
                        "encryption" to mapOf(
                            "enable" to mapOf("no" to false),
                            "wpa" to mapOf("no" to true),
                            "wpa2" to mapOf("no" to false),
                            "owe" to mapOf("no" to true),
                            "wpa3" to mapOf("no" to true)
                        ),
                        "authentication" to mapOf("wpa-psk" to mapOf("psk" to password)),
                        "name" to stationId
                    )),
                    mapOf("interface" to mapOf("up" to true, "name" to stationId))
                )
                val success = repository.executeRciWithSave(cmds)
                _wifiActionMessage.value = if (success) "Подключение к «$ssid» настроено" else "Команда RCI отправлена"
                loadWifiData()
            } catch (e: Exception) {
                AppLogger.logError("connectWifiStation", e)
                _wifiActionMessage.value = "Ошибка подключения: ${e.message}"
            } finally {
                _isWifiLoading.value = false
            }
        }
    }

    fun disconnectWifiStation(masterRadio: String) {
        viewModelScope.launch {
            _isWifiLoading.value = true
            try {
                val stationId = "$masterRadio/WifiStation0"
                val cmds = listOf(
                    mapOf("interface" to mapOf("ssid" to mapOf("no" to true), "name" to stationId)),
                    mapOf("interface" to mapOf("up" to false, "name" to stationId)),
                    mapOf("interface" to mapOf("description" to mapOf("no" to true), "name" to stationId)),
                    mapOf("interface" to mapOf("authentication" to mapOf("wpa-psk" to mapOf("no" to true)), "name" to stationId)),
                    mapOf("interface" to mapOf(
                        "encryption" to mapOf("enable" to mapOf("no" to true), "wpa" to mapOf("no" to true), "wpa2" to mapOf("no" to true)),
                        "name" to stationId
                    ))
                )
                repository.executeRciWithSave(cmds)
                _wifiActionMessage.value = "WifiStation отключен"
                loadWifiData()
            } catch (e: Exception) {
                AppLogger.logError("disconnectWifiStation", e)
            } finally {
                _isWifiLoading.value = false
            }
        }
    }

    fun scanWifiSiteSurvey(masterRadio: String) {
        viewModelScope.launch {
            _isWifiScanning.value = true
            _wifiScanResults.value = emptyList()
            try {
                val res = repository.queryShow("site-survey", mapOf("site-survey" to mapOf("name" to masterRadio)))
                val results = mutableListOf<WifiSiteSurveyEntry>()
                val cells = when {
                    res?.isJsonObject == true && res.asJsonObject.has("ap_cell") -> res.asJsonObject.getAsJsonArray("ap_cell")
                    res?.isJsonObject == true && res.asJsonObject.has("cell") -> res.asJsonObject.getAsJsonArray("cell")
                    res?.isJsonArray == true -> res.asJsonArray
                    else -> null
                }
                cells?.forEach { cellEl ->
                    if (cellEl.isJsonObject) {
                        val c = cellEl.asJsonObject
                        val essid = c.get("essid")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: c.get("ssid")?.takeIf { it.isJsonPrimitive }?.asString
                            ?: return@forEach
                        val bssid = c.get("bssid")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                        val channel = c.get("channel")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(0) ?: 0
                        val rssi = c.get("rssi")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(-80) ?: -80
                        val enc = c.get("encryption")?.takeIf { it.isJsonPrimitive }?.asString ?: "WPA2"
                        val band = if (masterRadio.contains("1") || channel > 14) "5 GHz" else "2.4 GHz"
                        results.add(WifiSiteSurveyEntry(essid, bssid, channel, rssi, enc, band))
                    }
                }
                _wifiScanResults.value = results.sortedByDescending { it.rssi }
            } catch (e: Exception) {
                AppLogger.logError("scanWifiSiteSurvey", e)
            } finally {
                _isWifiScanning.value = false
            }
        }
    }

    fun updateWifiNetwork(
        networkId: String,
        ssid: String? = null,
        password: String? = null,
        enabled: Boolean? = null,
        wpsEnabled: Boolean? = null,
        peerIsolation: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val wlanFields = mutableMapOf<String, Any>("id" to networkId)
                ssid?.let { wlanFields["ssid"] = mapOf("name" to it) }
                password?.let { wlanFields["wpa"] = mapOf("psk" to it) }
                enabled?.let { wlanFields["enable"] = it }
                wpsEnabled?.let { wlanFields["wps"] = mapOf("enable" to it) }
                peerIsolation?.let { wlanFields["peer-isolation"] = it }

                if (wlanFields.size > 1) {
                    val cmd = mapOf("mws" to mapOf("wlan" to wlanFields))
                    repository.executeRciWithSave(listOf(cmd))
                    loadInterfaces()
                }
            } catch (e: Exception) {
                AppLogger.logError("updateWifiNetwork", e)
            }
        }
    }

    fun toggleInterface(name: String, up: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf("up" to up, "name" to name))
                repository.executeRciWithSave(listOf(cmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("toggleInterface", e)
            }
        }
    }

    fun setCustomDoh(url: String) {
        viewModelScope.launch {
            try {
                val upstream = listOf(
                    mapOf("no" to true),
                    mapOf("url" to url, "hash" to "", "domain" to "")
                )
                val cmd = mapOf("dns-proxy" to mapOf("https" to mapOf("upstream" to upstream)))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setCustomDoh", e)
            }
        }
    }

    fun createSchedule(name: String, description: String, daysOfWeek: List<Int>, startHour: Int, startMin: Int, stopHour: Int, stopMin: Int) {
        viewModelScope.launch {
            try {
                val actions = mutableListOf<Map<String, Any>>()
                daysOfWeek.forEach { dow ->
                    actions += mapOf("action" to "start", "hour" to startHour.toString(), "min" to startMin.toString(), "dow" to dow.toString())
                    actions += mapOf("action" to "stop", "hour" to stopHour.toString(), "min" to stopMin.toString(), "dow" to dow.toString())
                }
                val cmd = mapOf("schedule" to mapOf("name" to name, "description" to description, "action" to actions))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("createSchedule", e)
            }
        }
    }

    fun addPortForwardingRule(rule: PortForwardingRule) {
        _portForwardingRules.value = _portForwardingRules.value + rule
        viewModelScope.launch {
            try {
                val cmd = mapOf(
                    "ip" to mapOf(
                        "static" to listOf(
                            mapOf(
                                "comment" to rule.name,
                                "protocol" to rule.proto.lowercase(),
                                "interface" to rule.interfaceName,
                                "port" to rule.srcPort,
                                "to-address" to rule.dstIp,
                                "to-port" to rule.dstPort
                            )
                        )
                    )
                )
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("addPortForwardingRule", e)
            }
        }
    }

    fun deletePortForwardingRule(id: String) {
        _portForwardingRules.value = _portForwardingRules.value.filter { it.id != id }
    }

    fun addFirewallRule(rule: FirewallRule) {
        _firewallRules.value = _firewallRules.value + rule
        viewModelScope.launch {
            try {
                val aclName = "_WEBADMIN_${rule.interfaceName}"
                val verbKey = if (rule.action.lowercase() == "deny") "deny" else "permit"
                val ruleFields = mutableMapOf<String, Any>(
                    "index" to 0,
                    "action" to rule.action.lowercase(),
                    "source" to rule.srcIp.ifBlank { "0.0.0.0" },
                    "source-mask" to "0.0.0.0",
                    "destination" to rule.dstIp.ifBlank { "0.0.0.0" },
                    "destination-mask" to "0.0.0.0",
                    "disable" to false
                )
                if (rule.proto.isNotBlank()) ruleFields["protocol"] = rule.proto.lowercase()
                if (rule.comment.isNotBlank()) ruleFields["description"] = rule.comment

                val aclCmd = mapOf("access-list" to listOf(mapOf("acl" to aclName, verbKey to ruleFields)))
                val ifaceCmd = mapOf("interface" to mapOf("ip" to mapOf("access-group" to listOf(mapOf("acl" to aclName, "direction" to "in"))), "name" to rule.interfaceName))
                repository.executeRciWithSave(listOf(aclCmd, ifaceCmd))
            } catch (e: Exception) {
                AppLogger.logError("addFirewallRule", e)
            }
        }
    }

    fun deleteFirewallRule(id: String) {
        _firewallRules.value = _firewallRules.value.filter { it.id != id }
    }

    fun addStaticRoute(route: StaticRoute) {
        _staticRoutes.value = _staticRoutes.value + route
        viewModelScope.launch {
            try {
                val routeMap = mutableMapOf<String, Any>(
                    "network" to route.network,
                    "mask" to route.mask,
                    "interface" to route.interfaceName
                )
                if (route.gateway.isNotBlank()) routeMap["gateway"] = route.gateway
                if (route.comment.isNotBlank()) routeMap["comment"] = route.comment
                val cmd = mapOf("ip" to mapOf("route" to routeMap))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("addStaticRoute", e)
            }
        }
    }

    fun deleteStaticRoute(id: String) {
        _staticRoutes.value = _staticRoutes.value.filter { it.id != id }
    }

    fun loadDnsFilters() {
        viewModelScope.launch {
            _dnsFilterPresets.value = ApiCallState.Loading
            _dnsFilterProfiles.value = ApiCallState.Loading
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("show" to mapOf("dns-proxy" to mapOf("filter" to mapOf("presets" to emptyMap<String, Any>())))),
                        mapOf("show" to mapOf("dns-proxy" to mapOf("filter" to mapOf("profiles" to emptyMap<String, Any>()))))
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isJsonArray == true) {
                        val arr = body.asJsonArray
                        val item0 = if (arr.size() > 0) arr.get(0) else null
                        val item1 = if (arr.size() > 1) arr.get(1) else null
                        val p0 = item0?.asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("dns-proxy")?.getAsJsonObject("filter")?.get("presets")
                        val p1 = item1?.asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("dns-proxy")?.getAsJsonObject("filter")?.get("profiles")
                        _dnsFilterPresets.value = if (p0 != null) ApiCallState.Success(p0) else ApiCallState.Error("Пустой ответ")
                        _dnsFilterProfiles.value = if (p1 != null) ApiCallState.Success(p1) else ApiCallState.Error("Пустой ответ")

                        // Parse into typed models
                        _dnsFilterPresetList.value = DnsAndScheduleParser.parseDnsFilterPresets(p0)
                        _dnsFilterProfileList.value = DnsAndScheduleParser.parseDnsFilterProfiles(p1)
                    } else if (body != null) {
                        _dnsFilterPresets.value = ApiCallState.Success(body)
                        _dnsFilterProfiles.value = ApiCallState.Success(body)
                    }
                } else {
                    _dnsFilterPresets.value = ApiCallState.Error("HTTP ${response.code()}")
                    _dnsFilterProfiles.value = ApiCallState.Error("HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _dnsFilterPresets.value = ApiCallState.Error(e.message ?: "Ошибка загрузки пресетов DNS")
                _dnsFilterProfiles.value = ApiCallState.Error(e.message ?: "Ошибка загрузки профилей DNS")
            }
        }
    }

    fun loadVpnServerStatus() {
        viewModelScope.launch {
            _vpnServerRaw.value = ApiCallState.Loading
            _vpnServerStatus.value = null
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("vpn-server" to emptyMap<String, Any>())))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) {
                        body.asJsonArray[0].asJsonObject?.getAsJsonObject("show")?.get("vpn-server") ?: body.asJsonArray[0]
                    } else body
                    _vpnServerRaw.value = if (first != null) ApiCallState.Success(first) else ApiCallState.Error("Пустой ответ")

                    // Parse into typed VpnServerStatus
                    if (first != null) {
                        _vpnServerStatus.value = VpnServerParser.parseToStatus(first)
                    }
                } else {
                    _vpnServerRaw.value = ApiCallState.Error("HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _vpnServerRaw.value = ApiCallState.Error(e.message ?: "Ошибка загрузки статуса VPN-сервера")
            }
        }
    }

    fun wakeOnLan(mac: String) {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Wake-on-LAN", "mac=$mac")
                val cmd = mapOf("ip" to mapOf("hotspot" to mapOf("wake" to mapOf("mac" to mac))))
                repository.getRestApi().executeRci(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("wakeOnLan", e)
            }
        }
    }

    fun fetchRunningConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            _configLoading.value = true
            try {
                // Try fetching via RCI show/running-config
                val response = repository.getRestApi().getRunningConfig()
                if (response.isSuccessful && response.body() != null) {
                    val text = response.body()!!.string()
                    _rawRunningConfig.value = text
                    val parsed = KeeneticConfigParser.parse(text)
                    _parsedConfig.value = parsed

                    // Sync policies if found
                    if (parsed.policies.isNotEmpty()) {
                        val newPolicies = mutableListOf(
                            ConnectionPolicy("", "Основная (по умолчанию)", "Следовать политике сегмента сети (conform)")
                        )
                        parsed.policies.forEach { pol ->
                            val desc = pol.description.ifBlank { pol.id }
                            newPolicies.add(ConnectionPolicy(id = pol.id, name = desc, description = "Permit: ${pol.permitInterfaces.joinToString(", ")}"))
                        }
                        _connectionPolicies.value = newPolicies
                    }
                } else {
                    // Try fallback via SSH if available
                    val ip = _savedIp.value.ifBlank { "192.168.1.1" }
                    val user = _savedUsername.value.ifBlank { "admin" }
                    val pass = encryptedStorage.getPassword() ?: ""
                    val port = _sshPort.value.toIntOrNull() ?: 22
                    if (pass.isNotBlank()) {
                        val sshRes = sshService.executeCommand(ip, port, user, pass, "show running-config")
                        if (sshRes.success && sshRes.output.isNotBlank()) {
                            _rawRunningConfig.value = sshRes.output
                            val parsed = KeeneticConfigParser.parse(sshRes.output)
                            _parsedConfig.value = parsed
                        } else {
                            _rawRunningConfig.value = "# Не удалось получить running-config: HTTP ${response.code()}"
                        }
                    } else {
                        _rawRunningConfig.value = "# Не удалось получить running-config: HTTP ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("fetchRunningConfig", e)
                _rawRunningConfig.value = "# Ошибка загрузки: ${e.localizedMessage}"
            } finally {
                _configLoading.value = false
            }
        }
    }

    fun saveConfigurationToNvram() {
        viewModelScope.launch(Dispatchers.IO) {
            _isExecutingCli.value = true
            _saveConfigMessage.value = null
            try {
                // system configuration save
                val cmd = mapOf("system" to mapOf("configuration" to mapOf("save" to true)))
                val response = repository.getRestApi().executeRci(listOf(cmd))
                if (response.isSuccessful) {
                    _saveConfigMessage.value = "Конфигурация успешно сохранена в NVRAM (startup-config)"
                    AppLogger.logAction("SaveConfig", "Saved to startup-config via RCI")
                } else {
                    // Fallback to SSH
                    val ip = _savedIp.value.ifBlank { "192.168.1.1" }
                    val user = _savedUsername.value.ifBlank { "admin" }
                    val pass = encryptedStorage.getPassword() ?: ""
                    val port = _sshPort.value.toIntOrNull() ?: 22
                    val sshRes = sshService.executeCommand(ip, port, user, pass, "system configuration save")
                    if (sshRes.success) {
                        _saveConfigMessage.value = "Конфигурация успешно сохранена в NVRAM через SSH CLI"
                    } else {
                        _saveConfigMessage.value = "Ошибка сохранения: HTTP ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _saveConfigMessage.value = "Ошибка сохранения: ${e.localizedMessage}"
            } finally {
                _isExecutingCli.value = false
            }
        }
    }

    fun executeRawCliOrRci(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isExecutingCli.value = true
            _cliExecutionResult.value = "Выполнение команды..."
            try {
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    // Raw JSON payload for RCI
                    val gson = com.google.gson.Gson()
                    val elem = gson.fromJson(trimmed, JsonElement::class.java)
                    val cmdList = if (elem.isJsonArray) {
                        elem.asJsonArray.map { gson.fromJson(it, Map::class.java) as Map<String, Any> }
                    } else {
                        listOf(gson.fromJson(elem, Map::class.java) as Map<String, Any>)
                    }
                    val resp = repository.getRestApi().executeRci(cmdList)
                    if (resp.isSuccessful) {
                        _cliExecutionResult.value = gson.toJson(resp.body())
                    } else {
                        _cliExecutionResult.value = "HTTP Error ${resp.code()}: ${resp.errorBody()?.string()}"
                    }
                } else {
                    // CLI command (e.g. "show running-config", "system configuration save", "show version")
                    // If SSH is available, execute via SSH CLI for complete output
                    val ip = _savedIp.value.ifBlank { "192.168.1.1" }
                    val user = _savedUsername.value.ifBlank { "admin" }
                    val pass = encryptedStorage.getPassword() ?: ""
                    val port = _sshPort.value.toIntOrNull() ?: 22

                    if (pass.isNotBlank()) {
                        val sshRes = sshService.executeCommand(ip, port, user, pass, trimmed)
                        if (sshRes.success) {
                            _cliExecutionResult.value = sshRes.output.ifBlank { "Команда выполнена успешно (без вывода)" }
                            return@launch
                        }
                    }

                    // RCI mapping fallback
                    if (trimmed.startsWith("show ", ignoreCase = true)) {
                        val path = trimmed.substringAfter("show ").trim().replace(" ", "/")
                        val resp = repository.getRestApi().queryShow(path)
                        if (resp.isSuccessful) {
                            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                            _cliExecutionResult.value = gson.toJson(resp.body())
                        } else {
                            _cliExecutionResult.value = "RCI HTTP ${resp.code()}: ${resp.errorBody()?.string()}"
                        }
                    } else {
                        _cliExecutionResult.value = "Для выполнения произвольных конфигурационных команд CLI настройте SSH-доступ в разделе Настройки/Перезагрузка, либо отправьте RCI JSON."
                    }
                }
            } catch (e: Exception) {
                _cliExecutionResult.value = "Ошибка выполнения: ${e.localizedMessage}"
            } finally {
                _isExecutingCli.value = false
            }
        }
    }

    fun clearCliResult() {
        _cliExecutionResult.value = null
    }

    fun clearSaveConfigMessage() {
        _saveConfigMessage.value = null
    }

    fun logout() {
        repository.clearSession()
        _isLoggedIn.value = false
        _isDemoMode.value = false
    }
}
