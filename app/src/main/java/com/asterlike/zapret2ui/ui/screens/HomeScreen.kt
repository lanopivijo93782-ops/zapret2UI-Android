package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel
import com.asterlike.zapret2ui.engine.EngineService

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.engineState.collectAsStateWithLifecycleCompat()
    val preset by vm.selectedPreset.collectAsStateWithLifecycleCompat()
    val settings by vm.settings.collectAsStateWithLifecycleCompat()
    val isRunning = state == EngineService.EngineState.RUNNING
    val isStarting = state == EngineService.EngineState.STARTING

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Zapret2UI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Обход блокировок в один клик — теперь на Android", style = MaterialTheme.typography.bodySmall)
                Text("Пресет: ${preset?.primaryLabel ?: "—"}", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Status card
        Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val (dot, label, color) = when (state) {
                    EngineService.EngineState.RUNNING -> Triple("🟢", "Работает", MaterialTheme.colorScheme.secondary)
                    EngineService.EngineState.STARTING -> Triple("🟡", "Запуск…", MaterialTheme.colorScheme.tertiary)
                    EngineService.EngineState.STOPPING -> Triple("🟠", "Остановка…", MaterialTheme.colorScheme.tertiary)
                    else -> Triple("🔴", "Остановлен", MaterialTheme.colorScheme.error)
                }
                Text("$dot  $label", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                if (isRunning) Text("TUN 10.111.222.1  •  ${preset?.name}", style = MaterialTheme.typography.bodySmall)

                Button(
                    onClick = { vm.toggleEngine() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                    enabled = !isStarting
                ) {
                    Text(if (isRunning) "Выключить обход" else "Включить обход", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    if (isRunning) "Трафик Discord/YouTube идёт через desync. Проверьте сайты." else "Нажмите чтобы поднять локальный TUN и активировать desync.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.runAutoSelect() }, modifier = Modifier.weight(1f)) { Text("Подобрать") }
            OutlinedButton(onClick = { vm.generateStrategy() }, modifier = Modifier.weight(1f)) { Text("Сгенерировать") }
        }

        // Telegram card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Telegram через прокси", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Работает без VPN и без root — локальный MTProto → WS-TLS через Cloudflare", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { /* vm telegram toggle */ }) { Text("Включить") }
                    OutlinedButton(onClick = {}) { Text("Открыть в Telegram") }
                }
            }
        }

        // Info
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text("Память по сетям: ${settings.networkStrategies.size} сетей запомнено", style = MaterialTheme.typography.labelSmall)
                Text("VPN — локальный TUN, трафик не уходит на чужой сервер. Desync делается на устройстве.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleCompat(): androidx.compose.runtime.State<T> = collectAsState(initial = value)
