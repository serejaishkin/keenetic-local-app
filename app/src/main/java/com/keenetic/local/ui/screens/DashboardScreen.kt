package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DashboardScreen(viewModel: RouterViewModel) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

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