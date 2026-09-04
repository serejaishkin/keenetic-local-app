package com.keenetic.local.ui.screens

import androidx.lifecycle.viewModelScope
import com.google.gson.JsonElement
import com.keenetic.local.KeeneticApp
import com.keenetic.local.api.DnsServerInfo
import com.keenetic.local.api.SavedService
import com.keenetic.local.api.VpnServerParser
import com.keenetic.local.ssh.KeeneticSshService
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Compatibility methods for UI screens whose ViewModel API was added later. */
private fun RouterViewModel.setState(name: String, value: Any?) {
    runCatching {
        val field = RouterViewModel::class.java.getDeclaredField(name)
        field.isAccessible = true
        val state = field.get(this)
        val method = state.javaClass.methods.firstOrNull { it.name == "setValue" && it.parameterTypes.size == 1 }
            ?: return
        method.invoke(state, value)
    }
}

fun RouterViewModel.refreshNetworkHint() {
    viewModelScope.launch(Dispatchers.IO) {
        val gateway = NetworkUtils.detectRouterGatewayIp(KeeneticApp.instance)
        val suggested = NetworkUtils.getSuggestedRouterIps(KeeneticApp.instance)
        setState("_networkHint", com.keenetic.local.api.NetworkHint(gateway = gateway, suggestedRouterIps = suggested))
        if (!gateway.isNullOrBlank()) setState("_detectedGatewayIp", gateway)
        setState("_suggestedIps", suggested)
    }
}

fun RouterViewModel.saveConnectionSettings(ip: String, login: String, password: String?, autoLogin: Boolean) {
    val cleanIp = ip.trim().ifBlank { "192.168.1.1" }
    val cleanLogin = login.trim().ifBlank { "admin" }
    setState("_routerIp", cleanIp)
    setState("_routerLogin", cleanLogin)
    setState("_savedIp", cleanIp)
    setState("_savedUsername", cleanLogin)
    setState("_autoLoginEnabled", autoLogin)
    if (!password.isNullOrBlank()) com.keenetic.local.security.EncryptedStorage(KeeneticApp.instance).savePassword(password)
    viewModelScope.launch {
        KeeneticApp.instance.dataStoreManager.saveSettings(cleanIp, "80", cleanLogin, autoLogin, false)
    }
}

fun RouterViewModel.executeSsh(command: String, port: Int = 22, login: String? = null, password: String? = null) {
    viewModelScope.launch(Dispatchers.IO) {
        setState("_isLoading", true)
        try {
            val host = savedIp.value.ifBlank { "192.168.1.1" }
            val user = login?.ifBlank { null } ?: savedUsername.value.ifBlank { "admin" }
            val pass = password?.ifBlank { null } ?: com.keenetic.local.security.EncryptedStorage(KeeneticApp.instance).getPassword().orEmpty()
            if (pass.isBlank()) {
                setState("_sshOutput", "Ошибка: SSH пароль не указан и не сохранён.")
                return@launch
            }
            val result = KeeneticSshService().executeCommand(host, port, user, pass, command)
            setState("_sshOutput", result.output.ifBlank { result.error.orEmpty().ifBlank { "Команда выполнена без вывода (exit ${result.exitCode})" } })
        } catch (e: Exception) {
            setState("_sshOutput", "Ошибка SSH: ${e.message}")
        } finally {
            setState("_isLoading", false)
        }
    }
}

private suspend fun repositoryExecute(vm: RouterViewModel, command: Map<String, Any>): Boolean = runCatching {
    val field = RouterViewModel::class.java.getDeclaredField("repository")
    field.isAccessible = true
    (field.get(vm) as com.keenetic.local.api.RouterRepository).executeRciWithSave(listOf(command))
}.getOrDefault(false)

fun RouterViewModel.setTorrentSettings(directory: String, rpcPort: Int, rpcPublic: Boolean, peerPort: Int) {
    viewModelScope.launch {
        val cmd = mapOf("torrent" to mapOf("directory" to directory, "rpc-port" to rpcPort, "peer-port" to peerPort, "rpc-public" to rpcPublic))
        if (!repositoryExecute(this@setTorrentSettings, cmd)) setState("_error", "Не удалось применить настройки торрент-клиента")
    }
}

fun RouterViewModel.setIntelliQos(enabled: Boolean) {
    viewModelScope.launch {
        if (!repositoryExecute(this@setIntelliQos, mapOf("ntce" to mapOf("enable" to enabled)))) setState("_error", "Не удалось изменить IntelliQoS")
    }
}

fun RouterViewModel.setIntelliQosPriority(category: String, priority: Int) {
    viewModelScope.launch {
        val cmd = mapOf("ntce" to mapOf("priority" to mapOf(category to priority.coerceIn(1, 4))))
        if (!repositoryExecute(this@setIntelliQosPriority, cmd)) setState("_error", "Не удалось изменить приоритет IntelliQoS")
    }
}

