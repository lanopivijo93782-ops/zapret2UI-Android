package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel

@Composable
fun LogScreen(vm: MainViewModel) {
    val logs by vm.logLines.collectAsState(initial = emptyList())
    val proxyLogs by vm.proxyLog.collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Движок") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Telegram") })
        }
        if (tab == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { /* copy */ }) { Text("Копировать") }
                OutlinedButton(onClick = { /* clear */ }) { Text("Очистить") }
            }
            Card(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.padding(8.dp)) {
                    items(logs.takeLast(300)) { line ->
                        Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                    if (logs.isEmpty()) item { Text("Журнал пуст — включите обход", style = MaterialTheme.typography.bodySmall) }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.padding(8.dp)) {
                    items(proxyLogs.takeLast(300)) { line ->
                        Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                    if (proxyLogs.isEmpty()) item { Text("Прокси не запущен", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
