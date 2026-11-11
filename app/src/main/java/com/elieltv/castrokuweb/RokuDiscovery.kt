package com.elieltv.castrokuweb

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URL

object RokuDiscovery {
    data class Device(val ip: String, val name: String)

    fun scan(context: Context, callback: (List<Device>) -> Unit) {
        Thread {
            val found = linkedSetOf<String>()
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("CastRokuWebDiscovery")
            try {
                lock.setReferenceCounted(false)
                lock.acquire()
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 300
                    val request = ("M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: roku:ecp\r\n\r\n").toByteArray(Charsets.US_ASCII)
                    socket.send(DatagramPacket(request, request.size,
                        InetAddress.getByName("239.255.255.250"), 1900))
                    val until = System.currentTimeMillis() + 3500
                    while (System.currentTimeMillis() < until) {
                        try {
                            val bytes = ByteArray(4096)
                            val packet = DatagramPacket(bytes, bytes.size)
                            socket.receive(packet)
                            val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                            if (response.contains("roku:ecp", true)) {
                                found.add(packet.address.hostAddress ?: continue)
                            }
                        } catch (_: java.net.SocketTimeoutException) { }
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (lock.isHeld) lock.release()
            }

            val active = found.mapNotNull { ip ->
                val activeApp = httpGet("http://$ip:8060/query/active-app") ?: return@mapNotNull null
                if (!activeApp.contains("Cast Roku Web", ignoreCase = true)) return@mapNotNull null
                val info = httpGet("http://$ip:8060/query/device-info").orEmpty()
                val name = Regex("<friendly-device-name>(.*?)</friendly-device-name>",
                    RegexOption.IGNORE_CASE).find(info)?.groupValues?.getOrNull(1)
                    ?.replace("&amp;", "&")?.takeIf { it.isNotBlank() } ?: "Roku"
                Device(ip, name)
            }
            callback(active)
        }.start()
    }

    private fun httpGet(address: String): String? {
        return try {
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 1200
            connection.readTimeout = 1200
            connection.requestMethod = "GET"
            try {
                if (connection.responseCode == 200)
                    connection.inputStream.bufferedReader().use { it.readText() }
                else null
            } finally { connection.disconnect() }
        } catch (_: Exception) { null }
    }
}
