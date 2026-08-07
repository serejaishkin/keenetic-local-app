package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
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
import com.keenetic.local.api.DeviceListEntry
import com.keenetic.local.api.IpPolicy
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f ГБ".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f МБ".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.1f КБ".format(bytes / 1024.0)
    else -> "$bytes Б"
}

@Composable
fun DevicesScreen(viewModel: RouterViewModel) {
    val clients by viewModel.clients.collectAsState()
    val deviceList by viewModel.deviceList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val ipPolicies by viewModel.ipPolicies.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadClients()
        viewModel.loadDeviceList()
    }

    val useRichData = deviceList.isNotEmpty()
    val totalCount = if (useRichData) deviceList.size else clients.size
    val isEmpty = if (useRichData) deviceList.isEmpty() else clients.isEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Подключённые устройства",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$totalCount устройств · нажми на устройство для настроек",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isEmpty) {
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
                    Button(onClick = { viewModel.loadClients(); viewModel.loadDeviceList() }) {
                        Text("Обновить")
                    }
                }
            }
        } else if (useRichData) {
            val sorted = deviceList.sortedWith(compareBy({ !it.active }, { -(it.rxbytes + it.txbytes) }))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted.size) { index ->
                    DeviceCard(entry = sorted[index], viewModel = viewModel, ipPolicies = ipPolicies)
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

    if (isLoading && isEmpty) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

/**
 * Единая строка действия внутри нижнего листа настроек устройства - раньше
 * это были DropdownMenuItem внутри мелкого "⋮" меню, которое легко не
 * заметить на телефоне. Теперь весь список действий виден сразу после тапа
 * по самому устройству.
 */
@Composable
private fun DeviceActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color = KeeneticColors.Primary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = KeeneticColors.TextPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(entry: DeviceListEntry, viewModel: RouterViewModel, ipPolicies: List<IpPolicy> = emptyList()) {
    val isBlocked = entry.access == "deny"
    var showSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        colors = CardDefaults.cardColors(
            containerColor = if (entry.active) KeeneticColors.Surface else KeeneticColors.Surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (entry.active) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (entry.wired) Icons.Default.Cable else Icons.Default.Smartphone,
                contentDescription = null,
                tint = when {
                    isBlocked -> KeeneticColors.Error
                    !entry.active -> KeeneticColors.TextSecondary
                    else -> KeeneticColors.Primary
                },
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName
                        ?: com.keenetic.local.util.OuiLookup.guessName(entry.mac)
                        ?: "Неизвестное устройство",
                    fontWeight = FontWeight.Medium,
                    color = if (isBlocked) KeeneticColors.Error else KeeneticColors.TextPrimary
                )
                Text(
                    text = "${entry.ip ?: "—"} · ${entry.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                if (entry.active && (entry.rxbytes > 0 || entry.txbytes > 0)) {
                    Text(
                        text = "${formatBytes(entry.rxbytes)} ↓ · ${formatBytes(entry.txbytes)} ↑",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Row {
                    if (isBlocked) {
                        Text("Заблокировано", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.Error)
                    } else if (!entry.active) {
                        Text("Офлайн", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                    }
                    if (!entry.policy.isNullOrBlank()) {
                        Text(
                            "  ·  политика: ${entry.policy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.Primary
                        )
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Настройки устройства", tint = KeeneticColors.TextSecondary)
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (entry.wired) Icons.Default.Cable else Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = if (isBlocked) KeeneticColors.Error else KeeneticColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            entry.displayName
                                ?: com.keenetic.local.util.OuiLookup.guessName(entry.mac)
                                ?: "Неизвестное устройство",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${entry.ip ?: "—"} · ${entry.mac}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Статус", if (entry.active) "Онлайн" else "Офлайн")
                if (entry.active && (entry.rxbytes > 0 || entry.txbytes > 0)) {
                    InfoRow("Трафик", "${formatBytes(entry.rxbytes)} ↓ · ${formatBytes(entry.txbytes)} ↑")
                }
                if (!entry.policy.isNullOrBlank()) {
                    InfoRow("Политика маршрутизации", entry.policy)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DeviceActionRow(Icons.Default.Edit, "Переименовать") {
                    showSheet = false; showRenameDialog = true
                }
                DeviceActionRow(Icons.Default.Route, "Политика маршрутизации") {
                    showSheet = false; showPolicyDialog = true
                }
                DeviceActionRow(Icons.Default.Schedule, "Расписание доступа") {
                    showSheet = false; showScheduleDialog = true
                }
                DeviceActionRow(
                    icon = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    label = if (isBlocked) "Разблокировать" else "Заблокировать",
                    tint = if (isBlocked) KeeneticColors.Accent else KeeneticColors.Error
                ) {
                    viewModel.toggleClient(entry.mac, !isBlocked)
                    showSheet = false
                }
            }
        }
    }

    if (showPolicyDialog) {
        var policyName by remember { mutableStateOf(entry.policy ?: "") }
        var policyLabel by remember { mutableStateOf(entry.policy ?: "") }
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Политика маршрутизации") },
            text = {
                Column {
                    if (ipPolicies.isNotEmpty()) {
                        Box {
                            OutlinedTextField(
                                value = policyLabel,
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
                                ipPolicies.forEach { policy ->
                                    val label = policy.description ?: policy.name ?: "—"
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            policyName = policy.name ?: ""
                                            policyLabel = label
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
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setClientPolicy(entry.mac, policyName)
                        showPolicyDialog = false
                    },
                    enabled = policyName.isNotBlank()
                ) {
                    Text("Применить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPolicyDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showScheduleDialog) {
        var scheduleName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Расписание доступа") },
            text = {
                Column {
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        label = { Text("Имя расписания") },
                        placeholder = { Text("например schedule0") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Расписание должно быть создано заранее в Настройках. Привязка к устройству не проверена реальным действием на роутере - тестируй сначала на некритичном устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.setClientSchedule(entry.mac, scheduleName); showScheduleDialog = false },
                    enabled = scheduleName.isNotBlank()
                ) {
                    Text("Применить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(entry.customName ?: "") }
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
                    onClick = { viewModel.renameDevice(entry.mac, newName); showRenameDialog = false },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Сохранить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCard(client: Client, viewModel: RouterViewModel, ipPolicies: List<IpPolicy> = emptyList()) {
    val isBlocked = client.access == "deny"
    var showSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
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
                    text = client.name?.takeIf { it.isNotBlank() }
                        ?: com.keenetic.local.util.OuiLookup.guessName(client.mac)
                        ?: "Неизвестное устройство",
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
            Icon(Icons.Default.ChevronRight, contentDescription = "Настройки устройства", tint = KeeneticColors.TextSecondary)
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = if (isBlocked) KeeneticColors.Error else KeeneticColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            client.name?.takeIf { it.isNotBlank() }
                                ?: com.keenetic.local.util.OuiLookup.guessName(client.mac)
                                ?: "Неизвестное устройство",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${client.ip ?: "—"} · ${client.mac ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Статус", if (isBlocked) "Заблокировано" else "Разрешено")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DeviceActionRow(Icons.Default.Edit, "Переименовать") {
                    showSheet = false; showRenameDialog = true
                }
                DeviceActionRow(Icons.Default.Route, "Политика маршрутизации") {
                    showSheet = false; showPolicyDialog = true
                }
                if (client.mac != null) {
                    DeviceActionRow(Icons.Default.Schedule, "Расписание доступа") {
                        showSheet = false; showScheduleDialog = true
                    }
                }
                DeviceActionRow(
                    icon = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    label = if (isBlocked) "Разблокировать" else "Заблокировать",
                    tint = if (isBlocked) KeeneticColors.Accent else KeeneticColors.Error
                ) {
                    viewModel.toggleClient(client.mac ?: "", !isBlocked)
                    showSheet = false
                }
            }
        }
    }

    if (showPolicyDialog) {
        var policyName by remember { mutableStateOf("") }
        var policyLabel by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Политика маршрутизации") },
            text = {
                Column {
                    if (ipPolicies.isNotEmpty()) {
                        Box {
                            OutlinedTextField(
                                value = policyLabel,
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
                                ipPolicies.forEach { policy ->
                                    val label = policy.description ?: policy.name ?: "—"
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            policyName = policy.name ?: ""
                                            policyLabel = label
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

    if (showScheduleDialog) {
        var scheduleName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Расписание доступа") },
            text = {
                Column {
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        label = { Text("Имя расписания") },
                        placeholder = { Text("например schedule0") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Расписание должно быть создано заранее в Настройках. Привязка к устройству не проверена реальным действием на роутере - тестируй сначала на некритичном устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        client.mac?.let { viewModel.setClientSchedule(it, scheduleName) }
                        showScheduleDialog = false
                    },
                    enabled = scheduleName.isNotBlank() && client.mac != null
                ) {
                    Text("Применить", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text("Отмена") }
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
