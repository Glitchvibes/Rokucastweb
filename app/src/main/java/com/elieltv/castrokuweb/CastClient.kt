package com.elieltv.castrokuweb

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

object CastClient {

    interface Callback {
        fun onSuccess()
        fun onError(message: String)
    }

    private const val ECP_PORT = 8060

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendCast(ip: String, port: Int, videoUrl: String, title: String, callback: Callback) {
        if (ip.isBlank()) {
            callback.onError("Configure o IP da Roku primeiro (engrenagem no topo).")
            return
        }

        executor.execute {
            try {
                val encodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val format = guessFormat(videoUrl)

                val target = URL(
                    "http://$ip:$ECP_PORT/launch/dev?u=$encodedUrl&videoFormat=$format&title=$encodedTitle"
                )
                val conn = target.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = false
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val code = conn.responseCode
                conn.disconnect()

                mainHandler.post {
                    if (code in 200..299) {
                        callback.onSuccess()
                    } else {
                        callback.onError("A Roku respondeu com erro $code.")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError("Nao consegui falar com a Roku: ${e.message}")
                }
            }
        }
    }

    private fun guessFormat(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> "hls"
            lower.contains(".mpd") -> "dash"
            else -> "mp4"
        }
    }
}
