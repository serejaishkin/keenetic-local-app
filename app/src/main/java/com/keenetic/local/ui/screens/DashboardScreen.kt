package com.keenetic.local.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WifiAssoc
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun DashboardScreen(viewModel: RouterViewModel) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val versionInfo by viewModel.versionInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    val interfaceStats by viewModel.interfaceStats.collectAsState()
    val associations by viewModel.associations.collectAsState()
    val wans by viewModel.wans.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
        viewModel.loadVersionInfo()
    }

    // Периодический опрос раз в 5 сек, чтобы статус (в т.ч. VPN/Proxy,
    // переключённые через веб-морду или другое устройство) обновлялся сам,
    // без ручного нажатия кнопки обновления.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            viewModel.loadInterfaces()
            viewModel.loadWans()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Статус роутера",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            StatusCard(systemInfo, versionInfo)
        }

        item {
            QuickActionsCard(viewModel)
        }

        item {
            // Официальный источник - `show wans` (SSH), подтверждено реальным
            // выводом с роутера: {"wan":{"id":"GigabitEthernet0/Vlan4",...}}.
            // Раньше искали WAN эвристикой (любой интерфейс с непустым
            // address) - оставлена как запасной вариант, если show wans
            // недоступен (SSH не подключён и т.п.).
            val wanIds = buildSet {
                wans?.wan?.id?.let { add(it) }
                wans?.wbk?.forEach { it.id?.let { id -> add(id) } }
            }
            val wanCandidates = if (wanIds.isNotEmpty()) {
                interfaces.filter { it.id in wanIds }
            } else {
                interfaces.filter {
                    !it.address.isNullOrBlank() &&
                        it.type !in setOf("AccessPoint", "Bridge", "Port", "WifiStation", "Loopback")
                }
            }
            wanCandidates.forEach { wan ->
                WanStatusCard(wan, viewModel, interfaceStats[wan.id])
            }
        }

        item {
            NetworksAndWifiCard(viewModel)
        }

        item {
            // Раньше матчили только по полю "type" - но после выключения
            // интерфейса через веб-морду это поле иногда пропадает из
            // ответа роутера, и карточка целиком исчезала. Добавили запасное
            // сравнение по префиксу id ("Proxy0", "Wireguard4" и т.п.).
            val tunnels = interfaces.filter {
                it.type in setOf("Proxy", "Wireguard") ||
                    it.id.startsWith("Proxy") || it.id.startsWith("Wireguard")
            }
            if (tunnels.isNotEmpty()) {
                VpnStatusCard(tunnels, interfaceStats, viewModel)
            }
        }

        item {
            if (associations.isNotEmpty()) {
                TrafficChartCard(associations)
            }
        }

        item {
            Text(
                text = "Интерфейсы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(interfaces.size) { index ->
            val iface = interfaces[index]
            InterfaceCard(iface)
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
    }
}

@Composable
fun StatusCard(info: com.keenetic.local.api.SystemInfo?, version: com.keenetic.local.api.VersionInfo? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("О системе", fontWeight = FontWeight.Medium, color = KeeneticColors.Accent)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Поля и порядок - по образцу реальной карточки "О системе"
            // (card_system) из веб-дашборда, см. assets/language/locale.ru.json
            // из htdocs. Модель/версия ОС - ПОДТВЕРЖДЕНО HAR 07.08, отдельный
            // эндпоинт rci/show/version (раньше "Версия ОС" искалась в
            // show/system, где её нет вообще - потому и была всегда пустой).
            InfoRow("Модель", version?.model ?: version?.hwId ?: "—")
            InfoRow("Версия ОС", version?.title ?: "—")
            InfoRow("ЦП", "${info?.cpuload ?: "—"}%")
            InfoRow("ОЗУ", info?.memory ?: "—")
            InfoRow("Время работы", formatUptime(info?.uptime))
            if (info?.conntotal != null && info.connfree != null) {
                InfoRow("Активные соединения", "${info.conntotal - info.connfree}")
            }
            InfoRow("Имя", info?.hostname ?: "—")
        }
    }
}

