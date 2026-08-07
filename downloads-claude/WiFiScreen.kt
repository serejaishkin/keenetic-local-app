package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WifiNetwork
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun WiFiScreen(viewModel: RouterViewModel) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInterfaces()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Wi-Fi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoading && wifiNetworks.isEmpty()) {
            Text(
                text = "Точки доступа не найдены. Потяните для обновления или проверьте соединение.",
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextSecondary
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(wifiNetworks.size) { index ->
                WiFiCard(wifiNetworks[index], viewModel)
            }
            item {
                WifiClientModeCard(viewModel)
            }
            item {
                SiteSurveyCard(viewModel)
            }
        }
    }

    if (isLoading && wifiNetworks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun WiFiCard(network: WifiNetwork, viewModel: RouterViewModel) {
    var confirmToggle by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (network.enabled) KeeneticColors.Accent else KeeneticColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (network.guest) "${network.ssid} (гостевая)" else network.ssid,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${network.band} · ${if (network.enabled) "Активна" else "Выключена"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (network.enabled) KeeneticColors.Accent else KeeneticColors.TextSecondary
                        )
                    }
                }
                Switch(
                    checked = network.enabled,
                    onCheckedChange = { confirmToggle = true },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KeeneticColors.Accent,
                        checkedTrackColor = KeeneticColors.Accent.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Защита: ${network.security}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                TextButton(onClick = { showPasswordDialog = true }) {
                    Text("Сменить пароль")
                }
            }
        }
    }

    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Новый пароль для «${network.ssid}»") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Пароль (минимум 8 символов)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Сеть: ${if (network.guest) "Guest" else "Home"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val networkId = if (network.guest) "Guest" else "Home"
                        viewModel.setWifiPassword(networkId, newPassword)
                        showPasswordDialog = false
                    },
                    enabled = newPassword.length >= 8
                ) {
                    Text("Сохранить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (confirmToggle) {
        AlertDialog(
            onDismissRequest = { confirmToggle = false },
            title = { Text("Подтвердите действие") },
            text = { Text("${if (network.enabled) "Выключить" else "Включить"} сеть «${network.ssid}»?") },
            confirmButton = {
                TextButton(onClick = {
                    val networkId = if (network.guest) "Guest" else "Home"
                    viewModel.updateWifiNetwork(networkId = networkId, enable = !network.enabled)
                    confirmToggle = false
                }) {
                    Text("Да", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmToggle = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun WifiClientModeCard(viewModel: RouterViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var radioBand by remember { mutableStateOf("WifiMaster0") } // 2.4 ГГц по умолчанию
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Wi-Fi как клиент (мост/повторитель)", fontWeight = FontWeight.Medium)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Роутер подключится к чужой Wi-Fi сети как обычное устройство (режим WifiStation). Полезно как мост/повторитель.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = radioBand == "WifiMaster0",
                        onClick = { radioBand = "WifiMaster0" },
                        label = { Text("2.4 ГГц") }
                    )
                    FilterChip(
                        selected = radioBand == "WifiMaster1",
                        onClick = { radioBand = "WifiMaster1" },
                        label = { Text("5 ГГц") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val scanResults by viewModel.scanResults.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val error by viewModel.error.collectAsState()
                var hasScanned by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { hasScanned = true; viewModel.clearError(); viewModel.scanWifiNetworks(radioBand) },
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сканирование...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сканировать сети")
                    }
                }

                if (hasScanned && !isScanning) {
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ошибка: $error",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.Error
                        )
                    } else if (scanResults.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Сети не найдены. Попробуй другой диапазон (2.4/5 ГГц) или подожди - модуль мог быть занят.",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }

                if (scanResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 220.dp)) {
                        scanResults.forEach { net ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ssid = net.essid ?: "" }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(net.essid ?: "—", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${net.encryption ?: "?"} · канал ${net.channel ?: "?"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                                Text(
                                    "${net.rssi ?: "—"} дБм",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Имя сети (SSID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.connectWifiClient(radioBand, ssid, password) },
                        enabled = ssid.isNotBlank() && password.length >= 8,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Подключить")
                    }
                    OutlinedButton(
                        onClick = { viewModel.disconnectWifiClient(radioBand) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отключить")
                    }
                }
            }
        }
    }
}

/**
 * Скан эфира ("Монитор Wi-Fi" на сайте) - соседние точки доступа на том же
 * канале/диапазоне. ПОДТВЕРЖДЕНО HAR (06.08): читается НЕ простым GET, а
 * батч-POST на /rci/: {"show":{"site-survey":{"name":"WifiMaster0"}}}.
 * Это объясняет, почему раньше поиск строки "show.site-survey" в JS-бандле
 * ничего не находил - путь строится динамически, не литеральной строкой.
 * Диапазон 5ГГц (WifiMaster1) не проверен - добавлена кнопка на оба.
 */
@Composable
private fun SiteSurveyCard(viewModel: RouterViewModel) {
    val result by viewModel.siteSurveyRaw.collectAsState()
    var lastRequested by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Соседние сети (скан эфира)", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Разовый скан - на некоторых прошивках занимает несколько секунд и может кратковременно повлиять на связь с текущими клиентами",
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lastRequested = "WifiMaster0"; viewModel.scanSiteSurvey("WifiMaster0") }) {
                    Text("2.4 ГГц")
                }
                OutlinedButton(onClick = { lastRequested = "WifiMaster1"; viewModel.scanSiteSurvey("WifiMaster1") }) {
                    Text("5 ГГц")
                }
            }
            if (lastRequested != null) {
                Spacer(modifier = Modifier.height(12.dp))
                com.keenetic.local.ui.screens.common.RawJsonCard(
                    title = "Результат скана ($lastRequested)",
                    state = result,
                    emptyText = "Соседние сети не найдены"
                )
            }
        }
    }
}
