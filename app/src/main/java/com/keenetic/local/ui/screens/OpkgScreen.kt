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
import com.keenetic.local.api.MediaPartition
import com.keenetic.local.api.MediaStorage
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private data class OpkgDiskOption(val id: String, val label: String)

/**
 * Web UI «Менеджер пакетов OPKG»: выбор диска для пакетов и файла конфигурации (initrc).
 * Paths: read show/sc/opkg, write {"opkg":{"disk":{"disk","no"},"initrc":{"path","no"}}}.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpkgScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val config by viewModel.opkgConfig.collectAsState()
    val media by viewModel.mediaStorageList.collectAsState()

    var selectedDiskId by remember { mutableStateOf("") }
    var initrcEnabled by remember { mutableStateOf(true) }
    var initrcPath by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.SdCard, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Менеджер пакетов OPKG",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SdCard, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Накопитель для пакетов", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    var expanded by remember { mutableStateOf(false) }
                    val current = diskOptions.firstOrNull { it.id == selectedDiskId } ?: diskOptions[0]
                    if (diskOptions.size > 1) {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = current.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Диск") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                diskOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, color = KeeneticColors.TextPrimary) },
                                        onClick = {
                                            selectedDiskId = option.id
                                            saved = false
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Доступные диски не найдены. Подключите USB-накопитель.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                    Text(
                        config.diskId.ifBlank { "Диск не выбран" },
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Файл конфигурации (initrc)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Включить", color = KeeneticColors.TextPrimary)
                            Text(
                                "Скрипт выполняется при загрузке системы",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = initrcEnabled,
                            onCheckedChange = {
                                initrcEnabled = it
                                if (it && initrcPath.isBlank()) initrcPath = "/kmod.rc"
                                if (!it) initrcPath = ""
                                saved = false
                            }
                        )
                    }

                    if (initrcEnabled) {
                        OutlinedTextField(
                            value = initrcPath,
                            onValueChange = {
                                initrcPath = it
                                saved = false
                            },
                            label = { Text("Путь к файлу") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
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