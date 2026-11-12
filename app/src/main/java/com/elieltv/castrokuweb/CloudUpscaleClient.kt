package com.elieltv.castrokuweb

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object CloudUpscaleClient {
    interface Callback {
        fun onProgress(message: String)
        fun onSuccess(processedVideoUrl: String)
        fun onError(message: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun process(baseUrl: String, token: String, sourceUrl: String, callback: Callback) {
        if (baseUrl.isBlank() || token.isBlank()) {
            callback.onError("Configure a URL e o token do serviço AI Upscale nas configurações.")
            return
        }
        executor.execute {
            try {
                val root = baseUrl.trim().trimEnd('/')
                val create = JSONObject().apply {
                    put("source_url", sourceUrl)
                    put("model", "RealESRGAN_x4plus")
                    put("scale", 2)
                }
                val created = request("$root/jobs", "POST", token, create.toString())
                val id = created.getString("id")
                val deadline = System.currentTimeMillis() + 30L * 60L * 1000L
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(4000)
                    val state = request("$root/jobs/$id", "GET", token, null)
                    when (state.optString("status")) {
                        "queued" -> postProgress(callback, "AI Upscale: aguardando na nuvem...")
                        "downloading" -> postProgress(callback, "AI Upscale: baixando vídeo para processamento...")
                        "processing" -> postProgress(callback, "AI Upscale: aprimorando os quadros do vídeo...")
                        "completed" -> {
                            val path = state.getString("output_url")
                            val result = if (path.startsWith("http")) path else root + "/" + path.trimStart('/')
                            main.post { callback.onSuccess(result) }
                            return@execute
                        }
                        "failed" -> throw IllegalStateException(state.optString("error", "Falha no processamento cloud."))
                    }
                }
                throw IllegalStateException("O processamento excedeu 30 minutos. Verifique o painel do servidor.")
            } catch (e: Exception) {
                main.post { callback.onError("AI Upscale: ${e.message ?: "erro desconhecido"}") }
            }
        }
    }

    private fun postProgress(callback: Callback, message: String) {
        main.post { callback.onProgress(message) }
    }

    private fun request(address: String, method: String, token: String, body: String?): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Servidor respondeu HTTP $code: $response")
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}
