package com.elieltv.castrokuweb

import android.content.Context

object AdBlockPrefs {
    private const val PREFS = "cast_roku_browser_prefs"
    private const val KEY_BLOCK_ADS = "block_ads"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BLOCK_ADS, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BLOCK_ADS, enabled).apply()
    }
}
