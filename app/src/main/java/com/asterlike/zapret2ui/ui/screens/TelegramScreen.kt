package com.asterlike.zapret2ui.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.asterlike.zapret2ui.MainViewModel

@Composable
fun TelegramScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState(initial = com.asterlike.zapret2ui.data.AppSettings())
    val ctx = LocalContext.current
    val link = "tg://proxy?server=127.0.0.1&port=${settings.tgProxyPort}&secret=dd${settings.tgProxySecret.ifEmpty { "…" }}"

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Telegram прокси", style = MaterialTheme.typography.headlineSmall)
        Text("Обходит блокировку Telegram по IP — без VPN и без root. Локальный MTProto → WebSocket-TLS через Cloudflare.", style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("127.0.0.1:${settings.tgProxyPort}", style = MaterialTheme.typography.titleMedium)
                Text("Секрет: dd${settings.tgProxySecret.take(8)}…", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                    }) { Text("Открыть в Telegram") }
                    OutlinedButton(onClick = {
                        val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("proxy", link))
                    }) { Text("Копировать") }
                }
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text("Как включить:", style = MaterialTheme.typography.labelMedium)
                Text("1. Включите тумблер ниже\n2. Нажмите «Открыть в Telegram» — прокси пропишется сам\n3. Если не сработало: Настройки Telegram → Данные и память → Прокси → Добавить MTProto", style = MaterialTheme.typography.bodySmall)
            }
        }
        // Toggle would be here
        Text("Порт можно менять в Настройках. Смена порта перезапускает прокси.", style = MaterialTheme.typography.labelSmall)
    }
}
