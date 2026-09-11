package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.intelliQosCategoryName
import com.keenetic.local.api.intelliQosPriorityName
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

private val PRIORITY_LEVELS = (1..7).toList()

@Composable
fun IntelliQosScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val cfg by viewModel.intelliQos.collectAsState()
    var classify by remember { mutableStateOf(false) }
    var prioritize by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadIntelliQos()
    }
    LaunchedEffect(cfg) {
        classify = cfg.classifyEnabled
        prioritize = cfg.qosEnabled
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Speed, contentDescription = null, tint = KeeneticColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "IntelliQoS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Интеллектуальная классификация и приоритизация трафика",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadIntelliQos() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
            }
        }

        message?.let {
            Snackbar(
                action = {
                    TextButton(onClick = { message = null }) {
                        Text("OK", color = KeeneticColors.Primary)
                    }
                }
            ) { Text(it) }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Классификация и приоритизация приложений",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Требуется установленный компонент NTCE. При включении аппаратный сетевой ускоритель (PACT) отключается.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                ToggleRow("Распознавание приложений", classify) { classify = it }
                ToggleRow("Приоритизация приложений", prioritize) { prioritize = it }
                Button(
                    onClick = {
                        viewModel.setIntelliQos(classify, prioritize)
                        message = "Настройки IntelliQoS отправлены на роутер"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
            }
        }

        val sorted = cfg.categories.sortedBy { it.priority }
        if (sorted.isNotEmpty()) {
            Text("Группы категорий", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            Text(
                "Приоритеты " + sorted.joinToString(", ") { "${it.name}: ${intelliQosPriorityName(it.priority)}" },
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
            sorted.forEach { cat ->
                CategoryRow(viewModel, cat.id, cat.name, cat.priority) { msg -> message = msg }
            }
        } else {
            Text(
                "Данные категорий не загружены. Обновите или проверьте установленный компонент NTCE.",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryRow(
    viewModel: RouterViewModel,
    categoryId: Int,
    name: String,
    currentPriority: Int,
    onDone: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var localPriority by remember(categoryId, currentPriority) { mutableStateOf(currentPriority) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                Text(intelliQosPriorityName(localPriority), style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(localPriority.toString(), color = KeeneticColors.TextPrimary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
                ) {
                    PRIORITY_LEVELS.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(intelliQosPriorityName(p)) },
                            onClick = {
                                localPriority = p
                                expanded = false
                                viewModel.setIntelliQosPriority(categoryId, p)
                                onDone("Приоритет «${intelliQosCategoryName(categoryId)}» = ${intelliQosPriorityName(p)}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}