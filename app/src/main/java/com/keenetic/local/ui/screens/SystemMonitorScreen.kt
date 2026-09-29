package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.SystemInfo
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
fun SystemMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rawInfo by viewModel.systemInfo.collectAsState()
    val info = rawInfo ?: SystemInfo()
    val interfaces by viewModel.interfaces.collectAsState()
    val devices by viewModel.deviceListFull.collectAsState()
    val product by viewModel.productInfo.collectAsState()

    fun refresh() {
        viewModel.loadSystemInfo()
        viewModel.loadProductInfo()
        viewModel.loadInterfaces()
        viewModel.loadDeviceListFull()
    }

    LaunchedEffect(Unit) { refresh() }

    val masters = remember(interfaces) { interfaces.filter { it.id.startsWith("WifiMaster") } }
    val ifaces = remember(interfaces) {
        interfaces.filter {
            it.id.startsWith("Ethernet") ||
                it.type.lowercase().contains("modem") ||
                it.type.lowercase().contains("wisp")
        }
    }
    val online = devices.count { it.online }

    SectionScaffold(
        title = "Системный монитор",
        subtitle = "CPU, память, радио, интерфейсы и клиенты",
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
            item(key = "resources") {
                SectionCard(title = "Ресурсы системы", icon = Icons.Default.Memory) {
                    InfoRow("Загрузка CPU", "${info.cpuUsagePercent}%")
                    LinearProgressIndicator(
                        progress = { (info.cpuUsagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = KeeneticColors.Primary
                    )
                    InfoRow(
                        "Память · ${fmtBytes(info.memoryTotal)} всего",
                        "${fmtBytes((info.memoryTotal - info.memoryFree).coerceAtLeast(0))} занято • ${info.memoryUsagePercent}%"
                    )
                    LinearProgressIndicator(
                        progress = { (info.memoryUsagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF6B6B)
                    )
                    if (info.cpus > 0) InfoRow("Ядер", info.cpus.toString())
                    if (info.clockTime.isNotBlank()) InfoRow("Время на роутере", info.clockTime)
                    InfoRow("Время работы", info.uptimeFormatted)
                }
            }

            item(key = "device") {
                SectionCard(title = "Устройство", icon = Icons.Default.Router) {
                    InfoRow("Модель", info.title)
                    InfoRow("Версия KeeneticOS", info.osVersion)
                    InfoRow("Ядро", info.kernel)
                    InfoRow("Аппаратная ревизия", info.hwVersion)
                    if (product.serialNumber.isNotBlank() || product.model.isNotBlank()) {
                        RowDivider()
                        InfoRow("Серийный номер", product.serialNumber.ifBlank { "—" })
                        InfoRow("Производитель", product.vendor.ifBlank { "—" })
                    }
                    InfoRow("Имя роутера", info.hostname)
                }
            }

            item(key = "radio") {
                SectionCard(title = "Радиоканалы", icon = Icons.Default.Wifi) {
                    if (masters.isEmpty()) {
                        EmptyHint("Данные радиомодулей не загружены")
                    }
                    masters.forEach { m ->
                        val band = when {
                            m.id.contains("WifiMaster0") -> "2.4 ГГц"
                            m.id.contains("WifiMaster1") -> "5 ГГц"
                            else -> m.id
                        }
                        val channel = if (m.channel > 0) "Канал ${m.channel}" else "Канал авто"
                        val width = if (m.channelWidth > 0) "${m.channelWidth} МГц" else null
                        InfoRow(band, listOfNotNull(channel, width).joinToString(" • "))
                    }
                }
            }

            item(key = "interfaces") {
                SectionCard(title = "Интерфейсы", icon = Icons.Default.Lan) {
                    if (ifaces.isEmpty()) {
                        EmptyHint("Интерфейсы не найдены")
                    }
                    ifaces.forEach { itf ->
                        InfoRow(
                            itf.name,
                            if (itf.isUp) "Подключено" else "Отключено",
                            valueColor = if (itf.isUp) KeeneticColors.Primary else KeeneticColors.TextSecondary
                        )
                        InfoRow("  Принято", fmtBytes(itf.rxBytes))
                        InfoRow("  Передано", fmtBytes(itf.txBytes))
                        InfoRow("  Скорость", "▼ ${fmtSpeed(itf.rxSpeedKbps)} • ▲ ${fmtSpeed(itf.txSpeedKbps)}")
                    }
                }
            }

            item(key = "clients") {
                SectionCard(
                    title = "Клиенты",
                    icon = Icons.Default.Computer,
                    subtitle = "онлайн $online из ${devices.size}"
                ) {
                    if (devices.isEmpty()) {
                        EmptyHint("Список клиентов пуст")
                    }
                    devices.sortedBy { !it.online }.take(12).forEach { d ->
                        val name = d.name.ifBlank { d.hostname }.ifBlank { d.mac }
                        InfoRow(
                            name,
                            if (d.online) d.ip.ifBlank { "подключено" } else "офлайн",
                            valueColor = if (d.online) KeeneticColors.Primary else KeeneticColors.TextSecondary
                        )
                        Text(
                            d.mac,
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun fmtBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    if (bytes < 1024) return "$bytes Б"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f КБ", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f МБ", mb)
    return String.format(Locale.US, "%.2f ГБ", mb / 1024.0)
}

private fun fmtSpeed(kbps: Long): String {
    if (kbps <= 0) return "0 кбит/с"
    if (kbps < 1024) return "$kbps кбит/с"
    return String.format(Locale.US, "%.1f Мбит/с", kbps / 1024.0)
}
