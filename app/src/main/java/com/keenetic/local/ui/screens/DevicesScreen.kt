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
import com.keenetic.local.api.Client
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DevicesScreen(viewModel: RouterViewModel) {
    val clients by viewModel.clients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val ipPolicies by viewModel.ipPolicies.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadClients()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Подключённые устройства",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${clients.size} устройств",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (clients.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DevicesOther,
                        contentDescription = null,
                        tint = KeeneticColors.TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Список устройств пуст",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Роутер не вернул активных клиентов или данные пока недоступны",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadClients() }) {
                        Text("Обновить")
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clients.size) { index ->
                    ClientCard(client = clients[index], viewModel = viewModel, ipPolicies = ipPolicies)
                }
            }
        }
    }

    if (isLoading && clients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun ClientCard(client: Client, viewModel: RouterViewModel, ipPolicies: List<String> = emptyList()) {
    val isBlocked = client.access == "deny"
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = if (isBlocked) KeeneticColors.Error else KeeneticColors.Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name ?: "Unknown",
                    fontWeight = FontWeight.Medium,
                    color = if (isBlocked) KeeneticColors.Error else KeeneticColors.TextPrimary
                )
                Text(
                    text = "${client.ip ?: "—"}  •  ${client.mac ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                if (isBlocked) {
                    Text(
                        text = "Заблокировано",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.Error
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Переименовать") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = KeeneticColors.Primary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Политика маршрутизации") },
                        onClick = {
                            showMenu = false
                            showPolicyDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Route, contentDescription = null, tint = KeeneticColors.Primary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBlocked) "Разблокировать" else "Заблокировать") },
                        onClick = {
                            viewModel.toggleClient(client.mac ?: "", !isBlocked)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isBlocked) KeeneticColors.Accent else KeeneticColors.Error
                            )
                        }
                    )
                }
            }
        }
    }

    if (showPolicyDialog) {
        var policyName by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Политика маршрутизации") },
            text = {
                Column {
                    if (ipPolicies.isNotEmpty()) {
                        Box {
                            OutlinedTextField(
                                value = policyName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Политика") },
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                ipPolicies.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            policyName = name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = policyName,
                            onValueChange = { policyName = it },
                            label = { Text("Имя политики") },
                            placeholder = { Text("например Policy0") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Список политик с роутера не загрузился - введи имя вручную, оно должно совпадать с существующей политикой (ip policy ...).",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        client.mac?.let { viewModel.setClientPolicy(it, policyName) }
                        showPolicyDialog = false
                    },
                    enabled = policyName.isNotBlank() && client.mac != null
                ) {
                    Text("Применить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(client.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать устройство") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Имя устройства") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        client.mac?.let { viewModel.renameDevice(it, newName) }
                        showRenameDialog = false
                    },
                    enabled = newName.isNotBlank() && client.mac != null
                ) {
                    Text("Сохранить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}