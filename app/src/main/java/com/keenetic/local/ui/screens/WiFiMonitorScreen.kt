package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WirelessClient
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import java.util.Locale

@Composable
fun WiFiMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val wirelessClients by viewModel.wirelessClients.collectAsState()
    val scanResults by viewModel.wifiScanResults.collectAsState()
    val isScanning by viewModel.isWifiScanning.collectAsState()
    val traffic by viewModel.trafficCounters.collectAsState()
    val channelUtilization by viewModel.channelUtilization.collectAsState()
    val spectrum by viewModel.spectrumData.collectAsState()

    var selectedRadio by remember { mutableStateOf("WifiMaster0") }
    val radioIs5g = selectedRadio.contains("Master1")

    LaunchedEffect(Unit) {
        viewModel.loadWifiData()
        viewModel.loadChannelUtilization()
        viewModel.loadSpectrum()
        viewModel.loadTrafficCounters()
    }

    LaunchedEffect(selectedRadio) {
        viewModel.loadChannelUtilization()
        viewModel.loadSpectrum()
    }

    val clients = wirelessClients.filter {
        if (radioIs5g) it.band.contains("5") else !it.band.contains("5")
    }

    val wifiTraffic = traffic.filter { t ->
        t.id.contains("Wi-Fi", ignoreCase = true) || t.id.contains("band0") || t.id.contains("Master")
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Монитор Wi-Fi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Радио", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !radioIs5g,
                            onClick = { selectedRadio = "WifiMaster0" },
                            label = { Text("2.4 ГГц") }
                        )
                        FilterChip(
                            selected = radioIs5g,
                            onClick = { selectedRadio = "WifiMaster1" },
                            label = { Text("5 ГГц") }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Button(
                                onClick = { viewModel.scanWifiSiteSurvey(selectedRadio) },
                                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сканировать")
                            }
                        }
                    }
                }
            }
        }

        item {
            MonitorCard(
                icon = { Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = KeeneticColors.Primary) },
                title = "Соседние Wi-Fi сети"
            ) {
                if (scanResults.isEmpty()) {
                    Text(
                        if (isScanning) "Сканирование эфира..." else "Нажмите «Сканировать», чтобы увидеть сети",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeeneticColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    scanResults.forEach { net ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(net.ssid, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
                                Text(
                                    "канал ${net.channel} · ${net.band} · ${net.encryption}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            Text(
                                signalLabel(net.rssi),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = signalColor(net.rssi),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (net != scanResults.last()) HorizontalDivider(color = KeeneticColors.Divider)
                    }
                    Text(
                        "Всего найдено: ${scanResults.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            MonitorCard(
                icon = { Icon(Icons.Default.Devices, contentDescription = null, tint = KeeneticColors.Primary) },
                title = "Подключённые клиенты (${if (radioIs5g) "5 ГГц" else "2.4 ГГц"})"
            ) {
                if (clients.isEmpty()) {
                    Text("Нет подключённых клиентов", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    clients.forEach { c ->
                        ClientRow(c)
                        if (c != clients.last()) HorizontalDivider(color = KeeneticColors.Divider)
                    }
                }
            }
        }

        item {
            MonitorCard(
                icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = KeeneticColors.Primary) },
                title = "Wi-Fi трафик"
            ) {
                if (wifiTraffic.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    wifiTraffic.forEach { t ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(shortInterfaceName(t.id), style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                            Text("↓ ${formatBytes(t.rxBytes)}  ↑ ${formatBytes(t.txBytes)}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        if (channelUtilization.isNotEmpty()) {
            item {
                MonitorCard(
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = null, tint = KeeneticColors.Primary) },
                    title = "Загрузка каналов"
                ) {
                    channelUtilization.forEach { u ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("канал ${u.channel}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary, modifier = Modifier.width(72.dp))
                            LinearProgressIndicator(
                                progress = { (u.load.coerceIn(0, 100)) / 100f },
                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (u.load >= 70) KeeneticColors.Error else if (u.load >= 40) KeeneticColors.Warning else KeeneticColors.Primary,
                                trackColor = KeeneticColors.Divider
                            )
                            Text(" ${u.load}%", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        if (spectrum.isNotEmpty()) {
            item {
                MonitorCard(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = KeeneticColors.Primary) },
                    title = "Спектр каналов"
                ) {
                    spectrum.forEach { ch ->
                        val peak = ch.utilization.maxOfOrNull { it.load } ?: 0
                        Text(
                            "Канал ${ch.number}: пик ${peak}% (точек: ${ch.utilization.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorCard(icon: @Composable () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(title, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            content()
        }
    }
}

@Composable
private fun ClientRow(client: WirelessClient) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                client.displayName.ifBlank { client.hostname.ifBlank { client.mac } },
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                buildString {
                    if (!client.ip.isNullOrBlank()) append("${client.ip} · ")
                    append(client.mac)
                    append(" · ")
                    append(client.ssid.ifBlank { client.ap })
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = KeeneticColors.TextSecondary
            )
            Text(
                "RX ${formatRate(client.rxRateKbps)}  TX ${formatRate(client.txRateKbps)}",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
        client.rssi?.let { r ->
            Text(signalLabel(r), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = signalColor(r), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun signalLabel(rssi: Int): String = "${rssi} dBm"

private fun signalColor(rssi: Int): Color = when {
    rssi >= -60 -> KeeneticColors.Primary
    rssi >= -75 -> KeeneticColors.Warning
    else -> KeeneticColors.Error
}

private fun formatRate(kbps: Long): String =
    if (kbps >= 1000) String.format(Locale.US, "%.1f Мбит/с", kbps / 1000.0) else "$kbps Кбит/с"

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    var value = bytes.toDouble()
    var idx = 0
    while (value >= 1024 && idx < units.size - 1) {
        value /= 1024
        idx++
    }
    return if (idx == 0) "%.0f %s".format(value, units[idx]) else "%.1f %s".format(value, units[idx])
}

private fun shortInterfaceName(id: String): String =
    id.replace("HomeWi-Fi-", "Wi-Fi ").replace("WifiMaster", "Мастер").replace("/band0", "")