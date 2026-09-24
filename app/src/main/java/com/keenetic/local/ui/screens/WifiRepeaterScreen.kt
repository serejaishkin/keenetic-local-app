package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WifiSiteSurveyEntry
import com.keenetic.local.api.WifiStationStatus
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun WifiRepeaterScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val station by viewModel.wifiStationStatus.collectAsState()
    val scanResults by viewModel.wifiScanResults.collectAsState()
    val isScanning by viewModel.isWifiScanning.collectAsState()
    val actionMessage by viewModel.wifiActionMessage.collectAsState()

    var selectedRadio by remember { mutableStateOf("WifiMaster0") }
    var targetSsid by remember { mutableStateOf("") }
    var targetPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    fun refresh() {
        viewModel.loadWifiData()
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Подключение через соседнюю Wi-Fi сеть", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                    Text("Репитер (WISP)", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            RepeaterStatusCard(
                station = station,
                onToggle = { enabled -> viewModel.toggleWifiStation(station.id, enabled) }
            )
        }

        item {
            RadioBandSelector(selectedRadio = selectedRadio, onSelect = { selectedRadio = it })
        }

        item {
            ScanAndConnectCard(
                selectedRadio = selectedRadio,
                isScanning = isScanning,
                scanResults = scanResults,
                targetSsid = targetSsid,
                targetPassword = targetPassword,
                passwordVisible = passwordVisible,
                onSsidChange = { targetSsid = it },
                onPasswordChange = { targetPassword = it },
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                onScan = { viewModel.scanWifiSiteSurvey(selectedRadio) },
                onSelectDiscoveredSsid = { targetSsid = it },
                onConnect = {
                    viewModel.connectWifiStation(selectedRadio, targetSsid, targetPassword)
                },
                onDisconnect = { viewModel.disconnectWifiStation(selectedRadio) },
                isConnected = station.isUp && selectedRadio in (station.masterRadio ?: "")
            )
        }

        if (!actionMessage.isNullOrBlank()) {
            val message: String = actionMessage ?: ""
            item {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RepeaterStatusCard(station: WifiStationStatus, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(if (station.isUp) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (station.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text("Клиент Wi-Fi (WifiStation)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                        Text(
                            if (station.isUp) {
                                if (!station.connectedSsid.isNullOrBlank()) "Подключен к «${station.connectedSsid}»" else "Интерфейс активен"
                            } else "Выключен",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (station.isUp) KeeneticColors.Success else KeeneticColors.TextSecondary
                        )
                    }
                }
                Switch(
                    checked = station.isUp,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KeeneticColors.Success,
                        checkedTrackColor = KeeneticColors.Success.copy(alpha = 0.4f)
                    )
                )
            }

            HorizontalDivider(color = KeeneticColors.Divider)

            StatRow("Состояние", station.state)
            if (!station.connectedSsid.isNullOrBlank()) StatRow("Имя сети (SSID)", station.connectedSsid!!)
            if (!station.ip.isNullOrBlank()) StatRow("IP-адрес", station.ip!!)
            if (!station.mac.isNullOrBlank()) StatRow("BSSID (MAC-адрес)", station.mac!!)
            if (station.rssi != null) StatRow("Сигнал", "${station.rssi} dBm")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RadioBandSelector(selectedRadio: String, onSelect: (String) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = KeeneticColors.Primary)
                Text("Диапазон (WISP)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedRadio == "WifiMaster0",
                    onClick = { onSelect("WifiMaster0") },
                    label = { Text("2.4 ГГц (Master0)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFFF59E0B)
                    )
                )
                FilterChip(
                    selected = selectedRadio == "WifiMaster1",
                    onClick = { onSelect("WifiMaster1") },
                    label = { Text("5 ГГц (Master1)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.25f),
                        selectedLabelColor = KeeneticColors.Primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ScanAndConnectCard(
    selectedRadio: String,
    isScanning: Boolean,
    scanResults: List<WifiSiteSurveyEntry>,
    targetSsid: String,
    targetPassword: String,
    passwordVisible: Boolean,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onScan: () -> Unit,
    onSelectDiscoveredSsid: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, tint = KeeneticColors.Primary)
                Text("Настройка беспроводного подключения", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onScan,
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сканирование эфира...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обзор сетей")
                }
            }

            if (scanResults.isNotEmpty()) {
                Text(
                    "Найдено сетей (${scanResults.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    scanResults.take(8).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(KeeneticColors.SurfaceElevated)
                                .clickable { onSelectDiscoveredSsid(entry.ssid) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(14.dp))
                                Column {
                                    Text(entry.ssid, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                                    Text(
                                        "канал ${entry.channel} · ${entry.band} · ${entry.encryption.ifBlank { "без защиты" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KeeneticColors.TextSecondary
                                    )
                                }
                            }
                            Text(
                                "${entry.rssi} dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = targetSsid,
                onValueChange = onSsidChange,
                label = { Text("Имя сети (SSID)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = targetPassword,
                onValueChange = onPasswordChange,
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = KeeneticColors.TextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error)
                ) {
                    Text("Отключить")
                }
            } else {
                Button(
                    onClick = onConnect,
                    enabled = targetSsid.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Подключить")
                }
            }
        }
    }
}