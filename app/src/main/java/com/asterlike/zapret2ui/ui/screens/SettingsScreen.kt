package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState(initial = com.asterlike.zapret2ui.data.AppSettings())
    // Use DataStore update via vm — simplified switches
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        SettingsSection("Обход") {
            SwitchRow("Игровой фильтр", "Расширить захват на все высокие UDP-порты (для игр)", s.gameFilter, {})
            SwitchRow("Обход всех сайтов", "Весь TLS/QUIC кроме exclude (иначе только списки)", s.bypassAllSites, {})
            SwitchRow("Отключить QUIC", "Дропать QUIC → откат на TCP (если YouTube буферит)", s.disableQuic, {})
            SwitchRow("Прикрывать Telegram-прокси", "Дополнительно desync для Cloudflare-фронтов", s.tgProxyCoverage, {})
            SwitchRow("Подробный лог (--debug)", "Перезапускает движок", s.debugLog, {})
        }
        SettingsSection("Приложение") {
            SwitchRow("Простой режим", "Одна кнопка vs вкладки", s.simpleMode, {})
            SwitchRow("Авто-починка", "Следить и переподбирать при сбое", s.autoHeal, {})
            SwitchRow("Автозапуск при загрузке", "Запускать с системой", s.autoStartOnBoot, {})
            SwitchRow("Уведомления", "Тосты при старте/остановке", s.notificationsEnabled, {})
            SwitchRow("Использовать root (если есть)", "nfqws напрямую вместо VPN", s.useRoot, {})
        }
        SettingsSection("Автообновление") {
            SwitchRow("Авто-обновление движка", "Тихо обновлять zapret2 из релизов", s.autoUpdateEngine, {})
            Text("Движок проверяется по SHA256, как в оригинале. При первом запуске скачивается.", style = MaterialTheme.typography.labelSmall)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("О приложении", style = MaterialTheme.typography.titleSmall)
                Text("Zapret2UI Android 1.0.0 — форк Asterlike/zapret2UI для Android.\nДвижок: bol-van/zapret2 (nfqws)\nСтратегии: Flowseal + рекомендуемые комбо\nЛицензия: MIT", style = MaterialTheme.typography.bodySmall)
                Text("Исходники: github.com/Asterlike/zapret2UI (десктоп) → этот репо (Android)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
