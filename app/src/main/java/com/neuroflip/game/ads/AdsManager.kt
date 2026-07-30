package com.neuroflip.game.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * IDs de bloco de anúncio.
 *
 * ATENÇÃO: são os IDs de TESTE oficiais do Google AdMob. Eles funcionam em
 * qualquer aparelho e nunca geram receita. Antes de publicar, substitua pelos
 * IDs da sua conta AdMob (e o APPLICATION_ID no AndroidManifest.xml).
 */
object AdUnits {
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
}

/**
 * Camada de anúncios: vídeo premiado (o coração da monetização) e intersticial
 * com limite de frequência.
 *
 * Regras autoimpostas para não estragar a experiência:
 *  - vídeo premiado é SEMPRE opcional e sempre entrega algo de valor;
 *  - intersticial no máximo 1 a cada 3 níveis e nunca antes de 2 minutos;
 *  - nada de anúncio no meio de uma partida.
 */
class AdsManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false

    private var lastInterstitialAtMs = 0L
    private var levelsSinceInterstitial = 0

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch(Dispatchers.IO) {
            runCatching {
                MobileAds.initialize(context) { }
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                        .setTagForChildDirectedTreatment(
                            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
                        )
                        .build()
                )
            }.onFailure { Log.w(TAG, "Falha ao inicializar AdMob: ${it.message}") }
            preload()
        }
    }

    fun preload() {
        loadRewarded()
        loadInterstitial()
    }

    // ------------------------------------------------------------------ premiado

    private fun loadRewarded() {
        if (rewardedAd != null || loadingRewarded) return
        loadingRewarded = true
        scope.launch {
            runCatching {
                RewardedAd.load(
                    context,
                    AdUnits.REWARDED,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            loadingRewarded = false
                            _rewardedReady.value = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedAd = null
                            loadingRewarded = false
                            _rewardedReady.value = false
                            Log.w(TAG, "Vídeo premiado indisponível: ${error.message}")
                        }
                    }
                )
            }.onFailure { loadingRewarded = false }
        }
    }

    /**
     * Mostra o vídeo premiado.
     * @param onReward chamado apenas quando o jogador realmente completa o vídeo.
     * @param onUnavailable chamado quando não há anúncio (ex.: sem internet) —
     *        neste caso o jogo entrega uma recompensa de consolo menor.
     */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedReady.value = false
                loadRewarded()
                if (!earned) onUnavailable()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _rewardedReady.value = false
                loadRewarded()
                onUnavailable()
            }
        }
        ad.show(activity) {
            earned = true
            onReward()
        }
    }

    // ------------------------------------------------------------------ intersticial

    private fun loadInterstitial() {
        if (interstitialAd != null || loadingInterstitial) return
        loadingInterstitial = true
        scope.launch {
            runCatching {
                InterstitialAd.load(
                    context,
                    AdUnits.INTERSTITIAL,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            loadingInterstitial = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitialAd = null
                            loadingInterstitial = false
                        }
                    }
                )
            }.onFailure { loadingInterstitial = false }
        }
    }

    fun onLevelFinished() {
        levelsSinceInterstitial++
    }

    /** Respeita o limite de frequência; se não puder mostrar, segue direto. */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit) {
        val elapsed = SystemClock.elapsedRealtime() - lastInterstitialAtMs
        val allowed = levelsSinceInterstitial >= 3 &&
            (lastInterstitialAtMs == 0L || elapsed > MIN_INTERSTITIAL_GAP_MS)
        val ad = interstitialAd
        if (!allowed || ad == null) {
            loadInterstitial()
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastInterstitialAtMs = SystemClock.elapsedRealtime()
                levelsSinceInterstitial = 0
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial()
                onDone()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val TAG = "NeuroFlipAds"
        private const val MIN_INTERSTITIAL_GAP_MS = 120_000L
    }
}
