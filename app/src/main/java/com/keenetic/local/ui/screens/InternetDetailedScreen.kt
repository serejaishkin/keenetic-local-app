package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun InternetDetailedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val detailed by viewModel.internetDetailed.collectAsState()
    val switchPorts by viewModel.switchPorts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInternetDetailed()
        viewModel.loadInterfaces()
    }

    val lanPorts = remember(switchPorts) { switchPorts.sortedBy { it.id } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Интернет (подробно)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Статус подключения", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Подключён", color = KeeneticColors.TextPrimary)
                        Text(if (detailed.connected) "Да" else "Нет", color = if (detailed.connected) KeeneticColors.Primary else KeeneticColors.TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    InfoRow("Интерфейс", detailed.interfaceName)
                    InfoRow("IP-адрес", detailed.ip)
                    InfoRow("Маска", detailed.mask)
                    InfoRow("Шлюз", detailed.gateway)
                    InfoRow("Тип", detailed.type)
                    InfoRow("Скорость", detailed.speed)
                    if (detailed.dns.isNotEmpty()) {
                        InfoRow("DNS", detailed.dns.joinToString(", "))
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Проводные порты LAN", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (lanPorts.isEmpty()) {
                        Text(
                            "Нет данных о физических LAN-портах в show/interface",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextSecondary
                        )
                    } else {
                        lanPorts.forEach { port ->
                            val up = port.state.equals("up", ignoreCase = true)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(port.name.ifBlank { port.id }, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (up) "Up • ${port.speed.ifBlank { "—" }}" else "Down",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (up) KeeneticColors.Primary else KeeneticColors.TextSecondary
                                    )
                                }
                                Text(
                                    if (up) "Включён" else "Выключен",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                                Switch(
                                    checked = up,
                                    onCheckedChange = { checked ->
                                        viewModel.setPortUp(port.id, checked)
                                    }
                                )
                            }
                            InfoRow("Состояние", port.state.ifBlank { port.state })
                            HorizontalDivider(color = KeeneticColors.Divider)
                        }
                        Text(
                            "Включение/выключение порта выполняется командой interface <порт> up/down (проверено на KN-2311). Будьте осторожны: выключение порта отключит устройство, подключённое к нему кабелем.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
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
