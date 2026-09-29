package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun NetworkMonitorScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val conntrack by viewModel.conntrackEntries.collectAsState()
    val arpEntries by viewModel.arpEntries.collectAsState()
    val neighbours by viewModel.neighbourEntries.collectAsState()
    val ipRules by viewModel.ipRules.collectAsState()
    val monitorStatus by viewModel.monitorStatus.collectAsState()

    fun loadAll() {
        viewModel.loadConntrack()
        viewModel.loadArpEntries()
        viewModel.loadNeighbourEntries()
        viewModel.loadIpRules()
        viewModel.loadMonitorStatus()
    }

    LaunchedEffect(Unit) { loadAll() }

    SectionScaffold(
        title = "Мониторинг сети",
        subtitle = "Соединения, ARP, IPv6- соседи, правила",
        onBack = onBack,
        onRefresh = { loadAll() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "monitor") {
                SectionCard(title = "Пакетный захват", icon = Icons.Default.Visibility) {
                    InfoRow(
                        "Состояние",
                        if (monitorStatus.active) "Активен" else "Остановлен",
                        valueColor = if (monitorStatus.active) KeeneticColors.Primary else KeeneticColors.TextSecondary
                    )
                    RowDivider()
                    InfoRow("Интерфейс", monitorStatus.interfaceName.ifBlank { "—" })
                    InfoRow("Фильтр", monitorStatus.filter.ifBlank { "—" })
                }
            }

            item(key = "conntrack") {
                SectionCard(
                    title = "Активные соединения",
                    icon = Icons.Default.SyncAlt,
                    subtitle = "первые 20 из ${conntrack.size}"
                ) {
                    if (conntrack.isEmpty()) EmptyHint("Соединений нет")
                    conntrack.take(20).forEach { entry ->
                        SubRow(
                            "${entry.protocol.ifBlank { "?" }} ${entry.srcIp.ifBlank { "?" }}:${entry.srcPort.ifBlank { "?" }}",
                            "→ ${entry.dstIp.ifBlank { "?" }}:${entry.dstPort.ifBlank { "?" }}"
                        )
                        InfoRow(
                            "  состояние",
                            entry.state.ifBlank { "—" },
                            valueColor = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            item(key = "arp") {
                SectionCard(
                    title = "ARP-таблица",
                    icon = Icons.AutoMirrored.Filled.List,
                    subtitle = "первые 20 из ${arpEntries.size}"
                ) {
                    if (arpEntries.isEmpty()) EmptyHint("ARP-таблица пуста")
                    arpEntries.take(20).forEach { entry ->
                        SubRow(entry.ip, "→ ${entry.mac}")
                        InfoRow(
                            "  интерфейс",
                            entry.interfaceName.ifBlank { "—" },
                            valueColor = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            item(key = "neighbours") {
                SectionCard(
                    title = "IPv6-соседи",
                    icon = Icons.Default.People,
                    subtitle = "первые 20 из ${neighbours.size}"
                ) {
                    if (neighbours.isEmpty()) EmptyHint("Соседей не найдено")
                    neighbours.take(20).forEach { entry ->
                        SubRow(entry.ip, "→ ${entry.mac}")
                        InfoRow(
                            "  интерфейс",
                            entry.interfaceName.ifBlank { "—" },
                            valueColor = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            item(key = "iprules") {
                SectionCard(
                    title = "IP-правила",
                    icon = Icons.AutoMirrored.Filled.Rule,
                    subtitle = "первые 20 из ${ipRules.size}"
                ) {
                    if (ipRules.isEmpty()) EmptyHint("Правил нет")
                    ipRules.take(20).forEach { rule ->
                        SubRow("Приоритет ${rule.priority}", rule.lookup.ifBlank { "—" })
                        InfoRow(
                            "  откуда",
                            rule.from.ifBlank { "—" },
                            valueColor = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubRow(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = KeeneticColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
    InfoRow(" ", value, monospace = true)
}
