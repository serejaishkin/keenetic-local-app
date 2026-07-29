package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Отдельный экран "Приложения" (opkg/торрент/IntelliQoS), раньше жил вместе
 * с системными настройками подключения в одном SettingsScreen - развели по
 * аналогии с реальным сайтом, где это разные разделы ("Управление →
 * Настройки системы" отдельно от "Управление → Приложения").
 */
@Composable
fun AppsScreen(viewModel: RouterViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Приложения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "IntelliQoS, менеджер пакетов, торрент-клиент",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { ExtraServicesCard(viewModel) }
        }
    }
}
