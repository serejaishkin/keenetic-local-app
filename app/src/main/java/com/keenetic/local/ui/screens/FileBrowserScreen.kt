package com.keenetic.local.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.FileAclEntry
import com.keenetic.local.ui.RouterViewModel
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Folder, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Файлы",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showMkdir = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Новая папка", tint = KeeneticColors.Primary)
                }
                IconButton(onClick = { pickFile.launch("*/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = "Загрузить", tint = KeeneticColors.Primary)
                }
                IconButton(onClick = { viewModel.browseFiles(path) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (path.isNotBlank()) {
                        IconButton(onClick = { viewModel.fileBrowserUp() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", tint = KeeneticColors.Primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (path.isBlank()) "/" else path,
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (loading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = KeeneticColors.TextSecondary, modifier = Modifier.size(40.dp))
                        Text(error ?: "", color = KeeneticColors.TextSecondary)
                        Button(onClick = { viewModel.browseFiles(path) }) {
                            Text("Повторить")
                        }
                    }
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

    if (showMkdir) {
        AlertDialog(
            onDismissRequest = { showMkdir = false },
            title = { Text("Новая папка", color = KeeneticColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = mkdirName,
                    onValueChange = { mkdirName = it },
                    label = { Text("Имя папки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mkdirName.isNotBlank()) {
                            viewModel.createFolder(path, mkdirName.trim())
                            mkdirName = ""
                            showMkdir = false
                        }
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showMkdir = false }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить?", color = KeeneticColors.TextPrimary) },
            text = { Text(target, color = KeeneticColors.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFileEntry(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Права доступа", color = KeeneticColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(path, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                if (aclList.isEmpty()) {
                    Text("Загрузка...", color = KeeneticColors.TextSecondary)
                }
                aclList.forEach { entry ->
                    var expanded by remember { mutableStateOf(false) }
                    val current = modes[entry.user] ?: entry.effective
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.user, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                            Text(
                                "действует: ${entry.effective}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(current.ifBlank { "—" }, color = KeeneticColors.Primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                ACL_MODES.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode) },
                                        onClick = {
                                            modes[entry.user] = mode
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(modes.toMap()) }, enabled = aclList.isNotEmpty()) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = KeeneticColors.TextSecondary)
            }
        }
    )
}
