package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.SystemInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import java.util.Locale

@Composable
fun SystemMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rawInfo by viewModel.systemInfo.collectAsState()
    val info = rawInfo ?: SystemInfo()
    val interfaces by viewModel.interfaces.collectAsState()
    val devices by viewModel.deviceListFull.collectAsState()
    val product by viewModel.productInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSystemInfo()
        viewModel.loadProductInfo()
        viewModel.loadInterfaces()
        viewModel.loadDeviceListFull()
    }

    val masters = remember(interfaces) { interfaces.filter { it.id.startsWith("WifiMaster") } }
    val ifaces = remember(interfaces) {
        interfaces.filter { it.id.startsWith("Ethernet") || it.type.lowercase().contains("modem") || it.type.lowercase().contains("wisp") }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Monitor, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Системный монитор",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "CPU • RAM • радио • интерфейсы • клиенты",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                viewModel.loadSystemInfo()
                viewModel.loadProductInfo()
                viewModel.loadInterfaces()
                viewModel.loadDeviceListFull()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Процессор и память", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    if (info.clockTime.isNotBlank()) {
                        Text(info.clockTime, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                }
                InfoRow("Загрузка CPU", "${info.cpuUsagePercent}%")
                LinearProgressIndicator(
                    progress = { info.cpuUsagePercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = KeeneticColors.Primary
                )
                InfoRow(
                    "Память (${info.cpus} ядра)",
                    "${fmtBytes((info.memoryTotal - info.memoryFree).coerceAtLeast(0))} из ${fmtBytes(info.memoryTotal)} • ${info.memoryUsagePercent}%"
                )
                LinearProgressIndicator(
                    progress = { info.memoryUsagePercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF6B6B)
                )
                HorizontalLine()
                InfoRow("Модель", info.title)
                InfoRow("Версия KeeneticOS", info.osVersion)
                InfoRow("Ядро (Kernel)", info.kernel)
                InfoRow("Аппаратная ревизия", info.hwVersion)
                if (product.serialNumber.isNotBlank() || product.model.isNotBlank()) {
                    InfoRow("Модель (product)", product.model.ifBlank { info.title })
                    InfoRow("Серийный номер", product.serialNumber.ifBlank { "—" })
                    InfoRow("Производитель", product.vendor.ifBlank { "—" })
                }
                InfoRow("Uptime", info.uptimeFormatted)
                InfoRow("Имя роутера", info.hostname)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("Радиоканалы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
                if (masters.isEmpty()) {
                    Text("Данные радиомодулей не загружены", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                masters.forEach { m ->
                    val band = if (m.id.contains("WifiMaster0")) "2.4 ГГц" else if (m.id.contains("WifiMaster1")) "5 ГГц" else "—"
                    val channel = if (m.channel > 0) "Канал: ${m.channel}" else "Канал: авто"
                    val width = if (m.channelWidth > 0) "${m.channelWidth} МГц" else ""
                    InfoRow(band, listOf(channel, width).filter { it.isNotBlank() }.joinToString(" • "))
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lan, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Интерфейсы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("▼ входящие • ▲ исходящие", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                ifaces.forEach { itf ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(itf.name, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (itf.isUp) "Подключено" else "Отключено, " + (if (itf.uptime > 0) fmtUptime(itf.uptime) else "нет потока"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (itf.isUp) KeeneticColors.Primary else KeeneticColors.TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("▼ ${fmtBytes(itf.rxBytes)}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                            Text("▲ ${fmtBytes(itf.txBytes)}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            Text("▼ ${fmtSpeed(itf.rxSpeedKbps)} • ▲ ${fmtSpeed(itf.txSpeedKbps)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }

        val online = devices.count { it.online }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF34D399))
                    Text("Клиенты (онлайн: $online, всего: ${devices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
                if (devices.isEmpty()) {
                    Text("Список клиентов пуст", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                devices.sortedBy { !it.online }.take(12).forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.size(8.dp).background(
                                if (d.online) Color(0xFF34D399) else KeeneticColors.Divider,
                                androidx.compose.foundation.shape.CircleShape
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name.ifBlank { d.hostname }.ifBlank { d.mac }, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${d.mac} • ${d.ip.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
    }
}

@Composable
private fun HorizontalLine() {
    HorizontalDivider(color = KeeneticColors.Divider)
}

private fun fmtBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes Б"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f КБ", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f МБ", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f ГБ", gb)
}

private fun fmtSpeed(kbps: Long): String {
    if (kbps < 1024) return "${kbps} кбит/с"
    val mbps = kbps / 1024.0
    return String.format(Locale.US, "%.1f Мбит/с", mbps)
}

private fun fmtUptime(seconds: Long): String {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    return if (d > 0) "${d}д ${h}ч" else if (h > 0) "${h}ч ${m}м" else "${m}м"
}