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
import com.keenetic.local.api.*
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun Ipv6Screen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val addresses by viewModel.ipv6Addresses.collectAsState()
    val prefixes by viewModel.ipv6Prefixes.collectAsState()
    val routes by viewModel.ipv6Routes.collectAsState()
    val subnets by viewModel.ipv6Subnets.collectAsState()
    val dhcpBindings by viewModel.ipv6DhcpBindings.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadIpv6Addresses()
        viewModel.loadIpv6Prefixes()
        viewModel.loadIpv6Routes()
        viewModel.loadIpv6Subnets()
        viewModel.loadIpv6DhcpBindings()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Language, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("IPv6", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        }

        item { SectionCard("Адреса (${addresses.size})", addresses) { a -> Text("${a.address}/${a.prefix} [${a.interfaceName}]", color = KeeneticColors.TextSecondary) } }
        item { SectionCard("Префиксы (${prefixes.size})", prefixes) { p -> Text("${p.prefix} → ${p.interfaceName}", color = KeeneticColors.TextSecondary) } }
        item { SectionCard("Маршруты (${routes.size})", routes) { r -> Text("${r.network}/${r.prefix} → ${r.gateway} [${r.interfaceName}]", color = KeeneticColors.TextSecondary) } }
        item { SectionCard("Подсети (${subnets.size})", subnets) { s -> Text("${s.network}/${s.prefix} [${s.interfaceName}]", color = KeeneticColors.TextSecondary) } }
        item { SectionCard("DHCPv6 привязки (${dhcpBindings.size})", dhcpBindings) { b -> Text("${b.address} ${b.hostname}", color = KeeneticColors.TextSecondary) } }
    }
}

@Composable
private fun <T> SectionCard(title: String, items: List<T>, content: @Composable (T) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = KeeneticColors.Divider)
            if (items.isEmpty()) {
                Text("Нет данных", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
            }
            items.forEach { item ->
                content(item)
            }
        }
    }
}
