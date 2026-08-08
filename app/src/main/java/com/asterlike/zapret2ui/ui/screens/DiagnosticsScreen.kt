package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel
import com.asterlike.zapret2ui.diagnostics.DiagStatus

@Composable
fun DiagnosticsScreen(vm: MainViewModel) {
    val rows by vm.diagRows.collectAsState(initial = emptyList())
    val isDiagnosing by vm.isDiagnosing.collectAsState(initial = false)
    val dpiResults by vm.dpiResults.collectAsState(initial = emptyList())
    val isDpiChecking by vm.isDpiChecking.collectAsState(initial = false)
    val autoScores by vm.autoScores.collectAsState(initial = emptyList())
    val autoStatus by vm.autoStatus.collectAsState(initial = "")

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.runDiagnostics() }, enabled = !isDiagnosing) { Text(if (isDiagnosing) "Проверка…" else "Диагностика") }
                OutlinedButton(onClick = { vm.runDpiCheck() }, enabled = !isDpiChecking) { Text("Проверка DPI") }
            }
            if (autoStatus.isNotEmpty()) Text(autoStatus, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            Text("Доступность", style = MaterialTheme.typography.titleMedium)
        }
        items(rows) { row ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(row.service, style = MaterialTheme.typography.labelMedium)
                        Text(row.host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip("HTTP", row.http)
                        StatusChip("12", row.tls12)
                        StatusChip("13", row.tls13)
                    }
                }
            }
        }
        if (dpiResults.isNotEmpty()) {
            item { Text("Проверка DPI", style = MaterialTheme.typography.titleMedium) }
            items(dpiResults) { (host, verdict) ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(host, style = MaterialTheme.typography.labelMedium)
                        Text(verdict.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.runAutoSelect() }) { Text("Подобрать") }
                OutlinedButton(onClick = { vm.generateStrategy() }) { Text("Сгенерировать") }
            }
        }
        if (autoScores.isNotEmpty()) {
            item { Text("Результаты подбора", style = MaterialTheme.typography.titleMedium) }
            items(autoScores) { score ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${score.glyph} ${score.name}", style = MaterialTheme.typography.labelMedium)
                            Text(score.detail, style = MaterialTheme.typography.labelSmall)
                        }
                        if (score.canApply) Button(onClick = { vm.applyAutoScore(score) }) { Text("Применить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, status: DiagStatus) {
    val color = when (status) {
        DiagStatus.OK -> MaterialTheme.colorScheme.secondary
        DiagStatus.FAIL -> MaterialTheme.colorScheme.error
        DiagStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    val text = when (status) {
        DiagStatus.OK -> "✓"
        DiagStatus.FAIL -> "✗"
        DiagStatus.PARTIAL -> "≈"
        else -> "·"
    }
    Badge(containerColor = color) { Text("$label $text", Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall) }
}
