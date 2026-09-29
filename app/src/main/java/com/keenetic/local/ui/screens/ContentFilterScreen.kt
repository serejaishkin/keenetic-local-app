package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.DnsFilterPreset
import com.keenetic.local.api.DnsFilterProfile
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
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

    SectionScaffold(
        title = "Контентная фильтрация",
        subtitle = "Интернет-фильтр",
        onBack = onBack,
        onRefresh = { viewModel.loadDnsFilters() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!dnsFilterInstalled) {
                item(key = "not-installed") {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = KeeneticColors.Error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "  Компонент «Интернет-фильтр» не установлен на роутере. " +
                                    "Фильтрация недоступна, пока компонент не установлен " +
                                    "через «Общие настройки» → «Компоненты».",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextPrimary
                            )
                        }
                    }
                }
            }

            item(key = "presets-header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SubGroupHeader("Доступные пресеты", dnsPresets.size)
                    Text(
                        "RCI: dns-proxy/filter/presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            if (dnsPresets.isEmpty()) {
                item(key = "presets-empty") {
                    SectionCard {
                        EmptyHint(
                            when {
                                rawPresets is ApiCallState.Loading -> "Загрузка пресетов с роутера..."
                                !dnsFilterInstalled -> "Компонент «Интернет-фильтр» не установлен"
                                else -> "Пресеты фильтрации не поставляются этой прошивкой. " +
                                    "Настроенные DNS-серверы смотрите в разделе «DNS»."
                            }
                        )
                    }
                }
            } else {
                items(dnsPresets, key = { it.id }) { preset ->
                    FilterPresetCard(preset = preset)
                }
            }

            item(key = "profiles-header") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SubGroupHeader("Активные профили", dnsProfiles.size)
                    Text(
                        "RCI: dns-proxy/filter/profiles",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            if (dnsProfiles.isEmpty()) {
                item(key = "profiles-empty") {
                    SectionCard {
                        EmptyHint(
                            when {
                                rawProfiles is ApiCallState.Loading -> "Загрузка профилей с роутера..."
                                !dnsFilterInstalled -> "Компонент «Интернет-фильтр» не установлен"
                                else -> "Профили фильтрации не настроены"
                            }
                        )
                    }
                }
            } else {
                items(dnsProfiles, key = { it.id }) { profile ->
                    FilterProfileCard(profile = profile, presets = dnsPresets)
                }
            }
        }
    }
}

@Composable
private fun FilterPresetCard(preset: DnsFilterPreset) {
    SectionCard(
        title = preset.name,
        subtitle = preset.provider.ifBlank { preset.type.ifBlank { null } },
        icon = Icons.Default.Shield
    ) {
        if (preset.description.isNotBlank()) {
            Text(
                preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
        InfoRow("Идентификатор", preset.id, monospace = true)
        InfoRow("Тип", preset.type.ifBlank { "—" })
        InfoRow("Профилей", preset.profilesCount.toString())
    }
}

@Composable
private fun FilterProfileCard(profile: DnsFilterProfile, presets: List<DnsFilterPreset>) {
    val preset = presets.firstOrNull { it.id == profile.presetId || it.name == profile.presetName }

    SectionCard(
        title = profile.name,
        subtitle = preset?.name ?: profile.presetName.ifBlank { "Без пресета" },
        icon = Icons.Default.FilterAlt
    ) {
        if (profile.description.isNotBlank()) {
            Text(
                profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }
        InfoRow("Пресет", preset?.id ?: profile.presetId.ifBlank { "—" }, monospace = true)
        InfoRow(
            label = "Назначен интерфейсам",
            value = if (profile.assignedTo.isEmpty()) "—" else profile.assignedTo.joinToString(", "),
            monospace = true
        )
        SwitchRow(
            label = "Профиль активен",
            checked = profile.enabled,
            onCheckedChange = {},
            enabled = false,
            description = "Переключение профиля выполняется в веб-интерфейсе"
        )
    }
}
