package com.keenetic.local.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.WifiNetworkInfo
import com.keenetic.local.api.WifiSiteSurveyEntry
import com.keenetic.local.api.WirelessClient
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiScreen(
    viewModel: RouterViewModel,
    onBack: () -> Unit = {}
) {
    val networks by viewModel.wifiNetworks.collectAsState()
    val wirelessClients by viewModel.wirelessClients.collectAsState()
    val wifiStation by viewModel.wifiStationStatus.collectAsState()
    val isWifiLoading by viewModel.isWifiLoading.collectAsState()
    val isWifiScanning by viewModel.isWifiScanning.collectAsState()
    val scanResults by viewModel.wifiScanResults.collectAsState()
    val actionMessage by viewModel.wifiActionMessage.collectAsState()

    var selectedBandFilter by remember { mutableStateOf("all") } // "all", "2.4", "5"
    var selectedClientForDetails by remember { mutableStateOf<WirelessClient?>(null) }
    var editingNetwork by remember { mutableStateOf<WifiNetworkInfo?>(null) }

    // WifiStation setup form state
    var showStationConfig by remember { mutableStateOf(false) }
    var stationRadioBand by remember { mutableStateOf("WifiMaster0") }
    var stationTargetSsid by remember { mutableStateOf("") }
    var stationTargetPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadWifiData()
    }

    // Identify primary 2.4 GHz and 5 GHz networks
    val net24 = networks.firstOrNull { it.band.contains("2.4") || it.id.contains("WifiMaster1") }
        ?: WifiNetworkInfo("WifiMaster1/AccessPoint0", "Keenetic-Home-2.4G", "2.4 GHz", true, 6, "WPA2-PSK", wirelessClients.count { it.band.contains("2.4") })
    val net5 = networks.firstOrNull { it.band.contains("5") || it.id.contains("WifiMaster0") }
        ?: WifiNetworkInfo("WifiMaster0/AccessPoint0", "Keenetic-Home-5G", "5 GHz", true, 36, "WPA2/WPA3-PSK", wirelessClients.count { it.band.contains("5") })

    val clients24Count = wirelessClients.count { it.band.contains("2.4") }
    val clients5Count = wirelessClients.count { it.band.contains("5") }

    val filteredClients = remember(wirelessClients, selectedBandFilter) {
        when (selectedBandFilter) {
            "2.4" -> wirelessClients.filter { it.band.contains("2.4") }
            "5" -> wirelessClients.filter { it.band.contains("5") }
            else -> wirelessClients
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Управление Wi-Fi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "2.4 & 5 ГГц • Клиенты • WifiStation",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = KeeneticColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadWifiData() },
                        enabled = !isWifiLoading
                    ) {
                        if (isWifiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = KeeneticColors.Primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                tint = KeeneticColors.Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KeeneticColors.Background
                )
            )
        },
        containerColor = KeeneticColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Action feedback banner
            if (actionMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Primary.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Primary))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = KeeneticColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    actionMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearWifiMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = KeeneticColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Band Control Section (2.4 GHz & 5 GHz)
            item {
                Text(
                    text = "Диапазоны Wi-Fi сети",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KeeneticColors.TextPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 2.4 GHz Band Card
                    WifiBandCard(
                        bandLabel = "2.4 ГГц",
                        bandTag = "Wi-Fi 4 / N",
                        accentColor = Color(0xFFF59E0B),
                        ssid = net24.ssid,
                        isEnabled = net24.enabled,
                        channel = net24.channel,
                        security = net24.security,
                        connectedClientsCount = clients24Count,
                        onToggle = { enabled ->
                            viewModel.toggleWifiBand("2.4 GHz", enabled)
                        },
                        onEdit = {
                            editingNetwork = net24
                        }
                    )

                    // 5 GHz Band Card
                    WifiBandCard(
                        bandLabel = "5 ГГц",
                        bandTag = "Wi-Fi 6 / AX",
                        accentColor = KeeneticColors.Primary,
                        ssid = net5.ssid,
                        isEnabled = net5.enabled,
                        channel = net5.channel,
                        security = net5.security,
                        connectedClientsCount = clients5Count,
                        onToggle = { enabled ->
                            viewModel.toggleWifiBand("5 GHz", enabled)
                        },
                        onEdit = {
                            editingNetwork = net5
                        }
                    )
                }
            }

            // 2. Connected Wireless Clients Section (RCI 'show wifi' & 'show associations')
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Беспроводные клиенты",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(KeeneticColors.SurfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${wirelessClients.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KeeneticColors.Primary
                            )
                        }
                    }

                    Text(
                        text = "RCI: show wifi",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            // Filter Chips: Все / 2.4 ГГц / 5 ГГц
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedBandFilter == "all",
                        onClick = { selectedBandFilter = "all" },
                        label = { Text("Все (${wirelessClients.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.2f),
                            selectedLabelColor = KeeneticColors.Primary,
                            containerColor = KeeneticColors.Surface,
                            labelColor = KeeneticColors.TextSecondary
                        )
                    )
                    FilterChip(
                        selected = selectedBandFilter == "2.4",
                        onClick = { selectedBandFilter = "2.4" },
                        label = { Text("2.4 ГГц ($clients24Count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF59E0B),
                            containerColor = KeeneticColors.Surface,
                            labelColor = KeeneticColors.TextSecondary
                        )
                    )
                    FilterChip(
                        selected = selectedBandFilter == "5",
                        onClick = { selectedBandFilter = "5" },
                        label = { Text("5 ГГц ($clients5Count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.2f),
                            selectedLabelColor = KeeneticColors.Primary,
                            containerColor = KeeneticColors.Surface,
                            labelColor = KeeneticColors.TextSecondary
                        )
                    )
                }
            }

            // Wireless Clients List
            if (filteredClients.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Devices,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Нет подключённых беспроводных устройств",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KeeneticColors.TextSecondary
                            )
                            Text(
                                "Подключите телефон или ноутбук к сети Wi-Fi",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredClients, key = { it.mac }) { client ->
                    WirelessClientRow(
                        client = client,
                        onClick = { selectedClientForDetails = client }
                    )
                }
            }

            // 3. WifiStation Section (RCI 'interface WifiStation')
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Клиентский режим (WifiStation)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                    }

                    Text(
                        text = "RCI: interface WifiStation",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            item {
                WifiStationCard(
                    stationStatus = wifiStation,
                    isConfigExpanded = showStationConfig,
                    onToggleExpand = { showStationConfig = !showStationConfig },
                    onTogglePower = { enabled ->
                        viewModel.toggleWifiStation(wifiStation.id, enabled)
                    },
                    selectedRadio = stationRadioBand,
                    onRadioSelected = { stationRadioBand = it },
                    targetSsid = stationTargetSsid,
                    onSsidChange = { stationTargetSsid = it },
                    targetPassword = stationTargetPassword,
                    onPasswordChange = { stationTargetPassword = it },
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                    isScanning = isWifiScanning,
                    scanResults = scanResults,
                    onScan = { viewModel.scanWifiSiteSurvey(stationRadioBand) },
                    onSelectDiscoveredSsid = { ssid -> stationTargetSsid = ssid },
                    onConnect = {
                        if (stationTargetSsid.isNotBlank()) {
                            viewModel.connectWifiStation(stationRadioBand, stationTargetSsid, stationTargetPassword)
                        }
                    },
                    onDisconnect = {
                        viewModel.disconnectWifiStation(stationRadioBand)
                    }
                )
            }
        }
    }

    // Client Details Modal Dialog
    if (selectedClientForDetails != null) {
        val client = selectedClientForDetails!!
        AlertDialog(
            onDismissRequest = { selectedClientForDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        getDeviceIcon(client.displayName, client.hostname),
                        contentDescription = null,
                        tint = if (client.band.contains("5")) KeeneticColors.Primary else Color(0xFFF59E0B)
                    )
                    Text(
                        client.displayName.ifBlank { client.hostname },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "IP-адрес", value = client.ip ?: "—")
                    DetailRow(label = "MAC-адрес", value = client.mac, isMonospace = true)
                    DetailRow(label = "Диапазон", value = client.band)
                    DetailRow(
                        label = "Уровень сигнала",
                        value = "${client.rssi ?: "—"} dBm (${getRssiLabel(client.rssi)})"
                    )
                    DetailRow(
                        label = "Скорость передачи (Tx)",
                        value = formatRate(client.txRateKbps)
                    )
                    DetailRow(
                        label = "Скорость приёма (Rx)",
                        value = formatRate(client.rxRateKbps)
                    )
                    DetailRow(label = "Точка доступа", value = client.ap.ifBlank { "Home" })
                    DetailRow(label = "Сеть SSID", value = client.ssid)
                    DetailRow(label = "Стандарт", value = client.mode.ifBlank { "802.11" })
                    DetailRow(
                        label = "Статус доступа",
                        value = if (client.isBlocked) "Заблокирован" else "Разрешён",
                        valueColor = if (client.isBlocked) KeeneticColors.Error else KeeneticColors.Success
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wakeOnLan(client.mac)
                        selectedClientForDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wake-on-LAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForDetails = null }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            },
            containerColor = KeeneticColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // WiFi Band Configuration Dialog
    editingNetwork?.let { net ->
        val is24 = net.band.contains("2.4") || net.id.contains("WifiMaster1")
        var newSsid by remember(net.id) { mutableStateOf(net.ssid) }
        var newPassword by remember(net.id) { mutableStateOf("") }
        var isPassVisible by remember(net.id) { mutableStateOf(false) }
        var selectedChannel by remember(net.id) { mutableStateOf(if (net.channel > 0) net.channel.toString() else "auto") }
        var txPower by remember(net.id) { mutableStateOf("100") }

        val availableChannels = if (is24) listOf("auto", "1", "6", "11") else listOf("auto", "36", "40", "44", "48", "149", "153")

        AlertDialog(
            onDismissRequest = { editingNetwork = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = if (is24) Color(0xFFF59E0B) else KeeneticColors.Primary)
                    Text(
                        if (is24) "Настройки Wi-Fi 2.4 ГГц" else "Настройки Wi-Fi 5 ГГц",
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSsid,
                        onValueChange = { newSsid = it },
                        label = { Text("Имя сети (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Пароль WPA-PSK (пусто если без изменений)") },
                        singleLine = true,
                        visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPassVisible = !isPassVisible }) {
                                Icon(
                                    if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Канал вещания:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableChannels.take(4).forEach { ch ->
                            FilterChip(
                                selected = selectedChannel == ch,
                                onClick = { selectedChannel = ch },
                                label = { Text(if (ch == "auto") "Авто" else "№$ch") }
                            )
                        }
                    }

                    Text("Мощность передатчика (Tx-Power):", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("100", "75", "50", "25").forEach { pwr ->
                            FilterChip(
                                selected = txPower == pwr,
                                onClick = { txPower = pwr },
                                label = { Text("$pwr%") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chInt = if (selectedChannel == "auto") null else selectedChannel.toIntOrNull()
                        val pwrInt = txPower.toIntOrNull()
                        viewModel.updateWifiNetworkConfig(
                            band = net.band,
                            newSsid = newSsid,
                            newPassword = newPassword,
                            channel = chInt,
                            txPowerPercent = pwrInt
                        )
                        editingNetwork = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNetwork = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}

/**
 * Visual Interactive Card for 2.4 GHz and 5 GHz Bands
 */
@Composable
private fun WifiBandCard(
    bandLabel: String,
    bandTag: String,
    accentColor: Color,
    ssid: String,
    isEnabled: Boolean,
    channel: Int,
    security: String,
    connectedClientsCount: Int,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isEnabled) accentColor.copy(alpha = 0.35f) else KeeneticColors.Divider
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) accentColor.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isEnabled) accentColor else KeeneticColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = bandLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KeeneticColors.TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = bandTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) KeeneticColors.Success else KeeneticColors.TextSecondary)
                            )
                            Text(
                                text = if (isEnabled) "Активен" else "Отключен",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnabled) KeeneticColors.Success else KeeneticColors.TextSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Tune, contentDescription = "Настройки диапазона", tint = accentColor)
                    }
                    // Dedicated Switch for toggling this band
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = KeeneticColors.TextSecondary,
                            uncheckedTrackColor = KeeneticColors.SurfaceElevated
                        )
                    )
                }
            }

            HorizontalDivider(color = KeeneticColors.Divider)

            // Band specs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SSID сети",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Text(
                        text = ssid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KeeneticColors.TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Канал / Защита",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Text(
                        text = "Канал $channel • $security",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextPrimary
                    )
                }
            }

            // Clients count badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(KeeneticColors.SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Подключено клиентов:",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                Text(
                    text = "$connectedClientsCount устр.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (connectedClientsCount > 0) accentColor else KeeneticColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Wireless Client Item Row (parsed from RCI 'show wifi' & 'show associations')
 */
