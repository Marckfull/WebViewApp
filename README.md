# PRISMA — Fusão Cromática

Jogo de puzzle para Android em **Kotlin + Jetpack Compose**. É um match-3 na
superfície, mas a mecânica central — a **Fusão de Essências** — muda o que você
pensa a cada jogada.

---

## A mecânica que muda o jogo

Nos jogos do gênero, um match de 4 vira uma peça que explode. Aqui ele vira um
**recurso que você carrega pelo tabuleiro**.

| Ação | O que acontece |
|---|---|
| Match de 3 | Estoura normalmente |
| Match de 4, L ou T | Condensa uma **ESSÊNCIA** — ela fica parada no tabuleiro |
| Match de 3 dentro de uma cascata | Também condensa uma essência (premia reação em cadeia) |
| Match de 5 em linha | Condensa duas essências vizinhas, que se fundem na hora |
| **Trocar uma essência** | Ela pode ir para **qualquer casa vizinha, sem precisar formar match** — e **não explode** ao ser movida |
| **Duas essências encostadas** | Fundem sozinhas: mesma cor → **SUPERNOVA**, cores diferentes → **PRISMA** |
| Trocar Prisma / Supernova | Detonam. Prisma limpa linha + coluna + diagonais; Supernova limpa a cor inteira + área 5x5 |
| **Dois Prismas/Supernovas encostados** | Fundem na **NOVA CROMÁTICA**, que varre o tabuleiro inteiro |

A regra é uma só, em todos os níveis: **peças especiais que se encostam se
fundem e sobem um degrau; peças especiais trocadas com uma gema comum detonam.**

O resultado é uma pergunta que nenhum outro jogo do gênero faz: a boa jogada
deixa de ser *"onde eu limpo agora"* e passa a ser *"onde eu deposito essa
essência para fundir daqui a duas jogadas"*.

E o topo da escada é genuinamente difícil por um motivo estrutural: um Prisma
**detona ao ser trocado**, então não dá para caminhá-lo até outro. A única forma
de encostar dois é fundir duas essências exatamente na casa ao lado de um que já
estava lá — uma jogada de três etapas. Medindo com bots, a Nova aparece em cerca
de **11% das partidas**.

Como as essências não formam match por cor, elas ficam no caminho — e essa
tensão entre "ela atrapalha" e "ela vale muito" é o coração do jogo.

---

## O que está pronto

- **Motor de jogo** em Kotlin puro, determinístico por semente, sem dependência
  de Android — e por isso coberto por testes que rodam sem emulador.
- **Campanha de 150 fases** em 6 mundos, com 6 tipos de objetivo, 10 formatos de
  tabuleiro, fases-chefe a cada 10 e dificuldade **calibrada por medição**
  (ver [docs/BALANCEAMENTO.md](docs/BALANCEAMENTO.md)).
- **Tutorial interativo obrigatório** na primeira execução: quatro passos em
  tabuleiros desenhados à mão, e o passo **só avança quando o jogador faz a
  jogada** — errar não pune, apenas reforça o destaque.
- **Splash animada** com a metáfora do jogo (um feixe branco entra no prisma e
  sai decomposto nas seis cores).
- **Termos de Uso e Política de Privacidade** dentro do app, com aceite
  obrigatório versionado e opção de apagar todos os dados.
- **Anúncios AdMob** com consentimento (UMP), vídeos premiados sempre opcionais
  e intersticiais só em pausa natural — ver [docs/ANUNCIOS.md](docs/ANUNCIOS.md).
- **Gamificação**: estrelas, XP e nível do jogador, moedas, vidas com
  regeneração, missões diárias, prêmio diário em ciclo de 7 dias e loja.
- **Áudio 100% sintetizado em tempo de execução** — trilha em loop e 16 efeitos,
  sem um único arquivo binário no repositório.
- **Efeitos visuais**: partículas, feixes de detonação, tremor de tela, números
  de pontuação flutuantes e peças desenhadas vetorialmente no Canvas.

---

## Como compilar

Requisitos: **Android Studio Ladybug (ou mais novo)**, **JDK 17**,
**Android SDK 35**.

```bash
git clone <este-repositorio>
cd WebViewApp
./gradlew assembleDebug        # gera o APK de depuração
./gradlew testDebugUnitTest    # roda os testes do motor
```

No Android Studio: *Open* na pasta do projeto e rodar a configuração `app`.

---

## Estrutura

```
app/src/main/java/com/prisma/fusao/
  core/            Motor do jogo — Kotlin puro, sem Android, testável
    Model.kt         Gema, casa, tabuleiro
    GameEngine.kt    Match, fusão, detonação, gravidade, cascata
    Campaign.kt      As 150 fases
    TutorialScript.kt  Roteiro do tutorial (separado da UI para ser testável)
  data/            Persistência (DataStore), economia, missões, vidas
  audio/           Sintetizador de trilha e efeitos
  ads/             AdMob + consentimento UMP
  ui/
    game/            Renderizador do tabuleiro, animação, ViewModel
    screens/         Splash, aceite, legal, mapa, partida, loja, ajustes, tutorial
    components/      Botões, diálogos, fundo estrelado
app/src/test/      Testes do motor e do tutorial (JUnit, sem emulador)
```

---

## Testes

O motor e o roteiro do tutorial são Kotlin puro justamente para poderem ser
verificados sem emulador:

```bash
./gradlew testDebugUnitTest
```

Cobrem, entre outras coisas: as regras de match e fusão, prisma e supernova,
gravidade sem buracos, entrega de prismoides, derrota e jogadas extras, e a
verificação de que **as 150 fases abrem jogáveis** e de que **nenhuma pede mais
gelo ou pedra do que o tabuleiro tem** — um erro que existia e foi pego assim.

O tutorial tem teste próprio: cada passo é jogado do início ao fim, conferindo
que o tabuleiro não começa resolvido e que a jogada indicada produz exatamente a
lição prometida.

---

## Antes de publicar

Leia [docs/PUBLICACAO.md](docs/PUBLICACAO.md). O resumo do que **precisa** ser
trocado:

1. IDs do AdMob (hoje são os IDs oficiais de **teste** do Google), em
   `app/build.gradle.kts` e `app/src/main/res/values/strings.xml`.
2. Os campos entre colchetes nos textos legais
   (`ui/screens/LegalScreens.kt`): nome do responsável e e-mail de contato.
3. Hospedar a Política de Privacidade numa URL pública — a Play Store exige o
   link na ficha do app.
4. Assinatura de release e `applicationId` definitivo.

---

## Documentação

- [docs/MECANICA.md](docs/MECANICA.md) — regras completas e decisões de design
- [docs/BALANCEAMENTO.md](docs/BALANCEAMENTO.md) — como a dificuldade foi medida
- [docs/ANUNCIOS.md](docs/ANUNCIOS.md) — conformidade com as políticas do AdMob
- [docs/PUBLICACAO.md](docs/PUBLICACAO.md) — checklist de lançamento
