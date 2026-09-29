package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.DnsRouteData
import com.keenetic.local.api.FqdnGroup
import com.keenetic.local.api.RouterRouteEntry
import com.keenetic.local.api.StaticRoute
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EditableRow
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.OptionPickerDialog
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
import com.keenetic.local.ui.theme.KeeneticColors

private val ROUTE_TABS = listOf("IPv4-маршруты", "IPv6-маршруты", "DNS-маршруты")

@Composable
fun StaticRoutesScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val routes by viewModel.staticRoutes.collectAsState()
    val ipv6Routes by viewModel.ipv6StaticRoutes.collectAsState()
    val dnsRoutes by viewModel.dnsRoutes.collectAsState()
    val fqdnGroups by viewModel.fqdnGroups.collectAsState()
    val currentIpv4Routes by viewModel.currentIpv4Routes.collectAsState()
    val currentIpv6Routes by viewModel.currentIpv6Routes.collectAsState()
    val viaInterfaces by viewModel.vpnViaInterfaces.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
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
    val currentRoutes = if (isIpv6) ipv6Routes else routes

    fun addRoute() {
        editingRoute = null
        editingDnsRoute = null
        showEditor = true
    }

    SectionScaffold(
        title = "Статическая маршрутизация",
        subtitle = "IPv4, IPv6 и доменные маршруты",
        onBack = onBack,
        onRefresh = {
            when (selectedTab) {
                0 -> viewModel.loadStaticRoutes()
                1 -> viewModel.loadIpv6StaticRoutes()
                else -> viewModel.loadDnsRoutes()
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "tabs") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ROUTE_TABS.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KeeneticColors.Primary.copy(alpha = 0.15f),
                                selectedLabelColor = KeeneticColors.Primary
                            )
                        )
                    }
                }
            }

            item(key = "add") {
                SectionCard {
                    OutlinedButton(
                        onClick = { addRoute() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = KeeneticColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить маршрут", color = KeeneticColors.Primary)
                    }
                }
            }

            if (feedbackMessage != null) {
                item(key = "feedback") {
                    SectionCard {
                        Text(
                            feedbackMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeeneticColors.TextSecondary
                        )
                    }
                }
            }

            if (isDns) {
                if (dnsRoutes.isEmpty()) {
                    item(key = "dns-empty") {
                        SectionCard {
                            EmptyHint(
                                "Доменные маршруты не настроены. Трафик к доменным именам из " +
                                    "выбранного списка будет направляться через указанный интерфейс. " +
                                    "Списки доменов создаются в разделе «Контентная фильтрация»."
                            )
                        }
                    }
                } else {
                    item(key = "dns-header") {
                        SubGroupHeader("Доменные маршруты", dnsRoutes.size)
                    }
                    items(dnsRoutes) { route ->
                        DnsRouteCard(
                            route = route,
                            groupDescription = fqdnGroups.firstOrNull { it.name == route.group }?.description,
                            onToggle = { enabled -> viewModel.toggleDnsRoute(route, enabled) },
                            onEdit = { editingDnsRoute = route; showEditor = true },
                            onDelete = { dnsRouteToDelete = route }
                        )
                    }
                }
            } else {
                if (currentRoutes.isEmpty()) {
                    item(key = "empty") {
                        SectionCard {
                            EmptyHint(
                                if (isIpv6) {
                                    "IPv6-маршруты не настроены. Используются маршруты по умолчанию, " +
                                        "полученные от интернет-провайдера."
                                } else {
                                    "Статические маршруты не настроены. Используются маршруты по " +
                                        "умолчанию, полученные от интернет-провайдера (DHCP/PPPoE/IPoE)."
                                }
                            )
                        }
                    }
                } else {
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
                }
                item(key = "current") {
                    CurrentRoutesSection(
                        entries = if (isIpv6) currentIpv6Routes else currentIpv4Routes,
                        title = if (isIpv6) "Текущие маршруты IPv6 (роутер)" else "Текущие маршруты IPv4 (роутер)"
                    )
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
                        "DNS-маршрут обновлён"
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
                        "Маршрут обновлён"
                    else
                        "Маршрут ${routeDestinationTitle(route)} добавлен"
                    showEditor = false
                    editingRoute = null
                }
            )
        }
    }

    dnsRouteToDelete?.let { route ->
        ConfirmDialog(
            title = "Удалить DNS-маршрут?",
            message = "Доменный маршрут «${route.group}» будет удалён с роутера.",
            onConfirm = {
                viewModel.deleteDnsRoute(route)
                feedbackMessage = "DNS-маршрут удалён"
                dnsRouteToDelete = null
            },
            onDismiss = { dnsRouteToDelete = null }
        )
    }

    routeToDelete?.let { route ->
        ConfirmDialog(
            title = "Удалить маршрут?",
            message = "Маршрут «${routeDestinationTitle(route)}» будет удалён с роутера.",
            onConfirm = {
                if (isIpv6) viewModel.deleteIpv6StaticRoute(route) else viewModel.deleteStaticRoute(route)
                feedbackMessage = "Маршрут удалён"
                routeToDelete = null
            },
            onDismiss = { routeToDelete = null }
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
    SectionCard(
        title = routeDestinationTitle(route),
        subtitle = if (isIpv6) "IPv6" else "IPv4",
        icon = when (route.type) {
            "host" -> Icons.Default.Dns
            "default" -> Icons.Default.Public
            else -> Icons.Default.Link
        }
    ) {
        InfoRow("Шлюз", if (route.gateway.isNotBlank()) route.gateway else "авто", monospace = true)
        if (route.interfaceName.isNotBlank()) {
            InfoRow("Интерфейс", route.interfaceName, monospace = true)
        }
        if (route.comment.isNotBlank()) {
            InfoRow("Описание", route.comment)
        }
        RowDivider()
        SwitchRow(
            label = "Маршрут включён",
            checked = route.enabled,
            onCheckedChange = onToggle
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Изменить", color = KeeneticColors.Primary)
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = KeeneticColors.Error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Удалить", color = KeeneticColors.Error)
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
    var ifacePicker by remember { mutableStateOf(false) }

    val types = if (ipv6) listOf("node", "default") else listOf("host", "network", "default")
    val typeLabels = mapOf(
        "host" to "Хост",
        "network" to "Сеть",
        "node" to "Сеть",
        "default" to "По умолчанию"
    )
    val ifaceOptions = listOf("Auto" to "Авто") + interfaces.map { it to it }

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

    val valid = when (type) {
        "host" -> destination.isNotBlank()
        "network" -> if (ipv6) prefix.isNotBlank() else destination.isNotBlank() && mask.isNotBlank()
        else -> true
    }

    FormDialog(
        title = if (initial != null) "Редактирование маршрута" else if (ipv6) "Добавить IPv6-маршрут" else "Добавить статический маршрут",
        confirmLabel = if (initial != null) "Сохранить" else "Добавить",
        confirmEnabled = valid,
        onDismiss = onDismiss,
        onConfirm = { if (valid) onSave(build()) }
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(typeLabels[t] ?: t) }
                )
            }
        }

        if (type == "host") {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Адрес хоста") },
                placeholder = { Text("8.8.8.8") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Сеть назначения") },
                    placeholder = { Text("10.8.0.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mask,
                    onValueChange = { mask = it },
                    label = { Text("Маска подсети") },
                    placeholder = { Text("255.255.255.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        OutlinedTextField(
            value = gateway,
            onValueChange = { gateway = it },
            label = { Text("Шлюз (Gateway, опционально)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        EditableRow(
            label = "Интерфейс",
            value = iface.ifBlank { "Авто" },
            onClick = { ifacePicker = true }
        )

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Описание") },
            placeholder = { Text("Напр. VPN сеть офиса") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SwitchRow(
            label = "Маршрут включён",
            checked = enabled,
            onCheckedChange = { enabled = it }
        )
    }

    if (ifacePicker) {
        OptionPickerDialog(
            title = "Интерфейс",
            options = ifaceOptions,
            selectedKey = iface,
            onSelect = { iface = it },
            onDismiss = { ifacePicker = false }
        )
    }
}

@Composable
private fun DnsRouteCard(
    route: DnsRouteData,
    groupDescription: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(
        title = route.group,
        icon = Icons.Default.Public
    ) {
        if (!groupDescription.isNullOrBlank() && groupDescription != route.group) {
            InfoRow("Список", groupDescription)
        }
        InfoRow("Шлюз", if (route.gateway.isNotBlank()) route.gateway else "авто", monospace = true)
        if (route.interfaceName.isNotBlank()) {
            InfoRow("Интерфейс", route.interfaceName, monospace = true)
        }
        RowDivider()
        SwitchRow(
            label = "Маршрут включён",
            checked = route.enabled,
            onCheckedChange = onToggle
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Изменить", color = KeeneticColors.Primary)
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = KeeneticColors.Error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Удалить", color = KeeneticColors.Error)
            }
        }
    }
}

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
    var groupPicker by remember { mutableStateOf(false) }
    var ifacePicker by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }

    val groupOptions = groups.map { it.name to it.name }
    val ifaceOptions = listOf("Auto" to "Авто") + interfaces.map { it to it }

    if (showCreateGroup) {
        CreateGroupDialog(
            onCreate = { name, description, domains ->
                onCreateGroup(name, description, domains)
                showCreateGroup = false
            },
            onDismiss = { showCreateGroup = false }
        )
    }

    FormDialog(
        title = if (initial != null) "Редактирование DNS-маршрута" else "Добавить DNS-маршрут",
        confirmLabel = if (initial != null) "Сохранить" else "Добавить",
        confirmEnabled = groupName.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
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
        }
    ) {
        if (groups.isEmpty()) {
            Text(
                "Список доменных имён пока пуст. Создайте список, затем выберите его для маршрута.",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }

        EditableRow(
            label = "Список доменных имён",
            value = groupName.ifBlank { "не выбран" },
            onClick = { groupPicker = true }
        )

        TextButton(onClick = { showCreateGroup = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Создать список доменных имён", color = KeeneticColors.Primary)
        }

        OutlinedTextField(
            value = gateway,
            onValueChange = { gateway = it },
            label = { Text("Шлюз (Gateway, опционально)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        EditableRow(
            label = "Интерфейс",
            value = iface.ifBlank { "Авто" },
            onClick = { ifacePicker = true }
        )

        SwitchRow(
            label = "Маршрут включён",
            checked = enabled,
            onCheckedChange = { enabled = it }
        )
    }

    if (groupPicker) {
        OptionPickerDialog(
            title = "Список доменных имён",
            options = groupOptions,
            selectedKey = groupName,
            onSelect = { groupName = it },
            onDismiss = { groupPicker = false }
        )
    }

    if (ifacePicker) {
        OptionPickerDialog(
            title = "Интерфейс",
            options = ifaceOptions,
            selectedKey = iface,
            onSelect = { iface = it },
            onDismiss = { ifacePicker = false }
        )
    }
}

@Composable
private fun CreateGroupDialog(
    onCreate: (String, String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var domains by remember { mutableStateOf("") }

    FormDialog(
        title = "Новый список доменов",
        confirmLabel = "Создать",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            onCreate(
                name.trim(),
                description.trim(),
                domains.split('\n').map { it.trim() }.filter { it.isNotBlank() }
            )
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя списка") },
            placeholder = { Text("Напр. office-resources") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание (опционально)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = domains,
            onValueChange = { domains = it },
            label = { Text("Домены (по одному на строку)") },
            placeholder = { Text("example.com\nyoutube.com") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
        )
    }
}

@Composable
private fun CurrentRoutesSection(entries: List<RouterRouteEntry>, title: String) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard(
        title = title,
        trailing = {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Скрыть" else "Показать", color = KeeneticColors.Primary)
            }
        }
    ) {
        Text(
            "Таблица маршрутов, фактически используемая роутером. Пользовательские " +
                "маршруты редактируются выше.",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary
        )
        if (expanded) {
            RowDivider()
            if (entries.isEmpty()) {
                EmptyHint("Нет данных")
            } else {
                entries.take(40).forEach { entry ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
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
