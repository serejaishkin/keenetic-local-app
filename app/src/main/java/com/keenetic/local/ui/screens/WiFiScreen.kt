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
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun WiFiScreen(viewModel: RouterViewModel) {
    val interfaces by viewModel.interfaces.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val wifiInterfaces = interfaces.filter {
        it.id?.contains("Wifi", ignoreCase = true) == true ||
        it.id?.contains("AccessPoint", ignoreCase = true) == true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Wi-Fi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(wifiInterfaces.size) { index ->
                val iface = wifiInterfaces[index]
                WiFiCard(iface, viewModel)
            }
            if (wifiInterfaces.isEmpty()) {
                item {
                    Text(
                        "Нет данных. Нажмите 🔄 для обновления.",
                        color = KeeneticColors.TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (isLoading && wifiInterfaces.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun WiFiCard(iface: com.keenetic.local.api.InterfaceInfo, viewModel: RouterViewModel) {
    val isUp = iface.up == true
    var confirmToggle by remember { mutableStateOf(false) }

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
                        tint = if (isUp) KeeneticColors.Accent else KeeneticColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                iface.id?.contains("Guest", ignoreCase = true) == true -> "Гостевая сеть"
                                iface.id?.contains("Master", ignoreCase = true) == true -> "Основная сеть"
                                else -> iface.id ?: "Wi-Fi"
                            },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isUp) "Активна" else "Выключена",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUp) KeeneticColors.Accent else KeeneticColors.TextSecondary
                        )
                    }
                }
                Switch(
                    checked = isUp,
                    onCheckedChange = { confirmToggle = true },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KeeneticColors.Accent,
                        checkedTrackColor = KeeneticColors.Accent.copy(alpha = 0.5f)
                    )
                )
            }

            if (isUp && iface.connected != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Подключено: ${iface.connected}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }

    if (confirmToggle) {
        AlertDialog(
            onDismissRequest = { confirmToggle = false },
            title = { Text("Подтвердите действие") },
            text = { Text("${if (isUp) "Выключить" else "Включить"} ${iface.id}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleInterface(iface.id ?: "", !isUp)
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
