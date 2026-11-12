package com.example.goldshop

import android.content.Context
import java.security.MessageDigest

object PinManager {
    private const val PREF_NAME = "security"
    private const val KEY_PIN_HASH = "pin_hash"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hasPin(context: Context): Boolean = prefs(context).contains(KEY_PIN_HASH)

    fun check(context: Context, pin: String): Boolean =
        prefs(context).getString(KEY_PIN_HASH, null) == hash(pin)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun clearPin(context: Context) {
        prefs(context).edit().remove(KEY_PIN_HASH).apply()
    }
}
