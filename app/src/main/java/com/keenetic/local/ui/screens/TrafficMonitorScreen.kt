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
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors
import java.util.Locale

@Composable
fun TrafficMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val clients by viewModel.wirelessClients.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInterfaces()
        viewModel.loadWifiData()
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
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Монитор трафика",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Текущая скорость на интерфейсах",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadInterfaces() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        val activeIfaces = interfaces.filter { it.isUp && (it.rxSpeedKbps > 0 || it.txSpeedKbps > 0 || it.type == "Ethernet" || it.type.lowercase().contains("modem")) }
            .sortedByDescending { it.rxSpeedKbps + it.txSpeedKbps }

        if (activeIfaces.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Нет активного трафика", color = KeeneticColors.TextSecondary)
                }
            }
        }

        activeIfaces.forEach { itf ->
            TrafficCard(itf.name, itf.rxSpeedKbps, itf.txSpeedKbps, itf.rxBytes, itf.txBytes)
        }

        val topClients = clients
            .filter { it.active && (it.rxRateKbps > 0 || it.txRateKbps > 0) }
            .sortedByDescending { it.rxRateKbps + it.txRateKbps }
            .take(5)

        if (topClients.isNotEmpty()) {
            Text(
                "Топ-5 клиентов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topClients.forEach { c ->
                        val clientName = c.displayName.ifBlank { c.hostname.ifBlank { c.mac } }
                        Column {
                            Text(
                                clientName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = KeeneticColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SpeedRow("Входящий", c.rxRateKbps, Color(0xFF34D399))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    SpeedRow("Исходящий", c.txRateKbps, Color(0xFFF59E0B))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${fmtBytes(c.rxBytes + c.txBytes)}", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                                }
                            }
                        }
                        if (c != topClients.last()) HorizontalDivider(color = KeeneticColors.Divider)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficCard(name: String, rxKbps: Long, txKbps: Long, rxBytes: Long, txBytes: Long) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SpeedRow("Входящий", rxKbps, Color(0xFF34D399))
                    Text("Всего: ${fmtBytes(rxBytes)}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    SpeedRow("Исходящий", txKbps, Color(0xFFF59E0B))
                    Text("Всего: ${fmtBytes(txBytes)}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
            }
            
            val totalKbps = rxKbps + txKbps
            if (totalKbps > 0) {
                LinearProgressIndicator(
                    progress = { (rxKbps.toFloat() / totalKbps.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF34D399),
                    trackColor = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
private fun SpeedRow(label: String, kbps: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(fmtSpeed(kbps), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
    }
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