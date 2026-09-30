package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
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
    LaunchedEffect(Unit) { viewModel.loadInterfaces() }
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

    var pickerFor by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    SectionScaffold(
        title = "Приоритеты подключений",
        subtitle = "Порядок интернет-каналов: чем меньше значение, тем выше приоритет",
        onBack = onBack,
        onRefresh = { viewModel.loadInterfaces() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                SectionCard(
                    title = "Как это работает",
                    icon = Icons.Default.List
                ) {
                    InfoRow(
                        label = "Авто",
                        value = "Роутер сам выбирает порядок"
                    )
                }
            }

            if (feedbackMessage != null) {
                item {
                    SectionCard(title = "Статус") {
                        InfoRow(label = "Результат", value = feedbackMessage ?: "")
                    }
                }
            }

            if (internetInterfaces.isEmpty()) {
                item {
                    SectionCard(title = "Подключения") {
                        EmptyHint(text = "Интернет-интерфейсы не найдены. Обновите список.")
                    }
                }
            } else {
                item {
                    SectionCard(title = "Подключения", subtitle = "Изменения отправляются на роутер кнопкой ниже") {
                        SubGroupHeader(title = "Интерфейсы", count = internetInterfaces.size)
                    }
                }
                items(internetInterfaces, key = { it.id }) { itf ->
                    val order = priorities[itf.id]?.toIntOrNull()
                    val label = when (order) {
                        -1 -> "Авто"
                        0 -> "0"
                        1 -> "1"
                        2 -> "2"
                        else -> order?.toString() ?: "Авто"
                    }
                    SectionCard(title = itf.name, icon = Icons.Default.List, subtitle = itf.type) {
                        EditableRow(
                            label = "Приоритет",
                            value = label,
                            onClick = { pickerFor = itf.id }
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Сохранение") {
                    Button(
                        onClick = {
                            val changed = priorities.mapNotNull { (id, order) ->
                                val newVal = order.toIntOrNull()
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
    }

    pickerFor?.let { id ->
        val current = priorities[id]?.toIntOrNull()?.toString() ?: "-1"
        OptionPickerDialog(
            title = "Приоритет",
            options = PRIORITY_OPTIONS.map { (v, l) -> v.toString() to l },
            selectedKey = current,
            onSelect = { key -> priorities = priorities + (id to key) },
            onDismiss = { pickerFor = null }
        )
    }
}
