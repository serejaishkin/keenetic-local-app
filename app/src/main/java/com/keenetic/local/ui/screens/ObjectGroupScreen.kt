package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ObjectGroupFqdn
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader

@Composable
fun ObjectGroupScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val groups by viewModel.objectGroups.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadObjectGroups() }

    SectionScaffold(
        title = "Группы доменных имён",
        subtitle = "Списки FQDN для доменных маршрутов",
        onBack = onBack,
        onRefresh = { viewModel.loadObjectGroups() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                SubGroupHeader("Группы FQDN", groups.size)
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint(
                            "Групп доменных имён нет. Группы FQDN используются в доменных " +
                                "маршрутах («Маршрутизация» → вкладка DNS) для отправки трафика " +
                                "по списку доменов."
                        )
                    }
                }
            } else {
                items(groups, key = { it.name }) { group ->
                    ObjectGroupCard(group)
                }
            }
        }
    }
}

@Composable
private fun ObjectGroupCard(group: ObjectGroupFqdn) {
    SectionCard(title = group.name, icon = Icons.Default.Dns) {
        if (group.members.isEmpty()) {
            EmptyHint("В группе нет доменов")
        } else {
            group.members.forEach { member ->
                InfoRow(label = "Домен", value = member, monospace = true)
            }
        }
    }
}
