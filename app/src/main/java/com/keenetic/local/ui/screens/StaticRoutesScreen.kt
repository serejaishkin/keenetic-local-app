package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.DnsRouteData
import com.keenetic.local.api.FqdnGroup
import com.keenetic.local.api.RouterRouteEntry
import com.keenetic.local.api.StaticRoute
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticRoutesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val routes by viewModel.staticRoutes.collectAsState()
    val ipv6Routes by viewModel.ipv6StaticRoutes.collectAsState()
    val dnsRoutes by viewModel.dnsRoutes.collectAsState()
    val fqdnGroups by viewModel.fqdnGroups.collectAsState()
    val currentIpv4Routes by viewModel.currentIpv4Routes.collectAsState()
    val currentIpv6Routes by viewModel.currentIpv6Routes.collectAsState()
    val viaInterfaces by viewModel.vpnViaInterfaces.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingRoute by remember { mutableStateOf<StaticRoute?>(null) }
    var editingDnsRoute by remember { mutableStateOf<DnsRouteData?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var routeToDelete by remember { mutableStateOf<StaticRoute?>(null) }
    var dnsRouteToDelete by remember { mutableStateOf<DnsRouteData?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadStaticRoutes()
        viewModel.loadIpv6StaticRoutes()
        viewModel.loadDnsRoutes()
        viewModel.loadFqdnGroups()
        viewModel.loadCurrentRoutes()
        if (viaInterfaces.isEmpty()) viewModel.loadViaInterfaces()
    }
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadStaticRoutes()
            1 -> viewModel.loadIpv6StaticRoutes()
            else -> viewModel.loadDnsRoutes()
        }
    }

    val isIpv6 = selectedTab == 1
    val isDns = selectedTab == 2
    val currentDnsRoutes = if (isDns) dnsRoutes else emptyList()
    val currentRoutes = if (isIpv6) ipv6Routes else routes

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
                onClick = { editingRoute = null; editingDnsRoute = null; showEditor = true },
                containerColor = KeeneticColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить маршрут", tint = KeeneticColors.Background)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KeeneticColors.Background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                IconButton(onClick = {
                    when (selectedTab) {
                        0 -> viewModel.loadStaticRoutes()
                        1 -> viewModel.loadIpv6StaticRoutes()
                        else -> viewModel.loadDnsRoutes()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = KeeneticColors.Surface,
                contentColor = KeeneticColors.Primary,
                edgePadding = 16.dp
            ) {
                listOf("IPv4-маршруты", "IPv6-маршруты", "DNS-маршруты").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) KeeneticColors.Primary else KeeneticColors.TextSecondary
                            )
                        }
                    )
                }
            }

            if (isDns && currentDnsRoutes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = KeeneticColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Доменные маршруты не настроены",
                        style = MaterialTheme.typography.titleMedium,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Трафик к доменным именам из выбранного списка будет направляться через указанный интерфейс. Списки доменов создаются в разделе «Контентная фильтрация».",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { editingDnsRoute = null; showEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить DNS-маршрут")
                    }
                }
            } else if (isDns) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentDnsRoutes) { route ->
                        DnsRouteCard(
                            route = route,
                            groupDescription = fqdnGroups.firstOrNull { it.name == route.group }?.description,
                            onToggle = { enabled -> viewModel.toggleDnsRoute(route, enabled) },
                            onEdit = { editingDnsRoute = route; showEditor = true },
                            onDelete = { dnsRouteToDelete = route }
                        )
                    }
                }
            } else if (currentRoutes.isEmpty()) {
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
                        if (isIpv6) "IPv6-маршруты не настроены" else "Статические маршруты не настроены",
                        style = MaterialTheme.typography.titleMedium,
                        color = KeeneticColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isIpv6)
                            "Используются маршруты по умолчанию, полученные от интернет-провайдера."
                        else
                            "Используются маршруты по умолчанию, полученные от интернет-провайдера (DHCP/PPPoE/IPoE).",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { editingRoute = null; showEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить маршрут")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentRoutes) { route ->
                        RouteCard(
                            route = route,
                            isIpv6 = isIpv6,
                            onToggle = { enabled ->
                                if (isIpv6) viewModel.toggleIpv6StaticRoute(route, enabled)
                                else viewModel.toggleStaticRoute(route, enabled)
                            },
                            onEdit = { editingRoute = route; showEditor = true },
                            onDelete = { routeToDelete = route }
                        )
                    }
                    item {
                        CurrentRoutesSection(
                            entries = if (isIpv6) currentIpv6Routes else currentIpv4Routes,
                            title = if (isIpv6) "Текущие маршруты IPv6 (роутер)" else "Текущие маршруты IPv4 (роутер)"
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        if (isDns) {
            DnsRouteEditorDialog(
                initial = editingDnsRoute,
                groups = fqdnGroups,
                interfaces = viaInterfaces,
                onDismiss = { showEditor = false; editingDnsRoute = null },
                onSave = { route ->
                    viewModel.saveDnsRoute(route)
                    feedbackMessage = if (editingDnsRoute != null)
                        "DNS-маршрут обновлен"
                    else
                        "DNS-маршрут «${route.group}» добавлен"
                    showEditor = false
                    editingDnsRoute = null
                },
                onCreateGroup = { name, description, domains ->
                    viewModel.createFqdnGroup(name, description, domains)
                }
            )
        } else {
            RouteEditorDialog(
                ipv6 = isIpv6,
                initial = editingRoute,
                interfaces = viaInterfaces,
                onDismiss = { showEditor = false; editingRoute = null },
                onSave = { route ->
                    if (isIpv6) viewModel.saveIpv6StaticRoute(route) else viewModel.saveStaticRoute(route)
                    feedbackMessage = if (editingRoute != null)
                        "Маршрут обновлен"
                    else
                        "Маршрут ${routeDestinationTitle(route)} добавлен"
                    showEditor = false
                    editingRoute = null
                }
            )
        }
    }

    dnsRouteToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { dnsRouteToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = KeeneticColors.Error)
                    Text("Удалить DNS-маршрут?", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Text(
                    "Доменный маршрут «${route.group}» будет удалён с роутера.",
                    color = KeeneticColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDnsRoute(route)
                        feedbackMessage = "DNS-маршрут удален"
                        dnsRouteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { dnsRouteToDelete = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = KeeneticColors.Error)
                    Text("Удалить маршрут?", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Text(
                    "Маршрут «${routeDestinationTitle(route)}» будет удалён с роутера.",
                    color = KeeneticColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isIpv6) viewModel.deleteIpv6StaticRoute(route) else viewModel.deleteStaticRoute(route)
                        feedbackMessage = "Маршрут удален"
                        routeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("Отмена", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RouteCard(
    route: StaticRoute,
    isIpv6: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (route.type) {
                    "host" -> Icons.Default.Dns
                    "default" -> Icons.Default.Public
                    else -> Icons.Default.Link
                },
                contentDescription = null,
                tint = KeeneticColors.Primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    routeDestinationTitle(route),
                    style = MaterialTheme.typography.titleMedium,
                    color = KeeneticColors.TextPrimary
                )
                Text(
                    "Шлюз: ${if (route.gateway.isNotBlank()) route.gateway else "авто"}" +
                        (if (route.interfaceName.isNotBlank()) " · ${route.interfaceName}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
                if (route.comment.isNotBlank()) {
                    Text(
                        route.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextPrimary.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = route.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = KeeneticColors.TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeeneticColors.Error)
            }
        }
    }
}

private fun routeDestinationTitle(route: StaticRoute): String = when (route.type) {
    "host" -> "Хост ${route.network}"
    "default" -> "По умолчанию"
    "node" -> "Сеть ${route.prefix.ifBlank { route.network }}"
    else -> "${route.network} / ${route.mask}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteEditorDialog(
    ipv6: Boolean,
    initial: StaticRoute?,
    interfaces: List<String>,
    onDismiss: () -> Unit,
    onSave: (StaticRoute) -> Unit
) {
    val from = initial
    var type by remember(from, ipv6) { mutableStateOf(from?.type ?: if (ipv6) "node" else "network") }
    var destination by remember(from, ipv6) { mutableStateOf(from?.network ?: "") }
    var mask by remember(from, ipv6) { mutableStateOf(from?.mask?.takeIf { !ipv6 } ?: "255.255.255.0") }
    var prefix by remember(from, ipv6) { mutableStateOf(from?.prefix ?: "") }
    var gateway by remember(from, ipv6) { mutableStateOf(from?.gateway ?: "") }
    var iface by remember(from, ipv6) { mutableStateOf(from?.interfaceName ?: "") }
    var comment by remember(from, ipv6) { mutableStateOf(from?.comment ?: "") }
    var enabled by remember(from, ipv6) { mutableStateOf(from?.enabled ?: true) }
    var ifaceExpanded by remember { mutableStateOf(false) }

    val types = if (ipv6) listOf("node", "default") else listOf("host", "network", "default")
    val typeLabels = mapOf(
        "host" to "Хост",
        "network" to "Сеть",
        "node" to "Сеть",
        "default" to "По умолчанию"
    )

    fun build(): StaticRoute {
        val newType = type
        val isId = from?.index ?: ""
        return when {
            newType == "host" -> StaticRoute(
                id = isId.ifBlank { destination },
                network = destination, mask = "", gateway = gateway,
                interfaceName = iface, comment = comment,
                index = isId, type = "host", enabled = enabled
            )
            newType == "default" -> StaticRoute(
                id = isId.ifBlank { "default_$iface" },
                network = "", mask = "", gateway = gateway,
                interfaceName = iface, comment = comment,
                index = isId, type = "default", enabled = enabled
            )
            ipv6 -> StaticRoute(
                id = isId.ifBlank { prefix },
                network = "", mask = "", gateway = gateway,
                interfaceName = iface, comment = comment,
                index = isId, type = "node", prefix = prefix, enabled = enabled
            )
            else -> StaticRoute(
                id = isId.ifBlank { "$destination/$mask" },
                network = destination, mask = mask, gateway = gateway,
                interfaceName = iface, comment = comment,
                index = isId, type = "network", enabled = enabled
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Route, contentDescription = null, tint = KeeneticColors.Primary)
                Text(
                    if (initial != null) "Редактирование маршрута" else if (ipv6) "Добавить IPv6-маршрут" else "Добавить статический маршрут",
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(typeLabels[t] ?: t) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KeeneticColors.Primary,
                                selectedLabelColor = KeeneticColors.Background,
                                containerColor = KeeneticColors.Surface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                if (type == "host") {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Адрес хоста") },
                        placeholder = { Text("8.8.8.8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider
                        )
                    )
                }

                if (type == "network") {
                    if (ipv6) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("Префикс сети (CIDR)") },
                            placeholder = { Text("2001:db8::/48") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KeeneticColors.Primary,
                                unfocusedBorderColor = KeeneticColors.Divider
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Сеть назначения") },
                            placeholder = { Text("10.8.0.0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KeeneticColors.Primary,
                                unfocusedBorderColor = KeeneticColors.Divider
                            )
                        )
                        OutlinedTextField(
                            value = mask,
                            onValueChange = { mask = it },
                            label = { Text("Маска подсети") },
                            placeholder = { Text("255.255.255.0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KeeneticColors.Primary,
                                unfocusedBorderColor = KeeneticColors.Divider
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = gateway,
                    onValueChange = { gateway = it },
                    label = { Text(if (ipv6) "Шлюз (Gateway, опционально)" else "Шлюз (Gateway, опционально)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = ifaceExpanded,
                    onExpandedChange = { ifaceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = iface,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Интерфейс") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ifaceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = ifaceExpanded,
                        onDismissRequest = { ifaceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Авто") },
                            onClick = { iface = "Auto"; ifaceExpanded = false }
                        )
                        interfaces.forEach { id ->
                            DropdownMenuItem(
                                text = { Text(id) },
                                onClick = { iface = id; ifaceExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Описание") },
                    placeholder = { Text("Напр. VPN сеть офиса") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Маршрут включён", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = when (type) {
                        "host" -> destination.isNotBlank()
                        "network" -> if (ipv6) prefix.isNotBlank() else destination.isNotBlank() && mask.isNotBlank()
                        else -> true
                    }
                    if (valid) onSave(build())
                },
                enabled = when (type) {
                    "host" -> destination.isNotBlank()
                    "network" -> if (ipv6) prefix.isNotBlank() else destination.isNotBlank() && mask.isNotBlank()
                    else -> true
                },
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (initial != null) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = KeeneticColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun DnsRouteCard(
    route: DnsRouteData,
    groupDescription: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                tint = KeeneticColors.Primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    route.group,
                    style = MaterialTheme.typography.titleMedium,
                    color = KeeneticColors.TextPrimary
                )
                if (!groupDescription.isNullOrBlank() && groupDescription != route.group) {
                    Text(
                        groupDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }
                Text(
                    "Шлюз: ${if (route.gateway.isNotBlank()) route.gateway else "авто"}" +
                        (if (route.interfaceName.isNotBlank()) " · ${route.interfaceName}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeneticColors.TextSecondary
                )
            }
            Switch(
                checked = route.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = KeeneticColors.TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeeneticColors.Error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsRouteEditorDialog(
    initial: DnsRouteData?,
    groups: List<FqdnGroup>,
    interfaces: List<String>,
    onDismiss: () -> Unit,
    onSave: (DnsRouteData) -> Unit,
    onCreateGroup: (String, String, List<String>) -> Unit
) {
    var groupName by remember(initial) { mutableStateOf(initial?.group ?: "") }
    var gateway by remember(initial) { mutableStateOf(initial?.gateway ?: "") }
    var iface by remember(initial) { mutableStateOf(initial?.interfaceName ?: "") }
    var enabled by remember(initial) { mutableStateOf(initial?.enabled ?: true) }
    var groupExpanded by remember { mutableStateOf(false) }
    var ifaceExpanded by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }

    if (showCreateGroup) {
        CreateGroupDialog(
            onCreate = { name, description, domains ->
                onCreateGroup(name, description, domains)
                showCreateGroup = false
            },
            onDismiss = { showCreateGroup = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Public, contentDescription = null, tint = KeeneticColors.Primary)
                Text(
                    if (initial != null) "Редактирование DNS-маршрута" else "Добавить DNS-маршрут",
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (groups.isEmpty()) {
                    Text(
                        "Список доменных имён пока пуст. Создайте список, затем выберите его для маршрута.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = it }
                ) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Список доменных имён") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = { groupName = group.name; groupExpanded = false }
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { showCreateGroup = true },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Создать список доменных имён")
                }

                OutlinedTextField(
                    value = gateway,
                    onValueChange = { gateway = it },
                    label = { Text("Шлюз (Gateway, опционально)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = ifaceExpanded,
                    onExpandedChange = { ifaceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = iface,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Интерфейс") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ifaceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KeeneticColors.Primary,
                            unfocusedBorderColor = KeeneticColors.Divider
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = ifaceExpanded,
                        onDismissRequest = { ifaceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Авто") },
                            onClick = { iface = "Auto"; ifaceExpanded = false }
                        )
                        interfaces.forEach { id ->
                            DropdownMenuItem(
                                text = { Text(id) },
                                onClick = { iface = id; ifaceExpanded = false }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Маршрут включён", style = MaterialTheme.typography.bodyMedium, color = KeeneticColors.TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KeeneticColors.Primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DnsRouteData(
                            id = initial?.id ?: groupName,
                            index = initial?.index ?: "",
                            group = groupName,
                            gateway = gateway,
                            interfaceName = iface,
                            reject = initial?.reject ?: false,
                            enabled = enabled
                        )
                    )
                },
                enabled = groupName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (initial != null) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = KeeneticColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun CreateGroupDialog(
    onCreate: (String, String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var domains by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                Text("Новый список доменов", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя списка") },
                    placeholder = { Text("Напр. office-resources") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )
                OutlinedTextField(
                    value = domains,
                    onValueChange = { domains = it },
                    label = { Text("Домены (по одному на строку)") },
                    placeholder = { Text("example.com\nyoutube.com") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KeeneticColors.Primary,
                        unfocusedBorderColor = KeeneticColors.Divider
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name.trim(),
                        description.trim(),
                        domains.split('\n').map { it.trim() }.filter { it.isNotBlank() }
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = KeeneticColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun CurrentRoutesSection(entries: List<RouterRouteEntry>, title: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KeeneticColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = KeeneticColors.TextSecondary
                )
            }
            Text(
                "Таблица маршрутов, фактически используемая роутером. Пользовательские маршруты редактируются выше.",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
            if (expanded) {
                HorizontalDivider(color = KeeneticColors.Divider)
                if (entries.isEmpty()) {
                    Text(
                        "Нет данных",
                        style = MaterialTheme.typography.bodySmall,
                        color = KeeneticColors.TextSecondary
                    )
                } else {
                    entries.take(40).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.destination,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = KeeneticColors.TextPrimary
                                )
                                Text(
                                    (if (entry.gateway.isNotBlank()) entry.gateway else "—") +
                                        (if (entry.interfaceName.isNotBlank()) " · ${entry.interfaceName}" else "") +
                                        (if (entry.isStatic) " · статический" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}