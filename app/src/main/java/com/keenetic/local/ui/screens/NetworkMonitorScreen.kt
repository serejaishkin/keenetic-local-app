package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.*
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun NetworkMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val conntrack by viewModel.conntrackEntries.collectAsState()
    val arpEntries by viewModel.arpEntries.collectAsState()
    val neighbours by viewModel.neighbourEntries.collectAsState()
    val ipRules by viewModel.ipRules.collectAsState()
    val monitorStatus by viewModel.monitorStatus.collectAsState()

    val loadAll = {
        viewModel.loadConntrack()
        viewModel.loadArpEntries()
        viewModel.loadNeighbourEntries()
        viewModel.loadIpRules()
        viewModel.loadMonitorStatus()
    }

    LaunchedEffect(Unit) { loadAll() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Monitor, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Мониторинг сети", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { loadAll() }) { Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary) }
            }
        }

        // Monitor
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Пакетный захват", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Активен", color = KeeneticColors.TextPrimary)
                        Text(if (monitorStatus.active) "Да" else "Нет", color = if (monitorStatus.active) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    InfoRow("Интерфейс", monitorStatus.interfaceName)
                    InfoRow("Фильтр", monitorStatus.filter)
                }
            }
        }

        // ARP
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.List, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("ARP-таблица (${arpEntries.size})", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (arpEntries.isEmpty()) { Text("Пусто", color = KeeneticColors.TextSecondary) }
                }
            }
        }
        items(arpEntries.take(20)) { entry ->
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Background)) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${entry.ip} → ${entry.mac}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                    Text(entry.interfaceName, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
            }
        }

        // Neighbours
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.People, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("IPv6 соседи (${neighbours.size})", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (neighbours.isEmpty()) { Text("Пусто", color = KeeneticColors.TextSecondary) }
                }
            }
        }
        items(neighbours.take(20)) { entry ->
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Background)) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${entry.ip} → ${entry.mac}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                    Text(entry.interfaceName, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
            }
        }

        // IP Rules
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("IP-правила (${ipRules.size})", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (ipRules.isEmpty()) { Text("Пусто", color = KeeneticColors.TextSecondary) }
                }
            }
        }
        items(ipRules.take(20)) { rule ->
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Background)) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("pri:${rule.priority} from:${rule.from}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                    Text(rule.lookup, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
