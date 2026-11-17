package com.pingmaster.pubg

import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    data class Endpoint(val name: String, val host: String, val port: Int)

    // TCP connect time, not ICMP ping and not the PUBG game-server latency.
    fun measure(endpoint: Endpoint): Long? {
        val socket = Socket()
        return try {
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), 1800)
            ((System.nanoTime() - start) / 1_000_000).coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally {
            try { socket.close() } catch (_: Exception) { }
        }
    }
}
