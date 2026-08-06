package com.keenetic.local.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keenetic.local.api.*
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.data.SavedService
import com.keenetic.local.discovery.AutoDiscovery
import com.keenetic.local.util.AppLogger
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NetworkHint(
    val currentIp: String? = null,
    val gateway: String? = null,
    val suggestedRouterIps: List<String> = emptyList()
)

class RouterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RouterRepository(application)
    private val dataStore = DataStoreManager(application)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // true пока идёт проверка автовхода при старте - экран логина ждёт этот флаг,
    // чтобы не мигнуть формой входа, если сессию можно восстановить автоматически.
    private val _isCheckingAutoLogin = MutableStateFlow(true)
    val isCheckingAutoLogin: StateFlow<Boolean> = _isCheckingAutoLogin.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    private val _interfaces = MutableStateFlow<List<InterfaceInfo>>(emptyList())
    val interfaceStats = MutableStateFlow<Map<String, InterfaceStat>>(emptyMap())
    val interfaces: StateFlow<List<InterfaceInfo>> = _interfaces.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetwork>> = _wifiNetworks.asStateFlow()

    private val _associations = MutableStateFlow<List<WifiAssoc>>(emptyList())
    val associations: StateFlow<List<WifiAssoc>> = _associations.asStateFlow()

    private val _deviceList = MutableStateFlow<List<DeviceListEntry>>(emptyList())
    val deviceList: StateFlow<List<DeviceListEntry>> = _deviceList.asStateFlow()

    private val _wans = MutableStateFlow<WansResponse?>(null)
    val wans: StateFlow<WansResponse?> = _wans.asStateFlow()

    private val _ipPolicies = MutableStateFlow<List<IpPolicy>>(emptyList())
    val ipPolicies: StateFlow<List<IpPolicy>> = _ipPolicies.asStateFlow()

    private val _dhcpBindings = MutableStateFlow<List<DhcpBinding>>(emptyList())
    val dhcpBindings: StateFlow<List<DhcpBinding>> = _dhcpBindings.asStateFlow()

    // Сырой JSON правил "ip static" (переадресация портов). Схема ответа
    // (массив/объект-словарь) ещё не подтверждена HAR - см. ROADMAP.md,
    // раздел "Задача 1: паритет с веб-мордой". Пока храним как есть и
    // показываем в UI построчно вместо строгого парсинга под неизвестную
    // схему (по опыту - угадывание схемы уже дважды приводило к
    // "Expected BEGIN_ARRAY but was BEGIN_OBJECT").
    private val _portForwardingRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val portForwardingRaw: StateFlow<com.google.gson.JsonElement?> = _portForwardingRaw.asStateFlow()

    private val _dnsFilterPresets = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val dnsFilterPresets: StateFlow<com.google.gson.JsonElement?> = _dnsFilterPresets.asStateFlow()

    private val _dnsFilterProfiles = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val dnsFilterProfiles: StateFlow<com.google.gson.JsonElement?> = _dnsFilterProfiles.asStateFlow()

    private val _vpnServerRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val vpnServerRaw: StateFlow<com.google.gson.JsonElement?> = _vpnServerRaw.asStateFlow()

    private val _ipRouteRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val ipRouteRaw: StateFlow<com.google.gson.JsonElement?> = _ipRouteRaw.asStateFlow()

    private val _mobileRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val mobileRaw: StateFlow<com.google.gson.JsonElement?> = _mobileRaw.asStateFlow()

    private val _simRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val simRaw: StateFlow<com.google.gson.JsonElement?> = _simRaw.asStateFlow()

    private val _ipNeighbourRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val ipNeighbourRaw: StateFlow<com.google.gson.JsonElement?> = _ipNeighbourRaw.asStateFlow()

    private val _usersRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val usersRaw: StateFlow<com.google.gson.JsonElement?> = _usersRaw.asStateFlow()

    private val _systemUpdateStatusRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val systemUpdateStatusRaw: StateFlow<com.google.gson.JsonElement?> = _systemUpdateStatusRaw.asStateFlow()

    private val _dhcpPoolRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val dhcpPoolRaw: StateFlow<com.google.gson.JsonElement?> = _dhcpPoolRaw.asStateFlow()

    private val _upnpRedirectRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val upnpRedirectRaw: StateFlow<com.google.gson.JsonElement?> = _upnpRedirectRaw.asStateFlow()

    private val _internetStatusRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val internetStatusRaw: StateFlow<com.google.gson.JsonElement?> = _internetStatusRaw.asStateFlow()

    private val _ntceSummaryRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val ntceSummaryRaw: StateFlow<com.google.gson.JsonElement?> = _ntceSummaryRaw.asStateFlow()

    private val _ntceStatusRaw = MutableStateFlow<com.google.gson.JsonElement?>(null)
    val ntceStatusRaw: StateFlow<com.google.gson.JsonElement?> = _ntceStatusRaw.asStateFlow()

    private val _sshOutput = MutableStateFlow("")
    val sshOutput: StateFlow<String> = _sshOutput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasSavedPassword = MutableStateFlow(false)
    val hasSavedPassword: StateFlow<Boolean> = _hasSavedPassword.asStateFlow()

    private val _discoveredRouters = MutableStateFlow<List<AutoDiscovery.DiscoveredRouter>>(emptyList())
    val discoveredRouters: StateFlow<List<AutoDiscovery.DiscoveredRouter>> = _discoveredRouters.asStateFlow()

    private val _networkHint = MutableStateFlow(NetworkHint())
    val networkHint: StateFlow<NetworkHint> = _networkHint.asStateFlow()

    private val _savedServices = MutableStateFlow<List<SavedService>>(emptyList())
    val savedServices: StateFlow<List<SavedService>> = _savedServices.asStateFlow()

    val routerIp: StateFlow<String> = dataStore.routerIp.stateIn(viewModelScope, SharingStarted.Lazily, "192.168.1.1")
    val routerLogin: StateFlow<String> = dataStore.routerLogin.stateIn(viewModelScope, SharingStarted.Lazily, "admin")
    val autoLoginEnabled: StateFlow<Boolean> = dataStore.autoLogin.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        refreshNetworkHint()
        viewModelScope.launch {
            dataStore.webServices.collect { services ->
                _savedServices.value = services
            }
        }
        viewModelScope.launch {
            _hasSavedPassword.value = repository.hasSavedCredentials()
            val autoLogin = dataStore.autoLogin.first()
            val hasSaved = repository.hasSavedCredentials()
            if (autoLogin && hasSaved) {
                val result = repository.tryAutoLogin()
                if (result == "OK" || result.startsWith("OK")) {
                    _isLoggedIn.value = true
                    refreshAll()
                }
            }
            _isCheckingAutoLogin.value = false
        }
    }

    fun refreshNetworkHint() {
        viewModelScope.launch {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return@launch
            val activeNetwork = connectivityManager.activeNetwork ?: return@launch
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

            val currentIp = linkProperties?.linkAddresses
                ?.asSequence()
                ?.mapNotNull { it.address?.hostAddress }
                ?.filter { it.contains(".") && !it.startsWith("127.") && !it.startsWith("169.254.") }
                ?.firstOrNull()

            val gateway = linkProperties?.routes
                ?.asSequence()
                ?.filter { it.isDefaultRoute }
                ?.mapNotNull { it.gateway?.hostAddress }
                ?.firstOrNull()

            val candidates = mutableListOf<String>()
            gateway?.let(candidates::add)
            currentIp?.let(candidates::add)

            if (currentIp != null) {
                val parts = currentIp.split(".")
                if (parts.size == 4) {
                    val prefix = parts.take(3).joinToString(".")
                    candidates.add("$prefix.1")
                    candidates.add("$prefix.254")
                    candidates.add("$prefix.100")
                }
            }

            if (gateway != null) {
                val prefix = gateway.substringBeforeLast(".")
                candidates.add("$prefix.1")
                candidates.add("$prefix.254")
                candidates.add("$prefix.100")
            }

            candidates.addAll(listOf("192.168.1.1", "192.168.0.1", "10.0.0.1", "10.0.1.1", "router.local", "keenetic.local"))
            val suggested = candidates.distinct().filter { it.isNotBlank() }

            _networkHint.value = NetworkHint(
                currentIp = currentIp,
                gateway = gateway,
                suggestedRouterIps = suggested
            )
        }
    }

    fun saveService(service: SavedService) {
        viewModelScope.launch {
            val normalized = service.copy(
                name = service.name.trim(),
                host = service.host.trim(),
                port = service.port.trim().ifBlank { "80" },
                username = service.username.trim(),
                password = service.password.trim()
            )
            val updated = listOf(normalized) + _savedServices.value.filterNot {
                it.host.equals(normalized.host, ignoreCase = true) && it.port == normalized.port
            }
            _savedServices.value = updated
            dataStore.saveWebServices(updated)
        }
    }

    fun deleteService(service: SavedService) {
        viewModelScope.launch {
            val updated = _savedServices.value.filterNot { it.host == service.host && it.port == service.port && it.name == service.name }
            _savedServices.value = updated
            dataStore.saveWebServices(updated)
        }
    }

    fun login(password: String, ip: String = "192.168.1.1", login: String = "admin", rememberMe: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                AppLogger.logAction("Manual login requested", "ip=$ip login=$login")
                dataStore.saveRouterIp(ip)
                dataStore.saveRouterLogin(login)

                val effectivePassword = if (password.isBlank()) {
                    repository.readSavedPassword()
                } else {
                    password
                }

                if (effectivePassword.isNullOrBlank()) {
                    _error.value = "Введите пароль или сначала выполните вход с паролем"
                    _isLoggedIn.value = false
                    return@launch
                }

                val result = repository.login(effectivePassword)
                if (result == "OK" || result.startsWith("OK")) {
                    _hasSavedPassword.value = true
                    AppLogger.logAction("Login success", "rememberMe=$rememberMe")
                    dataStore.setAutoLogin(rememberMe)
                    _isLoggedIn.value = true
                    refreshAll()
                } else {
                    AppLogger.w("Login failed: $result")
                    _error.value = result
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                AppLogger.e("Login exception", throwable = e)
                _error.value = "Ошибка авторизации: ${e.message}"
                _isLoggedIn.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAll() {
        loadSystemInfo()
        loadClients()
        loadInterfaces()
        loadAssociations()
        loadIpPolicies()
        loadDhcpBindings()
        loadDeviceList()
        loadWans()
    }

    fun loadSystemInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction("Refresh system info")
                val response = repository.getRestApi().getSystem()
                if (response.isSuccessful) {
                    _systemInfo.value = response.body()
                } else {
                    _error.value = "HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Refresh clients")
                val response = repository.getRestApi().getClients()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _clients.value = body.host
                    } else {
                        val raw = response.errorBody()?.string()
                        if (!raw.isNullOrBlank()) {
                            val json = JsonParser.parseString(raw)
                            if (json.isJsonArray) {
                                val type = object : TypeToken<List<Client>>() {}.type
                                _clients.value = Gson().fromJson(json, type)
                            } else {
                                AppLogger.w("Clients payload is not an array: $raw")
                                _clients.value = emptyList()
                            }
                        } else {
                            _clients.value = emptyList()
                        }
                    }
                } else {
                    _clients.value = emptyList()
                    _error.value = "Ошибка загрузки устройств: ${response.code()}"
                }
            } catch (e: Exception) {
                AppLogger.w("Failed to parse clients response", throwable = e)
                _clients.value = emptyList()
                _error.value = "Ошибка загрузки устройств: ${e.message}"
            }
        }
    }

    /**
     * Активные Wi-Fi подключения с трафиком (для графика на Dashboard).
     * Формат ответа подтверждён официальной документацией Keenetic (RCI
     * reference, "show associations"), но не HAR-дампом с конкретного
     * роутера - парсинг защитный, на случай если реальная форма ответа
     * (массив vs объект по ключу интерфейса) отличается.
     */
    fun loadAssociations() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getAssociations()
                if (response.isSuccessful) {
                    _associations.value = com.keenetic.local.api.AssociationsParser.parse(response.body())
                }
                // Намеренно не показываем _error здесь: это дополнительная,
                // не критичная для работы приложения информация.
            } catch (e: Exception) {
                AppLogger.logAction("Associations load failed", e.message ?: "")
            }
        }
    }

    /**
     * Список имён политик маршрутизации (ip policy ...) для выпадающего
     * списка при назначении политики устройству. Эндпоинт НЕ подтверждён
     * HAR-дампом - если роутер ответит не 200 или неожиданным форматом,
     * список останется пустым и экран покажет ручной ввод как фолбэк.
     */
    fun loadIpPolicies() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getIpPoliciesRaw()
                if (response.isSuccessful) {
                    _ipPolicies.value = com.keenetic.local.api.AssociationsParser.parsePolicyNames(response.body())
                }
            } catch (e: Exception) {
                AppLogger.logAction("IP policies load failed", e.message ?: "")
            }
        }
    }

    /**
     * Статические DHCP-резервации, включая офлайн-устройства (которых нет
     * в списке текущих подключений). Эндпоинт подтверждён сторонним
     * open-source проектом (keenetic-monitor), не первичным HAR - если
     * формат не совпадёт, список останется пустым без падения приложения.
     */
    fun loadDhcpBindings() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getDhcpBindings()
                if (response.isSuccessful) {
                    _dhcpBindings.value = com.keenetic.local.api.AssociationsParser.parseDhcpBindings(response.body())
                }
            } catch (e: Exception) {
                AppLogger.logAction("DHCP bindings load failed", e.message ?: "")
            }
        }
    }

    /**
     * Загружает правила переадресации портов (ip.static). Читаем как сырой
     * JSON - схема (массив/объект) не подтверждена, строгий парсер писать
     * рано. Если понадобится точный список, попроси прислать результат
     * команды `show ip static` из Терминала - по нему построим парсер, как
     * уже делали для wans/associations.
     */
    fun loadPortForwardingRules() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getIpStaticRaw()
                if (response.isSuccessful) {
                    _portForwardingRaw.value = response.body()
                } else {
                    AppLogger.logAction("Port forwarding load failed", "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                AppLogger.logAction("Port forwarding load failed", e.message ?: "")
            }
        }
    }

    /**
     * Загружает пресеты и профили DNS-фильтрации (dns-proxy.filter.*).
     * Только чтение - привязка профиля к клиенту/сети (dns-proxy.filter.assign)
     * не подключена, set-команда не подтверждена HAR.
     */
    /**
     * Пробует прочитать статус VPN-сервера (L2TP/IKEv2) через REST /rci/.
     * Ранее команда `show vpn-server` через SSH возвращала пустой ответ -
     * но это отдельный механизм (CLI shell), REST-путь show/vpn-server
     * подтверждён строкой в JS-бандле веб-морды и может вести себя иначе.
     * Если и тут пусто/ошибка - VPN-сервер на этом роутере, вероятно,
     * действительно не настроен или endpoint не поддерживается прошивкой.
     */
    fun loadVpnServerStatus() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getVpnServerRaw()
                if (response.isSuccessful) {
                    _vpnServerRaw.value = response.body()
                } else {
                    AppLogger.logAction("VPN server status load failed", "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                AppLogger.logAction("VPN server status load failed", e.message ?: "")
            }
        }
    }

    /**
     * Общий helper для простых read-only GET-запросов, которые возвращают
     * сырой JSON и пока не нуждаются в строгом парсере (см. соответствующие
     * комментарии в KeeneticRestApi.kt про то, какой show-путь чем
     * подтверждён). Не глотает ошибки молча - пишет в AppLogger, чтобы
     * было видно в Терминале/логах, что конкретно не удалось загрузить.
     */
    private fun loadRawInto(
        label: String,
        target: MutableStateFlow<com.google.gson.JsonElement?>,
        call: suspend () -> retrofit2.Response<com.google.gson.JsonElement>
    ) {
        viewModelScope.launch {
            try {
                val response = call()
                if (response.isSuccessful) {
                    target.value = response.body()
                } else {
                    AppLogger.logAction("$label load failed", "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                AppLogger.logAction("$label load failed", e.message ?: "")
            }
        }
    }

    fun loadStaticRoutes() = loadRawInto("Static routes", _ipRouteRaw) { repository.getRestApi().getIpRouteRaw() }

    fun loadMobileStatus() {
        loadRawInto("Mobile status", _mobileRaw) { repository.getRestApi().getMobileRaw() }
        loadRawInto("SIM status", _simRaw) { repository.getRestApi().getSimRaw() }
    }

    fun loadIpNeighbours() = loadRawInto("IP neighbours", _ipNeighbourRaw) { repository.getRestApi().getIpNeighbourRaw() }

    fun loadUsers() = loadRawInto("Users", _usersRaw) { repository.getRestApi().getUsersRaw() }

    fun loadSystemUpdateStatus() = loadRawInto("System update status", _systemUpdateStatusRaw) { repository.getRestApi().getSystemUpdateStatusRaw() }

    fun loadDhcpPool() = loadRawInto("DHCP pool", _dhcpPoolRaw) { repository.getRestApi().getDhcpPoolRaw() }

    fun loadUpnpRedirect() = loadRawInto("UPnP redirect", _upnpRedirectRaw) { repository.getRestApi().getUpnpRedirectRaw() }

    fun loadInternetStatus() = loadRawInto("Internet status", _internetStatusRaw) { repository.getRestApi().getInternetStatusRaw() }

    fun loadIntelliQos() {
        loadRawInto("IntelliQoS summary", _ntceSummaryRaw) { repository.getRestApi().getNtceSummaryRaw() }
        loadRawInto("IntelliQoS status", _ntceStatusRaw) { repository.getRestApi().getNtceStatusRaw() }
    }

    fun loadDnsFilters() {
        viewModelScope.launch {
            try {
                val presets = repository.getRestApi().getDnsFilterPresets()
                if (presets.isSuccessful) _dnsFilterPresets.value = presets.body()
            } catch (e: Exception) {
                AppLogger.logAction("DNS filter presets load failed", e.message ?: "")
            }
            try {
                val profiles = repository.getRestApi().getDnsFilterProfiles()
                if (profiles.isSuccessful) _dnsFilterProfiles.value = profiles.body()
            } catch (e: Exception) {
                AppLogger.logAction("DNS filter profiles load failed", e.message ?: "")
            }
        }
    }

    fun loadInterfaces() {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Refresh interfaces")
                val response = repository.getRestApi().getInterfacesRaw()
                if (response.isSuccessful) {
                    val raw = response.body() ?: emptyMap()
                    _interfaces.value = InterfaceMapper.toInterfaceList(raw)
                    _wifiNetworks.value = InterfaceMapper.toWifiNetworks(raw)
                    loadInterfaceStats()
                } else {
                    _error.value = "HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки интерфейсов: ${e.message}"
            }
        }
    }

    /**
     * Живая скорость (байт/сек) для WAN и VPN/Proxy интерфейсов через
     * `show interface <id> stat` по SSH. Подтверждено реальным выводом
     * с роутера (rxspeed/txspeed - актуальная скорость на момент запроса).
     * REST-эндпоинта с этими данными нет, поэтому используем уже
     * подключённый SSH-клиент, как в "Терминале".
     */
    fun loadInterfaceStats() {
        viewModelScope.launch {
            val candidates = _interfaces.value.filter {
                it.type in setOf("Proxy", "Wireguard") || !it.address.isNullOrBlank()
            }
            if (candidates.isEmpty()) return@launch

            val ssh = try {
                repository.getSshClient()
            } catch (e: Exception) {
                AppLogger.logAction("Interface stats: no SSH client", e.message ?: "")
                return@launch
            }

            val results = mutableMapOf<String, InterfaceStat>()
            candidates.forEach { iface ->
                try {
                    ssh.execute("show interface ${iface.id} stat").onSuccess { raw ->
                        InterfaceStatParser.parse(raw)?.let { results[iface.id] = it }
                    }
                } catch (e: Exception) {
                    AppLogger.logAction("Interface stat failed", "${iface.id}: ${e.message}")
                }
            }
            interfaceStats.value = results
        }
    }

    /**
     * Полный список устройств (включая оффлайн) с реальным трафиком rx/tx
     * на устройство. Подтверждено реальным выводом `show device-list` по
     * SSH (не JSON, а текст с отступами - парсится DeviceListParser).
     * Гораздо полнее, чем /rci/show/ip/hotspot (которым пользуемся в
     * getClients/loadClients): даёт офлайн-устройства без отдельного
     * запроса DHCP-резерваций, реальный трафик, политику и приоритет прямо
     * на хосте.
     */
    fun loadDeviceList() {
        viewModelScope.launch {
            try {
                val ssh = repository.getSshClient()
                ssh.execute("show device-list")
                    .onSuccess { raw ->
                        val parsed = DeviceListParser.parse(raw)
                        _deviceList.value = parsed
                        if (parsed.isEmpty() && raw.isNotBlank()) {
                            AppLogger.logAction("Device list parsed empty", raw.take(300))
                        }
                    }
                    .onFailure { e ->
                        AppLogger.logAction("Device list SSH command failed", e.message ?: "")
                        _error.value = "Не удалось получить список устройств по SSH: ${e.message}"
                    }
            } catch (e: Exception) {
                AppLogger.logAction("Device list load failed", e.message ?: "")
                _error.value = "Не удалось получить список устройств по SSH: ${e.message}"
            }
        }
    }

    /**
     * Официальный список WAN-подключений (основной + резервные) через
     * `show wans` по SSH - подтверждено реальным выводом. Точнее, чем наша
     * прежняя эвристика поиска WAN по наличию address у интерфейса.
     */
    fun loadWans() {
        viewModelScope.launch {
            try {
                val ssh = repository.getSshClient()
                ssh.execute("show wans").onSuccess { raw ->
                    _wans.value = WansParser.parse(raw)
                }
            } catch (e: Exception) {
                AppLogger.logAction("WANs load failed", e.message ?: "")
            }
        }
    }

    /**
     * Создаёт именованное расписание (родительский контроль/ограничение
     * доступа по времени). Формат подтверждён реальным HAR:
     *   {"schedule":{"description":..,"action":[{"action":"start"|"stop","hour":..,"min":..,"dow":..}],"name":..}}
     * dow (день недели) - число 0-6, где 0 = воскресенье (стандарт cron-like
     * для Keenetic - не проверено на 100%, но обычная конвенция).
     * Одна пара start/stop на каждый выбранный день недели - расписание
     * "действует" в промежутке [start, stop) в указанные дни.
     */
    private val _schedules = MutableStateFlow<List<ScheduleInfo>>(emptyList())
    val schedules: StateFlow<List<ScheduleInfo>> = _schedules.asStateFlow()

    private val _nameServers = MutableStateFlow<List<DnsServerInfo>>(emptyList())
    val nameServers: StateFlow<List<DnsServerInfo>> = _nameServers.asStateFlow()

    private val _dohUpstream = MutableStateFlow<List<String>>(emptyList())
    val dohUpstream: StateFlow<List<String>> = _dohUpstream.asStateFlow()

    fun loadDohUpstream() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("dns-proxy" to mapOf("https" to mapOf("upstream" to emptyMap<String, Any>()))))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) body.asJsonArray[0] else body
                    _dohUpstream.value = DnsAndScheduleParser.parseDohUpstream(first)
                }
            } catch (e: Exception) {
                AppLogger.logAction("DoH upstream load failed", e.message ?: "")
            }
        }
    }

    /** Список уже созданных расписаний - подтверждено HAR (show sc schedule). */
    fun loadSchedules() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("schedule" to emptyMap<String, Any>()))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) body.asJsonArray[0] else body
                    _schedules.value = DnsAndScheduleParser.parseSchedules(first)
                }
            } catch (e: Exception) {
                AppLogger.logAction("Schedules load failed", e.message ?: "")
            }
        }
    }

    /**
     * Текущие DNS-серверы - подтверждено HAR (show sc ip name-server).
     * Показываем ПЕРЕД тем, как дать менять DoH, чтобы не перезаписывать
     * настройки вслепую.
     */
    fun loadNameServers() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("ip" to mapOf("name-server" to emptyMap<String, Any>())))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) body.asJsonArray[0] else body
                    _nameServers.value = DnsAndScheduleParser.parseNameServers(first)
                }
            } catch (e: Exception) {
                AppLogger.logAction("Name servers load failed", e.message ?: "")
            }
        }
    }

    fun createSchedule(name: String, description: String, daysOfWeek: List<Int>, startHour: Int, startMin: Int, stopHour: Int, stopMin: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val actions = mutableListOf<Map<String, Any>>()
                daysOfWeek.forEach { dow ->
                    actions += mapOf("action" to "start", "hour" to startHour.toString(), "min" to startMin.toString(), "dow" to dow.toString())
                    actions += mapOf("action" to "stop", "hour" to stopHour.toString(), "min" to stopMin.toString(), "dow" to dow.toString())
                }
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("schedule" to mapOf("name" to name, "description" to description, "action" to actions)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка создания расписания: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка создания расписания: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Привязывает расписание к устройству - по аналогии с уже проверенным
     * setClientPolicy() (тот же узел ip.hotspot.host, поле "schedule"
     * вместо "policy"). НЕ подтверждено отдельным HAR именно для привязки
     * расписания к хосту - только структурная аналогия с полем "policy" в
     * том же узле. Проверить перед активным использованием.
     */
    fun setClientSchedule(mac: String, scheduleName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "schedule" to scheduleName)))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadDeviceList()
                    loadClients()
                } else {
                    _error.value = "Ошибка привязки расписания: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка привязки расписания: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Назначает политику маршрутизации устройству. Основано на реальном
     * startup-config с роутера, где видна ветка:
     *   ip hotspot
     *       host <mac> permit
     *       host <mac> policy Policy0
     * "policy" - соседнее поле в том же узле "host <mac>", что и "access",
     * который уже подтверждённо работает через toggleClient() - используем
     * тот же командный путь ip.hotspot.host, просто с другим полем.
     * policyName должно совпадать с одним из имён `ip policy ...` на роутере
     * (в конфиге видны: HydraRoute, Policy0, Proxy4, Policy1) - вводится
     * вручную, автосписка политик пока нет.
     */
    fun setClientPolicy(mac: String, policyName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction("Set client policy", "mac=$mac policy=$policyName")
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("ip" to mapOf("hotspot" to mapOf("host" to mapOf("mac" to mac, "policy" to policyName)))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadClients()
                } else {
                    _error.value = "Ошибка назначения политики: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка назначения политики: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleClient(mac: String, allow: Boolean) {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Toggle client access", "mac=$mac allow=$allow")
                repository.getRestApi().setClientAccess(
                    mapOf("mac" to mac, "access" to if (allow) "permit" else "deny")
                )
                loadClients()
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Переименовывает устройство в списке клиентов. Использует минимальную
     * реальную команду ("known.host.name") без побочных сбросов других
     * настроек устройства (в оригинальном веб-UI тот же диалог заодно
     * сбрасывает лимиты трафика/DNS-профиль - мы этого сознательно не делаем).
     */
    fun renameDevice(mac: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction("Rename device", "mac=$mac name=$newName")
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("known" to mapOf("host" to mapOf("name" to newName, "mac" to mac))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadClients()
                } else {
                    _error.value = "Ошибка переименования: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка переименования: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Создаёт правило межсетевого экрана (ip access-list) для входящего
     * трафика указанного WAN-интерфейса. Формат команды - тот же принцип
     * частичного патча, что и у остальных set-команд в проекте: массив с
     * завершающим system.configuration.save. protocol == "" означает "любой
     * протокол" (поле не отправляется вовсе, а не отправляется пустой
     * строкой - на 🔴/непроверенных полях лучше не отправлять лишнего).
     */
    fun createFirewallRule(
        wanId: String,
        action: String,
        protocol: String,
        sourceIp: String,
        sourceMask: String,
        destIp: String,
        destMask: String,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction(
                    "Create firewall rule",
                    "wan=$wanId action=$action protocol=$protocol src=$sourceIp/$sourceMask dst=$destIp/$destMask"
                )
                val rule = mutableMapOf<String, Any>(
                    "interface" to wanId,
                    "action" to action,
                    "source-address" to mapOf("ip" to sourceIp, "mask" to sourceMask),
                    "destination-address" to mapOf("ip" to destIp, "mask" to destMask)
                )
                if (protocol.isNotBlank()) rule["protocol"] = protocol
                if (description.isNotBlank()) rule["description"] = description

                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("ip" to mapOf("access-list" to rule)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка создания правила: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка создания правила: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Меняет пароль Wi-Fi сети. networkId - "Home" для основной сети или
     * "Guest" для гостевой (подтверждено реальным дампом /rci/show/mws/wlan
     * с этого роутера; на других моделях имена id теоретически могут
     * отличаться). Отправляется только поле пароля - остальные настройки
     * сети (SSID, band-steering и т.д.) не трогаются.
     */
    /**
     * Обновляет несколько настроек Wi-Fi сети одним запросом. Отправляет
     * ТОЛЬКО те поля, что переданы не-null - остальные настройки на роутере
     * не трогаются (подтверждённое поведение частичного патча для mws.wlan,
     * см. ROADMAP.md). wpsEnabled/peerIsolation оставляй null, если не хочешь
     * их менять - мы не знаем их текущее состояние на роутере.
     */
    fun updateWifiNetwork(
        networkId: String,
        ssidName: String? = null,
        password: String? = null,
        wpsEnabled: Boolean? = null,
        peerIsolation: Boolean? = null,
        enable: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val wlanFields = mutableMapOf<String, Any>("id" to networkId)
                ssidName?.let { wlanFields["ssid"] = mapOf("name" to it) }
                password?.let { wlanFields["wpa"] = mapOf("psk" to it) }
                wpsEnabled?.let { wlanFields["wps"] = mapOf("enable" to it) }
                peerIsolation?.let { wlanFields["peer-isolation"] = it }
                enable?.let { wlanFields["enable"] = it }

                if (wlanFields.size <= 1) {
                    _isLoading.value = false
                    return@launch
                }

                AppLogger.logAction("Update wifi network", "network=$networkId fields=${wlanFields.keys}")
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("mws" to mapOf("wlan" to wlanFields)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка обновления сети: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка обновления сети: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * IntelliQoS (приоритезация трафика по категориям). Подтверждено
     * реальным HAR: включение/выключение отдельно от service.ntce, плюс
     * приоритеты категорий (1 - высший, 7 - низший). Стандартный набор
     * категорий и приоритетов Keenetic по умолчанию: calling(1),
     * streaming(2), gaming(3), work(4), surfing(5), other(6),
     * filetransfering(7) - меняем только сам enable, приоритеты не трогаем,
     * если не передали свои.
     */
    fun setIntelliQos(enabled: Boolean, priorities: Map<String, Int>? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val defaultPriorities = priorities ?: mapOf(
                    "calling" to 1, "streaming" to 2, "gaming" to 3,
                    "work" to 4, "surfing" to 5, "other" to 6, "filetransfering" to 7
                )
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("ntce" to mapOf("qos" to mapOf(
                            "category" to defaultPriorities.map { (cat, pr) -> mapOf("category" to cat, "priority" to pr) }
                        ))),
                        mapOf("ntce" to mapOf("qos" to mapOf("enable" to enabled))),
                        mapOf("service" to mapOf("ntce" to enabled)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка IntelliQoS: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка IntelliQoS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Менеджер пакетов (opkg) - хранилище для установки пакетов (obly
     * появляется при подключённом USB-накопителе или встроенной памяти
     * OPKG:). Подтверждено реальным HAR.
     */
    fun setOpkgManager(enabled: Boolean, disk: String = "OPKG:/") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("opkg" to mapOf(
                            "disk" to mapOf("disk" to (if (enabled) disk else ""), "no" to !enabled),
                            "initrc" to mapOf("path" to (if (enabled) "1" else ""), "no" to !enabled)
                        )),
                        mapOf("user" to listOf(mapOf("name" to "admin", "tag" to mapOf("tag" to "opt")))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка менеджера пакетов: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка менеджера пакетов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Торрент-клиент. Подтверждено реальным HAR. */
    fun setTorrentClient(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("service" to mapOf("torrent" to enabled)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка торрент-клиента: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка торрент-клиента: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _vpnServer = MutableStateFlow<VpnServerConfig?>(null)
    val vpnServer: StateFlow<VpnServerConfig?> = _vpnServer.asStateFlow()

    fun loadVpnServerConfig() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("sc" to mapOf("vpn-server" to emptyMap<String, Any>()))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) body.asJsonArray[0] else body
                    _vpnServer.value = VpnServerParser.parse(first)
                }
            } catch (e: Exception) {
                AppLogger.logAction("VPN server config load failed", e.message ?: "")
            }
        }
    }

    fun setCustomDoh(url: String, targetInterface: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entry = mutableMapOf<String, Any>("url" to url, "hash" to "", "domain" to "")
                targetInterface?.let { entry["interface"] = it }
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("dns-proxy" to mapOf("https" to mapOf("upstream" to listOf(mapOf("no" to true), entry)))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка смены DNS: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка смены DNS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Подключает роутер к внешней Wi-Fi сети как клиент (режим WifiStation,
     * используется например для повторителя/моста). Формат подтверждён
     * реальным HAR - полная последовательность полей для интерфейса
     * WifiMaster{0|1}/WifiStation0.
     */
    private val _scanResults = MutableStateFlow<List<SiteSurveyResult>>(emptyList())
    val scanResults: StateFlow<List<SiteSurveyResult>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * Сканирует соседние Wi-Fi сети. Подтверждено реальным HAR:
     * {"show":{"site-survey":{"name":"WifiMasterX"}}} через REST /rci/.
     */
    fun scanWifiNetworks(masterRadio: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResults.value = emptyList()
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(mapOf("show" to mapOf("site-survey" to mapOf("name" to masterRadio))))
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val first = if (body?.isJsonArray == true && body.asJsonArray.size() > 0) body.asJsonArray[0] else body
                    _scanResults.value = SiteSurveyParser.parse(first)
                } else {
                    _error.value = "Ошибка сканирования: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка сканирования: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Назначает физические LAN-порты под отдельные службы провайдера
     * (интернет / IPTV / VoIP). Формат подтверждён реальным HAR - команда
     * IPoE-конфигурации на WAN-интерфейсе (тот, что реально даёт IP -
     * см. show wans). Пустая строка в поле = "не назначено".
     *
     * ⚠️ РИСК: ошибка здесь может отключить интернет через физический порт,
     * если назначить один и тот же порт не туда. Проверяй значения дважды
     * перед применением, и лучше сначала на некритичном порту.
     */
    fun setPortRoles(wanInterfaceId: String, inetPort: String, iptvPort: String, voipPort: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("interface" to mapOf(
                            "ipoe" to mapOf(
                                "inet-port" to inetPort, "inet-pcp" to "", "inet-vlan" to "",
                                "iptv-port" to iptvPort, "iptv-pcp" to "", "iptv-vlan" to "",
                                "voip-port" to voipPort, "voip-pcp" to "", "voip-vlan" to ""
                            ),
                            "name" to wanInterfaceId
                        )),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка назначения портов: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка назначения портов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun connectWifiClient(masterRadio: String, ssid: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stationId = "$masterRadio/WifiStation0"
                val response = repository.getRestApi().executeRci(
                    listOf(
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
                        mapOf("interface" to mapOf("up" to true, "name" to stationId)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка подключения Wi-Fi клиента: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка подключения Wi-Fi клиента: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Отключает режим Wi-Fi клиента. Формат подтверждён реальным HAR. */
    fun disconnectWifiClient(masterRadio: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stationId = "$masterRadio/WifiStation0"
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("interface" to mapOf("ssid" to mapOf("no" to true), "name" to stationId)),
                        mapOf("interface" to mapOf("up" to false, "name" to stationId)),
                        mapOf("interface" to mapOf("description" to mapOf("no" to true), "name" to stationId)),
                        mapOf("interface" to mapOf("authentication" to mapOf("wpa-psk" to mapOf("no" to true)), "name" to stationId)),
                        mapOf("interface" to mapOf(
                            "encryption" to mapOf("enable" to mapOf("no" to true), "wpa" to mapOf("no" to true), "wpa2" to mapOf("no" to true)),
                            "name" to stationId
                        )),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка отключения Wi-Fi клиента: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка отключения Wi-Fi клиента: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setWifiPassword(networkId: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction("Set WiFi password", "network=$networkId")
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("mws" to mapOf("wlan" to mapOf("id" to networkId, "wpa" to mapOf("psk" to newPassword)))),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка смены пароля: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка смены пароля: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Reboot requested")
                val response = repository.getRestApi().reboot()
                if (!response.isSuccessful) {
                    val raw = response.errorBody()?.string()
                    _error.value = "Ошибка перезагрузки: HTTP ${response.code()} $raw"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка перезагрузки: ${e.message}"
            }
        }
    }

    fun toggleInterface(name: String, up: Boolean) {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Toggle interface", "name=$name up=$up")
                // Подтверждено реальным HAR: {"interface":{"up":bool,"name":..}}
                // через общий /rci/ (не rci/interface/{name}, как было раньше -
                // тот путь не был подтверждён ни разу).
                val response = repository.getRestApi().executeRci(
                    listOf(
                        mapOf("interface" to mapOf("up" to up, "name" to name)),
                        mapOf("system" to mapOf("configuration" to mapOf("save" to emptyMap<String, Any>())))
                    )
                )
                if (response.isSuccessful) {
                    loadInterfaces()
                } else {
                    _error.value = "Ошибка: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun executeSsh(command: String, port: Int = 22, login: String? = null, password: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppLogger.logAction("SSH command", "$command (port=$port login=${login ?: "<default>"})")
                val result = repository.getSshClient(port, login, password).execute(command)
                result.onSuccess { _sshOutput.value = it }
                    .onFailure { _sshOutput.value = "Ошибка: ${it.message}" }
            } catch (e: Exception) {
                _sshOutput.value = "Ошибка SSH: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun discoverRouters() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ip = routerIp.value.substringBeforeLast(".")
                AppLogger.logAction("Discover routers", "network=$ip")
                _discoveredRouters.value = AutoDiscovery.discover(ip)
            } catch (e: Exception) {
                _error.value = "Ошибка поиска: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Сохраняет настройки подключения из экрана "Настройки" и перелогинивается. */
    fun saveConnectionSettings(ip: String, login: String, password: String?, autoLogin: Boolean) {
        viewModelScope.launch {
            dataStore.saveRouterIp(ip)
            dataStore.saveRouterLogin(login)
            dataStore.setAutoLogin(autoLogin)
            if (!password.isNullOrBlank()) {
                login(password, ip, login, rememberMe = autoLogin)
            }
        }
    }

    fun setAutoLogin(enabled: Boolean) {
        viewModelScope.launch { dataStore.setAutoLogin(enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            AppLogger.logAction("Logout requested")
            dataStore.clear()
            repository.clearSession()
            _hasSavedPassword.value = false
            _isLoggedIn.value = false
            _systemInfo.value = null
            _clients.value = emptyList()
            _interfaces.value = emptyList()
            _wifiNetworks.value = emptyList()
        }
    }

    fun clearError() { _error.value = null }
}
