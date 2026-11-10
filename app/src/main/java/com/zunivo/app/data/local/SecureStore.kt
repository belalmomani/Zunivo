package com.zunivo.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores one small secret (the sign-in session) encrypted with an Android Keystore key. Never plain text. */
class SecureStore(context: Context, private val fileName: String = "session.bin") {
    private val file = File(context.filesDir, fileName)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    @Synchronized
    fun write(text: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        file.writeBytes(byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(text.toByteArray(Charsets.UTF_8)))
    }

    @Synchronized
    fun read(): String? = try {
        val all = file.readBytes()
        val n = all[0].toInt()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all.copyOfRange(1, 1 + n)))
        String(cipher.doFinal(all.copyOfRange(1 + n, all.size)), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    @Synchronized
    fun clear() { file.delete() }

    private companion object { const val ALIAS = "zunivo_session_key" }
}
