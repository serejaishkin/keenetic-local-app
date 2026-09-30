package com.keenetic.local.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.FileAclEntry
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
import com.keenetic.local.ui.theme.KeeneticColors

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.0f КБ", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f МБ", mb)
    return String.format("%.2f ГБ", mb / 1024.0)
}

private val ACL_MODES = listOf("read/write", "read", "none")

@Composable
fun FileBrowserScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val path by viewModel.fileBrowserPath.collectAsState()
    val entries by viewModel.fileBrowserEntries.collectAsState()
    val loading by viewModel.fileBrowserLoading.collectAsState()
    val error by viewModel.fileBrowserError.collectAsState()
    val message by viewModel.fileBrowserMessage.collectAsState()
    val aclList by viewModel.fileAclList.collectAsState()
    val context = LocalContext.current

    var showMkdir by remember { mutableStateOf(false) }
    var mkdirName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var aclTarget by remember { mutableStateOf<String?>(null) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadFileEntry(path, uri, context)
    }

    LaunchedEffect(aclTarget) {
        aclTarget?.let { viewModel.loadFileAcl(it) }
    }

    SectionScaffold(
        title = "Файлы",
        subtitle = if (path.isBlank()) "/" else path,
        onBack = onBack,
        onRefresh = { viewModel.browseFiles(path) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(
                    title = "Текущая папка",
                    icon = Icons.Default.Folder,
                    subtitle = if (path.isBlank()) "/" else path
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (path.isNotBlank()) {
                            IconButton(onClick = { viewModel.fileBrowserUp() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", tint = KeeneticColors.Primary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        InfoRow(label = "Путь", value = if (path.isBlank()) "/" else path, monospace = true)
                        if (loading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    RowDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMkdir = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Папка")
                        }
                        OutlinedButton(
                            onClick = { pickFile.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Загрузить")
                        }
                    }
                }
            }

            if (message != null) {
                item {
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.clearFileBrowserMessage() }) {
                                Text("OK", color = KeeneticColors.Primary)
                            }
                        }
                    ) {
                        Text(message ?: "")
                    }
                }
            }

            if (error != null && entries.isEmpty() && !loading) {
                item {
                    SectionCard(title = "Содержимое", icon = Icons.Default.Folder) {
                        EmptyHint(text = error ?: "Ошибка")
                        Button(onClick = { viewModel.browseFiles(path) }) {
                            Text("Повторить")
                        }
                    }
                }
            }

            if (entries.isEmpty() && !loading && error == null) {
                item {
                    SectionCard(title = "Содержимое", icon = Icons.Default.Folder) {
                        EmptyHint(text = "Папка пуста")
                    }
                }
            }

            if (entries.isNotEmpty()) {
                item {
                    SectionCard(title = "Содержимое", icon = Icons.Default.Folder, subtitle = "Элементов: ${entries.size}") {
                        EmptyHint(text = "Папка — открыть, файл — скачать.")
                    }
                }
            }

            items(entries, key = { it.fullPath }) { e ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (e.isDirectory) viewModel.browseFiles(e.fullPath)
                        else viewModel.downloadFileEntry(e, context)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when {
                                e.isVolume -> Icons.Default.Storage
                                e.isDirectory -> Icons.Default.Folder
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = if (e.isDirectory) KeeneticColors.Primary else KeeneticColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (e.label.isNotBlank() && e.isVolume) "${e.label} (${e.name})" else e.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = KeeneticColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = when {
                                e.isVolume && e.totalBytes > 0 ->
                                    "${e.fstype} • свободно ${formatSize(e.freeBytes)} из ${formatSize(e.totalBytes)}"
                                e.isVolume && e.fstype.isNotBlank() -> e.fstype
                                !e.isDirectory && e.sizeBytes > 0 -> formatSize(e.sizeBytes)
                                e.isDirectory -> "Папка"
                                else -> ""
                            }
                            if (sub.isNotBlank()) {
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                        if (!e.isVolume) {
                            IconButton(onClick = { aclTarget = e.fullPath }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Права", tint = KeeneticColors.TextSecondary)
                            }
                        }
                        if (!e.isVolume) {
                            IconButton(onClick = { deleteTarget = e.fullPath }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeeneticColors.Error)
                            }
                        }
                        if (e.isDirectory && !e.isVolume) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showMkdir) {
        FormDialog(
            title = "Новая папка",
            confirmLabel = "Создать",
            onDismiss = { showMkdir = false },
            onConfirm = {
                if (mkdirName.isNotBlank()) {
                    viewModel.createFolder(path, mkdirName.trim())
                    mkdirName = ""
                    showMkdir = false
                }
            },
            confirmEnabled = mkdirName.isNotBlank()
        ) {
            OutlinedTextField(
                value = mkdirName,
                onValueChange = { mkdirName = it },
                label = { Text("Имя папки") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Удалить?",
            message = target,
            confirmLabel = "Удалить",
            onConfirm = {
                viewModel.deleteFileEntry(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    aclTarget?.let { target ->
        AclDialog(
            path = target,
            aclList = aclList,
            onDismiss = { aclTarget = null },
            onSave = { modes ->
                viewModel.saveFileAcl(target, modes)
                aclTarget = null
            }
        )
    }
}

@Composable
private fun AclDialog(
    path: String,
    aclList: List<FileAclEntry>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val modes = remember(aclList) {
        mutableStateMapOf(*aclList.map { it.user to it.assigned.ifBlank { it.effective } }.toTypedArray())
    }
    var pickerUser by remember { mutableStateOf<String?>(null) }
    FormDialog(
        title = "Права доступа",
        confirmLabel = "Сохранить",
        onDismiss = onDismiss,
        onConfirm = { onSave(modes.toMap()) },
        confirmEnabled = aclList.isNotEmpty()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow(label = "Путь", value = path, monospace = true)
            RowDivider()
            if (aclList.isEmpty()) {
                EmptyHint(text = "Загрузка...")
            }
            aclList.forEach { entry ->
                val current = modes[entry.user] ?: entry.effective
                EditableRow(
                    label = entry.user,
                    value = current.ifBlank { "—" },
                    hint = "действует: ${entry.effective}",
                    onClick = { pickerUser = entry.user },
                    monospaceValue = true
                )
            }
        }
    }
    pickerUser?.let { user ->
        val current = modes[user] ?: ""
        OptionPickerDialog(
            title = user,
            options = ACL_MODES.map { it to it },
            selectedKey = current,
            onSelect = { modes[user] = it },
            onDismiss = { pickerUser = null }
        )
    }
}
