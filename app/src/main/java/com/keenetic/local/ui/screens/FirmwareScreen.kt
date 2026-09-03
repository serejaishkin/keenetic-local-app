package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun FirmwareScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val status by viewModel.firmwareStatus.collectAsState()
    val isRebooting by viewModel.isRebooting.collectAsState()
    val rebootMessage by viewModel.rebootMessage.collectAsState()
    val selectedRebootMethod by viewModel.selectedRebootMethod.collectAsState()
    val sshPort by viewModel.sshPort.collectAsState()
    val savedIp by viewModel.savedIp.collectAsState()
    val savedUsername by viewModel.savedUsername.collectAsState()
    var showRebootDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFirmwareStatus()
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
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "KeeneticOS и система",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        rebootMessage?.let { msg ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Primary)
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearRebootMessage() }) {
                            Text("OK", color = KeeneticColors.Primary, fontWeight = FontWeight.Bold)
                        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = KeeneticColors.Primary)
                            Text(status?.title ?: "KeeneticOS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                        }
                        Surface(
                            color = KeeneticColors.Primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                status?.channel ?: "Release",
                                color = KeeneticColors.Primary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (status?.model?.isNotBlank() == true) {
                        Text(
                            "Модель: ${status?.model}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextSecondary
                        )
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    if (status?.updateAvailable == true) {
                        Text("Доступно обновление: ${status?.availableVersion}", color = KeeneticColors.Success, style = MaterialTheme.typography.titleSmall)
                        Text(status?.changelog ?: "", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { viewModel.startFirmwareUpdate() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Установить обновление")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KeeneticColors.Success)
                            Text("Установлена актуальная версия ПО", color = KeeneticColors.Success, style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = { viewModel.loadFirmwareStatus() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить обновления", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = KeeneticColors.Error
                        )
                        Column {
                            Text(
                                "Перезагрузка интернет-центра",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Основной метод через RCI и вторичный через SSH (JSch)",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }

                    Text(
                        "Выберите протокол для передачи команды перезагрузки интернет-центру Keenetic. При перезагрузке все текущие соединения будут кратковременно прерваны.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )

                    // Method Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RCI API Card
                        val isRciSelected = selectedRebootMethod == RouterViewModel.RebootMethod.RCI
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setSelectedRebootMethod(RouterViewModel.RebootMethod.RCI) }
                                .border(
                                    width = if (isRciSelected) 2.dp else 1.dp,
                                    color = if (isRciSelected) KeeneticColors.Primary else KeeneticColors.Border,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRciSelected) KeeneticColors.Primary.copy(alpha = 0.12f) else KeeneticColors.Background
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = if (isRciSelected) KeeneticColors.Primary else KeeneticColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "RCI REST API",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRciSelected) KeeneticColors.Primary else KeeneticColors.TextPrimary
                                    )
                                }
                                Text(
                                    "Основной способ (HTTP/S)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }

                        // SSH JSch Card
                        val isSshSelected = selectedRebootMethod == RouterViewModel.RebootMethod.SSH
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setSelectedRebootMethod(RouterViewModel.RebootMethod.SSH) }
                                .border(
                                    width = if (isSshSelected) 2.dp else 1.dp,
                                    color = if (isSshSelected) KeeneticColors.Primary else KeeneticColors.Border,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSshSelected) KeeneticColors.Primary.copy(alpha = 0.12f) else KeeneticColors.Background
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = if (isSshSelected) KeeneticColors.Primary else KeeneticColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "SSH (JSch)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSshSelected) KeeneticColors.Primary else KeeneticColors.TextPrimary
                                    )
                                }
                                Text(
                                    "Вторичный способ (SSH2)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }
                    }

                    // Configuration & Explanations based on selected method
                    if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KeeneticColors.Background, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = KeeneticColors.Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Защищенный SSH-канал (библиотека JSch v0.1.55)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KeeneticColors.Success
                                )
                            }
                            Text(
                                "Отправляет CLI-команду 'system reboot' напрямую через зашифрованный SSH2 сеанс. Незаменим в качестве резервного метода, если веб-сервер роутера (порт 80/443) недоступен.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                            OutlinedTextField(
                                value = sshPort,
                                onValueChange = { viewModel.setSshPort(it.filter { ch -> ch.isDigit() }) },
                                label = { Text("SSH порт интернет-центра") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KeeneticColors.Primary,
                                    unfocusedBorderColor = KeeneticColors.Border
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KeeneticColors.Background, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lan,
                                contentDescription = null,
                                tint = KeeneticColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Отправка команды {'system': {'reboot': {}}} через REST API локального интерфейса KeeneticOS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showRebootDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRebooting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeeneticColors.Error.copy(alpha = 0.85f),
                            contentColor = KeeneticColors.Background
                        )
                    ) {
                        if (isRebooting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KeeneticColors.Background,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправка команды...")
                        } else {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH)
                                    "Перезагрузить через SSH (JSch)"
                                else
                                    "Перезагрузить через RCI API"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRebooting) showRebootDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = KeeneticColors.Error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Подтверждение перезагрузки",
                    style = MaterialTheme.typography.titleLarge,
                    color = KeeneticColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Вы действительно хотите перезагрузить интернет-центр Keenetic? Все активные сетевые сессии клиентов будут кратковременно прерваны.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KeeneticColors.TextSecondary
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Background),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Метод: ${if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) "SSH (JSch библиотека)" else "RCI REST API"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = KeeneticColors.TextPrimary
                            )
                            Text(
                                "Цель: $savedIp:${if (selectedRebootMethod == RouterViewModel.RebootMethod.SSH) sshPort else "80/443"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                            Text(
                                "Пользователь: $savedUsername",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rebootRouter(selectedRebootMethod) { success, _ ->
                            showRebootDialog = false
                        }
                    },
                    enabled = !isRebooting,
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error)
                ) {
                    if (isRebooting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = KeeneticColors.Background,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Да, перезагрузить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRebootDialog = false },
                    enabled = !isRebooting
                ) {
                    Text("Отмена", color = KeeneticColors.TextPrimary)
                }
            },
            containerColor = KeeneticColors.Surface
        )
    }
}
