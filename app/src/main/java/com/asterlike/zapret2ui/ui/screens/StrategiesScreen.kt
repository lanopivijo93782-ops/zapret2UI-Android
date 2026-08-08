package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel
import com.asterlike.zapret2ui.data.Preset

@Composable
fun StrategiesScreen(vm: MainViewModel) {
    val presets by vm.presets.collectAsState(initial = emptyList())
    val selected by vm.selectedPreset.collectAsState(initial = null)
    val grouped = presets.groupBy { it.groupTitle }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        grouped.forEach { (group, list) ->
            item {
                Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            items(list) { preset ->
                PresetCard(preset = preset, isSelected = preset.name == selected?.name, onApply = { vm.applyPreset(preset) }, onDuplicate = { vm.duplicatePreset(preset) }, onDelete = { vm.deletePreset(preset) })
            }
        }
    }
}

@Composable
private fun PresetCard(preset: Preset, isSelected: Boolean, onApply: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onApply() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(preset.primaryLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (preset.isRecommended) Badge { Text("★", Modifier.padding(4.dp)) }
                if (isSelected) Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("✓", Modifier.padding(4.dp)) }
            }
            Text(preset.secondaryLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, enabled = !isSelected) { Text(if (isSelected) "Активна" else "Применить") }
                OutlinedButton(onClick = onDuplicate) { Text("Дублировать") }
                if (!preset.isBuiltIn) OutlinedButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}
