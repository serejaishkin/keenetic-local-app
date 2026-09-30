package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.LanSegment
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun LanSegmentsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val segments by viewModel.lanSegments.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    var selectedSegment by remember { mutableStateOf<LanSegment?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLanSegments()
    }

    SectionScaffold(
        title = "Сегменты сети (VLAN)",
        subtitle = "Локальные интерфейсы и мосты роутера",
        onBack = onBack,
        onRefresh = { viewModel.loadLanSegments() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (feedbackMessage != null) {
                item {
                    SectionCard(title = "Статус") {
                        InfoRow(label = "Результат", value = feedbackMessage ?: "")
                    }
                }
            }

            if (segments.isEmpty()) {
                item {
                    SectionCard(title = "Сегменты", icon = Icons.Default.Hub) {
                        EmptyHint(text = "Сегменты сети не обнаружены. Опросите локальные интерфейсы и мосты роутера или настройте сегмент на существующем интерфейсе.")
                        Button(
                            onClick = { viewModel.loadLanSegments() },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Опросить интерфейсы")
                        }
                    }
                }
            } else {
                item {
                    SectionCard(title = "Сегменты", icon = Icons.Default.Hub) {
                        SubGroupHeader(title = "Найдено", count = segments.size)
                    }
                }
                items(segments, key = { it.id }) { seg ->
                    SectionCard(
                        title = seg.name,
                        icon = Icons.Default.Hub,
                        subtitle = if (seg.isolateClients) "Изоляция клиентов включена" else null
                    ) {
                        InfoRow(label = "IP", value = seg.ip, monospace = true)
                        InfoRow(label = "Маска", value = seg.mask, monospace = true)
                        InfoRow(
                            label = "DHCP пул",
                            value = if (seg.dhcpStart.isNotBlank() || seg.dhcpEnd.isNotBlank()) {
                                "${seg.dhcpStart} - ${seg.dhcpEnd}"
                            } else {
                                "не задан"
                            },
                            monospace = true
                        )
                        EditableRow(
                            label = "Параметры сегмента",
                            value = "Настроить",
                            onClick = { selectedSegment = seg }
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Новый сегмент",
                    icon = Icons.Default.Add,
                    subtitle = "Настраивается на базе существующего интерфейса"
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить сегмент (VLAN)")
                    }
                }
            }
        }
    }

    // Edit Segment Dialog — updateLanSegment вызывается с id существующего интерфейса
    selectedSegment?.let { seg ->
        var ip by remember(seg.id) { mutableStateOf(seg.ip) }
        var mask by remember(seg.id) { mutableStateOf(seg.mask) }
        var dhcpStart by remember(seg.id) { mutableStateOf(seg.dhcpStart) }
        var dhcpEnd by remember(seg.id) { mutableStateOf(seg.dhcpEnd) }
        var isolate by remember(seg.id) { mutableStateOf(seg.isolateClients) }

        FormDialog(
            title = "Настройки сегмента: ${seg.name}",
            confirmLabel = "Сохранить",
            onDismiss = { selectedSegment = null },
            onConfirm = {
                viewModel.updateLanSegment(seg.id, ip, mask, dhcpStart, dhcpEnd, isolate)
                feedbackMessage = "Параметры сегмента «${seg.name}» обновлены!"
                selectedSegment = null
            }
        ) {
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
            OutlinedTextField(
                value = dhcpStart,
                onValueChange = { dhcpStart = it },
                label = { Text("DHCP от") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dhcpEnd,
                onValueChange = { dhcpEnd = it },
                label = { Text("DHCP до") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchRow(
                label = "Изоляция клиентов",
                checked = isolate,
                onCheckedChange = { isolate = it },
                description = "Запрет взаимодействия между хостами в сегменте"
            )
        }
    }

    // Add Segment Dialog — сегмент выбирается из СУЩЕСТВУЮЩИХ интерфейсов
    // (interfaces из ViewModel); свободного ввода имени нет, create-bridge нет.
    if (showAddDialog) {
        val candidates = interfaces.filter { iface -> !iface.ip.isNullOrBlank() && segments.none { it.id == iface.id } }
        var selectedId by remember { mutableStateOf("") }
        var ip by remember(selectedId) { mutableStateOf(candidates.firstOrNull { it.id == selectedId }?.ip.orEmpty()) }
        var mask by remember(selectedId) { mutableStateOf(candidates.firstOrNull { it.id == selectedId }?.mask.orEmpty()) }
        var dhcpStart by remember(selectedId) { mutableStateOf("") }
        var dhcpEnd by remember(selectedId) { mutableStateOf("") }
        var isolate by remember { mutableStateOf(false) }
        var showIfacePicker by remember { mutableStateOf(false) }
        val selectedIface = candidates.find { it.id == selectedId }

        FormDialog(
            title = "Добавить сегмент (VLAN)",
            confirmLabel = "Создать",
            onDismiss = { showAddDialog = false },
            onConfirm = {
                if (selectedId.isBlank()) {
                    feedbackMessage = "Выберите интерфейс для сегмента"
                } else {
                    viewModel.updateLanSegment(selectedId, ip, mask, dhcpStart, dhcpEnd, isolate)
                    feedbackMessage = "Сегмент «$selectedId» настроен!"
                    showAddDialog = false
                }
            },
            confirmEnabled = selectedId.isNotBlank()
        ) {
            InfoRow(
                label = "Подсказка",
                value = "Сегмент настраивается на базе существующего интерфейса роутера"
            )
            if (candidates.isEmpty()) {
                EmptyHint(text = "Нет интерфейсов без настроенного сегмента.")
            } else {
                EditableRow(
                    label = "Интерфейс",
                    value = selectedIface?.let { "${it.id} (${it.name})" } ?: "Выберите…",
                    onClick = { showIfacePicker = true }
                )
            }
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
            OutlinedTextField(
                value = dhcpStart,
                onValueChange = { dhcpStart = it },
                label = { Text("DHCP от") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dhcpEnd,
                onValueChange = { dhcpEnd = it },
                label = { Text("DHCP до") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchRow(
                label = "Изоляция клиентов",
                checked = isolate,
                onCheckedChange = { isolate = it }
            )
        }

        if (showIfacePicker) {
            OptionPickerDialog(
                title = "Интерфейс",
                options = candidates.map { it.id to "${it.id} (${it.name}) · ${it.ip}" },
                selectedKey = selectedId,
                onSelect = { key ->
                    selectedId = key
                    ip = candidates.firstOrNull { it.id == key }?.ip.orEmpty()
                    mask = candidates.firstOrNull { it.id == key }?.mask.orEmpty()
                },
                onDismiss = { showIfacePicker = false }
            )
        }
    }
}
