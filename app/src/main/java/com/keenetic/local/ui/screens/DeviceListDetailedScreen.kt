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
import com.keenetic.local.api.DeviceListEntryFull
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DeviceListDetailedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val devices by viewModel.deviceListFull.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDeviceListFull() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Devices, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Список устройств", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Всего: ${devices.size}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                }
            }
        }

        items(devices) { device ->
            DeviceCard(device)
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceListEntryFull) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(device.name.ifEmpty { "Устройство" }, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (device.online) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (device.online) KeeneticColors.Primary else KeeneticColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(if (device.online) "Online" else "Offline", style = MaterialTheme.typography.bodySmall, color = if (device.online) KeeneticColors.Primary else KeeneticColors.TextSecondary)
                }
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("MAC", device.mac)
            InfoRow("IP", device.ip)
            InfoRow("Хост", device.hostname)
            InfoRow("Интерфейс", device.interfaceName)
            InfoRow("Тип", device.type)
            if (device.policy.isNotEmpty()) InfoRow("Политика", device.policy)
            if (device.schedule.isNotEmpty()) InfoRow("Расписание", device.schedule)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
    }
}
