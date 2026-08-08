package com.asterlike.zapret2ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.asterlike.zapret2ui.ui.navigation.ZapretNavGraph
import com.asterlike.zapret2ui.ui.navigation.bottomTabs
import com.asterlike.zapret2ui.ui.theme.ZapretTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.toggleEngine()
        } else {
            vm.clearVpnPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by vm.settings.collectAsStateWithLifecycleCompat()
            ZapretTheme(themeMode = themeMode.themeMode) {
                val navController = rememberNavController()
                var selectedTab by remember { mutableStateOf(0) }

                // VPN permission observer
                val vpnIntent by vm.vpnPermissionNeeded.collectAsStateWithLifecycleCompat()
                LaunchedEffect(vpnIntent) {
                    vpnIntent?.let { vpnPermissionLauncher.launch(it) }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            bottomTabs.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = {
                                        selectedTab = index
                                        navController.navigate(screen.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Text(screen.icon) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        ZapretNavGraph(navController = navController, viewModel = vm)
                    }
                }
            }
        }
    }
}

// Compat helper for StateFlow collection without lifecycle-runtime-compose dependency cycle
@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleCompat(): State<T> {
    return collectAsState(initial = value)
}
