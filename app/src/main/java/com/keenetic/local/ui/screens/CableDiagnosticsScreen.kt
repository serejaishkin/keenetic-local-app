package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.CableDiagnosticResult
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader

@Composable
fun CableDiagnosticsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val results by viewModel.cableDiagnostics.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCableDiagnostics() }

    SectionScaffold(
        title = "Диагностика кабеля",
        subtitle = "Длина пар и состояние портов",
        onBack = onBack,
        onRefresh = { viewModel.loadCableDiagnostics() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (results.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint("Нет данных. Обновите экран, чтобы опросить порты.")
                    }
                }
            } else {
                item(key = "header") {
                    SubGroupHeader("Порты", results.size)
                }
                items(results) { result ->
                    CableResultCard(result)
                }
            }
        }
    }
}

@Composable
private fun CableResultCard(result: CableDiagnosticResult) {
    SectionCard(
        title = result.interfaceName,
        subtitle = "Пара ${result.pair}",
        icon = Icons.Default.Cable
    ) {
        InfoRow("Длина", "${result.length} м", monospace = true)
        RowDivider()
        InfoRow("Статус", result.status)
    }
}
