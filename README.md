# 🧠 NeuroFlip — Memória Recodificada

Jogo da memória para Android, escrito **100% em Kotlin com Jetpack Compose**, com
animações, trilha sonora original, vibração, campanha de 30 níveis, economia de
moedas e monetização por **vídeos premiados (rewarded ads)**.

Todas as dependências são **gratuitas e de código aberto** (AndroidX / Jetpack /
Kotlin) mais os SDKs gratuitos do Google para anúncios e consentimento. Não há
nenhum asset pago no repositório: **o áudio é sintetizado em tempo de execução
pelo próprio app** e os ícones são vetores desenhados à mão.

---

## 🎮 A mecânica que não existe nos outros jogos da memória

Jogo da memória comum: vire duas cartas, se forem iguais elas saem. Fim.
O NeuroFlip acrescenta quatro camadas que mudam a forma de jogar:

### ◎ ECO (pulso sináptico)
Quando você acerta um par, ele **emite um pulso** que revela por 0,9 s as cartas
vizinhas no tabuleiro. Isso transforma o jogo: a **posição** do par que você
resolve passa a importar, porque cada acerto entrega informação sobre a
vizinhança. Usar uma carta revelada pelo pulso dá **bônus de ECO** nos pontos.

### ⇄ MUTAÇÃO
A cada N jogadas, **duas cartas escondidas trocam de lugar** — com animação de
deslizamento em arco, dá para rastrear o movimento se você estiver atento. É
memória + jogo de acompanhar o copo. A dificuldade cresce reduzindo o intervalo
entre mutações (de 8 jogadas no nível 5 até 3 no nível 30).

### ⚡ SINAPSE / SOBRECARGA
Acertos consecutivos carregam a barra de Sinapse. Cheia, você pode disparar a
**SOBRECARGA**: revela o tabuleiro inteiro por 1,5 s e dobra os pontos por 10 s.
Errar descarrega a barra — o jogo premia ritmo, não tentativa e erro.

### 🃏 Cartas especiais
| Carta | Efeito |
|---|---|
| ❄ **Crio** | Congela o cronômetro por 6 s e devolve 8 s |
| ★ **Dourada** | Vale 3× pontos |
| ◈ **Espelho** | Ao resolver, marca permanentemente um par ainda escondido |
| ⌭ **Fantasma** | Se você errar com ela, ela foge de lugar sozinha |
| ☢ **Instável** | Se você a esquecer por 3 jogadas, ela sacode o tabuleiro |

As mecânicas entram **uma por vez** ao longo da campanha, com um balão de
tutorial no nível em que aparecem.

---

## 🕹 Modos de jogo

- **Campanha Sinapse** — 30 níveis, tabuleiros de 3×4 até 6×7, tempo e mecânicas
  progressivas, 3 estrelas por nível (baseadas no número de jogadas).
- **Blitz 90** — 90 segundos, mutação agressiva, foco em pontuação e recorde.
- **Zen** — sem cronômetro e sem mutação, para treinar memória em paz.

## 💰 Gamificação e receita com anúncios

O jogo é gratuito e se sustenta com o AdMob. A regra de ouro do projeto:
**vídeo premiado nunca é obrigatório e sempre entrega algo de valor real.**

| Onde | O que o jogador ganha |
|---|---|
| Fim de nível | **Dobrar a recompensa** de neurônios |
| Derrota por tempo | **+30 s e continuar** de onde parou (1× por partida) |
| Bônus diário | **Dobrar o bônus** do dia (sequência de dias aumenta o valor) |
| Loja | +60 neurônios, +1 Scan, +1 Curinga |
| Dentro da partida | Power-up esgotado? troque por um vídeo na hora |

Moeda: **neurônios** 🧠 — ganhos jogando (20 + 15/estrela + 3/combo), gastos em
power-ups (Scan 🔍 40, Curinga ⧉ 70, Tempo ⏱ 50) e em **5 temas visuais**
desbloqueáveis (Neon Córtex, Erupção Solar, Bio Lab, Sakura, Retro CRT).

Anúncio intersticial: no máximo **1 a cada 3 níveis** e nunca antes de 2 minutos
do anterior — limite implementado em `AdsManager`, nunca no meio de uma partida.

## 🎨 Apresentação

- **Splash animada em Compose**: duas cartas cruzam e viram em 3D, o nome surge
  letra a letra e uma barra de "sincronização neural" completa a entrada
  (`SplashScreen.kt`), depois da splash de sistema (`core-splashscreen`).
- Fundo vivo com névoa neon em movimento e malha sináptica (`AnimatedNeuroBackground`).
- Cartas com **virada 3D real** (`rotationY` + `cameraDistance`), brilho por tipo
  de carta, anel pulsante no ECO e partículas na resolução do par.
- HUD com barra de tempo que muda de cor na reta final, multiplicador de combo e
  botão de Sobrecarga.

## 🔊 Áudio 100% original (sem arquivos de áudio no repositório)

`audio/Synth.kt` é um pequeno sintetizador em Kotlin puro (osciladores, envelopes,
filtro passa-baixa, gravador WAV). Na primeira execução o app **compõe e grava no
cache**:

- 12 efeitos sonoros (virar, acerto, erro, combo, eco, mutação, sobrecarga,
  vitória, derrota, moeda, toque, tique) tocados via `SoundPool`;
