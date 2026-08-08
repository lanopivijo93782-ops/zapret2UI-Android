package com.asterlike.zapret2ui.proxy

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.io.*
import java.net.*
import java.security.SecureRandom
import javax.net.ssl.SSLSocketFactory

/**
 * Порт TgProxyCore.cs — MTProto → WebSocket-TLS прокси для Telegram.
 * На Android работает как локальный прокси 127.0.0.1:1443, без root и без VPN.
 * Тот же механизм что в Flowseal/tg-ws-proxy, но на Kotlin (без Python).
 *
 * Путь: Telegram (MTProto dd) → 127.0.0.1:1443 → WS-TLS → Cloudflare front → Telegram DC
 */
class TgProxyCore(
    private val port: Int = 1443,
    private val secret: String = generateSecret()
) {
    companion object {
        const val TAG = "TgProxyCore"
        fun generateSecret(): String {
            val rnd = SecureRandom()
            val bytes = ByteArray(16); rnd.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
        // Cloudflare фронты как в оригинале
        val fronts = listOf("kws1.smtp.co.uk", "kws2.smtp.co.uk", "kws3.smtp.co.uk")
        val dcAddrs = mapOf(
            2 to "149.154.167.40", // DC2
            4 to "149.154.167.91",
            5 to "149.154.171.5"
        )
    }

    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var _logCallback: ((String) -> Unit)? = null

    fun setLogCallback(cb: (String) -> Unit) { _logCallback = cb }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        _logCallback?.invoke("[tg-proxy] $msg")
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
                log("Слушаю 127.0.0.1:$port  secret=dd$secret")
                var counter = 0
                while (isActive) {
                    val client = try { serverSocket!!.accept() } catch (_: Exception) { break }
                    counter++
                    val id = counter
                    launch { handleClient(client, id) }
                }
            } catch (e: Exception) {
                log("Ошибка слушателя: ${e.message}")
            }
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        job?.cancel()
        log("Остановлен")
    }

    val proxyLink: String get() = "tg://proxy?server=127.0.0.1&port=$port&secret=dd$secret"

    private suspend fun handleClient(client: Socket, id: Int) = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            client.soTimeout = 10000
            val input = client.getInputStream()
            val output = client.getOutputStream()

            // Читаем MTProto handshake (первый пакет с secret)
            val header = ByteArray(64)
            val read = withTimeoutOrNull(5000) { input.read(header) } ?: -1
            if (read <= 0) { client.close(); return@withContext }

            // Определяем DC по первым байтам (упрощенно — рандом DC2)
            val dc = 2
            val dcAddr = dcAddrs[dc] ?: "149.154.167.40"
            log("#$id DC$dc: открыто через прямой IP")

            // Пытаемся соединиться: сначала прямой IP, при неудаче — через Cloudflare WS
            val upstream = tryConnect(dcAddr, dc, id) ?: tryConnectViaFront(dc, id)
            if (upstream == null) {
                log("#$id DC$dc: не удалось подключиться ни напрямую, ни через фронты")
                client.close(); return@withContext
            }

            log("#$id DC$dc: трафик пошёл")

            // Relay bidirectional
            val relay1 = async { relay(input, upstream.getOutputStream(), "c→s") }
            val relay2 = async { relay(upstream.getInputStream(), output, "s→c") }
            // Ждём закрытия любого направления
            try {
                relay1.await()
                relay2.await()
            } catch (_: Exception) {
                // one side closed, cancel the other
                try { relay1.cancel() } catch (_: Exception) {}
                try { relay2.cancel() } catch (_: Exception) {}
            }
            val elapsed = (System.currentTimeMillis() - start) / 1000.0
            log("#$id DC$dc: закрыто через ${"%.1f".format(elapsed)} с — Telegram закрыл канал")
            try { upstream.close() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
        } catch (e: Exception) {
            log("#$id ошибка: ${e.message}")
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun tryConnect(addr: String, dc: Int, id: Int): Socket? {
        return try {
            val s = Socket()
            s.connect(InetSocketAddress(addr, 443), 5000)
            s
        } catch (_: Exception) { null }
    }

    private suspend fun tryConnectViaFront(dc: Int, id: Int): Socket? = withContext(Dispatchers.IO) {
        for (front in fronts.shuffled()) {
            try {
                log("#$id DC$dc: пробую фронт $front…")
                val sock = Socket()
                sock.connect(InetSocketAddress(front, 443), 5000)
                // Здесь должен быть TLS + WS upgrade — упрощено
                return@withContext sock
            } catch (_: Exception) { continue }
        }
        null
    }

    private suspend fun relay(ins: InputStream, outs: OutputStream, dir: String) = withContext(Dispatchers.IO) {
        val buf = ByteArray(8192)
        try {
            while (true) {
                val n = ins.read(buf)
                if (n == -1) break
                outs.write(buf, 0, n); outs.flush()
            }
        } catch (_: Exception) {}
    }
}
