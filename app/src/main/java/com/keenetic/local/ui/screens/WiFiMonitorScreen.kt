package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
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

    SectionScaffold(
        title = "Монитор Wi-Fi",
        subtitle = "Клиенты, эфир и загрузка каналов",
        onBack = onBack,
        onRefresh = { viewModel.loadWifiData() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "radio") {
                SectionCard(icon = Icons.Default.RadioButtonChecked) {
                    SubGroupHeader("Радиомодуль")
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
                            OutlinedButton(
                                onClick = { viewModel.scanWifiSiteSurvey(selectedRadio) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = KeeneticColors.Primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сканировать", color = KeeneticColors.Primary)
                            }
                        }
                    }
                }
            }

            item(key = "scan") {
                SectionCard(
                    title = "Соседние Wi-Fi сети",
                    icon = Icons.Default.SignalCellularAlt,
                    trailing = {
                        Text(
                            "${scanResults.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                ) {
                    if (scanResults.isEmpty()) {
                        EmptyHint(
                            if (isScanning) {
                                "Сканирование эфира..."
                            } else {
                                "Нажмите «Сканировать», чтобы увидеть сети"
                            }
                        )
                    } else {
                        scanResults.forEachIndexed { index, net ->
                            if (index > 0) RowDivider()
                            InfoRow(
                                label = net.ssid.ifBlank { "(скрытая сеть)" },
                                value = signalLabel(net.rssi),
                                valueColor = signalColor(net.rssi)
                            )
                            Text(
                                "канал ${net.channel} · ${net.band} · ${net.encryption}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }

            item(key = "clients") {
                SectionCard(
                    title = "Подключённые клиенты",
                    subtitle = if (radioIs5g) "5 ГГц" else "2.4 ГГц",
                    icon = Icons.Default.Devices
                ) {
                    if (clients.isEmpty()) {
                        EmptyHint("Нет подключённых клиентов")
                    } else {
                        clients.forEachIndexed { index, c ->
                            if (index > 0) RowDivider()
                            InfoRow(
                                label = c.displayName.ifBlank { c.hostname.ifBlank { c.mac } },
                                value = c.rssi?.let { signalLabel(it) } ?: "—",
                                valueColor = c.rssi?.let { signalColor(it) } ?: KeeneticColors.TextSecondary
                            )
                            Text(
                                buildString {
                                    if (!c.ip.isNullOrBlank()) append("${c.ip} · ")
                                    append(c.mac)
                                    append(" · ")
                                    append(c.ssid.ifBlank { c.ap })
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = KeeneticColors.TextSecondary
                            )
                            Text(
                                "RX ${formatRate(c.rxRateKbps)}  TX ${formatRate(c.txRateKbps)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }

            item(key = "traffic") {
                SectionCard(title = "Wi-Fi трафик", icon = Icons.Default.Storage) {
                    if (wifiTraffic.isEmpty()) {
                        EmptyHint("Нет данных")
                    } else {
                        wifiTraffic.forEach { t ->
                            InfoRow(
                                label = shortInterfaceName(t.id),
                                value = "↓ ${formatBytes(t.rxBytes)}  ↑ ${formatBytes(t.txBytes)}",
                                monospace = true
                            )
                        }
                    }
                }
            }

            if (channelUtilization.isNotEmpty()) {
                item(key = "channels") {
                    SectionCard(title = "Загрузка каналов", icon = Icons.Default.Equalizer) {
                        channelUtilization.forEach { u ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "канал ${u.channel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeeneticColors.TextSecondary,
                                    modifier = Modifier.width(72.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { (u.load.coerceIn(0, 100)) / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (u.load >= 70) {
                                        KeeneticColors.Error
                                    } else if (u.load >= 40) {
                                        KeeneticColors.Warning
                                    } else {
                                        KeeneticColors.Primary
                                    },
                                    trackColor = KeeneticColors.Divider
                                )
                                Text(
                                    " ${u.load}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            if (spectrum.isNotEmpty()) {
                item(key = "spectrum") {
                    SectionCard(title = "Спектр каналов", icon = Icons.Default.BarChart) {
                        spectrum.forEach { ch ->
                            val peak = ch.utilization.maxOfOrNull { it.load } ?: 0
                            InfoRow(
                                label = "Канал ${ch.number}",
                                value = "пик ${peak}% (точек: ${ch.utilization.size})"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun signalLabel(rssi: Int): String = "$rssi dBm"

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