fun RouterViewModel.setOpkgManager(enabled: Boolean) {
    viewModelScope.launch {
        val cmd = if (enabled) mapOf("opkg" to mapOf("enable" to true)) else mapOf("no" to mapOf("opkg" to true))
        if (!repositoryExecute(this@setOpkgManager, cmd)) setState("_error", "Не удалось изменить opkg")
    }
}

fun RouterViewModel.setTorrentClient(enabled: Boolean) {
    viewModelScope.launch {
        val cmd = if (enabled) mapOf("torrent" to mapOf("enable" to true)) else mapOf("no" to mapOf("torrent" to true)
        if (!repositoryExecute(this@setTorrentClient, cmd)) setState("_error", "Не удалось изменить торрент-клиент")
    }
}

fun RouterViewModel.loadAutoUpdateStatus() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadAutoUpdateStatus, "system/update")
        setState("_autoUpdateEnabled", raw?.let { findBoolean(it, "auto-update") ?: findBoolean(it, "enable") } ?: false)
    }
}

fun RouterViewModel.setAutoUpdate(enabled: Boolean) {
    viewModelScope.launch {
        val cmd = mapOf("system" to mapOf("update" to mapOf("auto" to enabled)))
        if (repositoryExecute(this@setAutoUpdate, cmd)) setState("_autoUpdateEnabled", enabled)
    }
}

fun RouterViewModel.loadSystemUpdateStatus() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadSystemUpdateStatus, "system/update/status")
        setState("_systemUpdateStatusRaw", raw ?: com.google.gson.JsonNull.INSTANCE)
    }
}

fun RouterViewModel.setAdminPassword(username: String, password: String) {
    viewModelScope.launch {
        if (!repositoryExecute(this@setAdminPassword, mapOf("user" to mapOf("name" to username, "password" to password)))) setState("_error", "Не удалось сменить пароль")
    }
}

fun RouterViewModel.loadDhcpPool() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadDhcpPool, "ip/dhcp/pool")
        setState("_dhcpPoolRaw", raw ?: com.google.gson.JsonNull.INSTANCE)
    }
}

fun RouterViewModel.loadIntelliQos() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadIntelliQos, "ntce/summary")
        setState("_ntceSummaryRaw", raw ?: com.google.gson.JsonNull.INSTANCE)
    }
}

fun RouterViewModel.loadNameServers() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadNameServers, "dns-proxy")
        val list = mutableListOf<DnsServerInfo>()
        if (raw?.isJsonArray == true) raw.asJsonArray.forEach { if (it.isJsonObject) {
            val o = it.asJsonObject
            list += DnsServerInfo(address = o.get("address")?.takeIf { p -> p.isJsonPrimitive }?.asString, interfaceName = o.get("interface")?.takeIf { p -> p.isJsonPrimitive }?.asString)
        } }
        setState("_nameServers", list)
    }
}

fun RouterViewModel.loadDohUpstream() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadDohUpstream, "dns-proxy/https/upstream")
        val values = mutableListOf<String>()
        if (raw != null) collectStrings(raw, values)
        setState("_dohUpstream", values.distinct())
    }
}

fun RouterViewModel.loadVpnServerConfig() {
    viewModelScope.launch {
        val raw = queryCompat(this@loadVpnServerConfig, "vpn-server")
        setState("_vpnServer", raw?.let { VpnServerParser.parse(it) })
    }
}

fun RouterViewModel.setCustomDoh(url: String, targetInterface: String?) { setCustomDoh(url) }

fun RouterViewModel.saveService(service: SavedService) {
    val current = savedServices.value.toMutableList()
    val index = current.indexOfFirst { it.name == service.name }
    if (index >= 0) current[index] = service else current += service
    setState("_savedServices", current)
}

fun RouterViewModel.deleteService(service: SavedService) {
    setState("_savedServices", savedServices.value.filterNot { it.name == service.name && it.host == service.host })
}

private suspend fun queryCompat(vm: RouterViewModel, path: String): JsonElement? = runCatching {
    val field = RouterViewModel::class.java.getDeclaredField("repository")
    field.isAccessible = true
    (field.get(vm) as com.keenetic.local.api.RouterRepository).queryShow(path)
}.getOrNull()

private fun findBoolean(root: JsonElement, key: String): Boolean? {
    if (root.isJsonObject) {
        val obj = root.asJsonObject
        obj.get(key)?.takeIf { it.isJsonPrimitive }?.let { return runCatching { it.asBoolean }.getOrNull() }
        obj.entrySet().forEach { (_, value) -> findBoolean(value, key)?.let { return it } }
    } else if (root.isJsonArray) root.asJsonArray.forEach { findBoolean(it, key)?.let { return it } }
    return null
}

private fun collectStrings(root: JsonElement, out: MutableList<String>) {
    when {
        root.isJsonPrimitive -> root.asString.takeIf { it.startsWith("https://") || it.contains("dns", ignoreCase = true) }?.let(out::add)
        root.isJsonArray -> root.asJsonArray.forEach { collectStrings(it, out) }
        root.isJsonObject -> root.asJsonObject.entrySet().forEach { collectStrings(it.value, out) }
    }
}
