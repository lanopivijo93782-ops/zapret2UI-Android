package com.asterlike.zapret2ui.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiagRow(
    val service: String,
    val host: String,
    var http: DiagStatus = DiagStatus.UNKNOWN,
    var tls12: DiagStatus = DiagStatus.UNKNOWN,
    var tls13: DiagStatus = DiagStatus.UNKNOWN,
    var ping: DiagStatus = DiagStatus.UNKNOWN
)
enum class DiagStatus { UNKNOWN, OK, FAIL, PARTIAL }

class DiagnosticsService {

    val defaultTargets = listOf(
        "discord.com" to "Discord вход",
        "gateway.discord.gg" to "Discord Gateway",
        "cdn.discordapp.com" to "Discord CDN",
        "www.youtube.com" to "YouTube",
        "youtu.be" to "YouTube short",
        "i.ytimg.com" to "YouTube картинки",
        "googlevideo.com" to "YouTube видео",
        "www.google.com" to "Google",
        "cloudflare.com" to "Cloudflare"
    )

    suspend fun runBasic(rows: List<DiagRow>, onUpdate: (DiagRow) -> Unit = {}) = withContext(Dispatchers.IO) {
        for (row in rows) {
            val res = NetProbe.probeHost(row.host, timeoutMs = 5000)
            row.tls12 = if (res.tls12) DiagStatus.OK else DiagStatus.FAIL
            row.tls13 = if (res.tls13) DiagStatus.OK else DiagStatus.FAIL
            row.http = if (res.https) DiagStatus.OK else DiagStatus.FAIL
            row.ping = if (NetProbe.canConnect(row.host, 2000)) DiagStatus.OK else DiagStatus.FAIL
            onUpdate(row)
        }
    }

    fun buildRows(): List<DiagRow> = defaultTargets.map { (host, svc) -> DiagRow(service = svc, host = host) }
}
