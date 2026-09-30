package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.MwsMember
import com.keenetic.local.api.MwsWlan
import com.keenetic.local.api.MwsWlanBand
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun MwsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val mwsStatus by viewModel.mwsStatus.collectAsState()
    val members by viewModel.mwsMembers.collectAsState()
    val wlanList by viewModel.mwsWlanList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMwsStatus()
        viewModel.loadMwsWlan()
    }

    SectionScaffold(
        title = "Mesh Wi-Fi",
        subtitle = "Состав и настройки Mesh-системы",
        onBack = onBack,
        onRefresh = {
            viewModel.loadMwsStatus()
            viewModel.loadMwsWlan()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                SectionCard(
                    title = "Статус MWS",
                    icon = Icons.Default.Wifi
                ) {
                    SwitchRow(
                        label = "Mesh Wi-Fi включён",
                        checked = mwsStatus.enabled,
                        onCheckedChange = { viewModel.setMwsEnabled(it) }
                    )
                    RowDivider()
                    InfoRow("Роль", mwsStatus.role.ifBlank { "—" })
                    InfoRow("SSID", mwsStatus.ssid.ifBlank { "—"}, monospace = true)
                    InfoRow("Канал", mwsStatus.channel.toString())
                }
            }

            if (wlanList.isNotEmpty()) {
                item(key = "wlan-header") {
                    SubGroupHeader("Сети Mesh (WLAN)", wlanList.size)
                }
                items(wlanList) { wlan ->
                    MwsWlanCard(wlan, onToggleWlan = { id, enabled -> viewModel.setMwsWlanEnabled(id, enabled) })
                }
                item(key = "wlan-note") {
                    SectionCard {
                        Text(
                            "Переключение «Включён» выполняет mws wlan {id} enable " +
                                "(проверено на KN-2311).",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            if (members.isNotEmpty()) {
                item(key = "members-header") {
                    SubGroupHeader("Участники", members.size)
                }
                items(members) { member ->
                    MwsMemberCard(member)
                }
            }
        }
    }
}

@Composable
private fun MwsWlanCard(wlan: MwsWlan, onToggleWlan: (String, Boolean) -> Unit) {
    SectionCard(title = wlan.id) {
        InfoRow("По расписанию выключена", if (wlan.disabledBySchedule) "Да" else "Нет")
        if (wlan.bands.isNotEmpty()) {
            RowDivider()
        }
        wlan.bands.forEach { band ->
            MwsBandRow(band, onToggleBand = { enabled -> onToggleWlan(wlan.id, enabled) })
        }
    }
}

@Composable
private fun MwsBandRow(band: MwsWlanBand, onToggleBand: (Boolean) -> Unit) {
    val label = when (band.band) {
        "0" -> "2.4 ГГц"
        "1" -> "5 ГГц"
        else -> "Полоса ${band.band}"
    }
    val wpsReady = band.wpsConfigured && band.wpsStatus.equals("enabled", ignoreCase = true)

    SectionCard {
        InfoRow(
            label = label,
            value = band.accessPointId.ifBlank { "точка доступа не задана" },
            monospace = true
        )
        InfoRow(
            label = "WPS",
            value = if (wpsReady) "готов" else band.wpsStatus.ifBlank { "выкл" },
            valueColor = if (wpsReady) KeeneticColors.Primary else KeeneticColors.TextSecondary
        )
        SwitchRow(
            label = "Полоса включена",
            checked = band.enabled,
            onCheckedChange = onToggleBand
        )
    }
}

@Composable
private fun MwsMemberCard(member: MwsMember) {
    SectionCard(title = member.name.ifBlank { "Участник" }) {
        InfoRow("MAC", member.mac, monospace = true)
        InfoRow("IP", member.ip, monospace = true)
        InfoRow("Статус", member.status.ifBlank { "—" })
        InfoRow("Прошивка", member.firmware.ifBlank { "—" }, monospace = true)
    }
}
