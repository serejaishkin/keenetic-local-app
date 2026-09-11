package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.*
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import kotlinx.coroutines.launch

@Composable
fun VpnServersScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("PPTP", "L2TP", "SSTP", "OpenConnect", "WireGuard", "IKEv2")

    val pptpServer by viewModel.pptpServer.collectAsState()
    val l2tpServer by viewModel.l2tpServer.collectAsState()
    val sstpServer by viewModel.sstpServer.collectAsState()
    val ocServer by viewModel.ocServer.collectAsState()
    val wireguardServer by viewModel.wireguardServer.collectAsState()
    val ikev2Server by viewModel.ikev2Server.collectAsState()

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPptpServer()
        viewModel.loadL2tpServer()
        viewModel.loadSstpServer()
        viewModel.loadOcServer()
        viewModel.loadWireguardServer()
        viewModel.loadIkev2Server()
        viewModel.loadInterfaces()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "VPN Серверы",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = KeeneticColors.Surface,
            contentColor = KeeneticColors.Primary,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) KeeneticColors.Primary else KeeneticColors.TextSecondary
                        )
                    }
                )
            }
        }

        if (saveMessage != null) {
            Surface(
                color = KeeneticColors.Primary.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = saveMessage ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.Primary
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> ServerFormTab(
                    title = "PPTP-сервер",
                    enabled = pptpServer.enabled,
                    poolStart = pptpServer.poolStart,
                    poolSize = pptpServer.poolSize,
                    nat = pptpServer.nat,
                    encryption = pptpServer.encryption,
                    multiLogin = pptpServer.multiLogin,
                    isSaving = isSaving,
                    onSave = { enable, start, size, isNat, enc, multi, _ ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mutableMapOf<String, Any>(
                                "enable" to enable,
                                "pool-start" to start,
                                "pool-size" to size,
                                "nat" to isNat,
                                "multi-login" to multi,
                                "encryption" to enc
                            )
                            val ok = viewModel.saveVpnServerConfig("vpn-server", cfg)
                            saveMessage = if (ok) "PPTP-сервер сохранён" else "Ошибка сохранения PPTP"
                            isSaving = false
                            viewModel.loadPptpServer()
                        }
                    }
                )

                1 -> ServerFormTab(
                    title = "L2TP/IPsec-сервер",
                    enabled = l2tpServer.enabled,
                    poolStart = l2tpServer.poolStart,
                    poolSize = l2tpServer.poolSize,
                    nat = l2tpServer.nat,
                    encryption = l2tpServer.encryption,
                    isSaving = isSaving,
                    onSave = { enable, start, size, isNat, enc, _, _ ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mutableMapOf<String, Any>(
                                "enable" to enable,
                                "pool-start" to start,
                                "pool-size" to size,
                                "nat" to isNat,
                                "encryption" to enc
                            )
                            val ok = viewModel.saveVpnServerConfig("crypto.l2tp-server", cfg)
                            saveMessage = if (ok) "L2TP-сервер сохранён" else "Ошибка сохранения L2TP"
                            isSaving = false
                            viewModel.loadL2tpServer()
                        }
                    }
                )

                2 -> ServerFormTab(
                    title = "SSTP-сервер",
                    enabled = sstpServer.enabled,
                    poolStart = sstpServer.poolStart,
                    poolSize = sstpServer.poolSize,
                    camouflage = sstpServer.camouflage,
                    hasCamouflage = true,
                    isSaving = isSaving,
                    onSave = { enable, start, size, _, _, _, cam ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mutableMapOf<String, Any>(
                                "enable" to enable,
                                "pool-start" to start,
                                "pool-size" to size,
                                "camouflage" to cam
                            )
                            val ok = viewModel.saveVpnServerConfig("sstp-server", cfg)
                            saveMessage = if (ok) "SSTP-сервер сохранён" else "Ошибка сохранения SSTP"
                            isSaving = false
                            viewModel.loadSstpServer()
                        }
                    }
                )

                3 -> ServerFormTab(
                    title = "OpenConnect (OC) сервер",
                    enabled = ocServer.enabled,
                    poolStart = ocServer.poolStart,
                    poolSize = ocServer.poolSize,
                    nat = ocServer.nat,
                    camouflage = ocServer.camouflage,
                    hasCamouflage = true,
                    isSaving = isSaving,
                    onSave = { enable, start, size, isNat, _, _, cam ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mutableMapOf<String, Any>(
                                "enable" to enable,
                                "pool-start" to start,
                                "pool-size" to size,
                                "nat" to isNat,
                                "camouflage" to cam
                            )
                            val ok = viewModel.saveVpnServerConfig("oc-server", cfg)
                            saveMessage = if (ok) "OpenConnect-сервер сохранён" else "Ошибка сохранения OpenConnect"
                            isSaving = false
                            viewModel.loadOcServer()
                        }
                    }
                )

                4 -> WireGuardServerTab(
                    server = wireguardServer,
                    isSaving = isSaving,
                    onToggleEnable = { newEnable ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mapOf("enable" to newEnable)
                            val ok = viewModel.saveVpnServerConfig("wireguard-server", cfg)
                            saveMessage = if (ok) "WireGuard-сервер обновлён" else "Ошибка обновления WireGuard"
                            isSaving = false
                            viewModel.loadWireguardServer()
                        }
                    }
                )

                5 -> ServerFormTab(
                    title = "IKEv2-сервер",
                    enabled = ikev2Server.enabled,
                    poolStart = ikev2Server.poolStart,
                    poolSize = ikev2Server.poolSize,
                    hasEncryption = false,
                    hasNat = false,
                    isSaving = isSaving,
                    onSave = { enable, start, size, _, _, _, _ ->
                        scope.launch {
                            isSaving = true
                            saveMessage = null
                            val cfg = mutableMapOf<String, Any>(
                                "enable" to enable,
                                "pool-start" to start,
                                "pool-size" to size
                            )
                            val ok = viewModel.saveVpnServerConfig("crypto.virtual-ip-server-ikev2", cfg)
                            saveMessage = if (ok) "IKEv2-сервер сохранён" else "Ошибка сохранения IKEv2"
                            isSaving = false
                            viewModel.loadIkev2Server()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ServerFormTab(
    title: String,
    enabled: Boolean,
    poolStart: String,
    poolSize: String,
    nat: Boolean = false,
    encryption: Boolean = false,
    multiLogin: Boolean = false,
    camouflage: Boolean = false,
    hasNat: Boolean = true,
    hasEncryption: Boolean = true,
    hasMultiLogin: Boolean = false,
    hasCamouflage: Boolean = false,
    isSaving: Boolean,
    onSave: (
        enabled: Boolean,
        poolStart: String,
        poolSize: String,
        nat: Boolean,
        encryption: Boolean,
        multiLogin: Boolean,
        camouflage: Boolean
    ) -> Unit
) {
    var curEnabled by remember(enabled) { mutableStateOf(enabled) }
    var curPoolStart by remember(poolStart) { mutableStateOf(poolStart) }
    var curPoolSize by remember(poolSize) { mutableStateOf(poolSize) }
    var curNat by remember(nat) { mutableStateOf(nat) }
    var curEncryption by remember(encryption) { mutableStateOf(encryption) }
    var curMultiLogin by remember(multiLogin) { mutableStateOf(multiLogin) }
    var curCamouflage by remember(camouflage) { mutableStateOf(camouflage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .heightIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = curEnabled,
                        onCheckedChange = { curEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                    )
                }

                HorizontalDivider(color = KeeneticColors.Divider)

                OutlinedTextField(
                    value = curPoolStart,
                    onValueChange = { curPoolStart = it },
                    label = { Text("Начальный IP пула") },
                    placeholder = { Text("172.16.1.2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )

                OutlinedTextField(
                    value = curPoolSize,
                    onValueChange = { curPoolSize = it },
                    label = { Text("Размер пула (кол-во адресов)") },
                    placeholder = { Text("10") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )

                if (hasNat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Трансляция адресов (NAT)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = curNat,
                            onCheckedChange = { curNat = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                        )
                    }
                }

                if (hasEncryption) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Шифрование",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = curEncryption,
                            onCheckedChange = { curEncryption = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                        )
                    }
                }

                if (hasMultiLogin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Множественный вход одного пользователя",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = curMultiLogin,
                            onCheckedChange = { curMultiLogin = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                        )
                    }
                }

                if (hasCamouflage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Маскировка (Camouflage)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = curCamouflage,
                            onCheckedChange = { curCamouflage = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                        )
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            curEnabled,
                            curPoolStart,
                            curPoolSize,
                            curNat,
                            curEncryption,
                            curMultiLogin,
                            curCamouflage
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранение...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить настройки")
                    }
                }
            }
        }
    }
}

