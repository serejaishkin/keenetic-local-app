package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun SystemLogsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val logs by viewModel.systemLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSystemLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Terminal, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Журнал событий (Логи)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadSystemLogs() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        if (logs.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = KeeneticColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Записи журнала загружаются...",
                        style = MaterialTheme.typography.titleMedium,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Запрос системного журнала ndm/syslog из KeeneticOS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                    Button(
                        onClick = { viewModel.loadSystemLogs() },
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Запросить логи", color = KeeneticColors.Primary)
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = KeeneticColors.TerminalBg
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val color = when (log.level.lowercase()) {
                            "err", "error" -> KeeneticColors.Error
                            "warn", "warning" -> KeeneticColors.Warning
                            else -> KeeneticColors.TerminalText
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (log.timestamp.isNotBlank()) {
                                Text(
                                    log.timestamp,
                                    color = KeeneticColors.TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                "[${log.facility}] ${log.message}",
                                color = color,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
