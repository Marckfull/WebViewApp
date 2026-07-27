package com.neonsombra.game.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neonsombra.game.ui.screens.GameScreen
import com.neonsombra.game.ui.screens.MenuScreen
import com.neonsombra.game.ui.screens.SettingsScreen
import com.neonsombra.game.ui.screens.SplashScreen
import com.neonsombra.game.ui.screens.TermsScreen
import com.neonsombra.game.ui.screens.TutorialScreen
import com.neonsombra.game.ui.theme.NeonNight

private object Routes {
    const val SPLASH = "splash"
    const val TERMS_FIRST = "termos_primeira_vez"
    const val TERMS_READ = "termos_leitura"
    const val TUTORIAL_FIRST = "tutorial_primeira_vez"
    const val TUTORIAL_READ = "tutorial_leitura"
    const val MENU = "menu"
    const val GAME = "jogo"
    const val SETTINGS = "configuracoes"
}

/**
 * Fluxo do app: abertura -> termos (so na primeira vez) -> tutorial (so na
 * primeira vez) -> menu -> jogo.
 */
@Composable
fun NeonSombraApp() {
    val navController = rememberNavController()
    val prefs = LocalPrefs.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonNight),
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            enterTransition = { fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 8 } },
            exitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 8 } },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { fadeOut(tween(220)) },
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val destination = when {
                            !prefs.termsAccepted -> Routes.TERMS_FIRST
                            !prefs.tutorialSeen -> Routes.TUTORIAL_FIRST
                            else -> Routes.MENU
                        }
                        navController.replaceWith(destination, Routes.SPLASH)
                    },
                )
            }

            composable(Routes.TERMS_FIRST) {
                TermsScreen(
                    requireAcceptance = true,
                    onAccept = {
                        prefs.termsAccepted = true
                        val destination =
                            if (prefs.tutorialSeen) Routes.MENU else Routes.TUTORIAL_FIRST
                        navController.replaceWith(destination, Routes.TERMS_FIRST)
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TERMS_READ) {
                TermsScreen(
                    requireAcceptance = false,
                    onAccept = {},
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TUTORIAL_FIRST) {
                TutorialScreen(
                    onFinished = {
                        prefs.tutorialSeen = true
                        navController.replaceWith(Routes.MENU, Routes.TUTORIAL_FIRST)
                    },
                )
            }

            composable(Routes.TUTORIAL_READ) {
                TutorialScreen(onFinished = { navController.popBackStack() })
            }

            composable(Routes.MENU) {
                MenuScreen(
                    onPlay = { navController.navigate(Routes.GAME) },
                    onTutorial = { navController.navigate(Routes.TUTORIAL_READ) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onTerms = { navController.navigate(Routes.TERMS_READ) },
                )
            }

            composable(Routes.GAME) {
                GameScreen(onExitToMenu = { navController.popBackStack(Routes.MENU, false) })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Vai para [destination] e tira [from] da pilha, para o botao voltar nao regressar. */
private fun NavHostController.replaceWith(destination: String, from: String) {
    navigate(destination) {
        popUpTo(from) { inclusive = true }
        launchSingleTop = true
    }
}
