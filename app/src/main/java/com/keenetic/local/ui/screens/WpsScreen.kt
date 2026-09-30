package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun WpsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val wpsStatus by viewModel.wpsStatus.collectAsState()
    val wlanList by viewModel.mwsWlanList.collectAsState()
    val autoPinMode = remember(wlanList) {
        wlanList.any { wlan -> wlan.bands.any { b -> b.accessPointId.contains("AccessPoint0") && b.wpsAutoSelfPin } }
    }

    LaunchedEffect(Unit) { viewModel.loadWpsStatus() }

    SectionScaffold(
        title = "WPS",
        subtitle = "Подключение клиента по кнопке и PIN",
        onBack = onBack,
        onRefresh = { viewModel.loadWpsStatus() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "wps") {
                SectionCard(title = "Настройки WPS", icon = Icons.Default.Wifi) {
                    SwitchRow(
                        label = "WPS включён",
                        description = "Wi-Fi Protected Setup",
                        checked = wpsStatus.enabled,
                        onCheckedChange = { viewModel.setWpsEnabled(it) }
                    )
                    RowDivider()
                    SwitchRow(
                        label = "Авто-PIN (auto-self-pin)",
                        description = "Роутер сам сообщает PIN клиенту",
                        checked = autoPinMode,
                        onCheckedChange = { viewModel.setWpsAutoSelfPin(it) }
                    )
                    RowDivider()
                    InfoRow(label = "PIN роутера", value = wpsStatus.pin, monospace = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.startWpsButton() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Подключить клиента (кнопка WPS)")
                    }
                    Text(
                        "Команда wps.button direction=receive запускает сессию WPS PBC на основной точке доступа (проверено на KN-2311).",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            if (wlanList.isNotEmpty()) {
                item(key = "ap-title") {
                    SubGroupHeader("Точки доступа Wi-Fi")
                }
                items(wlanList, key = { it.id }) { wlan ->
                    SectionCard(icon = Icons.Default.Wifi) {
                        InfoRow(label = "ID", value = wlan.id, monospace = true)
                        wlan.bands.forEachIndexed { index, band ->
                            RowDivider()
                            InfoRow(
                                label = wpsBandLabel(band),
                                value = wpsBandStatus(band),
                                valueColor = wpsBandStatusColor(band)
                            )
                            if (band.accessPointId.isNotBlank()) {
                                Text(
                                    band.accessPointId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun wpsBandLabel(band: MwsWlanBand): String = when (band.band) {
    "0" -> "2.4 ГГц"
    "1" -> "5 ГГц"
    else -> "Полоса ${band.band}"
}

private fun wpsBandStatus(band: MwsWlanBand): String = when {
    !band.wpsConfigured -> "WPS недоступен"
    band.wpsStatus.equals("enabled", ignoreCase = true) -> "WPS готов"
    else -> "WPS: ${band.wpsStatus}"
}

private fun wpsBandStatusColor(band: MwsWlanBand) = when {
    !band.wpsConfigured -> KeeneticColors.TextSecondary
    band.wpsStatus.equals("enabled", ignoreCase = true) -> KeeneticColors.Primary
    else -> KeeneticColors.Warning
}
