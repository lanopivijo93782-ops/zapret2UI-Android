package com.asterlike.zapret2ui.vpn

import java.nio.ByteBuffer

/**
 * Утилиты для парсинга IP/TCP/UDP пакетов из TUN.
 * Порт части EngineService + NetworkFingerprint логики.
 */
object TunPacketHandler {
    data class IpHeader(val version: Int, val ihl: Int, val protocol: Int, val srcIp: String, val dstIp: String, val totalLen: Int)
    data class TcpHeader(val srcPort: Int, val dstPort: Int, val seq: Long, val ack: Long, val flags: Int, val headerLen: Int)

    fun parseIp(packet: ByteArray): IpHeader? {
        if (packet.size < 20) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        val ihl = (packet[0].toInt() and 0xF) * 4
        val proto = packet[9].toInt() and 0xFF
        val totalLen = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
        val src = "${packet[12].toInt() and 0xFF}.${packet[13].toInt() and 0xFF}.${packet[14].toInt() and 0xFF}.${packet[15].toInt() and 0xFF}"
        val dst = "${packet[16].toInt() and 0xFF}.${packet[17].toInt() and 0xFF}.${packet[18].toInt() and 0xFF}.${packet[19].toInt() and 0xFF}"
        return IpHeader(version, ihl, proto, src, dst, totalLen)
    }

    fun parseTcp(packet: ByteArray, ipIhl: Int): TcpHeader? {
        if (packet.size < ipIhl + 20) return null
        val srcPort = ((packet[ipIhl].toInt() and 0xFF) shl 8) or (packet[ipIhl+1].toInt() and 0xFF)
        val dstPort = ((packet[ipIhl+2].toInt() and 0xFF) shl 8) or (packet[ipIhl+3].toInt() and 0xFF)
        val seq = ((packet[ipIhl+4].toLong() and 0xFF) shl 24) or ((packet[ipIhl+5].toLong() and 0xFF) shl 16) or ((packet[ipIhl+6].toLong() and 0xFF) shl 8) or (packet[ipIhl+7].toLong() and 0xFF)
        val ack = ((packet[ipIhl+8].toLong() and 0xFF) shl 24) or ((packet[ipIhl+9].toLong() and 0xFF) shl 16) or ((packet[ipIhl+10].toLong() and 0xFF) shl 8) or (packet[ipIhl+11].toLong() and 0xFF)
        val flags = packet[ipIhl+13].toInt() and 0xFF
        val hlen = ((packet[ipIhl+12].toInt() and 0xF0) shr 4) * 4
        return TcpHeader(srcPort, dstPort, seq, ack, flags, hlen)
    }

    fun isTlsClientHello(packet: ByteArray, ipIhl: Int, tcpHlen: Int): Boolean {
        val payloadStart = ipIhl + tcpHlen
        if (payloadStart + 6 > packet.size) return false
        return (packet[payloadStart].toInt() and 0xFF == 0x16) && (packet[payloadStart+1].toInt() and 0xFF == 0x03)
    }

    fun buildIpv4Checksum(header: ByteArray): Int {
        var sum = 0
        for (i in header.indices step 2) {
            if (i == 10) continue // skip checksum field
            sum += ((header[i].toInt() and 0xFF) shl 8) or (header[i+1].toInt() and 0xFF)
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
