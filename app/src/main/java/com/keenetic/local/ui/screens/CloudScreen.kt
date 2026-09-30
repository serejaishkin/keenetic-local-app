package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun CloudScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val cloudStatus by viewModel.cloudStatus.collectAsState()
    val cloudNdmp by viewModel.cloudNdmp.collectAsState()
    val cloudInstalled by viewModel.cloudInstalled.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCloudStatus() }

    SectionScaffold(
        title = "Облачные сервисы",
        subtitle = "Keenetic Cloud и NDMP",
        onBack = onBack,
        onRefresh = { viewModel.loadCloudStatus() }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!cloudInstalled) {
                item(key = "not-installed") {
                    SectionCard(title = "Keenetic Cloud") {
                        EmptyHint("Компонент Keenetic Cloud не установлен")
                    }
                }
            } else {
                item(key = "cloud") {
                    SectionCard(
                        title = "Keenetic Cloud",
                        icon = Icons.Default.Cloud
                    ) {
                        InfoRow(
                            "Включён",
                            if (cloudStatus.enabled) "Да" else "Нет",
                            valueColor = if (cloudStatus.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                        )
                        InfoRow("Тип", cloudStatus.cloudType.ifBlank { "—" })
                        InfoRow("Адрес", cloudStatus.address.ifBlank { "—" }, monospace = true)
                    }
                }

                item(key = "ndmp") {
                    SectionCard(
                        title = "NDMP (сетевое резервное копирование)",
                        icon = Icons.Default.Storage
                    ) {
                        InfoRow(
                            "Включён",
                            if (cloudNdmp.enabled) "Да" else "Нет",
                            valueColor = if (cloudNdmp.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                        )
                        InfoRow("Статус", cloudNdmp.status.ifBlank { "—" })
                        InfoRow("Подготовлен", if (cloudNdmp.prepared) "Да" else "Нет")
                    }
                }
            }
        }
    }
}
