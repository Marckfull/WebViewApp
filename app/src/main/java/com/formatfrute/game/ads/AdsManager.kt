package com.formatfrute.game.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.formatfrute.game.BuildConfig
import com.formatfrute.game.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * IDs do AdMob. Os valores abaixo sao os oficiais de TESTE do Google — o app
 * ja roda com anuncio de verdade na tela sem risco de banimento. Troque pelos
 * IDs da sua conta antes de publicar (e o app id no AndroidManifest).
 */
object AdsConfig {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // TODO: publicar -> colar aqui os IDs reais da conta AdMob do Format Frute.
    private const val PROD_INTERSTITIAL = TEST_INTERSTITIAL
    private const val PROD_REWARDED = TEST_REWARDED

    val interstitial: String get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL
    val rewarded: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED
}

/** Por que o anuncio premiado esta sendo aberto — vira texto na tela de oferta. */
enum class RewardReason(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val emoji: String,
) {
    PODER(R.string.reward_poder_title, R.string.reward_poder_sub, "✨"),
    REVIVER(R.string.reward_reviver_title, R.string.reward_reviver_sub, "❤️"),
    DOBRAR(R.string.reward_dobrar_title, R.string.reward_dobrar_sub, "🌱"),
    TEMPO(R.string.reward_tempo_title, R.string.reward_tempo_sub, "⏱️"),
    DIARIO(R.string.reward_diario_title, R.string.reward_diario_sub, "🎁"),
    LOJA(R.string.reward_loja_title, R.string.reward_loja_sub, "🪙"),
}

/**
 * Anuncios do jogo. A regra e simples e respeitosa: intersticial so entre
 * partidas e com folga de tempo; premiado sempre por escolha do jogador.
 */
object AdsManager {

    private const val TAG = "FormatFruteAds"
    private const val MIN_GAP_MS = 95_000L
    private const val GAMES_BEFORE_FIRST = 2

    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false

    private var lastInterstitialAt = 0L
    private var gamesFinished = 0

    var available: Boolean = false
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        runCatching {
            // O Format Frute é para todas as idades: mesmo sem entrar no
            // programa Famílias, o anúncio precisa ser aceitável para uma
            // criança jogando ao lado do adulto. "G" é a classificação de
            // conteúdo geral do AdMob — nada de bebida, aposta ou violência.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build()
            )
            MobileAds.initialize(context.applicationContext) {
                available = true
                preload(context)
            }
        }.onFailure { Log.w(TAG, "MobileAds indisponível: ${it.message}") }
    }

    fun preload(context: Context) {
        loadInterstitial(context)
        loadRewarded(context)
    }

    // --------------------------------------------------------- intersticial

    private fun loadInterstitial(context: Context) {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        runCatching {
            InterstitialAd.load(
                context.applicationContext,
                AdsConfig.interstitial,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        loadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitial = null
                        loadingInterstitial = false
                        Log.d(TAG, "intersticial falhou: ${error.message}")
                    }
                },
            )
        }.onFailure { loadingInterstitial = false }
    }

    fun onGameFinished() {
        gamesFinished++
    }

    /**
     * Mostra o intersticial se for a hora certa. Devolve false quando decide
     * nao atrapalhar — nesse caso a UI segue direto.
     */
    fun maybeShowInterstitial(activity: Activity, onClosed: () -> Unit): Boolean {
        val now = System.currentTimeMillis()
        val ad = interstitial
        if (ad == null || gamesFinished < GAMES_BEFORE_FIRST || now - lastInterstitialAt < MIN_GAP_MS) {
            loadInterstitial(activity)
            return false
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastInterstitialAt = System.currentTimeMillis()
                loadInterstitial(activity)
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial(activity)
                onClosed()
            }
        }
        runCatching { ad.show(activity) }.onFailure {
            interstitial = null
            onClosed()
            return false
        }
        return true
    }

    // ------------------------------------------------------------ premiado

    private fun loadRewarded(context: Context) {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        runCatching {
            RewardedAd.load(
                context.applicationContext,
                AdsConfig.rewarded,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewarded = ad
                        loadingRewarded = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewarded = null
                        loadingRewarded = false
                        Log.d(TAG, "premiado falhou: ${error.message}")
                    }
                },
            )
        }.onFailure { loadingRewarded = false }
    }

    val rewardedReady: Boolean get() = rewarded != null

    /**
     * Abre o premiado. [onResult] recebe true quando o jogador assistiu ate o
     * fim e merece o premio.
     */
    fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded(activity)
            onResult(false)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                loadRewarded(activity)
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                loadRewarded(activity)
                onResult(false)
            }
        }
        runCatching {
            ad.show(activity) { earned = true }
        }.onFailure {
            rewarded = null
            onResult(false)
        }
    }
}

/**
 * Consentimento (UMP). Necessario para anuncios personalizados na Europa e
 * exigido pelas politicas do Google Play.
 */
object ConsentManager {

    private var info: ConsentInformation? = null

    fun request(activity: Activity, onDone: () -> Unit) {
        runCatching {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            val consent = UserMessagingPlatform.getConsentInformation(activity)
            info = consent
            consent.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onDone() }
                },
                { onDone() },
            )
        }.onFailure { onDone() }
    }

    fun canShowPrivacyOptions(): Boolean =
        info?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        runCatching { UserMessagingPlatform.showPrivacyOptionsForm(activity) { } }
    }
}