@Composable
private fun WirelessClientRow(
    client: WirelessClient,
    onClick: () -> Unit
) {
    val is5G = client.band.contains("5")
    val bandColor = if (is5G) KeeneticColors.Primary else Color(0xFFF59E0B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Signal and device icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bandColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(client.displayName, client.hostname),
                    contentDescription = null,
                    tint = bandColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Client info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = client.displayName.ifBlank { client.hostname },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KeeneticColors.TextPrimary,
                        maxLines = 1
                    )
                    // Band chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bandColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = client.band,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = bandColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${client.ip ?: "—"}  •  ${client.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = KeeneticColors.TextSecondary
                )

                // Transfer rate / speed indicator
                if (client.txRateKbps > 0 || client.rxRateKbps > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "↓ ${formatRate(client.rxRateKbps)}  ↑ ${formatRate(client.txRateKbps)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            // Signal strength display
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val rssi = client.rssi
                val (rssiColor, rssiIcon) = getRssiVisuals(rssi)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (rssi != null) "$rssi dBm" else "—",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = rssiColor
                    )
                    Icon(
                        imageVector = rssiIcon,
                        contentDescription = null,
                        tint = rssiColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = getRssiLabel(rssi),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

/**
 * WifiStation Card for viewing and configuring client mode
 */
@Composable
private fun WifiStationCard(
    stationStatus: com.keenetic.local.api.WifiStationStatus,
    isConfigExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTogglePower: (Boolean) -> Unit,
    selectedRadio: String,
    onRadioSelected: (String) -> Unit,
    targetSsid: String,
    onSsidChange: (String) -> Unit,
    targetPassword: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isScanning: Boolean,
    scanResults: List<WifiSiteSurveyEntry>,
    onScan: () -> Unit,
    onSelectDiscoveredSsid: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Divider))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status & switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (stationStatus.isUp) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = if (stationStatus.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Клиент Wi-Fi (WifiStation)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            text = if (stationStatus.isUp) {
                                if (!stationStatus.connectedSsid.isNullOrBlank()) "Подключен к «${stationStatus.connectedSsid}»" else "Интерфейс активен"
                            } else "Выключен",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stationStatus.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary
                        )
                    }
                }

                Switch(
                    checked = stationStatus.isUp,
                    onCheckedChange = onTogglePower,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KeeneticColors.Success,
                        checkedTrackColor = KeeneticColors.Success.copy(alpha = 0.4f)
                    )
                )
            }

            // Connection metrics if active
            if (stationStatus.isUp && !stationStatus.connectedSsid.isNullOrBlank()) {
                HorizontalDivider(color = KeeneticColors.Divider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Сеть", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                        Text(stationStatus.connectedSsid ?: "—", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                    }
                    Column {
                        Text("IP-адрес", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                        Text(stationStatus.ip ?: "DHCP...", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Сигнал", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                        Text(
                            if (stationStatus.rssi != null) "${stationStatus.rssi} dBm" else "В норме",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.Success
                        )
                    }
                }
            }

            // Expand / collapse setup panel button
            OutlinedButton(
                onClick = onToggleExpand,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KeeneticColors.Primary
                )
            ) {
                Icon(
                    imageVector = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isConfigExpanded) "Скрыть настройки подключения" else "Настроить подключение к сети")
            }

            // Expanded setup controls
            AnimatedVisibility(
                visible = isConfigExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KeeneticColors.SurfaceElevated)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Подключение к внешней сети",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )

                    // Radio band selector (2.4 GHz vs 5 GHz)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRadio == "WifiMaster0",
                            onClick = { onRadioSelected("WifiMaster0") },
                            label = { Text("5 ГГц (Master0)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.25f),
                                selectedLabelColor = KeeneticColors.Primary
                            )
                        )
                        FilterChip(
                            selected = selectedRadio == "WifiMaster1",
                            onClick = { onRadioSelected("WifiMaster1") },
                            label = { Text("2.4 ГГц (Master1)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFFF59E0B)
                            )
                        )
                    }

                    // Site survey scan button
                    OutlinedButton(
                        onClick = onScan,
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сканирование эфира...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Поиск доступных сетей Wi-Fi")
                        }
                    }

                    // Discovered SSIDs list
                    if (scanResults.isNotEmpty()) {
                        Text(
                            text = "Найдено сетей (${scanResults.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scanResults.take(6).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(KeeneticColors.Surface)
                                        .clickable { onSelectDiscoveredSsid(entry.ssid) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = KeeneticColors.Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            entry.ssid,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = KeeneticColors.TextPrimary
                                        )
                                    }
                                    Text(
                                        "${entry.rssi} dBm (кан. ${entry.channel})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Target SSID Input
                    OutlinedTextField(
                        value = targetSsid,
                        onValueChange = onSsidChange,
                        label = { Text("Имя сети (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider,
                            focusedTextColor = KeeneticColors.TextPrimary,
                            unfocusedTextColor = KeeneticColors.TextPrimary
                        )
                    )

                    // Target Password Input
                    OutlinedTextField(
                        value = targetPassword,
                        onValueChange = onPasswordChange,
                        label = { Text("Пароль Wi-Fi") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = KeeneticColors.TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider,
                            focusedTextColor = KeeneticColors.TextPrimary,
                            unfocusedTextColor = KeeneticColors.TextPrimary
                        )
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnect,
                            enabled = targetSsid.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                        ) {
                            Text("Подключить")
                        }

                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error)
                        ) {
                            Text("Отключить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    valueColor: Color = KeeneticColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun getDeviceIcon(displayName: String, hostname: String): ImageVector {
    val name = "$displayName $hostname".lowercase()
    return when {
        name.contains("iphone") || name.contains("phone") || name.contains("pixel") || name.contains("galaxy") || name.contains("xiaomi") -> Icons.Default.Smartphone
        name.contains("macbook") || name.contains("laptop") || name.contains("pc") || name.contains("thinkpad") -> Icons.Default.Laptop
        name.contains("tv") || name.contains("oled") || name.contains("playstation") || name.contains("xbox") -> Icons.Default.Devices
        else -> Icons.Default.Devices
    }
}

private fun getRssiVisuals(rssi: Int?): Pair<Color, ImageVector> {
    if (rssi == null) return Pair(KeeneticColors.TextSecondary, Icons.Default.Wifi)
    return when {
        rssi >= -60 -> Pair(KeeneticColors.Success, Icons.Default.Wifi)
        rssi >= -72 -> Pair(Color(0xFFF59E0B), Icons.Default.Wifi)
        else -> Pair(KeeneticColors.Error, Icons.Default.Wifi)
    }
}

private fun getRssiLabel(rssi: Int?): String {
    if (rssi == null) return "Неизвестно"
    return when {
        rssi >= -55 -> "Отличный"
        rssi >= -68 -> "Хороший"
        rssi >= -78 -> "Средний"
        else -> "Слабый"
    }
}

private fun formatRate(kbps: Long): String {
    return when {
        kbps <= 0 -> "—"
        kbps >= 1000000 -> "%.1f Гб/с".format(kbps / 1000000.0)
        kbps >= 1000 -> "%.0f Мб/с".format(kbps / 1000.0)
        else -> "$kbps Кб/с"
    }
}
