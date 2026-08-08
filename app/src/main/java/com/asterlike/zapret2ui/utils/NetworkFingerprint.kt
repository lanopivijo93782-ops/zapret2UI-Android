package com.asterlike.zapret2ui.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.security.MessageDigest

/**
 * Порт NetworkFingerprint.cs — локальный отпечаток сети для "памяти по сетям".
 * На Android используем BSSID/SSID + gateway + transport type.
 * Ничего не отправляется в интернет, только хеш.
 */
object NetworkFingerprint {
    fun current(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "offline"
            val caps = cm.getNetworkCapabilities(network)
            val transport = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "wifi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "cell"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "eth"
                else -> "other"
            }
            // WiFi BSSID as stable identifier (requires location permission on Android 10+, so degrade gracefully)
            var wifiId = ""
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wm.connectionInfo
                wifiId = info?.bssid ?: info?.ssid ?: ""
                if (wifiId == "<unknown ssid>" || wifiId == "02:00:00:00:00:00") wifiId = ""
            } catch (_: Exception) { }

            val raw = "$transport|$wifiId|${network.hashCode()}"
            sha256(raw).take(12)
        } catch (_: Exception) {
            "generic"
        }
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
