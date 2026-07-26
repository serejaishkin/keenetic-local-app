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
