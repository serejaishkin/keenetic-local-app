package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WifiSiteSurveyEntry
import com.keenetic.local.api.WifiStationStatus
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

private const val RADIO_24 = "WifiMaster0"
private const val RADIO_5 = "WifiMaster1"

@Composable
fun WifiRepeaterScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val station by viewModel.wifiStationStatus.collectAsState()
    val scanResults by viewModel.wifiScanResults.collectAsState()
    val isScanning by viewModel.isWifiScanning.collectAsState()
    val actionMessage by viewModel.wifiActionMessage.collectAsState()

    var selectedRadio by remember { mutableStateOf(RADIO_24) }
    var targetSsid by remember { mutableStateOf("") }
    var targetPassword by remember { mutableStateOf("") }
    var showConnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadWifiData() }

    val isConnected = station.isUp && selectedRadio in (station.masterRadio ?: "")

    SectionScaffold(
        title = "Репитер (WISP)",
        subtitle = "Подключение через соседнюю Wi-Fi сеть",
        onBack = onBack,
        onRefresh = { viewModel.loadWifiData() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                RepeaterStatusCard(
                    station = station,
                    onToggle = { enabled -> viewModel.toggleWifiStation(station.id, enabled) }
                )
            }

            item(key = "radio") {
                SectionCard(title = "Диапазон (WISP)", icon = Icons.Default.SettingsInputAntenna) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRadio == RADIO_24,
                            onClick = { selectedRadio = RADIO_24 },
                            label = { Text("2.4 ГГц (Master0)") }
                        )
                        FilterChip(
                            selected = selectedRadio == RADIO_5,
                            onClick = { selectedRadio = RADIO_5 },
                            label = { Text("5 ГГц (Master1)") }
                        )
                    }
                }
            }

            item(key = "connect") {
                SectionCard(
                    title = "Настройка беспроводного подключения",
                    icon = Icons.Default.Wifi
                ) {
                    TextButton(
                        onClick = { viewModel.scanWifiSiteSurvey(selectedRadio) },
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isScanning) "Сканирование эфира…" else "Обзор сетей",
                            color = KeeneticColors.Primary
                        )
                    }
                    InfoRow(
                        label = "Имя сети (SSID)",
                        value = targetSsid.ifBlank { "не выбрано" },
                        monospace = true
                    )
                    RowDivider()
                    EditableRow(
                        label = "Параметры подключения",
                        value = "изменить",
                        onClick = { showConnectDialog = true }
                    )
                    RowDivider()
                    if (isConnected) {
                        TextButton(onClick = { viewModel.disconnectWifiStation(selectedRadio) }) {
                            Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Отключиться", color = KeeneticColors.Error)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.connectWifiStation(selectedRadio, targetSsid, targetPassword)
                                showConnectDialog = false
                            },
                            enabled = targetSsid.isNotBlank()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Подключиться", color = KeeneticColors.Primary)
                        }
                    }
                }
            }

            if (scanResults.isNotEmpty()) {
                item(key = "scan-header") {
                    SubGroupHeader("Найденные сети", scanResults.size)
                }
                items(
                    scanResults.take(15),
                    key = { it.ssid + it.channel + it.bssid }
                ) { entry ->
                    SurveyRow(
                        entry = entry,
                        selected = entry.ssid == targetSsid,
                        onClick = { targetSsid = entry.ssid }
                    )
                }
            }

            if (!actionMessage.isNullOrBlank()) {
                item(key = "message") {
                    SectionCard {
                        Text(
                            actionMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.Primary
                        )
                    }
                }
            }
        }
    }

    if (showConnectDialog) {
        ConnectDialog(
            initialSsid = targetSsid,
            initialPassword = targetPassword,
            onDismiss = { showConnectDialog = false },
            onSave = { ssid, password ->
                targetSsid = ssid
                targetPassword = password
                showConnectDialog = false
            }
        )
    }
}

@Composable
private fun RepeaterStatusCard(station: WifiStationStatus, onToggle: (Boolean) -> Unit) {
    SectionCard(
        title = "Клиент Wi-Fi (WifiStation)",
        icon = Icons.Default.Wifi,
        subtitle = if (station.isUp) {
            if (station.connectedSsid.isNullOrBlank()) "Интерфейс активен" else "Подключен к «${station.connectedSsid}»"
        } else {
            "Выключен"
        }
    ) {
        SwitchRow(
            label = "Wi-Fi-клиент включён",
            checked = station.isUp,
            onCheckedChange = onToggle
        )
        RowDivider()
        InfoRow("Состояние", station.state)
        if (!station.connectedSsid.isNullOrBlank()) {
            InfoRow("Имя сети (SSID)", station.connectedSsid!!, monospace = true)
        }
        if (!station.ip.isNullOrBlank()) {
            InfoRow("IP-адрес", station.ip!!, monospace = true)
        }
        if (!station.mac.isNullOrBlank()) {
            InfoRow("BSSID (MAC-адрес)", station.mac!!, monospace = true)
        }
        if (station.rssi != null) {
            InfoRow(
                "Сигнал",
                "${station.rssi} dBm",
                valueColor = if (station.rssi >= -60) KeeneticColors.Success else KeeneticColors.Warning
            )
        }
    }
}

@Composable
private fun SurveyRow(entry: WifiSiteSurveyEntry, selected: Boolean, onClick: () -> Unit) {
    SectionCard(
        title = entry.ssid,
        subtitle = "канал ${entry.channel} · ${entry.band} · ${entry.encryption.ifBlank { "без защиты" }}",
        icon = Icons.Default.Wifi
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoRow(
                label = "Уровень сигнала",
                value = "${entry.rssi} dBm",
                monospace = true,
                valueColor = if (selected) KeeneticColors.Primary else KeeneticColors.TextPrimary
            )
        }
    }
}

@Composable
private fun ConnectDialog(
    initialSsid: String,
    initialPassword: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var ssid by remember { mutableStateOf(initialSsid) }
    var password by remember { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    FormDialog(
        title = "Параметры подключения",
        confirmLabel = "Сохранить",
        confirmEnabled = ssid.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(ssid.trim(), password) }
    ) {
        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("Имя сети (SSID)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        if (passwordVisible) "Скрыть" else "Показать",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
