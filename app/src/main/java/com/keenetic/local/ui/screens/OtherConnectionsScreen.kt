package com.keenetic.local.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.keenetic.local.api.VpnConnection
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val CONN_TYPES = listOf(
    "proxy" to "Прокси (VPN Liberty)",
    "wireguard" to "WireGuard",
    "zerotier" to "ZeroTier",
    "openvpn" to "OpenVPN",
    "ike" to "IKEv2",
    "openconnect" to "OpenConnect",
    "sstp" to "SSTP",
    "pppoe" to "PPPoE",
    "pptp" to "PPTP",
    "l2tp" to "L2TP",
    "gre" to "GRE",
    "ipip" to "IPIP",
    "eoip" to "EoIP",
    "ipsec" to "IPsec"
)

@Composable
fun OtherConnectionsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val connections by viewModel.vpnConnections.collectAsState()
    var detailConn by remember { mutableStateOf<VpnConnection?>(null) }
    var addConn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadVpnConnections()
    }

    detailConn?.let { conn ->
        ConnSettingsDialog(conn = conn, viewModel = viewModel, onDismiss = { detailConn = null })
    }

    if (addConn) {
        AddConnectionDialog(
            viewModel = viewModel,
            onDismiss = { addConn = false },
            onCreated = {
                addConn = false
                viewModel.loadVpnConnections()
            }
        )
    }

    SectionScaffold(
        title = "Другие подключения",
        subtitle = "WireGuard, OpenVPN, L2TP, PPPoE, SSTP, ZeroTier, прокси",
        onBack = onBack,
        onRefresh = { viewModel.loadVpnConnections() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "add") {
                SectionCard {
                    Text(
                        "Подключения создаются на роутере, включение и отключение отправляется сразу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    OutlinedButton(
                        onClick = { addConn = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить подключение", color = KeeneticColors.Primary)
                    }
                }
            }

            if (connections.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        Text(
                            "Подключений нет",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "Нажмите «Добавить подключение», чтобы создать новое, или обновите список.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            } else {
                item(key = "list-header") {
                    SubGroupHeader("Подключения", connections.size)
                }
                items(connections.size) { index ->
                    OtherConnectionCard(
                        conn = connections[index],
                        viewModel = viewModel,
                        onEdit = { detailConn = connections[index] }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddConnectionDialog(viewModel: RouterViewModel, onDismiss: () -> Unit, onCreated: () -> Unit) {
    var selectedType by remember { mutableStateOf(CONN_TYPES.first().first) }
    var name by remember { mutableStateOf("") }
    var typePicker by remember { mutableStateOf(false) }
    val selectedLabel = CONN_TYPES.firstOrNull { it.first == selectedType }?.second ?: selectedType

    FormDialog(
        title = "Добавить подключение",
        confirmLabel = "Создать",
        onDismiss = onDismiss,
        onConfirm = {
            viewModel.addVpnConnection(selectedType, name.trim())
            onCreated()
        }
    ) {
        EditableRow(
            label = "Тип подключения",
            value = selectedLabel,
            onClick = { typePicker = true }
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название (описание)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Интерфейс «$selectedLabel» будет создан на роутере. Настройки (сервер, ключи и т.д.) " +
                "можно будет заполнить после создания.",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
    }

    if (typePicker) {
        OptionPickerDialog(
            title = "Тип подключения",
            options = CONN_TYPES,
            selectedKey = selectedType,
            onSelect = { selectedType = it },
            onDismiss = { typePicker = false }
        )
    }
}

@Composable
private fun ConnSettingsDialog(conn: VpnConnection, viewModel: RouterViewModel, onDismiss: () -> Unit) {
    val detail by viewModel.vpnConnDetail.collectAsState()
    val detailId by viewModel.vpnConnDetailId.collectAsState()
    val viaList by viewModel.vpnViaInterfaces.collectAsState()
    val isLoaded = detail != null && detailId == conn.id

    val fieldsByType = remember(conn.type, conn.protocol) { connFieldsSchema(conn.type, conn.protocol) }
    fun has(fid: FieldId) = fieldsByType.contains(fid)

    fun raw(k: String): String = if (isLoaded) (jsonValue(detail, k) ?: "") else ""
    fun rawList(k: String): List<String> = if (isLoaded) jsonList(detail, k) else emptyList()
    fun rawBool(k: String) = isLoaded && jsonBool(detail, k)

    var description by remember(conn.id, isLoaded) { mutableStateOf(raw("description")) }
    var username by remember(conn.id, isLoaded) { mutableStateOf(raw("authentication.identity")) }
    var password by remember(conn.id, isLoaded) { mutableStateOf("") }
    var server by remember(conn.id, isLoaded) { mutableStateOf(raw("upstream")) }
    var mtu by remember(conn.id, isLoaded) { mutableStateOf(raw("ip.mtu")) }
    var mss by remember(conn.id, isLoaded) { mutableStateOf(raw("ip.tcp.adjust-mss")) }
    var psk by remember(conn.id, isLoaded) { mutableStateOf(raw("ipsec.preshared-key")) }
    var wgKey by remember(conn.id, isLoaded) { mutableStateOf(raw("wireguard.private-key")) }
    var service by remember(conn.id, isLoaded) { mutableStateOf(raw("pppoe.service")) }
    var openvpnConfig by remember(conn.id, isLoaded) { mutableStateOf(raw("openvpn.config")) }
    var ccp by remember(conn.id, isLoaded) { mutableStateOf(rawBool("ccp")) }
    var mppe by remember(conn.id, isLoaded) { mutableStateOf(rawBool("encryption.mppe")) }
    var acceptRoutes by remember(conn.id, isLoaded) {
        mutableStateOf(rawBool("openvpn.accept-routes") || rawBool("openconnect.accept-routes"))
    }
    var internetAccess by remember(conn.id, isLoaded) {
        mutableStateOf(rawBool("ip.global.enabled") || rawBool("ip.global.auto") || raw("ip.global").equals("true", ignoreCase = true))
    }
    var protocol by remember(conn.id, isLoaded) { mutableStateOf(raw("proxy.protocol.proto")) }
    var authType by remember(conn.id, isLoaded) { mutableStateOf(raw("authentication.type").ifBlank { "auto" }) }
    var wgAddress by remember(conn.id, isLoaded) { mutableStateOf(raw("ip.address")) }
    var listenPort by remember(conn.id, isLoaded) { mutableStateOf(raw("wireguard.listen-port")) }
    var importedPeers by remember(conn.id, isLoaded) { mutableStateOf(0) }
    var peer by remember(conn.id, isLoaded) { mutableStateOf(raw("peer")) }
    var authScheme by remember(conn.id, isLoaded) { mutableStateOf(raw("authentication.type").ifBlank { "auto" }) }
    var ipMode by remember(conn.id, isLoaded) {
        mutableStateOf(if (raw("ip.address").isNotBlank()) "manual" else "auto")
    }
    var manualAddress by remember(conn.id, isLoaded) {
        val a = raw("ip.address"); mutableStateOf(if (a.contains("/")) a.substringBefore("/") else a)
    }
    var manualMask by remember(conn.id, isLoaded) {
        val a = raw("ip.address"); val m = raw("ip.mask").ifBlank { "" }
        val prefix = if (a.contains("/")) a.substringAfter("/").toIntOrNull() else null
        mutableStateOf(if (m.isNotBlank()) m else prefix?.let { prefixToMask(it) } ?: "")
    }
    var manualRemote by remember(conn.id, isLoaded) { mutableStateOf(raw("ip.remote.address")) }
    var dnsList by remember(conn.id, isLoaded) {
        mutableStateOf(rawList("ip.name-server").joinToString(", "))
    }
    var connectVia by remember(conn.id, isLoaded) {
        mutableStateOf(raw("proxy.connect").ifBlank { raw("via") })
    }
    var isDnsV4Ignored by remember(conn.id, isLoaded) {
        mutableStateOf(detail?.has("ip.name-servers") == true && !rawBool("ip.name-servers"))
    }
    var ipv6Mode by remember(conn.id, isLoaded) {
        val addrs = rawList("ipv6.address")
        mutableStateOf(
            if (addrs.any { it.equals("auto", ignoreCase = true) }) "auto"
            else if (addrs.isNotEmpty()) "manual"
            else "disabled"
        )
    }
    var ipv6Address by remember(conn.id, isLoaded) {
        val a = rawList("ipv6.address").firstOrNull { !it.equals("auto", ignoreCase = true) } ?: ""
        mutableStateOf(if (a.contains("/")) a.substringBefore("/") else a)
    }
    var ipv6PrefixLen by remember(conn.id, isLoaded) {
        val a = rawList("ipv6.address").firstOrNull { !it.equals("auto", ignoreCase = true) } ?: ""
        val len = a.substringAfter("/", "").ifBlank { "64" }
        mutableStateOf(len)
    }
    var ipv6Gateway by remember(conn.id, isLoaded) { mutableStateOf(raw("ipv6.gateway")) }
    var ipv6IgnoreDns by remember(conn.id, isLoaded) {
        mutableStateOf(detail?.has("ipv6.name-servers") == true && !rawBool("ipv6.name-servers"))
    }

    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (!text.isNullOrBlank()) {
                if (conn.type.equals("openvpn", ignoreCase = true)) {
                    openvpnConfig = text
                } else {
                    val conf = parseWgConf(text)
                    if (conf.privateKey.isNotBlank()) wgKey = conf.privateKey
                    if (conf.address.isNotBlank()) wgAddress = conf.address
                    if (conf.listenPort.isNotBlank()) listenPort = conf.listenPort
                    if (conf.mtu.isNotBlank()) mtu = conf.mtu
                    importedPeers = conf.peerCount
                }
            }
        }
    }

    LaunchedEffect(conn.id) { viewModel.loadVpnConnDetail(conn.id) }

    FormDialog(
        title = conn.name.ifBlank { conn.id },
        onDismiss = onDismiss,
        onConfirm = {
            val fields = linkedMapOf<String, Any>()
            if (has(FieldId.DESCRIPTION)) fields["description"] = description.trim()
            if (has(FieldId.INTERNET_ACCESS)) fields["ip.global"] = mapOf("enabled" to internetAccess)
            if (has(FieldId.PROTOCOL) && protocol.isNotBlank()) fields["proxy.protocol.proto"] = protocol.trim()
            val authSchemeVal = if (has(FieldId.AUTH_TYPE)) authType else authScheme
            val includeScheme = has(FieldId.AUTH_TYPE) || has(FieldId.AUTHENTICATION)
            if (includeScheme || has(FieldId.USERNAME) || has(FieldId.PASSWORD)) {
                val auth = linkedMapOf<String, Any>()
                if (includeScheme && authSchemeVal.trim().isNotBlank() && !authSchemeVal.trim().equals("auto", ignoreCase = true)) auth["type"] = authSchemeVal.trim()
                if (username.isNotBlank()) auth["identity"] = username.trim()
                if (password.isNotBlank()) auth["password"] = password.trim()
                if (auth.isNotEmpty()) fields["authentication"] = auth
            }
            if (has(FieldId.PEER) && peer.isNotBlank()) fields["peer"] = peer.trim()
            if (has(FieldId.SERVICE)) fields["pppoe.service"] = service.trim()
            if (has(FieldId.WG_KEY) && wgKey.isNotBlank()) fields["wireguard.private-key"] = wgKey.trim()
            if (has(FieldId.IP_ADDRESS) && wgAddress.isNotBlank()) fields["ip.address"] = wgAddress.trim()
            if (has(FieldId.LISTEN_PORT)) listenPort.trim().toIntOrNull()?.let { fields["wireguard.listen-port"] = it }
            if (has(FieldId.PSK) && (psk.isNotBlank() || detail?.has("ipsec.preshared-key") == true)) fields["ipsec.preshared-key"] = psk.trim()
            if (has(FieldId.SERVER) && server.isNotBlank()) fields["upstream"] = server.trim()
            if (has(FieldId.OPENVPN_CONFIG)) fields["openvpn.config"] = openvpnConfig.trim()
            if (has(FieldId.MTU) && mtu.isNotBlank()) mtu.trim().toIntOrNull()?.let { fields["ip.mtu"] = it }
            if (has(FieldId.MSS)) fields["ip.tcp.adjust-mss"] = if (mss.isNotBlank()) mss.trim() else "disable"
            if (has(FieldId.CCP)) fields["ccp"] = ccp
            if (has(FieldId.MPPE)) fields["encryption.mppe"] = mppe
            if (has(FieldId.ACCEPT_ROUTES)) {
                if (conn.type.equals("openconnect", ignoreCase = true)) fields["openconnect.accept-routes"] = acceptRoutes
                else fields["openvpn.accept-routes"] = acceptRoutes
            }
            if (has(FieldId.IP_MODE)) {
                if (ipMode == "manual" && manualAddress.isNotBlank()) {
                    val m = maskToPrefix(manualMask).let { if (it > 0) it else 24 }
                    fields["ip.address"] = manualAddress.trim()
                    fields["ip.mask"] = prefixToMask(m)
                } else {
                    fields["ip.address"] = ""
                    fields["ip.remote.address"] = ""
                }
            }
            if (has(FieldId.REMOTE)) fields["ip.remote.address"] = manualRemote.trim()
            if (has(FieldId.DNS_LIST)) {
                val list = dnsList.split(",").map { it.trim() }.filter { it.isNotBlank() }
                fields["ip.name-server"] = list
            }
            if (has(FieldId.CONNECT_VIA)) {
                val v = connectVia.trim()
                if (conn.type.equals("proxy", ignoreCase = true)) fields["proxy.connect"] = v
                else fields["via"] = v
            }
            if (has(FieldId.IS_DNSV4_IGNORED) && (detail?.has("ip.name-servers") == true || isDnsV4Ignored)) fields["ip.name-servers"] = !isDnsV4Ignored
            if (has(FieldId.IPV6)) {
                val addrs = mutableListOf<String>()
                val prefixes = mutableListOf<String>()
                when (ipv6Mode) {
                    "auto" -> { addrs.add("auto"); prefixes.add("auto") }
                    "manual" -> {
                        if (ipv6Address.isNotBlank()) addrs.add("${ipv6Address.trim()}/${ipv6PrefixLen.trim().ifBlank { "64" }}")
                        if (ipv6Address.isNotBlank()) prefixes.add("/${ipv6PrefixLen.trim().ifBlank { "64" }}")
                    }
                }
                fields["ipv6.address"] = addrs
                fields["ipv6.prefix"] = prefixes
                fields["ipv6.gateway"] = ipv6Gateway.trim()
                if (detail?.has("ipv6.name-servers") == true || ipv6IgnoreDns) fields["ipv6.name-servers"] = !ipv6IgnoreDns
            }
            viewModel.updateVpnConnection(conn.id, fields)
            onDismiss()
        }
    ) {
        Text(
            typeLabel(conn.type, conn.protocol),
            style = MaterialTheme.typography.labelSmall,
            color = KeeneticColors.TextSecondary
        )
        if (!isLoaded) {
            Text(
                "Загрузка конфигурации…",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
        if (has(FieldId.INTERNET_ACCESS)) ConnSwitch("Использовать для доступа в Интернет", internetAccess, { internetAccess = it }, loaded = isLoaded)
        if (has(FieldId.DESCRIPTION)) ConnText("Описание", description, { description = it }, hint = "Необязательно", loaded = isLoaded)
        if (has(FieldId.PROTOCOL)) ConnDropdown("Протокол (тип)", listOf("http" to "HTTP", "https" to "HTTPS", "socks5" to "SOCKS5"), protocol, { protocol = it }, loaded = isLoaded)
        if (has(FieldId.AUTH_TYPE)) ConnDropdown("Авторизация", listOf("auto" to "Автоматически", "none" to "Не требуется"), authType, { authType = it }, loaded = isLoaded)
        if (has(FieldId.PEER)) ConnText("Пиринговый адрес (внешний)", peer, { peer = it }, hint = "IP или домен", loaded = isLoaded)
        if (has(FieldId.USERNAME)) ConnText("Имя пользователя", username, { username = it }, loaded = isLoaded)
        if (has(FieldId.PASSWORD)) ConnText("Пароль", password, { password = it }, hint = "Оставьте пустым, чтобы не менять", password = true, loaded = isLoaded)
        if (has(FieldId.AUTHENTICATION)) ConnDropdown("Метод аутентификации", listOf("auto" to "Автоматически", "pap" to "PAP", "chap" to "CHAP", "mschap" to "MS CHAP v1", "mschap-v2" to "MS CHAP v2"), authScheme, { authScheme = it }, loaded = isLoaded)
        if (has(FieldId.SERVICE)) ConnText("Имя службы (service-name)", service, { service = it }, loaded = isLoaded)
        if (has(FieldId.WG_KEY)) ConnText("Приватный ключ WireGuard", wgKey, { wgKey = it }, hint = "Оставьте пустым, чтобы не менять", password = true, loaded = isLoaded)
        if (has(FieldId.WG_KEY) || has(FieldId.OPENVPN_CONFIG)) {
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, enabled = isLoaded, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (conn.type.equals("openvpn", ignoreCase = true)) "Загрузить конфигурацию (.ovpn)" else "Импортировать конфигурацию WireGuard (.conf)", color = KeeneticColors.Primary)
            }
            if (importedPeers > 0) Text("Импортировано пиров: $importedPeers", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        }
        if (has(FieldId.IP_ADDRESS)) ConnText("Адрес", wgAddress, { wgAddress = it }, hint = "Например, 10.0.0.2/24", loaded = isLoaded)
        if (has(FieldId.LISTEN_PORT)) ConnText("Порт (ListenPort)", listenPort, { listenPort = it }, numeric = true, loaded = isLoaded)
        if (has(FieldId.PSK)) ConnText("Общий ключ (PSK)", psk, { psk = it }, hint = "Оставьте пустым, чтобы не менять", password = true, loaded = isLoaded)
        if (has(FieldId.SERVER)) ConnText("Сервер / адрес", server, { server = it }, loaded = isLoaded)
        if (has(FieldId.IP_MODE)) ConnDropdown("Настройка IP", listOf("auto" to "Автоматически (DHCP)", "manual" to "Вручную"), ipMode, { ipMode = it }, loaded = isLoaded)
        if (has(FieldId.IP_MODE) && ipMode == "manual") {
            ConnText("IP-адрес", manualAddress, { manualAddress = it }, hint = "10.0.0.2", loaded = isLoaded)
            ConnText("Маска", manualMask, { manualMask = it }, hint = "255.255.255.0", loaded = isLoaded)
        }
        if (has(FieldId.REMOTE)) ConnText("Удалённый адрес", manualRemote, { manualRemote = it }, hint = "IP или домен", loaded = isLoaded)
        if (has(FieldId.DNS_LIST)) ConnText("DNS-серверы (через запятую)", dnsList, { dnsList = it }, hint = "1.1.1.1, 8.8.8.8", loaded = isLoaded)
        if (has(FieldId.OPENVPN_CONFIG)) ConnText("Конфигурация OpenVPN", openvpnConfig, { openvpnConfig = it }, hint = "Содержимое файла .ovpn", loaded = isLoaded)
        if (has(FieldId.MTU)) ConnText("MTU", mtu, { mtu = it }, numeric = true, loaded = isLoaded)
        if (has(FieldId.MSS)) ConnSwitch("Корректировать TCP MSS", mss.isNotBlank(), { mss = if (it) "enable" else "" }, loaded = isLoaded)
        if (has(FieldId.CCP)) ConnSwitch("Протокол CCP", ccp, { ccp = it }, loaded = isLoaded)
        if (has(FieldId.MPPE)) ConnSwitch("Шифрование MPPE", mppe, { mppe = it }, loaded = isLoaded)
        if (has(FieldId.ACCEPT_ROUTES)) ConnSwitch("Принимать маршруты от сервера", acceptRoutes, { acceptRoutes = it }, loaded = isLoaded)
        if (has(FieldId.CONNECT_VIA)) ConnDropdown("Выход в интернет через", listOf("" to "Автоматически") + viaList.map { it to it }, connectVia, { connectVia = it }, loaded = isLoaded)
        if (has(FieldId.IS_DNSV4_IGNORED)) ConnSwitch("Не использовать DNS-серверы IPv4 из ответа", isDnsV4Ignored, { isDnsV4Ignored = it }, loaded = isLoaded)
        if (has(FieldId.IPV6)) {
            RowDivider()
            SubGroupHeader("IPv6")
            ConnDropdown("Режим IPv6", listOf("disabled" to "Отключено", "auto" to "Автоматически", "manual" to "Вручную"), ipv6Mode, { ipv6Mode = it }, loaded = isLoaded)
            if (ipv6Mode == "manual") {
                ConnText("IPv6-адрес", ipv6Address, { ipv6Address = it }, hint = "fd00::1", loaded = isLoaded)
                ConnText("Длина префикса", ipv6PrefixLen, { ipv6PrefixLen = it }, numeric = true, loaded = isLoaded)
                ConnText("Шлюз", ipv6Gateway, { ipv6Gateway = it }, loaded = isLoaded)
                ConnSwitch("Игнорировать DNS IPv6 от провайдера", ipv6IgnoreDns, { ipv6IgnoreDns = it }, loaded = isLoaded)
            }
        }
        Text(
            "Изменения применяются через RCI и сохраняются в конфигурацию роутера.",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
    }
}

private fun prefixToMask(prefix: Int): String = when (prefix) {
    in 0..8 -> "${256 - (1 shl (8 - prefix))}.0.0.0"
    in 9..16 -> "255.${256 - (1 shl (16 - prefix))}.0.0"
    in 17..24 -> "255.255.${256 - (1 shl (24 - prefix))}.0"
    in 25..32 -> "255.255.255.${256 - (1 shl (32 - prefix))}"
    else -> "255.255.255.0"
}

private fun maskToPrefix(mask: String): Int {
    val parts = mask.split(".")
    if (parts.size != 4) return 0
    var bits = 0
    for (p in parts) { bits += p.toIntOrNull()?.countOneBits() ?: 0 }
    return bits
}

private enum class FieldId {
    DESCRIPTION, INTERNET_ACCESS, PROTOCOL, SERVER, AUTH_TYPE, USERNAME, PASSWORD,
    CONNECT_VIA, WG_KEY, IP_ADDRESS, LISTEN_PORT, DNS_LIST, MTU, MSS,
    PEER, AUTHENTICATION, IP_MODE, REMOTE, SERVICE, CCP, MPPE,
    IS_DNSV4_IGNORED, IPV6, ACCEPT_ROUTES, OPENVPN_CONFIG, PSK
}

private fun connFieldsSchema(type: String, protocol: String?): List<FieldId> {
    val t = type.lowercase()
    if (t == "wireguard" || t == "wg" || t == "awg") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.WG_KEY, FieldId.IP_ADDRESS,
            FieldId.LISTEN_PORT, FieldId.DNS_LIST, FieldId.MTU, FieldId.IPV6, FieldId.MSS, FieldId.CONNECT_VIA
        )
    }
    if (t == "openvpn") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.USERNAME, FieldId.PASSWORD,
            FieldId.OPENVPN_CONFIG, FieldId.DNS_LIST, FieldId.MTU, FieldId.MSS, FieldId.ACCEPT_ROUTES
        )
    }
    if (t == "openconnect") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.SERVER, FieldId.USERNAME,
            FieldId.PASSWORD, FieldId.DNS_LIST, FieldId.MTU, FieldId.MSS, FieldId.ACCEPT_ROUTES
        )
    }
    if (t == "pppoe") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.PEER, FieldId.USERNAME,
            FieldId.PASSWORD, FieldId.AUTHENTICATION, FieldId.SERVICE, FieldId.IP_MODE,
            FieldId.REMOTE, FieldId.DNS_LIST, FieldId.MTU, FieldId.IPV6,
            FieldId.MSS, FieldId.IS_DNSV4_IGNORED, FieldId.CONNECT_VIA
        )
    }
    if (t == "pptp" || t == "l2tp" || t == "sstp") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.PEER, FieldId.USERNAME,
            FieldId.PASSWORD, FieldId.AUTHENTICATION, FieldId.IP_MODE,
            FieldId.REMOTE, FieldId.DNS_LIST, FieldId.MTU, FieldId.IPV6, FieldId.MSS,
            FieldId.IS_DNSV4_IGNORED, FieldId.CONNECT_VIA,
            FieldId.CCP, FieldId.MPPE
        )
    }
    if (t == "ike" || t == "ikev2") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.PEER, FieldId.USERNAME,
            FieldId.PASSWORD, FieldId.PSK, FieldId.MTU, FieldId.MSS
        )
    }
    if (t == "gre" || t == "ipip" || t == "eoip") {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.REMOTE, FieldId.MTU, FieldId.MSS
        )
    }
    if (t == "zerotier" || t == "ipsec") {
        return listOf(FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.MTU)
    }
    if (t == "proxy" || protocol?.lowercase() in listOf("http", "https", "socks5")) {
        return listOf(
            FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS, FieldId.PROTOCOL, FieldId.SERVER,
            FieldId.AUTH_TYPE, FieldId.USERNAME, FieldId.PASSWORD, FieldId.CONNECT_VIA
        )
    }
    return listOf(FieldId.DESCRIPTION, FieldId.INTERNET_ACCESS)
}

