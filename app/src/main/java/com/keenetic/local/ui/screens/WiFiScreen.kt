package com.keenetic.local.ui.screens

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
                    viewModel.toggleInterface(network.id, !network.enabled)
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
