package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.theme.KeeneticColors

private val ACL_MODES = listOf(
    "none" to "Отключен (все разрешены)",
    "permit" to "Белый список (только из списка)",
    "deny" to "Черный список (все кроме списка)"
)

@Composable
fun WifiAclScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val interfaces by viewModel.interfaces.collectAsState()
    val unsupported by viewModel.unsupportedFeatures.collectAsState()
    val segments = remember(interfaces) { interfaces.filter { it.type == "Bridge" } }

    LaunchedEffect(Unit) {
        viewModel.checkFeatureSupport("wifi_acl", "sc/interface/mac.access-list")
        viewModel.loadInterfaces()
    }

    SectionScaffold(
        title = "Контроль доступа Wi-Fi",
        subtitle = "Черные и белые списки для сегментов",
        onBack = onBack,
        onRefresh = { viewModel.loadInterfaces() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (unsupported.contains("wifi_acl")) {
                item(key = "unsupported") {
                    UnsupportedNotice(
                        "Контроль доступа Wi-Fi",
                        "RCI-путь show/sc/interface/mac.access-list не поддерживается на данной " +
                            "прошивке KN-2311 (fw 5.01.C.4.0-1). MAC-фильтрация управляется через " +
                            "веб-интерфейс, но не через REST API."
                    )
                }
            }

            if (segments.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint("Сегменты сети не найдены")
                    }
                }
            } else {
                items(segments, key = { it.id }) { seg ->
                    AclSegmentCard(seg.id, seg.name, seg.macAccessMode) { mode ->
                        viewModel.setWifiAclMode(seg.id, mode)
                    }
                }
            }

            item(key = "note") {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = KeeneticColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Для добавления устройств в списки используйте веб-интерфейс. " +
                                "В данном приложении доступно только переключение режимов.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AclSegmentCard(id: String, name: String, currentMode: String, onModeChange: (String) -> Unit) {
    var picker by remember { mutableStateOf(false) }
    val modeText = ACL_MODES.find { it.first == currentMode }?.second ?: currentMode

    SectionCard(
        title = name,
        subtitle = "Сегмент",
        icon = Icons.Default.Security
    ) {
        EditableRow(
            label = "Режим доступа",
            value = modeText,
            onClick = { picker = true }
        )
    }

    if (picker) {
        OptionPickerDialog(
            title = "Режим доступа: $name",
            options = ACL_MODES,
            selectedKey = currentMode,
            onSelect = onModeChange,
            onDismiss = { picker = false }
        )
    }
}
