package com.asterlike.zapret2ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.asterlike.zapret2ui.data.SettingsRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, "android.intent.action.MY_PACKAGE_REPLACED")) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val settings = repo.settingsFlow.first()
                if (settings.autoStartOnBoot && settings.autostartEngine) {
                    val svc = Intent(context, com.asterlike.zapret2ui.vpn.ZapretVpnService::class.java)
                    svc.action = com.asterlike.zapret2ui.vpn.ZapretVpnService.ACTION_START
                    context.startForegroundService(svc)
                }
                if (settings.tgProxyAutostart) {
                    context.startForegroundService(Intent(context, com.asterlike.zapret2ui.proxy.TgProxyService::class.java))
                }
            } catch (_: Exception) {}
        }
    }
}
