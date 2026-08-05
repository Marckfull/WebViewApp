package com.formatfrute.game.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.formatfrute.game.MainActivity
import com.formatfrute.game.R
import com.formatfrute.game.data.GameRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** O texto e o produto aqui: se nao arrancar um sorriso, nao traz ninguem de volta. */
object FunnyMessages {

    private val saudade = listOf(
        "🍌 A banana ficou marrom de tanto te esperar." to "Volta lá antes que vire bolo.",
        "🍒 Suas cerejas estão em fila indiana esperando fusão." to "Elas são educadas, mas impacientes.",
        "🥝 O Kiwi está se sentindo peludo e sozinho." to "Ninguém merece isso.",
        "🍇 Suas uvas estão virando passas." to "Passa lá rapidinho pra salvar.",
        "🍊 A Laranja perguntou de você." to "Falei que você já estava vindo. Não me deixe mal.",
        "🍓 O Morango chorou hoje." to "De verdade. Foi constrangedor pra todo mundo.",
        "🍐 A Pera está encostada na parede desde ontem." to "Ela finge que não liga, mas liga.",
    )

    private val desafio = listOf(
        "🍉 A Melancia disse que você nunca chega nela." to "Prova o contrário. Só uma partida.",
        "🧃 O Monstro Azedo declarou vitória." to "Ele disse que você fugiu da Batalha do Suco.",
        "🍍 O Abacaxi te desafiou." to "E olha, esse abacaxi é fácil de descascar.",
        "🐉 A Pitaya está achando graça." to "Ninguém do seu nível chegou nela hoje.",
        "🏆 Alguém bateu um recorde hoje." to "Foi você? Não. Ainda dá tempo.",
    )

    private val economia = listOf(
        "🎁 Seu presente do dia está esfriando." to "Presente frio não tem graça, vai lá.",
        "🔥 Sua sequência de dias corre perigo!" to "Um dia sem jogar e ela volta pro começo.",
        "🌱 Sementes grátis te esperando na barraca." to "É de graça. Literalmente.",
        "✨ Poder novo desbloqueado na feira." to "Dá pra pegar assistindo um vídeo rapidinho.",
    )

    private val receita = listOf(
        "🧾 A Receita do Dia mudou!" to "Pedido novo, jogadas contadas. Bora?",
        "📅 A receita de hoje está aberta." to "Todo mundo joga a mesma. Sem desculpa.",
        "👨‍🍳 O freguês chegou com um pedido." to "Ele disse que só você sabe montar.",
        "🏅 Tem conquista quase fechando." to "Falta pouquinho. Vem terminar!",
    )

    fun pick(kind: String, seed: Long = System.currentTimeMillis()): Pair<String, String> {
        val list = when (kind) {
            KIND_DESAFIO -> desafio
            KIND_ECONOMIA -> economia
            KIND_RECEITA -> receita
            else -> saudade
        }
        return list[Random(seed).nextInt(list.size)]
    }

    const val KIND_SAUDADE = "saudade"
    const val KIND_DESAFIO = "desafio"
    const val KIND_ECONOMIA = "economia"
    const val KIND_RECEITA = "receita"
}

object Notifier {

    const val CHANNEL_ID = "format_frute_feira"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recados da Feira",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Lembretes engraçados, presentes do dia e desafios novos."
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, title: String, body: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(title.hashCode(), notification)
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val repo = GameRepository.get(applicationContext)
        if (!repo.current.notifications) return Result.success()

        val kind = inputData.getString(KEY_KIND) ?: FunnyMessages.KIND_SAUDADE
        val (title, body) = FunnyMessages.pick(kind)
        Notifier.show(applicationContext, title, body)
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
    }
}

/**
 * Agenda os recados. A ideia e voltar a lembrar sem virar praga: um por dia
 * no fim da tarde, mais um empurraozinho para quem sumiu por dias.
 */
object ReminderScheduler {

    private const val DAILY = "ff_daily"
    private const val COMEBACK_1 = "ff_comeback_1"
    private const val COMEBACK_3 = "ff_comeback_3"
    private const val COMEBACK_7 = "ff_comeback_7"

    fun scheduleAll(context: Context) {
        val repo = GameRepository.get(context)
        val work = WorkManager.getInstance(context.applicationContext)
        if (!repo.current.notifications) {
            cancelAll(context)
            return
        }

        val daily = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilHour(19), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_KIND to FunnyMessages.KIND_ECONOMIA))
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        work.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.UPDATE, daily)

        comeback(context, COMEBACK_1, 1, FunnyMessages.KIND_SAUDADE)
        comeback(context, COMEBACK_3, 3, FunnyMessages.KIND_DESAFIO)
        comeback(context, COMEBACK_7, 7, FunnyMessages.KIND_RECEITA)
    }

    /** Chamado quando o jogador abre o app: reprograma os "volta aqui". */
    fun refreshComebacks(context: Context) {
        val repo = GameRepository.get(context)
        if (!repo.current.notifications) return
        comeback(context, COMEBACK_1, 1, FunnyMessages.KIND_SAUDADE)
        comeback(context, COMEBACK_3, 3, FunnyMessages.KIND_DESAFIO)
        comeback(context, COMEBACK_7, 7, FunnyMessages.KIND_RECEITA)
    }

    private fun comeback(context: Context, name: String, days: Long, kind: String) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(days, TimeUnit.DAYS)
            .setInputData(workDataOf(ReminderWorker.KEY_KIND to kind))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        val work = WorkManager.getInstance(context.applicationContext)
        listOf(DAILY, COMEBACK_1, COMEBACK_3, COMEBACK_7).forEach { work.cancelUniqueWork(it) }
    }

    private fun delayUntilHour(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, (0..40).random())
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
