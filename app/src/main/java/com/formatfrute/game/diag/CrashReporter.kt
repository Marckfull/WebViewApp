package com.formatfrute.game.diag

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import com.formatfrute.game.BuildConfig
import com.formatfrute.game.R
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Caixa-preta do jogo.
 *
 * Jogo publicado sem telemetria é jogo cego: não dá para saber em que fase as
 * pessoas travam nem que existe um erro só naquele aparelho. Como o Format
 * Frute não tem servidor, o relatório fica guardado no próprio aparelho e o
 * jogador decide se manda por e-mail na próxima abertura.
 *
 * Para trocar por Crashlytics depois, basta adicionar o plugin do Firebase e
 * chamar `FirebaseCrashlytics.getInstance().recordException(error)` dentro do
 * [install] — o resto do app não muda.
 */
object CrashReporter {

    private const val PREFS = "format_frute_diag"
    private const val KEY_REPORT = "last_crash"
    private const val KEY_SCREEN = "last_screen"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { save(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Marca onde o jogador estava; entra no relatório e economiza adivinhação. */
    fun breadcrumb(context: Context, where: String) {
        runCatching {
            prefs(context).edit { putString(KEY_SCREEN, where) }
        }
    }

    private fun save(context: Context, thread: Thread, error: Throwable) {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val when_ = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val report = buildString {
            appendLine("Format Frute ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Quando: $when_")
            appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Tela: ${prefs(context).getString(KEY_SCREEN, "?")}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(stack)
        }

        prefs(context).edit { putString(KEY_REPORT, report) }
    }

    fun pending(context: Context): String? =
        runCatching { prefs(context).getString(KEY_REPORT, null) }.getOrNull()

    fun clear(context: Context) {
        runCatching { prefs(context).edit { remove(KEY_REPORT) } }
    }

    /** Abre o app de e-mail com o relatório pronto. Nada sai sem o jogador mandar. */
    fun shareIntent(context: Context, report: String): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.contact_email)))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_subject))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.crash_body, report))
        }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