@Composable
private fun ConnText(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    hint: String = "",
    password: Boolean = false,
    numeric: Boolean = false,
    loaded: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        enabled = loaded,
        placeholder = { if (hint.isNotBlank()) Text(hint) },
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformationNone,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KeeneticColors.Primary,
            cursorColor = KeeneticColors.Primary,
            unfocusedBorderColor = KeeneticColors.Divider,
            disabledBorderColor = KeeneticColors.Divider,
            focusedTextColor = KeeneticColors.TextPrimary,
            unfocusedTextColor = KeeneticColors.TextPrimary,
            disabledTextColor = KeeneticColors.TextPrimary,
            focusedLabelColor = KeeneticColors.Primary,
            unfocusedLabelColor = KeeneticColors.TextSecondary,
            disabledLabelColor = KeeneticColors.TextSecondary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private object VisualTransformationNone : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText =
        androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
}

@Composable
private fun ConnSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit, loaded: Boolean) {
    SwitchRow(label = label, checked = checked, onCheckedChange = onChange, enabled = loaded)
}

@Composable
private fun ConnDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    loaded: Boolean
) {
    var picker by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected.ifBlank { "Автоматически" }
    EditableRow(
        label = label,
        value = selectedLabel,
        enabled = loaded,
        onClick = { picker = true }
    )
    if (picker) {
        OptionPickerDialog(
            title = label,
            options = options,
            selectedKey = selected,
            onSelect = onSelect,
            onDismiss = { picker = false }
        )
    }
}

