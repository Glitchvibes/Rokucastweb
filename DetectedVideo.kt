package com.elieltv.castrokuweb

/**
 * Representa um link de video/stream encontrado numa pagina.
 */
data class DetectedVideo(
    val url: String,
    val title: String,
    val sourcePageUrl: String
)
