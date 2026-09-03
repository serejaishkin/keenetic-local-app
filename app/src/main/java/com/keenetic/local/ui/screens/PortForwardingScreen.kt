package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.PortForwardingRule
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun PortForwardingScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rules by viewModel.portForwardingRules.collectAsState()
    var selectedRule by remember { mutableStateOf<PortForwardingRule?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Add rule form fields
    var ruleName by remember { mutableStateOf("") }
    var ruleProto by remember { mutableStateOf("TCP") }
    var ruleSrcPort by remember { mutableStateOf("") }
    var ruleDstIp by remember { mutableStateOf("192.168.1.") }
    var ruleDstPort by remember { mutableStateOf("") }
    var ruleIface by remember { mutableStateOf("ISP") }

    LaunchedEffect(Unit) {
        viewModel.loadPortForwardingRules()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            feedbackMessage = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KeeneticColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить правило", tint = KeeneticColors.Background)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KeeneticColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Переадресация портов",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.loadPortForwardingRules() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                    }
                }
            }

            if (rules.isEmpty()) {
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
                                Icons.Default.Router,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Правила переадресации не настроены",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Переадресация портов (Port Forwarding / NAT) позволяет открыть доступ к внутренним серверам, NAS, игровым сервисам или камерам видеонаблюдения из интернета.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Создать правило переадресации")
                            }
                        }
                    }
                }
            } else {
                items(rules) { rule ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        modifier = Modifier.clickable { selectedRule = rule }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = KeeneticColors.Primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    rule.proto,
                                    color = KeeneticColors.Primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                                Text(
                                    "Порт ${rule.srcPort} → ${rule.dstIp}:${rule.dstPort} (${rule.interfaceName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deletePortForwardingRule(rule.id)
                                feedbackMessage = "Правило «${rule.name}» удалено"
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeeneticColors.Error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Rule Details Sub-actions Dialog
    selectedRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { selectedRule = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Правило: ${rule.name}", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Протокол", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.proto, fontWeight = FontWeight.Bold, color = KeeneticColors.Primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Входящий порт (WAN)", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.srcPort, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IP-адрес назначения", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.dstIp, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Порт назначения", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.dstPort, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Входной интерфейс", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.interfaceName, color = KeeneticColors.TextPrimary)
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    OutlinedButton(
                        onClick = {
                            viewModel.deletePortForwardingRule(rule.id)
                            feedbackMessage = "Правило «${rule.name}» удалено"
                            selectedRule = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Удалить правило")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRule = null }) {
                    Text("Закрыть", color = KeeneticColors.TextPrimary)
                }
            }
        )
    }

    // Add New Rule Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Новое правило NAT", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text("Название правила (напр. Web, NAS, SSH)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Протокол:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TCP", "UDP", "TCP/UDP").forEach { proto ->
                            FilterChip(
                                selected = ruleProto == proto,
                                onClick = { ruleProto = proto },
                                label = { Text(proto) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ruleSrcPort,
                        onValueChange = { ruleSrcPort = it },
                        label = { Text("Входящий порт (напр. 8080)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ruleDstIp,
                        onValueChange = { ruleDstIp = it },
                        label = { Text("IP-адрес в локальной сети") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ruleDstPort,
                        onValueChange = { ruleDstPort = it },
                        label = { Text("Порт назначения (напр. 80)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleName.isNotBlank() && ruleSrcPort.isNotBlank() && ruleDstIp.isNotBlank()) {
                            val newRule = PortForwardingRule(
                                id = (rules.size + 1).toString(),
                                name = ruleName,
                                proto = ruleProto,
                                srcPort = ruleSrcPort,
                                dstIp = ruleDstIp,
                                dstPort = ruleDstPort.ifBlank { ruleSrcPort },
                                interfaceName = ruleIface,
                                enabled = true
                            )
                            viewModel.addPortForwardingRule(newRule)
                            feedbackMessage = "Правило «$ruleName» добавлено"
                            showAddDialog = false
                            ruleName = ""
                            ruleSrcPort = ""
                            ruleDstPort = ""
                        }
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

