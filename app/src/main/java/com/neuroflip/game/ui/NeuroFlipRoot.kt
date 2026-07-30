package com.neuroflip.game.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neuroflip.game.ads.ConsentManager
import com.neuroflip.game.domain.GameMode
import com.neuroflip.game.ui.screens.ConsentGateScreen
import com.neuroflip.game.ui.screens.GameScreen
import com.neuroflip.game.ui.screens.HomeScreen
import com.neuroflip.game.ui.screens.LegalScreen
import com.neuroflip.game.ui.screens.LevelMapScreen
import com.neuroflip.game.ui.screens.SettingsScreen
import com.neuroflip.game.ui.screens.ShopScreen
import com.neuroflip.game.ui.screens.SplashScreen
import com.neuroflip.game.ui.theme.LocalNeuro
import kotlinx.coroutines.delay

private object Routes {
    const val SPLASH = "splash"
    const val GATE = "gate"
    const val HOME = "home"
    const val LEVELS = "levels"
    const val GAME = "game"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val LEGAL = "legal/{doc}"
    fun legal(doc: String) = "legal/$doc"
}

@Composable
fun NeuroFlipRoot(
    appVm: AppViewModel,
    gameVm: GameViewModel,
    consent: ConsentManager?
) {
    val navController = rememberNavController()
    val player by appVm.player.collectAsStateWithLifecycle()
    val toast by appVm.toast.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_600)
            appVm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.SPLASH) {

            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val next = if (player.legalAccepted) Routes.HOME else Routes.GATE
                        navController.navigate(next) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.GATE) {
                ConsentGateScreen(
                    onAccept = {
                        appVm.acceptLegal()
                        activity?.let { act -> consent?.gather(act) { } }
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.GATE) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                LaunchedEffect(Unit) { appVm.playMenuMusic() }
                HomeScreen(
                    player = player,
                    daily = appVm.dailyReward(),
                    onCampaign = {
                        appVm.tap()
                        navController.navigate(Routes.LEVELS)
                    },
                    onBlitz = {
                        appVm.tap()
                        gameVm.startLevel(GameMode.BLITZ, 0)
                        navController.navigate(Routes.GAME)
                    },
                    onZen = {
                        appVm.tap()
                        gameVm.startLevel(GameMode.ZEN, 0)
                        navController.navigate(Routes.GAME)
                    },
                    onShop = {
                        appVm.tap()
                        navController.navigate(Routes.SHOP)
                    },
                    onSettings = {
                        appVm.tap()
                        navController.navigate(Routes.SETTINGS)
                    },
                    onLegal = { doc -> navController.navigate(Routes.legal(doc)) },
                    onClaimDaily = { appVm.claimDaily() },
                    onClaimDailyWithAd = { activity?.let { appVm.watchForDaily(it) } }
                )
            }

            composable(Routes.LEVELS) {
                LevelMapScreen(
                    player = player,
                    onPlay = { level ->
                        appVm.tap()
                        gameVm.startLevel(GameMode.CAMPAIGN, level)
                        navController.navigate(Routes.GAME)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.GAME) {
                GameScreen(
                    vm = gameVm,
                    player = player,
                    onExit = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                    onNextLevel = { level ->
                        activity?.let { act ->
                            appVm.ads.maybeShowInterstitial(act) {
                                gameVm.startLevel(GameMode.CAMPAIGN, level)
                            }
                        } ?: gameVm.startLevel(GameMode.CAMPAIGN, level)
                    }
                )
            }

            composable(Routes.SHOP) {
                ShopScreen(
                    player = player,
                    onBack = { navController.popBackStack() },
                    onBuyPowerUp = { appVm.buyPowerUp(it) },
                    onWatchForNeurons = { activity?.let { appVm.watchForNeurons(it) } },
                    onWatchForPowerUp = { kind ->
                        activity?.let { appVm.watchForPowerUp(it, kind) }
                    },
                    onSelectTheme = { appVm.selectTheme(it) },
                    onBuyTheme = { appVm.buyTheme(it) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    player = player,
                    privacyOptionsRequired = consent?.isPrivacyOptionsRequired ?: false,
                    onBack = { navController.popBackStack() },
                    onMusic = { appVm.setMusic(it) },
                    onSfx = { appVm.setSfx(it) },
                    onHaptics = { appVm.setHaptics(it) },
                    onPrivacyOptions = {
                        activity?.let { act -> consent?.showPrivacyOptions(act) }
                    },
                    onLegal = { doc -> navController.navigate(Routes.legal(doc)) }
                )
            }

            composable(
                route = Routes.LEGAL,
                arguments = listOf(navArgument("doc") { type = NavType.StringType })
            ) { entry ->
                LegalScreen(
                    document = entry.arguments?.getString("doc") ?: "terms",
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Aviso flutuante (compras, recompensas, erros)
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
        ) {
            val palette = LocalNeuro.current
            Text(
                text = toast.orEmpty(),
                color = palette.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            )
        }
    }
}
