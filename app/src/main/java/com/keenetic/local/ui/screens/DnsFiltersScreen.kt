package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.DnsFilterPreset
import com.keenetic.local.api.DnsFilterProfile
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsFiltersScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val presets by viewModel.dnsFilterPresetList.collectAsState()
    val profiles by viewModel.dnsFilterProfileList.collectAsState()
    val rawPresets by viewModel.dnsFilterPresets.collectAsState()
    val rawProfiles by viewModel.dnsFilterProfiles.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDnsFilters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DNS-фильтры",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "Профили контентной фильтрации",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = KeeneticColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDnsFilters() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = KeeneticColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KeeneticColors.Background)
            )
        },
        containerColor = KeeneticColors.Background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Info banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Primary.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Primary.copy(alpha = 0.3f))
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
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "DNS-фильтрация защищает устройства от фишинга, рекламы и вредоносных сайтов на уровне роутера.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextPrimary
                        )
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
                            Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Доступные пресеты",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                    Text(
                        "RCI: dns-proxy/filter/presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            if (presets.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.FilterAlt,
                        title = "Пресеты не найдены",
                        subtitle = if (rawPresets is com.keenetic.local.ui.screens.common.ApiCallState.Loading)
                            "Загрузка с роутера..." else "Фильтры не активированы или не установлены компоненты"
                    )
                }
            } else {
                items(presets, key = { it.id }) { preset ->
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
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Активные профили",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                    }
                    Text(
                        "RCI: dns-proxy/filter/profiles",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            if (profiles.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Dns,
                        title = "Нет активных профилей",
                        subtitle = if (rawProfiles is com.keenetic.local.ui.screens.common.ApiCallState.Loading)
                            "Загрузка с роутера..." else "Профили фильтрации не настроены"
                    )
                }
            } else {
                items(profiles, key = { it.id }) { profile ->
                    DnsFilterProfileCard(profile = profile, presets = presets)
                }
            }
        }
    }
}

@Composable
private fun DnsFilterPresetCard(preset: DnsFilterPreset) {
    val typeColor = when (preset.type) {
        "adguard" -> KeeneticColors.Primary
        "nextdns" -> KeeneticColors.Success
        "cloudflare" -> KeeneticColors.Warning
        "safe" -> KeeneticColors.Secondary
        else -> KeeneticColors.TextSecondary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (preset.enabled) typeColor.copy(alpha = 0.4f) else KeeneticColors.Divider
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        if (preset.provider.isNotBlank()) {
                            Text(
                                preset.provider,
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (preset.enabled) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (preset.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary)
                        )
                        Text(
                            if (preset.enabled) "Активен" else "Выкл",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (preset.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            // Type badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = typeColor.copy(alpha = 0.12f)
            ) {
                Text(
                    preset.type.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            if (preset.description.isNotBlank()) {
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DnsFilterProfileCard(profile: DnsFilterProfile, presets: List<DnsFilterPreset>) {
    val matchedPreset = presets.find { it.id == profile.presetId || it.name == profile.presetName }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = KeeneticColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = KeeneticColors.TextPrimary
                        )
                        if (profile.presetId.isNotBlank() || profile.presetName.isNotBlank()) {
                            Text(
                                "Пресет: ${matchedPreset?.name ?: profile.presetId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary
                            )
                        }
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (profile.enabled) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (profile.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary)
                        )
                        Text(
                            if (profile.enabled) "Активен" else "Выкл",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.enabled) KeeneticColors.Success else KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            if (profile.description.isNotBlank()) {
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }

            // Assigned interfaces
            if (profile.assignedTo.isNotEmpty()) {
                HorizontalDivider(color = KeeneticColors.Divider)
                Text(
                    "Назначен на:",
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.assignedTo.forEach { iface ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KeeneticColors.Primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                iface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KeeneticColors.Primary
                            )
                        }
                    }
                }
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
                color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