private fun jsonValue(obj: JsonObject?, key: String): String {
    var cur: com.google.gson.JsonElement? = obj ?: return ""
    for (p in key.split(".")) {
        cur = (cur as? JsonObject)?.get(p) ?: return ""
    }
    return (cur as? com.google.gson.JsonPrimitive)?.asString ?: ""
}

private fun jsonBool(obj: JsonObject?, key: String): Boolean {
    var cur: com.google.gson.JsonElement? = obj ?: return false
    for (p in key.split(".")) {
        cur = (cur as? JsonObject)?.get(p) ?: return false
    }
    return (cur as? com.google.gson.JsonPrimitive)?.runCatching { asBoolean }?.getOrDefault(false) ?: false
}

private fun jsonList(obj: JsonObject?, key: String): List<String> {
    var cur: com.google.gson.JsonElement? = obj ?: return emptyList()
    for (p in key.split(".")) {
        cur = (cur as? JsonObject)?.get(p) ?: return emptyList()
    }
    return when (cur) {
        is com.google.gson.JsonArray -> cur.mapNotNull { e ->
            (e as? com.google.gson.JsonPrimitive)?.let { if (it.isNumber) it.asInt.toString() else it.asString }
        }
        is com.google.gson.JsonPrimitive -> listOf(if (cur.isNumber) cur.asInt.toString() else cur.asString)
        else -> emptyList()
    }
}

