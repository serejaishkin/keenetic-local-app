package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.theme.KeeneticColors

@Composable
fun LoginScreen(viewModel: RouterViewModel, onLoginSuccess: () -> Unit) {
    var ip1 by remember { mutableStateOf("192") }
    var ip2 by remember { mutableStateOf("168") }
    var ip3 by remember { mutableStateOf("1") }
    var ip4 by remember { mutableStateOf("1") }
    var login by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val hasSavedPassword by viewModel.hasSavedPassword.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Router,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = KeeneticColors.Primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Keenetic Local",
            style = MaterialTheme.typography.headlineMedium,
            color = KeeneticColors.TextPrimary
        )
        Text(
            text = "Управление роутером без облака",
            style = MaterialTheme.typography.bodyMedium,
            color = KeeneticColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "IP адрес роутера",
            style = MaterialTheme.typography.bodySmall,
            color = KeeneticColors.TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ip1,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter(Char::isDigit).take(3)
                    ip1 = sanitized
                },
                placeholder = { Text("192") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ip2,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter(Char::isDigit).take(3)
                    ip2 = sanitized
                },
                placeholder = { Text("168") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ip3,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter(Char::isDigit).take(3)
                    ip3 = sanitized
                },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ip4,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter(Char::isDigit).take(3)
                    ip4 = sanitized
                },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            placeholder = { Text(if (hasSavedPassword) "Пароль сохранён, поле можно оставить пустым" else "Введите пароль") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (hasSavedPassword) {
            Text(
                text = "Сохранённый пароль доступен — можно просто нажать «Подключиться»",
                style = MaterialTheme.typography.bodySmall,
                color = KeeneticColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(
                text = error ?: "",
                color = KeeneticColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = { viewModel.login(password, "$ip1.$ip2.$ip3.$ip4", login) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && (password.isNotBlank() || hasSavedPassword) && login.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Подключиться")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.discoverRouters() }) {
            Text("Найти роутер в сети")
        }
    }
}