package com.keenetic.local.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.keenetic.local.api.RouterUserAccount
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.components.ConfirmDialog
import com.keenetic.local.ui.components.EmptyHint
import com.keenetic.local.ui.components.FormDialog
import com.keenetic.local.ui.components.InfoRow
import com.keenetic.local.ui.components.RowDivider
import com.keenetic.local.ui.components.SectionCard
import com.keenetic.local.ui.components.SectionScaffold
import com.keenetic.local.ui.components.SubGroupHeader
import com.keenetic.local.ui.components.SwitchRow
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
    var pendingDeleteUser by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    SectionScaffold(
        title = "Пользователи и доступ",
        subtitle = if (users.isEmpty()) "Учётные записи" else "Учётных записей: ${users.size}",
        onBack = onBack,
        onRefresh = { viewModel.loadUsers() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (feedbackMessage != null) {
                item(key = "feedback") {
                    SectionCard {
                        InfoRow(label = "Результат", value = feedbackMessage ?: "")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { feedbackMessage = null }) {
                                Text("OK", color = KeeneticColors.Primary)
                            }
                        }
                    }
                }
            }

            item(key = "add") {
                SectionCard {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить пользователя")
                    }
                }
            }

            if (users.isEmpty()) {
                item(key = "empty") {
                    SectionCard(title = "Учётные записи", icon = Icons.Default.Person) {
                        EmptyHint("Учетные записи не загружены. Нажмите кнопку обновления или создайте нового пользователя.")
                        Button(
                            onClick = { viewModel.loadUsers() },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Обновить список")
                        }
                    }
                }
            } else {
                item(key = "list_header") {
                    SectionCard(title = "Учётные записи", icon = Icons.Default.Person) {
                        SubGroupHeader(title = "Пользователи", count = users.size)
                    }
                }
                items(users, key = { it.name }) { user ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUser = user }
                    ) {
                        SectionCard(
                            title = user.name,
                            icon = Icons.Default.Person,
                            subtitle = if (user.permissions.isNotEmpty()) user.permissions.joinToString(", ") else "Пользователь без спец. прав",
                            trailing = {
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
                        ) {
                            InfoRow(
                                label = "Теги",
                                value = if (user.tags.isNotEmpty()) user.tags.joinToString(", ") else "—",
                                monospace = true
                            )
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
        // Rights live in the `tag` list reported by the router; `permissions` is only a
        // human-readable rendering of those tags, so the switches must be driven by tags.
        var hasVpn by remember(user.name) { mutableStateOf(user.tags.contains("vpn")) }
        var hasSmb by remember(user.name) { mutableStateOf(user.tags.contains("smb")) }
        var hasFtp by remember(user.name) { mutableStateOf(user.tags.contains("ftp")) }
        var hasMedia by remember(user.name) { mutableStateOf(user.tags.contains("media")) }

        FormDialog(
            title = "Управление: ${user.name}",
            confirmLabel = "Сохранить",
            onDismiss = { selectedUser = null },
            onConfirm = {
                viewModel.createUserAccount(
                    user.name,
                    newPass,
                    hasAdmin,
                    hasSmb,
                    hasVpn,
                    hasFtp,
                    hasMedia
                )
                feedbackMessage = if (newPass.isNotBlank()) "Пароль и права «${user.name}» сохранены" else "Права «${user.name}» сохранены"
                selectedUser = null
            }
        ) {
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("Новый пароль (оставьте пустым если не менять)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SubGroupHeader(title = "Права доступа пользователя")
            SwitchRow(
                label = "Управление роутером (CLI / Web)",
                checked = hasAdmin,
                onCheckedChange = { hasAdmin = it },
                enabled = user.name != "admin"
            )
            RowDivider()
            SwitchRow(
                label = "Доступ к VPN-серверам",
                checked = hasVpn,
                onCheckedChange = { hasVpn = it }
            )
            RowDivider()
            SwitchRow(
                label = "Сетевой диск и USB (SMB/FTP)",
                checked = hasSmb,
                onCheckedChange = { hasSmb = it }
            )
            RowDivider()
            SwitchRow(
                label = "Сетевой диск по FTP (FTP-доступ)",
                checked = hasFtp,
                onCheckedChange = { hasFtp = it }
            )
            RowDivider()
            SwitchRow(
                label = "Доступ к мультимедиа (DLNA)",
                checked = hasMedia,
                onCheckedChange = { hasMedia = it }
            )
            if (user.name != "admin") {
                RowDivider()
                OutlinedButton(
                    onClick = { pendingDeleteUser = user.name },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KeeneticColors.Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Удалить пользователя")
                }
            }
        }
    }

    pendingDeleteUser?.let { name ->
        ConfirmDialog(
            title = "Удалить пользователя?",
            message = "Пользователь «$name» будет удалён.",
            onConfirm = {
                viewModel.deleteUserAccount(name)
                feedbackMessage = "Пользователь «$name» удален"
                pendingDeleteUser = null
                selectedUser = null
            },
            onDismiss = { pendingDeleteUser = null }
        )
    }

    if (showAddDialog) {
        FormDialog(
            title = "Новый пользователь",
            confirmLabel = "Создать",
            onDismiss = { showAddDialog = false },
            onConfirm = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    viewModel.createUserAccount(username, password, isSuperuser, true, true)
                    feedbackMessage = "Пользователь «$username» создан!"
                    username = ""
                    password = ""
                    showAddDialog = false
                }
            },
            confirmEnabled = username.isNotBlank() && password.isNotBlank()
        ) {
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
            SwitchRow(
                label = "Права администратора",
                checked = isSuperuser,
                onCheckedChange = { isSuperuser = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
