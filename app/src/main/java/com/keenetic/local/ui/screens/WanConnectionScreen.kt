package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.WanConnection
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Полные настройки Ethernet-подключения — повторяет структуру веб-страницы
 * «Подключения к интернету по Ethernet-кабелю» (и приложения PACT):
 * расписание, приоритет, имя, удаление, порты/VLAN, IPv4, IPv6, требования
 * провайдера (MAC/MTU/hostname), авторизация PPPoE, Ping Check.
 *
 * Сохранение идёт через validate-then-save: команды сначала прогоняются БЕЗ
 * save (executeRciChecked), save отправляется только если роутер всё принял.
 * Перед отправкой — диалог со списком изменённых разделов.
 */
@Composable
fun WanConnectionScreen(
    viewModel: RouterViewModel,
    ifaceId: String,
    onBack: () -> Unit = {}
) {
    val orig by viewModel.wanConnection.collectAsState()
    val schedules by viewModel.wanSchedules.collectAsState()
    val saving by viewModel.wanSaving.collectAsState()
    val saveMessage by viewModel.wanSaveMessage.collectAsState()

    LaunchedEffect(ifaceId) { viewModel.loadWanConnection(ifaceId) }

    var draft by remember { mutableStateOf<WanConnection?>(null) }
    var lastLoadedId by remember { mutableStateOf("") }
    if (orig != null && (draft == null || lastLoadedId != orig!!.id || lastLoadedId.isBlank())) {
        draft = orig
        lastLoadedId = orig!!.id
    }
    var showConfirm by remember { mutableStateOf(false) }
    var deleteArmed by remember { mutableStateOf(false) }
    var deleteResult by remember { mutableStateOf("") }

    SectionScaffold(
        title = draft?.description?.ifBlank { ifaceId } ?: ifaceId,
        subtitle = "Ethernet-подключение",
        onBack = onBack,
        onRefresh = { viewModel.loadWanConnection(ifaceId) }
    ) { padding ->
        val d = draft
        if (d == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    SectionCard {
                        Text(
                            "Загрузка параметров подключения…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
            return@SectionScaffold
        }
        val changed = d != orig
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Статус + выключатель
            item(key = "status") {
                SectionCard(title = "Ethernet-подключение") {
                    SwitchRow(
                        label = "Подключение включено",
                        checked = d.adminUp,
                        onCheckedChange = { draft = d.copy(adminUp = it) }
                    )
                    InfoRow(
                        "Состояние",
                        if (d.connected) "Подключено" else "Отключено",
                        valueColor = if (d.connected) KeeneticColors.Success else KeeneticColors.Error
                    )
                    if (d.address.isNotBlank()) {
                        InfoRow("IP-адрес", d.address, monospace = true)
                    }
                }
            }

            // 2. Расписание работы
            item(key = "schedule") {
                SectionCard(title = "Расписание работы") {
                    WanDropdown(
                        label = "Расписание",
                        options = listOf("" to "Работает постоянно") +
                            schedules.map { it.name to it.name },
                        current = d.schedule,
                        onSelect = { draft = d.copy(schedule = it) }
                    )
                }
            }

            // 3. Приоритет подключения
            item(key = "order") {
                SectionCard(title = "Приоритет подключения") {
                    WanDropdown(
                        label = "Приоритет",
                        options = listOf(
                            "-1" to "Приоритет не выбран",
                            "0" to "Основное подключение",
                            "1" to "Резервное 1",
                            "2" to "Резервное 2"
                        ),
                        current = d.order.toString(),
                        onSelect = { draft = d.copy(order = it.toIntOrNull() ?: 0) }
                    )
                }
            }

            // 4. Тип подключения (отображение; смена типа — через веб)
            item(key = "conntype") {
                SectionCard(title = "Тип подключения") {
                    InfoRow(
                        "Режим",
                        if (d.connType == "ON_DEMAND") "По требованию" else "Работает постоянно"
                    )
                    if (d.connType == "ON_DEMAND" && d.onDemandTimeout > 0) {
                        InfoRow("Время ожидания попыток, сек", d.onDemandTimeout.toString())
                    }
                }
            }

            // 5. Имя провайдера
            item(key = "name") {
                SectionCard(title = "Имя провайдера") {
                    OutlinedTextField(
                        value = d.description,
                        onValueChange = { draft = d.copy(description = it) },
                        label = { Text("Имя провайдера") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        "Вы можете назвать это подключение, как вам удобно.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            // 6. Удаление (двойное подтверждение)
            item(key = "delete") {
                SectionCard {
                    OutlinedButton(
                        onClick = {
                            if (deleteArmed) {
                                viewModel.deleteWanConnection(d.id) { ok, msg ->
                                    deleteResult = msg
                                    if (ok) onBack()
                                }
                            } else {
                                deleteArmed = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error)
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(if (deleteArmed) "Нажмите ещё раз для удаления" else "Удалить подключение")
                    }
                    if (deleteArmed) {
                        Text(
                            "Внимание: удаление активного WAN-подключения разорвёт интернет. " +
                                "Повторное нажатие выполняет удаление.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.Error
                        )
                    }
                    if (deleteResult.isNotBlank()) {
                        Text(
                            deleteResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            // 7. Порты и VLAN'ы (только просмотр: смена роли порта может убить WAN)
            item(key = "ports") {
                SectionCard(title = "Порты и VLAN'ы") {
                    if (d.switchPorts.isEmpty()) {
                        Text(
                            "Нет данных о портах свитча.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    } else {
                        d.switchPorts.forEach { p ->
                            InfoRow(
                                "Порт ${p.label}" + (if (p.role.isNotBlank()) " (${p.role})" else ""),
                                (if (p.linkUp) "up" else "down") +
                                    (if (p.speed.isNotBlank()) " · ${p.speed}" else "") +
                                    (if (p.duplex.isNotBlank()) " · ${p.duplex}" else ""),
                                valueColor = if (p.linkUp) KeeneticColors.Success else KeeneticColors.TextSecondary
                            )
                        }
                    }
                    Text(
                        "Назначение ролей портам меняется в веб-интерфейсе роутера.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            // 8. Параметры IPv4
            item(key = "ipv4") {
                SectionCard(title = "Параметры IPv4") {
                    if (d.address.isNotBlank()) {
                        InfoRow("Текущий IP-адрес", d.address, monospace = true)
                    }
                    WanDropdown(
                        label = "Настройка IPv4",
                        options = listOf(
                            "dhcp" to "Автоматическая (DHCP)",
                            "static" to "Ручная",
                            "off" to "Не используется"
                        ),
                        current = d.ipMode,
                        onSelect = { draft = d.copy(ipMode = it) }
                    )
                    if (d.ipMode == "static") {
                        WanText("IPv4-адрес", d.staticIp.ifBlank { d.address }, { draft = d.copy(staticIp = it) })
                        WanText("Маска подсети IPv4", d.staticMask.ifBlank { d.mask }, { draft = d.copy(staticMask = it) })
                        WanText("Основной шлюз IPv4", d.staticGateway, { draft = d.copy(staticGateway = it) })
                    }
                    SwitchRow(
                        label = "Игнорировать DNS провайдера",
                        checked = !d.dhcpDnsRoutes,
                        onCheckedChange = { draft = d.copy(dhcpDnsRoutes = !it) }
                    )
                }
            }

            // 9. Параметры IPv6 (отображение)
            item(key = "ipv6") {
                SectionCard(title = "Параметры IPv6") {
                    InfoRow(
                        "Настройка IPv6",
                        when (d.ipv6Mode) {
                            "auto" -> "Автоматическая"
                            "manual" -> "Ручная"
                            else -> "Не используется"
                        }
                    )
                    if (d.ipv6Address.isNotBlank()) InfoRow("IPv6-адрес", d.ipv6Address, monospace = true)
                    if (d.ipv6Prefix.isNotBlank()) InfoRow("IPv6-префикс", d.ipv6Prefix, monospace = true)
                    if (d.ipv6Gateway.isNotBlank()) InfoRow("Основной шлюз IPv6", d.ipv6Gateway, monospace = true)
                    if (d.ipv6Dns.isNotBlank()) InfoRow("IPv6 DNS", d.ipv6Dns, monospace = true)
                }
            }

            // 10. Требования провайдера
            item(key = "provider") {
                SectionCard(title = "Требования провайдера") {
                    WanDropdown(
                        label = "MAC-адрес",
                        options = listOf(
                            "factory" to "По умолчанию",
                            "manual" to "Вручную",
                            "random" to "Случайный"
                        ),
                        current = d.macMode,
                        onSelect = { draft = d.copy(macMode = it) }
                    )
                    if (d.macMode == "manual") {
                        WanText("MAC вручную", d.macManual.ifBlank { d.stateMac }, { draft = d.copy(macManual = it) })
                    } else {
                        InfoRow("Текущий MAC", d.stateMac.ifBlank { "—" }, monospace = true)
                    }
                    WanText("Имя устройства", d.hostname, { draft = d.copy(hostname = it) })
                    WanText(
                        "Размер MTU",
                        d.cfgMtu.takeIf { it > 0 }?.toString() ?: d.stateMtu.takeIf { it > 0 }?.toString().orEmpty(),
                        { draft = d.copy(cfgMtu = it.toIntOrNull() ?: 0) }
                    )
                }
            }

            // 11. Аутентификация (PPPoE; PPTP/L2TP — только отображение типа)
            item(key = "auth") {
                SectionCard(title = "Аутентификация у провайдера (PPPoE / PPTP / L2TP)") {
                    WanDropdown(
                        label = "Тип (протокол)",
                        options = listOf(
                            "none" to "Не использовать",
                            "pppoe" to "PPPoE"
                        ) + (if (d.authType.equals("pptp", true) || d.authType.equals("l2tp", true))
                            listOf(d.authType.lowercase() to d.authType.uppercase() + " (текущий)") else emptyList()),
                        current = if (d.authType.equals("pppoe", true)) "pppoe"
                        else if (d.authType.equals("pptp", true) || d.authType.equals("l2tp", true)) d.authType.lowercase()
                        else "none",
                        onSelect = { draft = d.copy(authType = it) }
                    )
                    if (d.authType.equals("pppoe", true)) {
                        WanText("Имя пользователя", d.authLogin, { draft = d.copy(authLogin = it) })
                        WanText("Пароль", d.authPassword, { draft = d.copy(authPassword = it) }, secret = true)
                        WanText("Адрес сервера", d.authServer, { draft = d.copy(authServer = it) })
                        WanDropdown(
                            label = "Проверка подлинности",
                            options = listOf(
                                "auto" to "Авто",
                                "pap" to "PAP",
                                "chap" to "CHAP",
                                "mschap" to "MSCHAP",
                                "mschap-v2" to "MSCHAP-V2"
                            ),
                            current = d.authMethod.ifBlank { "auto" },
                            onSelect = { draft = d.copy(authMethod = it) }
                        )
                    }
                }
            }

            // 12. Ping Check
            item(key = "pingcheck") {
                SectionCard(title = "Проверка доступности интернета (Ping Check)") {
                    InfoRow(
                        "Статус",
                        if (d.connected && d.pingCheckProfile.isNotBlank()) "Доступен" else "Не проверяется",
                        valueColor = if (d.connected && d.pingCheckProfile.isNotBlank()) KeeneticColors.Success else KeeneticColors.TextSecondary
                    )
                    WanText("Профиль", d.pingCheckProfile, { draft = d.copy(pingCheckProfile = it) })
                    InfoRow("Режим", "Автоматический")
                }
            }

            // 13. Сохранить / Отменить как в веб
            item(key = "savebar") {
                SectionCard {
                    if (saveMessage.isNotBlank()) {
                        Text(
                            saveMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (saveMessage == "Сохранено") KeeneticColors.Success else KeeneticColors.Error
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { draft = orig; deleteArmed = false },
                            enabled = changed && !saving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Отменить")
                        }
                        Button(
                            onClick = { showConfirm = true },
                            enabled = changed && !saving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (saving) "Сохранение…" else "Сохранить")
                        }
                    }
                    if (!changed) {
                        Text(
                            "Изменений нет.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }

        if (showConfirm && draft != null) {
            val dd = draft!!
            val changedSections = mutableListOf<String>().apply {
                if (dd.adminUp != orig?.adminUp) add("включение")
                if (dd.schedule != orig?.schedule) add("расписание")
                if (dd.order != orig?.order) add("приоритет")
                if (dd.description != orig?.description) add("имя")
                if (dd.ipMode != orig?.ipMode || dd.staticIp != orig?.staticIp ||
                    dd.staticMask != orig?.staticMask || dd.staticGateway != orig?.staticGateway ||
                    dd.dhcpDnsRoutes != orig?.dhcpDnsRoutes
                ) add("IPv4")
                if (dd.macMode != orig?.macMode || dd.macManual != orig?.macManual ||
                    dd.hostname != orig?.hostname || dd.cfgMtu != orig?.cfgMtu
                ) add("требования провайдера")
                if (dd.authType != orig?.authType || dd.authLogin != orig?.authLogin ||
                    dd.authPassword != orig?.authPassword || dd.authServer != orig?.authServer ||
                    dd.authMethod != orig?.authMethod
                ) add("авторизация")
                if (dd.pingCheckProfile != orig?.pingCheckProfile) add("Ping Check")
            }
            ConfirmDialog(
                title = "Сохранить настройки?",
                message = "Изменённые разделы: ${changedSections.joinToString(", ")}. " +
                    "Неверные настройки могут разорвать интернет-соединение. " +
                    "Команды сначала проверяются роутером без сохранения.",
                confirmLabel = "Сохранить",
                onConfirm = {
                    showConfirm = false
                    viewModel.saveWanConnection(dd) { _, _ -> }
                },
                onDismiss = { showConfirm = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WanDropdown(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == current }?.second ?: current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, title) ->
                    DropdownMenuItem(
                        text = { Text(title, color = KeeneticColors.TextPrimary) },
                        onClick = { onSelect(value); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun WanText(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    secret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    )
}
