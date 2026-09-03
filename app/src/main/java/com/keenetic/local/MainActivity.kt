package com.keenetic.local

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keenetic.local.ui.KeeneticNavHost
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.screens.LoginScreen
import com.keenetic.local.ui.theme.KeeneticAppTheme
import com.keenetic.local.ui.theme.KeeneticColors
import com.keenetic.local.util.AppLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.logAction("App started")
        setContent {
            KeeneticAppTheme {
                MainAppContent()
            }
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val viewModel: RouterViewModel = viewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomNavItem(Screen.Dashboard, "Главная", Icons.Default.Dashboard),
        BottomNavItem(Screen.Devices, "Устройства", Icons.Default.Devices),
        BottomNavItem(Screen.Internet, "Интернет", Icons.Default.Public),
        BottomNavItem(Screen.WiFi, "Wi-Fi", Icons.Default.Wifi),
        BottomNavItem(Screen.AllSections, "Разделы", Icons.Default.GridView)
    )

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when (currentRoute) {
                            Screen.Dashboard.route -> Screen.Dashboard.title
                            Screen.Devices.route -> Screen.Devices.title
                            Screen.Internet.route -> Screen.Internet.title
                            Screen.WiFi.route -> Screen.WiFi.title
                            Screen.AllSections.route -> Screen.AllSections.title
                            Screen.PortForwarding.route -> Screen.PortForwarding.title
                            Screen.Firewall.route -> Screen.Firewall.title
                            Screen.StaticRoutes.route -> Screen.StaticRoutes.title
                            Screen.LanSegments.route -> Screen.LanSegments.title
                            Screen.Mobile.route -> Screen.Mobile.title
                            Screen.UsbDevices.route -> Screen.UsbDevices.title
                            Screen.UserAccounts.route -> Screen.UserAccounts.title
                            Screen.SystemLogs.route -> Screen.SystemLogs.title
                            Screen.Firmware.route -> Screen.Firmware.title
                            Screen.Diagnostics.route -> Screen.Diagnostics.title
                            Screen.DnsFilters.route -> Screen.DnsFilters.title
                            Screen.VpnAdvanced.route -> Screen.VpnAdvanced.title
                            else -> "Keenetic Local"
                        }
                        Text(
                            title,
                            color = KeeneticColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        val isRootTab = bottomItems.any { it.screen.route == currentRoute }
                        if (!isRootTab) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = KeeneticColors.TextPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(Icons.Default.Refresh, "Обновить", tint = KeeneticColors.TextSecondary)
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, "Выйти", tint = KeeneticColors.TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KeeneticColors.Surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = KeeneticColors.Surface
                ) {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KeeneticColors.Primary,
                                selectedTextColor = KeeneticColors.Primary,
                                unselectedIconColor = KeeneticColors.TextSecondary,
                                unselectedTextColor = KeeneticColors.TextSecondary,
                                indicatorColor = KeeneticColors.Primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            containerColor = KeeneticColors.Background
        ) { innerPadding ->
            KeeneticNavHost(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(KeeneticColors.Background)
            )
        }
    }
}
