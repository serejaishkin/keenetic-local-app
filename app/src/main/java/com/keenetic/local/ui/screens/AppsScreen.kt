package com.keenetic.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenetic.local.ui.*
import com.keenetic.local.ui.theme.KeeneticColors

/**
 * Отдельный экран "Приложения" (opkg/торрент/IntelliQoS), раньше жил вместе
 * с системными настройками подключения в одном SettingsScreen - развели по
 * аналогии с реальным сайтом, где это разные разделы ("Управление →
 * Настройки системы" отдельно от "Управление → Приложения").