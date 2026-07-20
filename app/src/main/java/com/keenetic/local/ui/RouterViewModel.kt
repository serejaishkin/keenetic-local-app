package com.keenetic.local.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keenetic.local.api.*
import com.keenetic.local.data.DataStoreManager
import com.keenetic.local.discovery.AutoDiscovery
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

    private val _sshOutput = MutableStateFlow("")
    val sshOutput: StateFlow<String> = _sshOutput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _discoveredRouters = MutableStateFlow<List<AutoDiscovery.DiscoveredRouter>>(emptyList())
    val discoveredRouters: StateFlow<List<AutoDiscovery.DiscoveredRouter>> = _discoveredRouters.asStateFlow()

    val routerIp: StateFlow<String> = dataStore.routerIp.stateIn(viewModelScope, SharingStarted.Lazily, "192.168.1.1")
    val routerLogin: StateFlow<String> = dataStore.routerLogin.stateIn(viewModelScope, SharingStarted.Lazily, "admin")
    val autoLoginEnabled: StateFlow<Boolean> = dataStore.autoLogin.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        viewModelScope.launch {
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
                dataStore.saveRouterIp(ip)
                dataStore.saveRouterLogin(login)
                val result = repository.login(password)
                if (result == "OK" || result.startsWith("OK")) {
                    dataStore.setAutoLogin(rememberMe)
                    _isLoggedIn.value = true
                    refreshAll()
                } else {
                    _error.value = result
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
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
    }

    fun loadSystemInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
                val response = repository.getRestApi().getClients()
                if (response.isSuccessful) {
                    _clients.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки устройств: ${e.message}"
            }
        }
    }

    fun loadInterfaces() {
        viewModelScope.launch {
            try {
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

    fun toggleClient(mac: String, allow: Boolean) {
        viewModelScope.launch {
            try {
                repository.getRestApi().setClientAccess(
                    mapOf("mac" to mac, "access" to if (allow) "permit" else "deny")
                )
                loadClients()
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            try {
                repository.getRestApi().reboot()
            } catch (e: Exception) {
                _error.value = "Ошибка перезагрузки: ${e.message}"
            }
        }
    }

    fun toggleInterface(name: String, up: Boolean) {
        viewModelScope.launch {
            try {
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

    fun executeSsh(command: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getSshClient().execute(command)
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
            dataStore.clear()
            repository.clearSession()
            _isLoggedIn.value = false
            _systemInfo.value = null
            _clients.value = emptyList()
            _interfaces.value = emptyList()
            _wifiNetworks.value = emptyList()
        }
    }

    fun clearError() { _error.value = null }
}
