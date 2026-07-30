package com.prisma.fusao.ads

import android.app.Activity
import android.content.Context
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
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.prisma.fusao.BuildConfig
import com.prisma.fusao.data.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prêmios que um vídeo recompensado pode dar. O texto fica junto do prêmio porque a
 * política do AdMob exige que o jogador saiba exatamente o que vai ganhar **antes**
 * de decidir assistir.
 */
enum class RewardedOffer(
    val title: String,
    val description: String,
    val buttonLabel: String,
) {
    EXTRA_MOVES(
        title = "Mais 5 jogadas",
        description = "Assista a um vídeo curto e continue de onde parou, sem perder o progresso da fase.",
        buttonLabel = "Assistir e ganhar 5 jogadas",
    ),
    EXTRA_LIFE(
        title = "Uma vida extra",
        description = "Assista a um vídeo curto para receber uma vida e voltar a jogar agora.",
        buttonLabel = "Assistir e ganhar 1 vida",
    ),
    DOUBLE_COINS(
        title = "Dobrar as moedas",
        description = "Assista a um vídeo curto para dobrar as moedas ganhas nesta fase.",
        buttonLabel = "Assistir e dobrar",
    ),
    FREE_BOOSTER(
        title = "Item grátis",
        description = "Assista a um vídeo curto e leve um item de graça para a próxima fase.",
        buttonLabel = "Assistir e ganhar o item",
    ),
    DAILY_BONUS(
        title = "Dobrar o prêmio diário",
        description = "Assista a um vídeo curto para dobrar as moedas do prêmio de hoje.",
        buttonLabel = "Assistir e dobrar",
    ),
}

/** Por que um vídeo recompensado não está disponível agora. */
enum class RewardUnavailableReason { NOT_LOADED, DAILY_LIMIT, NO_CONSENT }

/**
 * Camada de anúncios do jogo.
 *
 * Decisões tomadas para ficar dentro das políticas do Google AdMob e da Play:
 *
 * - **Consentimento primeiro.** Nada é pedido à rede de anúncios antes do fluxo UMP
 *   terminar. O jogador pode reabrir as opções de privacidade pelos Ajustes.
 * - **Recompensado é sempre opt-in.** Nunca abrimos um vídeo sozinhos: há um diálogo
 *   descrevendo o prêmio, e o jogador escolhe. Recusar nunca bloqueia o jogo — todo
 *   prêmio de vídeo é conveniência, não obrigação.
 * - **Intersticial só em pausa natural.** Nunca durante a partida, nunca antes da
 *   fase [PlayerState.INTERSTITIAL_FIRST_LEVEL], com intervalo mínimo entre
 *   exibições, e nunca logo depois de um recompensado.
 * - **Sem clique acidental.** Nenhum anúncio é aberto por toque no tabuleiro; os
 *   diálogos têm botão de recusa do mesmo tamanho do de aceitar.
 * - **IDs de teste por padrão.** Ver `app/build.gradle.kts`.
 */
class AdsManager(private val context: Context) {

    private val _consentResolved = MutableStateFlow(false)
    val consentResolved: StateFlow<Boolean> = _consentResolved.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var consentInformation: ConsentInformation? = null
    private var mobileAdsInitialized = false
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false
    private var showingFullScreenAd = false

    /** Última vez que um recompensado terminou — evita emendar um intersticial nele. */
    private var lastRewardedFinishedAt = 0L

    // -------------------------------------------------------- consentimento

