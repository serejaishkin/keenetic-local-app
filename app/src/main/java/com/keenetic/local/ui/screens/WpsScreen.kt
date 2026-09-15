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
import com.keenetic.local.api.MwsWlan
import com.keenetic.local.api.MwsWlanBand
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun WpsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val wpsStatus by viewModel.wpsStatus.collectAsState()
    val wlanList by viewModel.mwsWlanList.collectAsState()
    val autoPinMode = remember(wlanList) {
        wlanList.any { wlan -> wlan.bands.any { b -> b.accessPointId.contains("AccessPoint0") && b.wpsAutoSelfPin } }
    }

    LaunchedEffect(Unit) { viewModel.loadWpsStatus() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("WPS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadWpsStatus() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Настройки WPS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("WPS включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = wpsStatus.enabled, onCheckedChange = { viewModel.setWpsEnabled(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Авто-PIN (auto-self-pin)", color = KeeneticColors.TextPrimary)
                        Switch(checked = autoPinMode, onCheckedChange = { viewModel.setWpsAutoSelfPin(it) })
                    }
                    InfoRow("PIN роутера", wpsStatus.pin)
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Button(
                        onClick = { viewModel.startWpsButton() },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Подключить клиента (кнопка WPS)")
                    }
                    Text(
                        "Команда wps.button direction=receive запускает сессию WPS PBC на основной точке доступа (проверено на KN-2311).",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }

        if (wlanList.isNotEmpty()) {
            item { Text("Точки доступа Wi-Fi", style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(wlanList) { wlan ->
                WpsWlanCard(wlan)
            }
        }
    }
}

@Composable
private fun WpsWlanCard(wlan: MwsWlan) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Text(wlan.id, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
            wlan.bands.forEach { band ->
                WpsBandRow(band)
            }
        }
    }
}

@Composable
private fun WpsBandRow(band: MwsWlanBand) {
    val label = when (band.band) {
        "0" -> "2.4 ГГц"
        "1" -> "5 ГГц"
        else -> "Полоса ${band.band}"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
            if (band.accessPointId.isNotBlank()) {
                Text(band.accessPointId, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
        }
        val configured = band.wpsConfigured
        val statusOk = band.wpsStatus.equals("enabled", ignoreCase = true)
        Text(
            when {
                !configured -> "WPS недоступен"
                statusOk -> "WPS готов"
                else -> "WPS: ${band.wpsStatus}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                !configured -> KeeneticColors.TextSecondary
                statusOk -> KeeneticColors.Primary
                else -> KeeneticColors.Warning
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}