private data class WgConf(
    val privateKey: String,
    val address: String,
    val listenPort: String,
    val mtu: String,
    val peerCount: Int
)

private fun parseWgConf(text: String): WgConf {
    var section = ""
    var privateKey = ""
    var address = ""
    var listenPort = ""
    var mtu = ""
    var peerCount = 0
    text.lines().forEach { raw ->
        val line = raw.trim()
        if (line.startsWith("[", ignoreCase = false)) {
            section = line
            if (line.equals("[Peer]", ignoreCase = true)) peerCount++
            return@forEach
        }
        val eq = line.indexOf('=')
        if (eq < 0) return@forEach
        val key = line.substring(0, eq).trim().lowercase()
        val value = line.substring(eq + 1).trim()
        if (section.equals("[Interface]", ignoreCase = true)) {
            when (key) {
                "privatekey" -> if (privateKey.isEmpty()) privateKey = value
                "address" -> if (address.isEmpty()) address = value
                "listenport" -> if (listenPort.isEmpty()) listenPort = value
                "mtu" -> if (mtu.isEmpty()) mtu = value
            }
        }
    }
    return WgConf(privateKey, address, listenPort, mtu, peerCount)
}

@Composable
private fun OtherConnectionCard(conn: VpnConnection, viewModel: RouterViewModel, onEdit: () -> Unit) {
    SectionCard(
        title = conn.name.ifBlank { conn.id },
        subtitle = typeLabel(conn.type, conn.protocol),
        icon = typeIcon(conn.type)
    ) {
        InfoRow(
            label = "Состояние",
            value = if (conn.isUp) "Подключено" else "Отключено",
            valueColor = if (conn.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary
        )
        if (conn.ip != null) {
            InfoRow("IP-адрес", conn.ip, monospace = true)
        }
        if (conn.upstream != null) {
            InfoRow("Сервер", conn.upstream, monospace = true)
        }
        RowDivider()
        SwitchRow(
            label = "Подключение включено",
            checked = conn.isUp,
            onCheckedChange = { up -> viewModel.toggleInterface(conn.id, up) }
        )
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Настройки подключения", color = KeeneticColors.Primary)
        }
    }
}

private fun typeIcon(type: String): ImageVector = when (type.lowercase()) {
    "proxy" -> Icons.Default.Dns
    "zerotier" -> Icons.Default.Share
    "gre", "ipip", "eoip", "ipv6to4", "6to4", "tunnel" -> Icons.Default.Link
    "ppp", "pppoe", "pptp" -> Icons.Default.Call
    else -> Icons.Default.Lock
}

private fun typeLabel(type: String, protocol: String?): String {
    val typeName = when (type.lowercase()) {
        "proxy" -> "Proxy"
        "wireguard" -> "WireGuard"
        "openvpn" -> "OpenVPN"
        "pptp" -> "PPTP"
        "ppp" -> "PPP"
        "pppoe" -> "PPPoE"
        "l2tp" -> "L2TP"
        "sstp" -> "SSTP"
        "ike" -> "IPsec IKE"
        "openconnect" -> "OpenConnect"
        "zerotier" -> "ZeroTier"
        "gre" -> "GRE"
        "ipip" -> "IPIP"
        "eoip" -> "EoIP"
        "ipv6to4", "6to4" -> "6to4-туннель"
        else -> type
    }
    return if (protocol != null) "$typeName ($protocol)" else typeName
}
