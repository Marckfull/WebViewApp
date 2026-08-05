package com.formatfrute.game.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.formatfrute.game.R
import com.formatfrute.game.data.BoardTheme
import com.formatfrute.game.data.Mission
import com.formatfrute.game.data.PassReward
import com.formatfrute.game.data.RewardKind
import com.formatfrute.game.core.Power

/**
 * Frases montadas a partir de mais de um recurso.
 *
 * Elas vivem na UI porque é aqui que existe Context para resolver — as classes
 * de dados guardam só os ids, e continuam puras e testáveis na JVM.
 */

/** "Junte 50 frutas", "Chegue na Pera", "Jogue o modo Zen do Pomar"… */
@Composable
fun Mission.label(): String {
    val arg = argRes
    return if (arg != null) {
        stringResource(titleRes, stringResource(arg))
    } else {
        stringResource(titleRes, target)
    }
}

/** "120 sementes", "Martelinho x2", "Feira da Meia-Noite", "Rei da Cesta". */
@Composable
fun PassReward.label(): String = label(LocalContext.current)

/** Mesma frase, para quem só tem Context (callback de anúncio, por exemplo). */
fun PassReward.label(context: Context): String = when (kind) {
    RewardKind.SEMENTES -> context.getString(R.string.pass_reward_seeds, amount)
    RewardKind.PODER -> context.getString(
        R.string.pass_reward_power,
        context.getString(Power.byId(powerId)?.title ?: R.string.power_martelo_title),
        amount,
    )
    RewardKind.PELE -> context.getString(BoardTheme.byId(themeId).title)
    RewardKind.TITULO -> context.getString(title)
}
