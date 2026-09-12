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

    private val _vpnConnections = MutableStateFlow<List<VpnConnection>>(emptyList())
    val vpnConnections: StateFlow<List<VpnConnection>> = _vpnConnections.asStateFlow()

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

    private val _ipv6StaticRoutes = MutableStateFlow<List<StaticRoute>>(emptyList())
    val ipv6StaticRoutes: StateFlow<List<StaticRoute>> = _ipv6StaticRoutes.asStateFlow()

    private val _dnsRoutes = MutableStateFlow<List<DnsRouteData>>(emptyList())
    val dnsRoutes: StateFlow<List<DnsRouteData>> = _dnsRoutes.asStateFlow()

    private val _fqdnGroups = MutableStateFlow<List<FqdnGroup>>(emptyList())
    val fqdnGroups: StateFlow<List<FqdnGroup>> = _fqdnGroups.asStateFlow()

    private val _currentIpv4Routes = MutableStateFlow<List<RouterRouteEntry>>(emptyList())
    val currentIpv4Routes: StateFlow<List<RouterRouteEntry>> = _currentIpv4Routes.asStateFlow()

    private val _currentIpv6Routes = MutableStateFlow<List<RouterRouteEntry>>(emptyList())
    val currentIpv6Routes: StateFlow<List<RouterRouteEntry>> = _currentIpv6Routes.asStateFlow()

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

    private val _fileBrowserPath = MutableStateFlow("")
    val fileBrowserPath: StateFlow<String> = _fileBrowserPath.asStateFlow()

    private val _fileBrowserEntries = MutableStateFlow<List<FileEntry>>(emptyList())
    val fileBrowserEntries: StateFlow<List<FileEntry>> = _fileBrowserEntries.asStateFlow()

    private val _fileBrowserLoading = MutableStateFlow(false)
    val fileBrowserLoading: StateFlow<Boolean> = _fileBrowserLoading.asStateFlow()

    private val _fileBrowserError = MutableStateFlow<String?>(null)
    val fileBrowserError: StateFlow<String?> = _fileBrowserError.asStateFlow()

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

    private val _dnsFilterInstalled = MutableStateFlow(true)
    val dnsFilterInstalled: StateFlow<Boolean> = _dnsFilterInstalled.asStateFlow()

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

    private val _dohServers = MutableStateFlow<List<DohServerInfo>>(emptyList())
    val dohServers: StateFlow<List<DohServerInfo>> = _dohServers.asStateFlow()

    private val _dotUpstream = MutableStateFlow<List<String>>(emptyList())
    val dotUpstream: StateFlow<List<String>> = _dotUpstream.asStateFlow()

    private val _dotServers = MutableStateFlow<List<DotServerInfo>>(emptyList())
    val dotServers: StateFlow<List<DotServerInfo>> = _dotServers.asStateFlow()

    private val _scNameServers = MutableStateFlow<List<DnsServerInfo>>(emptyList())
    val scNameServers: StateFlow<List<DnsServerInfo>> = _scNameServers.asStateFlow()

    private val _dnsInterceptEnabled = MutableStateFlow<Boolean?>(null)
    val dnsInterceptEnabled: StateFlow<Boolean?> = _dnsInterceptEnabled.asStateFlow()

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
            StaticRoute("1", "10.8.0.0", "255.255.255.0", "10.8.0.1", "Wireguard0", false, "Маршрут к ресурсам офиса", index = "1", type = "network"),
            StaticRoute("2", "192.168.2.0", "255.255.255.0", "192.168.1.254", "Bridge0", false, "Гостевая подсеть", index = "2", type = "network")
        )

        _ipv6StaticRoutes.value = listOf(
            StaticRoute("2001:db8::/64", "2001:db8::", "64", "6000::1", "Home", false, "Маршрут IPv6", index = "1", type = "node", prefix = "2001:db8::/64")
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
                    _vpnConnections.value = InterfaceMapper.toVpnConnections(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadInterfaces", e)
            }
        }
    }

    fun loadVpnConnections() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface")
                if (res != null) {
                    _vpnConnections.value = InterfaceMapper.toVpnConnections(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadVpnConnections", e)
            }
        }
    }

    private val _vpnConnDetail = MutableStateFlow<com.google.gson.JsonObject?>(null)
    val vpnConnDetail: StateFlow<com.google.gson.JsonObject?> = _vpnConnDetail.asStateFlow()

    private val _vpnConnDetailId = MutableStateFlow<String?>(null)
    val vpnConnDetailId: StateFlow<String?> = _vpnConnDetailId.asStateFlow()

    private val _vpnViaInterfaces = MutableStateFlow<List<String>>(emptyList())
    val vpnViaInterfaces: StateFlow<List<String>> = _vpnViaInterfaces.asStateFlow()

    fun loadVpnConnDetail(id: String) {
        viewModelScope.launch {
            try {
                _vpnConnDetail.value = null
                _vpnConnDetailId.value = null
                val res = repository.queryShow("interface")
                if (res != null) {
                    _vpnViaInterfaces.value = InterfaceMapper.suitableViaInterfaces(res)
                    val obj = InterfaceMapper.interfaceObject(res, id)
                    if (obj != null) {
                        val cfg = obj.get("sc")?.takeIf { it.isJsonObject }?.asJsonObject ?: obj
                        _vpnConnDetail.value = cfg
                        _vpnConnDetailId.value = id
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadVpnConnDetail", e)
            }
        }
    }

    fun updateVpnConnection(id: String, fields: Map<String, Any>) {
        if (fields.isEmpty()) {
            loadVpnConnDetail(id)
            return
        }
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf(id to fields))
                repository.executeRciWithSave(listOf(cmd))
                loadVpnConnections()
                loadVpnConnDetail(id)
            } catch (e: Exception) {
                AppLogger.logError("updateVpnConnection", e)
            }
        }
    }

    fun addVpnConnection(type: String, description: String) {
        val id = nextVpnConnectionId(type, _vpnConnections.value.map { it.id })
        if (id == null) {
            AppLogger.logError("addVpnConnection", IllegalStateException("no free id for $type"))
            return
        }
        viewModelScope.launch {
            try {
                val fields = linkedMapOf<String, Any>("description" to description.ifBlank { id })
                val cmd = mapOf("interface" to mapOf(id to fields))
                repository.executeRciWithSave(listOf(cmd))
                _vpnConnections.value = InterfaceMapper.toVpnConnections(repository.queryShow("interface"))
            } catch (e: Exception) {
                AppLogger.logError("addVpnConnection", e)
            }
        }
    }

    private fun nextVpnConnectionId(type: String, existing: List<String>): String? {
        val (prefix, startIndex) = when (type.lowercase()) {
            "pppoe" -> "PPPoE" to 0
            "pptp" -> "Pptp" to 0
            "l2tp" -> "L2tp" to 0
            "sstp" -> "Sstp" to 0
            "wireguard", "wg", "awg" -> "Wireguard" to 0
            "openvpn" -> "OpenVPN" to 0
            "ike", "ikev2" -> "Ike" to 0
            "openconnect" -> "OpenConnect" to 0
            "zerotier" -> "ZeroTier" to 0
            "gre" -> "Gre" to 0
            "ipip" -> "IPIP" to 0
            "eoip" -> "EoIP" to 0
            "ipsec" -> "Ipsec" to 0
            "proxy" -> "Proxy" to 0
            else -> return null
        }
        val used = existing.mapTo(HashSet()) { it.lowercase() }
        for (i in startIndex..99) {
            val candidate = prefix + i
            if (candidate.lowercase() !in used) return candidate
        }
        return null
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
                        res.isJsonObject && res.asJsonObject.has("lease") && res.asJsonObject.get("lease").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("lease")
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
                val res = repository.queryShow("ip/nat")
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
                val text = repository.queryShowText("ip/rule")
                if (text != null) {
                    val list = mutableListOf<FirewallRule>()
                    val lineRegex = Regex("""^\s*(\d+)\s*:\s*from\s+(\S+)\s+to\s+(\S+)\s+lookup\s+(\S+)\s*(?:\((\d+)\))?\s*(.*)$""")
                    text.lines().forEach { line ->
                        val m = lineRegex.find(line) ?: return@forEach
                        val priority = m.groupValues[1]
                        val src = m.groupValues[2]
                        val dst = m.groupValues[3]
                        val table = m.groupValues[4]
                        val tableId = m.groupValues[5]
                        val rest = m.groupValues[6]
                        list.add(
                            FirewallRule(
                                id = priority,
                                action = if ("blackhole" in rest) "reject" else "permit",
                                proto = "IP",
                                srcIp = src,
                                dstIp = dst,
                                dstPort = "any",
                                interfaceName = table,
                                enabled = true,
                                comment = rest.trim()
                            )
                        )
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
        if (_isDemoMode.value && _staticRoutes.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/ip")
                if (res != null) {
                    _staticRoutes.value = parseStaticRouteList(res, ipv6 = false)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadStaticRoutes", e)
            }
        }
    }

    fun loadIpv6StaticRoutes() {
        if (_isDemoMode.value && _ipv6StaticRoutes.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                // Verified: user IPv6 routes live in sc/ipv6 -> route[] (sc/ipv6/static is empty)
                val res = repository.queryShow("sc/ipv6")
                if (res != null) {
                    _ipv6StaticRoutes.value = parseStaticRouteList(res, ipv6 = true)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6StaticRoutes", e)
            }
        }
    }

    private fun parseStaticRouteList(element: com.google.gson.JsonElement?, ipv6: Boolean): List<StaticRoute> {
        val list = mutableListOf<StaticRoute>()
        if (element == null || element.isJsonNull) return list

        fun addRoute(o: com.google.gson.JsonObject, forcedId: String?) {
            fun str(key: String): String =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            fun bool(key: String): Boolean =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false

            val index = str("index").ifBlank { forcedId.orEmpty() }
            val host = str("host")
            val networkAddr = str("network")
            val mask = str("mask").ifBlank { "255.255.255.0" }
            val prefix = str("prefix")
            val isDefault = bool("default") || (host.isBlank() && networkAddr.isBlank() && prefix.isBlank())
            val gateway = str("gateway")
            val iface = str("interface")
            val comment = str("comment")
            val auto = bool("auto")
            val reject = bool("reject")
            val enabled = !bool("disable")

            val type = when {
                isDefault -> "default"
                host.isNotBlank() -> "host"
                ipv6 || prefix.isNotBlank() -> "node"
                else -> "network"
            }

            list.add(
                StaticRoute(
                    id = index.ifBlank { "${prefix.ifBlank { networkAddr.ifBlank { host } }}_$iface" },
                    network = networkAddr.ifBlank { host },
                    mask = mask,
                    gateway = gateway,
                    interfaceName = iface,
                    auto = auto,
                    comment = comment,
                    index = index,
                    type = type,
                    prefix = prefix,
                    reject = reject,
                    enabled = enabled
                )
            )
        }

        if (element.isJsonArray) {
            element.asJsonArray.forEach { item ->
                if (item.isJsonObject) addRoute(item.asJsonObject, null)
            }
        } else if (element.isJsonObject) {
            val root = element.asJsonObject
            if (root.has("static") && root.get("static").isJsonObject) {
                root.getAsJsonObject("static").entrySet().forEach { (k, v) ->
                    if (v.isJsonObject) addRoute(v.asJsonObject, k)
                }
            } else if (root.has("route") && root.get("route").isJsonArray) {
                root.getAsJsonArray("route").forEach { item ->
                    if (item.isJsonObject) addRoute(item.asJsonObject, null)
                }
            } else {
                root.entrySet().forEach { (k, v) ->
                    if (v.isJsonObject) addRoute(v.asJsonObject, k)
                }
            }
        }
        return list
    }

    private fun buildIpv4RouteData(route: StaticRoute): Map<String, Any> {
        val m = mutableMapOf<String, Any>()
        when (route.type) {
            "host" -> m["host"] = route.network
            "default" -> m["default"] = true
            else -> {
                m["network"] = route.network
                m["mask"] = route.mask.ifBlank { "255.255.255.0" }
            }
        }
        if (route.gateway.isNotBlank()) m["gateway"] = route.gateway
        if (route.interfaceName.isNotBlank() && route.interfaceName != "Auto") m["interface"] = route.interfaceName
        if (route.comment.isNotBlank()) m["comment"] = route.comment
        m["auto"] = route.auto
        m["reject"] = route.reject
        m["disable"] = !route.enabled
        return m
    }

    private fun buildIpv6RouteData(route: StaticRoute): Map<String, Any> {
        val m = mutableMapOf<String, Any>()
        if (route.type == "default") {
            m["default"] = true
        } else {
            m["prefix"] = route.prefix.ifBlank { route.network }
        }
        if (route.gateway.isNotBlank()) m["gateway"] = route.gateway
        if (route.interfaceName.isNotBlank() && route.interfaceName != "Auto") m["interface"] = route.interfaceName
        if (route.comment.isNotBlank()) m["comment"] = route.comment
        m["auto"] = route.auto
        m["reject"] = route.reject
        m["disable"] = !route.enabled
        return m
    }

    fun saveStaticRoute(route: StaticRoute) {
        viewModelScope.launch {
            try {
                val data = buildIpv4RouteData(route).toMutableMap()
                if (route.index.isNotBlank()) data["index"] = route.index
                // Verified on KN-2311: POST {"ip": {"route": {...}}} (dotted "ip.static" -> "not found")
                repository.executeRciWithSave(listOf(mapOf("ip" to mapOf("route" to data))))
                loadStaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("saveStaticRoute", e)
            }
        }
    }

    fun deleteStaticRoute(route: StaticRoute?) {
        val index = route?.index
        if (index.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(listOf(mapOf("ip" to mapOf("route" to mapOf("index" to index, "no" to true)))))
                loadStaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("deleteStaticRoute", e)
            }
        }
    }

    fun toggleStaticRoute(route: StaticRoute, enabled: Boolean) {
        if (route.index.isBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(
                    listOf(mapOf("ip" to mapOf("route" to mapOf("disable" to mapOf("index" to route.index, "no" to enabled)))))
                )
                loadStaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("toggleStaticRoute", e)
            }
        }
    }

    fun saveIpv6StaticRoute(route: StaticRoute) {
        viewModelScope.launch {
            try {
                val data = buildIpv6RouteData(route).toMutableMap()
                if (route.index.isNotBlank()) data["index"] = route.index
                repository.executeRciWithSave(listOf(mapOf("ipv6" to mapOf("route" to data))))
                loadIpv6StaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("saveIpv6StaticRoute", e)
            }
        }
    }

    fun deleteIpv6StaticRoute(route: StaticRoute?) {
        val index = route?.index
        if (index.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(listOf(mapOf("ipv6" to mapOf("route" to mapOf("index" to index, "no" to true)))))
                loadIpv6StaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("deleteIpv6StaticRoute", e)
            }
        }
    }

    fun toggleIpv6StaticRoute(route: StaticRoute, enabled: Boolean) {
        if (route.index.isBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(
                    listOf(mapOf("ipv6" to mapOf("route" to mapOf("disable" to mapOf("index" to route.index, "no" to enabled)))))
                )
                loadIpv6StaticRoutes()
            } catch (e: Exception) {
                AppLogger.logError("toggleIpv6StaticRoute", e)
            }
        }
    }

    fun loadViaInterfaces() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface")
                if (res != null) {
                    _vpnViaInterfaces.value = InterfaceMapper.suitableViaInterfaces(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadViaInterfaces", e)
            }
        }
    }

    fun loadDnsRoutes() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/dns-proxy/route")
                if (res != null) {
                    _dnsRoutes.value = parseDnsRouteList(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDnsRoutes", e)
            }
        }
    }

    private fun parseDnsRouteList(element: com.google.gson.JsonElement?): List<DnsRouteData> {
        val list = mutableListOf<DnsRouteData>()
        if (element == null || element.isJsonNull) return list

        fun addRoute(o: com.google.gson.JsonObject) {
            fun str(key: String): String =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            fun bool(key: String): Boolean =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
            val index = str("index")
            val group = str("group")
            if (group.isBlank()) return
            list.add(
                DnsRouteData(
                    id = index.ifBlank { group },
                    index = index,
                    group = group,
                    gateway = str("gateway"),
                    interfaceName = str("interface"),
                    reject = bool("reject"),
                    enabled = !bool("disable")
                )
            )
        }

        if (element.isJsonArray) {
            element.asJsonArray.forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
        } else if (element.isJsonObject) {
            element.asJsonObject.entrySet().forEach { (k, v) ->
                if (v.isJsonObject) addRoute(v.asJsonObject)
                else if (v.isJsonArray) v.asJsonArray.forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
            }
        }
        return list
    }

    fun saveDnsRoute(route: DnsRouteData) {
        viewModelScope.launch {
            try {
                val data = mutableMapOf<String, Any>()
                data["group"] = route.group
                if (route.gateway.isNotBlank()) data["gateway"] = route.gateway
                if (route.interfaceName.isNotBlank() && route.interfaceName != "Auto") data["interface"] = route.interfaceName
                data["auto"] = false
                data["reject"] = route.reject
                if (route.index.isNotBlank()) {
                    data["index"] = route.index
                } else {
                    data["disable"] = !route.enabled
                }
                repository.executeRciWithSave(listOf(mapOf("dns-proxy.route" to data)))
                loadDnsRoutes()
            } catch (e: Exception) {
                AppLogger.logError("saveDnsRoute", e)
            }
        }
    }

    fun deleteDnsRoute(route: DnsRouteData?) {
        val index = route?.index
        if (index.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(listOf(mapOf("dns-proxy.route" to mapOf("index" to index, "no" to true))))
                loadDnsRoutes()
            } catch (e: Exception) {
                AppLogger.logError("deleteDnsRoute", e)
            }
        }
    }

    fun toggleDnsRoute(route: DnsRouteData, enabled: Boolean) {
        if (route.index.isBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(
                    listOf(mapOf("dns-proxy.route" to mapOf("disable" to mapOf("index" to route.index, "no" to enabled))))
                )
                loadDnsRoutes()
            } catch (e: Exception) {
                AppLogger.logError("toggleDnsRoute", e)
            }
        }
    }

    fun loadFqdnGroups() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/object-group/fqdn")
                if (res != null) {
                    val list = mutableListOf<FqdnGroup>()
                    if (res.isJsonObject) {
                        res.asJsonObject.entrySet().forEach { (name, value) ->
                            if (value.isJsonObject) {
                                val o = value.asJsonObject
                                val desc = o.get("description")?.takeIf { it.isJsonPrimitive }?.asString ?: name
                                val include = mutableListOf<String>()
                                o.get("include")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
                                    if (item.isJsonObject) {
                                        item.asJsonObject.get("address")?.takeIf { it.isJsonPrimitive }?.asString?.let { include.add(it) }
                                    }
                                }
                                list.add(FqdnGroup(name = name, description = desc, domains = include))
                            }
                        }
                    } else if (res.isJsonArray) {
                        res.asJsonArray.forEach { item ->
                            if (item.isJsonObject) {
                                val o = item.asJsonObject
                                val name = o.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: o.get("group")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                                if (name.isNotBlank()) {
                                    val desc = o.get("description")?.takeIf { it.isJsonPrimitive }?.asString ?: name
                                    val include = mutableListOf<String>()
                                    o.get("include")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { i ->
                                        if (i.isJsonObject) i.asJsonObject.get("address")?.takeIf { it.isJsonPrimitive }?.asString?.let { include.add(it) }
                                    }
                                    list.add(FqdnGroup(name = name, description = desc, domains = include))
                                }
                            }
                        }
                    }
                    _fqdnGroups.value = list
                }
            } catch (e: Exception) {
                AppLogger.logError("loadFqdnGroups", e)
            }
        }
    }

    fun createFqdnGroup(name: String, description: String, domains: List<String>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val include = domains.filter { it.isNotBlank() }.distinct().map { mapOf("address" to it) }
                val groupData = mutableMapOf<String, Any>()
                groupData["description"] = description.ifBlank { name }
                groupData["include"] = include
                repository.executeRciWithSave(listOf(mapOf("object-group.fqdn" to mapOf(name to groupData))))
                loadFqdnGroups()
            } catch (e: Exception) {
                AppLogger.logError("createFqdnGroup", e)
            }
        }
    }

    fun deleteFqdnGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.executeRciWithSave(listOf(mapOf("object-group.fqdn" to mapOf("name" to name, "no" to true))))
                loadFqdnGroups()
            } catch (e: Exception) {
                AppLogger.logError("deleteFqdnGroup", e)
            }
        }
    }

    fun loadCurrentRoutes() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/route")
                if (res != null) {
                    _currentIpv4Routes.value = parseCurrentRouteList(res)
                }
                val res6 = repository.queryShow("ipv6/route")
                if (res6 != null) {
                    _currentIpv6Routes.value = parseCurrentRouteList(res6)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadCurrentRoutes", e)
            }
        }
    }

    private fun parseCurrentRouteList(element: com.google.gson.JsonElement?): List<RouterRouteEntry> {
        val list = mutableListOf<RouterRouteEntry>()
        if (element == null || element.isJsonNull) return list

        fun addRoute(o: com.google.gson.JsonObject) {
            fun str(key: String): String =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            fun bool(key: String): Boolean =
                o.get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
            val dest = str("destination").ifBlank { str("network") }
            if (dest.isBlank()) return
            list.add(
                RouterRouteEntry(
                    id = "${dest}_${str("interface")}",
                    destination = dest,
                    gateway = str("gateway"),
                    interfaceName = str("interface"),
                    isStatic = bool("static"),
                    isRejecting = bool("rejecting")
                )
            )
        }

        if (element.isJsonArray) {
            element.asJsonArray.forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
        } else if (element.isJsonObject) {
            val root = element.asJsonObject
            if (root.has("route") && root.get("route").isJsonArray) {
                root.getAsJsonArray("route").forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
            } else if (root.has("route6") && root.get("route6").isJsonArray) {
                root.getAsJsonArray("route6").forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
            } else {
                root.entrySet().forEach { (_, v) ->
                    if (v.isJsonObject) addRoute(v.asJsonObject)
                    else if (v.isJsonArray) v.asJsonArray.forEach { item -> if (item.isJsonObject) addRoute(item.asJsonObject) }
                }
            }
        }
        return list
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
                _usbDevicesRaw.value = if (res != null) ApiCallState.Success(res) else ApiCallState.Error("Нет данных")
                val mediaRes = repository.queryShow("media")
                val cifsRes = repository.queryShow("cifs")
                // mount-id (без ":") -> (smb-метка, активна)
                val cifsShares = mutableMapOf<String, Pair<String, Boolean>>()
                cifsRes?.takeIf { it.isJsonObject }?.asJsonObject?.get("share")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
                    if (it.isJsonObject) {
                        val o = it.asJsonObject
                        val mount = o.get("mount")?.takeIf { p -> p.isJsonPrimitive }?.asString?.trimEnd(':') ?: return@forEach
                        val label = o.get("label")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val active = o.get("active")?.takeIf { p -> p.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(true) ?: true
                        cifsShares[mount] = label to active
                    }
                }
                if (res != null) {
                    val list = mutableListOf<UsbStorageDevice>()
                    fun numLong(o: com.google.gson.JsonObject, key: String): Long =
                        o.get(key)?.takeIf { p -> p.isJsonPrimitive }?.runCatching {
                            try { asLong } catch (_: Exception) { asString.toLongOrNull() ?: 0L }
                        }?.getOrDefault(0L) ?: 0L
                    fun strOf(o: com.google.gson.JsonObject, key: String): String? =
                        o.get(key)?.takeIf { p -> p.isJsonPrimitive }?.asString
                    // Богатый источник: media -> разделы с реальными размерами
                    fun parseMediaDrive(key: String, o: com.google.gson.JsonObject): Boolean {
                        val partObj = o.get("partition")?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
                        var added = false
                        partObj.entrySet().forEach { (partId, v) ->
                            if (!v.isJsonObject) return@forEach
                            val po = v.asJsonObject
                            if ((strOf(po, "state") ?: "") != "MOUNTED") return@forEach
                            val fstype = strOf(po, "fstype") ?: ""
                            val uuid = strOf(po, "uuid") ?: ""
                            val isSwap = fstype == "swap"
                            val label = strOf(po, "label")?.ifBlank { null }
                                ?: if (isSwap) "SWAP" else key
                            val share = cifsShares[uuid]
                            val total = numLong(po, "total").let { if (it > 0) it else numLong(po, "size") }
                            val free = numLong(po, "free")
                            val fmtOpts = po.get("format-supported")?.takeIf { it.isJsonArray }?.asJsonArray
                                ?.mapNotNull { it.takeIf { p -> p.isJsonPrimitive }?.asString } ?: emptyList()
                            list.add(UsbStorageDevice(
                                name = key,
                                label = share?.first?.ifBlank { null } ?: label,
                                vendor = strOf(o, "manufacturer") ?: strOf(o, "vendor") ?: "Generic",
                                model = strOf(o, "product") ?: strOf(o, "model") ?: "",
                                sizeBytes = total,
                                freeBytes = free,
                                filesystem = fstype.ifBlank { "ext4" },
                                mountPoint = if (isSwap) "" else "/tmp/mnt/$label",
                                shareSmb = if (isSwap) false else share?.second ?: false,
                                uuid = uuid,
                                partitionId = partId,
                                formatOptions = fmtOpts
                            ))
                            added = true
                        }
                        return added
                    }
                    var mediaUsed = false
                    mediaRes?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (k, v) ->
                        if (v.isJsonObject) {
                            if (parseMediaDrive(k, v.asJsonObject)) mediaUsed = true
                        }
                    }
                    if (!mediaUsed) {
                    fun parseUsb(key: String, o: com.google.gson.JsonObject) {
                        val partObj = o.get("partition")?.takeIf { it.isJsonObject }?.asJsonObject
                        val firstPart = partObj?.entrySet()?.firstOrNull()?.value?.takeIf { it.isJsonObject }?.asJsonObject
                        val name = key.ifBlank { o.get("name")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "USB Drive" }
                        val label = firstPart?.get("label")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("label")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: name
                        val vendor = o.get("manufacturer")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("vendor")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "Generic"
                        val model = o.get("product")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("model")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                        val size = numLong(o, "size")
                        val free = (firstPart?.let { numLong(it, "free") } ?: 0L).let { if (it > 0) it else size / 2 }
                        val fs = firstPart?.get("fstype")?.takeIf { p -> p.isJsonPrimitive }?.asString
                            ?: o.get("filesystem")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: "ext4"
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
                        res.asJsonArray.forEach { if (it.isJsonObject) parseUsb("", it.asJsonObject) }
                    } else if (res.isJsonObject) {
                        val obj = res.asJsonObject
                        val devEl = obj.get("device")
                        if (devEl != null && devEl.isJsonObject) {
                            devEl.asJsonObject.entrySet().forEach { (k, v) ->
                                if (v.isJsonObject) parseUsb(k, v.asJsonObject)
                            }
                        } else if (obj.has("device") && obj.get("device").isJsonArray) {
                            obj.getAsJsonArray("device").forEach { if (it.isJsonObject) parseUsb("", it.asJsonObject) }
                        } else if (obj.has("media") && obj.get("media").isJsonArray) {
                            obj.getAsJsonArray("media").forEach { if (it.isJsonObject) parseUsb("", it.asJsonObject) }
                        } else {
                            obj.entrySet().forEach { (k, v) ->
                                if (v.isJsonObject) {
                                    val o = v.asJsonObject
                                    if (o.has("partition") || o.has("bus") || o.has("manufacturer")) parseUsb(k, o)
                                    else parseUsb("", o)
                                }
                                else if (v.isJsonArray) v.asJsonArray.forEach { if (it.isJsonObject) parseUsb("", it.asJsonObject) }
                            }
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

    fun openFileBrowser(startPath: String = "") {
        _fileBrowserPath.value = startPath
        browseFiles(startPath)
    }

    fun browseFiles(directory: String) {
        viewModelScope.launch {
            _fileBrowserLoading.value = true
            _fileBrowserError.value = null
            try {
                val resp = repository.executeRci(listOf(mapOf("ls" to mapOf("directory" to directory))))
                val body = resp.body()
                val list = mutableListOf<FileEntry>()
                val lsObj = body?.takeIf { it.isJsonArray }?.asJsonArray?.firstOrNull()
                    ?.takeIf { it.isJsonObject }?.asJsonObject?.get("ls")
                    ?.takeIf { it.isJsonObject }?.asJsonObject
                val rel = lsObj?.get("rel")?.takeIf { it.isJsonPrimitive }?.asString ?: directory
                lsObj?.get("entry")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (name, v) ->
                    if (!v.isJsonObject) return@forEach
                    val o = v.asJsonObject
                    fun s(k: String): String? = o.get(k)?.takeIf { it.isJsonPrimitive }?.asString
                    fun n(k: String): Long = o.get(k)?.takeIf { it.isJsonPrimitive }?.runCatching {
                        try { asLong } catch (_: Exception) { asString.toLongOrNull() ?: 0L }
                    }?.getOrDefault(0L) ?: 0L
                    val type = s("type") ?: ""
                    val childPath = if (rel.isBlank()) name else "$rel/$name"
                    list.add(FileEntry(
                        name = name,
                        fullPath = childPath,
                        isDirectory = type == "D" || type == "V",
                        isVolume = type == "V",
                        sizeBytes = n("size"),
                        label = s("label") ?: "",
                        fstype = s("fstype") ?: "",
                        mounted = (s("mounted") ?: "yes") == "yes",
                        totalBytes = n("total"),
                        freeBytes = n("free")
                    ))
                }
                _fileBrowserPath.value = rel
                _fileBrowserEntries.value = list.sortedWith(
                    compareByDescending<FileEntry> { it.isVolume }
                        .thenByDescending { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
                if (list.isEmpty()) _fileBrowserError.value = "Папка пуста"
            } catch (e: Exception) {
                AppLogger.logError("browseFiles", e)
                _fileBrowserError.value = "Ошибка чтения: ${e.message}"
            } finally {
                _fileBrowserLoading.value = false
            }
        }
    }

    fun fileBrowserUp() {
        val cur = _fileBrowserPath.value
        if (cur.isBlank()) return
        val parent = cur.trimEnd(':').substringBeforeLast("/", "")
        browseFiles(parent)
    }

    private val _fileBrowserMessage = MutableStateFlow<String?>(null)
    val fileBrowserMessage: StateFlow<String?> = _fileBrowserMessage.asStateFlow()

    private val _fileAclList = MutableStateFlow<List<FileAclEntry>>(emptyList())
    val fileAclList: StateFlow<List<FileAclEntry>> = _fileAclList.asStateFlow()

    fun clearFileBrowserMessage() { _fileBrowserMessage.value = null }

    fun deleteFileEntry(path: String) {
        viewModelScope.launch {
            try {
                // Web: eraseApi.perform({filename})
                val resp = repository.executeRci(listOf(mapOf("erase" to mapOf("filename" to path))))
                if (resp.isSuccessful && !repository.isRciError(resp.body())) {
                    _fileBrowserMessage.value = "Удалено"
                    browseFiles(_fileBrowserPath.value)
                } else {
                    _fileBrowserMessage.value = "Не удалось удалить"
                }
            } catch (e: Exception) {
                AppLogger.logError("deleteFileEntry", e)
                _fileBrowserMessage.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun createFolder(parentPath: String, name: String) {
        viewModelScope.launch {
            try {
                // Web: mkdirApi.perform({directory: "<parent>/<name>/"})
                val dir = (if (parentPath.isBlank()) "" else "$parentPath/") + name.trim().trim('/') + "/"
                val resp = repository.executeRci(listOf(mapOf("mkdir" to mapOf("directory" to dir))))
                if (resp.isSuccessful && !repository.isRciError(resp.body())) {
                    _fileBrowserMessage.value = "Папка создана"
                    browseFiles(_fileBrowserPath.value)
                } else {
                    _fileBrowserMessage.value = "Не удалось создать папку"
                }
            } catch (e: Exception) {
                AppLogger.logError("createFolder", e)
                _fileBrowserMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun downloadFileEntry(entry: FileEntry, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Web: GET /download/<uuid>/<path...>  (":/" -> "/")
                val rel = entry.fullPath.split("/").filter { it.isNotBlank() }
                    .joinToString("/").replace(":/", "/")
                val resp = repository.downloadRaw("download/$rel")
                if (!resp.isSuccessful || resp.body == null) {
                    resp.close()
                    _fileBrowserMessage.value = "Не удалось скачать"
                    return@launch
                }
                val resolver = context.contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, entry.name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/Keenetic")
                }
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri == null) {
                    resp.close()
                    _fileBrowserMessage.value = "Нет доступа к загрузкам"
                    return@launch
                }
                try {
                    resp.body!!.byteStream().use { input ->
                        resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                    }
                } finally {
                    resp.close()
                }
                _fileBrowserMessage.value = "Сохранено в Download/Keenetic/${entry.name}"
            } catch (e: Exception) {
                AppLogger.logError("downloadFileEntry", e)
                _fileBrowserMessage.value = "Ошибка скачивания: ${e.message}"
            }
        }
    }

    fun uploadFileEntry(dirPath: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var fname = "upload.bin"
                var fsize = -1L
                var mime = "application/octet-stream"
                resolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            .takeIf { it >= 0 }?.let { fname = c.getString(it) ?: fname }
                        c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            .takeIf { it >= 0 }?.let { fsize = c.getLong(it) }
                    }
                }
                resolver.getType(uri)?.let { mime = it }
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    _fileBrowserMessage.value = "Не удалось прочитать файл"
                    return@launch
                }
                if (bytes.size > 100 * 1024 * 1024) {
                    _fileBrowserMessage.value = "Файл больше 100 МБ"
                    return@launch
                }
                val remotePath = (if (dirPath.isBlank()) "" else "$dirPath/") + fname
                // Web: rci put {filename, size} -> port, then POST multipart {port, Filedata} to /fui
                val port = repository.putAllocatePort(remotePath, bytes.size.toLong())
                if (port.isNullOrBlank()) {
                    _fileBrowserMessage.value = "Роутер отклонил загрузку"
                    return@launch
                }
                val ok = repository.uploadFui(port, fname, mime, bytes)
                _fileBrowserMessage.value = if (ok) "Загружено: $fname" else "Ошибка загрузки"
                if (ok) browseFiles(dirPath)
            } catch (e: Exception) {
                AppLogger.logError("uploadFileEntry", e)
                _fileBrowserMessage.value = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    fun loadFileAcl(path: String) {
        viewModelScope.launch {
            try {
                // Web: showAccessApi.read([{directory: path}, {directory: parent}])
                val parent = path.trimEnd(':').substringBeforeLast("/", "")
                val cmds = if (parent.isBlank() || parent == path) {
                    listOf(mapOf("directory" to path))
                } else {
                    listOf(mapOf("directory" to path), mapOf("directory" to parent))
                }
                val resp = repository.executeRci(listOf(mapOf("show" to mapOf("access" to cmds))))
                val list = mutableListOf<FileAclEntry>()
                val arr = resp.body()?.takeIf { it.isJsonArray }?.asJsonArray
                val users = arr?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("show")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("access")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject
                users?.entrySet()?.forEach { (name, v) ->
                    if (v.isJsonObject) {
                        val o = v.asJsonObject
                        fun s(k: String): String = o.get(k)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                        list.add(FileAclEntry(
                            user = name,
                            assigned = s("assigned"),
                            effective = s("effective"),
                            exists = o.get("exists")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                        ))
                    }
                }
                _fileAclList.value = list.sortedBy { it.user }
                if (list.isEmpty()) _fileBrowserMessage.value = "Нет данных о правах"
            } catch (e: Exception) {
                AppLogger.logError("loadFileAcl", e)
                _fileBrowserMessage.value = "Ошибка чтения прав: ${e.message}"
            }
        }
    }

    fun saveFileAcl(path: String, modes: Map<String, String>) {
        viewModelScope.launch {
            try {
                // Web: accessApi.write([{user, directory, mode}])
                val cmds = modes.map { (user, mode) ->
                    mapOf("user" to user, "directory" to path, "mode" to mode)
                }
                val resp = repository.executeRci(listOf(mapOf("access" to cmds)))
                if (resp.isSuccessful && !repository.isRciError(resp.body())) {
                    _fileBrowserMessage.value = "Права сохранены"
                    loadFileAcl(path)
                } else {
                    _fileBrowserMessage.value = "Не удалось сохранить права"
                }
            } catch (e: Exception) {
                AppLogger.logError("saveFileAcl", e)
                _fileBrowserMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun formatPartition(driveName: String, partitionId: String, fstype: String) {
        viewModelScope.launch {
            try {
                // Web: mediaPartitionFormatApi.perform({name, partition, type})
                val ok = repository.executeRciWithSave(
                    listOf(mapOf("media" to mapOf("partition" to mapOf(
                        "format" to mapOf("name" to driveName, "partition" to partitionId, "type" to fstype)
                    ))))
                )
                _fileBrowserMessage.value = if (ok) "Форматирование запущено" else "Не удалось отформатировать"
                if (ok) loadUsbDevices()
            } catch (e: Exception) {
                AppLogger.logError("formatPartition", e)
                _fileBrowserMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun loadFirmwareStatus() {
        viewModelScope.launch {
            try {
                val verRes = repository.queryShow("version")
                val updateRes = repository.queryShow("system/update/status") ?: repository.queryShow("components")

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
                val res = repository.queryLog()
                if (res != null) {
                    val list = mutableListOf<SystemLogEntry>()
                    val arr = when {
                        res.isJsonArray -> res.asJsonArray
                        res.isJsonObject && res.asJsonObject.has("log") && res.asJsonObject.get("log").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("log")
                        res.isJsonObject && res.asJsonObject.has("entry") && res.asJsonObject.get("entry").isJsonArray ->
                            res.asJsonObject.getAsJsonArray("entry")
                        res.isJsonObject && res.asJsonObject.size() > 0 && !res.asJsonObject.has("log") && !res.asJsonObject.has("entry") -> {
                            // Web configurator: show log returns an object whose values are the log entries.
                            val arrTemp = com.google.gson.JsonArray()
                            for ((_, v) in res.asJsonObject.entrySet()) arrTemp.add(v)
                            arrTemp
                        }
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
                                val msg = o.get("message")
                                val msgText = when {
                                    msg != null && msg.isJsonObject -> {
                                        msg.asJsonObject.get("message")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                            ?: msg.asJsonObject.get("msg")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                                    }
                                    msg != null && msg.isJsonPrimitive -> msg.asString
                                    else -> o.get("msg")?.takeIf { p -> p.isJsonPrimitive }?.asString ?: ""
                                }
                                val msgLevel = msg?.takeIf { it.isJsonObject }
                                    ?.asJsonObject?.get("level")?.takeIf { p -> p.isJsonPrimitive }?.asString
                                val finalLevel = msgLevel ?: level

                                if (msgText.isNotBlank()) {
                                    list.add(SystemLogEntry(time, facility, finalLevel, msgText))
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
                // Web configurator has no show/mobile or show/sim tree: the modem state
                // is read from the interface map (show.interface UsbModem*), so query it directly.
                val mobileRes = repository.queryShow("interface")
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
        viewModelScope.launch {
            try {
                // Web: ejectApi.perform({name}) -> POST system.eject
                val ok = repository.executeRciWithSave(
                    listOf(mapOf("system" to mapOf("eject" to mapOf("name" to name))))
                )
                if (ok) {
                    _usbStorageList.value = _usbStorageList.value.filter { it.name != name }
                    loadUsbDevices()
                }
            } catch (e: Exception) {
                AppLogger.logError("ejectUsbDevice", e)
            }
        }
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
                // Web: serviceApi.write({[name]: bool}) on the service tree (sc/service);
                // same convention as ssh/ftp/telnet/http-proxy/ntp toggles in this codebase.
                val cmd = when (serviceName.lowercase()) {
                    "smb", "cifs" -> mapOf("service" to mapOf("cifs" to enabled))
                    "ftp" -> mapOf("service" to mapOf("ftp" to enabled))
                    "dlna", "media" -> mapOf("service" to mapOf("dlna" to enabled))
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
                // Get the first available modem interface
                val modemInterface = _interfaces.value.firstOrNull { it.type.lowercase() == "modem" || it.type.lowercase() == "usbmodem" }?.id ?: "UsbModem0"
                
                val cmd = mapOf("interface" to mapOf("name" to modemInterface, "modem" to mapOf("mode" to mode.lowercase())))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setModemMode", e)
            }
        }
    }

    fun setMobileActivationType(type: String) {
        viewModelScope.launch {
            try {
                // Web UI: mobile.app.activation_type = always | backup | schedule | disabled
                val cmd = mapOf("mobile" to mapOf("app" to mapOf("activation_type" to type.lowercase())))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setMobileActivationType", e)
            }
        }
    }

    fun sendUssdCommand(command: String) {
        viewModelScope.launch {
            try {
                // Web UI modem diagnostics: mobile.req_diag.chat
                val cmd = mapOf("mobile" to mapOf("req_diag" to mapOf("chat" to command)))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("sendUssdCommand", e)
            }
        }
    }

    fun updateMobileConnectionSettings(
        interfaceId: String? = null,
        apn: String? = null,
        username: String? = null,
        password: String? = null,
        phone: String? = null,
        atCommands: List<String>? = null,
        ttlModification: String? = null,
        ignoreRemoteDns: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val modemInterface = interfaceId
                    ?: _interfaces.value.firstOrNull { it.type.lowercase().contains("modem") }?.id
                    ?: "UsbModem0"
                val mobileMap = linkedMapOf<String, Any>()
                if (apn != null) mobileMap["apn"] = mapOf("apn" to apn, "comment" to "")
                if (username != null) mobileMap["username"] = username
                if (password != null) mobileMap["password"] = password
                if (phone != null) mobileMap["phone"] = phone
                if (atCommands != null) mobileMap["init"] = atCommands
                if (ttlModification != null) mobileMap["ttl-modification"] = ttlModification
                if (ignoreRemoteDns != null) mobileMap["ignore-remote-dns"] = ignoreRemoteDns
                if (mobileMap.isEmpty()) return@launch

                val cmd = mapOf("interface" to mapOf("name" to modemInterface, "mobile" to mobileMap))
                repository.executeRciWithSave(listOf(cmd))
                loadMobileStatus()
            } catch (e: Exception) {
                AppLogger.logError("updateMobileConnectionSettings", e)
            }
        }
    }

    fun updateWispClientSettings(
        id: String,
        ssid: String? = null,
        password: String? = null,
        channel: String? = null,
        bssid: String? = null
    ) {
        viewModelScope.launch {
            try {
                val wirelessMap = linkedMapOf<String, Any>()
                if (ssid != null) wirelessMap["ssid"] = ssid
                if (password != null) wirelessMap["authentication"] = mapOf("wpa-psk" to password)
                if (channel != null) wirelessMap["channel"] = channel
                if (bssid != null) wirelessMap["bssid"] = bssid
                if (wirelessMap.isEmpty()) return@launch

                val cmd = mapOf("interface" to mapOf(id to mapOf("wireless" to wirelessMap)))
                repository.executeRciWithSave(listOf(cmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("updateWispClientSettings", e)
            }
        }
    }

    private val _mobileTraffic = MutableStateFlow(MobileTraffic())
    val mobileTraffic: StateFlow<MobileTraffic> = _mobileTraffic.asStateFlow()

    fun resolveModemInterfaceId(): String? {
        return _interfaces.value.firstOrNull {
            it.type.lowercase().contains("modem")
        }?.id ?: _interfaces.value.firstOrNull {
            it.id.lowercase().contains("mobile") ||
                it.id.lowercase().contains("modem") ||
                it.id.lowercase().contains("lte") ||
                it.id.lowercase().contains("cellular")
        }?.id
    }

    fun loadMobileTraffic(interfaceId: String? = null) {
        viewModelScope.launch {
            try {
                val id = interfaceId ?: resolveModemInterfaceId() ?: return@launch
                val res = repository.queryShow("interface/$id")
                if (res != null) {
                    _mobileTraffic.value = InterfaceMapper.toMobileTraffic(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadMobileTraffic", e)
            }
        }
    }

    fun updateMobileTraffic(
        enable: Boolean,
        limit: Long,
        unit: String,
        dayOfMonth: Int,
        cycleResetEnabled: Boolean,
        threshold: Int,
        smsWarningEnabled: Boolean,
        smsLimitEnabled: Boolean,
        smsPhone: String,
        smsMessage: String,
        disconnect: Boolean
    ) {
        viewModelScope.launch {
            try {
                val modemInterface = resolveModemInterfaceId() ?: return@launch
                val phoneList = listOf(smsPhone.ifBlank { "sms" })
                val actions = mutableListOf<Map<String, Any>>()
                if (smsWarningEnabled) {
                    actions.add(
                        mapOf(
                            "trigger" to "threshold",
                            "sms-alert" to mapOf("message" to smsMessage, "phone" to phoneList)
                        )
                    )
                }
                if (smsLimitEnabled || disconnect) {
                    val limitAction = linkedMapOf<String, Any>(
                        "trigger" to "limit",
                        "sms-alert" to mapOf("message" to smsMessage, "phone" to phoneList)
                    )
                    if (disconnect) limitAction["disconnect"] = true
                    actions.add(limitAction)
                }

                val traffic = linkedMapOf<String, Any>()
                traffic["enable"] = enable
                traffic["limit"] = limit
                traffic["unit"] = unit
                traffic["multiplier"] = unitMultiplier(unit)
                if (cycleResetEnabled) traffic["monthly"] = mapOf("day-of-month" to dayOfMonth)
                traffic["threshold"] = threshold
                if (actions.isNotEmpty()) traffic["action"] = actions

                val cmd = mapOf("interface" to mapOf("name" to modemInterface, "traffic-counter" to traffic))
                repository.executeRciWithSave(listOf(cmd))
                loadMobileTraffic(modemInterface)
            } catch (e: Exception) {
                AppLogger.logError("updateMobileTraffic", e)
            }
        }
    }

private val _intelliQos = MutableStateFlow(IntelliQosConfig())
    val intelliQos: StateFlow<IntelliQosConfig> = _intelliQos.asStateFlow()

    fun loadIntelliQos() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ntce/qos")
                val obj = res?.takeIf { it.isJsonObject }?.asJsonObject ?: return@launch
                val catArr = obj.get("category")?.takeIf { it.isJsonArray }?.asJsonArray
                val categories = catArr?.mapNotNull { el ->
                    if (!el.isJsonObject) return@mapNotNull null
                    val o = el.asJsonObject
                    val id = o.get("category")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(0) ?: 0
                    val pr = o.get("priority")?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrDefault(5) ?: 5
                    IntelliQosCategory(id = id, name = intelliQosCategoryName(id), priority = pr)
                } ?: emptyList()
                val enabled = obj.get("enable")?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrDefault(false) ?: false
                _intelliQos.value = IntelliQosConfig(classifyEnabled = enabled, qosEnabled = enabled, categories = categories)
            } catch (e: Exception) {
                AppLogger.logError("loadIntelliQos", e)
            }
        }
    }

    fun setIntelliQos(enableService: Boolean, enableQos: Boolean) {
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()
                cmds.add(mapOf("ntce" to mapOf("enable" to enableService)))
                cmds.add(mapOf("ntce" to mapOf("qos" to mapOf("enable" to enableQos))))
                repository.executeRciWithSave(cmds)
                loadIntelliQos()
            } catch (e: Exception) {
                AppLogger.logError("setIntelliQos", e)
            }
        }
    }

    fun setIntelliQosPriority(category: Int, priority: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ntce" to mapOf("qos" to mapOf("category" to category, "priority" to priority)))
                repository.executeRciWithSave(listOf(cmd))
                loadIntelliQos()
            } catch (e: Exception) {
                AppLogger.logError("setIntelliQosPriority", e)
            }
        }
    }

    fun setWifiAclMode(interfaceId: String, mode: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf(interfaceId to mapOf("mac-access" to mode)))
                repository.executeRciWithSave(listOf(cmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("setWifiAclMode", e)
            }
        }
    }

    fun renameDevice(mac: String, newName: String) {
        _clients.value = _clients.value.map {
            if (it.mac == mac) it.copy(displayName = newName.ifBlank { it.displayName }) else it
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

                // 2. Parse WifiStation interface (only if it exists)
                val knownInterfaces = _interfaces.value.map { it.id }
                val hasSt0 = knownInterfaces.any { it.contains("WifiStation0") && it.contains("WifiMaster0") }
                val hasSt1 = knownInterfaces.any { it.contains("WifiStation0") && it.contains("WifiMaster1") }
                val st0 = if (hasSt0) repository.queryShow("interface/WifiMaster0/WifiStation0") else null
                val st1 = if (hasSt1) repository.queryShow("interface/WifiMaster1/WifiStation0") else null
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
        val is24G = band.contains("2.4")
        val masterRadio = if (is24G) "WifiMaster0" else "WifiMaster1"
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
        txPowerPercent: Int?,
        security: String? = null,
        bridge: String? = null
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
                val pskModes = listOf("wpa", "wpa2", "wpa2+3", "wpa3")
                if (security != null && security in pskModes && newPassword.isBlank()) {
                    _wifiActionMessage.value = "Выберите тип защиты $label и укажите новый пароль сети"
                    return@launch
                }
                if (security != null) {
                    val encryption = mutableMapOf<String, Any>("enable" to mapOf("no" to (security == "open")))
                    encryption["wpa"] = mapOf("no" to (security != "wpa" && security != "wpa2+3"))
                    val wpa2On = security in listOf("wpa2", "wpa2+3")
                    encryption["wpa2"] = mapOf("no" to !wpa2On)
                    encryption["wpa3"] = mapOf("no" to (security != "wpa3" && security != "wpa2+3"))
                    encryption["owe"] = mapOf("no" to (security != "owe"))
                    val authentication = if (security in pskModes) {
                        mapOf("wpa-psk" to mapOf("no" to false, "psk" to newPassword))
                    } else {
                        mapOf("wpa-psk" to mapOf("no" to true))
                    }
                    val patch = mutableMapOf<String, Any>("name" to apName)
                    patch["encryption"] = encryption
                    patch["authentication"] = authentication
                    commands.add(mapOf("interface" to patch))
                }
                if (newPassword.isNotBlank() && security == null) {
                    commands.add(mapOf("interface" to mapOf("name" to apName, "wpa-psk" to newPassword)))
                }
                if (bridge != null && bridge != "keep") {
                    commands.add(mapOf("interface" to mapOf("name" to apName, "bridge" to bridge)))
                }
                if (channel != null && channel > 0) {
                    commands.add(mapOf("interface" to mapOf("name" to masterRadio, "channel" to channel)))
                }
                if (txPowerPercent != null) {
                    commands.add(mapOf("interface" to mapOf("name" to masterRadio, "power" to txPowerPercent)))
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

    fun updateWifiInterface(name: String, vararg patches: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val commands = patches.map { patch ->
                    val fields = HashMap<String, Any>()
                    fields["name"] = name
                    fields.putAll(patch)
                    mapOf("interface" to fields)
                }
                if (commands.isEmpty()) return@launch
                val success = repository.executeRciWithSave(commands)
                _wifiActionMessage.value = if (success) "Параметры Wi-Fi ${name} применены" else "Параметры Wi-Fi применяются; обновите список"
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("updateWifiInterface", e)
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
        _portForwardingRules.value =
            if (_portForwardingRules.value.any { it.id == rule.id }) {
                _portForwardingRules.value.map { if (it.id == rule.id) rule else it }
            } else {
                _portForwardingRules.value + rule
            }
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
        _firewallRules.value =
            if (_firewallRules.value.any { it.id == rule.id }) {
                _firewallRules.value.map { if (it.id == rule.id) rule else it }
            } else {
                _firewallRules.value + rule
            }
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

    fun loadDnsFilters() {
        viewModelScope.launch {
            _dnsFilterPresets.value = ApiCallState.Loading
            _dnsFilterProfiles.value = ApiCallState.Loading
            try {
                // Component presence check (mirrors sc/cloud detection) - avoids 404 noise
                // when the Internet Filter component is not installed on the router.
                val component = repository.queryShow("sc/dns-proxy/filter")
                if (component == null) {
                    _dnsFilterInstalled.value = false
                    _dnsFilterPresetList.value = emptyList()
                    _dnsFilterProfileList.value = emptyList()
                    _dnsFilterPresets.value = ApiCallState.Error("Компонент не установлен")
                    _dnsFilterProfiles.value = ApiCallState.Error("Компонент не установлен")
                    return@launch
                }
                _dnsFilterInstalled.value = true

                // Live presets + configured profiles (same RCI paths as web morda).
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("show" to mapOf("dns-proxy" to mapOf("filter" to mapOf("presets" to emptyMap<String, Any>())))),
                        mapOf("show" to mapOf("sc" to mapOf("dns-proxy" to mapOf("filter" to mapOf("profile" to emptyMap<String, Any>())))))
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isJsonArray == true) {
                        val arr = body.asJsonArray
                        val item0 = if (arr.size() > 0) arr.get(0) else null
                        val item1 = if (arr.size() > 1) arr.get(1) else null
                        val p0 = item0?.asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("dns-proxy")?.getAsJsonObject("filter")?.get("presets")
                        val p1 = item1?.asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("sc")?.getAsJsonObject("dns-proxy")?.getAsJsonObject("filter")?.get("profile")
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

    fun refreshNetworkHint() {
        viewModelScope.launch(Dispatchers.IO) {
            val gateway = NetworkUtils.detectRouterGatewayIp(KeeneticApp.instance)
            val suggested = NetworkUtils.getSuggestedRouterIps(KeeneticApp.instance)
            _networkHint.value = NetworkHint(
                gateway = gateway,
                currentIp = gateway,
                suggestedRouterIps = suggested
            )
        }
    }

    fun saveConnectionSettings(ip: String, login: String, password: String?, autoLogin: Boolean) {
        _routerIp.value = ip
        _routerLogin.value = login
        _autoLoginEnabled.value = autoLogin
        _savedIp.value = ip
        _savedUsername.value = login
        viewModelScope.launch {
            dataStore.saveSettings(ip, _savedPort.value, login, autoLogin)
            if (!password.isNullOrBlank()) {
                encryptedStorage.savePassword(password)
            }
        }
    }

    fun executeSsh(command: String, port: Int = 22, login: String? = null, password: String? = null) {
        viewModelScope.launch {
            _sshOutput.value = ""
            try {
                val user = login ?: _routerLogin.value
                val pass = password ?: encryptedStorage.getPassword() ?: ""
                val host = _routerIp.value.ifBlank { _savedIp.value }
                val ssh = com.keenetic.local.api.KeeneticSshClient(
                    host = host, port = port, login = user, password = pass
                )
                val result = ssh.execute(command)
                _sshOutput.value = result.getOrElse { it.message ?: "Ошибка" }
            } catch (e: Exception) {
                _sshOutput.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun setTorrentSettings(directory: String, rpcPort: Int, rpcPublic: Boolean, peerPort: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf(
                    "torrent" to mapOf(
                        "client" to mapOf(
                            "rpc-port" to rpcPort,
                            "rpc-public" to rpcPublic,
                            "peer-port" to peerPort,
                            "download-dir" to directory
                        )
                    )
                )
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setTorrentSettings", e)
            }
        }
    }

    fun saveService(service: SavedService) {
        _savedServices.value = _savedServices.value.filter { it.host != service.host || it.port != service.port } + service
    }

    fun deleteService(service: SavedService) {
        _savedServices.value = _savedServices.value.filter { it.host != service.host || it.port != service.port }
    }

    fun loadNameServers() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/name-server")
                if (res != null) {
                    _nameServers.value = DnsAndScheduleParser.parseNameServers(res)
                }
                val scRes = repository.querySc("ip", "name-server")
                if (scRes != null) {
                    _scNameServers.value = DnsAndScheduleParser.parseScNameServers(scRes)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNameServers", e)
            }
        }
    }

    fun loadDohUpstream() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("dns-proxy" to mapOf("https" to mapOf("upstream" to emptyMap<String, Any>()))))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val upstream = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) {
                        body.asJsonArray[0].asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("sc")
                            ?.getAsJsonObject("dns-proxy")?.getAsJsonObject("https")?.get("upstream")
                    } else body
                    _dohServers.value = DnsAndScheduleParser.parseDohServers(upstream)
                    _dohUpstream.value = DnsAndScheduleParser.parseDohUpstream(upstream)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDohUpstream", e)
            }
        }
    }

    fun loadDotUpstream() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("dns-proxy" to mapOf("tls" to mapOf("upstream" to emptyMap<String, Any>()))))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val upstream = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) {
                        body.asJsonArray[0].asJsonObject?.getAsJsonObject("show")?.getAsJsonObject("sc")
                            ?.getAsJsonObject("dns-proxy")?.getAsJsonObject("tls")?.get("upstream")
                    } else body
                    _dotServers.value = DnsAndScheduleParser.parseDotServers(upstream)
                    _dotUpstream.value = DnsAndScheduleParser.parseDotUpstream(upstream)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDotUpstream", e)
            }
        }
    }

    /** Полная перезапись списка DoH-серверов (как это делает веб-конфигуратор). */
    fun setDohUpstreams(servers: List<DohServerInfo>) {
        viewModelScope.launch {
            try {
                val list = servers.map { s ->
                    mutableMapOf<String, Any>("url" to s.url).apply {
                        if (!s.interfaceName.isNullOrBlank()) put("interface", s.interfaceName!!)
                    }
                }
                val cmd = mapOf("dns-proxy" to mapOf("https" to mapOf("upstream" to list)))
                repository.executeRciWithSave(listOf(cmd))
                loadDohUpstream()
            } catch (e: Exception) {
                AppLogger.logError("setDohUpstreams", e)
            }
        }
    }

    /** Полная перезапись списка DoT-серверов. */
    fun setDotUpstreams(servers: List<DotServerInfo>) {
        viewModelScope.launch {
            try {
                val list = servers.map { s ->
                    mutableMapOf<String, Any>().apply {
                        s.address?.let { put("address", it) }
                        s.fqdn?.let { put("fqdn", it) }
                        if (!s.interfaceName.isNullOrBlank()) put("interface", s.interfaceName!!)
                    }
                }
                val cmd = mapOf("dns-proxy" to mapOf("tls" to mapOf("upstream" to list)))
                repository.executeRciWithSave(listOf(cmd))
                loadDotUpstream()
            } catch (e: Exception) {
                AppLogger.logError("setDotUpstreams", e)
            }
        }
    }

    fun addDohServer(url: String, interfaceName: String? = null) {
        setDohUpstreams(_dohServers.value.filter { it.url != url } + DohServerInfo(url = url, interfaceName = interfaceName))
    }

    fun removeDohServer(url: String, interfaceName: String? = null) {
        setDohUpstreams(
            _dohServers.value.filter {
                if (interfaceName.isNullOrBlank()) it.url != url
                else it.url != url || it.interfaceName != interfaceName
            }
        )
    }

    fun addDotServer(address: String, fqdn: String? = null, interfaceName: String? = null) {
        val existing = _dotServers.value.filter {
            it.address != address && (fqdn.isNullOrBlank() || it.fqdn != fqdn)
        }
        setDotUpstreams(existing + DotServerInfo(address = address, fqdn = fqdn, interfaceName = interfaceName))
    }

    fun removeDotServer(address: String, interfaceName: String? = null) {
        setDotUpstreams(
            _dotServers.value.filter {
                if (interfaceName.isNullOrBlank()) it.address != address
                else it.address != address || it.interfaceName != interfaceName
            }
        )
    }

    /** Ручные plain DNS (ip name-server). Добавление/удаление записей списка. */
    fun setPlainDnsServers(servers: List<DnsServerInfo>) {
        viewModelScope.launch {
            try {
                val list = servers.map { s ->
                    mutableMapOf<String, Any>("address" to (s.address ?: "")).apply {
                        s.interfaceName?.let { put("interface", it) }
                    }
                }
                val cmd = mapOf("ip" to mapOf("name-server" to list))
                repository.executeRciWithSave(listOf(cmd))
                loadNameServers()
            } catch (e: Exception) {
                AppLogger.logError("setPlainDnsServers", e)
            }
        }
    }

    fun addPlainDnsServer(address: String, interfaceName: String) {
        val existing = _scNameServers.value
        if (existing.any { it.address == address && it.interfaceName == interfaceName }) return
        setPlainDnsServers(existing + DnsServerInfo(address = address, interfaceName = interfaceName))
    }

    fun removePlainDnsServer(address: String, interfaceName: String) {
        setPlainDnsServers(
            _scNameServers.value.filter { it.address != address || it.interfaceName != interfaceName }
        )
    }

    fun loadDnsIntercept() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("dns-proxy" to mapOf("intercept" to emptyMap<String, Any>()))))))
                if (response.isSuccessful) {
                    var enable: Boolean? = null
                    val body = response.body()
                    if (body?.isJsonArray == true && body.asJsonArray.size() > 0) {
                        val o = body.asJsonArray[0].asJsonObject
                            ?.getAsJsonObject("show")?.getAsJsonObject("sc")?.getAsJsonObject("dns-proxy")
                            ?.getAsJsonObject("intercept")
                        enable = o?.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean
                    } else if (body?.isJsonObject == true) {
                        enable = body.asJsonObject.get("enable")?.takeIf { it.isJsonPrimitive }?.asBoolean
                    }
                    _dnsInterceptEnabled.value = enable
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDnsIntercept", e)
            }
        }
    }

    fun setDnsIntercept(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("dns-proxy" to mapOf("intercept" to mapOf("enable" to enabled)))
                repository.executeRciWithSave(listOf(cmd))
                _dnsInterceptEnabled.value = enabled
            } catch (e: Exception) {
                AppLogger.logError("setDnsIntercept", e)
            }
        }
    }

    fun setCustomDoh(url: String, interfaceName: String? = null) {
        addDohServer(url, interfaceName)
    }

    fun loadAutoUpdateStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("system/update/auto")
                if (res != null && res.isJsonObject) {
                    _autoUpdateEnabled.value = res.asJsonObject.get("enable")
                        ?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
                } else {
                    _autoUpdateEnabled.value = false
                }
            } catch (e: Exception) {
                _autoUpdateEnabled.value = false
            }
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        _autoUpdateEnabled.value = enabled
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("update" to mapOf("auto" to mapOf("enable" to enabled))))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("setAutoUpdate", e)
            }
        }
    }

    fun loadSystemUpdateStatus() {
        viewModelScope.launch {
            _systemUpdateStatusRaw.value = ApiCallState.Loading
            try {
                val res = repository.queryShow("system/update/status")
                _systemUpdateStatusRaw.value = if (res != null) ApiCallState.Success(res) else ApiCallState.Error("Нет данных")
            } catch (e: Exception) {
                _systemUpdateStatusRaw.value = ApiCallState.Error(e.message ?: "Ошибка")
            }
        }
    }

    // ===== Фаза 3: Новые модели и методы =====

    private val _environmentInfo = MutableStateFlow(EnvironmentInfo())
    val environmentInfo: StateFlow<EnvironmentInfo> = _environmentInfo.asStateFlow()

    private val _productInfo = MutableStateFlow(ProductInfo())
    val productInfo: StateFlow<ProductInfo> = _productInfo.asStateFlow()

    private val _ntpStatus = MutableStateFlow(NtpStatus())
    val ntpStatus: StateFlow<NtpStatus> = _ntpStatus.asStateFlow()

    private val _backupStatus = MutableStateFlow(BackupStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _ledConfig = MutableStateFlow(LedConfig())
    val ledConfig: StateFlow<LedConfig> = _ledConfig.asStateFlow()

    private val _systemMode = MutableStateFlow(SystemMode())
    val systemMode: StateFlow<SystemMode> = _systemMode.asStateFlow()

    private val _trafficCounters = MutableStateFlow<List<TrafficCounter>>(emptyList())
    val trafficCounters: StateFlow<List<TrafficCounter>> = _trafficCounters.asStateFlow()

    private val _channelUtilization = MutableStateFlow<List<ChannelUtilization>>(emptyList())
    val channelUtilization: StateFlow<List<ChannelUtilization>> = _channelUtilization.asStateFlow()

    private val _spectrumData = MutableStateFlow<List<SpectrumChannel>>(emptyList())
    val spectrumData: StateFlow<List<SpectrumChannel>> = _spectrumData.asStateFlow()

    private val _cableDiagnostics = MutableStateFlow<List<CableDiagnosticResult>>(emptyList())
    val cableDiagnostics: StateFlow<List<CableDiagnosticResult>> = _cableDiagnostics.asStateFlow()

    private val _wpsStatus = MutableStateFlow(WpsStatus())
    val wpsStatus: StateFlow<WpsStatus> = _wpsStatus.asStateFlow()

    private val _mwsStatus = MutableStateFlow(MwsStatus())
    val mwsStatus: StateFlow<MwsStatus> = _mwsStatus.asStateFlow()

    private val _mwsMembers = MutableStateFlow<List<MwsMember>>(emptyList())
    val mwsMembers: StateFlow<List<MwsMember>> = _mwsMembers.asStateFlow()

    private val _internetDetailed = MutableStateFlow(InternetDetailedStatus())
    val internetDetailed: StateFlow<InternetDetailedStatus> = _internetDetailed.asStateFlow()

    private val _conntrackEntries = MutableStateFlow<List<ConntrackEntry>>(emptyList())
    val conntrackEntries: StateFlow<List<ConntrackEntry>> = _conntrackEntries.asStateFlow()

    private val _natEntries = MutableStateFlow<List<NatEntry>>(emptyList())
    val natEntries: StateFlow<List<NatEntry>> = _natEntries.asStateFlow()

    private val _arpEntries = MutableStateFlow<List<ArpEntry>>(emptyList())
    val arpEntries: StateFlow<List<ArpEntry>> = _arpEntries.asStateFlow()

    private val _neighbourEntries = MutableStateFlow<List<NeighbourEntry>>(emptyList())
    val neighbourEntries: StateFlow<List<NeighbourEntry>> = _neighbourEntries.asStateFlow()

    private val _ipRules = MutableStateFlow<List<IpRule>>(emptyList())
    val ipRules: StateFlow<List<IpRule>> = _ipRules.asStateFlow()

    private val _pptpServer = MutableStateFlow(PptpServer())
    val pptpServer: StateFlow<PptpServer> = _pptpServer.asStateFlow()

    private val _ocServer = MutableStateFlow(OcServer())
    val ocServer: StateFlow<OcServer> = _ocServer.asStateFlow()

    private val _wireguardServer = MutableStateFlow(WireguardServerStatus())
    val wireguardServer: StateFlow<WireguardServerStatus> = _wireguardServer.asStateFlow()

    private val _l2tpServer = MutableStateFlow(L2tpServer())
    val l2tpServer: StateFlow<L2tpServer> = _l2tpServer.asStateFlow()

    private val _ikev2Server = MutableStateFlow(Ikev2Server())
    val ikev2Server: StateFlow<Ikev2Server> = _ikev2Server.asStateFlow()

    private val _sstpServer = MutableStateFlow(SstpServerFull())
    val sstpServer: StateFlow<SstpServerFull> = _sstpServer.asStateFlow()

    private val _ipsecStatus = MutableStateFlow(IpsecStatus())
    val ipsecStatus: StateFlow<IpsecStatus> = _ipsecStatus.asStateFlow()

    private val _ntceApps = MutableStateFlow<List<NtceApp>>(emptyList())
    val ntceApps: StateFlow<List<NtceApp>> = _ntceApps.asStateFlow()

    private val _ntceHosts = MutableStateFlow<List<NtceHost>>(emptyList())
    val ntceHosts: StateFlow<List<NtceHost>> = _ntceHosts.asStateFlow()

    private val _ntceOses = MutableStateFlow<List<NtceOs>>(emptyList())
    val ntceOses: StateFlow<List<NtceOs>> = _ntceOses.asStateFlow()

    private val _ntceGroups = MutableStateFlow<List<NtceGroup>>(emptyList())
    val ntceGroups: StateFlow<List<NtceGroup>> = _ntceGroups.asStateFlow()

    private val _dyndnsStatus = MutableStateFlow(DyndnsStatus())
    val dyndnsStatus: StateFlow<DyndnsStatus> = _dyndnsStatus.asStateFlow()

    private val _dyndnsProfiles = MutableStateFlow<List<DyndnsProfile>>(emptyList())
    val dyndnsProfiles: StateFlow<List<DyndnsProfile>> = _dyndnsProfiles.asStateFlow()

    private val _dyndnsUpdaters = MutableStateFlow<List<DyndnsUpdater>>(emptyList())
    val dyndnsUpdaters: StateFlow<List<DyndnsUpdater>> = _dyndnsUpdaters.asStateFlow()

    private val _upnpRedirects = MutableStateFlow<List<UpnpRedirect>>(emptyList())
    val upnpRedirects: StateFlow<List<UpnpRedirect>> = _upnpRedirects.asStateFlow()

    private val _upnpPinholes = MutableStateFlow<List<UpnpPinhole>>(emptyList())
    val upnpPinholes: StateFlow<List<UpnpPinhole>> = _upnpPinholes.asStateFlow()

    private val _torrentStatusFull = MutableStateFlow(TorrentStatusFull())
    val torrentStatusFull: StateFlow<TorrentStatusFull> = _torrentStatusFull.asStateFlow()

    private val _torrentLocalAccount = MutableStateFlow(TorrentLocalAccount())
    val torrentLocalAccount: StateFlow<TorrentLocalAccount> = _torrentLocalAccount.asStateFlow()

    private val _cloudStatus = MutableStateFlow(CloudStatus())
    val cloudStatus: StateFlow<CloudStatus> = _cloudStatus.asStateFlow()

    private val _cloudNdmp = MutableStateFlow(CloudNdmp())
    val cloudNdmp: StateFlow<CloudNdmp> = _cloudNdmp.asStateFlow()

    private val _cloudInstalled = MutableStateFlow(true)
    val cloudInstalled: StateFlow<Boolean> = _cloudInstalled.asStateFlow()

    private val _sshSettings = MutableStateFlow(SshSettings())
    val sshSettings: StateFlow<SshSettings> = _sshSettings.asStateFlow()

    private val _sshFingerprint = MutableStateFlow(SshFingerprint())
    val sshFingerprint: StateFlow<SshFingerprint> = _sshFingerprint.asStateFlow()

    private val _snmpView = MutableStateFlow(SnmpView())
    val snmpView: StateFlow<SnmpView> = _snmpView.asStateFlow()

    private val _ftpSettings = MutableStateFlow(FtpSettings())
    val ftpSettings: StateFlow<FtpSettings> = _ftpSettings.asStateFlow()

    private val _telnetSettings = MutableStateFlow(TelnetSettings())
    val telnetSettings: StateFlow<TelnetSettings> = _telnetSettings.asStateFlow()

    private val _httpProxySettings = MutableStateFlow(HttpProxySettings())
    val httpProxySettings: StateFlow<HttpProxySettings> = _httpProxySettings.asStateFlow()

    private val _ipv6Addresses = MutableStateFlow<List<Ipv6Address>>(emptyList())
    val ipv6Addresses: StateFlow<List<Ipv6Address>> = _ipv6Addresses.asStateFlow()

    private val _ipv6Prefixes = MutableStateFlow<List<Ipv6Prefix>>(emptyList())
    val ipv6Prefixes: StateFlow<List<Ipv6Prefix>> = _ipv6Prefixes.asStateFlow()

    private val _ipv6Routes = MutableStateFlow<List<Ipv6Route>>(emptyList())
    val ipv6Routes: StateFlow<List<Ipv6Route>> = _ipv6Routes.asStateFlow()

    private val _ipv6Subnets = MutableStateFlow<List<Ipv6Subnet>>(emptyList())
    val ipv6Subnets: StateFlow<List<Ipv6Subnet>> = _ipv6Subnets.asStateFlow()

    private val _ipv6DhcpBindings = MutableStateFlow<List<Ipv6DhcpBinding>>(emptyList())
    val ipv6DhcpBindings: StateFlow<List<Ipv6DhcpBinding>> = _ipv6DhcpBindings.asStateFlow()

    private val _mediaStorageList = MutableStateFlow<List<MediaStorage>>(emptyList())
    val mediaStorageList: StateFlow<List<MediaStorage>> = _mediaStorageList.asStateFlow()

    private val _componentList = MutableStateFlow<List<ComponentInfo>>(emptyList())
    val componentList: StateFlow<List<ComponentInfo>> = _componentList.asStateFlow()

    private val _deviceListFull = MutableStateFlow<List<DeviceListEntryFull>>(emptyList())
    val deviceListFull: StateFlow<List<DeviceListEntryFull>> = _deviceListFull.asStateFlow()

    private val _objectGroups = MutableStateFlow<List<ObjectGroupFqdn>>(emptyList())
    val objectGroups: StateFlow<List<ObjectGroupFqdn>> = _objectGroups.asStateFlow()

    private val _monitorStatus = MutableStateFlow(MonitorStatus())
    val monitorStatus: StateFlow<MonitorStatus> = _monitorStatus.asStateFlow()

    private val _fingerprintRaw = MutableStateFlow<ApiCallState>(ApiCallState.Loading)
    val fingerprintRaw: StateFlow<ApiCallState> = _fingerprintRaw.asStateFlow()

    // ===== Load methods =====

    fun loadEnvironmentInfo() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("environment")
                if (res != null) {
                    _environmentInfo.value = SystemDetailParser.parseEnvironment(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadEnvironmentInfo", e)
            }
        }
    }

    fun loadProductInfo() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("product")
                if (res != null) {
                    _productInfo.value = SystemDetailParser.parseProduct(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadProductInfo", e)
            }
        }
    }

    fun loadNtpStatus() {
        viewModelScope.launch {
            try {
                val flags = repository.querySc("service")?.let { SystemDetailParser.parseServiceFlags(it) }
                val res = repository.queryShow("ntp")
                val parsed = if (res != null) SystemDetailParser.parseNtp(res) else NtpStatus()
                _ntpStatus.value = if (flags != null) parsed.copy(enabled = flags.ntp) else parsed
            } catch (e: Exception) {
                AppLogger.logError("loadNtpStatus", e)
            }
        }
    }

    fun loadBackupStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("system/backup")
                if (res != null) {
                    _backupStatus.value = SystemDetailParser.parseBackup(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadBackupStatus", e)
            }
        }
    }

    fun loadLedConfig() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("led")
                if (res != null) {
                    _ledConfig.value = SystemDetailParser.parseLed(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadLedConfig", e)
            }
        }
    }

    fun loadSystemMode() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("system/mode")
                if (res != null) {
                    _systemMode.value = SystemDetailParser.parseMode(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSystemMode", e)
            }
        }
    }

    fun loadTrafficCounters() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface/traffic-counter")
                if (res != null) {
                    _trafficCounters.value = InterfaceDetailParser.parseTrafficCounters(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadTrafficCounters", e)
            }
        }
    }

    fun loadChannelUtilization() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface/channel-utilization")
                if (res != null) {
                    _channelUtilization.value = InterfaceDetailParser.parseChannelUtilization(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadChannelUtilization", e)
            }
        }
    }

    fun loadSpectrum() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface/spectrum")
                if (res != null) {
                    _spectrumData.value = InterfaceDetailParser.parseSpectrum(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSpectrum", e)
            }
        }
    }

    fun loadCableDiagnostics() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface/cable-diagnostics")
                if (res != null) {
                    _cableDiagnostics.value = InterfaceDetailParser.parseCableDiagnostics(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadCableDiagnostics", e)
            }
        }
    }

    fun loadWpsStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("interface/wps")
                if (res != null) {
                    _wpsStatus.value = InterfaceDetailParser.parseWps(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadWpsStatus", e)
            }
        }
    }

    fun loadMwsStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("mws")
                if (res != null) {
                    _mwsStatus.value = InterfaceDetailParser.parseMws(res)
                    _mwsMembers.value = InterfaceDetailParser.parseMwsMembers(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadMwsStatus", e)
            }
        }
    }

    fun loadInternetDetailed() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("internet")
                if (res != null) {
                    _internetDetailed.value = InterfaceDetailParser.parseInternetDetailed(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadInternetDetailed", e)
            }
        }
    }

    fun loadConntrack() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/conntrack")
                if (res != null) {
                    _conntrackEntries.value = ConntrackParser.parseConntrack(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadConntrack", e)
            }
        }
    }

    fun loadNatEntries() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/nat")
                if (res != null) {
                    _natEntries.value = ConntrackParser.parseNat(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNatEntries", e)
            }
        }
    }

    fun loadArpEntries() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/arp")
                if (res != null) {
                    _arpEntries.value = ConntrackParser.parseArp(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadArpEntries", e)
            }
        }
    }

    fun loadNeighbourEntries() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/neighbour")
                if (res != null) {
                    _neighbourEntries.value = ConntrackParser.parseNeighbours(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNeighbourEntries", e)
            }
        }
    }

    fun loadIpRules() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ip/rules")
                if (res != null) {
                    _ipRules.value = ConntrackParser.parseIpRules(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpRules", e)
            }
        }
    }

    fun loadPptpServer() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/vpn-server")
                if (res != null) {
                    _pptpServer.value = VpnDetailParser.parsePptpServer(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadPptpServer", e)
            }
        }
    }

    fun loadOcServer() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/oc-server")
                if (res != null) {
                    _ocServer.value = VpnDetailParser.parseOcServer(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadOcServer", e)
            }
        }
    }

    fun loadWireguardServer() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/vpn-server")
                if (res != null) {
                    _wireguardServer.value = VpnDetailParser.parseWireguardServer(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadWireguardServer", e)
            }
        }
    }

    fun loadL2tpServer() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/l2tp-server")
                if (res != null) {
                    _l2tpServer.value = VpnDetailParser.parseL2tpServer(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadL2tpServer", e)
            }
        }
    }

    fun loadIkev2Server() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/ikev2-server")
                if (res != null) {
                    _ikev2Server.value = VpnDetailParser.parseIkev2Server(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIkev2Server", e)
            }
        }
    }

    fun loadSstpServer() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("sc/sstp-server")
                if (res != null) {
                    _sstpServer.value = VpnDetailParser.parseSstpServer(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSstpServer", e)
            }
        }
    }

    fun loadIpsecStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipsec")
                if (res != null) {
                    _ipsecStatus.value = VpnDetailParser.parseIpsec(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpsecStatus", e)
            }
        }
    }

    suspend fun saveVpnServerConfig(writePath: String, config: Map<String, Any>): Boolean {
        return try {
            val cmd = mapOf(writePath to mapOf("config" to config))
            repository.executeRciWithSave(listOf(cmd))
        } catch (e: Exception) {
            AppLogger.logError("saveVpnServerConfig($writePath)", e)
            false
        }
    }

    fun loadNtceApps() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ntce/applications")
                if (res != null) {
                    _ntceApps.value = NtceDetailParser.parseApplications(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNtceApps", e)
            }
        }
    }

    fun loadNtceHosts() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ntce/hosts")
                if (res != null) {
                    _ntceHosts.value = NtceDetailParser.parseHosts(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNtceHosts", e)
            }
        }
    }

    fun loadNtceOses() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ntce/oses")
                if (res != null) {
                    _ntceOses.value = NtceDetailParser.parseOses(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNtceOses", e)
            }
        }
    }

    fun loadNtceGroups() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ntce/groups")
                if (res != null) {
                    _ntceGroups.value = NtceDetailParser.parseGroups(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadNtceGroups", e)
            }
        }
    }

    fun loadDyndnsStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("dyndns")
                if (res != null) {
                    _dyndnsStatus.value = DyndnsParser.parseStatus(res)
                    _dyndnsProfiles.value = DyndnsParser.parseProfiles(res)
                    _dyndnsUpdaters.value = DyndnsParser.parseUpdaters(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDyndnsStatus", e)
            }
        }
    }

    fun loadUpnpStatus() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("show" to mapOf("upnp" to mapOf("redirect" to emptyMap<String, Any>()))),
                        mapOf("show" to mapOf("upnp" to mapOf("pinhole" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isJsonArray == true) {
                        val arr = body.asJsonArray
                        val item0 = if (arr.size() > 0) arr.get(0) else null
                        val item1 = if (arr.size() > 1) arr.get(1) else null
                        _upnpRedirects.value = UpnpParser.parseRedirects(item0)
                        _upnpPinholes.value = UpnpParser.parsePinholes(item1)
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("loadUpnpStatus", e)
            }
        }
    }

    fun loadTorrentStatusFull() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("torrent")
                if (res != null) {
                    _torrentStatusFull.value = TorrentDetailParser.parseStatus(res)
                    _torrentLocalAccount.value = TorrentDetailParser.parseLocalAccount(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadTorrentStatusFull", e)
            }
        }
    }

    fun loadCloudStatus() {
        viewModelScope.launch {
            try {
                val scRes = repository.queryShow("sc/cloud")
                if (scRes == null) {
                    _cloudInstalled.value = false
                    _cloudStatus.value = CloudStatus()
                    _cloudNdmp.value = CloudNdmp()
                    return@launch
                }
                _cloudInstalled.value = true
                val res = repository.queryShow("cloud")
                if (res != null) {
                    _cloudStatus.value = CloudParser.parseStatus(res)
                    _cloudNdmp.value = CloudParser.parseNdmp(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadCloudStatus", e)
            }
        }
    }

    fun loadSshSettings() {
        viewModelScope.launch {
            try {
                val flags = repository.querySc("service")?.let { SystemDetailParser.parseServiceFlags(it) }
                val res = repository.queryShow("ssh")
                val parsed = if (res != null) SshSnmpParser.parseSsh(res) else SshSettings()
                _sshSettings.value = if (flags != null) parsed.copy(enabled = flags.ssh) else parsed
                if (res != null) {
                    _sshFingerprint.value = SshSnmpParser.parseSshFingerprint(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSshSettings", e)
            }
        }
    }

    fun loadSnmpSettings() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("snmp")
                if (res != null) {
                    _snmpView.value = SshSnmpParser.parseSnmp(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadSnmpSettings", e)
            }
        }
    }

    fun loadFtpSettings() {
        viewModelScope.launch {
            try {
                val flags = repository.querySc("service")?.let { SystemDetailParser.parseServiceFlags(it) }
                val res = repository.queryShow("ftp")
                val parsed = if (res != null) SshSnmpParser.parseFtp(res) else FtpSettings()
                _ftpSettings.value = if (flags != null) parsed.copy(enabled = flags.ftp) else parsed
            } catch (e: Exception) {
                AppLogger.logError("loadFtpSettings", e)
            }
        }
    }

    fun loadTelnetSettings() {
        viewModelScope.launch {
            try {
                val flags = repository.querySc("service")?.let { SystemDetailParser.parseServiceFlags(it) }
                _telnetSettings.value = if (flags != null) {
                    TelnetSettings(enabled = flags.telnet, port = 23)
                } else {
                    SshSnmpParser.parseTelnet(repository.queryShow("telnet"))
                }
            } catch (e: Exception) {
                AppLogger.logError("loadTelnetSettings", e)
            }
        }
    }

    fun loadHttpProxySettings() {
        viewModelScope.launch {
            try {
                val flags = repository.querySc("service")?.let { SystemDetailParser.parseServiceFlags(it) }
                _httpProxySettings.value = if (flags != null) {
                    HttpProxySettings(enabled = flags.httpProxy, port = 3128)
                } else {
                    SshSnmpParser.parseHttpProxy(repository.queryShow("http-proxy"))
                }
            } catch (e: Exception) {
                AppLogger.logError("loadHttpProxySettings", e)
            }
        }
    }

    fun loadIpv6Addresses() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipv6/addresses")
                if (res != null) {
                    _ipv6Addresses.value = Ipv6DetailParser.parseAddresses(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6Addresses", e)
            }
        }
    }

    fun loadIpv6Prefixes() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipv6/prefix")
                if (res != null) {
                    _ipv6Prefixes.value = Ipv6DetailParser.parsePrefixes(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6Prefixes", e)
            }
        }
    }

    fun loadIpv6Routes() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipv6/route")
                if (res != null) {
                    _ipv6Routes.value = Ipv6DetailParser.parseRoutes(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6Routes", e)
            }
        }
    }

    fun loadIpv6Subnets() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipv6/subnets")
                if (res != null) {
                    _ipv6Subnets.value = Ipv6DetailParser.parseSubnets(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6Subnets", e)
            }
        }
    }

    fun loadIpv6DhcpBindings() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("ipv6/dhcp/bindings")
                if (res != null) {
                    _ipv6DhcpBindings.value = Ipv6DetailParser.parseDhcpBindings(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadIpv6DhcpBindings", e)
            }
        }
    }

    fun loadMediaStorage() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("media")
                if (res != null) {
                    _mediaStorageList.value = DeviceListParser.parseMedia(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadMediaStorage", e)
            }
        }
    }

    fun loadComponents() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("components/list")
                if (res != null) {
                    _componentList.value = DeviceListParser.parseComponents(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadComponents", e)
            }
        }
    }

    fun loadDeviceListFull() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("devices")
                if (res != null) {
                    _deviceListFull.value = DeviceListParser.parseFull(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadDeviceListFull", e)
            }
        }
    }

    fun loadObjectGroups() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("object-group/fqdn")
                if (res != null) {
                    _objectGroups.value = DeviceListParser.parseObjectGroup(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadObjectGroups", e)
            }
        }
    }

    fun loadMonitorStatus() {
        viewModelScope.launch {
            try {
                val res = repository.queryShow("monitor")
                if (res != null) {
                    _monitorStatus.value = DeviceListParser.parseMonitor(res)
                }
            } catch (e: Exception) {
                AppLogger.logError("loadMonitorStatus", e)
            }
        }
    }

    fun loadSshFingerprint() {
        viewModelScope.launch {
            _fingerprintRaw.value = ApiCallState.Loading
            try {
                val res = repository.queryShow("ssh/fingerprint")
                _fingerprintRaw.value = if (res != null) ApiCallState.Success(res) else ApiCallState.Error("Нет данных")
            } catch (e: Exception) {
                _fingerprintRaw.value = ApiCallState.Error(e.message ?: "Ошибка")
            }
        }
    }

    // ===== Write methods =====

    fun setHostname(hostname: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("name" to hostname))
                repository.executeRciWithSave(listOf(cmd))
                loadSystemInfo()
            } catch (e: Exception) {
                AppLogger.logError("setHostname", e)
            }
        }
    }

    fun setTimezone(timezone: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("clock" to mapOf("time-zone" to timezone)))
                repository.executeRciWithSave(listOf(cmd))
                loadNtpStatus()
            } catch (e: Exception) {
                AppLogger.logError("setTimezone", e)
            }
        }
    }

    fun setNtpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("service" to mapOf("ntp" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadNtpStatus()
            } catch (e: Exception) {
                AppLogger.logError("setNtpEnabled", e)
            }
        }
    }

    fun setNtpServer(server: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ntp" to mapOf("server" to server.trim()))
                repository.executeRciWithSave(listOf(cmd))
                loadNtpStatus()
            } catch (e: Exception) {
                AppLogger.logError("setNtpServer", e)
            }
        }
    }

    fun setLedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("led" to mapOf("enable" to enabled)))
                repository.executeRciWithSave(listOf(cmd))
                loadLedConfig()
            } catch (e: Exception) {
                AppLogger.logError("setLedEnabled", e)
            }
        }
    }

    fun setLedMode(mode: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("led" to mapOf("mode" to mode)))
                repository.executeRciWithSave(listOf(cmd))
                loadLedConfig()
            } catch (e: Exception) {
                AppLogger.logError("setLedMode", e)
            }
        }
    }

    fun installComponent(name: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("components" to mapOf("component" to listOf(mapOf("name" to name, "install" to true))))
                repository.executeRciWithSave(listOf(cmd))
                loadComponents()
            } catch (e: Exception) {
                AppLogger.logError("installComponent", e)
            }
        }
    }

    fun removeComponent(name: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("components" to mapOf("component" to listOf(mapOf("name" to name, "uninstall" to true))))
                repository.executeRciWithSave(listOf(cmd))
                loadComponents()
            } catch (e: Exception) {
                AppLogger.logError("removeComponent", e)
            }
        }
    }

    fun saveInterfacePriorities(orders: Map<String, Int>) {
        viewModelScope.launch {
            try {
                val cmds = orders.map { (id, order) ->
                    mapOf("interface" to mapOf(id to mapOf("order" to order)))
                }
                if (cmds.isNotEmpty()) {
                    repository.executeRciWithSave(cmds)
                    loadInterfaces()
                }
            } catch (e: Exception) {
                AppLogger.logError("saveInterfacePriorities", e)
            }
        }
    }

    fun setInterfaceUp(id: String, up: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("interface" to mapOf(id to mapOf("up" to up)))
                repository.executeRciWithSave(listOf(cmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("setInterfaceUp", e)
            }
        }
    }

    fun updateInterfaceIpConfig(id: String, useDhcp: Boolean, ip: String?, mask: String?, gateway: String?) {
        viewModelScope.launch {
            try {
                val cmd = if (useDhcp) {
                    mapOf("interface" to mapOf(id to mapOf("ip" to mapOf("address" to "dhcp"))))
                } else {
                    val ipMap = mutableMapOf<String, Any>()
                    if (!ip.isNullOrBlank()) ipMap["address"] = ip
                    if (!mask.isNullOrBlank()) ipMap["mask"] = mask
                    if (!gateway.isNullOrBlank()) ipMap["gateway"] = gateway
                    mapOf("interface" to mapOf(id to mapOf("ip" to ipMap)))
                }
                repository.executeRciWithSave(listOf(cmd))
                loadInterfaces()
            } catch (e: Exception) {
                AppLogger.logError("updateInterfaceIpConfig", e)
            }
        }
    }

    fun updateWiredConnectionSettings(
        id: String,
        description: String? = null,
        mtu: String? = null,
        hostname: String? = null,
        order: String? = null,
        schedule: String? = null,
        macMode: String? = null,
        macAddress: String? = null,
        pppoeIdentity: String? = null,
        pppoePassword: String? = null,
        pppoeService: String? = null,
        pppoeAuth: String? = null
    ) {
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()

                val ifaceMap = linkedMapOf<String, Any>()
                if (description != null) ifaceMap["description"] = description
                if (mtu != null) ifaceMap["mtu"] = mtu
                if (hostname != null) ifaceMap["hostname"] = hostname
                if (order != null) ifaceMap["order"] = order.toIntOrNull() ?: 0
                if (schedule != null) ifaceMap["schedule"] = schedule
                if (macMode != null) ifaceMap["mac-config-mode"] = macMode
                if (macAddress != null) ifaceMap["mac"] = macAddress
                if (ifaceMap.isNotEmpty()) {
                    cmds.add(mapOf("interface" to mapOf(id to ifaceMap)))
                }

                val pppoeMap = linkedMapOf<String, Any>()
                if (pppoeIdentity != null) pppoeMap["identity"] = pppoeIdentity
                if (pppoePassword != null) pppoeMap["password"] = pppoePassword
                if (pppoeService != null) pppoeMap["service"] = pppoeService
                if (pppoeAuth != null) pppoeMap["type"] = pppoeAuth
                if (pppoeMap.isNotEmpty()) {
                    cmds.add(mapOf("interface" to mapOf(id to mapOf("pppoe" to pppoeMap))))
                }

                if (cmds.isNotEmpty()) {
                    repository.executeRciWithSave(cmds)
                    loadInterfaces()
                }
            } catch (e: Exception) {
                AppLogger.logError("updateWiredConnectionSettings", e)
            }
        }
    }

    fun setSshPort(port: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ssh" to mapOf("port" to port))
                repository.executeRciWithSave(listOf(cmd))
                loadSshSettings()
            } catch (e: Exception) {
                AppLogger.logError("setSshPort", e)
            }
        }
    }

    fun setSshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("service" to mapOf("ssh" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadSshSettings()
            } catch (e: Exception) {
                AppLogger.logError("setSshEnabled", e)
            }
        }
    }

    fun setFtpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("service" to mapOf("ftp" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadFtpSettings()
            } catch (e: Exception) {
                AppLogger.logError("setFtpEnabled", e)
            }
        }
    }

    fun setFtpPort(port: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ftp" to mapOf("port" to port))
                repository.executeRciWithSave(listOf(cmd))
                loadFtpSettings()
            } catch (e: Exception) {
                AppLogger.logError("setFtpPort", e)
            }
        }
    }

    fun setFtpAnonymousAccess(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ftp" to mapOf("allow-anonymous" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadFtpSettings()
            } catch (e: Exception) {
                AppLogger.logError("setFtpAnonymousAccess", e)
            }
        }
    }

    fun setTelnetPort(port: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("telnet" to mapOf("port" to port))
                repository.executeRciWithSave(listOf(cmd))
                loadTelnetSettings()
            } catch (e: Exception) {
                AppLogger.logError("setTelnetPort", e)
            }
        }
    }

    fun setHttpProxyPort(port: Int) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("http-proxy" to mapOf("port" to port))
                repository.executeRciWithSave(listOf(cmd))
                loadHttpProxySettings()
            } catch (e: Exception) {
                AppLogger.logError("setHttpProxyPort", e)
            }
        }
    }

    fun setTelnetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("service" to mapOf("telnet" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadTelnetSettings()
            } catch (e: Exception) {
                AppLogger.logError("setTelnetEnabled", e)
            }
        }
    }

    fun setHttpProxyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("service" to mapOf("http-proxy" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadHttpProxySettings()
            } catch (e: Exception) {
                AppLogger.logError("setHttpProxyEnabled", e)
            }
        }
    }

    fun setSnmpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("snmp" to mapOf("enable" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadSnmpSettings()
            } catch (e: Exception) {
                AppLogger.logError("setSnmpEnabled", e)
            }
        }
    }

    fun setSnmpCommunity(community: String) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("snmp" to mapOf("community" to community))
                repository.executeRciWithSave(listOf(cmd))
                loadSnmpSettings()
            } catch (e: Exception) {
                AppLogger.logError("setSnmpCommunity", e)
            }
        }
    }

    fun setUpnpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("upnp" to mapOf("enable" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadUpnpStatus()
            } catch (e: Exception) {
                AppLogger.logError("setUpnpEnabled", e)
            }
        }
    }

    fun setDyndnsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("dyndns" to mapOf("enable" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadDyndnsStatus()
            } catch (e: Exception) {
                AppLogger.logError("setDyndnsEnabled", e)
            }
        }
    }

    fun saveDyndnsProfile(
        provider: String,
        domain: String,
        username: String,
        password: String,
        url: String,
        autoDetectIp: Boolean,
        interfaces: List<String>
    ) {
        viewModelScope.launch {
            try {
                val cmds = mutableListOf<Map<String, Any>>()
                val profile = linkedMapOf<String, Any>(
                    "name" to "_WEBADMIN",
                    "type" to provider,
                    "domain" to domain,
                    "username" to username,
                    "password" to password,
                    "send-address" to !autoDetectIp
                )
                if (url.isNotBlank()) profile["url"] = url
                cmds.add(mapOf("dyndns" to mapOf("profile" to profile)))

                // Bind selected interfaces to the DDNS profile (same as web morda).
                val profileName = "_WEBADMIN"
                interfaces.forEach { iface ->
                    cmds.add(mapOf("interface" to mapOf("name" to iface, "dyndns" to listOf(profileName))))
                }
                repository.executeRciWithSave(cmds)
                loadDyndnsStatus()
            } catch (e: Exception) {
                AppLogger.logError("saveDyndnsProfile", e)
            }
        }
    }

    fun deleteDyndnsProfile() {
        viewModelScope.launch {
            try {
                val cmd = mapOf("dyndns" to mapOf("profile" to mapOf("name" to "_WEBADMIN", "delete" to true)))
                repository.executeRciWithSave(listOf(cmd))
                loadDyndnsStatus()
            } catch (e: Exception) {
                AppLogger.logError("deleteDyndnsProfile", e)
            }
        }
    }

    fun setWpsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Get the first available AccessPoint interface
                val apInterface = _interfaces.value.firstOrNull { it.type.lowercase() == "accesspoint" }?.id ?: "WifiMaster0/AccessPoint0"
                
                val cmd = mapOf("interface" to mapOf("name" to apInterface, "wps" to mapOf("enable" to enabled)))
                repository.executeRciWithSave(listOf(cmd))
                loadWpsStatus()
            } catch (e: Exception) {
                AppLogger.logError("setWpsEnabled", e)
            }
        }
    }

    fun setMwsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("wifi" to mapOf("mws" to mapOf("enable" to enabled)))
                repository.executeRciWithSave(listOf(cmd))
                loadMwsStatus()
            } catch (e: Exception) {
                AppLogger.logError("setMwsEnabled", e)
            }
        }
    }

    fun setIpv6Enabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = mapOf("ipv6" to mapOf("enable" to enabled))
                repository.executeRciWithSave(listOf(cmd))
                loadIpv6Addresses()
            } catch (e: Exception) {
                AppLogger.logError("setIpv6Enabled", e)
            }
        }
    }

    fun rebootRouter() {
        viewModelScope.launch {
            try {
                val cmd = mapOf("system" to mapOf("reboot" to true))
                repository.executeRciWithSave(listOf(cmd))
            } catch (e: Exception) {
                AppLogger.logError("rebootRouter", e)
            }
        }
    }
}
