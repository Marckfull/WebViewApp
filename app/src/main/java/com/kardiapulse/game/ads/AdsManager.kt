package com.kardiapulse.game.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.kardiapulse.game.BuildConfig

/** Para que serve cada anúncio premiado. Cada um tem um gancho diferente na economia. */
enum class RewardedPurpose(val title: String, val description: String, val glyph: String) {
    DOBRAR_RECOMPENSA(
        "Dobrar recompensa",
        "Assista a um anúncio e leve o dobro de Fragmentos e XP deste duelo.",
        "×2"
    ),
    GANHAR_PODER(
        "Poder grátis",
        "Assista a um anúncio e receba um poder aleatório na hora.",
        "✦"
    ),
    REVIVER(
        "Continuar de pé",
        "Assista a um anúncio e volte ao duelo com metade da vida em vez de perder a sequência.",
        "♥"
    ),
    SALVAR_SEQUENCIA(
        "Salvar sequência",
        "Assista a um anúncio e recupere a sua sequência de dias perdida.",
        "◷"
    ),
    FRAGMENTOS(
        "Fragmentos extras",
        "Assista a um anúncio e leve Fragmentos direto para a loja.",
        "◈"
    )
}

/**
 * Camada de anúncios.
 *
 * Duas regras que valem mais que qualquer receita: **nunca** interromper um duelo em andamento,
 * e **nunca** deixar a falta de anúncio travar o jogo. Todo callback tem um caminho de fracasso
 * que devolve o controle na hora, e os premiados são sempre opcionais — o jogador que recusa
 * continua conseguindo tudo jogando.
 */
class AdsManager(private val appContext: Context) {

    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var lastInterstitialAt = 0L

    /** Fica falso enquanto o consentimento não permitir anúncios personalizados ou não. */
    var canRequestAds: Boolean = false
        private set

    /** Desliga tudo quando o jogador compra a remoção de anúncios. */
    var adsRemoved: Boolean = false

    fun initialize(context: Context, consentAllows: Boolean) {
        canRequestAds = consentAllows
        if (initialized || !consentAllows) return
        initialized = true
        runCatching {
            MobileAds.initialize(context) {
                preloadInterstitial()
                preloadRewarded()
            }
        }.onFailure { Log.w(TAG, "Falha ao iniciar o MobileAds", it) }
    }

    // ------------------------------------------------------------------ intersticial

    fun preloadInterstitial() {
        if (adsRemoved || !canRequestAds || loadingInterstitial || interstitial != null) return
        loadingInterstitial = true
        runCatching {
            InterstitialAd.load(
                appContext,
                BuildConfig.AD_INTERSTITIAL,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        loadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitial = null
                        loadingInterstitial = false
                        Log.d(TAG, "Intersticial não carregou: ${error.message}")
                    }
                }
            )
        }.onFailure { loadingInterstitial = false }
    }

    /**
     * Decide se cabe um intersticial agora. A cadência é conservadora de propósito: no mínimo
     * dois duelos e dois minutos entre anúncios.
     */
    fun shouldShowInterstitial(duelsSinceLast: Int): Boolean {
        if (adsRemoved || !canRequestAds) return false
        if (duelsSinceLast < MIN_DUELS_BETWEEN) return false
        if (System.currentTimeMillis() - lastInterstitialAt < MIN_MILLIS_BETWEEN) return false
        return interstitial != null
    }

    /** Mostra o intersticial. [onDone] é sempre chamado, com ou sem anúncio. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (adsRemoved || ad == null) {
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastInterstitialAt = System.currentTimeMillis()
                preloadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preloadInterstitial()
                onDone()
            }
        }
        runCatching { ad.show(activity) }.onFailure {
            interstitial = null
            onDone()
        }
    }

    // ------------------------------------------------------------------ premiado

    fun preloadRewarded() {
        if (!canRequestAds || loadingRewarded || rewarded != null) return
        loadingRewarded = true
        runCatching {
            RewardedAd.load(
                appContext,
                BuildConfig.AD_REWARDED,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewarded = ad
                        loadingRewarded = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewarded = null
                        loadingRewarded = false
                        Log.d(TAG, "Premiado não carregou: ${error.message}")
                    }
                }
            )
        }.onFailure { loadingRewarded = false }
    }

    val rewardedReady: Boolean get() = rewarded != null

    /**
     * Mostra o anúncio premiado. [onResult] recebe `true` somente quando o jogador assistiu até
     * o fim e a recompensa deve ser creditada.
     */
    fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preloadRewarded()
            onResult(false)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                preloadRewarded()
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                preloadRewarded()
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

    private companion object {
        const val TAG = "AdsManager"
        const val MIN_DUELS_BETWEEN = 2
        const val MIN_MILLIS_BETWEEN = 120_000L
    }
}
