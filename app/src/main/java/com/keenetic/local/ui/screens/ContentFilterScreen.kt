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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.screens.common.ApiCallState
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ContentFilterScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val dnsPresets by viewModel.dnsFilterPresetList.collectAsState()
    val dnsProfiles by viewModel.dnsFilterProfileList.collectAsState()
    val rawPresets by viewModel.dnsFilterPresets.collectAsState()
    val rawProfiles by viewModel.dnsFilterProfiles.collectAsState()
    val dnsFilterInstalled by viewModel.dnsFilterInstalled.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDnsFilters()
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

        // Note about missing components
        if (!dnsFilterInstalled) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Error.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Error.copy(alpha = 0.3f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = KeeneticColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Компонент Интернет-фильтр не установлен",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Presets section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = KeeneticColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Доступные пресеты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                }
                Text(
                    text = "RCI: dns-proxy/filter/presets",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = KeeneticColors.TextSecondary
                )
            }
        }

        if (dnsPresets.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.FilterAlt,
                    title = "Пресеты не найдены",
                    subtitle = if (rawPresets is ApiCallState.Loading)
                        "Загрузка с роутера..."
                    else if (!dnsFilterInstalled) "Компонент не установлен на роутере"
                    else "Фильтры не активированы"
                )
            }
        } else {
            items(dnsPresets, key = { it.id }) { preset ->
                DnsFilterPresetCard(preset = preset)
            }
        }

        // Profiles section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = KeeneticColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Активные профили",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                }
                Text(
                    text = "RCI: dns-proxy/filter/profiles",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = KeeneticColors.TextSecondary
                )
            }
        }

        if (dnsProfiles.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Dns,
                    title = "Нет активных профилей",
                    subtitle = if (rawProfiles is ApiCallState.Loading)
                        "Загрузка с роутера..."
                    else if (!dnsFilterInstalled) "Компонент не установлен на роутере"
                    else "Профили фильтрации не настроены"
                )
            }
        } else {
            items(dnsProfiles, key = { it.id }) { profile ->
                DnsFilterProfileCard(profile = profile, presets = dnsPresets)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KeeneticColors.TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = KeeneticColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
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
