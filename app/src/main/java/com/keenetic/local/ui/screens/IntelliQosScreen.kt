package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.keenetic.local.api.intelliQosPriorityName
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val PRIORITY_LEVELS = (1..7).toList()

@Composable
fun IntelliQosScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val cfg by viewModel.intelliQos.collectAsState()
    var classify by remember { mutableStateOf(false) }
    var prioritize by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pickerFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadIntelliQos()
    }
    LaunchedEffect(cfg) {
        classify = cfg.classifyEnabled
        prioritize = cfg.qosEnabled
    }

    val sorted = cfg.categories.sortedBy { it.priority }

    SectionScaffold(
        title = "IntelliQoS",
        subtitle = "Интеллектуальная классификация и приоритизация трафика",
        onBack = onBack,
        onRefresh = { viewModel.loadIntelliQos() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (message != null) {
                item {
                    SectionCard(title = "Статус") {
                        InfoRow(label = "Результат", value = message ?: "")
                    }
                }
            }

            item {
                SectionCard(
                    title = "Классификация и приоритизация приложений",
                    icon = Icons.Default.Speed,
                    subtitle = "Требуется установленный компонент NTCE. При включении аппаратный сетевой ускоритель (PACT) отключается."
                ) {
                    SwitchRow(
                        label = "Распознавание приложений",
                        checked = classify,
                        onCheckedChange = { classify = it }
                    )
                    SwitchRow(
                        label = "Приоритизация приложений",
                        checked = prioritize,
                        onCheckedChange = { prioritize = it }
                    )
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

            if (sorted.isNotEmpty()) {
                item {
                    SectionCard(title = "Группы категорий") {
                        SubGroupHeader(title = "Категории", count = sorted.size)
                        InfoRow(
                            label = "Приоритеты",
                            value = sorted.joinToString(", ") { "${it.name}: ${intelliQosPriorityName(it.priority)}" }
                        )
                    }
                }
                items(sorted, key = { it.id }) { cat ->
                    SectionCard(title = cat.name, subtitle = intelliQosPriorityName(cat.priority)) {
                        EditableRow(
                            label = "Приоритет",
                            // категории IntelliQoS — строки (например "calling"), модель не меняем
                            value = "${cat.priority} · ${intelliQosPriorityName(cat.priority)}",
                            onClick = { pickerFor = cat.id }
                        )
                    }
                }
            } else {
                item {
                    SectionCard(title = "Группы категорий") {
                        EmptyHint(text = "Данные категорий не загружены. Обновите или проверьте установленный компонент NTCE.")
                    }
                }
            }
        }
    }

    pickerFor?.let { categoryId ->
        val cat = sorted.find { it.id == categoryId }
        if (cat != null) {
            OptionPickerDialog(
                title = "Приоритет «${cat.name}»",
                options = PRIORITY_LEVELS.map { p -> p.toString() to "$p · ${intelliQosPriorityName(p)}" },
                selectedKey = cat.priority.toString(),
                onSelect = { key ->
                    val p = key.toIntOrNull() ?: return@OptionPickerDialog
                    viewModel.setIntelliQosPriority(categoryId, p)
                    message = "Приоритет «${cat.name}» = ${intelliQosPriorityName(p)}"
                },
                onDismiss = { pickerFor = null }
            )
        }
    }
}
