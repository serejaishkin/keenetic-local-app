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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.api.RouterUserAccount
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun UserAccountsScreen(viewModel: RouterViewModel, onBack: () -> Unit = {}) {
    val users by viewModel.userAccounts.collectAsState()
    var selectedUser by remember { mutableStateOf<RouterUserAccount?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSuperuser by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KeeneticColors.Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = KeeneticColors.Background)
            }
        },
        containerColor = KeeneticColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = KeeneticColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Пользователи и доступ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KeeneticColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = KeeneticColors.Primary)
                    }
                }
            }

            if (feedbackMessage != null) {
                item {
                    Snackbar(
                        action = {
                            TextButton(onClick = { feedbackMessage = null }) {
                                Text("OK", color = KeeneticColors.Primary)
                            }
                        }
                    ) {
                        Text(feedbackMessage ?: "")
                    }
                }
            }

            if (users.isEmpty()) {
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
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = KeeneticColors.TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Учетные записи не загружены",
                                style = MaterialTheme.typography.titleMedium,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Нажмите кнопку обновления или создайте нового пользователя с правами доступа к веб-конфигуратору, VPN или сетевым дискам.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KeeneticColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadUsers() },
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = KeeneticColors.Primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Обновить список", color = KeeneticColors.Primary)
                            }
                        }
                    }
                }
            } else {
                items(users) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUser = user },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = KeeneticColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary)
                                Text(
                                    if (user.permissions.isNotEmpty()) user.permissions.joinToString(", ") else "Пользователь без спец. прав",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                            if (user.tags.contains("admin")) {
                                Surface(
                                    color = KeeneticColors.Primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(14.dp))
                                        Text("Admin", color = KeeneticColors.Primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // User Sub-items and Permissions Dialog
    selectedUser?.let { user ->
        var newPass by remember(user.name) { mutableStateOf("") }
        var hasAdmin by remember(user.name) { mutableStateOf(user.tags.contains("admin")) }
        var hasVpn by remember(user.name) { mutableStateOf(user.permissions.any { it.contains("vpn", ignoreCase = true) }) }
        var hasSmb by remember(user.name) { mutableStateOf(user.permissions.any { it.contains("cifs", ignoreCase = true) || it.contains("smb", ignoreCase = true) }) }

        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = KeeneticColors.Primary)
                    Text("Управление: ${user.name}", fontWeight = FontWeight.Bold, color = KeeneticColors.TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Новый пароль (оставьте пустым если не менять)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Права доступа пользователя:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = KeeneticColors.TextPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Управление роутером (CLI / Web)", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        Switch(checked = hasAdmin, onCheckedChange = { hasAdmin = it }, enabled = user.name != "admin")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Доступ к VPN-серверам", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        Switch(checked = hasVpn, onCheckedChange = { hasVpn = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сетевой диск и USB (SMB/FTP)", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary)
                        Switch(checked = hasSmb, onCheckedChange = { hasSmb = it })
                    }

                    if (user.name != "admin") {
                        HorizontalDivider(color = KeeneticColors.Divider)
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteUserAccount(user.name)
                                feedbackMessage = "Пользователь «${user.name}» удален"
                                selectedUser = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Удалить пользователя")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.isNotBlank()) {
                            viewModel.createUserAccount(user.name, newPass, hasAdmin, hasVpn, hasSmb)
                            feedbackMessage = "Настройки и пароль для «${user.name}» обновлены"
                        } else {
                            feedbackMessage = "Права доступа для «${user.name}» сохранены"
                        }
                        selectedUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUser = null }) {
                    Text("Закрыть", color = KeeneticColors.TextSecondary)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый пользователь", color = KeeneticColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Имя пользователя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSuperuser, onCheckedChange = { isSuperuser = it })
                        Text("Права администратора", color = KeeneticColors.TextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            viewModel.createUserAccount(username, password, isSuperuser, true, true)
                            feedbackMessage = "Пользователь «$username» создан!"
                            username = ""
                            password = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
