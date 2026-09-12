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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@Composable
fun FileBrowserScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val path by viewModel.fileBrowserPath.collectAsState()
    val entries by viewModel.fileBrowserEntries.collectAsState()
    val loading by viewModel.fileBrowserLoading.collectAsState()
    val error by viewModel.fileBrowserError.collectAsState()

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
                modifier = Modifier.fillMaxWidth().clickable(enabled = e.isDirectory) {
                    viewModel.browseFiles(e.fullPath)
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
                    if (e.isDirectory) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KeeneticColors.TextSecondary)
                    }
                }
            }
        }
    }
}
