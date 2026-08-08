package com.asterlike.zapret2ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.asterlike.zapret2ui.data.HostlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZapretApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure built-in hostlists exist
        CoroutineScope(Dispatchers.IO).launch {
            try { HostlistRepository(this@ZapretApplication).ensureBuiltIns() } catch (_: Exception) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("zapret_vpn", "Защита соединения", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel("zapret_proxy", "Telegram Proxy", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel("zapret_diag", "Диагностика", NotificationManager.IMPORTANCE_LOW))
        }
    }
}
