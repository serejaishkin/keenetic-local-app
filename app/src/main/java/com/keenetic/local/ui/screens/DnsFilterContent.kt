package com.keenetic.local.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.DnsFilterPreset
import com.keenetic.local.api.DnsFilterProfile
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Блок «Фильтрация DNS-запросов» для раздела DNS.
 * Вынесено из DnsScreen.kt как LazyListScope-расширение; внутренние карточки приватны для этого файла.
 * Вставляется в общий LazyColumn раздела, поэтому задаётся как LazyListScope.
 * Список серверов здесь намеренно не выводится — он живёт в карточке «DNS-серверы».
 */
internal data class DnsFilterUiState(
    val presets: List<DnsFilterPreset>,
    val profiles: List<DnsFilterProfile>,
    val presetsLoading: Boolean,
    val profilesLoading: Boolean,
    val installed: Boolean
)

internal fun LazyListScope.dnsFilterItems(state: DnsFilterUiState) {
    val presets = state.presets
    val profiles = state.profiles
    val installed = state.installed

    item(key = "dns-filter-header") {
        Spacer(modifier = Modifier.height(8.dp))
        DnsSectionHeader(
            icon = Icons.Default.Security,
            title = "Фильтрация DNS-запросов",
            subtitle = "dns-proxy / filter"
        )
    }

    if (!installed) {
        item(key = "dns-filter-missing") {
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
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = KeeneticColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            "Компонент «Интернет-фильтр» не установлен",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KeeneticColors.TextPrimary
                        )
                        Text(
                            "Профили и пресеты фильтрации станут доступны после установки компонента.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    item(key = "dns-filter-presets-title") {
        DnsSubHeader(title = "Пресеты", count = presets.size)
    }

    if (presets.isEmpty()) {
        item(key = "dns-filter-presets-empty") {
            DnsEmptyState(
                title = "Пресеты не найдены",
                subtitle = when {
                    state.presetsLoading -> "Загрузка с роутера…"
                    !installed -> "Компонент не установлен на роутере"
                    else -> "На этой прошивке встроенные пресеты не поставляются"
                }
            )
        }
    } else {
        items(presets, key = { "preset-${it.id}" }) { preset ->
            DnsContentPresetCard(preset = preset)
        }
    }

    item(key = "dns-filter-profiles-title") {
        DnsSubHeader(title = "Профили фильтрации", count = profiles.size)
    }

    if (profiles.isEmpty()) {
        item(key = "dns-filter-profiles-empty") {
            DnsEmptyState(
                title = "Профили не настроены",
                subtitle = when {
                    state.profilesLoading -> "Загрузка с роутера…"
                    !installed -> "Компонент не установлен на роутере"
                    else -> "Фильтрация запросов для устройств не задана"
                }
            )
        }
    } else {
        items(profiles, key = { "profile-${it.id}" }) { profile ->
            DnsContentProfileCard(profile = profile, presets = presets)
        }
    }

    item(key = "dns-filter-bottom") { Spacer(modifier = Modifier.height(4.dp)) }
}

@Composable
private fun DnsContentPresetCard(preset: DnsFilterPreset) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                DnsStatusBadge(active = preset.enabled)
            }

            if (preset.type.isNotBlank()) {
                Text(
                    preset.type.uppercase(),
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
private fun DnsContentProfileCard(profile: DnsFilterProfile, presets: List<DnsFilterPreset>) {
    val matchedPreset = presets.find { it.id == profile.presetId || it.name == profile.presetName }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                DnsStatusBadge(active = profile.enabled)
            }

            if (profile.description.isNotBlank()) {
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }

            if (profile.assignedTo.isNotEmpty()) {
                Text(
                    "Назначен на: ${profile.assignedTo.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DnsStatusBadge(active: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.SurfaceElevated
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
                    .background(if (active) KeeneticColors.Success else KeeneticColors.TextSecondary)
            )
            Text(
                if (active) "Активен" else "Выкл",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) KeeneticColors.Success else KeeneticColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DnsEmptyState(title: String, subtitle: String, icon: ImageVector = Icons.Default.FilterAlt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KeeneticColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = KeeneticColors.TextSecondary
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
internal fun DnsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KeeneticColors.TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = KeeneticColors.TextSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun DnsSubHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = KeeneticColors.TextSecondary
        )
        if (count != null) {
            Text(
                " · $count",
                style = MaterialTheme.typography.labelLarge,
                color = KeeneticColors.TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
