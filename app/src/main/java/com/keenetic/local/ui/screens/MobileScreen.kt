package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun MobileScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val modemStatus by viewModel.mobileModemStatus.collectAsState()
    var selectedNetMode by remember { mutableStateOf("auto") } // "auto", "lte_only", "3g_only"
    var selectedAppRole by remember { mutableStateOf("backup") } // "always", "backup", "schedule", "disabled"
    var showUssdDialog by remember { mutableStateOf(false) }
    var ussdCommand by remember { mutableStateOf("*100#") }
    var showApnDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMobileStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Мобильный интернет",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadMobileStatus() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        if (feedbackMessage != null) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { feedbackMessage = null }) {
                            Text("OK", color = KeeneticColors.Primary)
                        }
                    }
                ) {
                    Text(feedbackMessage ?: "")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("USB-модем / SIM-карта", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    }
                    Text(
                        "Подключите совместимый 3G/4G/5G USB-модем к Keenetic или установите SIM-карту (для моделей со встроенным модемом) для резервного или основного канала связи.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Статус подключения", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                        Surface(
                            color = if (modemStatus.connected) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.TextSecondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (modemStatus.connected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (modemStatus.connected) KeeneticColors.Success else KeeneticColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    if (modemStatus.connected) "Подключено" else "Не подключено",
                                    color = if (modemStatus.connected) KeeneticColors.Success else KeeneticColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    if (modemStatus.connected) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Оператор", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(modemStatus.operator, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Стандарт связи", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(modemStatus.networkType, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Уровень сигнала", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("${modemStatus.signalStrengthPercent}%", color = KeeneticColors.Primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        if (modemStatus.ip.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IP-адрес", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                Text(modemStatus.ip, color = KeeneticColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text(
                            "Модем не обнаружен в USB-порту или не отвечает на AT/QMI/NCM команды.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                        Button(
                            onClick = { viewModel.loadMobileStatus() },
                            colors = ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить опрос модема", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        }

        // Режим работы мобильного приложения (веб-морда: Мобильный интернет -> Режим)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Режим работы", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    }
                    Text(
                        "Определяет, когда мобильное соединение используется в качестве канала доступа в интернет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    HorizontalDivider(color = KeeneticColors.Divider)
                    listOf(
                        "always" to "Всегда основной канал",
                        "backup" to "Резервный канал (по умолчанию)",
                        "schedule" to "По расписанию",
                        "disabled" to "Отключено"
                    ).forEach { (modeKey, modeName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAppRole = modeKey
                                    viewModel.setMobileActivationType(modeKey)
                                    feedbackMessage = "Задан режим работы: $modeName"
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(modeName, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                            Icon(
                                if (selectedAppRole == modeKey) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedAppRole == modeKey) KeeneticColors.Primary else KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Sub-settings Section: Network standard and controls
        item {
            Text(
                "Параметры мобильной сети",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Настройки подключения (APN)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Text(
                        "Точка доступа, имя пользователя, пароль, телефон, TTL и игнорирование DNS — как на странице веб-морды «Через сотовую сеть».",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Button(
                        onClick = { showApnDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Настроить подключение")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Выбор технологии сети", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedNetMode == "auto",
                            onClick = {
                                selectedNetMode = "auto"
                                viewModel.setModemMode("auto")
                                feedbackMessage = "Установлен режим: Авто 4G/3G/2G"
                            },
                            label = { Text("Авто 4G/3G") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedNetMode == "lte_only",
                            onClick = {
                                selectedNetMode = "lte_only"
                                viewModel.setModemMode("lte_only")
                                feedbackMessage = "Установлен режим: Только 4G (LTE)"
                            },
                            label = { Text("Только 4G") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedNetMode == "3g_only",
                            onClick = {
                                selectedNetMode = "3g_only"
                                viewModel.setModemMode("3g_only")
                                feedbackMessage = "Установлен режим: Только 3G (UMTS)"
                            },
                            label = { Text("Только 3G") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showUssdDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.SurfaceElevated, contentColor = KeeneticColors.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("USSD запрос")
                        }

                        Button(
                            onClick = {
                                viewModel.reconnectInterface(modemStatus.interfaceName)
                                feedbackMessage = "Отправлена команда перезапуска модема «${modemStatus.interfaceName}»"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.SurfaceElevated, contentColor = KeeneticColors.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Рестарт модема")
                        }
                    }
                }
            }
        }
    }

    // USSD Dialog
    if (showUssdDialog) {
        AlertDialog(
            onDismissRequest = {
                showUssdDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Dialpad, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("USSD-запрос модема", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ussdCommand,
                        onValueChange = { ussdCommand = it },
                        label = { Text("Команда (например *100#)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Запрос будет отправлен на модем для получения ответа оператора (баланс, тариф и т.п.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendUssdCommand(ussdCommand)
                        feedbackMessage = "USSD-запрос «$ussdCommand» отправлен на модем"
                        showUssdDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUssdDialog = false
                }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    // APN (мобильное подключение) Dialog
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
        var ignoreDns by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApnDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Подключение через сотовую сеть", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Телефон (номер APN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Дополнительная инициализация", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        Switch(checked = extraInit, onCheckedChange = { extraInit = it })
                    }
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

                    HorizontalDivider(color = KeeneticColors.Divider)

                    Text("Установить TTL", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    var ttlExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { ttlExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                when (ttl) {
                                    "INCOMING" -> "Входящий"
                                    "OUTGOING" -> "Исходящий"
                                    "BOTH_DIRECTIONS" -> "Оба направления"
                                    else -> "Отключено"
                                },
                                color = KeeneticColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DropdownMenu(expanded = ttlExpanded, onDismissRequest = { ttlExpanded = false }, modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
                            DropdownMenuItem(text = { Text("Отключено") }, onClick = { ttl = "DISABLED"; ttlExpanded = false })
                            DropdownMenuItem(text = { Text("Входящий") }, onClick = { ttl = "INCOMING"; ttlExpanded = false })
                            DropdownMenuItem(text = { Text("Исходящий") }, onClick = { ttl = "OUTGOING"; ttlExpanded = false })
                            DropdownMenuItem(text = { Text("Оба направления") }, onClick = { ttl = "BOTH_DIRECTIONS"; ttlExpanded = false })
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Игнорировать DNS интернет-провайдера", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        Switch(checked = ignoreDns, onCheckedChange = { ignoreDns = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                        feedbackMessage = "Настройки мобильного подключения отправлены на роутер"
                        showApnDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApnDialog = false }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}
