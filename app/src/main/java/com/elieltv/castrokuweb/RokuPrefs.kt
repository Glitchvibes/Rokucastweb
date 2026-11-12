package com.elieltv.castrokuweb

import android.content.Context

/**
 * Guarda o IP/porta do canal "Cast Roku Web", salvo localmente no aparelho.
 * O canal companheiro v1.2 recebe comandos pelo ECP padrão da Roku (8060).
 */
object RokuPrefs {
    private const val PREFS_NAME = "cast_roku_web_prefs"
    private const val KEY_IP = "roku_ip"
    private const val KEY_PORT = "roku_port"
    private const val DEFAULT_PORT = 8060

    fun getIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IP, "") ?: ""
    }

    fun getPort(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPort = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        // Versões anteriores salvavam 8065, mas o CastClient atual e o canal v1.2
        // usam o ECP padrão da Roku na porta 8060.
        if (savedPort == 8065) {
            prefs.edit().putInt(KEY_PORT, DEFAULT_PORT).apply()
            return DEFAULT_PORT
        }
        return savedPort
    }

    fun save(context: Context, ip: String, port: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_IP, ip)
            .putInt(KEY_PORT, port)
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getIp(context).isNotBlank()
    }
}
