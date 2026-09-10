package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SystemAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val environmentInfo by viewModel.environmentInfo.collectAsState()
    val ntpStatus by viewModel.ntpStatus.collectAsState()
    val ledConfig by viewModel.ledConfig.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val systemMode by viewModel.systemMode.collectAsState()
    var hostnameInput by remember { mutableStateOf(systemInfo?.hostname ?: "Keenetic") }
    var showNtpDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showLedModeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSystemInfo()
        viewModel.loadEnvironmentInfo()
        viewModel.loadNtpStatus()
        viewModel.loadLedConfig()
        viewModel.loadBackupStatus()
        viewModel.loadSystemMode()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Settings, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Системные настройки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        // Hostname
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Имя хоста", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = hostnameInput,
                        onValueChange = { hostnameInput = it },
                        label = { Text("Hostname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.setHostname(hostnameInput) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                    ) { Text("Сохранить", color = KeeneticColors.TextPrimary) }
                }
            }
        }

        // Environment
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Состояние", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    InfoRow("Температура", "${environmentInfo.temperature}°C")
                    InfoRow("Скорость вентилятора", "${environmentInfo.fanSpeed} RPM")
                    InfoRow("Uptime", formatUptime(systemInfo?.uptime ?: environmentInfo.uptime))
                }
            }
        }

        // NTP
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Время и NTP", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("NTP синхронизация", color = KeeneticColors.TextPrimary)
                        Switch(checked = ntpStatus.enabled, onCheckedChange = { viewModel.setNtpEnabled(it) })
                    }
                    InfoRow("Сервер NTP", ntpStatus.server.ifBlank { "0.pool.ntp.org (по умолчанию)" }, onClick = { showNtpDialog = true })
                    InfoRow("Часовой пояс", systemInfo?.clockTime?.ifBlank { "MSK / UTC" } ?: "Изменить часовой пояс", onClick = { showTimezoneDialog = true })
                    InfoRow("Последняя синхронизация", ntpStatus.lastSync)
                }
            }
        }

        // LED
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LightMode, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Индикаторы (LED)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Индикаторы включены", color = KeeneticColors.TextPrimary)
                        Switch(checked = ledConfig.enabled, onCheckedChange = { viewModel.setLedEnabled(it) })
                    }
                    InfoRow("Режим работы LED", ledConfig.mode.ifBlank { "Стандартный" }, onClick = { showLedModeDialog = true })
                }
            }
        }

        // Mode
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Режим работы", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    InfoRow("Текущий режим", systemMode.mode)
                }
            }
        }

        // Backup
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Резервная копия", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    InfoRow("Файл", backupStatus.filename)
                    InfoRow("Размер", formatBytes(backupStatus.size))
                    InfoRow("Дата", backupStatus.date)
                }
            }
        }
    }
    if (showNtpDialog) {
        var ntpServerInput by remember { mutableStateOf(ntpStatus.server) }
        AlertDialog(
            onDismissRequest = { showNtpDialog = false },
            title = { Text("Настройка сервера NTP", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Укажите доменное имя или IP-адрес сервера точного времени:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    OutlinedTextField(
                        value = ntpServerInput,
                        onValueChange = { ntpServerInput = it },
                        label = { Text("NTP сервер") },
                        placeholder = { Text("pool.ntp.org или time.google.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setNtpServer(ntpServerInput)
                        showNtpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNtpDialog = false }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    if (showTimezoneDialog) {
        var tzInput by remember { mutableStateOf("MSK-3") }
        AlertDialog(
            onDismissRequest = { showTimezoneDialog = false },
            title = { Text("Выбор часового пояса", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Укажите смещение или имя часового пояса (например MSK-3, UTC, EET-2):", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    OutlinedTextField(
                        value = tzInput,
                        onValueChange = { tzInput = it },
                        label = { Text("Timezone (например MSK-3)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setTimezone(tzInput)
                        showTimezoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimezoneDialog = false }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    if (showLedModeDialog) {
        AlertDialog(
            onDismissRequest = { showLedModeDialog = false },
            title = { Text("Режим индикаторов (LED)", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("enabled" to "Стандартный (все индикаторы)", "night" to "Ночной режим (приглушённые)", "disabled" to "Выключены все").forEach { (modeKey, modeName) ->
                        Button(
                            onClick = {
                                viewModel.setLedMode(modeKey)
                                showLedModeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ledConfig.mode == modeKey) KeeneticColors.Primary else KeeneticColors.SurfaceElevated,
                                contentColor = if (ledConfig.mode == modeKey) KeeneticColors.Background else KeeneticColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(modeName)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLedModeDialog = false }) { Text("Закрыть", color = KeeneticColors.TextSecondary) }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = if (onClick != null) KeeneticColors.Primary else KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
            if (onClick != null) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = KeeneticColors.Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return "${days}д ${hours}ч ${minutes}м"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}
