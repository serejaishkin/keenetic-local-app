package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.PortForwardingRule
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

private val PF_PROTOCOLS = listOf("TCP", "UDP", "TCP/UDP")

@Composable
fun PortForwardingScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rules by viewModel.portForwardingRules.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

    var editingRule by remember { mutableStateOf<PortForwardingRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<PortForwardingRule?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPortForwardingRules()
        if (interfaces.isEmpty()) viewModel.loadInterfaces()
    }

    val interfaceNames = interfaces.map { it.id }.ifEmpty { listOf("ISP") }

    SectionScaffold(
        title = "Переадресация портов",
        subtitle = "Доступ к устройствам из интернета",
        onBack = onBack,
        onRefresh = { viewModel.loadPortForwardingRules() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "list-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubGroupHeader("Правила переадресации", rules.size)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            editingRule = null
                            showEditor = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", color = KeeneticColors.Primary)
                    }
                }
            }

            if (rules.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint(
                            "Правила переадресации не настроены. Переадресация портов " +
                                "(port forwarding) открывает доступ из интернета к устройствам " +
                                "локальной сети: NAS, камерам, игровым серверам."
                        )
                    }
                }
            } else {
                items(rules, key = { it.id.ifBlank { it.name + it.srcPort } }) { rule ->
                    PortForwardingRuleRow(
                        rule = rule,
                        onEdit = {
                            editingRule = rule
                            showEditor = true
                        },
                        onDelete = { ruleToDelete = rule }
                    )
                }
            }
        }
    }

    if (showEditor) {
        PortForwardingRuleDialog(
            initial = editingRule,
            interfaces = interfaceNames,
            onDismiss = {
                showEditor = false
                editingRule = null
            },
            onSave = { rule ->
                viewModel.addPortForwardingRule(rule)
                showEditor = false
                editingRule = null
            }
        )
    }

    ruleToDelete?.let { rule ->
        ConfirmDialog(
            title = "Удалить правило?",
            message = "Правило «${rule.name}» (порт ${rule.srcPort}) будет удалено с роутера. " +
                "Отменить это действие нельзя.",
            onConfirm = {
                viewModel.deletePortForwardingRule(rule.id)
                ruleToDelete = null
            },
            onDismiss = { ruleToDelete = null }
        )
    }
}

@Composable
private fun PortForwardingRuleRow(
    rule: PortForwardingRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(
        title = rule.name,
        subtitle = "${rule.proto} · ${rule.interfaceName}",
        icon = Icons.Default.Router
    ) {
        InfoRow(label = "Входящий порт", value = rule.srcPort, monospace = true)
        InfoRow(label = "Адресат", value = "${rule.dstIp}:${rule.dstPort}", monospace = true)
        RowDivider()
        EditableRow(label = "Правило переадресации", value = "изменить", onClick = onEdit)
        TextButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Удалить правило", color = KeeneticColors.Error)
        }
    }
}

@Composable
private fun PortForwardingRuleDialog(
    initial: PortForwardingRule?,
    interfaces: List<String>,
    onDismiss: () -> Unit,
    onSave: (PortForwardingRule) -> Unit
) {
    val from = initial
    var name by remember(from) { mutableStateOf(from?.name ?: "") }
    var proto by remember(from) { mutableStateOf(from?.proto ?: "TCP") }
    var srcPort by remember(from) { mutableStateOf(from?.srcPort ?: "") }
    var dstIp by remember(from) { mutableStateOf(from?.dstIp ?: "") }
    var dstPort by remember(from) { mutableStateOf(from?.dstPort ?: "") }
    var iface by remember(from) { mutableStateOf(from?.interfaceName ?: "ISP") }
    var showInterfaces by remember { mutableStateOf(false) }

    val valid = name.isNotBlank() && srcPort.isNotBlank() && dstIp.isNotBlank()

    FormDialog(
        title = if (from == null) "Новое правило переадресации" else "Правило переадресации",
        confirmLabel = if (from == null) "Создать" else "Сохранить",
        confirmEnabled = valid,
        onDismiss = onDismiss,
        onConfirm = {
            onSave(
                PortForwardingRule(
                    id = from?.id.orEmpty(),
                    name = name.trim(),
                    proto = proto,
                    srcPort = srcPort.trim(),
                    dstIp = dstIp.trim(),
                    dstPort = dstPort.trim().ifBlank { srcPort.trim() },
                    interfaceName = iface,
                    enabled = from?.enabled ?: true
                )
            )
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название правила") },
            placeholder = { Text("Например: Web-сервер, NAS, SSH") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Протокол",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PF_PROTOCOLS.forEach { value ->
                FilterChip(
                    selected = proto == value,
                    onClick = { proto = value },
                    label = { Text(value) }
                )
            }
        }

        OutlinedTextField(
            value = srcPort,
            onValueChange = { srcPort = it.filter { ch -> ch.isDigit() } },
            label = { Text("Входящий порт (WAN)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dstIp,
            onValueChange = { dstIp = it },
            label = { Text("IP-адрес в локальной сети") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dstPort,
            onValueChange = { dstPort = it.filter { ch -> ch.isDigit() } },
            label = { Text("Порт назначения") },
            supportingText = { Text("Пусто — использовать входящий порт") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { showInterfaces = true }) {
            Text("Входной интерфейс: $iface", color = KeeneticColors.Primary)
        }
    }

    if (showInterfaces) {
        OptionPickerDialog(
            title = "Входной интерфейс",
            options = interfaces.map { it to it },
            selectedKey = iface,
            onSelect = { iface = it },
            onDismiss = { showInterfaces = false }
        )
    }
}
