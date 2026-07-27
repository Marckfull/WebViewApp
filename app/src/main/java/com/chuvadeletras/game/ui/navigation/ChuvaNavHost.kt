package com.chuvadeletras.game.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chuvadeletras.game.appContainer
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.ui.game.GameScreen
import com.chuvadeletras.game.ui.game.GameViewModel
import com.chuvadeletras.game.ui.home.HomeScreen
import com.chuvadeletras.game.ui.home.HomeViewModel
import com.chuvadeletras.game.ui.legal.PrivacyScreen
import com.chuvadeletras.game.ui.legal.TermsScreen
import com.chuvadeletras.game.ui.modes.ModeSelectScreen
import com.chuvadeletras.game.ui.progress.AchievementsScreen
import com.chuvadeletras.game.ui.progress.MissionsScreen
import com.chuvadeletras.game.ui.progress.SettingsScreen
import com.chuvadeletras.game.ui.progress.ShopScreen
import com.chuvadeletras.game.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val MODES = "modos"
    const val GAME = "jogo/{mode}/{difficulty}"
    const val MISSIONS = "missoes"
    const val ACHIEVEMENTS = "conquistas"
    const val SHOP = "loja"
    const val SETTINGS = "ajustes"
    const val TERMS = "termos"
    const val PRIVACY = "privacidade"

    fun game(mode: GameMode, difficulty: Difficulty) = "jogo/${mode.name}/${difficulty.name}"
}

@Composable
fun ChuvaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val container = LocalContext.current.appContainer

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container.playerRepository, container.adHost)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        enterTransition = { slideInHorizontally { it / 6 } + fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally { it / 6 } + fadeOut(tween(160)) }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onPlay = { navController.navigate(Routes.MODES) },
                onDailyChallenge = {
                    navController.navigate(Routes.game(GameMode.DIARIO, Difficulty.MEDIO))
                },
                onMissions = { navController.navigate(Routes.MISSIONS) },
                onAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onShop = { navController.navigate(Routes.SHOP) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.MODES) {
            ModeSelectScreen(
                viewModel = homeViewModel,
                onStart = { mode, difficulty ->
                    navController.navigate(Routes.game(mode, difficulty))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { entry ->
            val mode = runCatching {
                GameMode.valueOf(entry.arguments?.getString("mode").orEmpty())
            }.getOrDefault(GameMode.CLASSICO)
            val difficulty = runCatching {
                Difficulty.valueOf(entry.arguments?.getString("difficulty").orEmpty())
            }.getOrDefault(Difficulty.FACIL)

            // A chave garante uma partida nova a cada entrada nesta rota.
            val gameViewModel: GameViewModel = viewModel(
                key = "game-${mode.name}-${difficulty.name}-${entry.id}",
                factory = GameViewModel.Factory(
                    mode = mode,
                    difficulty = difficulty,
                    playerRepository = container.playerRepository,
                    puzzleRepository = container.puzzleRepository,
                    adHost = container.adHost
                )
            )

            GameScreen(
                viewModel = gameViewModel,
                onExit = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.MISSIONS) {
            MissionsScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SHOP) {
            ShopScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() },
                onTerms = { navController.navigate(Routes.TERMS) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onReplayTutorial = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.TERMS) {
            TermsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}
