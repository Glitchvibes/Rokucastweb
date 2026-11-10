package com.elieltv.castrokuweb

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Manda o link de video escolhido para o canal Roku companheiro ("Cast Roku Web"),
 * via um POST HTTP simples na rede local.
 *
 * PROTOCOLO (o canal Roku precisa implementar este lado, usando roStreamSocket
 * ou um roUrlTransfer em modo servidor/roListener pra escutar essa porta):
 *
 *   POST http://<IP_DA_ROKU>:<porta, padrao 8065>/cast
 *   Content-Type: application/json
 *   Corpo: {"url": "https://...", "title": "...", "from": "CastRokuWeb"}
 *
 * O canal Roku, ao receber isso, deve tocar a URL recebida no seu Video node.
 */
object CastClient {

    interface Callback {
        fun onSuccess()
        fun onError(message: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendCast(ip: String, port: Int, videoUrl: String, title: String, callback: Callback) {
        if (ip.isBlank()) {
            callback.onError("Configure o IP da Roku primeiro (engrenagem no topo).")
            return
        }

        executor.execute {
            try {
                val target = URL("http://$ip:$port/cast")
                val conn = target.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val body = JSONObject()
                body.put("url", videoUrl)
                body.put("title", title)
                body.put("from", "CastRokuWeb")

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

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
}
