package com.asterlike.zapret2ui.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.asterlike.zapret2ui.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Home : Screen("home", "Главная", "🏠")
    data object Strategies : Screen("strategies", "Стратегии", "🧩")
    data object Hostlists : Screen("hostlists", "Хостлисты", "📋")
    data object Diagnostics : Screen("diagnostics", "Диагностика", "🔍")
    data object Log : Screen("log", "Журнал", "📜")
    data object Telegram : Screen("telegram", "Telegram", "✈️")
    data object Settings : Screen("settings", "Настройки", "⚙️")
}

val bottomTabs = listOf(Screen.Home, Screen.Strategies, Screen.Diagnostics, Screen.Telegram, Screen.Settings)
val allScreens = listOf(Screen.Home, Screen.Strategies, Screen.Hostlists, Screen.Diagnostics, Screen.Log, Screen.Telegram, Screen.Settings)

@Composable
fun ZapretNavGraph(navController: NavHostController, viewModel: com.asterlike.zapret2ui.MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(viewModel) }
        composable(Screen.Strategies.route) { StrategiesScreen(viewModel) }
        composable(Screen.Hostlists.route) { HostlistsScreen(viewModel) }
        composable(Screen.Diagnostics.route) { DiagnosticsScreen(viewModel) }
        composable(Screen.Log.route) { LogScreen(viewModel) }
        composable(Screen.Telegram.route) { TelegramScreen(viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
    }
}
