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
import com.keenetic.local.api.UpnpRedirect
import com.keenetic.local.api.UpnpPinhole
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun UpnpScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val redirects by viewModel.upnpRedirects.collectAsState()
    val pinholes by viewModel.upnpPinholes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUpnpStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = KeeneticColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "UPnP / NAT-PMP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        }

        // Redirects
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Переадресации", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (redirects.isEmpty()) {
                        Text("Нет переадресаций", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        items(redirects) { redirect ->
            UpnpRedirectCard(redirect)
        }

        // Pinholes
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary)
                        Text("Дыры в NAT", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KeeneticColors.Divider)
                    if (pinholes.isEmpty()) {
                        Text("Нет дыр в NAT", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextSecondary)
                    }
                }
            }
        }

        items(pinholes) { pinhole ->
            UpnpPinholeCard(pinhole)
        }
    }
}

@Composable
private fun UpnpRedirectCard(redirect: UpnpRedirect) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(redirect.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Icon(
                    if (redirect.enabled) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (redirect.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                )
            }
            Text("${redirect.proto} ${redirect.externalPort} → ${redirect.internalIp}:${redirect.internalPort}", color = KeeneticColors.TextSecondary)
        }
    }
}

@Composable
private fun UpnpPinholeCard(pinhole: UpnpPinhole) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pinhole.name, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                Icon(
                    if (pinhole.enabled) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (pinhole.enabled) KeeneticColors.Primary else KeeneticColors.TextSecondary
                )
            }
            Text("${pinhole.proto} порт ${pinhole.port} → ${pinhole.internalIp}", color = KeeneticColors.TextSecondary)
        }
    }
}
