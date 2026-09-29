package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val MT_UNITS = listOf("Б", "КБ", "МБ", "ГБ", "ТБ")

private fun mtUnitToNdm(u: String): String = when (u) {
    "Б" -> "B"
    "КБ" -> "KB"
    "МБ" -> "MB"
    "ГБ" -> "GB"
    "ТБ" -> "TB"
    else -> u
}

private fun mtNdmToUnit(u: String): String = when (u) {
    "B" -> "Б"
    "KB" -> "КБ"
    "MB" -> "МБ"
    "GB" -> "ГБ"
    "TB" -> "ТБ"
    else -> u
}

private const val DLG_LIMIT = "limit"
private const val DLG_SMS = "sms"
private const val DLG_DAY = "day"

@Composable
fun MobileTrafficScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val traffic by viewModel.mobileTraffic.collectAsState()

    var limitText by remember { mutableStateOf(traffic.limit.toString()) }
    var unitText by remember { mutableStateOf(mtNdmToUnit(traffic.unit)) }
    var thresholdText by remember { mutableStateOf(traffic.threshold.toString()) }
    var dayText by remember { mutableStateOf(traffic.dayOfMonth.toString()) }
    var smsWarning by remember { mutableStateOf(traffic.smsWarningEnabled) }
    var smsLimit by remember { mutableStateOf(traffic.smsLimitEnabled) }
    var disconnect by remember { mutableStateOf(traffic.disconnect) }
    var smsPhone by remember { mutableStateOf(traffic.smsPhone) }
    var smsMessage by remember { mutableStateOf(traffic.smsMessage) }
    var cycleReset by remember { mutableStateOf(traffic.cycleResetEnabled) }

    var dialog by remember { mutableStateOf<String?>(null) }
    var showUnits by remember { mutableStateOf(false) }

    // Данные с роутера — источник истины, локальные поля подстраиваются под них.
    LaunchedEffect(traffic) {
        limitText = traffic.limit.toString()
        unitText = mtNdmToUnit(traffic.unit)
        thresholdText = traffic.threshold.toString()
        dayText = traffic.dayOfMonth.toString()
        smsWarning = traffic.smsWarningEnabled
        smsLimit = traffic.smsLimitEnabled
        disconnect = traffic.disconnect
        smsPhone = traffic.smsPhone
        smsMessage = traffic.smsMessage
        cycleReset = traffic.cycleResetEnabled
    }

    LaunchedEffect(Unit) {
        viewModel.loadMobileTraffic()
    }

    // Любое изменение сразу уходит на роутер — кнопка «Активировать» не нужна.
    fun push(enable: Boolean = traffic.enable) {
        viewModel.updateMobileTraffic(
            enable = enable,
            limit = limitText.toLongOrNull() ?: 0,
            unit = mtUnitToNdm(unitText),
            dayOfMonth = dayText.toIntOrNull() ?: 1,
            cycleResetEnabled = cycleReset,
            threshold = thresholdText.toIntOrNull() ?: 90,
            smsWarningEnabled = smsWarning,
            smsLimitEnabled = smsLimit,
            smsPhone = smsPhone,
            smsMessage = smsMessage,
            disconnect = disconnect
        )
    }

    SectionScaffold(
        title = "Квота мобильного трафика",
        subtitle = if (traffic.enable) "Ограничение включено" else "Ограничение выключено",
        onBack = onBack,
        onRefresh = { viewModel.loadMobileTraffic() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "limit") {
                SectionCard(title = "Лимит трафика", icon = Icons.Default.Speed) {
                    SwitchRow(
                        label = "Ограничить мобильный интернет",
                        checked = traffic.enable,
                        onCheckedChange = { push(enable = it) }
                    )
                    if (traffic.enable) {
                        EditableRow(
                            label = "Объём трафика",
                            value = "$limitText $unitText",
                            onClick = { dialog = DLG_LIMIT },
                            monospaceValue = true
                        )
                        EditableRow(
                            label = "Порог предупреждения",
                            value = "$thresholdText %",
                            onClick = { dialog = DLG_LIMIT }
                        )
                    }
                }
            }

            item(key = "actions") {
                SectionCard(title = "Действия при превышении", icon = Icons.Default.Notifications) {
                    SwitchRow(
                        label = "SMS при достижении порога",
                        checked = smsWarning,
                        enabled = traffic.enable,
                        onCheckedChange = {
                            smsWarning = it
                            push()
                        }
                    )
                    SwitchRow(
                        label = "SMS при достижении лимита",
                        checked = smsLimit,
                        enabled = traffic.enable,
                        onCheckedChange = {
                            smsLimit = it
                            push()
                        }
                    )
                    SwitchRow(
                        label = "Отключить мобильное подключение",
                        checked = disconnect,
                        enabled = traffic.enable,
                        onCheckedChange = {
                            disconnect = it
                            push()
                        }
                    )
                    if (smsWarning || smsLimit) {
                        EditableRow(
                            label = "Номер и текст SMS",
                            value = if (smsPhone.isBlank()) {
                                "центр SMS"
                            } else {
                                smsPhone
                            },
                            onClick = { dialog = DLG_SMS },
                            enabled = traffic.enable,
                            monospaceValue = true
                        )
                    }
                }
            }

            item(key = "reset") {
                SectionCard(title = "Сброс счётчика", icon = Icons.Default.DateRange) {
                    SwitchRow(
                        label = "Ежемесячно сбрасывать использование",
                        checked = cycleReset,
                        enabled = traffic.enable,
                        onCheckedChange = {
                            cycleReset = it
                            push()
                        }
                    )
                    if (cycleReset) {
                        EditableRow(
                            label = "День месяца",
                            value = dayText,
                            onClick = { dialog = DLG_DAY },
                            enabled = traffic.enable
                        )
                    }
                }
            }
        }
    }

    when (dialog) {
        DLG_LIMIT -> FormDialog(
            title = "Лимит трафика",
            onDismiss = { dialog = null; showUnits = false },
            onConfirm = {
                push()
                dialog = null
                showUnits = false
            }
        ) {
            OutlinedTextField(
                value = limitText,
                onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                label = { Text("Объём") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { showUnits = true }) {
                Text("Единица измерения: $unitText", color = KeeneticColors.Primary)
            }
            OutlinedTextField(
                value = thresholdText,
                onValueChange = { thresholdText = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Порог предупреждения, %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        DLG_SMS -> FormDialog(
            title = "SMS-оповещение",
            onDismiss = { dialog = null },
            onConfirm = {
                push()
                dialog = null
            }
        ) {
            OutlinedTextField(
                value = smsPhone,
                onValueChange = { smsPhone = it },
                label = { Text("Номер телефона") },
                supportingText = { Text("Пусто — центр SMS провайдера") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = smsMessage,
                onValueChange = { smsMessage = it },
                label = { Text("Текст сообщения") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        DLG_DAY -> FormDialog(
            title = "День сброса счётчика",
            onDismiss = { dialog = null },
            onConfirm = {
                push()
                dialog = null
            }
        ) {
            OutlinedTextField(
                value = dayText,
                onValueChange = { dayText = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("День месяца (1–28)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showUnits) {
        OptionPickerDialog(
            title = "Единица измерения",
            options = MT_UNITS.map { it to "$it (${mtUnitToNdm(it)})" },
            selectedKey = unitText,
            onSelect = { unitText = it },
            onDismiss = { showUnits = false }
        )
    }
}
