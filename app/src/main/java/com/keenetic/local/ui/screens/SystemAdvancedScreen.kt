package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.DialogForm
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SystemAdvancedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val environmentInfo by viewModel.environmentInfo.collectAsState()
    val ntpStatus by viewModel.ntpStatus.collectAsState()
    val ledConfig by viewModel.ledConfig.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val systemMode by viewModel.systemMode.collectAsState()
    val backupActionMessage by viewModel.backupActionMessage.collectAsState()

    var showHostnameDialog by remember { mutableStateOf(false) }
    var showNtpDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showLedModeDialog by remember { mutableStateOf(false) }

    fun refresh() {
        viewModel.loadSystemInfo()
        viewModel.loadEnvironmentInfo()
        viewModel.loadNtpStatus()
        viewModel.loadLedConfig()
        viewModel.loadBackupStatus()
        viewModel.loadSystemMode()
    }

    LaunchedEffect(Unit) { refresh() }

    val ledLoaded = ledConfig.mode.isNotBlank()
    val ledsOn = ledConfig.mode == "on"

    SectionScaffold(
        title = "Системные настройки",
        subtitle = "имя хоста, NTP, индикаторы, копия",
        onBack = onBack,
        onRefresh = { refresh() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "identity") {
                SectionCard(title = "Имя устройства", icon = Icons.Default.Settings) {
                    EditableRow(
                        label = "Имя хоста (hostname)",
                        value = systemInfo?.hostname ?: "нет данных",
                        monospaceValue = true,
                        onClick = { showHostnameDialog = true }
                    )
                    InfoRow("Домен", systemInfo?.domainName?.ifBlank { "local" } ?: "local")
                    InfoRow("Режим работы", systemMode.mode.ifBlank { "—" })
                }
            }

            item(key = "environment") {
                SectionCard(title = "Состояние", icon = Icons.Default.Thermostat) {
                    InfoRow("Температура", "${environmentInfo.temperature}°C")
                    InfoRow("Скорость вентилятора", "${environmentInfo.fanSpeed} RPM")
                    InfoRow("Время работы", formatUptime(systemInfo?.uptime ?: environmentInfo.uptime))
                }
            }

            item(key = "ntp") {
                SectionCard(title = "Время и NTP", icon = Icons.Default.Schedule) {
                    SwitchRow(
                        label = "NTP-синхронизация",
                        checked = ntpStatus.enabled,
                        onCheckedChange = { viewModel.setNtpEnabled(it) }
                    )
                    RowDivider()
                    EditableRow(
                        label = "Сервер NTP",
                        value = ntpStatus.server.ifBlank { "0.pool.ntp.org" },
                        onClick = { showNtpDialog = true }
                    )
                    EditableRow(
                        label = "Часовой пояс",
                        value = systemInfo?.clockTime?.ifBlank { "MSK / UTC" } ?: "MSK / UTC",
                        onClick = { showTimezoneDialog = true }
                    )
                    InfoRow(
                        "Последняя синхронизация",
                        ntpStatus.lastSync.ifBlank { "нет данных" },
                        valueColor = KeeneticColors.TextSecondary
                    )
                }
            }

            item(key = "led") {
                SectionCard(title = "Индикаторы (LED)", icon = Icons.Default.LightMode) {
                    SwitchRow(
                        label = "Индикаторы включены",
                        checked = ledsOn,
                        enabled = ledLoaded,
                        onCheckedChange = { viewModel.setLedEnabled(it) },
                        description = if (ledLoaded) null else "режим не загружен"
                    )
                    EditableRow(
                        label = "Режим работы LED",
                        value = ledModeLabel(ledConfig.mode),
                        enabled = ledLoaded,
                        onClick = { showLedModeDialog = true }
                    )
                    if (ledLoaded && !ledsOn && ledConfig.schedule.isNotBlank()) {
                        InfoRow("Расписание LED", ledConfig.schedule, valueColor = KeeneticColors.TextSecondary)
                    }
                }
            }

            item(key = "backup") {
                SectionCard(title = "Резервная копия", icon = Icons.Default.Backup) {
                    InfoRow("Файл", backupStatus.filename.ifBlank { "нет данных" })
                    InfoRow("Размер", if (backupStatus.size > 0) "${backupStatus.size} Б" else "—")
                    InfoRow("Дата", backupStatus.date.ifBlank { "—" })
                    RowDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Создать копию")
                        }
                        OutlinedButton(
                            onClick = { viewModel.downloadBackup() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Скачать")
                        }
                    }
                    if (!backupActionMessage.isNullOrBlank()) {
                        Text(
                            backupActionMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            item(key = "mode") {
                SectionCard(title = "Режим работы", icon = Icons.Default.Router) {
                    InfoRow("Текущий режим", systemMode.mode.ifBlank { "—" })
                }
            }
        }
    }

    if (showHostnameDialog) {
        var value by remember { mutableStateOf(systemInfo?.hostname ?: "") }
        FormDialog(
            title = "Имя хоста",
            confirmLabel = "Сохранить",
            confirmEnabled = value.isNotBlank(),
            onDismiss = { showHostnameDialog = false },
            onConfirm = {
                viewModel.setHostname(value)
                showHostnameDialog = false
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Hostname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showNtpDialog) {
        var value by remember { mutableStateOf(ntpStatus.server) }
        FormDialog(
            title = "Сервер точного времени",
            confirmLabel = "Сохранить",
            confirmEnabled = value.isNotBlank(),
            onDismiss = { showNtpDialog = false },
            onConfirm = {
                viewModel.setNtpServer(value)
                showNtpDialog = false
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("NTP-сервер") },
                placeholder = { Text("pool.ntp.org или time.google.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTimezoneDialog) {
        var value by remember { mutableStateOf("MSK-3") }
        FormDialog(
            title = "Часовой пояс",
            confirmLabel = "Сохранить",
            confirmEnabled = value.isNotBlank(),
            onDismiss = { showTimezoneDialog = false },
            onConfirm = {
                viewModel.setTimezone(value)
                showTimezoneDialog = false
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Timezone (например MSK-3)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showLedModeDialog) {
        OptionPickerDialog(
            title = "Режим индикаторов (LED)",
            // Веб-морда этой модели (KN-2311) отдаёт только режимы on/all.
            options = listOf(
                "on" to "Все индикаторы включены",
                "all" to "Все индикаторы выключены"
            ),
            selectedKey = ledConfig.mode,
            onSelect = { mode -> viewModel.setLedMode(mode) },
            onDismiss = { showLedModeDialog = false }
        )
    }
}

private fun ledModeLabel(mode: String): String = when (mode) {
    "on" -> "Все включены"
    "all" -> "Все выключены"
    "front" -> "Основные выключены"
    "back" -> "Индикаторы портов выключены"
    "" -> "нет данных"
    else -> "Неизвестный режим: $mode"
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return "${days}д ${hours}ч ${minutes}м"
}
