package com.kardiapulse.game

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kardiapulse.game.ads.ConsentManager
import com.kardiapulse.game.audio.MusicTrack
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.notifications.Notifier
import com.kardiapulse.game.notifications.TauntWorker
import com.kardiapulse.game.ui.daily.DailyPassScreen
import com.kardiapulse.game.ui.extras.ExtrasScreen
import com.kardiapulse.game.ui.game.GameScreen
import com.kardiapulse.game.ui.home.HomeActions
import com.kardiapulse.game.ui.home.HomeScreen
import com.kardiapulse.game.ui.legal.LegalContent
import com.kardiapulse.game.ui.legal.LegalDocumentScreen
import com.kardiapulse.game.ui.legal.LegalGateScreen
import com.kardiapulse.game.ui.modes.ModeSelectScreen
import com.kardiapulse.game.ui.profile.ProfileScreen
import com.kardiapulse.game.ui.settings.SettingsScreen
import com.kardiapulse.game.ui.shop.ShopScreen
import com.kardiapulse.game.ui.splash.SplashScreen
import com.kardiapulse.game.ui.theme.KardiaTheme
import com.kardiapulse.game.ui.tutorial.TutorialScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val MODES = "modes"
    const val GAME = "game/{mode}/{difficulty}"
    const val TUTORIAL = "tutorial"
    const val SHOP = "shop"
    const val PROFILE = "profile"
    const val DAILY = "daily"
    const val EXTRAS = "extras"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"

    fun game(mode: GameMode, difficulty: Difficulty) = "game/${mode.name}/${difficulty.name}"
}

class MainActivity : ComponentActivity() {

    private lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val services = (application as KardiaApp).services
        consentManager = ConsentManager(this)

        // Consentimento primeiro, anúncios depois. Se o usuário recusar, o jogo segue sem anúncios.
        consentManager.gather { allowed ->
            services.ads.initialize(applicationContext, allowed)
        }

        setContent {
            KardiaTheme {
                CompositionLocalProvider(LocalServices provides services) {
                    KardiaRoot(
                        onShowPrivacyOptions = { consentManager.showPrivacyOptions {} }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (application as KardiaApp).services.audio.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        (application as KardiaApp).services.audio.resumeMusic()
    }
}

@Composable
private fun KardiaRoot(onShowPrivacyOptions: () -> Unit) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val context = LocalContext.current

    // `null` enquanto o DataStore ainda não respondeu: evita a tela legal piscar por engano.
    val profile by services.repository.profile.collectAsState(initial = null)
    var splashDone by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* seguimos com ou sem permissão */ }

    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notifier.hasPermission(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val loaded = profile
    if (!splashDone || loaded == null) {
        SplashScreen(onDone = { splashDone = true })
        return
    }

    if (loaded.legalAcceptedVersion < LegalContent.VERSION) {
        LegalGateScreen(
            onAccept = {
                scope.launch {
                    services.repository.update {
                        it.copy(legalAcceptedVersion = LegalContent.VERSION)
                    }
                }
            }
        )
        return
    }

    // Android 13+ exige pedir a permissão de notificação. Pedimos uma única vez, depois do
    // aceite dos termos — nunca na cara do jogador antes dele saber o que o app é.
    var askedForNotifications by remember { mutableStateOf(false) }
    LaunchedEffect(loaded.notificationsOn) {
        if (!askedForNotifications && loaded.notificationsOn && !Notifier.hasPermission(context)) {
            askedForNotifications = true
            askNotificationPermission()
        }
    }

    // A trilha do menu volta sempre que saímos de um duelo.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && !currentRoute.startsWith("game/") && currentRoute != Routes.TUTORIAL) {
            services.audio.play(MusicTrack.MENU)
        }
    }

    // O agendamento das provocações acompanha a preferência do jogador.
    LaunchedEffect(loaded.notificationsOn) {
        if (loaded.notificationsOn) TauntWorker.schedule(context) else TauntWorker.cancel(context)
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                profile = loaded,
                actions = HomeActions(
                    onPlay = { navController.navigate(Routes.MODES) },
                    onTutorial = { navController.navigate(Routes.TUTORIAL) },
                    onDaily = { navController.navigate(Routes.DAILY) },
                    onDailyChallenge = {
                        navController.navigate(Routes.game(GameMode.DIARIO, Difficulty.DIFICIL))
                    },
                    onShop = { navController.navigate(Routes.SHOP) },
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    onExtras = { navController.navigate(Routes.EXTRAS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onTerms = { navController.navigate(Routes.TERMS) }
                )
            )
        }

        composable(Routes.MODES) {
            ModeSelectScreen(
                endlessBest = loaded.endlessBest,
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
                GameMode.valueOf(entry.arguments?.getString("mode") ?: "")
            }.getOrDefault(GameMode.DUELO)
            val difficulty = runCatching {
                Difficulty.valueOf(entry.arguments?.getString("difficulty") ?: "")
            }.getOrDefault(Difficulty.NORMAL)

            GameScreen(
                mode = mode,
                difficulty = difficulty,
                onExit = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.TUTORIAL) {
            TutorialScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.SHOP) {
            ShopScreen(profile = loaded, onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                profile = loaded,
                trustworthyDevice = services.integrity.trustworthy,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DAILY) {
            DailyPassScreen(profile = loaded, onBack = { navController.popBackStack() })
        }

        composable(Routes.EXTRAS) {
            ExtrasScreen(profile = loaded, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                profile = loaded,
                onBack = { navController.popBackStack() },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onTerms = { navController.navigate(Routes.TERMS) },
                onRequestNotificationPermission = { askNotificationPermission() },
                onShowPrivacyOptions = onShowPrivacyOptions,
                onProgressReset = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.PRIVACY) {
            LegalDocumentScreen(
                title = "Política de Privacidade",
                sections = LegalContent.PRIVACY,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TERMS) {
            LegalDocumentScreen(
                title = "Termos de Uso",
                sections = LegalContent.TERMS,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
