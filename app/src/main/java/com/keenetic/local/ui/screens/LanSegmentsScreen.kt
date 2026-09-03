package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.LanSegment
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun LanSegmentsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val segments by viewModel.lanSegments.collectAsState()
    var selectedSegment by remember { mutableStateOf<LanSegment?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLanSegments()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KeeneticColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить сегмент", tint = KeeneticColors.Background)
            }
        },
        containerColor = KeeneticColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Hub, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Сегменты сети (VLAN)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.loadLanSegments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                    }
                }
            }

            if (feedbackMessage != null) {
                item {
                    Snackbar(
                        action = {
                            TextButton(onClick = { feedbackMessage = null }) {
                                Text("OK", color = KeeneticColors.Primary)
                            }
                        }
                    ) {
                        Text(feedbackMessage ?: "")
                    }
                }
            }

            if (segments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Hub,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Сегменты сети не обнаружены",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Нажмите кнопку обновления для опроса локальных интерфейсов и мостов роутера или создайте новый сегмент VLAN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadLanSegments() },
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Опросить интерфейсы", color = KeeneticColors.Primary)
                            }
                        }
                    }
                }
            } else {
                items(segments) { seg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSegment = seg },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = KeeneticColors.Primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(seg.name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                                Text("IP: ${seg.ip} • Маска: ${seg.mask}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                                Text("DHCP пул: ${seg.dhcpStart} - ${seg.dhcpEnd}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                            if (seg.isolateClients) {
                                Surface(
                                    color = KeeneticColors.Warning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Изоляция",
                                        color = KeeneticColors.Warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { selectedSegment = seg }) {
                                Icon(Icons.Default.Edit, contentDescription = "Настроить", tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Segment Dialog
    selectedSegment?.let { seg ->
        var ip by remember(seg.id) { mutableStateOf(seg.ip) }
        var mask by remember(seg.id) { mutableStateOf(seg.mask) }
        var dhcpStart by remember(seg.id) { mutableStateOf(seg.dhcpStart) }
        var dhcpEnd by remember(seg.id) { mutableStateOf(seg.dhcpEnd) }
        var isolate by remember(seg.id) { mutableStateOf(seg.isolateClients) }

        AlertDialog(
            onDismissRequest = { selectedSegment = null },
            title = {
                Text("Настройки сегмента: ${seg.name}", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP-адрес шлюза") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mask,
                        onValueChange = { mask = it },
                        label = { Text("Маска подсети") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dhcpStart,
                            onValueChange = { dhcpStart = it },
                            label = { Text("DHCP от") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dhcpEnd,
                            onValueChange = { dhcpEnd = it },
                            label = { Text("DHCP до") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Изоляция клиентов", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                            Text("Запрет взаимодействия между хостами в сегменте", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        }
                        Switch(checked = isolate, onCheckedChange = { isolate = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLanSegment(seg.id, ip, mask, dhcpStart, dhcpEnd, isolate)
                        feedbackMessage = "Параметры сегмента «${seg.name}» обновлены!"
                        selectedSegment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSegment = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    // Add New Segment Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("SmartHome") }
        var ip by remember { mutableStateOf("192.168.10.1") }
        var mask by remember { mutableStateOf("255.255.255.0") }
        var dhcpStart by remember { mutableStateOf("192.168.10.33") }
        var dhcpEnd by remember { mutableStateOf("192.168.10.100") }
        var isolate by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый сегмент (VLAN)", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя сегмента") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP-адрес шлюза") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dhcpStart,
                            onValueChange = { dhcpStart = it },
                            label = { Text("DHCP от") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dhcpEnd,
                            onValueChange = { dhcpEnd = it },
                            label = { Text("DHCP до") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Изоляция клиентов", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = isolate, onCheckedChange = { isolate = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLanSegment(name, ip, mask, dhcpStart, dhcpEnd, isolate)
                        feedbackMessage = "Сегмент «$name» создан!"
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}
