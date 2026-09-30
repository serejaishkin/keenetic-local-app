package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
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
fun SmbScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val smb by viewModel.smbConfig.collectAsState()
    val unsupported by viewModel.unsupportedFeatures.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkFeatureSupport("smb", "smb")
        viewModel.loadSmbAndDlnaSettings()
    }

    SectionScaffold(
        title = "Сеть Windows (SMB)",
        subtitle = "Общий доступ к файлам",
        onBack = onBack
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (unsupported.contains("smb")) {
                item(key = "unsupported") {
                    UnsupportedNotice(
                        "Сеть Windows (SMB)",
                        "Компонент SMB/CIFS не установлен на прошивке KN-2311 (fw 5.01.C.4.0-1). " +
                            "Для установки перейдите в «Менеджер пакетов OPKG» и установите пакет «smbd»."
                    )
                }
            }

            item(key = "smb") {
                SectionCard(
                    title = "Символические имена для общего доступа (SMB / CIFS)",
                    icon = Icons.Default.Storage
                ) {
                    SwitchRow(
                        label = "Служба SMB включена",
                        checked = smb.enabled,
                        onCheckedChange = { viewModel.setSmbEnabled(it) }
                    )
                    InfoRow("Имя сервера", smb.name.ifBlank { "Н/Д" })
                    InfoRow("Рабочая группа", smb.workgroup.ifBlank { "Н/Д" })
                    InfoRow("Описание", smb.description.ifBlank { "Н/Д" })
                    Text(
                        "Общие папки и права доступа настраиваются в разделе «Накопители и устройства».",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
            }
        }
    }
}
