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
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val SCAN_OPTIONS = listOf(
    "dynamically" to "Постоянно",
    "every-6h" to "Раз в 6 ч",
    "every-12h" to "Раз в 12 ч",
    "every-24h" to "Раз в сутки",
    "on-device-start" to "При старте"
)

private val BAND_PREFS = listOf("no-priority" to "Без приоритета", "2" to "2.4 ГГц", "5" to "5 ГГц")

@Composable
fun WifiSystemScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val actionMessage by viewModel.wifiActionMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadInterfaces() }

    val masters = remember(interfaces) { interfaces.filter { it.id.startsWith("WifiMaster") } }
    val aps = remember(interfaces) { interfaces.filter { it.id.contains("/AccessPoint") } }

    SectionScaffold(
        title = "Общие параметры Wi-Fi",
        subtitle = "Радиомодули и точки доступа",
        onBack = onBack,
        onRefresh = { viewModel.loadInterfaces() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!actionMessage.isNullOrBlank()) {
                item(key = "message") {
                    Text(
                        actionMessage!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = KeeneticColors.Primary
                    )
                }
            }

            item(key = "about") {
                Text(
                    "Поля соответствуют странице роутера «Общие параметры Wi-Fi». Радиомодули — это физические приёмопередатчики 2.4 и 5 ГГц, точки доступа вещают сеть на них.",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }

            if (masters.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        Text(
                            "Радиомодули не найдены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            items(masters, key = { it.id }) { master ->
                WifiMasterCard(viewModel, master)
            }

            if (aps.isNotEmpty()) {
                item(key = "ap-title") {
                    SubGroupHeader("Точки доступа", count = aps.size)
                }
                items(aps, key = { it.id }) { ap ->
                    WifiApCard(viewModel, ap)
                }
            }
        }
    }
}

