package com.elieltv.castrokuweb

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Controles ECP padrão do Roku. Não altera o canal para Play/Pause/Rev/Fwd.
 * O estado de posição/duração é consultado pelo endpoint media-player quando
 * o firmware/canal expõe essas informações.
 */
object RokuRemoteClient {
    data class PlaybackInfo(val state: String, val positionSeconds: Int, val durationSeconds: Int)

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun key(ip: String, key: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        request(ip, "POST", "/keypress/$key") { code, error, _ ->
            main.post { callback(code in 200..299, error ?: if (code !in 200..299) "Roku respondeu $code" else null) }
        }
    }

    fun query(ip: String, callback: (PlaybackInfo?) -> Unit) {
        request(ip, "GET", "/query/media-player") { code, error, body ->
            val info = if (code in 200..299 && body != null) parseInfo(body) else null
            main.post { callback(info) }
        }
    }

    private fun request(ip: String, method: String, path: String,
                        done: (Int, String?, String?) -> Unit) {
        if (ip.isBlank()) { done(-1, "Configure o IP da Roku.", null); return }
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = URL("http://$ip:8060$path").openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                val code = conn.responseCode
                val body = try {
                    (if (code in 200..399) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }
                } catch (_: Exception) { null }
                done(code, null, body)
            } catch (e: Exception) {
                done(-1, e.message ?: "Falha de comunicação", null)
            } finally { conn?.disconnect() }
        }
    }

    private fun parseInfo(xml: String): PlaybackInfo? {
        fun value(tag: String): String? {
            val p = Pattern.compile("<$tag(?:\\s[^>]*)?>(.*?)</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            val m = p.matcher(xml)
            return if (m.find()) m.group(1)?.replace(Regex("<[^>]+>"), "")?.trim() else null
        }
        val state = value("state") ?: return null
        val position = value("position")?.toIntOrNull() ?: 0
        val duration = value("duration")?.toIntOrNull() ?: 0
        return PlaybackInfo(state, position, duration)
    }
}
