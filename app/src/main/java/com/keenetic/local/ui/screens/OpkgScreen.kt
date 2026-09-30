package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.MediaPartition
import com.keenetic.local.api.MediaStorage
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private data class OpkgDiskOption(val id: String, val label: String)

/**
 * Web UI «Менеджер пакетов OPKG»: выбор диска для пакетов и файла конфигурации (initrc).
 * Paths: read show/sc/opkg, write {"opkg":{"disk":{"disk","no"},"initrc":{"path","no"}}}.
 */
@Composable
fun OpkgScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val config by viewModel.opkgConfig.collectAsState()
    val media by viewModel.mediaStorageList.collectAsState()

    var selectedDiskId by remember { mutableStateOf("") }
    var initrcEnabled by remember { mutableStateOf(true) }
    var initrcPath by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var showDiskPicker by remember { mutableStateOf(false) }
    var showInitrcDialog by remember { mutableStateOf(false) }
    var draftInitrcPath by remember { mutableStateOf("") }

    val diskOptions = remember(media) { buildDiskOptions(media) }

    // Sync local form once the remote config arrives.
    LaunchedEffect(config) {
        if (saved) return@LaunchedEffect
        selectedDiskId = config.diskId
        initrcPath = config.initrcPath
        initrcEnabled = !config.initrcNo && config.initrcPath.isNotBlank()
    }
    LaunchedEffect(Unit) {
        viewModel.loadOpkgConfig()
        viewModel.loadMediaStorage()
    }

    val currentLabel = diskOptions.firstOrNull { it.id == selectedDiskId }?.label
        ?: config.diskId.ifBlank { "Диск не выбран" }

    SectionScaffold(
        title = "Менеджер пакетов OPKG",
        subtitle = "Накопитель и initrc",
        onBack = onBack,
        onRefresh = {
            viewModel.loadOpkgConfig()
            viewModel.loadMediaStorage()
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "disk") {
                SectionCard(
                    title = "Накопитель для пакетов",
                    icon = Icons.Default.SdCard
                ) {
                    if (diskOptions.size > 1) {
                        EditableRow(
                            label = "Диск",
                            value = currentLabel,
                            onClick = { showDiskPicker = true },
                            monospaceValue = true
                        )
                    } else {
                        EmptyHint("Доступные диски не найдены. Подключите USB-накопитель.")
                    }
                    InfoRow("Текущий диск", config.diskId.ifBlank { "Диск не выбран" }, monospace = true)
                }
            }

            item(key = "initrc") {
                SectionCard(
                    title = "Файл конфигурации (initrc)",
                    subtitle = "Скрипт выполняется при загрузке системы",
                    icon = Icons.Default.Storage
                ) {
                    SwitchRow(
                        label = "Включить",
                        checked = initrcEnabled,
                        onCheckedChange = {
                            initrcEnabled = it
                            if (it && initrcPath.isBlank()) initrcPath = "/kmod.rc"
                            if (!it) initrcPath = ""
                            saved = false
                        }
                    )
                    if (initrcEnabled) {
                        EditableRow(
                            label = "Путь к файлу",
                            value = initrcPath.ifBlank { "Не задан" },
                            onClick = {
                                draftInitrcPath = initrcPath
                                showInitrcDialog = true
                            },
                            monospaceValue = true
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.saveOpkgConfig(selectedDiskId, if (initrcEnabled) initrcPath else "")
                            saved = true
                        },
                        enabled = initrcEnabled || selectedDiskId.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }

    if (showDiskPicker) {
        OptionPickerDialog(
            title = "Диск для пакетов",
            options = diskOptions.map { it.id to it.label },
            selectedKey = selectedDiskId,
            onSelect = {
                selectedDiskId = it
                saved = false
            },
            onDismiss = { showDiskPicker = false }
        )
    }

    if (showInitrcDialog) {
        FormDialog(
            title = "Путь к файлу initrc",
            onDismiss = { showInitrcDialog = false },
            onConfirm = {
                initrcPath = draftInitrcPath
                saved = false
                showInitrcDialog = false
            }
        ) {
            OutlinedTextField(
                value = draftInitrcPath,
                onValueChange = { draftInitrcPath = it },
                label = { Text("Путь к файлу") },
                supportingText = { Text("Например /kmod.rc", color = KeeneticColors.TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun buildDiskOptions(media: List<MediaStorage>): List<OpkgDiskOption> {
    val options = linkedMapOf<String, OpkgDiskOption>()
    options[""] = OpkgDiskOption("", "Не выбрано")
    options["storage:/"] = OpkgDiskOption("storage:/", "Внутренняя память")
    val partitions = media.flatMap { it.partitions }
    for (p in partitions) {
        val id = partitionId(p)
        if (id.isBlank()) continue
        if (p.fstype.equals("swap", ignoreCase = true)) continue
        if (!p.state.equals("mounted", ignoreCase = true)) continue
        val text = p.label.ifBlank { p.uuid }
        val label = if (p.label.isNotBlank()) "$text (${p.uuid})" else text
        options[id] = OpkgDiskOption(id, label)
    }
    return options.values.toList()
}

private fun partitionId(p: MediaPartition): String {
    val base = p.label.ifBlank { p.uuid }
    return if (base.isBlank()) "" else "$base:/"
}