@Composable
private fun WifiMasterCard(viewModel: RouterViewModel, master: RouterInterface) {
    val is24 = master.id.contains("WifiMaster0")
    val bandLabel = if (is24) "2.4 ГГц" else "5 ГГц"

    var channel by remember(master.id) { mutableStateOf(master.channel) }
    var width by remember(master.id) { mutableStateOf("keep") }
    var rescan by remember(master.id) { mutableStateOf(master.autoRescan) }
    var power by remember(master.id) { mutableStateOf(master.powerPercent) }
    var txBurst by remember(master.id) { mutableStateOf(master.txBurst) }
    var beamforming by remember(master.id) { mutableStateOf(master.beamforming) }
    var qam256 by remember(master.id) { mutableStateOf(master.qam256) }
    var dlOfdma by remember(master.id) { mutableStateOf(master.downlinkOfdma) }
    var ulOfdma by remember(master.id) { mutableStateOf(master.uplinkOfdma) }
    var dlMumimo by remember(master.id) { mutableStateOf(master.downlinkMumimo) }
    var ulMumimo by remember(master.id) { mutableStateOf(master.uplinkMumimo) }
    var twt by remember(master.id) { mutableStateOf(master.targetWaketime) }
    var fairness by remember(master.id) { mutableStateOf(!master.atfDisabled) }
    var atfInbound by remember(master.id) { mutableStateOf(master.atfInbound) }
    var bandSteering by remember(master.id) { mutableStateOf(master.bandSteeringEnabled) }
    var preferBand by remember(master.id) { mutableStateOf(master.preferBand) }

    var picker by remember(master.id) { mutableStateOf<String?>(null) }

    val channelOptions = if (is24) listOf("Авто" to 0, "1" to 1, "6" to 6, "11" to 11) else listOf("Авто" to 0, "36" to 36, "40" to 40, "44" to 44, "48" to 48, "149" to 149, "153" to 153)
    val widthOptions = if (is24) listOf("20" to 20, "40" to 40) else listOf("20" to 20, "40" to 40, "80" to 80)
    val rescanEffective = if (rescan == "1") "dynamically" else if (rescan.matches(Regex("^\\d+$"))) "every-${rescan}h" else if (rescan.startsWith("every-")) rescan else if (rescan == "dynamically") "dynamically" else "on-device-start"

    SectionCard(
        title = bandLabel,
        subtitle = master.id,
        icon = Icons.Default.Router,
        trailing = {
            if (master.country.isNotBlank()) {
                Text(
                    master.country,
                    style = MaterialTheme.typography.labelMedium,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    ) {
        EditableRow(
            label = "Канал",
            value = if (channel == 0) "Авто" else channel.toString(),
            onClick = { picker = "channel" }
        )
        RowDivider()
        EditableRow(
            label = "Ширина канала",
            value = if (width == "keep") "Без изменений" else "$width МГц",
            onClick = { picker = "width" }
        )
        RowDivider()
        EditableRow(
            label = "Мощность",
            value = "$power%",
            onClick = { picker = "power" }
        )
        RowDivider()
        EditableRow(
            label = "Автопоиск каналов",
            value = SCAN_OPTIONS.firstOrNull { it.first == rescanEffective }?.second ?: rescanEffective,
            onClick = { picker = "rescan" }
        )
        RowDivider()
        SwitchRow(
            label = "TX Burst",
            description = "Пакетная передача",
            checked = txBurst,
            onCheckedChange = { txBurst = it }
        )
        RowDivider()
        SwitchRow(
            label = "Лучевое формирование",
            description = "Beamforming",
            checked = beamforming,
            onCheckedChange = { beamforming = it }
        )
        if (is24) {
            RowDivider()
            SwitchRow(
                label = "256-QAM",
                checked = qam256,
                onCheckedChange = { qam256 = it }
            )
        }
        RowDivider()
        SwitchRow(
            label = "Airtime Fairness",
            description = "Справедливый доступ к эфиру",
            checked = fairness,
            onCheckedChange = { fairness = it }
        )
        RowDivider()
        SwitchRow(
            label = "Airtime: входящий multicast",
            checked = atfInbound,
            onCheckedChange = { atfInbound = it }
        )
        RowDivider()
        SwitchRow(
            label = "DL OFDMA",
            checked = dlOfdma,
            onCheckedChange = { dlOfdma = it }
        )
        RowDivider()
        SwitchRow(
            label = "UL OFDMA",
            checked = ulOfdma,
            onCheckedChange = { ulOfdma = it }
        )
        RowDivider()
        SwitchRow(
            label = "DL MU-MIMO",
            checked = dlMumimo,
            onCheckedChange = { dlMumimo = it }
        )
        RowDivider()
        SwitchRow(
            label = "UL MU-MIMO",
            checked = ulMumimo,
            onCheckedChange = { ulMumimo = it }
        )
        RowDivider()
        SwitchRow(
            label = "Target Wake Time (TWT)",
            checked = twt,
            onCheckedChange = { twt = it }
        )
        RowDivider()
        SwitchRow(
            label = "Band Steering",
            description = "Направление в оптимальный диапазон",
            checked = bandSteering,
            onCheckedChange = { bandSteering = it }
        )
        if (bandSteering) {
            RowDivider()
            EditableRow(
                label = "Приоритетный диапазон",
                value = BAND_PREFS.firstOrNull { it.first == preferBand }?.second ?: preferBand,
                onClick = { picker = "preferBand" }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                val patches = mutableListOf<Map<String, Any>>()
                patches.add(mapOf("channel" to channel))
                if (width != "keep") patches.add(mapOf("channel" to mapOf("width" to width.toInt())))
                if (rescanEffective != "on-device-start") {
                    val iv = if (rescanEffective == "dynamically") "1" else rescanEffective.replace("every-", "").replace("h", "")
                    patches.add(mapOf("channel" to mapOf("auto-rescan" to mapOf("interval" to iv))))
                }
                patches.add(mapOf("power" to power))
                patches.add(mapOf("tx-burst" to txBurst))
                patches.add(mapOf("beamforming" to mapOf("explicit" to beamforming)))
                if (is24) patches.add(mapOf("vht" to qam256))
                patches.add(mapOf("atf" to mapOf("disable" to !fairness, "inbound" to atfInbound)))
                patches.add(mapOf("downlink-ofdma" to dlOfdma))
                patches.add(mapOf("uplink-ofdma" to ulOfdma))
                patches.add(mapOf("downlink-mumimo" to dlMumimo))
                patches.add(mapOf("uplink-mumimo" to ulMumimo))
                patches.add(mapOf("target-waketime" to twt))
                patches.add(
                    mapOf(
                        "band-steering" to (if (bandSteering) mapOf("enable" to true, "prefer-band" to preferBand) else mapOf("enable" to false))
                    )
                )
                viewModel.updateWifiInterface(master.id, *patches.toTypedArray())
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить $bandLabel")
        }
    }

    when (picker) {
        "channel" -> OptionPickerDialog(
            title = "Канал $bandLabel",
            options = channelOptions.map { (lbl, v) -> v.toString() to lbl },
            selectedKey = channel.toString(),
            onSelect = { channel = it.toInt() },
            onDismiss = { picker = null }
        )
        "width" -> OptionPickerDialog(
            title = "Ширина канала $bandLabel",
            options = listOf("keep" to "Без изменений") + widthOptions.map { (lbl, v) -> v.toString() to "$lbl МГц" },
            selectedKey = width,
            onSelect = { width = it },
            onDismiss = { picker = null }
        )
        "power" -> OptionPickerDialog(
            title = "Мощность $bandLabel",
            options = listOf(100, 75, 50, 25).map { it.toString() to "$it%" },
            selectedKey = power.toString(),
            onSelect = { power = it.toInt() },
            onDismiss = { picker = null }
        )
        "rescan" -> OptionPickerDialog(
            title = "Автопоиск каналов",
            options = SCAN_OPTIONS,
            selectedKey = rescanEffective,
            onSelect = { rescan = it },
            onDismiss = { picker = null }
        )
        "preferBand" -> OptionPickerDialog(
            title = "Приоритетный диапазон",
            options = BAND_PREFS,
            selectedKey = preferBand,
            onSelect = { preferBand = it },
            onDismiss = { picker = null }
        )
    }
}

@Composable
private fun WifiApCard(viewModel: RouterViewModel, ap: RouterInterface) {
    var hidden by remember(ap.id) { mutableStateOf(ap.ssidHidden) }
    var wps by remember(ap.id) { mutableStateOf(ap.wpsEnabled) }
    var ft by remember(ap.id) { mutableStateOf(ap.ftEnabled) }
    var mdid by remember(ap.id) { mutableStateOf(ap.mdid) }
    var iappKey by remember(ap.id) { mutableStateOf(ap.iappKey) }
    var rrm by remember(ap.id) { mutableStateOf(ap.rrmEnabled) }
    var isolation by remember(ap.id) { mutableStateOf(ap.peerIsolation) }

    var showFastTransition by remember(ap.id) { mutableStateOf<String?>(null) }

    SectionCard(
        title = ap.name,
        subtitle = ap.id.substringAfterLast("/"),
        icon = Icons.Default.Wifi
    ) {
        SwitchRow(
            label = "Скрыть SSID",
            description = "Сеть не отображается в поиске",
            checked = hidden,
            onCheckedChange = { hidden = it }
        )
        RowDivider()
        SwitchRow(
            label = "WPS",
            description = "Быстрое подключение по PIN",
            checked = wps,
            onCheckedChange = { wps = it }
        )
        RowDivider()
        SwitchRow(
            label = "802.11r Fast Roaming",
            description = "Быстрый роуминг",
            checked = ft,
            onCheckedChange = { ft = it }
        )
        RowDivider()
        SwitchRow(
            label = "802.11v RRM",
            description = "Управление радиоресурсами",
            checked = rrm,
            onCheckedChange = { rrm = it }
        )
        RowDivider()
        SwitchRow(
            label = "Клиентская изоляция",
            description = "Клиенты не видят друг друга",
            checked = isolation,
            onCheckedChange = { isolation = it }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                val patches = mutableListOf<Map<String, Any>>()
                patches.add(mapOf("ssid" to mapOf("hide" to hidden)))
                patches.add(mapOf("wps" to mapOf("enable" to wps)))
                patches.add(
                    mapOf(
                        "ft" to (if (ft) mapOf("enable" to true, "mdid" to mdid, "iapp" to mapOf("key" to iappKey)) else mapOf("enable" to false))
                    )
                )
                patches.add(mapOf("rrm" to mapOf("enable" to rrm)))
                patches.add(mapOf("peer-isolation" to isolation))
                viewModel.updateWifiInterface(ap.id, *patches.toTypedArray())
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить точку доступа")
        }
    }

    if (ft) {
        RowDivider()
        EditableRow(
            label = "MDID (домен мобильности)",
            value = mdid.ifBlank { "не задан" },
            onClick = { showFastTransition = "mdid" }
        )
        RowDivider()
        EditableRow(
            label = "Ключ IAPP",
            value = if (iappKey.isBlank()) "не задан" else "••••••",
            onClick = { showFastTransition = "iapp" }
        )
    }

    when (showFastTransition) {
        "mdid" -> FormDialog(
            title = "MDID (домен мобильности)",
            onConfirm = { showFastTransition = null },
            onDismiss = { showFastTransition = null }
        ) {
            OutlinedTextField(
                value = mdid,
                onValueChange = { mdid = it },
                label = { Text("MDID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            InfoRow(label = "Текущее значение", value = mdid.ifBlank { "—" }, monospace = true)
        }
        "iapp" -> FormDialog(
            title = "Ключ IAPP",
            onConfirm = { showFastTransition = null },
            onDismiss = { showFastTransition = null }
        ) {
            OutlinedTextField(
                value = iappKey,
                onValueChange = { iappKey = it },
                label = { Text("Ключ IAPP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
