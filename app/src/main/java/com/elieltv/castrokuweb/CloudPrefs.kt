package com.elieltv.castrokuweb

import android.content.Context

object CloudPrefs {
    private const val PREFS = "cast_roku_cloud_prefs"
    fun getUrl(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("url", "") ?: ""
    fun getToken(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("token", "") ?: ""
    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("url", url.trim().trimEnd('/'))
            .putString("token", token.trim())
            .apply()
    }
}
