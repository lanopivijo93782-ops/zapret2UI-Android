package com.asterlike.zapret2ui.proxy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.asterlike.zapret2ui.MainActivity
import com.asterlike.zapret2ui.R
import com.asterlike.zapret2ui.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ForegroundService для Telegram прокси — работает независимо от VPN.
 * Не требует root и не требует VPNService (обычный ServerSocket на loopback).
 */
class TgProxyService : Service() {

    private val binder = LocalBinder()
    private var core: TgProxyCore? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines

    inner class LocalBinder : Binder() {
        fun getService(): TgProxyService = this@TgProxyService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(Constants.CHANNEL_PROXY, "Telegram Proxy", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("port", 1443) ?: 1443
        val secret = intent?.getStringExtra("secret") ?: TgProxyCore.generateSecret()
        startProxy(port, secret)
        return START_STICKY
    }

    fun startProxy(port: Int, secret: String) {
        if (_isRunning.value) return
        core = TgProxyCore(port, secret).apply {
            setLogCallback { line ->
                val list = _logLines.value.toMutableList()
                list.add(line)
                if (list.size > 500) list.removeAt(0)
                _logLines.value = list
            }
            start()
        }
        _isRunning.value = true
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, Constants.CHANNEL_PROXY)
            .setContentTitle("Telegram прокси работает")
            .setContentText("127.0.0.1:$port  •  tg://proxy")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        startForeground(Constants.NOTIF_ID_PROXY, notif)
    }

    fun stopProxy() {
        core?.stop(); core = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    val proxyLink: String get() = core?.proxyLink ?: "tg://proxy?server=127.0.0.1&port=1443&secret=dd…"

    override fun onDestroy() {
        core?.stop()
        super.onDestroy()
    }
}
