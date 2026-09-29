package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun InternetDetailedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val detailed by viewModel.internetDetailed.collectAsState()
    val switchPorts by viewModel.switchPorts.collectAsState()

    fun refresh() {
        viewModel.loadInternetDetailed()
        // Порты LAN приходят из того же ответа show/interface, что и интерфейсы.
        viewModel.loadInterfaces()
    }

    LaunchedEffect(Unit) { refresh() }

    val lanPorts = remember(switchPorts) { switchPorts.sortedBy { it.id } }

    SectionScaffold(
        title = "Интернет (подробно)",
        subtitle = "статус подключения и порты LAN",
        onBack = onBack,
        onRefresh = { refresh() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                SectionCard(title = "Статус подключения", icon = Icons.Default.Public) {
                    InfoRow(
                        "Подключён",
                        if (detailed.connected) "Да" else "Нет",
                        valueColor = if (detailed.connected) KeeneticColors.Primary else KeeneticColors.TextSecondary
                    )
                    InfoRow("Интерфейс", detailed.interfaceName.ifBlank { "—" })
                    InfoRow("IP-адрес", detailed.ip.ifBlank { "—" }, monospace = true)
                    InfoRow("Маска", detailed.mask.ifBlank { "—" }, monospace = true)
                    InfoRow("Шлюз", detailed.gateway.ifBlank { "—" }, monospace = true)
                    InfoRow("Тип", detailed.type.ifBlank { "—" })
                    if (detailed.speed.isNotBlank()) InfoRow("Скорость", detailed.speed)
                    if (detailed.dns.isNotEmpty()) {
                        RowDivider()
                        SubGroupHeader("DNS", detailed.dns.size)
                        detailed.dns.forEach { server ->
                            InfoRow("  сервер", server, monospace = true)
                        }
                    }
                }
            }

            item(key = "ports") {
                SectionCard(
                    title = "Проводные порты LAN",
                    icon = Icons.Default.SettingsEthernet,
                    subtitle = "включение/выключение — interface <порт> up/down"
                ) {
                    if (lanPorts.isEmpty()) {
                        EmptyHint("Нет данных о физических LAN-портах в show/interface")
                    }
                    lanPorts.forEachIndexed { index, port ->
                        if (index > 0) RowDivider()
                        SwitchRow(
                            label = port.name.ifBlank { port.id },
                            checked = port.state.equals("up", ignoreCase = true),
                            onCheckedChange = { checked -> viewModel.setPortUp(port.id, checked) },
                            description = port.speed.ifBlank { "скорость неизвестна" }
                        )
                    }
                    if (lanPorts.isNotEmpty()) {
                        RowDivider()
                        EmptyHint("Выключение порта отключит устройство, подключённое к нему кабелем")
                    }
                }
            }
        }
    }
}
