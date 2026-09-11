package com.keenetic.local.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Tune, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Общие параметры Wi-Fi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "wifiSystem • Радиомодули и точки доступа",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadInterfaces() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        actionMessage?.let { msg ->
            Snackbar(
                action = {
                    TextButton(onClick = {}) {
                        Text("OK", color = KeeneticColors.Primary)
                    }
                }
            ) { Text(msg) }
        }

        Text(
            "Поля соответствуют странице роутера «Общие параметры Wi-Fi». Радиомодули — это физические приёмопередатчики 2.4 и 5 ГГц, точки доступа вещают сеть на них.",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )

        Text("Радиомодули", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
        masters.forEach { master ->
            WifiMasterCard(viewModel, master)
        }

        if (aps.isNotEmpty()) {
            Text("Точки доступа", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            aps.forEach { ap ->
                WifiApCard(viewModel, ap)
            }
        }
    }
}

@Composable
private fun WifiMasterCard(viewModel: RouterViewModel, master: RouterInterface) {
    val is24 = master.id.contains("WifiMaster0")
    val bandLabel = if (is24) "2.4 ГГц" else "5 ГГц"
    val accent = if (is24) Color(0xFFF59E0B) else Color(0xFFFF6B6B)

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

    val channelOptions = if (is24) listOf("Авто" to 0, "1" to 1, "6" to 6, "11" to 11) else listOf("Авто" to 0, "36" to 36, "40" to 40, "44" to 44, "48" to 48, "149" to 149, "153" to 153)
    val widthOptions = if (is24) listOf("20" to 20, "40" to 40) else listOf("20" to 20, "40" to 40, "80" to 80)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Router, contentDescription = null, tint = accent)
                Text(
                    "${master.id} • $bandLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                if (master.country.isNotBlank()) {
                    Spacer(Modifier.weight(1f))
                    Text("Страна: ${master.country}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                }
            }

            Text("Канал:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                channelOptions.forEach { (lbl, v) ->
                    FilterChip(
                        selected = channel == v,
                        onClick = { channel = v },
                        label = { Text(lbl) }
                    )
                }
            }

            Text("Ширина канала:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = width == "keep", onClick = { width = "keep" }, label = { Text("Без изменений") })
                widthOptions.forEach { (lbl, v) ->
                    FilterChip(
                        selected = width == v.toString(),
                        onClick = { width = v.toString() },
                        label = { Text("$lbl МГц") }
                    )
                }
            }

            Text("Мощность:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(100, 75, 50, 25).forEach { p ->
                    FilterChip(selected = power == p, onClick = { power = p }, label = { Text("$p%") })
                }
            }

            Text("Автопоиск каналов:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            val rescanEffective = if (rescan == "1") "dynamically" else if (rescan.matches(Regex("^\\d+$"))) "every-${rescan}h" else if (rescan.startsWith("every-")) rescan else if (rescan == "dynamically") "dynamically" else "on-device-start"
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SCAN_OPTIONS.forEach { (v, lbl) ->
                    FilterChip(selected = rescanEffective == v, onClick = { rescan = v }, label = { Text(lbl) })
                }
            }

            ToggleRow("TX Burst (пакетная передача)", txBurst) { txBurst = it }
            ToggleRow("Лучевое формирование (beamforming)", beamforming) { beamforming = it }
            if (is24) ToggleRow("256-QAM", qam256) { qam256 = it }
            ToggleRow("Airtime Fairness (справедливый доступ к эфиру)", fairness) { fairness = it }
            ToggleRow("Airtime: разрешить исходящий multicast", atfInbound) { atfInbound = it }
            ToggleRow("DL OFDMA", dlOfdma) { dlOfdma = it }
            ToggleRow("UL OFDMA", ulOfdma) { ulOfdma = it }
            ToggleRow("DL MU-MIMO", dlMumimo) { dlMumimo = it }
            ToggleRow("UL MU-MIMO", ulMumimo) { ulMumimo = it }
            ToggleRow("Target Wake Time (TWT)", twt) { twt = it }
            ToggleRow("Band Steering (направление в оптимальный диапазон)", bandSteering) { bandSteering = it }
            if (bandSteering) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Приоритетный диапазон:", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    BAND_PREFS.forEach { (v, lbl) ->
                        FilterChip(selected = preferBand == v, onClick = { preferBand = v }, label = { Text(lbl) })
                    }
                }
            }

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
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить $bandLabel")
            }
        }
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Text(
                    ap.id.split("/").last() + " • " + ap.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
            ToggleRow("Скрыть SSID (сеть не отображается в поиске)", hidden) { hidden = it }
            ToggleRow("WPS (быстрое подключение по PIN)", wps) { wps = it }
            ToggleRow("802.11r Fast Roaming (быстрый роуминг)", ft) { ft = it }
            if (ft) {
                OutlinedTextField(
                    value = mdid,
                    onValueChange = { mdid = it },
                    label = { Text("MDID (домен мобильности)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iappKey,
                    onValueChange = { iappKey = it },
                    label = { Text("Ключ IAPP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ToggleRow("802.11v RRM (управление радиоресурсами)", rrm) { rrm = it }
            ToggleRow("Клиентская изоляция (клиенты не видят друг друга)", isolation) { isolation = it }

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
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить точку доступа")
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}