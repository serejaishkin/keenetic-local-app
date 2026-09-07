package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.ObjectGroupFqdn
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun ObjectGroupScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val groups by viewModel.objectGroups.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadObjectGroups() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Dns, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("FQDN группы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        if (groups.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Нет групп", color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        items(groups) { group ->
            ObjectGroupCard(group)
        }
    }
}

@Composable
private fun ObjectGroupCard(group: ObjectGroupFqdn) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(group.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            HorizontalDivider(color = KeeneticColors.Divider)
            if (group.members.isEmpty()) {
                Text("Нет成员", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
            group.members.forEach { member ->
                Text(member, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
            }
        }
    }
}
