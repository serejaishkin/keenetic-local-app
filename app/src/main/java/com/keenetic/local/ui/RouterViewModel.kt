package com.keenetic.local.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keenetic.local.api.*
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.discovery.AutoDiscovery
import com.keenetic.local.util.AppLogger
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val interfaces: StateFlow<List<InterfaceInfo>> = _interfaces.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetwork>> = _wifiNetworks.asStateFlow()

    private val _associations = MutableStateFlow<List<WifiAssoc>>(emptyList())
    val associations: StateFlow<List<WifiAssoc>> = _associations.asStateFlow()

    private val _ipPolicies = MutableStateFlow<List<String>>(emptyList())
    val ipPolicies: StateFlow<List<String>> = _ipPolicies.asStateFlow()

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

    val routerIp: StateFlow<String> = dataStore.routerIp.stateIn(viewModelScope, SharingStarted.Lazily, "192.168.1.1")
    val routerLogin: StateFlow<String> = dataStore.routerLogin.stateIn(viewModelScope, SharingStarted.Lazily, "admin")
    val autoLoginEnabled: StateFlow<Boolean> = dataStore.autoLogin.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
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

    fun loadInterfaces() {
        viewModelScope.launch {
            try {
                AppLogger.logAction("Refresh interfaces")
                val response = repository.getRestApi().getInterfacesRaw()
                if (response.isSuccessful) {
                    val raw = response.body() ?: emptyMap()
                    _interfaces.value = InterfaceMapper.toInterfaceList(raw)
                    _wifiNetworks.value = InterfaceMapper.toWifiNetworks(raw)
                } else {
                    _error.value = "HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки интерфейсов: ${e.message}"
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
        peerIsolation: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val wlanFields = mutableMapOf<String, Any>("id" to networkId)
                ssidName?.let { wlanFields["ssid"] = mapOf("name" to it) }
                password?.let { wlanFields["wpa"] = mapOf("psk" to it) }
                wpsEnabled?.let { wlanFields["wps"] = mapOf("enable" to it) }
                peerIsolation?.let { wlanFields["peer-isolation"] = it }

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
                repository.getRestApi().setInterface(
                    name,
                    if (up) mapOf("up" to "true") else mapOf("down" to "true")
                )
                loadInterfaces()
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
