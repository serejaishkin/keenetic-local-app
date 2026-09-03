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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.FirewallRule
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun FirewallScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rules by viewModel.firewallRules.collectAsState()
    var selectedRule by remember { mutableStateOf<FirewallRule?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Add rule form fields
    var ruleAction by remember { mutableStateOf("permit") }
    var ruleProto by remember { mutableStateOf("IP") }
    var ruleSrcIp by remember { mutableStateOf("any") }
    var ruleDstIp by remember { mutableStateOf("any") }
    var ruleDstPort by remember { mutableStateOf("any") }
    var ruleIface by remember { mutableStateOf("ISP") }
    var ruleComment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadFirewallRules()
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
                    Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Межсетевой экран",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.loadFirewallRules() }) {
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
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Пользовательские правила отсутствуют",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Действуют стандартные политики безопасности KeeneticOS: доступ из внешней сети закрыт, исходящий трафик домашней сети разрешен (NAT/Stateful Firewall).",
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
                                Text("Создать правило безопасности")
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
                                color = if (rule.action.equals("permit", ignoreCase = true)) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.Error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (rule.action.equals("permit", ignoreCase = true)) "РАЗРЕШИТЬ" else "ЗАПРЕТИТЬ",
                                    color = if (rule.action.equals("permit", ignoreCase = true)) KeeneticColors.Success else KeeneticColors.Error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rule.comment.ifEmpty { "${rule.proto} • ${rule.dstPort}" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KeeneticColors.TextPrimary
                                )
                                Text(
                                    "${rule.srcIp} → ${rule.dstIp} (${rule.interfaceName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deleteFirewallRule(rule.id)
                                feedbackMessage = "Правило удалено"
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
        val isPermit = rule.action.equals("permit", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { selectedRule = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = if (isPermit) KeeneticColors.Success else KeeneticColors.Error)
                    Text("Правило безопасности", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Действие", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (isPermit) "Разрешить (Permit)" else "Запретить (Deny)",
                            fontWeight = FontWeight.Bold,
                            color = if (isPermit) KeeneticColors.Success else KeeneticColors.Error
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Протокол", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.proto, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Источник (Source)", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.srcIp, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Назначение (Destination)", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.dstIp, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Порт назначения", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.dstPort, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Интерфейс", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(rule.interfaceName, color = KeeneticColors.TextPrimary)
                    }
                    if (rule.comment.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Комментарий", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(rule.comment, color = KeeneticColors.TextPrimary)
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteFirewallRule(rule.id)
                            feedbackMessage = "Правило удалено"
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

    // Add New Firewall Rule Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Новое правило Firewall", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Действие:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = ruleAction == "permit",
                            onClick = { ruleAction = "permit" },
                            label = { Text("Разрешить (Permit)") }
                        )
                        FilterChip(
                            selected = ruleAction == "deny",
                            onClick = { ruleAction = "deny" },
                            label = { Text("Запретить (Deny)") }
                        )
                    }

                    Text("Протокол:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IP", "TCP", "UDP", "ICMP").forEach { proto ->
                            FilterChip(
                                selected = ruleProto == proto,
                                onClick = { ruleProto = proto },
                                label = { Text(proto) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ruleSrcIp,
                        onValueChange = { ruleSrcIp = it },
                        label = { Text("IP источника (или any)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ruleDstIp,
                        onValueChange = { ruleDstIp = it },
                        label = { Text("IP назначения (или any)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ruleDstPort,
                        onValueChange = { ruleDstPort = it },
                        label = { Text("Порт назначения (или any)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ruleComment,
                        onValueChange = { ruleComment = it },
                        label = { Text("Описание (напр. Блокировка DNS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newRule = FirewallRule(
                            id = (rules.size + 1).toString(),
                            action = ruleAction,
                            proto = ruleProto,
                            srcIp = ruleSrcIp,
                            dstIp = ruleDstIp,
                            dstPort = ruleDstPort,
                            interfaceName = ruleIface,
                            enabled = true,
                            comment = ruleComment
                        )
                        viewModel.addFirewallRule(newRule)
                        feedbackMessage = "Правило безопасности добавлено"
                        showAddDialog = false
                        ruleComment = ""
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

