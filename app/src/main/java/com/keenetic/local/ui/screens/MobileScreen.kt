package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

private val APP_ROLES = listOf(
    "always" to "Всегда основной канал",
    "backup" to "Резервный канал (по умолчанию)",
    "schedule" to "По расписанию",
    "disabled" to "Отключено"
)

private val NET_MODES = listOf(
    "auto" to "Авто 4G/3G",
    "lte_only" to "Только 4G",
    "3g_only" to "Только 3G"
)

private val TTL_OPTIONS = listOf(
    "DISABLED" to "Отключено",
    "INCOMING" to "Входящий",
    "OUTGOING" to "Исходящий",
    "BOTH_DIRECTIONS" to "Оба направления"
)

@Composable
fun MobileScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val modemStatus by viewModel.mobileModemStatus.collectAsState()
    var selectedNetMode by remember { mutableStateOf("auto") } // "auto", "lte_only", "3g_only"
    var selectedAppRole by remember { mutableStateOf("backup") } // "always", "backup", "schedule", "disabled"
    var showUssdDialog by remember { mutableStateOf(false) }
    var ussdCommand by remember { mutableStateOf("*100#") }
    var showApnDialog by remember { mutableStateOf(false) }
    var rolePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMobileStatus()
    }

    SectionScaffold(
        title = "Мобильный интернет",
        subtitle = "3G/4G/5G-модем или встроенный модуль с SIM-картой",
        onBack = onBack,
        onRefresh = { viewModel.loadMobileStatus() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hint") {
                SectionCard(icon = Icons.Default.SignalCellularAlt) {
                    Text(
                        "Подключите совместимый 3G/4G/5G USB-модем к Keenetic или установите " +
                            "SIM-карту (для моделей со встроенным модемом) для резервного или " +
                            "основного канала связи.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            item(key = "status") {
                SectionCard(
                    title = "Статус подключения",
                    subtitle = modemStatus.interfaceName.ifBlank { "Интерфейс не найден" }
                ) {
                    InfoRow(
                        label = "Модем",
                        value = if (modemStatus.connected) "Подключён" else "Не подключён",
                        valueColor = if (modemStatus.connected) KeeneticColors.Success else KeeneticColors.TextSecondary
                    )
                    if (modemStatus.connected) {
                        InfoRow("Оператор", modemStatus.operator)
                        InfoRow("Стандарт связи", modemStatus.networkType)
                        InfoRow("Уровень сигнала", "${modemStatus.signalStrengthPercent}%")
                        if (modemStatus.ip.isNotBlank()) {
                            InfoRow("IP-адрес", modemStatus.ip, monospace = true)
                        }
                    } else {
                        Text(
                            "Модем не обнаружен в USB-порту или не отвечает на AT/QMI/NCM команды.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                        OutlinedButton(
                            onClick = { viewModel.loadMobileStatus() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить опрос модема", color = KeeneticColors.Primary)
                        }
                    }
                }
            }

            item(key = "role") {
                SectionCard(icon = Icons.Default.Settings) {
                    Text(
                        "Определяет, когда мобильное соединение используется в качестве канала " +
                            "доступа в интернет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    EditableRow(
                        label = "Режим работы",
                        value = APP_ROLES.firstOrNull { it.first == selectedAppRole }?.second ?: selectedAppRole,
                        onClick = { rolePicker = true }
                    )
                }
            }

            item(key = "apn") {
                SectionCard(title = "Настройки подключения (APN)") {
                    Text(
                        "Точка доступа, имя пользователя, пароль, телефон, TTL и игнорирование DNS — " +
                            "как на странице веб-морды «Через сотовую сеть».",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    OutlinedButton(
                        onClick = { showApnDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Настроить подключение", color = KeeneticColors.Primary)
                    }
                }
            }

            item(key = "netmode") {
                SectionCard {
                    SubGroupHeader("Технология сети")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NET_MODES.forEach { (key, name) ->
                            FilterChip(
                                selected = selectedNetMode == key,
                                onClick = {
                                    selectedNetMode = key
                                    viewModel.setModemMode(key)
                                },
                                label = { Text(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    RowDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUssdDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("USSD-запрос", color = KeeneticColors.Primary)
                        }
                        OutlinedButton(
                            onClick = { viewModel.reconnectInterface(modemStatus.interfaceName) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Рестарт модема", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        }
    }

    if (rolePicker) {
        OptionPickerDialog(
            title = "Режим работы",
            options = APP_ROLES,
            selectedKey = selectedAppRole,
            onSelect = {
                selectedAppRole = it
                viewModel.setMobileActivationType(it)
            },
            onDismiss = { rolePicker = false }
        )
    }

    if (showUssdDialog) {
        FormDialog(
            title = "USSD-запрос модема",
            confirmLabel = "Отправить",
            onDismiss = { showUssdDialog = false },
            onConfirm = {
                viewModel.sendUssdCommand(ussdCommand)
                showUssdDialog = false
            }
        ) {
            OutlinedTextField(
                value = ussdCommand,
                onValueChange = { ussdCommand = it },
                label = { Text("Команда (например *100#)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Запрос будет отправлен на модем для получения ответа оператора " +
                    "(баланс, тариф и т.п.).",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
    }

    if (showApnDialog) {
        var apn by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var extraInit by remember { mutableStateOf(false) }
        var at0 by remember { mutableStateOf("") }
        var at1 by remember { mutableStateOf("") }
        var at2 by remember { mutableStateOf("") }
        var ttl by remember { mutableStateOf("DISABLED") }
        var ttlPicker by remember { mutableStateOf(false) }
        var ignoreDns by remember { mutableStateOf(false) }

        FormDialog(
            title = "Подключение через сотовую сеть",
            onDismiss = { showApnDialog = false },
            onConfirm = {
                viewModel.updateMobileConnectionSettings(
                    interfaceId = modemStatus.interfaceName.ifBlank { null },
                    apn = apn.ifBlank { null },
                    username = username.ifBlank { null },
                    password = password.ifBlank { null },
                    phone = phone.ifBlank { null },
                    atCommands = if (extraInit) listOf(at0, at1, at2).filter { it.isNotBlank() } else null,
                    ttlModification = ttl,
                    ignoreRemoteDns = ignoreDns
                )
                showApnDialog = false
            }
        ) {
            OutlinedTextField(
                value = apn,
                onValueChange = { apn = it },
                label = { Text("Точка доступа (APN)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Имя пользователя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Телефон (номер APN)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            RowDivider()

            SwitchRow(
                label = "Дополнительная инициализация",
                description = "AT-команды перед подключением",
                checked = extraInit,
                onCheckedChange = { extraInit = it }
            )
            if (extraInit) {
                OutlinedTextField(
                    value = at0,
                    onValueChange = { at0 = it },
                    label = { Text("AT-команда 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = at1,
                    onValueChange = { at1 = it },
                    label = { Text("AT-команда 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = at2,
                    onValueChange = { at2 = it },
                    label = { Text("AT-команда 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            RowDivider()

            EditableRow(
                label = "Установить TTL",
                value = TTL_OPTIONS.firstOrNull { it.first == ttl }?.second ?: ttl,
                onClick = { ttlPicker = true }
            )

            SwitchRow(
                label = "Игнорировать DNS интернет-провайдера",
                checked = ignoreDns,
                onCheckedChange = { ignoreDns = it }
            )
        }

        if (ttlPicker) {
            OptionPickerDialog(
                title = "Установить TTL",
                options = TTL_OPTIONS,
                selectedKey = ttl,
                onSelect = { ttl = it },
                onDismiss = { ttlPicker = false }
            )
        }
    }
}
