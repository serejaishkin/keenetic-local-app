package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    var addTo by remember { mutableStateOf<String?>(null) }      // имя группы, куда добавляем домен
    var showCreate by remember { mutableStateOf(false) }
    var deleteGroup by remember { mutableStateOf<String?>(null) }

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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Группы FQDN: ${groups.size}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCreate = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Создать")
                    }
                }
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
                    ObjectGroupCard(
                        group,
                        onAdd = { addTo = group.name },
                        onRemove = { d -> viewModel.updateFqdnGroup(group.name, emptyList(), listOf(d))},
                        onDelete = { deleteGroup = group.name }
                    )
                }
            }
        }
    }

    addTo?.let { g ->
        TextInputDialog("Добавить домен в «$g»", "Домен, например example.com", { addTo = null }) { d ->
            viewModel.updateFqdnGroup(g, listOf(d), emptyList()); addTo = null
        }
    }
    if (showCreate) {
        TextInputDialog("Новая группа", "Имя группы (латиница)", { showCreate = false }) { n ->
            viewModel.createFqdnGroup(n, "", emptyList()); showCreate = false
        }
    }
    deleteGroup?.let { g ->
        AlertDialog(
            onDismissRequest = { deleteGroup = null },
            title = { Text("Удалить группу «$g»?") },
            text = { Text("Доменные маршруты, использующие группу, перестанут работать.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteFqdnGroup(g); deleteGroup = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { deleteGroup = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ObjectGroupCard(
    group: ObjectGroupFqdn,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(title = group.name, icon = Icons.Default.Dns) {
        if (group.members.isEmpty()) {
            EmptyHint("В группе нет доменов")
        } else {
            group.members.forEach { member ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(member, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(member) }) {
                        Icon(Icons.Default.Close, contentDescription = "Убрать $member")
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onAdd) { Text("Добавить домен") }
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("Удалить группу")
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onOk: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onOk(text.trim()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
