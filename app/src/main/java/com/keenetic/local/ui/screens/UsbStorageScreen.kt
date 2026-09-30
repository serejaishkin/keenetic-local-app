package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.UsbStorageDevice
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun UsbStorageScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}, onOpenFiles: (String) -> Unit = {}) {
    val usbDevices by viewModel.usbStorageList.collectAsState()
    val serviceFlags by viewModel.usbServiceFlags.collectAsState()
    var selectedDevice by remember { mutableStateOf<UsbStorageDevice?>(null) }
    var ejectTarget by remember { mutableStateOf<UsbStorageDevice?>(null) }
    var formatTarget by remember { mutableStateOf<UsbStorageDevice?>(null) }
    var formatFstype by remember { mutableStateOf("") }
    var formatArmed by remember { mutableStateOf(false) }
    var showFstypePicker by remember { mutableStateOf(false) }
    // Состояние свитчей — только из загруженного usbServiceFlags, без хардкода.
    var smbEnabled by remember(serviceFlags) { mutableStateOf(serviceFlags?.cifs ?: false) }
    var dlnaEnabled by remember(serviceFlags) { mutableStateOf(serviceFlags?.dlna ?: false) }
    var ftpEnabled by remember(serviceFlags) { mutableStateOf(serviceFlags?.ftp ?: false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsbDevices()
    }

    SectionScaffold(
        title = "USB и накопители",
        subtitle = if (usbDevices.isEmpty()) null else "Устройств: ${usbDevices.size}",
        onBack = onBack,
        onRefresh = { viewModel.loadUsbDevices() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            if (usbDevices.isEmpty()) {
                item {
                    SectionCard(title = "Подключенные накопители", icon = Icons.Default.Usb) {
                        EmptyHint(text = "USB-накопители не подключены. Подключите диск или флешку к USB-порту Keenetic для общего сетевого диска (SMB), медиасервера или торрент-клиента.")
                        Button(onClick = { viewModel.loadUsbDevices() }) {
                            Text("Проверить USB-порты")
                        }
                    }
                }
            } else {
                item {
                    SectionCard(title = "Подключенные накопители", icon = Icons.Default.Usb, subtitle = "Устройств: ${usbDevices.size}") {
                        EmptyHint(text = "Нажмите на накопитель для подробностей.")
                    }
                }
                items(usbDevices, key = { it.name }) { dev ->
                    val usedBytes = (dev.sizeBytes - dev.freeBytes).coerceAtLeast(0L)
                    val usedPercent = if (dev.sizeBytes > 0) (usedBytes.toFloat() / dev.sizeBytes).coerceIn(0f, 1f) else 0f
                    SectionCard(
                        title = dev.label.ifBlank { dev.name },
                        icon = Icons.Default.Usb,
                        subtitle = "${dev.vendor} ${dev.model} • ${dev.filesystem}".trim(),
                        trailing = {
                            Row {
                                IconButton(onClick = { ejectTarget = dev }) {
                                    Icon(Icons.Default.Eject, contentDescription = "Извлечь", tint = KeeneticColors.Error)
                                }
                                if (dev.uuid.isNotBlank()) {
                                    IconButton(onClick = { onOpenFiles("${dev.uuid}:") }) {
                                        Icon(Icons.Default.Folder, contentDescription = "Файлы", tint = KeeneticColors.Primary)
                                    }
                                }
                            }
                        }
                    ) {
                        LinearProgressIndicator(
                            progress = { usedPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = KeeneticColors.Primary,
                            trackColor = KeeneticColors.Divider
                        )
                        val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024 * 1024))
                        val totalGb = String.format("%.1f", dev.sizeBytes / (1024.0 * 1024 * 1024))
                        InfoRow(label = "Занято", value = "$usedGb ГБ из $totalGb ГБ (${(usedPercent * 100).toInt()}%)")
                        if (dev.mountPoint.isNotBlank()) {
                            RowDivider()
                            InfoRow(label = "Точка монтирования", value = dev.mountPoint, monospace = true)
                        }
                        RowDivider()
                        EditableRow(
                            label = "Управление",
                            value = "Подробности",
                            onClick = { selectedDevice = dev }
                        )
                        if (dev.formatOptions.isNotEmpty()) {
                            RowDivider()
                            EditableRow(
                                label = "Форматирование",
                                value = "Выбрать ФС и отформатировать",
                                onClick = {
                                    formatFstype = ""
                                    formatArmed = false
                                    formatTarget = dev
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "Сетевые службы USB",
                    icon = Icons.Default.FolderShared,
                    subtitle = if (serviceFlags == null) "Состояние неизвестно" else null
                ) {
                    if (serviceFlags == null) {
                        EmptyHint(text = "Не удалось прочитать состояние служб с роутера (service.cifs/ftp/dlna) — переключатели заблокированы, чтобы не показать неверное состояние.")
                    }
                    SwitchRow(
                        label = "Сеть Windows (SMB / CIFS)",
                        checked = if (serviceFlags != null) smbEnabled else false,
                        onCheckedChange = {
                            smbEnabled = it
                            viewModel.toggleUsbService("cifs", it)
                            statusMessage = if (it) "Служба SMB включена" else "Служба SMB отключена"
                        },
                        description = "Общий доступ к файлам для ПК, ноутбуков и ТВ",
                        enabled = serviceFlags != null,
                        unknown = serviceFlags == null
                    )
                    RowDivider()
                    SwitchRow(
                        label = "Медиасервер DLNA",
                        checked = if (serviceFlags != null) dlnaEnabled else false,
                        onCheckedChange = {
                            dlnaEnabled = it
                            viewModel.toggleUsbService("dlna", it)
                            statusMessage = if (it) "Медиасервер DLNA включен" else "DLNA отключен"
                        },
                        description = "Потоковое воспроизведение видео и музыки на Smart TV",
                        enabled = serviceFlags != null,
                        unknown = serviceFlags == null
                    )
                    RowDivider()
                    SwitchRow(
                        label = "FTP-сервер",
                        checked = if (serviceFlags != null) ftpEnabled else false,
                        onCheckedChange = {
                            ftpEnabled = it
                            viewModel.toggleUsbService("ftp", it)
                            statusMessage = if (it) "FTP-сервер запущен" else "FTP-сервер остановлен"
                        },
                        description = "Доступ к файлам по протоколу FTP / FTPS",
                        enabled = serviceFlags != null,
                        unknown = serviceFlags == null
                    )
                }
            }
        }
    }

    // Подтверждение безопасного извлечения
    ejectTarget?.let { dev ->
        ConfirmDialog(
            title = "Извлечь накопитель?",
            message = "«${dev.label.ifBlank { dev.name }}» будет безопасно отключён.",
            confirmLabel = "Извлечь",
            onConfirm = {
                viewModel.ejectUsbDevice(dev.name)
                statusMessage = "Команда безопасного извлечения «${dev.label}» отправлена"
                ejectTarget = null
            },
            onDismiss = { ejectTarget = null }
        )
    }

    // Детали накопителя
    selectedDevice?.let { dev ->
        val details = "Производитель: ${dev.vendor} ${dev.model}\n" +
            "Файловая система: ${dev.filesystem}\n" +
            "Точка монтирования: ${dev.mountPoint}\n" +
            "Объем: ${String.format("%.1f", dev.sizeBytes / (1024.0 * 1024 * 1024))} ГБ\n" +
            "Свободно: ${String.format("%.1f", dev.freeBytes / (1024.0 * 1024 * 1024))} ГБ"
        ConfirmDialog(
            title = dev.label.ifBlank { "Накопитель" },
            message = details,
            confirmLabel = "Безопасно извлечь",
            onConfirm = {
                viewModel.ejectUsbDevice(dev.name)
                statusMessage = "Команда безопасного извлечения «${dev.label}» отправлена"
                selectedDevice = null
            },
            onDismiss = { selectedDevice = null }
        )
    }

    // Форматирование (двойное подтверждение, выбор ФС через OptionPicker)
    formatTarget?.let { dev ->
        if (formatFstype.isBlank() && dev.formatOptions.isNotEmpty()) {
            formatFstype = dev.formatOptions.firstOrNull { it == dev.filesystem } ?: dev.formatOptions.first()
        }
        FormDialog(
            title = "Форматировать раздел?",
            confirmLabel = if (!formatArmed) "Продолжить" else "ФОРМАТИРОВАТЬ",
            onDismiss = { formatTarget = null; formatArmed = false; formatFstype = "" },
            onConfirm = {
                if (!formatArmed) {
                    formatArmed = true
                } else {
                    viewModel.formatPartition(dev.name, dev.partitionId, formatFstype)
                    formatTarget = null; formatArmed = false; formatFstype = ""
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "«${dev.label}» (${dev.filesystem}, ${String.format("%.1f", dev.sizeBytes / (1024.0 * 1024 * 1024))} ГБ). ВСЕ ДАННЫЕ БУДУТ УДАЛЕНЫ!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KeeneticColors.Error
                )
                EditableRow(
                    label = "Файловая система",
                    value = formatFstype.ifBlank { "—" },
                    onClick = { showFstypePicker = true },
                    monospaceValue = true
                )
                Text(
                    if (!formatArmed) "Нажмите «Продолжить», затем подтвердите ещё раз."
                    else "ПОДТВЕРДИТЕ ОКОНЧАТЕЛЬНО: раздел будет отформатирован в $formatFstype.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!formatArmed) KeeneticColors.TextSecondary else KeeneticColors.Error,
                    fontWeight = if (!formatArmed) FontWeight.Normal else FontWeight.Bold
                )
            }
        }
        if (showFstypePicker) {
            OptionPickerDialog(
                title = "Файловая система",
                options = dev.formatOptions.map { it to it },
                selectedKey = formatFstype,
                onSelect = { formatFstype = it },
                onDismiss = { showFstypePicker = false }
            )
        }
    }
}
