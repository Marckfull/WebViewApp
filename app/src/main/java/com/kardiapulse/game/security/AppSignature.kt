package com.kardiapulse.game.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.kardiapulse.game.BuildConfig
import java.security.MessageDigest

/**
 * Impressão digital do certificado que assinou este APK.
 *
 * Serve para detectar uma versão recompilada ou re-assinada — o caminho por onde passam quase
 * todos os APKs "modificados" que circulam por aí. O jogo não bloqueia ninguém por causa disso:
 * apenas deixa de tratar o progresso como verificado.
 *
 * Enquanto `BuildConfig.EXPECTED_SIGNATURE` estiver vazio a checagem fica desligada. Para ligar,
 * rode uma vez em release, veja no Logcat a linha "assinatura atual" e cole o valor no
 * `buildConfigField` correspondente do `app/build.gradle.kts`.
 */
object AppSignature {

    private const val TAG = "AppSignature"

    /** SHA-256 do certificado de assinatura, em hexadecimal maiúsculo. */
    fun fingerprint(context: Context): String? = try {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val certificates: Array<out android.content.pm.Signature>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.let { signingInfo ->
                    if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                    else signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }

        val first = certificates?.firstOrNull()
        if (first == null) null
        else MessageDigest.getInstance("SHA-256")
            .digest(first.toByteArray())
            .joinToString("") { byte -> "%02X".format(byte) }
    } catch (t: Throwable) {
        Log.w(TAG, "Não foi possível ler a assinatura", t)
        null
    }

    /**
     * `true` quando a assinatura confere — ou quando a checagem não foi configurada.
     *
     * Em caso de dúvida o resultado é sempre permissivo: nunca vale a pena travar um jogador
     * legítimo por causa de uma leitura que falhou.
     */
    fun isTrusted(context: Context): Boolean {
        val expected = BuildConfig.EXPECTED_SIGNATURE
        val actual = fingerprint(context)
        if (BuildConfig.DEBUG && actual != null) {
            Log.i(TAG, "assinatura atual: $actual")
        }
        if (expected.isBlank()) return true
        if (actual == null) return true
        return actual.equals(expected, ignoreCase = true)
    }
}
