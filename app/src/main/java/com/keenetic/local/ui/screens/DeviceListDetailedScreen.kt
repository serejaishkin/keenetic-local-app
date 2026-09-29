package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.DeviceListEntryFull
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DeviceListDetailedScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val devices by viewModel.deviceListFull.collectAsState()

    fun refresh() = viewModel.loadDeviceListFull()

    LaunchedEffect(Unit) { refresh() }

    val online = devices.filter { it.online }
    val offline = devices.filter { !it.online }

    SectionScaffold(
        title = "Список устройств",
        subtitle = "онлайн ${online.size} из ${devices.size}",
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
            if (devices.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint("Список устройств пуст")
                    }
                }
            }

            if (online.isNotEmpty()) {
                item(key = "online-header") { SubGroupHeader("Подключены", online.size) }
                items(online, key = { "on-${it.mac}" }) { DeviceCard(it) }
            }

            if (offline.isNotEmpty()) {
                item(key = "offline-header") { SubGroupHeader("Не в сети", offline.size) }
                items(offline, key = { "off-${it.mac}" }) { DeviceCard(it) }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceListEntryFull) {
    SectionCard(title = device.name.ifBlank { "Устройство" }) {
        Text(
            text = if (device.online) "В сети" else "Не в сети",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (device.online) KeeneticColors.Primary else KeeneticColors.TextSecondary
        )
        RowDivider()
        InfoRow("MAC", device.mac.ifBlank { "—" }, monospace = true)
        InfoRow("IP", device.ip.ifBlank { "—" }, monospace = true)
        InfoRow("Хост", device.hostname.ifBlank { "—"})
        InfoRow("Интерфейс", device.interfaceName.ifBlank { "—"})
        InfoRow("Тип", device.type.ifBlank { "—"})
        if (device.policy.isNotBlank()) InfoRow("Политика", device.policy)
        if (device.schedule.isNotBlank()) InfoRow("Расписание", device.schedule)
    }
}
