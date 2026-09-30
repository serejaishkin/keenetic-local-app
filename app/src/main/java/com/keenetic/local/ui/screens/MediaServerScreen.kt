package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun MediaServerScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val dlna by viewModel.dlnaConfig.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSmbAndDlnaSettings() }

    SectionScaffold(
        title = "Медиасервер (DLNA)",
        subtitle = "Доступ к мультимедиа",
        onBack = onBack
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "dlna") {
                SectionCard(
                    title = "Доступ к мультимедиа по DLNA / UPnP",
                    icon = Icons.Default.PlayArrow
                ) {
                    SwitchRow(
                        label = "Медиасервер включён",
                        checked = dlna.enabled,
                        onCheckedChange = { viewModel.setDlnaEnabled(it) }
                    )
                    InfoRow("Имя сервера", dlna.name.ifBlank { "Н/Д" })
                    if (dlna.port > 0) InfoRow("Порт", dlna.port.toString())
                    InfoRow("Тип", dlna.type.ifBlank { "Н/Д" })
                    Text(
                        "Каталоги для мультимедиа настраиваются в разделе «Накопители и устройства».",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }
    }
}
