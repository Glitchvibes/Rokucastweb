package com.elieltv.castrokuweb

import android.content.Context

/**
 * Guarda o IP/porta do canal "Cast Roku Web" companheiro, salvo localmente no aparelho.
 */
object RokuPrefs {
    private const val PREFS_NAME = "cast_roku_web_prefs"
    private const val KEY_IP = "roku_ip"
    private const val KEY_PORT = "roku_port"
    private const val DEFAULT_PORT = 8065

    fun getIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IP, "") ?: ""
    }

    fun getPort(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PORT, DEFAULT_PORT)
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
