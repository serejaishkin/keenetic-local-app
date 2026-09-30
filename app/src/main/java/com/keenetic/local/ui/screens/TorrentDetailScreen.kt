package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Person
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
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun TorrentDetailScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val status by viewModel.torrentStatusFull.collectAsState()
    val account by viewModel.torrentLocalAccount.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTorrentStatusFull() }

    SectionScaffold(
        title = "Торрент-клиент",
        subtitle = "Transmission на роутере",
        onBack = onBack,
        onRefresh = { viewModel.loadTorrentStatusFull() }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                SectionCard(
                    title = "Статус",
                    icon = Icons.Default.CloudDownload
                ) {
                    InfoRow(
                        "Включён",
                        if (status.enabled) "Да" else "Нет",
                        valueColor = if (status.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                    )
                    InfoRow("Состояние", status.state.ifBlank { "—" })
                    InfoRow("RPC порт", "${status.rpcPort}")
                    InfoRow("RPC публичный", if (status.rpcPublic) "Да" else "Нет")
                    InfoRow("Пир порт", "${status.peerPort}")
                    InfoRow("Каталог загрузки", status.downloadDir.ifBlank { "—" }, monospace = true)
                    Text(
                        "RCI отдаёт состояние, настройки и локальную учётную запись. Список раздач и их скорости доступны только в веб-интерфейсе Transmission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }

            item(key = "account") {
                SectionCard(
                    title = "Локальная учётная запись",
                    icon = Icons.Default.Person
                ) {
                    InfoRow("Пользователь", account.username.ifBlank { "—" })
                    InfoRow(
                        "Включён",
                        if (account.enabled) "Да" else "Нет",
                        valueColor = if (account.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                    )
                }
            }
        }
    }
}
