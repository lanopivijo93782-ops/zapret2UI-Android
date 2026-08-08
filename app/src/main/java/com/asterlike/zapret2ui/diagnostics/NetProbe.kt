package com.asterlike.zapret2ui.diagnostics

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.*
import java.security.cert.X509Certificate

/**
 * Порт NetProbe.cs — проверка доступности хостов по TLS 1.2 / 1.3 + HTTPS GET.
 * Используется в Диагностике и Автоподборе.
 */
object NetProbe {
    data class HostResult(val host: String, val tls12: Boolean, val tls13: Boolean, val https: Boolean)

    suspend fun probeHost(host: String, timeoutMs: Int = 5000): HostResult = withContext(Dispatchers.IO) {
        val tls12 = probeTls(host, "TLSv1.2", timeoutMs)
        val tls13 = probeTls(host, "TLSv1.3", timeoutMs)
        val https = probeHttps(host, timeoutMs)
        HostResult(host, tls12, tls13, https)
    }

    private fun probeTls(host: String, protocol: String, timeoutMs: Int): Boolean {
        return try {
            val ctx = SSLContext.getInstance(protocol)
            ctx.init(null, arrayOf<TrustManager>(InsecureTrustManager), null)
            val factory = ctx.socketFactory
            (factory.createSocket() as SSLSocket).use { sock ->
                sock.soTimeout = timeoutMs
                sock.enabledProtocols = arrayOf(protocol)
                sock.connect(InetSocketAddress(host, 443), timeoutMs)
                sock.startHandshake()
                true
            }
        } catch (_: Exception) { false }
    }

    private fun probeHttps(host: String, timeoutMs: Int): Boolean {
        return try {
            val url = java.net.URL("https://$host/")
            val conn = url.openConnection() as HttpsURLConnection
            conn.apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Zapret2UI-Android/1.0")
                instanceFollowRedirects = true
                hostnameVerifier = HostnameVerifier { _, _ -> true }
                sslSocketFactory = insecureSslContext().socketFactory
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (_: Exception) { false }
    }

    private fun insecureSslContext(): SSLContext {
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(InsecureTrustManager), null)
        }
    }

    private object InsecureTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** Быстрая проверка без TLS — просто TCP connect к 443 */
    suspend fun canConnect(host: String, timeoutMs: Int = 3000): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { s -> s.connect(InetSocketAddress(host, 443), timeoutMs); true }
        } catch (_: Exception) { false }
    }
}
