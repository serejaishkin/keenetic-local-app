package com.keenetic.local.ui.screens

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
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ComponentInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ComponentsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val componentList by viewModel.componentList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadComponents()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Icon(Icons.Default.Extension, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Компоненты системы",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        if (componentList.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Нет данных", color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        items(componentList) { component ->
            ComponentCard(component)
        }
    }
}

@Composable
private fun ComponentCard(component: ComponentInfo) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        component.title.ifEmpty { component.name },
                        style = MaterialTheme.typography.bodyLarge,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        component.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Icon(
                    if (component.installed) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (component.installed) KeeneticColors.Primary else KeeneticColors.TextSecondary
                )
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Версия", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                Text(component.version, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Статус", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                Text(
                    if (component.installed) "Установлен" else if (component.available) "Доступен" else "Недоступен",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        component.installed -> KeeneticColors.Primary
                        component.available -> KeeneticColors.Warning
                        else -> KeeneticColors.TextSecondary
                    }
                )
            }
        }
    }
}
