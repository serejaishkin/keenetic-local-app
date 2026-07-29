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
                    "⚠️ Отдельные физические порты под интернет/IPTV/VoIP провайдера. Ошибка может отключить интернет на этом порту. Оставь поле пустым, если служба не нужна.",
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inetPort, onValueChange = { inetPort = it },
                        label = { Text("Интернет, порт №") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = iptvPort, onValueChange = { iptvPort = it },
                        label = { Text("IPTV, порт №") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = voipPort, onValueChange = { voipPort = it },
                        label = { Text("VoIP, порт №") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
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
