package com.elieltv.castrokuweb

import android.webkit.JavascriptInterface

/**
 * Deteccao de video/stream em qualquer site, usando DUAS tecnicas:
 *
 * 1) JavascriptInterface (este arquivo): um script injetado na pagina varre
 *    as tags <video>/<source> do DOM. Isso funciona bem pra paginas simples,
 *    mas NAO enxerga video dentro de <iframe> de outro dominio (politica de
 *    mesma origem do navegador bloqueia isso em qualquer app, nao e limitacao
 *    nossa).
 *
 * 2) shouldInterceptRequest no MainActivity (olha TODAS as requisicoes de
 *    rede que a WebView faz, inclusive as de dentro de iframes de outro
 *    dominio) procurando por extensao de arquivo de video/stream. Essa e a
 *    tecnica que realmente pega a maioria dos players embutidos.
 */
class VideoDetector(private val onFound: (url: String, title: String) -> Unit) {

    @JavascriptInterface
    fun onVideoFound(url: String, pageTitle: String) {
        if (url.isNotBlank()) {
            onFound(url, pageTitle)
        }
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidVideoDetector"

        /** Extensoes/caminhos tipicos de arquivo de video ou stream ao vivo. */
        val VIDEO_PATTERNS = listOf(
            ".m3u8", ".mp4", ".mkv", ".webm", ".mov", ".ts",
            ".mpd" // DASH
        )

        fun looksLikeVideo(url: String): Boolean {
            val lower = url.lowercase().substringBefore('?').substringBefore('#')
            return VIDEO_PATTERNS.any { lower.endsWith(it) }
        }

        /**
         * Script injetado apos a pagina carregar: varre <video>/<source> do DOM
         * e reporta qualquer src encontrado pro lado Kotlin.
         */
        const val SCAN_SCRIPT = """
            (function() {
                try {
                    var found = [];
                    document.querySelectorAll('video').forEach(function(v) {
                        if (v.src) found.push(v.src);
                        v.querySelectorAll('source').forEach(function(s) {
                            if (s.src) found.push(s.src);
                        });
                    });
                    document.querySelectorAll('source').forEach(function(s) {
                        if (s.src) found.push(s.src);
                    });
                    found.forEach(function(u) {
                        if (window.$JS_INTERFACE_NAME) {
                            window.$JS_INTERFACE_NAME.onVideoFound(u, document.title);
                        }
                    });
                } catch (e) {}
            })();
        """
    }
}