- 3 trilhas em loop (`MENU` calma em Am7–Fmaj7–Cmaj7–G, `GAME` a 120 bpm com
  baixo e bateria, `TENSE` para os últimos 15 segundos) via `MediaPlayer`.

Vantagem prática: nenhum risco de direito autoral, APK pequeno e nada para
licenciar. Se você preferir usar músicas próprias, é só colocá-las em
`res/raw` e trocar a fonte em `MusicEngine`.

## 📳 Vibração

`haptics/Haptics.kt` tem padrões distintos para acerto, erro, combo, sobrecarga,
vitória e derrota, com `VibrationEffect.createWaveform` quando o aparelho tem
controle de amplitude e degradação suave quando não tem. Pode ser desligada nas
Configurações.

## ⚖️ Termos de Uso e Política de Privacidade

- Telas nativas no app (`LegalScreen`), com **aceite obrigatório na primeira
  execução** (`ConsentGateScreen`) salvo no DataStore.
- Link permanente em **Configurações** e no rodapé da tela inicial.
- Consentimento de anúncios via **Google UMP** (LGPD/GDPR), com botão "Opções de
  privacidade" para o usuário rever a escolha quando quiser.
- Textos completos em `LegalTexts.kt` e espelhados em
  [`docs/TERMOS_DE_USO.md`](docs/TERMOS_DE_USO.md) e
  [`docs/POLITICA_DE_PRIVACIDADE.md`](docs/POLITICA_DE_PRIVACIDADE.md).

> ⚠️ **Antes de publicar** troque os campos entre `[colchetes]` em
> `LegalTexts.kt` (nome/razão social, e-mail e data de vigência) e hospede a
> política numa URL pública — a Google Play exige esse link na ficha do app.

---

## 🚀 Como abrir e rodar

1. Abra a pasta do projeto no **Android Studio** (Ladybug ou mais novo).
2. Aceite o download do SDK 35 e do JDK 17 quando o IDE pedir.
3. `Run ▶` num aparelho ou emulador com **Android 7.0 (API 24)** ou superior.

Ou pela linha de comando:

```bash
./gradlew assembleDebug          # gera o APK de debug
./gradlew test                   # roda os testes do motor de jogo
./gradlew installDebug           # instala no aparelho conectado
```

### Trocar os IDs do AdMob (obrigatório para faturar)

O projeto vem com os **IDs de teste oficiais do Google** — eles funcionam em
qualquer aparelho e nunca geram receita. Para publicar:

1. Crie o app na sua conta [AdMob](https://admob.google.com) e gere 2 blocos:
   vídeo premiado e intersticial.
2. Troque em `app/src/main/java/com/neuroflip/game/ads/AdsManager.kt` → `AdUnits`.
3. Troque o `com.google.android.gms.ads.APPLICATION_ID` no
   `app/src/main/AndroidManifest.xml`.
4. Adicione seu aparelho como dispositivo de teste enquanto desenvolve, para não
   clicar nos seus próprios anúncios reais (motivo comum de banimento).

Checklist completo de publicação: [`docs/PUBLICACAO.md`](docs/PUBLICACAO.md).

---

## 🗂 Estrutura

```
app/src/main/java/com/neuroflip/game/
├── MainActivity.kt            Activity única + splash de sistema
├── NeuroFlipApp.kt            Application e service locator
├── ads/                       AdsManager (rewarded/intersticial) + ConsentManager (UMP)
├── audio/                     Synth, SoundEngine (SFX), MusicEngine (trilhas)
├── data/                      PlayerRepository (DataStore), PlayerState, PowerUp
├── domain/                    Model, GameEngine (regras puras), LevelCatalog
├── haptics/                   Padrões de vibração
└── ui/
    ├── AppViewModel.kt        Perfil, economia, anúncios, bônus diário
    ├── GameViewModel.kt       Loop de jogo, eventos, áudio/háptica, recompensas
    ├── NeuroFlipRoot.kt       Navegação (Navigation Compose)
    ├── components/            Botões neon, carta 3D, barras, partículas, fundo
    ├── screens/               Splash, Aceite, Home, Mapa, Jogo, Loja, Config, Legal
    └── theme/                 5 paletas + tipografia
app/src/test/                  Testes unitários do GameEngine (17 casos)
```

O `GameEngine` **não importa nada de Android** — o tempo entra por `tick(delta)`,
então toda a regra do jogo é testável em JVM pura.

## 💡 Próximas ideias (roadmap)

Lista completa com prioridade e esforço em [`docs/IDEIAS.md`](docs/IDEIAS.md).
Destaques:

1. **Duelo Sináptico** — 1×1 no mesmo aparelho, tabuleiro compartilhado.
2. **Desafio diário com semente fixa** — todo mundo joga o mesmo tabuleiro do dia.
3. **Modo Corrupção** — cartas que "infectam" as vizinhas se você demorar.
4. **Passe de temporada gratuito** movido a vídeos premiados.
5. **Widget de bônus diário** na tela inicial do Android.

## 📄 Licença

Código do jogo: escolha a sua (MIT recomendado). Os SDKs do Google seguem os
termos da própria Google. Nenhum asset de terceiros é distribuído aqui.
