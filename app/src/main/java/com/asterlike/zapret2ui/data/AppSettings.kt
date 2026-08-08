package com.asterlike.zapret2ui.data

import kotlinx.serialization.Serializable

/**
 * Порт AppSettings.cs — все настройки хранятся в DataStore (JSON).
 * Ключи 1:1 с десктопом для совместимости импорта.
 */
@Serializable
data class AppSettings(
    val activePresetName: String? = null,
    val activeHostlist: String? = null,
    val autoUpdateEngine: Boolean = true,
    val autostart: Boolean = false,
    val autostartEngine: Boolean = false,
    val minimizeToTray: Boolean = true, // на Android → сворачивать в уведомление
    val startMinimized: Boolean = false,
    val simpleMode: Boolean = true,
    val autoHeal: Boolean = false,
    val gameFilter: Boolean = false,
    val bypassAllSites: Boolean = false,
    val disableQuic: Boolean = false,
    val tgProxyCoverage: Boolean = false,
    val debugLog: Boolean = false,
    val networkStrategies: Map<String, String> = emptyMap(),
    val tgProxyPort: Int = 1443,
    val tgProxySecret: String = "",
    val tgProxyAutostart: Boolean = false,
    val uiScale: Float = 1.0f,
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val donateCollapsed: Boolean = false,
    val welcomeShown: Boolean = false,
    // Android-specific
    val vpnExcludeApps: List<String> = emptyList(), // пакеты, идущие в обход VPN (игры/банки)
    val batteryOptimizationIgnored: Boolean = false,
    val themeMode: String = "system", // system / light / dark / amoled
    val useRoot: Boolean = false, // если есть root — использовать nfqws напрямую, а не VPNService
    val autoStartOnBoot: Boolean = false
)
