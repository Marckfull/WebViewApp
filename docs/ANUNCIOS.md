# Anúncios e conformidade

O jogo é gratuito e se sustenta com AdMob. As decisões abaixo foram tomadas para
ficar dentro das políticas do Google (AdMob e Play) — e, tão importante quanto,
para o anúncio não estragar o jogo.

Código relevante: `app/src/main/java/com/prisma/fusao/ads/AdsManager.kt`.

---

## 1. Consentimento antes de qualquer requisição

O fluxo do **User Messaging Platform (UMP)** roda em `MainActivity.onCreate`,
antes de o SDK de anúncios ser inicializado. Só depois de `canRequestAds()`
retornar verdadeiro é que `MobileAds.initialize` é chamado e algum anúncio é
pedido.

O jogador pode rever a decisão a qualquer momento em **Ajustes → Opções de
privacidade**, que reabre o formulário do UMP. O botão só aparece quando o
Google informa que aquele usuário tem direito a ele
(`PrivacyOptionsRequirementStatus.REQUIRED`).

Se o consentimento falhar por qualquer motivo, **o jogo continua funcionando
normalmente** — apenas sem anúncios. Nada é bloqueado.

---

## 2. Vídeo premiado é sempre opt-in

Nenhum vídeo abre sozinho. Sempre existe um diálogo antes
(`RewardedOfferDialog`) que:

- diz **exatamente qual é o prêmio**, antes da decisão;
- avisa que o prêmio só sai se o vídeo chegar ao fim;
- tem o botão **"Agora não" do mesmo tamanho e mesmo peso visual** do botão de
  aceitar — nada de recusa escondida em cinza minúsculo.

E a regra de design que sustenta tudo: **todo prêmio de vídeo é conveniência,
nunca obrigação**. Não existe fase que exija assistir a um anúncio para avançar,
nem item exclusivo de quem assiste. Tudo que está na loja se compra com moedas
ganhas jogando.

Prêmios oferecidos: +5 jogadas ao perder, +1 vida, dobrar as moedas da fase,
item grátis, dobrar o prêmio diário.

### Teto diário

`PlayerState.MAX_REWARDED_PER_DAY = 12`. Não é exigência de política: é para o
jogo não virar uma máquina de assistir anúncio. Ao atingir o teto, a interface
explica com clareza e volta ao normal no dia seguinte.

---

## 3. Intersticial só em pausa natural

Um intersticial só aparece quando **todas** as condições valem:

| Condição | Valor |
|---|---|
| A partida acabou (vitória ou saída pela tela de fim de fase) | obrigatório |
| Fase mínima | a partir da fase 6 |
| Intervalo desde o último intersticial | 3 minutos |
| Tempo desde o último vídeo premiado | 60 segundos |
| Jogador comprou remoção de anúncios | nunca exibe |

Nunca durante uma partida, nunca por cima do tabuleiro, nunca emendado num vídeo
premiado que o jogador acabou de assistir.

O contador de "último intersticial" só é gravado **quando o anúncio realmente
apareceu** — marcar sem exibir adiaria os próximos sem motivo.

---

## 4. Nada de clique acidental

- Nenhum anúncio ocupa a área do tabuleiro.
- Os itens ficam numa faixa separada embaixo, com folga.
- Nenhum toque no tabuleiro pode abrir um anúncio.
- Não há banner sobreposto a controles.

---

## 5. IDs de teste por padrão

`app/build.gradle.kts` e `res/values/strings.xml` usam os **IDs oficiais de teste
do Google**:

```
App ID:        ca-app-pub-3940256099942544~3347511713
Recompensado:  ca-app-pub-3940256099942544/5224354917
Intersticial:  ca-app-pub-3940256099942544/1033173712
```

Isso é proposital. Clicar no próprio anúncio real durante o desenvolvimento gera
tráfego inválido e pode suspender a conta do AdMob. Troque pelos IDs reais
somente ao publicar, e mantenha `ADS_TEST_MODE = true` enquanto testar em
aparelhos seus.

---

## 6. Público e idade

O jogo **não** é dirigido a menores de 13 anos.
`setTagForUnderAgeOfConsent(false)` é passado ao UMP, e os Termos de Uso deixam a
faixa etária explícita.

Se você decidir mirar público infantil, precisará revisar tudo isto: ativar
`setTagForChildDirectedTreatment`, limitar a classificação do conteúdo dos
anúncios e atender ao programa "Voltado para a família" da Play. Não é uma
mudança de uma linha.

---

## 7. Dados

O jogo não coleta nada por conta própria: sem cadastro, sem servidor, sem
telemetria. O progresso fica no DataStore local. A única coleta é a do próprio
AdMob, descrita na Política de Privacidade dentro do app, com link para as
políticas do Google.

Ajustes → **"Apagar meus dados"** limpa tudo do aparelho na hora. Como não existe
cópia em servidor, é a forma mais direta de exercer o direito de eliminação
previsto na LGPD.
