package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.UsbStorageDevice
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun UsbStorageScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val usbDevices by viewModel.usbStorageList.collectAsState()
    var selectedDevice by remember { mutableStateOf<UsbStorageDevice?>(null) }
    var smbEnabled by remember { mutableStateOf(true) }
    var dlnaEnabled by remember { mutableStateOf(false) }
    var ftpEnabled by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsbDevices()
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
                Icon(Icons.Default.Usb, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "USB и накопители",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadUsbDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        if (statusMessage != null) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { statusMessage = null }) {
                            Text("OK", color = KeeneticColors.Primary)
                        }
                    }
                ) {
                    Text(statusMessage ?: "")
                }
            }
        }

        // Section: Connected Drives
        item {
            Text(
                "Подключенные накопители",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
        }

        if (usbDevices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Usb,
                            contentDescription = null,
                            tint = KeeneticColors.TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "USB-накопители не подключены",
                            style = MaterialTheme.typography.titleMedium,
                            color = KeeneticColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Подключите внешний жесткий диск или флеш-накопитель к USB-порту Keenetic для организации общего сетевого диска (SMB), медиасервера или торрент-клиента.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadUsbDevices() },
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить USB-порты", color = KeeneticColors.Primary)
                        }
                    }
                }
            }
        } else {
            items(usbDevices) { dev ->
                val usedBytes = (dev.sizeBytes - dev.freeBytes).coerceAtLeast(0L)
                val usedPercent = if (dev.sizeBytes > 0) (usedBytes.toFloat() / dev.sizeBytes).coerceIn(0f, 1f) else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedDevice = dev },
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
                                Icon(Icons.Default.Usb, contentDescription = null, tint = KeeneticColors.Primary)
                                Column {
                                    Text(dev.label, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                                    Text("${dev.vendor} ${dev.model} • ${dev.filesystem}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                                }
                            }
                            IconButton(onClick = { viewModel.ejectUsbDevice(dev.name) }) {
                                Icon(Icons.Default.Eject, contentDescription = "Извлечь", tint = KeeneticColors.Error)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { usedPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = KeeneticColors.Primary,
                            trackColor = KeeneticColors.Divider
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024 * 1024))
                            val totalGb = String.format("%.1f", dev.sizeBytes / (1024.0 * 1024 * 1024))
                            Text(
                                "Занято $usedGb ГБ из $totalGb ГБ",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                            Text(
                                "${(usedPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (dev.mountPoint.isNotBlank()) {
                            Text(
                                "Точка монтирования: ${dev.mountPoint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section: Network USB Services
        item {
            Text(
                "Сетевые службы USB",
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
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // SMB Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = KeeneticColors.Primary)
                            Column {
                                Text("Сеть Windows (SMB / CIFS)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                                Text("Общий доступ к файлам для ПК, ноутбуков и ТВ", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                        Switch(
                            checked = smbEnabled,
                            onCheckedChange = {
                                smbEnabled = it
                                viewModel.toggleUsbService("cifs", it)
                                statusMessage = if (it) "Служба SMB включена" else "Служба SMB отключена"
                            }
                        )
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    // DLNA Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = KeeneticColors.Primary)
                            Column {
                                Text("Медиасервер DLNA", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                                Text("Потоковое воспроизведение видео и музыки на Smart TV", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                        Switch(
                            checked = dlnaEnabled,
                            onCheckedChange = {
                                dlnaEnabled = it
                                viewModel.toggleUsbService("dlna", it)
                                statusMessage = if (it) "Медиасервер DLNA включен" else "DLNA отключен"
                            }
                        )
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    // FTP Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = KeeneticColors.Primary)
                            Column {
                                Text("FTP-сервер", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                                Text("Доступ к файлам по протоколу FTP / FTPS", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                        Switch(
                            checked = ftpEnabled,
                            onCheckedChange = {
                                ftpEnabled = it
                                viewModel.toggleUsbService("ftp", it)
                                statusMessage = if (it) "FTP-сервер запущен" else "FTP-сервер остановлен"
                            }
                        )
                    }
                }
            }
        }
    }

    // Drive Management Dialog
    selectedDevice?.let { dev ->
        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = KeeneticColors.Primary)
                    Text(dev.label.ifBlank { "Накопитель" }, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Производитель: ${dev.vendor} ${dev.model}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                    Text("Файловая система: ${dev.filesystem}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                    Text("Точка монтирования: ${dev.mountPoint}", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                    Text("Объем: ${String.format("%.1f", dev.sizeBytes / (1024.0 * 1024 * 1024))} ГБ", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                    Text("Свободно: ${String.format("%.1f", dev.freeBytes / (1024.0 * 1024 * 1024))} ГБ", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.Success)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.ejectUsbDevice(dev.name)
                        statusMessage = "Команда безопасного извлечения «${dev.label}» отправлена"
                        selectedDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error)
                ) {
                    Icon(Icons.Default.Eject, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Безопасно извлечь")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDevice = null }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}
