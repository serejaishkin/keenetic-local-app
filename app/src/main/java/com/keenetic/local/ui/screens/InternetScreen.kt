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
fun InternetScreen(viewModel: RouterViewModel) {
    val wans by viewModel.wans.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWans()
        viewModel.loadInterfaces()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Интернет",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                val wanId = wans?.wan?.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cable, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Текущее подключение", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (wans == null) {
                            Text("Загрузка...", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        } else {
                            Text("Интерфейс: ${wanId ?: "—"}", style = MaterialTheme.typography.bodySmall)
                            Text("IP: ${wans!!.wan?.ip ?: "—"}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (wans!!.wan?.enabled == true) "Активно" else "Неактивно",
                                color = if (wans!!.wan?.enabled == true) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (wans!!.wbk.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Резервные подключения:", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                                wans!!.wbk.forEach {
                                    Text("• ${it.id ?: "—"} (${it.ip ?: "нет IP"})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                PortRolesCard(viewModel, defaultWanId = wans?.wan?.id ?: "")
            }
        }
    }
}

@Composable
fun PortRolesCard(viewModel: RouterViewModel, defaultWanId: String) {
    var expanded by remember { mutableStateOf(false) }
    var wanId by remember(defaultWanId) { mutableStateOf(defaultWanId) }
    // Подтверждено ndmConstants.json (PORTS_MAP) с реального роутера:
    // LAN1..LAN4 -> порт "1".."4" на GigabitEthernet0, WAN -> порт "0" на
    // GigabitEthernet1. Раньше давали вписывать номер руками - теперь
    // выбор из реального списка, нельзя вписать несуществующий порт.
    val portOptions = listOf("Не назначен" to "", "LAN1" to "1", "LAN2" to "2", "LAN3" to "3", "LAN4" to "4")
    var inetPort by remember { mutableStateOf("") }
    var iptvPort by remember { mutableStateOf("") }
    var voipPort by remember { mutableStateOf("") }
    var confirmApply by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Назначение LAN-портов (IPoE)", fontWeight = FontWeight.Medium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Скрыть" else "Настроить")
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Отдельные физические порты под интернет/IPTV/VoIP провайдера. Ошибка может отключить интернет на этом порту. \"Не назначен\" = служба не нужна на отдельном порту.",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.Error
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = wanId,
                    onValueChange = { wanId = it },
                    label = { Text("WAN-интерфейс") },
                    placeholder = { Text("например GigabitEthernet0/Vlan4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                PortSelectorRow("Интернет", portOptions, inetPort) { inetPort = it }
                Spacer(modifier = Modifier.height(8.dp))
                PortSelectorRow("IPTV", portOptions, iptvPort) { iptvPort = it }
                Spacer(modifier = Modifier.height(8.dp))
                PortSelectorRow("VoIP", portOptions, voipPort) { voipPort = it }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { confirmApply = true },
                    enabled = wanId.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Применить")
                }
            }
        }
    }

    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = KeeneticColors.Error) },
            title = { Text("Точно применить?") },
            text = { Text("Неверные значения портов могут отключить интернет на роутере. Убедись, что номера портов верные.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setPortRoles(wanId, inetPort, iptvPort, voipPort)
                    confirmApply = false
                }) {
                    Text("Применить", color = KeeneticColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmApply = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun PortSelectorRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (title, value) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
