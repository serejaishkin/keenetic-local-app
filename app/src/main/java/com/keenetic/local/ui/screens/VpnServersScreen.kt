package com.keenetic.local.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.Ikev2Server
import com.keenetic.local.api.L2tpServer
import com.keenetic.local.api.OcServer
import com.keenetic.local.api.PptpServer
import com.keenetic.local.api.SstpServerFull
import com.keenetic.local.api.WireguardServerStatus
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors
import kotlinx.coroutines.launch

private const val VPN_TABS = "PPTP|L2TP|SSTP|OpenConnect|WireGuard|IKEv2"

@Composable
fun VpnServersScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = remember { VPN_TABS.split("|") }

    val pptpServer by viewModel.pptpServer.collectAsState()
    val l2tpServer by viewModel.l2tpServer.collectAsState()
    val sstpServer by viewModel.sstpServer.collectAsState()
    val ocServer by viewModel.ocServer.collectAsState()
    val wireguardServer by viewModel.wireguardServer.collectAsState()
    val ikev2Server by viewModel.ikev2Server.collectAsState()

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPptpServer()
        viewModel.loadL2tpServer()
        viewModel.loadSstpServer()
        viewModel.loadOcServer()
        viewModel.loadWireguardServer()
        viewModel.loadIkev2Server()
        viewModel.loadInterfaces()
    }

    fun save(path: String, config: Map<String, Any>, reload: () -> Unit) {
        scope.launch {
            isSaving = true
            errorMessage = null
            val ok = viewModel.saveVpnServerConfig(path, config)
            if (!ok) errorMessage = "Не удалось сохранить настройки VPN-сервера"
            isSaving = false
            reload()
        }
    }

    SectionScaffold(
        title = "VPN-серверы",
        subtitle = "PPTP, L2TP/IPsec, SSTP, OpenConnect, WireGuard, IKEv2",
        onBack = onBack,
        onRefresh = {
            viewModel.loadPptpServer()
            viewModel.loadL2tpServer()
            viewModel.loadSstpServer()
            viewModel.loadOcServer()
            viewModel.loadWireguardServer()
            viewModel.loadIkev2Server()
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "tabs") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, name ->
                        FilterChip(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.15f),
                                selectedLabelColor = KeeneticColors.Primary
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item(key = "error") {
                    SectionCard {
                        Text(
                            errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.Error
                        )
                    }
                }
            }

            when (selectedTabIndex) {
                0 -> item(key = "pptp") {
                    ServerFormSection(
                        title = "PPTP-сервер",
                        interfaceName = pptpServer.interfaceName,
                        enabled = pptpServer.enabled,
                        poolStart = pptpServer.poolStart,
                        poolSize = pptpServer.poolSize,
                        nat = pptpServer.nat,
                        encryption = pptpServer.encryption,
                        multiLogin = pptpServer.multiLogin,
                        hasNat = true,
                        hasEncryption = true,
                        hasMultiLogin = true,
                        isSaving = isSaving,
                        onSave = { enable, start, size, isNat, enc, multi, _ ->
                            save(
                                "vpn-server",
                                mapOf(
                                    "enable" to enable,
                                    "pool-start" to start,
                                    "pool-size" to size,
                                    "nat" to isNat,
                                    "multi-login" to multi,
                                    "encryption" to enc
                                ),
                                viewModel::loadPptpServer
                            )
                        }
                    )
                }

                1 -> item(key = "l2tp") {
                    ServerFormSection(
                        title = "L2TP/IPsec-сервер",
                        interfaceName = l2tpServer.interfaceName,
                        enabled = l2tpServer.enabled,
                        poolStart = l2tpServer.poolStart,
                        poolSize = l2tpServer.poolSize,
                        nat = l2tpServer.nat,
                        encryption = l2tpServer.encryption,
                        isSaving = isSaving,
                        onSave = { enable, start, size, isNat, enc, _, _ ->
                            save(
                                "crypto.l2tp-server",
                                mapOf(
                                    "enable" to enable,
                                    "pool-start" to start,
                                    "pool-size" to size,
                                    "nat" to isNat,
                                    "encryption" to enc
                                ),
                                viewModel::loadL2tpServer
                            )
                        }
                    )
                }

                2 -> item(key = "sstp") {
                    ServerFormSection(
                        title = "SSTP-сервер",
                        interfaceName = sstpServer.interfaceName,
                        enabled = sstpServer.enabled,
                        poolStart = sstpServer.poolStart,
                        poolSize = sstpServer.poolSize,
                        camouflage = sstpServer.camouflage,
                        hasCamouflage = true,
                        isSaving = isSaving,
                        onSave = { enable, start, size, _, _, _, cam ->
                            save(
                                "sstp-server",
                                mapOf(
                                    "enable" to enable,
                                    "pool-start" to start,
                                    "pool-size" to size,
                                    "camouflage" to cam
                                ),
                                viewModel::loadSstpServer
                            )
                        }
                    )
                }

                3 -> item(key = "oc") {
                    ServerFormSection(
                        title = "OpenConnect-сервер",
                        interfaceName = ocServer.interfaceName,
                        enabled = ocServer.enabled,
                        poolStart = ocServer.poolStart,
                        poolSize = ocServer.poolSize,
                        nat = ocServer.nat,
                        camouflage = ocServer.camouflage,
                        hasCamouflage = true,
                        isSaving = isSaving,
                        onSave = { enable, start, size, isNat, _, _, cam ->
                            save(
                                "oc-server",
                                mapOf(
                                    "enable" to enable,
                                    "pool-start" to start,
                                    "pool-size" to size,
                                    "nat" to isNat,
                                    "camouflage" to cam
                                ),
                                viewModel::loadOcServer
                            )
                        }
                    )
                }

                4 -> item(key = "wireguard") {
                    WireGuardServerSection(
                        server = wireguardServer,
                        isSaving = isSaving,
                        onToggleEnable = { newEnable ->
                            save("wireguard-server", mapOf("enable" to newEnable), viewModel::loadWireguardServer)
                        }
                    )
                }

                5 -> item(key = "ikev2") {
                    ServerFormSection(
                        title = "IKEv2-сервер",
                        interfaceName = ikev2Server.interfaceName,
                        enabled = ikev2Server.enabled,
                        poolStart = ikev2Server.poolStart,
                        poolSize = ikev2Server.poolSize,
                        hasNat = false,
                        hasEncryption = false,
                        isSaving = isSaving,
                        onSave = { enable, start, size, _, _, _, _ ->
                            save(
                                "crypto.virtual-ip-server-ikev2",
                                mapOf(
                                    "enable" to enable,
                                    "pool-start" to start,
                                    "pool-size" to size
                                ),
                                viewModel::loadIkev2Server
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerFormSection(
    title: String,
    interfaceName: String,
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

    var poolStartDialog by remember { mutableStateOf(false) }
    var poolSizeDialog by remember { mutableStateOf(false) }
    var poolStartInput by remember { mutableStateOf(poolStart) }
    var poolSizeInput by remember { mutableStateOf(poolSize) }

    fun commit() = onSave(
        curEnabled,
        curPoolStart,
        curPoolSize,
        curNat,
        curEncryption,
        curMultiLogin,
        curCamouflage
    )

    SectionCard(
        title = title,
        subtitle = interfaceName.ifBlank { "Интерфейс не назначен" },
        icon = Icons.Default.VpnLock
    ) {
        SwitchRow(
            label = "Сервер включён",
            checked = curEnabled,
            enabled = !isSaving,
            onCheckedChange = {
                curEnabled = it
                commit()
            }
        )

        if (interfaceName.isNotBlank()) {
            InfoRow("Интерфейс", interfaceName, monospace = true)
        }

        RowDivider()

        EditableRow(
            label = "Начальный IP пула",
            value = curPoolStart.ifBlank { "не задан" },
            hint = "Первый адрес, который выдаётся клиентам",
            enabled = !isSaving,
            onClick = {
                poolStartInput = curPoolStart
                poolStartDialog = true
            },
            monospaceValue = true
        )

        EditableRow(
            label = "Размер пула",
            value = curPoolSize.ifBlank { "не задан" },
            hint = "Количество адресов для одновременных подключений",
            enabled = !isSaving,
            onClick = {
                poolSizeInput = curPoolSize
                poolSizeDialog = true
            },
            monospaceValue = true
        )

        if (hasNat || hasEncryption || hasMultiLogin || hasCamouflage) {
            RowDivider()
            SubGroupHeader("Параметры подключения")
        }

        if (hasNat) {
            SwitchRow(
                label = "Трансляция адресов (NAT)",
                checked = curNat,
                enabled = !isSaving,
                onCheckedChange = {
                    curNat = it
                    commit()
                }
            )
        }
        if (hasEncryption) {
            SwitchRow(
                label = "Шифрование",
                checked = curEncryption,
                enabled = !isSaving,
                onCheckedChange = {
                    curEncryption = it
                    commit()
                }
            )
        }
        if (hasMultiLogin) {
            SwitchRow(
                label = "Множественный вход",
                description = "Один пользователь может быть подключён несколько раз",
                checked = curMultiLogin,
                enabled = !isSaving,
                onCheckedChange = {
                    curMultiLogin = it
                    commit()
                }
            )
        }
        if (hasCamouflage) {
            SwitchRow(
                label = "Маскировка трафика",
                description = "Ответы выглядят как обычный HTTPS-сайт",
                checked = curCamouflage,
                enabled = !isSaving,
                onCheckedChange = {
                    curCamouflage = it
                    commit()
                }
            )
        }
    }

    if (poolStartDialog) {
        FormDialog(
            title = "Начальный IP пула",
            onDismiss = { poolStartDialog = false },
            onConfirm = {
                curPoolStart = poolStartInput.trim()
                poolStartDialog = false
                commit()
            }
        ) {
            OutlinedTextField(
                value = poolStartInput,
                onValueChange = { poolStartInput = it },
                label = { Text("Первый адрес пула") },
                placeholder = { Text("172.16.1.2") },
                singleLine = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (poolSizeDialog) {
        FormDialog(
            title = "Размер пула",
            onDismiss = { poolSizeDialog = false },
            onConfirm = {
                curPoolSize = poolSizeInput.trim()
                poolSizeDialog = false
                commit()
            }
        ) {
            OutlinedTextField(
                value = poolSizeInput,
                onValueChange = { poolSizeInput = it },
                label = { Text("Количество адресов") },
                placeholder = { Text("10") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WireGuardServerSection(
    server: WireguardServerStatus,
    isSaving: Boolean,
    onToggleEnable: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(
            title = "WireGuard-сервер",
            subtitle = "Ключи и пиры создаются в веб-интерфейсе",
            icon = Icons.Default.VpnLock
        ) {
            SwitchRow(
                label = "Сервер включён",
                checked = server.enabled,
                enabled = !isSaving,
                onCheckedChange = onToggleEnable
            )
            RowDivider()
            InfoRow("Порт (ListenPort)", server.listenPort.toString(), monospace = true)
            if (server.publicKey.isNotBlank()) {
                InfoRow("Публичный ключ", server.publicKey, monospace = true)
            }
            if (server.address.isNotBlank()) {
                InfoRow("Адрес сервера", server.address, monospace = true)
            }
        }

        if (server.peers.isNotEmpty()) {
            SubGroupHeader("Пиры WireGuard", server.peers.size)
            server.peers.forEach { peer ->
                SectionCard(
                    title = peer.name.ifBlank { "Пир WireGuard" },
                    icon = Icons.Default.Computer
                ) {
                    if (peer.publicKey.isNotBlank()) {
                        InfoRow("Public key", peer.publicKey, monospace = true)
                    }
                    if (peer.allowedIp.isNotBlank()) {
                        InfoRow("Разрешённый IP", peer.allowedIp, monospace = true)
                    }
                    if (peer.endpoint.isNotBlank()) {
                        InfoRow("Endpoint", peer.endpoint, monospace = true)
                    }
                }
            }
        }
    }
}
