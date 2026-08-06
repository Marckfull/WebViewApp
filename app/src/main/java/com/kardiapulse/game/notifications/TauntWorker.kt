package com.kardiapulse.game.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kardiapulse.game.data.DailyPass
import com.kardiapulse.game.data.PrefsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Decide, uma vez por dia, se vale a pena cutucar o jogador — e com qual provocação.
 *
 * O worker lê o próprio perfil para escolher o tom: quem está prestes a perder a sequência
 * recebe um aviso diferente de quem simplesmente perdeu o último duelo. Se o jogador já jogou
 * hoje, ninguém é incomodado.
 */
class TauntWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profile = runCatching { PrefsRepository(applicationContext).profile.first() }
            .getOrNull() ?: return Result.success()

        if (!profile.notificationsOn) return Result.success()
        if (!Notifier.hasPermission(applicationContext)) return Result.success()

        val today = DailyPass.todayEpochDay()

        // Já apareceu hoje: deixa a pessoa em paz.
        if (profile.lastClaimEpochDay == today) return Result.success()

        val daysAway = if (profile.lastClaimEpochDay < 0) 99
        else (today - profile.lastClaimEpochDay).toInt()

        val kind = when {
            profile.streakDays >= 3 && daysAway == 1 -> Taunts.Kind.SEQUENCIA
            profile.dailyChallengeDay != today && profile.duelsPlayed > 3 -> Taunts.Kind.DESAFIO
            daysAway >= 3 -> Taunts.Kind.SAUDADE
            profile.duelsPlayed > 0 && profile.duelsWon * 2 < profile.duelsPlayed -> Taunts.Kind.REVANCHE
            else -> Taunts.Kind.CONVITE
        }

        Notifier.show(applicationContext, Taunts.pick(kind, today.toInt()))
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "kardia_taunt_daily"

        /** Agenda a checagem diária para a tarde/noite, quando as pessoas realmente jogam. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TauntWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(millisUntilEveningHour(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun millisUntilEveningHour(hour: Int = 19): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
