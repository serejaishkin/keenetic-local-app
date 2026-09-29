package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors
import java.util.Locale

@Composable
fun TrafficMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val clients by viewModel.wirelessClients.collectAsState()

    fun refresh() {
        viewModel.loadInterfaces()
        viewModel.loadWifiData()
    }

    LaunchedEffect(Unit) { refresh() }

    val activeIfaces = interfaces
        .filter {
            it.isUp && (it.rxSpeedKbps > 0 || it.txSpeedKbps > 0 ||
                it.type == "Ethernet" || it.type.lowercase().contains("modem"))
        }
        .sortedByDescending { it.rxSpeedKbps + it.txSpeedKbps }

    val topClients = clients
        .filter { it.active && (it.rxRateKbps > 0 || it.txRateKbps > 0) }
        .sortedByDescending { it.rxRateKbps + it.txRateKbps }
        .take(5)

    SectionScaffold(
        title = "Монитор трафика",
        subtitle = "Текущая скорость на интерфейсах",
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
            item(key = "iface-header") {
                SubGroupHeader("Интерфейсы", activeIfaces.size)
            }

            if (activeIfaces.isEmpty()) {
                item(key = "iface-empty") {
                    SectionCard {
                        EmptyHint("Нет активного трафика")
                    }
                }
            }

            items(activeIfaces, key = { it.name }) { itf ->
                SectionCard {
                    Text(
                        itf.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = KeeneticColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SpeedBar(rx = itf.rxSpeedKbps, tx = itf.txSpeedKbps)
                    InfoRow("Входящий", fmtSpeed(itf.rxSpeedKbps), valueColor = Color(0xFF34D399))
                    InfoRow("Исходящий", fmtSpeed(itf.txSpeedKbps), valueColor = Color(0xFFF59E0B))
                    InfoRow("Всего принято", fmtBytes(itf.rxBytes))
                    InfoRow("Всего передано", fmtBytes(itf.txBytes))
                }
            }

            if (topClients.isNotEmpty()) {
                item(key = "clients") {
                    SectionCard(
                        title = "Топ-5 клиентов",
                        icon = Icons.Default.Devices,
                        subtitle = "по суммарной скорости"
                    ) {
                        topClients.forEach { c ->
                            val name = c.displayName.ifBlank { c.hostname.ifBlank { c.mac } }
                            InfoRow(name, "▼ ${fmtSpeed(c.rxRateKbps)} • ▲ ${fmtSpeed(c.txRateKbps)}")
                            InfoRow(
                                "  всего",
                                fmtBytes(c.rxBytes + c.txBytes),
                                valueColor = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Полоса «входящий/исходящий»: доли rx и tx от суммарной скорости. */
@Composable
private fun SpeedBar(rx: Long, tx: Long) {
    val total = rx + tx
    if (total <= 0) return
    val rxFraction = (rx.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(rxFraction.coerceAtLeast(0.001f))
                .height(4.dp)
                .background(Color(0xFF34D399), CircleShape)
        )
        Box(
            modifier = Modifier
                .weight((1f - rxFraction).coerceAtLeast(0.001f))
                .height(4.dp)
                .background(Color(0xFFF59E0B), CircleShape)
        )
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
