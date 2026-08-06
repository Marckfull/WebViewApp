package com.kardiapulse.game.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File

/**
 * Uma leitura leve e honesta do ambiente em que o app está rodando.
 *
 * Serve para decidir se vale confiar em recordes locais — não para punir ninguém. Um aparelho
 * com root continua jogando normalmente; o jogo apenas marca o perfil como "não verificado".
 * Detecção de root é sempre um jogo de gato e rato, então nada aqui pretende ser à prova de balas.
 */
object DeviceIntegrity {

    data class Report(
        val rooted: Boolean,
        val emulator: Boolean,
        val debuggable: Boolean
    ) {
        val trustworthy: Boolean get() = !rooted && !debuggable
    }

    private val SUSPECT_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    fun assess(context: Context): Report = Report(
        rooted = looksRooted(),
        emulator = looksEmulated(),
        debuggable = isDebuggable(context)
    )

    private fun looksRooted(): Boolean = try {
        SUSPECT_PATHS.any { File(it).exists() } || Build.TAGS?.contains("test-keys") == true
    } catch (_: SecurityException) {
        false
    }

    private fun looksEmulated(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("vbox") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT == "sdk_gphone64_x86_64"

    private fun isDebuggable(context: Context): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
