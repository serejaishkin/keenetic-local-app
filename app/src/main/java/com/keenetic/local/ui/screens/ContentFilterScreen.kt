package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ContentFilterScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val nextdns by viewModel.nextdnsProfile.collectAsState()
    val safedns by viewModel.safednsProfile.collectAsState()
    val skydns by viewModel.skydnsProfile.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNextdnsProfile()
        viewModel.loadSafednsProfile()
        viewModel.loadSkydnsProfile()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Shield, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Контентная фильтрация", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        // NextDNS
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("NextDNS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = nextdns.enabled, onCheckedChange = { viewModel.setNextdnsEnabled(it) })
                    }
                    InfoRow("ID", nextdns.id)
                    InfoRow("Имя", nextdns.name)
                    InfoRow("Настроен", if (nextdns.configured) "Да" else "Нет")
                }
            }
        }

        // SafeDNS
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("SafeDNS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = safedns.enabled, onCheckedChange = { viewModel.setSafednsEnabled(it) })
                    }
                    InfoRow("ID", safedns.id)
                    InfoRow("Имя", safedns.name)
                    InfoRow("Настроен", if (safedns.configured) "Да" else "Нет")
                }
            }
        }

        // SkyDNS
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("SkyDNS", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Включён", color = KeeneticColors.TextPrimary)
                        Switch(checked = skydns.enabled, onCheckedChange = { viewModel.setSkydnsEnabled(it) })
                    }
                    InfoRow("ID", skydns.id)
                    InfoRow("Имя", skydns.name)
                    InfoRow("Настроен", if (skydns.configured) "Да" else "Нет")
                }
            }
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
