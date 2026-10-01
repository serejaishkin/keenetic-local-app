package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.keenetic.local.ui.Screen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.DialogForm
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun InternetScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()

    fun refresh() = viewModel.loadInterfaces()

    LaunchedEffect(Unit) { refresh() }

    val wired = interfaces.filter { it.type.equals("Ethernet", ignoreCase = true) }
    val modem = interfaces.filter { it.type.contains("Modem", ignoreCase = true) }
    val wisp = interfaces.filter { it.type.contains("Wisp", ignoreCase = true) }
    val other = interfaces.filter { iface ->
        iface !in wired && iface !in modem && iface !in wisp
    }

    SectionScaffold(
        title = "Интернет",
        subtitle = "подключения и интерфейсы",
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
            if (interfaces.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint("Сетевые интерфейсы опрашиваются…")
                    }
                }
            }

            if (wired.isNotEmpty()) {
                item(key = "wired-h") { SubGroupHeader("Проводные подключения", wired.size) }
                items(wired, key = { "w-${it.id}" }) { iface -> ConnectionCard(viewModel, iface, onNavigate) }
            }
            if (modem.isNotEmpty()) {
                item(key = "modem-h") { SubGroupHeader("Сотовая сеть", modem.size) }
                items(modem, key = { "m-${it.id}" }) { iface -> ConnectionCard(viewModel, iface, onNavigate) }
            }
            if (wisp.isNotEmpty()) {
                item(key = "wisp-h") { SubGroupHeader("Публичные сети Wi-Fi", wisp.size) }
                items(wisp, key = { "wi-${it.id}" }) { iface -> ConnectionCard(viewModel, iface, onNavigate) }
            }
            if (other.isNotEmpty()) {
                item(key = "other-h") { SubGroupHeader("Прочие интерфейсы", other.size) }
                items(other, key = { "o-${it.id}" }) { iface -> ConnectionCard(viewModel, iface, onNavigate) }
            }
        }
    }
}

