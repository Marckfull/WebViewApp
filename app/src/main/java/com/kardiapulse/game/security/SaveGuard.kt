package com.kardiapulse.game.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Assina o progresso salvo com uma chave HMAC que vive dentro do Android Keystore.
 *
 * A chave nunca sai do Keystore: nem o app consegue exportá-la. Isso quer dizer que editar o
 * arquivo de preferências por fora (root, backup adulterado, editores de save) quebra a
 * assinatura e o jogo detecta na hora. Não é DRM — é só uma barreira honesta contra o
 * "editei meus cristais para 999999".
 */
object SaveGuard {

    private const val TAG = "SaveGuard"
    private const val KEY_ALIAS = "kardia_save_hmac_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALGORITHM = "HmacSHA256"

    /** Verdadeiro quando o Keystore não está disponível e caímos no modo sem assinatura. */
    @Volatile
    var degraded: Boolean = false
        private set

    fun sign(payload: String): String {
        val key = obtainKey() ?: return ""
        return try {
            val mac = Mac.getInstance(ALGORITHM)
            mac.init(key)
            Base64.encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao assinar o save", t)
            degraded = true
            ""
        }
    }

    /**
     * Confere a assinatura. Quando o Keystore não está disponível ([degraded]) aceitamos o
     * save mesmo assim — travar o jogador fora do próprio progresso seria pior que o risco.
     */
    fun verify(payload: String, signature: String): Boolean {
        if (signature.isEmpty()) return degraded
        val expected = sign(payload)
        if (expected.isEmpty()) return degraded
        return constantTimeEquals(expected, signature)
    }

    /** Comparação de tempo constante: nunca vaza quantos bytes bateram. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun obtainKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: generateKey()
        } catch (t: Throwable) {
            Log.w(TAG, "Keystore indisponível; seguindo sem assinatura", t)
            degraded = true
            null
        }
    }

    private fun generateKey(): SecretKey? {
        return try {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setUserAuthenticationRequired(false)
                    }
                }
                .build()
            generator.init(spec)
            generator.generateKey()
        } catch (t: Throwable) {
            Log.w(TAG, "Não foi possível gerar a chave HMAC", t)
            degraded = true
            null
        }
    }
}
