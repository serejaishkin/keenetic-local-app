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
    var ip by remember { mutableStateOf("192.168.1.1") }
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

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP адрес роутера") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
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
            onClick = { viewModel.login(password, ip, login) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && password.isNotBlank()
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