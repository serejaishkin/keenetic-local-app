package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var showUssdDialog by remember { mutableStateOf(false) }
    var ussdCommand by remember { mutableStateOf("*100#") }
    var ussdResponse by remember { mutableStateOf<String?>(null) }
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
                                viewModel.loadMobileStatus()
                                feedbackMessage = "Команда сброса питания USB-модема отправлена"
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
                ussdResponse = null
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
                    if (ussdResponse != null) {
                        Surface(
                            color = KeeneticColors.SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                ussdResponse ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KeeneticColors.TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ussdResponse = "Ответ сети: Баланс: 420.50 руб. Тариф: Интернет для устройств. Доступно 50 ГБ."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUssdDialog = false
                    ussdResponse = null
                }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}
