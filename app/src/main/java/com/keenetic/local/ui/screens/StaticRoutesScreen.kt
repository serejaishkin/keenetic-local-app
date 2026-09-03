package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.StaticRoute
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun StaticRoutesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val routes by viewModel.staticRoutes.collectAsState()
    var selectedRoute by remember { mutableStateOf<StaticRoute?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Add route form fields
    var routeNetwork by remember { mutableStateOf("") }
    var routeMask by remember { mutableStateOf("255.255.255.0") }
    var routeGateway by remember { mutableStateOf("") }
    var routeIface by remember { mutableStateOf("ISP") }
    var routeComment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadStaticRoutes()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            feedbackMessage = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KeeneticColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить маршрут", tint = KeeneticColors.Background)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KeeneticColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(Icons.Default.Route, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Статическая маршрутизация",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.loadStaticRoutes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                    }
                }
            }

            if (routes.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Статические маршруты не настроены",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Используются маршруты по умолчанию, полученные от интернет-провайдера (DHCP/PPPoE/IPoE).",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Добавить статический маршрут")
                            }
                        }
                    }
                }
            } else {
                items(routes) { route ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                        modifier = Modifier.clickable { selectedRoute = route }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = KeeneticColors.Primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    route.comment.ifEmpty { "${route.network} / ${route.mask}" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KeeneticColors.TextPrimary
                                )
                                Text(
                                    "Шлюз: ${if (route.gateway.isNotBlank()) route.gateway else "авто"} (${route.interfaceName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deleteStaticRoute(route.id)
                                feedbackMessage = "Маршрут удален"
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeeneticColors.Error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Route Details Sub-actions Dialog
    selectedRoute?.let { route ->
        AlertDialog(
            onDismissRequest = { selectedRoute = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Параметры маршрута", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Сеть назначения", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(route.network, fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Маска подсети", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(route.mask, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Шлюз (Gateway)", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(if (route.gateway.isNotBlank()) route.gateway else "Автоматически", color = KeeneticColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Интерфейс выхода", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(route.interfaceName, color = KeeneticColors.TextPrimary)
                    }
                    if (route.comment.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Описание / Метка", color = KeeneticColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(route.comment, color = KeeneticColors.TextPrimary)
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteStaticRoute(route.id)
                            feedbackMessage = "Маршрут удален"
                            selectedRoute = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Удалить маршрут")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRoute = null }) {
                    Text("Закрыть", color = KeeneticColors.TextPrimary)
                }
            }
        )
    }

    // Add New Route Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Добавить статический маршрут", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = routeNetwork,
                        onValueChange = { routeNetwork = it },
                        label = { Text("Сеть назначения (напр. 10.8.0.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = routeMask,
                        onValueChange = { routeMask = it },
                        label = { Text("Маска подсети (напр. 255.255.255.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = routeGateway,
                        onValueChange = { routeGateway = it },
                        label = { Text("Шлюз (Gateway, опционально)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = routeComment,
                        onValueChange = { routeComment = it },
                        label = { Text("Описание (напр. VPN сеть офиса)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeNetwork.isNotBlank() && routeMask.isNotBlank()) {
                            val newRoute = StaticRoute(
                                id = (routes.size + 1).toString(),
                                network = routeNetwork,
                                mask = routeMask,
                                gateway = routeGateway,
                                interfaceName = routeIface,
                                auto = false,
                                comment = routeComment
                            )
                            viewModel.addStaticRoute(newRoute)
                            feedbackMessage = "Маршрут для сети $routeNetwork добавлен"
                            showAddDialog = false
                            routeNetwork = ""
                            routeGateway = ""
                            routeComment = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}

