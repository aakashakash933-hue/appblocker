package com.appblocker.core.vpn

import java.net.InetAddress

data class DnsQueryPacket(
    val ipHeaderLength: Int,
    val udpOffset: Int,
    val dnsOffset: Int,
    val sourceAddress: InetAddress,
    val destinationAddress: InetAddress,
    val sourcePort: Int,
    val destinationPort: Int,
    val domain: String,
    val dnsPayload: ByteArray
)

object DnsPacket {
    fun parse(buffer: ByteArray, length: Int): DnsQueryPacket? {
        if (length < 48) return null
        val version = (buffer[0].toInt() ushr 4) and 0x0f
        if (version != 4) return null
        val ihl = (buffer[0].toInt() and 0x0f) * 4
        if (ihl < 20 || length < ihl + 8 + 12) return null
        val protocol = buffer[9].toInt() and 0xff
        if (protocol != 17) return null

        val udpOffset = ihl
        val sourcePort = u16(buffer, udpOffset)
        val destinationPort = u16(buffer, udpOffset + 2)
        if (destinationPort != 53) return null

        val dnsOffset = udpOffset + 8
        val flags = u16(buffer, dnsOffset + 2)
        val isResponse = (flags and 0x8000) != 0
        val questionCount = u16(buffer, dnsOffset + 4)
        if (isResponse || questionCount < 1) return null

        val domain = readQuestionName(buffer, dnsOffset + 12, length) ?: return null
        return DnsQueryPacket(
            ipHeaderLength = ihl,
            udpOffset = udpOffset,
            dnsOffset = dnsOffset,
            sourceAddress = InetAddress.getByAddress(buffer.copyOfRange(12, 16)),
            destinationAddress = InetAddress.getByAddress(buffer.copyOfRange(16, 20)),
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            domain = domain,
            dnsPayload = buffer.copyOfRange(dnsOffset, length)
        )
    }

    fun buildUdpIpResponse(query: DnsQueryPacket, dnsResponse: ByteArray): ByteArray {
        val totalLength = 20 + 8 + dnsResponse.size
        val packet = ByteArray(totalLength)
        packet[0] = 0x45
        packet[1] = 0
        put16(packet, 2, totalLength)
        put16(packet, 4, 0)
        put16(packet, 6, 0)
        packet[8] = 64
        packet[9] = 17
        query.destinationAddress.address.copyInto(packet, 12)
        query.sourceAddress.address.copyInto(packet, 16)
        put16(packet, 20, 53)
        put16(packet, 22, query.sourcePort)
        put16(packet, 24, 8 + dnsResponse.size)
        put16(packet, 26, 0)
        dnsResponse.copyInto(packet, 28)
        put16(packet, 10, checksum(packet, 0, 20))
        return packet
    }

    fun blockedResponse(queryPayload: ByteArray): ByteArray {
        val response = queryPayload.copyOf()
        if (response.size >= 12) {
            response[2] = 0x81.toByte()
            response[3] = 0x83.toByte()
            response[6] = 0
            response[7] = 0
            response[8] = 0
            response[9] = 0
            response[10] = 0
            response[11] = 0
        }
        return response
    }

    private fun readQuestionName(buffer: ByteArray, start: Int, limit: Int): String? {
        val labels = mutableListOf<String>()
        var offset = start
        while (offset < limit) {
            val size = buffer[offset].toInt() and 0xff
            if (size == 0) break
            if (size > 63 || offset + size >= limit) return null
            labels += buffer.copyOfRange(offset + 1, offset + 1 + size).toString(Charsets.UTF_8)
            offset += size + 1
        }
        return labels.joinToString(".").takeIf { it.isNotBlank() }
    }

    private fun u16(buffer: ByteArray, offset: Int): Int =
        ((buffer[offset].toInt() and 0xff) shl 8) or (buffer[offset + 1].toInt() and 0xff)

    private fun put16(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = ((value ushr 8) and 0xff).toByte()
        buffer[offset + 1] = (value and 0xff).toByte()
    }

    private fun checksum(buffer: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((buffer[i].toInt() and 0xff) shl 8) +
                if (i + 1 < offset + length) (buffer[i + 1].toInt() and 0xff) else 0
            sum += word
            while (sum > 0xffff) sum = (sum and 0xffff) + (sum ushr 16)
            i += 2
        }
        return sum.inv() and 0xffff
    }
}
