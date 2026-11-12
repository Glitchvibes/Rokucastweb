package com.elieltv.castrokuweb

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Bloqueio básico por domínio de requisições publicitárias conhecidas.
 * Não remove anúncios inseridos dentro do próprio fluxo de vídeo nem garante
 * bloqueio de todos os provedores; a lista é intencionalmente conservadora.
 */
object AdBlocker {
    private val blockedHosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.", "adnxs.com", "taboola.com", "outbrain.com",
        "popads.net", "popcash.net", "exoclick.com", "adsterra.com",
        "trafficjunky.net", "adform.net", "criteo.com"
    )

    fun shouldBlock(url: String): Boolean {
        val host = try {
            java.net.URI(url).host?.lowercase(Locale.ROOT) ?: return false
        } catch (_: Exception) { return false }
        return blockedHosts.any { host == it || host.endsWith(".$it") ||
            (it.endsWith(".") && host.startsWith(it)) }
    }

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", 204, "No Content",
            mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)))
}
