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
import com.keenetic.local.api.MwsMember
import com.keenetic.local.api.MwsWlan
import com.keenetic.local.api.MwsWlanBand
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun MwsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val mwsStatus by viewModel.mwsStatus.collectAsState()
    val members by viewModel.mwsMembers.collectAsState()
    val wlanList by viewModel.mwsWlanList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMwsStatus()
        viewModel.loadMwsWlan()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mesh Wi-Fi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    viewModel.loadMwsStatus()
                    viewModel.loadMwsWlan()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Статус MWS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = mwsStatus.enabled, onCheckedChange = { viewModel.setMwsEnabled(it) })
                    }
                    InfoRow("Роль", mwsStatus.role)
                    InfoRow("SSID", mwsStatus.ssid)
                    InfoRow("Канал", "${mwsStatus.channel}")
                }
            }
        }

        if (wlanList.isNotEmpty()) {
            item { Text("Сети Mesh (WLAN)", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(wlanList) { wlan ->
                MwsWlanCard(wlan, onToggleWlan = { id, enabled -> viewModel.setMwsWlanEnabled(id, enabled) })
            }
            item {
                Text(
                    "Переключение «Включён» выполняет mws wlan {id} enable (проверено на KN-2311).",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (members.isNotEmpty()) {
            item { Text("Участники", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(members) { member ->
                MwsMemberCard(member)
            }
        }
    }
}

@Composable
private fun MwsWlanCard(wlan: MwsWlan, onToggleWlan: (String, Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(wlan.id, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            InfoRow("По расписанию выключена", if (wlan.disabledBySchedule) "Да" else "Нет")
            wlan.bands.forEach { band ->
                MwsBandRow(band, onToggleBand = { enabled -> onToggleWlan(wlan.id, enabled) })
            }
        }
    }
}

@Composable
private fun MwsBandRow(band: MwsWlanBand, onToggleBand: (Boolean) -> Unit) {
    val label = when (band.band) {
        "0" -> "2.4 ГГц"
        "1" -> "5 ГГц"
        else -> "Полоса ${band.band}"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = KeeneticColors.TextPrimary)
            if (band.accessPointId.isNotBlank()) {
                Text(band.accessPointId, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
        }
        val wpsReady = band.wpsConfigured && band.wpsStatus.equals("enabled", ignoreCase = true)
        Text(
            if (wpsReady) "WPS: готов" else "WPS: ${band.wpsStatus.ifBlank { "выкл" }}",
            style = MaterialTheme.typography.bodySmall,
            color = if (wpsReady) KeeneticColors.Primary else KeeneticColors.TextSecondary
        )
        Switch(checked = band.enabled, onCheckedChange = onToggleBand)
    }
}

@Composable
private fun MwsMemberCard(member: MwsMember) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(member.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            InfoRow("MAC", member.mac)
            InfoRow("IP", member.ip)
            InfoRow("Статус", member.status)
            InfoRow("Прошивка", member.firmware)
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