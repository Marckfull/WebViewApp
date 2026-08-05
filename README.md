# 🍓 Format Frute

Jogo mobile de fusão de frutas com a mecânica do 2048 — mas em vez de números,
a cereja vira morango, o morango vira uva… até a melancia.

Feito para Android Studio, em **Kotlin + Jetpack Compose**.

---

## Como abrir

1. Android Studio (Koala ou mais novo) › **Open** › selecione esta pasta.
2. Aguarde o Gradle sincronizar (baixa AGP 8.5, Kotlin 2.0 e Compose).
3. Rode em um aparelho ou emulador com **Android 7.0 (API 24)** ou superior.

Nada mais precisa ser configurado: o jogo já vem com os anúncios de teste do
Google funcionando, arte, som e música inclusos.

```
./gradlew assembleDebug      # gera o APK de debug
./gradlew test               # roda os testes do motor do jogo
```

---

## O que tem dentro

### Mecânica
Arraste, as frutas deslizam, iguais se fundem e sobem um degrau na escada:

`Cereja → Morango → Uva → Limão → Banana → Maçã → Laranja → Pera → Kiwi → Abacaxi → Pitaya → Melancia`

Duas melancias juntas viram **Colheita**: as duas somem, o tabuleiro respira e
você leva 20.000 pontos.

Três peças especiais mudam o jogo:

| Peça | Comportamento |
|---|---|
| ❄️ **Congelada** | Não desliza nem funde. Uma fusão vizinha racha o gelo. |
| 🤢 **Podre** | Desliza, nunca funde, entope o tabuleiro. Some com uma fusão ao lado. |
| 🌈 **Arco-íris** | Coringa: combina com qualquer fruta e sobe um nível. |

### Seis modos, seis regras diferentes

| Modo | O que muda |
|---|---|
| 🍎 **Pomar Clássico** | 4×4, sem pressa. Meta: chegar na Pitaya. |
| 🍊 **Vitamina Turbo** | 60 segundos. Cada fusão devolve tempo, combo multiplica pontos. |
| 🍇 **Geleia Congelada** | 5×5 com frutas nascendo dentro do gelo. |
| 🍍 **Cesta do Dia** | Tabuleiro e objetivo sorteados pela data — iguais para todo mundo — com movimentos contados. |
| 🍐 **Zen do Pomar** | 5×5 sem relógio e sem derrota: lotou, o pomar colhe as menores sozinho. |
| 🍋 **Batalha do Suco** | Contra o Monstro Azedo: fusão vira dano, ele cospe frutas podres de volta. |

### Poderes
Martelinho, Adubo Mágico, Voltar no Tempo, Peneira, Relógio de Açúcar e Fruta
Arco-íris. Todos podem ser comprados com Sementes, **mas o caminho principal é o
vídeo premiado** — o jogo oferece o vídeo sempre que falta o poder.

### Progressão e loja
Sementes, níveis com patente (de "Aprendiz de Feirante" a "Lenda da Feira"),
presente diário de 7 dias, 3 missões novas por dia, álbum de frutas e 5 peles de
tabuleiro para comprar.

### Tutorial travado
6 passos. **O próximo só libera depois que o anterior for cumprido de verdade** —
se o passo pede arrastar para a direita, os outros lados nem respondem (o
tabuleiro chacoalha e explica).

### Notificações
Lembretes escritos para arrancar sorriso, agendados com WorkManager
(1, 3 e 7 dias sem jogar + um recado diário no fim da tarde):

> 🍌 A banana ficou marrom de tanto te esperar.
> 🍉 A Melancia disse que você nunca chega nela.
> 🧃 O Monstro Azedo declarou vitória — disse que você fugiu.

---

## Anúncios (AdMob)

Já vem rodando com os **IDs oficiais de teste do Google**, então dá para jogar e
ver os anúncios de verdade sem risco de banimento. Para publicar, troque em dois
lugares:

1. `app/src/main/AndroidManifest.xml` → `com.google.android.gms.ads.APPLICATION_ID`
2. `app/src/main/java/com/formatfrute/game/ads/AdsManager.kt` → `PROD_INTERSTITIAL` e `PROD_REWARDED`

**Como o jogo usa cada formato:**

- **Intersticial**: só entre partidas, a partir da 2ª partida e com no mínimo
  95 s de intervalo. Nunca no meio de uma jogada.
- **Premiado (vídeo)**: sempre por escolha do jogador — liberar poder, continuar
  depois do fim, dobrar as sementes da partida, ganhar tempo, sementes grátis na
  loja. Nenhum vídeo é obrigatório para avançar.

O consentimento (UMP/GDPR) é pedido antes de qualquer anúncio, e a opção de rever
a escolha fica em **Ajustes › Opções de privacidade**.

---

## Arte, som e música

- **Frutas**: as 12 ilustrações enviadas, com o fundo xadrez recortado por
  script (`alpha` de verdade, sem franja branca) e normalizadas em 256×256.
- **Ícone**: gerado a partir das próprias frutas — adaptativo (108 dp) + legado
  em todas as densidades.
- **Áudio**: 11 efeitos e 3 trilhas (menu, partida e chefe) **sintetizados por
  código**, sem sample de terceiros e sem pendência de licença. O som da fusão é
  reafinado em tempo real conforme a fruta sobe de nível.

---

## Estrutura

```
app/src/main/java/com/formatfrute/game/
├── core/         Motor puro do jogo (sem Android): Engine, Tile, Fruit, GameMode, Power
├── data/         Persistência, missões, presente diário, peles, patentes
├── game/         GameViewModel + passos do tutorial
├── audio/        SoundManager (SoundPool + MediaPlayer) e Haptics
├── ads/          AdMob (intersticial, premiado) e consentimento UMP
├── notify/       Notificações engraçadas + agendamento com WorkManager
└── ui/           Compose: splash, home, tabuleiro, loja, ajustes, documentos
```

O motor (`core/Engine.kt`) é 100% Kotlin puro, sem dependência de Android — por
isso dá para testá-lo na JVM:

```
./gradlew test        # 20 testes cobrindo fusão, gelo, podre, coringa,
                      # colheita, fim de jogo, poderes e 200 partidas aleatórias
```

---

## Termos e privacidade

Estão dentro do app (**Ajustes › Documentos**), escritos em português e
descrevendo exatamente o que o jogo faz: progresso salvo só no aparelho,
anúncios pelo AdMob, notificações locais, nenhum cadastro.

Contato: **formatfrute@gmail.com**

---

## Antes de publicar

- [ ] Trocar os IDs do AdMob (manifest + `AdsManager.kt`)
- [ ] Definir `applicationId` final e assinar o release (`signingConfigs`)
- [ ] Hospedar a Política de Privacidade em uma URL pública (o Google Play exige
      link externo, além do texto que já está no app)
- [ ] Preencher o questionário de classificação e, se for o caso, inscrever no
      programa **Apps para Famílias**