    /**
     * Roda o fluxo do User Messaging Platform. Deve ser chamado a partir de uma
     * Activity, uma vez por sessão, antes de qualquer requisição de anúncio.
     */
    fun gatherConsent(activity: Activity, onFinished: () -> Unit = {}) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            // Em depuração dá para forçar a região do formulário sem publicar nada.
            paramsBuilder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build(),
            )
        }

        val info = UserMessagingPlatform.getConsentInformation(context)
        consentInformation = info
        info.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "formulário de consentimento: ${formError.message}")
                    }
                    finishConsent(info, onFinished)
                }
            },
            { requestError ->
                // Sem consentimento resolvido o jogo segue normalmente, só sem anúncios.
                Log.w(TAG, "consentimento indisponível: ${requestError.message}")
                finishConsent(info, onFinished)
            },
        )
    }

    private fun finishConsent(info: ConsentInformation, onFinished: () -> Unit) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _consentResolved.value = true
        if (info.canRequestAds()) initializeMobileAds()
        onFinished()
    }

    fun canRequestAds(): Boolean = consentInformation?.canRequestAds() == true

    /** Reabre o formulário de privacidade a partir dos Ajustes. */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "opções de privacidade: ${error.message}")
            consentInformation?.let {
                _privacyOptionsRequired.value =
                    it.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            }
            onDone()
        }
    }

    // ------------------------------------------------------------ inicialização

    private fun initializeMobileAds() {
        if (mobileAdsInitialized) return
        mobileAdsInitialized = true
        if (BuildConfig.ADS_TEST_MODE) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build(),
            )
        }
        MobileAds.initialize(context) {
            preloadRewarded()
            preloadInterstitial()
        }
    }

    // --------------------------------------------------------- recompensado

    fun preloadRewarded() {
        if (!canRequestAds() || loadingRewarded || rewardedAd != null) return
        loadingRewarded = true
        RewardedAd.load(
            context,
            BuildConfig.AD_UNIT_REWARDED,
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
                    Log.w(TAG, "recompensado não carregou: ${error.message}")
                }
            },
        )
    }

    /**
     * Exibe o vídeo recompensado. Só deve ser chamado depois de o jogador confirmar
     * num diálogo que descreve o prêmio.
     *
     * @param onReward chamado apenas quando o Google confirma que o vídeo foi visto.
     * @param onUnavailable chamado quando não há anúncio; a UI deve seguir sem punir
     *        o jogador.
     */
    fun showRewarded(
        activity: Activity,
        watchedToday: Int,
        onReward: () -> Unit,
        onUnavailable: (RewardUnavailableReason) -> Unit = {},
        onDismissed: () -> Unit = {},
    ) {
        if (!canRequestAds()) {
            onUnavailable(RewardUnavailableReason.NO_CONSENT)
            return
        }
        // Um teto diário evita transformar o jogo numa máquina de assistir anúncio.
        if (watchedToday >= PlayerState.MAX_REWARDED_PER_DAY) {
            onUnavailable(RewardUnavailableReason.DAILY_LIMIT)
            return
        }
        val ad = rewardedAd
        if (ad == null || showingFullScreenAd) {
            preloadRewarded()
            onUnavailable(RewardUnavailableReason.NOT_LOADED)
            return
        }

        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedReady.value = false
                showingFullScreenAd = false
                lastRewardedFinishedAt = System.currentTimeMillis()
                preloadRewarded()
                if (earned) onReward()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _rewardedReady.value = false
                showingFullScreenAd = false
                preloadRewarded()
                onUnavailable(RewardUnavailableReason.NOT_LOADED)
            }
        }
        showingFullScreenAd = true
        ad.show(activity) { earned = true }
    }

    // ---------------------------------------------------------- intersticial

    fun preloadInterstitial() {
        if (!canRequestAds() || loadingInterstitial || interstitialAd != null) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_UNIT_INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    loadingInterstitial = false
                    Log.w(TAG, "intersticial não carregou: ${error.message}")
                }
            },
        )
    }

    /**
     * Decide sozinho se pode mostrar um intersticial agora, aplicando todas as
     * travas de política. Devolve true quando exibiu.
     *
     * Deve ser chamado só em transição de tela (sair da tela de fim de fase),
     * nunca com o tabuleiro na frente do jogador.
     */
    fun maybeShowInterstitial(
        activity: Activity,
        levelIndex: Int,
        state: PlayerState,
        onFinished: () -> Unit = {},
    ): Boolean {
        val now = System.currentTimeMillis()
        val blocked = state.removeAdsPurchased ||
            !canRequestAds() ||
            showingFullScreenAd ||
            levelIndex < PlayerState.INTERSTITIAL_FIRST_LEVEL ||
            now - state.lastInterstitialAt < PlayerState.INTERSTITIAL_MIN_INTERVAL_MS ||
            now - lastRewardedFinishedAt < AFTER_REWARDED_COOLDOWN_MS

        val ad = interstitialAd
        if (blocked || ad == null) {
            preloadInterstitial()
            onFinished()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                showingFullScreenAd = false
                preloadInterstitial()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                showingFullScreenAd = false
                preloadInterstitial()
                onFinished()
            }
        }
        showingFullScreenAd = true
        ad.show(activity)
        return true
    }

    companion object {
        private const val TAG = "AdsManager"

        /** Nunca emendar um intersticial logo depois de um vídeo recompensado. */
        private const val AFTER_REWARDED_COOLDOWN_MS = 60_000L
    }
}