@Composable
fun WanStatusCard(wan: com.keenetic.local.api.InterfaceInfo, viewModel: RouterViewModel, stat: com.keenetic.local.api.InterfaceStat? = null) {
    var confirmToggle by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (wan.up) KeeneticColors.Accent else KeeneticColors.Error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Интернет (${wan.displayName})",
                        fontWeight = FontWeight.Medium,
                        color = if (wan.up) KeeneticColors.Accent else KeeneticColors.Error
                    )
                }
                Switch(
                    checked = wan.up,
                    onCheckedChange = { confirmToggle = true },
                    colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Accent)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val ipObtained = !wan.address.isNullOrBlank()
            InfoRow("Статус", if (wan.up) "Подключено" else "Нет соединения")
            if (stat != null) {
                InfoRow("Скорость", "${formatSpeed(stat.rxspeed)} ↓ · ${formatSpeed(stat.txspeed)} ↑")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ipObtained) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (ipObtained) KeeneticColors.Accent else KeeneticColors.Error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (ipObtained) "IP получен, всё в порядке" else "IP не получен - проверь подключение",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ipObtained) KeeneticColors.Accent else KeeneticColors.Error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            // Группировка полей 2x2, как в карточке "Интернет" веб-дашборда:
            // IP-адрес/MAC-адрес в одной строке, Маска/Принято-Отправлено в
            // следующих. "Шлюз" в веб-версии не показываю - подтверждённого
            // поля gateway нет ни в show/interface, ни в show wans, а
            // угадывать его не буду.
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("IP-адрес", wan.address ?: "—")
                }
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("MAC-адрес", wan.mac ?: "—")
                }
            }
            if (!wan.mask.isNullOrBlank()) {
                InfoRow("Маска подсети", wan.mask)
            }
            if (stat?.rxbytes != null || stat?.txbytes != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoRow("Принято", formatBytesTotal(stat?.rxbytes))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        InfoRow("Отправлено", formatBytesTotal(stat?.txbytes))
                    }
                }
            }
        }
    }

    if (confirmToggle) {
        AlertDialog(
            onDismissRequest = { confirmToggle = false },
            title = { Text("Подтвердите действие") },
            text = {
                Text(
                    "${if (wan.up) "Выключить" else "Включить"} подключение «${wan.displayName}»?" +
                        if (wan.up) " Если это единственный аплинк, интернет пропадёт до повторного включения." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleInterface(wan.id, !wan.up)
                    confirmToggle = false
                }) {
                    Text("Да", color = KeeneticColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmToggle = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun TrafficChartCard(associations: List<WifiAssoc>) {
    // Столбчатый график по топ-устройствам за текущий счётчик трафика с
    // момента подключения (это не "скорость сейчас", а накопленные байты -
    // честной живой линии скорости через простой REST-поллинг без хранения
    // истории не построить корректно, поэтому делаем то, что данные реально
    // позволяют).
    val top = associations
        .filter { (it.txbytes ?: 0) + (it.rxbytes ?: 0) > 0 }
        .sortedByDescending { (it.txbytes ?: 0) + (it.rxbytes ?: 0) }
        .take(6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Активные подключения (вх/исх)", fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Box(modifier = Modifier.size(10.dp).background(KeeneticColors.Accent))
                Spacer(modifier = Modifier.width(4.dp))
                Text("входящий", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(10.dp).background(KeeneticColors.Primary))
                Spacer(modifier = Modifier.width(4.dp))
                Text("исходящий", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (top.isEmpty()) {
                Text(
                    "Нет данных о трафике активных подключений",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            } else {
                val maxBytes = top.maxOf { (it.txbytes ?: 0) + (it.rxbytes ?: 0) }.coerceAtLeast(1)
                top.forEach { assoc ->
                    val rx = assoc.rxbytes ?: 0
                    val tx = assoc.txbytes ?: 0
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            assoc.hostname ?: assoc.mac ?: "—",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            val total = (rx + tx).toFloat()
                            if (total <= 0f) return@Canvas
                            val widthTotal = size.width * (total / maxBytes.toFloat())
                            val rxWidth = widthTotal * (rx / total)
                            val txWidth = widthTotal - rxWidth
                            drawRect(
                                color = KeeneticColorsAccentCompose,
                                topLeft = Offset(0f, 0f),
                                size = Size(rxWidth, size.height),
                                style = Fill
                            )
                            drawRect(
                                color = KeeneticColorsPrimaryCompose,
                                topLeft = Offset(rxWidth, 0f),
                                size = Size(txWidth, size.height),
                                style = Fill
                            )
                        }
                        Text(
                            "${formatBytes(rx)} ↓ · ${formatBytes(tx)} ↑",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private val KeeneticColorsAccentCompose = KeeneticColors.Accent
private val KeeneticColorsPrimaryCompose = KeeneticColors.Primary

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f ГБ".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f МБ".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.1f КБ".format(bytes / 1024.0)
    else -> "$bytes Б"
}

private fun formatSpeed(bytesPerSec: Long?): String {
    val v = bytesPerSec ?: return "—"
    return when {
        v >= 1024 * 1024 -> "%.1f МБ/с".format(v / (1024.0 * 1024))
        v >= 1024 -> "%.1f КБ/с".format(v / 1024.0)
        else -> "$v Б/с"
    }
}

/** Суммарный трафик (Принято/Отправлено) - как на веб-дашборде, "37,7 ГБ". */
private fun formatBytesTotal(bytes: Long?): String {
    val v = bytes ?: return "—"
    return when {
        v >= 1024L * 1024 * 1024 -> "%.1f ГБ".format(v / (1024.0 * 1024 * 1024))
        v >= 1024 * 1024 -> "%.1f МБ".format(v / (1024.0 * 1024))
        v >= 1024 -> "%.1f КБ".format(v / 1024.0)
        else -> "$v Б"
    }
}

@Composable
fun VpnStatusCard(tunnels: List<com.keenetic.local.api.InterfaceInfo>, stats: Map<String, com.keenetic.local.api.InterfaceStat> = emptyMap(), viewModel: RouterViewModel) {
    var toggleTarget by remember { mutableStateOf<com.keenetic.local.api.InterfaceInfo?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnLock, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("VPN / Прокси", fontWeight = FontWeight.Medium, color = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            tunnels.forEach { t ->
                val stat = stats[t.id]
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t.displayName, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (t.up) "Активен" else "Выключен",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (t.up) KeeneticColors.Accent else KeeneticColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = t.up,
                                onCheckedChange = { toggleTarget = t },
                                colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Accent),
                                modifier = Modifier.size(width = 40.dp, height = 24.dp)
                            )
                        }
                    }
                    if (stat != null && t.up) {
                        Text(
                            "${formatSpeed(stat.rxspeed)} ↓ · ${formatSpeed(stat.txspeed)} ↑",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    toggleTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { toggleTarget = null },
            title = { Text("Подтвердите действие") },
            text = { Text("${if (t.up) "Выключить" else "Включить"} «${t.displayName}»?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleInterface(t.id, !t.up)
                    toggleTarget = null
                }) {
                    Text("Да", color = KeeneticColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { toggleTarget = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

/**
 * Роутер отдаёт uptime сырыми секундами ("47222"), на скрине выглядело
 * нечитаемо на телефоне. Переводим в "Nд Nч Nм" - если формат вдруг не
 * число (другая прошивка/уже человекочитаемая строка) - показываем как есть.
 */
private fun formatUptime(raw: String?): String {
    val seconds = raw?.toLongOrNull() ?: return raw ?: "—"
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return buildString {
        if (days > 0) append("${days}д ")
        if (days > 0 || hours > 0) append("${hours}ч ")
        append("${minutes}м")
    }.trim()
}

@Composable
fun QuickActionsCard(viewModel: RouterViewModel) {
    var confirmReboot by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Быстрые действия", fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Раньше тут была иконка Refresh - визуально неотличима от
                // кнопки обновления данных, легко перепутать и случайно
                // перезагрузить роутер. Теперь отдельная иконка + подтверждение.
                ActionButton(Icons.Default.PowerSettingsNew, "Перезагрузить") { confirmReboot = true }
                ActionButton(Icons.Default.Wifi, "Wi-Fi") { /* navigate */ }
                ActionButton(Icons.Default.Devices, "Устройства") { /* navigate */ }
                ActionButton(Icons.Default.Terminal, "Терминал") { /* navigate */ }
            }
        }
    }

    if (confirmReboot) {
        AlertDialog(
            onDismissRequest = { confirmReboot = false },
            icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = KeeneticColors.Error) },
            title = { Text("Перезагрузить роутер?") },
            text = { Text("Интернет и Wi-Fi пропадут на 1-2 минуты, пока роутер перезагружается.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reboot()
                    confirmReboot = false
                }) {
                    Text("Перезагрузить", color = KeeneticColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReboot = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
    }
}

@Composable
fun InterfaceCard(iface: com.keenetic.local.api.InterfaceInfo) {
    val isUp = iface.up == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUp) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isUp) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = iface.displayName.ifBlank { iface.id }, fontWeight = FontWeight.Medium)
                Text(
                    text = "${iface.state ?: ""} ${iface.link ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            if (iface.connected != null) {
                Text(text = iface.connected, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.Accent)
            }
        }
    }
}

/**
 * "Мои сети и Wi-Fi" - структура подачи по образцу веб-дашборда: Wi-Fi сети
 * сгруппированы по Домашняя/Гостевая (WifiNetwork.guest - надёжное поле),
 * плюс общая сводка проводных/беспроводных устройств.
 *
 * Разбивку по количеству устройств именно НА КАЖДЫЙ сегмент (как в вебе -
 * "Сегмент 3: Wi-Fi 0, Проводные 0") сознательно не делаю: точной привязки
 * устройства к конкретному сегменту в уже собранных данных нет (только
 * текстовое interfaceDescription/ssid, сопоставление было бы нечётким и
 * могло бы показать неверные цифры) - лучше честная общая сводка со
 * ссылкой на полный список сегментов, чем гадание.
 */
@Composable
private fun NetworksAndWifiCard(viewModel: RouterViewModel) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val deviceList by viewModel.deviceList.collectAsState()

    val homeNetworks = wifiNetworks.filter { !it.guest }
    val guestNetworks = wifiNetworks.filter { it.guest }
    val wiredCount = deviceList.count { it.active && it.wired }
    val wifiCount = deviceList.count { it.active && !it.wired }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Мои сети и Wi-Fi", fontWeight = FontWeight.Medium, color = KeeneticColors.Primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Wi-Fi: $wifiCount активных · Проводные: $wiredCount активных",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (homeNetworks.isNotEmpty()) {
                NetworkGroupRow("Основная", homeNetworks)
            }
            if (guestNetworks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NetworkGroupRow("Гости", guestNetworks)
            }
        }
    }
}

@Composable
private fun NetworkGroupRow(title: String, networks: List<com.keenetic.local.api.WifiNetwork>) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${networks.count { it.enabled }}/${networks.size} вкл",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = KeeneticColors.TextSecondary
                )
            }
        }
        if (expanded) {
            networks.forEach { net ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(net.ssid, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${net.band} · ${net.security}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                    Icon(
                        if (net.enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (net.enabled) KeeneticColors.Accent else KeeneticColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
