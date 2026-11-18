package com.disha.filekit

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// المفتاح مشفر بمفتاح غير قابل للتصدير من مخزن أندرويد ولا يعبر جسر الويب.
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("ai", Context.MODE_PRIVATE)
    fun hasKey() = !prefs.getString("secret", "").isNullOrBlank()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("filekit-gemini", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("filekit-gemini", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun config(includeSecret: Boolean = true): AiClient.Config {
        val secret = prefs.getString("secret", "").orEmpty()
        val decrypted = if (!includeSecret || secret.isBlank()) "" else {
            val bytes = Base64.decode(secret, Base64.NO_WRAP)
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                String(doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            }
        }
        return AiClient.Config(prefs.getString("endpoint", "http://127.0.0.1:11434")!!,
            prefs.getString("primary", "llama3.2:3b")!!, prefs.getString("secondary", "qwen2.5:0.5b")!!,
            prefs.getString("gemini", "gemini-3.1-flash-lite")!!, decrypted)
    }
    fun save(config: AiClient.Config, replacement: String, remove: Boolean) {
        com.disha.filekit.core.AiFallback.localEndpoint(config.endpoint)
        require(config.primary.isNotBlank() && config.primary.length <= 100 && config.secondary.length <= 100)
        require(Regex("[a-zA-Z0-9._-]{1,100}").matches(config.gemini))
        val edit = prefs.edit().putString("endpoint", config.endpoint.trim().trimEnd('/')).putString("primary", config.primary.trim())
            .putString("secondary", config.secondary.trim()).putString("gemini", config.gemini.trim())
        if (remove) edit.remove("secret")
        else if (replacement.isNotBlank()) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            edit.putString("secret", Base64.encodeToString(cipher.iv + cipher.doFinal(replacement.trim().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        }
        check(edit.commit())
    }
}
