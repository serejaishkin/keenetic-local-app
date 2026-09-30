package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ComponentInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ComponentsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val componentList by viewModel.componentList.collectAsState()
    var selectedComponent by remember { mutableStateOf<ComponentInfo?>(null) }
    var pendingDelete by remember { mutableStateOf<ComponentInfo?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadComponents()
    }

    SectionScaffold(
        title = "Компоненты системы",
        subtitle = "Пакеты KeeneticOS",
        onBack = onBack,
        onRefresh = { viewModel.loadComponents() }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (feedbackMessage != null) {
                item(key = "feedback") {
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

            item(key = "list-header") {
                SubGroupHeader("Компоненты", componentList.size)
            }

            if (componentList.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint("Нет данных")
                    }
                }
            } else {
                items(componentList, key = { it.name.ifBlank { it.title } }) { component ->
                    ComponentCard(component = component, onClick = { selectedComponent = component })
                }
            }
        }
    }

    selectedComponent?.let { comp ->
        FormDialog(
            title = comp.title.ifBlank { comp.name },
            confirmLabel = "Закрыть",
            onDismiss = { selectedComponent = null },
            onConfirm = { selectedComponent = null }
        ) {
            Text(
                comp.description.ifBlank { "Системный компонент KeeneticOS" },
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextSecondary
            )
            RowDivider()
            InfoRow("Имя пакета", comp.name, monospace = true)
            InfoRow("Версия ПО", comp.version.ifBlank { "Н/Д" })
            InfoRow(
                "Статус установки",
                if (comp.installed) "Установлен" else if (comp.available) "Доступен к установке" else "Недоступен",
                valueColor = when {
                    comp.installed -> KeeneticColors.Success
                    comp.available -> KeeneticColors.Warning
                    else -> KeeneticColors.TextSecondary
                }
            )
            RowDivider()
            if (comp.installed) {
                Button(
                    onClick = { pendingDelete = comp },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить компонент")
                }
            } else if (comp.available) {
                Button(
                    onClick = {
                        viewModel.installComponent(comp.name)
                        feedbackMessage = "Отправлена команда на установку компонента «${comp.name}»"
                        selectedComponent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Установить компонент")
                }
            }
        }
    }

    pendingDelete?.let { comp ->
        ConfirmDialog(
            title = "Удалить компонент?",
            message = "Компонент «${comp.name}» будет удалён с роутера. Отменить это действие нельзя.",
            onConfirm = {
                viewModel.removeComponent(comp.name)
                feedbackMessage = "Отправлена команда на удаление компонента «${comp.name}»"
                pendingDelete = null
                selectedComponent = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun ComponentCard(component: ComponentInfo, onClick: () -> Unit = {}) {
    SectionCard(
        title = component.title.ifEmpty { component.name },
        subtitle = component.description.ifBlank { null },
        icon = Icons.Default.Extension
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Подробнее",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.Primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (component.installed) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (component.installed) KeeneticColors.Primary else KeeneticColors.TextSecondary
            )
        }
        RowDivider()
        InfoRow("Версия", component.version.ifBlank { "Н/Д" })
        InfoRow(
            "Статус",
            if (component.installed) "Установлен" else if (component.available) "Доступен" else "Недоступен",
            valueColor = when {
                component.installed -> KeeneticColors.Primary
                component.available -> KeeneticColors.Warning
                else -> KeeneticColors.TextSecondary
            }
        )
    }
}
