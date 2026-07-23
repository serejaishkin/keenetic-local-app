package com.keenetic.local.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WifiAssoc
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DashboardScreen(viewModel: RouterViewModel) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    val associations by viewModel.associations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Статус роутера",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            StatusCard(systemInfo)
        }

        item {
            QuickActionsCard(viewModel)
        }

        item {
            // Раньше матчили по id "GigabitEthernet1" / "ISP" - но у тебя
            // именно этот порт без IP (просто физический разъём), а реальный
            // аплинк с адресом - GigabitEthernet0/Vlan4 ("pakt"). Вместо
            // хардкода конкретных id теперь общее правило: WAN-кандидат - это
            // не LAN/AP/служебный интерфейс, у которого реально есть IP.
            // У тебя, судя по конфигу, потенциально несколько аплинков
            // (multi-WAN/failover) - показываем все, а не только первый.
            val wanCandidates = interfaces.filter {
                !it.address.isNullOrBlank() &&
                    it.type !in setOf("AccessPoint", "Bridge", "Port", "WifiStation", "Loopback")
            }
            wanCandidates.forEach { wan ->
                WanStatusCard(wan, viewModel)
            }
        }

        item {
            val tunnels = interfaces.filter { it.type in setOf("Proxy", "Wireguard") }
            if (tunnels.isNotEmpty()) {
                VpnStatusCard(tunnels)
            }
        }

        item {
            if (associations.isNotEmpty()) {
                TrafficChartCard(associations)
            }
        }

        item {
            Text(
                text = "Интерфейсы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(interfaces.size) { index ->
            val iface = interfaces[index]
            InterfaceCard(iface)
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun StatusCard(info: com.keenetic.local.api.SystemInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Роутер онлайн", fontWeight = FontWeight.Medium, color = KeeneticColors.Accent)
            }
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("Версия ОС", info?.version ?: "—")
            InfoRow("CPU", "${info?.cpuload ?: "—"}%")
            InfoRow("RAM", info?.memory ?: "—")
            InfoRow("Uptime", info?.uptime ?: "—")
            InfoRow("Имя", info?.hostname ?: "—")
        }
    }
}

@Composable
fun WanStatusCard(wan: com.keenetic.local.api.InterfaceInfo, viewModel: RouterViewModel) {
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
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (wan.up) KeeneticColors.Accent else KeeneticColors.Error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Интернет (${wan.displayName})",
                        fontWeight = FontWeight.Medium,
                        color = if (wan.up) KeeneticColors.Accent else KeeneticColors.Error
                    )
                }
                Switch(
                    checked = wan.up,
                    onCheckedChange = { confirmToggle = true },
                    colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Accent)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val ipObtained = !wan.address.isNullOrBlank()
            InfoRow("Статус", if (wan.up) "Подключено" else "Нет соединения")
            InfoRow("IP-адрес", wan.address ?: "—")
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ipObtained) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (ipObtained) KeeneticColors.Accent else KeeneticColors.Error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (ipObtained) "IP получен, всё в порядке" else "IP не получен - проверь подключение",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ipObtained) KeeneticColors.Accent else KeeneticColors.Error
                )
            }
        }
    }

    if (confirmToggle) {
        AlertDialog(
            onDismissRequest = { confirmToggle = false },
            title = { Text("Подтвердите действие") },
            text = {
                Text(
                    "${if (wan.up) "Выключить" else "Включить"} подключение «${wan.displayName}»?" +
                        if (wan.up) " Если это единственный аплинк, интернет пропадёт до повторного включения." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleInterface(wan.id, !wan.up)
                    confirmToggle = false
                }) {
                    Text("Да", color = KeeneticColors.Error)
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

@Composable
fun TrafficChartCard(associations: List<WifiAssoc>) {
    // Столбчатый график по топ-устройствам за текущий счётчик трафика с
    // момента подключения (это не "скорость сейчас", а накопленные байты -
    // честной живой линии скорости через простой REST-поллинг без хранения
    // истории не построить корректно, поэтому делаем то, что данные реально
    // позволяют).
    val top = associations
        .filter { (it.txbytes ?: 0) + (it.rxbytes ?: 0) > 0 }
        .sortedByDescending { (it.txbytes ?: 0) + (it.rxbytes ?: 0) }
        .take(6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Активные подключения (вх/исх)", fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Box(modifier = Modifier.size(10.dp).background(KeeneticColors.Accent))
                Spacer(modifier = Modifier.width(4.dp))
                Text("входящий", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(10.dp).background(KeeneticColors.Primary))
                Spacer(modifier = Modifier.width(4.dp))
                Text("исходящий", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (top.isEmpty()) {
                Text(
                    "Нет данных о трафике активных подключений",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            } else {
                val maxBytes = top.maxOf { (it.txbytes ?: 0) + (it.rxbytes ?: 0) }.coerceAtLeast(1)
                top.forEach { assoc ->
                    val rx = assoc.rxbytes ?: 0
                    val tx = assoc.txbytes ?: 0
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            assoc.hostname ?: assoc.mac ?: "—",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            val total = (rx + tx).toFloat()
                            if (total <= 0f) return@Canvas
                            val widthTotal = size.width * (total / maxBytes.toFloat())
                            val rxWidth = widthTotal * (rx / total)
                            val txWidth = widthTotal - rxWidth
                            drawRect(
                                color = KeeneticColorsAccentCompose,
                                topLeft = Offset(0f, 0f),
                                size = Size(rxWidth, size.height),
                                style = Fill
                            )
                            drawRect(
                                color = KeeneticColorsPrimaryCompose,
                                topLeft = Offset(rxWidth, 0f),
                                size = Size(txWidth, size.height),
                                style = Fill
                            )
                        }
                        Text(
                            "${formatBytes(rx)} ↓ · ${formatBytes(tx)} ↑",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private val KeeneticColorsAccentCompose = KeeneticColors.Accent
private val KeeneticColorsPrimaryCompose = KeeneticColors.Primary

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f ГБ".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f МБ".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.1f КБ".format(bytes / 1024.0)
    else -> "$bytes Б"
}

@Composable
fun VpnStatusCard(tunnels: List<com.keenetic.local.api.InterfaceInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VPN / Прокси", fontWeight = FontWeight.Medium, color = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            tunnels.forEach { t ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (t.up) "Активен" else "Выключен",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (t.up) KeeneticColors.Accent else KeeneticColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun QuickActionsCard(viewModel: RouterViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Быстрые действия", fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(Icons.Default.Refresh, "Перезагрузить") { viewModel.reboot() }
                ActionButton(Icons.Default.Wifi, "Wi-Fi") { /* navigate */ }
                ActionButton(Icons.Default.Devices, "Устройства") { /* navigate */ }
                ActionButton(Icons.Default.Terminal, "Терминал") { /* navigate */ }
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
    }
}

@Composable
fun InterfaceCard(iface: com.keenetic.local.api.InterfaceInfo) {
    val isUp = iface.up == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUp) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isUp) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = iface.displayName.ifBlank { iface.id }, fontWeight = FontWeight.Medium)
                Text(
                    text = "${iface.state ?: ""} ${iface.link ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            if (iface.connected != null) {
                Text(text = iface.connected, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.Accent)
            }
        }
    }
}