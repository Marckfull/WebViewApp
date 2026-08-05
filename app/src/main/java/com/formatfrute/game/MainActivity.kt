package com.formatfrute.game

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.formatfrute.game.ads.AdsManager
import com.formatfrute.game.ads.ConsentManager
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.audio.Track
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.notify.Notifier
import com.formatfrute.game.notify.ReminderScheduler
import com.formatfrute.game.ui.game.GameScreen
import com.formatfrute.game.ui.home.HomeScreen
import com.formatfrute.game.ui.legal.LegalScreen
import com.formatfrute.game.ui.legal.LegalTab
import com.formatfrute.game.ui.pass.PassScreen
import com.formatfrute.game.ui.recipe.RecipeMapScreen
import com.formatfrute.game.ui.settings.SettingsScreen
import com.formatfrute.game.ui.shop.ShopScreen
import com.formatfrute.game.ui.splash.SplashScreen
import com.formatfrute.game.ui.theme.FormatFruteTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) ReminderScheduler.scheduleAll(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Consentimento primeiro, anuncios depois — nessa ordem, sempre.
        ConsentManager.request(this) { AdsManager.init(this) }

        setContent {
            val repo = remember { GameRepository.get(this) }
            val profile by repo.profile.collectAsStateWithLifecycle()

            FormatFruteTheme(dark = profile.boardTheme.dark) {
                FormatFruteNav(
                    onAskNotifications = { askNotificationPermission() },
                )
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (Notifier.hasPermission(this)) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        SoundManager.get(this).resumeMusic()
    }

    override fun onPause() {
        SoundManager.get(this).pauseMusic()
        super.onPause()
    }
}

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game/{mode}/{tutorial}/{recipe}"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val LEGAL = "legal/{tab}"
    const val RECIPES = "recipes"
    const val PASS = "pass"

    fun game(mode: GameMode, tutorial: Boolean = false, recipe: Int = 1) =
        "game/${mode.id}/$tutorial/$recipe"

    fun legal(tab: LegalTab) = "legal/${tab.name}"
}

@Composable
private fun FormatFruteNav(onAskNotifications: () -> Unit) {
    val nav: NavHostController = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onDone = {
                    onAskNotifications()
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val context = LocalContext.current
            val sound = remember { SoundManager.get(context) }
            LaunchedEffect(Unit) { sound.music(Track.MENU) }

            HomeScreen(
                onPlay = { mode -> nav.navigate(Routes.game(mode)) },
                onTutorial = { nav.navigate(Routes.game(GameMode.POMAR, tutorial = true)) },
                onShop = { nav.navigate(Routes.SHOP) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onRecipes = { nav.navigate(Routes.RECIPES) },
                onPass = { nav.navigate(Routes.PASS) },
            )
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("tutorial") { type = NavType.BoolType },
                navArgument("recipe") { type = NavType.IntType },
            ),
        ) { entry ->
            val mode = GameMode.byId(entry.arguments?.getString("mode"))
            val tutorial = entry.arguments?.getBoolean("tutorial") ?: false
            val recipe = entry.arguments?.getInt("recipe") ?: 1
            GameScreen(
                mode = mode,
                tutorial = tutorial,
                recipeNumber = recipe,
                onExit = { nav.popBackStack() },
                onShop = { nav.navigate(Routes.SHOP) },
            )
        }

        composable(Routes.RECIPES) {
            RecipeMapScreen(
                onPlay = { number ->
                    nav.navigate(Routes.game(GameMode.RECEITA, recipe = number))
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.PASS) {
            PassScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.SHOP) {
            ShopScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onLegal = { tab -> nav.navigate(Routes.legal(tab)) },
                onRestartTutorial = {
                    nav.navigate(Routes.game(GameMode.POMAR, true)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(
            route = Routes.LEGAL,
            arguments = listOf(navArgument("tab") { type = NavType.StringType }),
        ) { entry ->
            val tab = runCatching {
                LegalTab.valueOf(entry.arguments?.getString("tab") ?: LegalTab.TERMOS.name)
            }.getOrDefault(LegalTab.TERMOS)
            LegalScreen(initialTab = tab, onBack = { nav.popBackStack() })
        }
    }
}
