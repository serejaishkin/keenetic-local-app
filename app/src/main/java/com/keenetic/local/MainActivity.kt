package com.keenetic.local

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keenetic.local.ui.KeeneticNavHost
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.theme.KeeneticColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeeneticAppTheme()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeeneticAppTheme() {
    val navController = rememberNavController()
    val viewModel: RouterViewModel = viewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isCheckingAutoLogin by viewModel.isCheckingAutoLogin.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomItems = listOf(
        Screen.Dashboard to Icons.Default.Dashboard,
        Screen.Devices to Icons.Default.Devices,
        Screen.WiFi to Icons.Default.Wifi,
        Screen.Terminal to Icons.Default.Terminal,
        Screen.Settings to Icons.Default.Settings
    )

    // Пока проверяем сохранённые данные для автовхода, не показываем
    // ни форму логина, ни основной интерфейс - только загрузку.
    if (isCheckingAutoLogin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = KeeneticColors.Primary)
        }
        return
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && currentRoute == Screen.Login.route) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isLoggedIn) {
                TopAppBar(
                    title = {
                        Text(
                            bottomItems.find { it.first.route == currentRoute }?.first?.title ?: "Keenetic Local",
                            color = KeeneticColors.TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KeeneticColors.Surface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(Icons.Default.Refresh, "Обновить")
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.Logout, "Выйти")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isLoggedIn) {
                NavigationBar(
                    containerColor = KeeneticColors.Surface,
                    tonalElevation = 2.dp
                ) {
                    bottomItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KeeneticColors.Primary,
                                selectedTextColor = KeeneticColors.Primary,
                                indicatorColor = KeeneticColors.Primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            KeeneticNavHost(navController = navController, viewModel = viewModel)
        }
    }
}