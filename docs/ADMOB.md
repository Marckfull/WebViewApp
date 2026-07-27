# Ligando os anúncios recompensados (AdMob)

O jogo já vem com todo o **loop de recompensa pronto e funcionando** — botões,
prêmios, contadores, overlay. O que roda por padrão é o `SimulatedRewardedAdHost`,
um anúncio de mentirinha de 5 segundos. Isso permite testar o jogo inteiro sem
ter conta de anúncio.

Trocar pelo AdMob de verdade mexe em **quatro arquivos** e não encosta em
nenhuma tela, porque tudo passa pela interface `RewardedAdHost`.

---

## Passo 1 — Criar a conta e as unidades

1. Crie uma conta em https://admob.google.com
2. Cadastre o app (Android, pacote `com.chuvadeletras.game`)
3. Anote o **ID do app**: `ca-app-pub-XXXXXXXX~YYYYYYYY`
4. Crie **uma unidade de anúncio recompensado** (dá para usar a mesma para todos
   os pontos do jogo) e anote o **ID da unidade**: `ca-app-pub-XXXXXXXX/ZZZZZZZZ`

> Enquanto estiver testando, use os IDs oficiais de teste do Google. Clicar em
> anúncio real do próprio app é motivo de banimento da conta.
>
> - App de teste: `ca-app-pub-3940256099942544~3347511713`
> - Recompensado de teste: `ca-app-pub-3940256099942544/5224354917`

## Passo 2 — Dependência

Em `app/build.gradle.kts`, descomente:

```kotlin
implementation("com.google.android.gms:play-services-ads:23.6.0")
```

## Passo 3 — Manifesto

Em `app/src/main/AndroidManifest.xml`, descomente o bloco `meta-data` e troque
o valor pelo seu ID de app:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXX~YYYYYYYY" />
```

## Passo 4 — A implementação

Crie `app/src/main/java/com/chuvadeletras/game/ads/AdMobRewardedAdHost.kt`:

```kotlin
package com.chuvadeletras.game.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AdMobRewardedAdHost(
    context: Context,
    private val adUnitId: String = "ca-app-pub-3940256099942544/5224354917"
) : RewardedAdHost {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<AdState>(AdState.Idle)
    override val state: StateFlow<AdState> = _state.asStateFlow()

    private var loaded: RewardedAd? = null

    init {
        MobileAds.initialize(appContext) { preload() }
    }

    override fun isReady(): Boolean = loaded != null

    /** Mantém sempre um anúncio na manga, para o botão abrir instantâneo. */
    private fun preload() {
        if (loaded != null) return
        RewardedAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { loaded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = null }
            }
        )
    }

    override suspend fun show(activity: Activity?, placement: AdPlacement): AdResult {
        if (activity == null) return AdResult.Failed("Sem Activity para exibir o anúncio")

        _state.value = AdState.Loading(placement)
        val ad = loaded ?: run {
            preload()
            // Dá um tempinho para o carregamento terminar.
            repeat(20) {
                kotlinx.coroutines.delay(250)
                loaded?.let { return@run it }
            }
            null
        }

        if (ad == null) {
            _state.value = AdState.Idle
            return AdResult.Failed("Nenhum anúncio disponível agora")
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var earned = false
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        loaded = null
                        preload()
                        _state.value = AdState.Idle
                        if (continuation.isActive) {
                            continuation.resume(
                                if (earned) AdResult.Rewarded else AdResult.Dismissed
                            )
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        error: com.google.android.gms.ads.AdError
                    ) {
                        loaded = null
                        _state.value = AdState.Idle
                        if (continuation.isActive) {
                            continuation.resume(AdResult.Failed(error.message))
                        }
                    }
                }
                ad.show(activity) { earned = true }
            }
        }
    }
}
```

## Passo 5 — Trocar o host

Em `app/src/main/java/com/chuvadeletras/game/ChuvaApp.kt`:

```kotlin
val adHost: RewardedAdHost = AdMobRewardedAdHost(context)
```

Pronto. Nenhuma tela precisa mudar.

---

## Onde os anúncios aparecem no jogo

Todos são **opcionais**: o jogador só assiste se tocar no botão. Isso é
essencial — anúncio forçado derruba a retenção e vai contra as políticas de
recompensa do AdMob.

| Ponto (`AdPlacement`)  | Onde fica                       | Prêmio                                   |
|------------------------|---------------------------------|------------------------------------------|
| `DOUBLE_DAILY`         | Baú diário, na tela inicial     | Recompensa do dia em dobro               |
| `EXTRA_MOVES`          | Barra de rodada, durante a partida | +3 jogadas na rodada atual            |
| `REVIVE`               | Diálogo de derrota              | Continua a partida com tudo reparado     |
| `REPAIR_CELL`          | Ao tocar num quadradinho trancado | Destrava aquele quadradinho            |
| `FREE_COINS`           | Tela inicial e loja             | +100 gotas (a cada 4 horas)              |
| `DOUBLE_MATCH_REWARD`  | Diálogo de vitória              | Gotas da partida em dobro                |

Para adicionar um ponto novo: crie o valor em `AdPlacement`, trate-o em
`GameViewModel.applyAdReward` (ou `HomeViewModel`) e chame `watchAd(...)` no botão.

---

## Antes de publicar

- [ ] Trocar os IDs de teste pelos IDs reais
- [ ] Preencher os campos `[...]` em `ui/legal/LegalTexts.kt`
- [ ] Publicar a Política de Privacidade também em uma URL pública (a Play Store exige)
- [ ] Preencher o formulário de **Segurança dos dados** na Play Console declarando
      a coleta feita pela rede de anúncios
- [ ] Se marcar o app como voltado ao público infantil, configurar
      `setTagForChildDirectedTreatment` no `RequestConfiguration` do AdMob
- [ ] Adicionar um formulário de consentimento (UMP/GDPR) se for distribuir na Europa
