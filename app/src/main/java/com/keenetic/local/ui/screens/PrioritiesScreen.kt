package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterInterface
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private val PRIORITY_OPTIONS = listOf(
    -1 to "Авто (по умолчанию)",
    0 to "0 — основной",
    1 to "1 — резервный",
    2 to "2 — запасной"
)

@Composable
fun PrioritiesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val internetInterfaces = remember(interfaces) {
        interfaces.filter { itf ->
            val t = itf.type.lowercase()
            t != "bridge" && t != "accesspoint" && t != "wifistation" && t != "port"
        }
    }

    // local copy of priorities: interface id -> order string
    var priorities by remember {
        mutableStateOf(internetInterfaces.associate { it.id to it.order.toString() })
    }
    LaunchedEffect(interfaces) {
        priorities = internetInterfaces.associate { it.id to it.order.toString() }
    }

    var expandedFor by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Icon(Icons.Default.List, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Приоритеты подключений",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadInterfaces() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }
        }

        item {
            Text(
                "Порядок использования интернет-каналов: чем меньше значение, тем выше приоритет. Авто — роутер сам выбирает порядок. Изменения отправляются на роутер.",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
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

        items(internetInterfaces.size) { index ->
            val itf = internetInterfaces[index]
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(itf.name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                        Text(itf.type, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                    }
                    Box {
                        OutlinedButton(onClick = { expandedFor = itf.id }) {
                            val order = priorities[itf.id]?.toIntOrNull()
                            val label = when (order) {
                                -1 -> "Авто"
                                0 -> "0"
                                1 -> "1"
                                2 -> "2"
                                else -> order?.toString() ?: "Авто"
                            }
                            Text(label, color = KeeneticColors.TextPrimary)
                        }
                        DropdownMenu(
                            expanded = expandedFor == itf.id,
                            onDismissRequest = { expandedFor = null },
                            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
                        ) {
                            PRIORITY_OPTIONS.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        priorities = priorities + (itf.id to value.toString())
                                        expandedFor = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val changed = priorities.mapNotNull { (id, order) ->
                        val newVal = order.toIntOrNull()
                        val current = internetInterfaces.find { it.id == id }?.order
                        if (newVal != null) id to newVal else null
                    }.toMap()
                    viewModel.saveInterfacePriorities(changed)
                    feedbackMessage = "Приоритеты подключений отправлены на роутер"
                },
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить приоритеты")
            }
        }
    }
}