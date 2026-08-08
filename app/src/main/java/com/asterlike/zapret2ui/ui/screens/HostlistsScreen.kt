package com.asterlike.zapret2ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asterlike.zapret2ui.MainViewModel

@Composable
fun HostlistsScreen(vm: MainViewModel) {
    val lists by vm.hostlists.collectAsState(initial = emptyList())
    val selected by vm.selectedHostlist.collectAsState(initial = null)
    val content by vm.hostlistContent.collectAsState(initial = "")
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Хостлисты — списки доменов для обхода", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Имя списка") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (newName.isNotBlank()) { vm.newHostlist(newName.trim()); newName = "" } }) { Text("Создать") }
        }
        LazyColumn(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lists) { name ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (name == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.loadHostlist(name) }) { Text("Открыть") }
                        TextButton(onClick = { vm.deleteHostlist(name) }) { Text("Удалить") }
                    }
                }
            }
        }
        if (selected != null) {
            OutlinedTextField(
                value = content,
                onValueChange = { vm.hostlistContent.value = it },
                label = { Text("$selected.txt — по домену на строку") },
                modifier = Modifier.fillMaxWidth().weight(0.6f),
                placeholder = { Text("youtube.com\ndiscord.com\n...") }
            )
            Button(onClick = { vm.saveHostlist() }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
        }
    }
}
