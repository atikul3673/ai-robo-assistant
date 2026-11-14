package com.flowfi.assistant.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AiConfig(val provider: String, val apiKey: String, val model: String)

class SecureStore(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx, "flowfi_secure",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    val provider get() = prefs.getString("provider", "gemini") ?: "gemini"
    val apiKey get() = prefs.getString("key", "") ?: ""
    val model get() = prefs.getString("model", "") ?: ""

    fun save(provider: String, key: String, model: String) {
        prefs.edit().putString("provider", provider).putString("key", key).putString("model", model).apply()
    }

    fun config() = AiConfig(provider, apiKey, model.ifBlank { defaultModel(provider) })

    companion object {
        // মডেলের নাম সময়ের সাথে বদলায় — Settings থেকে নিজে বদলে নিতে পারবেন
        fun defaultModel(p: String) = if (p == "openai") "gpt-4o-mini" else "gemini-2.0-flash"
    }
}
