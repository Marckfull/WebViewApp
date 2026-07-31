package com.prisma.fusao.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.ads.RewardedOffer
import com.prisma.fusao.data.DailyReward
import com.prisma.fusao.data.GameRepository
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.DailyRewardDialog
import com.prisma.fusao.ui.components.RewardedOfferDialog
import com.prisma.fusao.ui.screens.ConsentScreen
import com.prisma.fusao.ui.screens.GameScreen
import com.prisma.fusao.ui.screens.HomeScreen
import com.prisma.fusao.ui.screens.LegalDocument
import com.prisma.fusao.ui.screens.LegalScreen
import com.prisma.fusao.ui.screens.MissionsScreen
import com.prisma.fusao.ui.screens.SettingsScreen
import com.prisma.fusao.ui.screens.SplashScreen
import com.prisma.fusao.ui.screens.StoreScreen
import com.prisma.fusao.ui.screens.TutorialScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    const val SPLASH = "splash"
    const val CONSENT = "consent"
    const val TUTORIAL = "tutorial"
    const val HOME = "home"
    const val STORE = "store"
    const val SETTINGS = "settings"
    const val MISSIONS = "missions"
    const val GAME = "game/{level}"
    const val LEGAL = "legal/{doc}"

    fun game(level: Int) = "game/$level"
    fun legal(document: LegalDocument) = "legal/${document.name}"
}

/**
 * Fluxo do app. A ordem da primeira execução é fixa e não pode ser pulada:
 * splash -> aceite dos termos -> tutorial -> mapa.
 */
@Composable
fun PrismaNavHost(activity: Activity) {
    val app = LocalContext.current.applicationContext as PrismaApplication
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // null enquanto o DataStore ainda não respondeu: evita mandar um jogador antigo
    // de volta para a tela de aceite só porque o estado ainda estava carregando.
    val player by produceState<PlayerState?>(initialValue = null) {
        app.repository.state.collect { value = it }
    }

    var splashDone by remember { mutableStateOf(false) }
    var dailyReward by remember { mutableStateOf<DailyReward?>(null) }
    var pendingDailyDouble by remember { mutableStateOf(false) }

    // O roteamento inicial acontece uma única vez. Sem esta trava, qualquer gravação
    // no DataStore (terminar uma fase, ganhar moedas) emitiria um novo estado e
    // jogaria o jogador de volta para o mapa no meio da partida.
    var initialRouteDone by remember { mutableStateOf(false) }

    LaunchedEffect(splashDone, player, initialRouteDone) {
        val current = player
        if (initialRouteDone || !splashDone || current == null) return@LaunchedEffect
        val destination = when {
            current.acceptedTermsVersion < GameRepository.CURRENT_TERMS_VERSION -> Routes.CONSENT
            !current.tutorialCompleted -> Routes.TUTORIAL
            else -> Routes.HOME
        }
        initialRouteDone = true
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    // O relógio das vidas: atualiza o contador na tela sem gravar nada em disco.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onFinished = { splashDone = true })
        }

        composable(Routes.CONSENT) {
            ConsentScreen(
                onAccept = {
                    scope.launch {
                        app.repository.acceptTerms()
                        navController.navigate(Routes.TUTORIAL) {
                            popUpTo(Routes.CONSENT) { inclusive = true }
                        }
                    }
                },
                onOpenDocument = { navController.navigate(Routes.legal(it)) },
            )
        }

        composable(Routes.TUTORIAL) {
            TutorialScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.TUTORIAL) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val current = player ?: PlayerState()
            HomeScreen(
                player = current,
                onPlayLevel = { navController.navigate(Routes.game(it)) },
                onOpenStore = { navController.navigate(Routes.STORE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenDailyReward = {
                    scope.launch { dailyReward = app.repository.claimDailyReward() }
                },
                canClaimDaily = app.repository.canClaimDaily(current),
                millisToNextLife = current.millisToNextLife(now),
            )
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("level") { type = NavType.IntType }),
        ) { entry ->
            val level = entry.arguments?.getInt("level") ?: 1
            GameScreen(
                levelIndex = level,
                activity = activity,
                onExit = { navController.popBackStackTo(Routes.HOME) },
                onOpenStore = { navController.navigate(Routes.STORE) },
                onNextLevel = { next ->
                    // Cada fase ganha sua própria entrada de rota (e seu próprio
                    // ViewModel), mas sem empilhar: voltar leva ao mapa.
                    navController.navigate(Routes.game(next)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STORE) {
            StoreScreen(activity = activity, onBack = { navController.popBackStack() })
        }

        composable(Routes.MISSIONS) {
            MissionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                activity = activity,
                onBack = { navController.popBackStack() },
                onOpenDocument = { navController.navigate(Routes.legal(it)) },
                onDataErased = {
                    // Apagar tudo devolve o jogador para o começo do fluxo.
                    navController.navigate(Routes.CONSENT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.LEGAL,
            arguments = listOf(navArgument("doc") { type = NavType.StringType }),
        ) { entry ->
            val document = runCatching {
                LegalDocument.valueOf(entry.arguments?.getString("doc") ?: "")
            }.getOrDefault(LegalDocument.TERMS)
            LegalScreen(document = document, onBack = { navController.popBackStack() })
        }
    }

    dailyReward?.let { reward ->
        val current = player ?: PlayerState()
        DailyRewardDialog(
            streak = reward.day,
            reward = reward,
            canDouble = reward.coins > 0 &&
                current.rewardedWatchedToday < PlayerState.MAX_REWARDED_PER_DAY,
            onDouble = { pendingDailyDouble = true },
            onClose = { dailyReward = null },
        )
    }

    if (pendingDailyDouble) {
        val current = player ?: PlayerState()
        val ready by app.adsManager.rewardedReady.collectAsStateWithLifecycle()
        RewardedOfferDialog(
            offer = RewardedOffer.DAILY_BONUS,
            available = ready,
            unavailableMessage = if (!ready) {
                "Nenhum vídeo disponível agora. Tente de novo em instantes."
            } else {
                null
            },
            onWatch = {
                app.adsManager.showRewarded(
                    activity = activity,
                    watchedToday = current.rewardedWatchedToday,
                    onReward = {
                        scope.launch {
                            app.repository.registerRewardedWatched()
                            dailyReward?.let { app.repository.grantCoins(it.coins) }
                        }
                    },
                    onDismissed = {
                        pendingDailyDouble = false
                        dailyReward = null
                    },
                )
            },
            onDecline = { pendingDailyDouble = false },
        )
    }
}

/** Volta até uma tela já existente na pilha, sem empilhar outra cópia dela. */
private fun NavHostController.popBackStackTo(route: String) {
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { popUpTo(0) { inclusive = true } }
    }
}
