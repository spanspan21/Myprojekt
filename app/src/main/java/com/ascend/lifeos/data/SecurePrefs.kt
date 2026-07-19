package com.ascend.lifeos.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-encrypted SharedPreferences for CREDENTIALS (G-plan §8: the
 * GoCardless secrets and the Untis password sat in plaintext prefs — one of
 * the oldest open audit findings). One-time migration: on first access the
 * plaintext file's values are copied into the encrypted store and the
 * plaintext file is cleared. Falls back to the plain file if the Keystore is
 * broken on this device (rare OEM pathologies) — a working app beats a
 * perfectly locked one, and the fallback is logged, not silent.
 */
object SecurePrefs {

    fun get(ctx: Context, name: String): SharedPreferences {
        val app = ctx.applicationContext
        return runCatching {
            val key = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val enc = EncryptedSharedPreferences.create(
                app, "${name}_enc", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            migrateOnce(app, name, enc)
            enc
        }.getOrElse { e ->
            android.util.Log.w("SecurePrefs", "keystore unavailable for $name — plaintext fallback", e)
            app.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    private fun migrateOnce(app: Context, name: String, enc: SharedPreferences) {
        val plain = app.getSharedPreferences(name, Context.MODE_PRIVATE)
        if (plain.all.isEmpty() || enc.getBoolean("_migrated", false)) return
        val e = enc.edit()
        plain.all.forEach { (k, v) ->
            when (v) {
                is String -> e.putString(k, v)
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Float -> e.putFloat(k, v)
                else -> {}
            }
        }
        e.putBoolean("_migrated", true).apply()
        plain.edit().clear().apply() // the plaintext copy dies
    }
}
