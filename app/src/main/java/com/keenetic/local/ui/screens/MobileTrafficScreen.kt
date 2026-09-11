package com.keenetic.local.ui.screens

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

private val UNITS = listOf("Б", "КБ", "МБ", "ГБ", "ТБ")
private fun unitToNdm(u: String): String = when (u) {
    "Б" -> "B"
    "КБ" -> "KB"
    "МБ" -> "MB"
    "ГБ" -> "GB"
    "ТБ" -> "TB"
    else -> u
}
private fun ndmToUnit(u: String): String = when (u) {
    "B" -> "Б"
    "KB" -> "КБ"
    "MB" -> "МБ"
    "GB" -> "ГБ"
    "TB" -> "ТБ"
    else -> u
}

@Composable
fun MobileTrafficScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val traffic by viewModel.mobileTraffic.collectAsState()

    var limitText by remember { mutableStateOf(traffic.limit.toString()) }
    var unitText by remember { mutableStateOf(ndmToUnit(traffic.unit)) }
    var thresholdText by remember { mutableStateOf(traffic.threshold.toString()) }
    var dayText by remember { mutableStateOf(traffic.dayOfMonth.toString()) }
    var smsWarning by remember { mutableStateOf(traffic.smsWarningEnabled) }
    var smsLimit by remember { mutableStateOf(traffic.smsLimitEnabled) }
    var disconnect by remember { mutableStateOf(traffic.disconnect) }
    var smsPhone by remember { mutableStateOf(traffic.smsPhone) }
    var smsMessage by remember { mutableStateOf(traffic.smsMessage) }
    var cycleReset by remember { mutableStateOf(traffic.cycleResetEnabled) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // refresh local fields when router data arrives
    LaunchedEffect(traffic) {
        limitText = traffic.limit.toString()
        unitText = ndmToUnit(traffic.unit)
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

    var expandedUnit by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.Speed, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Квота мобильного трафика",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadMobileTraffic() }) {
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
                        Icon(Icons.Default.Speed, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Лимит трафика", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ограничить мобильный интернет", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = traffic.enable, onCheckedChange = {
                            viewModel.updateMobileTraffic(
                                enable = it,
                                limit = limitText.toLongOrNull() ?: 0,
                                unit = unitToNdm(unitText),
                                dayOfMonth = dayText.toIntOrNull() ?: 1,
                                cycleResetEnabled = cycleReset,
                                threshold = thresholdText.toIntOrNull() ?: 90,
                                smsWarningEnabled = smsWarning,
                                smsLimitEnabled = smsLimit,
                                smsPhone = smsPhone,
                                smsMessage = smsMessage,
                                disconnect = disconnect
                            )
                        })
                    }

                    if (traffic.enable) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = limitText,
                                onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                                label = { Text("Лимит данных") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                OutlinedButton(onClick = { expandedUnit = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(unitText, color = KeeneticColors.TextPrimary)
                                }
                                DropdownMenu(
                                    expanded = expandedUnit,
                                    onDismissRequest = { expandedUnit = false },
                                    modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
                                ) {
                                    UNITS.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text("$u (${unitToNdm(u)})") },
                                            onClick = { unitText = u; expandedUnit = false }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = thresholdText,
                            onValueChange = { thresholdText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Порог предупреждения, %") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Действия при превышении", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SMS при достижении порога", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = smsWarning, onCheckedChange = { smsWarning = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SMS при достижении лимита", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = smsLimit, onCheckedChange = { smsLimit = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Отключить мобильное подключение", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = disconnect, onCheckedChange = { disconnect = it })
                    }

                    if (smsWarning || smsLimit) {
                        OutlinedTextField(
                            value = smsPhone,
                            onValueChange = { smsPhone = it },
                            label = { Text("Номер телефона (пусто = центр SMS)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = smsMessage,
                            onValueChange = { smsMessage = it },
                            label = { Text("Текст SMS-сообщения") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Сброс счётчика", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ежемесячно сбрасывать использование", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                        Switch(checked = cycleReset, onCheckedChange = { cycleReset = it })
                    }
                    if (cycleReset) {
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { dayText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("День месяца (1-28)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.updateMobileTraffic(
                        enable = traffic.enable,
                        limit = limitText.toLongOrNull() ?: 0,
                        unit = unitToNdm(unitText),
                        dayOfMonth = dayText.toIntOrNull() ?: 1,
                        cycleResetEnabled = cycleReset,
                        threshold = thresholdText.toIntOrNull() ?: 90,
                        smsWarningEnabled = smsWarning,
                        smsLimitEnabled = smsLimit,
                        smsPhone = smsPhone,
                        smsMessage = smsMessage,
                        disconnect = disconnect
                    )
                    feedbackMessage = "Квота мобильного трафика отправлена на роутер"
                },
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Активировать")
            }
        }
    }
}