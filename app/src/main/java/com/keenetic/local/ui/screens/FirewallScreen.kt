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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
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
import com.keenetic.local.api.FirewallRule
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

private const val FW_ANY = "any"
private val FW_PROTOCOLS = listOf("IP", "TCP", "UDP", "ICMP")

@Composable
fun FirewallScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val rules by viewModel.firewallRules.collectAsState()
    val unsupported by viewModel.unsupportedFeatures.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()

    var editingRule by remember { mutableStateOf<FirewallRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<FirewallRule?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkFeatureSupport("firewall", "ip/rule")
        viewModel.loadFirewallRules()
        if (interfaces.isEmpty()) viewModel.loadInterfaces()
    }

    val interfaceOptions = interfaces.map { it.id }

    SectionScaffold(
        title = "Межсетевой экран",
        subtitle = "Правила фильтрации трафика",
        onBack = onBack,
        onRefresh = { viewModel.loadFirewallRules() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (unsupported.contains("firewall")) {
                item(key = "unsupported") {
                    UnsupportedNotice(
                        "Межсетевой экран",
                        "RCI-путь show/ip/rule не поддерживается на данной прошивке KN-2311 (fw 5.01.C.4.0-1). " +
                            "Фильтрация трафика управляется через веб-интерфейс Keenetic, но не через REST API."
                    )
                }
            }

            item(key = "list-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubGroupHeader("Пользовательские правила", rules.size)
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
                            "Пользовательские правила отсутствуют. Действуют стандартные политики " +
                                "безопасности KeeneticOS: доступ из внешней сети закрыт, исходящий " +
                                "трафик домашней сети разрешён (NAT/Stateful Firewall)."
                        )
                    }
                }
            } else {
                items(rules, key = { it.id.ifBlank { it.comment + it.dstPort } }) { rule ->
                    FirewallRuleRow(
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
        FirewallRuleDialog(
            initial = editingRule,
            interfaces = interfaceOptions,
            onDismiss = {
                showEditor = false
                editingRule = null
            },
            onSave = { rule ->
                viewModel.addFirewallRule(rule)
                showEditor = false
                editingRule = null
            }
        )
    }

    ruleToDelete?.let { rule ->
        ConfirmDialog(
            title = "Удалить правило?",
            message = "Правило «${rule.comment.ifBlank { rule.proto + " " + rule.dstPort }}» " +
                "будет удалено с роутера. Отменить это действие нельзя.",
            onConfirm = {
                viewModel.deleteFirewallRule(rule.id)
                ruleToDelete = null
            },
            onDismiss = { ruleToDelete = null }
        )
    }
}

@Composable
private fun FirewallRuleRow(
    rule: FirewallRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPermit = rule.action.equals("permit", ignoreCase = true)
    SectionCard(
        title = rule.comment.ifBlank { "${rule.proto} · ${rule.dstIp}" },
        subtitle = if (isPermit) "Разрешить" else "Запретить",
        icon = if (isPermit) Icons.Default.Shield else Icons.Default.Block
    ) {
        InfoRow(
            label = "Источник",
            value = rule.srcIp,
            monospace = true,
            valueColor = if (isPermit) KeeneticColors.TextPrimary else KeeneticColors.Error
        )
        InfoRow(label = "Назначение", value = rule.dstIp, monospace = true)
        InfoRow(label = "Порт назначения", value = rule.dstPort, monospace = true)
        InfoRow(label = "Протокол", value = rule.proto)
        InfoRow(label = "Интерфейс", value = rule.interfaceName.ifBlank { "все интерфейсы" })
        InfoRow(
            label = "Состояние",
            value = if (rule.enabled) "Включено" else "Отключено",
            valueColor = if (rule.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary
        )
        RowDivider()
        EditableRow(label = "Правило безопасности", value = "изменить", onClick = onEdit)
        TextButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Удалить правило", color = KeeneticColors.Error)
        }
    }
}

@Composable
private fun FirewallRuleDialog(
    initial: FirewallRule?,
    interfaces: List<String>,
    onDismiss: () -> Unit,
    onSave: (FirewallRule) -> Unit
) {
    val from = initial
    var action by remember(from) {
        mutableStateOf(if (from?.action?.equals("deny", ignoreCase = true) == true) "deny" else "permit")
    }
    var proto by remember(from) { mutableStateOf(from?.proto ?: "IP") }
    var srcIp by remember(from) { mutableStateOf(from?.srcIp?.ifBlank { FW_ANY } ?: FW_ANY) }
    var dstIp by remember(from) { mutableStateOf(from?.dstIp?.ifBlank { FW_ANY } ?: FW_ANY) }
    var dstPort by remember(from) { mutableStateOf(from?.dstPort?.ifBlank { FW_ANY } ?: FW_ANY) }
    var iface by remember(from) { mutableStateOf(from?.interfaceName ?: "ISP") }
    var comment by remember(from) { mutableStateOf(from?.comment ?: "") }
    var showInterfaces by remember { mutableStateOf(false) }

    val valid = dstIp.isNotBlank() && dstPort.isNotBlank()

    FormDialog(
        title = if (from == null) "Новое правило безопасности" else "Правило безопасности",
        confirmLabel = if (from == null) "Создать" else "Сохранить",
        confirmEnabled = valid,
        onDismiss = onDismiss,
        onConfirm = {
            onSave(
                FirewallRule(
                    id = from?.id.orEmpty(),
                    action = action,
                    proto = proto,
                    srcIp = srcIp.trim(),
                    dstIp = dstIp.trim(),
                    dstPort = dstPort.trim(),
                    interfaceName = iface,
                    enabled = from?.enabled ?: true,
                    comment = comment.trim()
                )
            )
        }
    ) {
        Text(
            "Действие",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = action == "permit",
                onClick = { action = "permit" },
                label = { Text("Разрешить") }
            )
            FilterChip(
                selected = action == "deny",
                onClick = { action = "deny" },
                label = { Text("Запретить") }
            )
        }

        Text(
            "Протокол",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FW_PROTOCOLS.forEach { value ->
                FilterChip(
                    selected = proto == value,
                    onClick = { proto = value },
                    label = { Text(value) }
                )
            }
        }

        OutlinedTextField(
            value = srcIp,
            onValueChange = { srcIp = it },
            label = { Text("IP источника") },
            supportingText = { Text("any — любой адрес") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dstIp,
            onValueChange = { dstIp = it },
            label = { Text("IP назначения") },
            supportingText = { Text("any — любой адрес") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dstPort,
            onValueChange = { dstPort = it },
            label = { Text("Порт назначения") },
            supportingText = { Text("any — любой порт") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Описание правила") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { showInterfaces = true }) {
            Text(
                "Интерфейс: ${iface.ifBlank { "все интерфейсы" }}",
                color = KeeneticColors.Primary
            )
        }
    }

    if (showInterfaces) {
        OptionPickerDialog(
            title = "Интерфейс правила",
            options = listOf("" to "Все интерфейсы (глобальный список)") + interfaces.map { it to it },
            selectedKey = iface,
            onSelect = { iface = it },
            onDismiss = { showInterfaces = false }
        )
    }
}