/** Карточка подключения = строка таблицы сайта: имя, тип, адрес, действия. */
@Composable
private fun ConnectionCard(viewModel: RouterViewModel, iface: RouterInterface, onNavigate: (String) -> Unit = {}) {
    var showIpDialog by remember(iface.id) { mutableStateOf(false) }
    var showMainDialog by remember(iface.id) { mutableStateOf(false) }
    var showPppoeDialog by remember(iface.id) { mutableStateOf(false) }
    val isWired = iface.type.equals("Ethernet", ignoreCase = true)

    SectionCard(title = iface.name, icon = Icons.Default.Language) {
        InfoRow(
            "Состояние",
            if (iface.isUp) "Подключено" else "Отключено",
            valueColor = if (iface.isUp) KeeneticColors.Success else KeeneticColors.Error
        )
        if (iface.description.isNotBlank()) {
            Text(
                iface.description,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
        InfoRow("IP-адрес", iface.ip ?: "нет данных", monospace = true)
        InfoRow("Маска подсети", iface.mask ?: "—", monospace = true)
        if (iface.mtu.isNotBlank()) InfoRow("MTU", iface.mtu)

        RowDivider()

        SwitchRow(
            label = "Интерфейс включён",
            checked = iface.isUp,
            onCheckedChange = { up -> viewModel.setInterfaceUp(iface.id, up) }
        )
        EditableRow(
            label = "Настройка IP-адреса",
            value = if (iface.ip.isNullOrBlank()) "автоматически" else "статически",
            onClick = { showIpDialog = true }
        )
        EditableRow(
            label = "Параметры подключения",
            value = "описание, MTU, MAC",
            onClick = { showMainDialog = true }
        )
        EditableRow(
            label = "Настройки подключения",
            value = "как в веб-интерфейсе",
            onClick = {
                viewModel.openWanConnection(iface.id)
                onNavigate(Screen.WanConnection.route)
            }
        )
        if (isWired) {
            EditableRow(label = "PPPoE", value = "логин и пароль", onClick = { showPppoeDialog = true })
        }
    }

    if (showIpDialog) {
        IpSettingsDialog(
            iface = iface,
            onDismiss = { showIpDialog = false },
            onSave = { useDhcp, ip, mask, gateway ->
                viewModel.updateInterfaceIpConfig(iface.id, useDhcp, ip, mask, gateway)
                showIpDialog = false
            }
        )
    }
    if (showMainDialog) {
        MainSettingsDialog(
            iface = iface,
            onDismiss = { showMainDialog = false },
            onSave = { description, mtu, hostname, order, macMode, macAddress ->
                viewModel.updateWiredConnectionSettings(
                    id = iface.id,
                    description = description,
                    mtu = mtu,
                    hostname = hostname,
                    order = order,
                    macMode = macMode,
                    macAddress = macAddress.ifBlank { null }
                )
                showMainDialog = false
            }
        )
    }
    if (showPppoeDialog) {
        PppoeSettingsDialog(
            iface = iface,
            onDismiss = { showPppoeDialog = false },
            onSave = { identity, password, service, auth ->
                viewModel.updateWiredConnectionSettings(
                    id = iface.id,
                    pppoeIdentity = identity,
                    pppoePassword = password,
                    pppoeService = service,
                    pppoeAuth = auth
                )
                showPppoeDialog = false
            }
        )
    }
}

@Composable
private fun IpSettingsDialog(
    iface: RouterInterface,
    onDismiss: () -> Unit,
    onSave: (useDhcp: Boolean, ip: String, mask: String, gateway: String) -> Unit
) {
    var useDhcp by remember(iface.id) { mutableStateOf(iface.ip.isNullOrBlank()) }
    var ip by remember(iface.id) { mutableStateOf(iface.ip.orEmpty()) }
    var mask by remember(iface.id) { mutableStateOf(iface.mask ?: "255.255.255.0") }
    var gateway by remember(iface.id) { mutableStateOf("") }

    FormDialog(
        title = "Настройка IP — ${iface.name}",
        confirmLabel = "Сохранить",
        confirmEnabled = useDhcp || ip.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(useDhcp, ip, mask, gateway) }
    ) {
        SwitchRow(
            label = "Получать автоматически (DHCP)",
            checked = useDhcp,
            onCheckedChange = { useDhcp = it }
        )
        if (!useDhcp) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP-адрес") },
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
                value = gateway,
                onValueChange = { gateway = it },
                label = { Text("Шлюз") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MainSettingsDialog(
    iface: RouterInterface,
    onDismiss: () -> Unit,
    onSave: (description: String, mtu: String, hostname: String, order: String, macMode: String, macAddress: String) -> Unit
) {
    var description by remember(iface.id) { mutableStateOf(iface.description) }
    var mtu by remember(iface.id) { mutableStateOf(iface.mtu) }
    var hostname by remember(iface.id) { mutableStateOf(iface.hostname) }
    var order by remember(iface.id) { mutableStateOf(iface.order.toString()) }
    var macMode by remember(iface.id) { mutableStateOf(iface.macConfigMode.ifBlank { "DEFAULT" }) }
    var macAddress by remember(iface.id) { mutableStateOf("") }

    FormDialog(
        title = "Параметры — ${iface.name}",
        confirmLabel = "Сохранить",
        onDismiss = onDismiss,
        onConfirm = { onSave(description, mtu, hostname, order, macMode, macAddress) }
    ) {
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = mtu,
            onValueChange = { mtu = it },
            label = { Text("MTU") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hostname,
            onValueChange = { hostname = it },
            label = { Text("Имя узла (hostname)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = order,
            onValueChange = { order = it.filter(Char::isDigit) },
            label = { Text("Приоритет подключения") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = macMode,
            onValueChange = { macMode = it },
            label = { Text("Режим MAC: DEFAULT / STATIC / RANDOM") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (macMode.equals("STATIC", ignoreCase = true)) {
            OutlinedTextField(
                value = macAddress,
                onValueChange = { macAddress = it },
                label = { Text("MAC-адрес") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PppoeSettingsDialog(
    iface: RouterInterface,
    onDismiss: () -> Unit,
    onSave: (identity: String, password: String, service: String, auth: String) -> Unit
) {
    var identity by remember(iface.id) { mutableStateOf("") }
    var password by remember(iface.id) { mutableStateOf("") }
    var service by remember(iface.id) { mutableStateOf("") }
    var auth by remember(iface.id) { mutableStateOf("auto") }

    FormDialog(
        title = "PPPoE — ${iface.name}",
        confirmLabel = "Сохранить",
        confirmEnabled = identity.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(identity, password, service, auth) }
    ) {
        OutlinedTextField(
            value = identity,
            onValueChange = { identity = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = service,
            onValueChange = { service = it },
            label = { Text("Сервис (необязательно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = auth,
            onValueChange = { auth = it },
            label = { Text("Метод авторизации: auto / chap / pap") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
