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

/**
 * Раздел "Межсетевой экран" (/firewall на сайте). Команда access-list
 * подтверждена реальным HAR. Поля формы и их подписи - из
 * assets/language/locale.ru.json (namespace "firewall") с реального роутера.
 */
@Composable
fun FirewallScreen(viewModel: RouterViewModel) {
    val wans by viewModel.wans.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadWans() }

    var wanId by remember(wans) { mutableStateOf(wans?.wan?.id ?: "") }
    var action by remember { mutableStateOf("permit") }
    var protocol by remember { mutableStateOf("") }
    var sourceIp by remember { mutableStateOf("0.0.0.0") }
    var sourceMask by remember { mutableStateOf("0.0.0.0") }
    var destIp by remember { mutableStateOf("0.0.0.0") }
    var destMask by remember { mutableStateOf("0.0.0.0") }
    var description by remember { mutableStateOf("") }
    var confirmApply by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Межсетевой экран",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Создание правила доступа для входящего трафика",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ Правило применяется к входящему трафику указанного WAN-интерфейса. Разрешить (permit) - подтверждено реальным тестом на роутере. Запретить (deny) - формат не подтверждён отдельно, экстраполирован по аналогии - проверь на некритичном правиле.",
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

                        Text("Действие", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = action == "permit", onClick = { action = "permit" }, label = { Text("Разрешить") })
                            FilterChip(selected = action == "deny", onClick = { action = "deny" }, label = { Text("Запретить") })
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Протокол", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = protocol == "", onClick = { protocol = "" }, label = { Text("Любой") })
                            FilterChip(selected = protocol == "tcp", onClick = { protocol = "tcp" }, label = { Text("TCP") })
                            FilterChip(selected = protocol == "udp", onClick = { protocol = "udp" }, label = { Text("UDP") })
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Источник", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sourceIp, onValueChange = { sourceIp = it },
                                label = { Text("IP") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sourceMask, onValueChange = { sourceMask = it },
                                label = { Text("Маска") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Назначение", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = destIp, onValueChange = { destIp = it },
                                label = { Text("IP") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = destMask, onValueChange = { destMask = it },
                                label = { Text("Маска") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Описание правила") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (error != null) {
                            Text(error ?: "", color = KeeneticColors.Error, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { confirmApply = true },
                            enabled = wanId.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Создать правило")
                        }
                    }
                }
            }
        }
    }

    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = KeeneticColors.Error) },
            title = { Text("Точно создать правило?") },
            text = { Text("Правило межсетевого экрана изменит доступность роутера из сети. Проверь настройки перед применением.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFirewallRule(wanId, action, protocol, sourceIp, sourceMask, destIp, destMask, description)
                    confirmApply = false
                }) {
                    Text("Создать", color = KeeneticColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmApply = false }) { Text("Отмена") }
            }
        )
    }
}
