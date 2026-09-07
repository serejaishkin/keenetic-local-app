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
import com.keenetic.local.api.CableDiagnosticResult
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun CableDiagnosticsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val results by viewModel.cableDiagnostics.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCableDiagnostics() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Cable, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Диагностика кабеля", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        if (results.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Нет данных", color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        items(results) { result ->
            CableResultCard(result)
        }
    }
}

@Composable
private fun CableResultCard(result: CableDiagnosticResult) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.interfaceName, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Text("Пара ${result.pair}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
            HorizontalDivider(color = KeeneticColors.Divider)
            InfoRow("Длина", "${result.length} м")
            InfoRow("Статус", result.status)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
