package com.keenetic.local.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
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
    private val context = application.applicationContext

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    private val _interfaces = MutableStateFlow<List<InterfaceInfo>>(emptyList())
    val interfaces: StateFlow<List<InterfaceInfo>> = _interfaces.asStateFlow()

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

    fun login(password: String, ip: String = "192.168.1.1", login: String = "admin") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("Keenetic", "login() start: ip=$ip")
                dataStore.saveRouterIp(ip)
                dataStore.saveRouterLogin(login)

                val testResult = repository.initClients(password)
                Log.d("Keenetic", "initClients result: $testResult")

                if (testResult.startsWith("FAIL")) {
                    Toast.makeText(context, "Роутер недоступен:\n$testResult", Toast.LENGTH_LONG).show()
                    _isLoggedIn.value = false
                    return@launch
                }

                Toast.makeText(context, "Подключено: $testResult", Toast.LENGTH_SHORT).show()
                _isLoggedIn.value = true
                // НЕ вызываем refreshAll() автоматически — ждём нажатия кнопки 🔄

            } catch (e: Exception) {
                val msg = "${e.javaClass.simpleName}: ${e.message?.take(200)}"
                Log.e("Keenetic", "login crash", e)
                Toast.makeText(context, "CRASH: $msg", Toast.LENGTH_LONG).show()
                _error.value = msg
                _isLoggedIn.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("Keenetic", "refreshAll start")
                loadSystemInfo()
                loadClients()
                loadInterfaces()
                Log.d("Keenetic", "refreshAll done")
            } catch (e: Exception) {
                Log.e("Keenetic", "refreshAll crash", e)
                Toast.makeText(context, "refreshAll: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSystemInfo() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getSystem()
                Log.d("Keenetic", "System HTTP: ${response.code()}")
                if (response.isSuccessful) {
                    _systemInfo.value = response.body()
                } else {
                    Log.w("Keenetic", "System not OK: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Keenetic", "System error: ${e.message}", e)
            }
        }
    }

    private fun loadClients() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getClients()
                Log.d("Keenetic", "Clients HTTP: ${response.code()}")
                if (response.isSuccessful) {
                    _clients.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("Keenetic", "Clients error: ${e.message}", e)
            }
        }
    }

    private fun loadInterfaces() {
        viewModelScope.launch {
            try {
                val response = repository.getRestApi().getInterfaces()
                Log.d("Keenetic", "Interfaces HTTP: ${response.code()}")
                if (response.isSuccessful) {
                    _interfaces.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("Keenetic", "Interfaces error: ${e.message}", e)
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
                Log.e("Keenetic", "toggleClient error", e)
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            try {
                repository.getRestApi().reboot()
            } catch (e: Exception) {
                Log.e("Keenetic", "reboot error", e)
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
                Log.e("Keenetic", "toggleInterface error", e)
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

    fun logout() {
        viewModelScope.launch {
            dataStore.clear()
            _isLoggedIn.value = false
            _systemInfo.value = null
            _clients.value = emptyList()
            _interfaces.value = emptyList()
        }
    }

    fun clearError() { _error.value = null }
}