@Composable
private fun WireGuardServerTab(
    server: WireguardServerStatus,
    isSaving: Boolean,
    onToggleEnable: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .heightIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "WireGuard-сервер",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = server.enabled,
                        onCheckedChange = { onToggleEnable(it) },
                        enabled = !isSaving,
                        colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                    )
                }

                HorizontalDivider(color = KeeneticColors.Divider)

                InfoRow("Порт (ListenPort)", "${server.listenPort}")
                if (server.publicKey.isNotBlank()) {
                    InfoRow("Публичный ключ", server.publicKey)
                }
                if (server.address.isNotBlank()) {
                    InfoRow("Адрес сервера", server.address)
                }
                InfoRow("Количество пиров", "${server.peers.size}")
            }
        }

        if (server.peers.isNotEmpty()) {
            Text(
                "Список пиров (${server.peers.size})",
                style = MaterialTheme.typography.titleSmall,
                color = KeeneticColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            server.peers.forEach { peer ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            peer.name.ifEmpty { "Пир WireGuard" },
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        if (peer.publicKey.isNotBlank()) {
                            Text(
                                "Ключ: ${peer.publicKey}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        if (peer.allowedIp.isNotBlank()) {
                            Text(
                                "Разрешённый IP: ${peer.allowedIp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        if (peer.endpoint.isNotBlank()) {
                            Text(
                                "Endpoint: ${peer.endpoint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
