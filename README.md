# 🌧️ Chuva de Letras

> **As letras caem. Se você não usar, elas somem.**

Uma cruzadinha em Kotlin + Jetpack Compose com uma reviravolta que não existe nas
palavras cruzadas comuns: você **não escolhe as letras**. Elas chovem numa bandeja,
você tem poucas jogadas por rodada, e **toda jogada que sobrar tranca um
quadradinho da grade para sempre**.

Não é um jogo de vocabulário. É um jogo de **decidir rápido onde gastar o que a
chuva te deu**.

---

## A mecânica em 30 segundos

1. A bandeja despeja um punhado de letras — sempre mais letras do que jogadas.
2. Você tem **4 jogadas** por rodada. Escolhe um quadradinho, toca numa letra, encaixa.
3. Ao virar a rodada, **tudo que sobrou na bandeja evapora**.
4. Se você virou a rodada **com jogada sobrando**, uma letra útil desperdiçada
   **tranca 🔒 um quadradinho** que precisava dela. Aquele espaço está perdido — a
   menos que você gaste um Reparo.
5. Terminar a grade **sem nenhum quadradinho trancado** = ⭐⭐⭐.

Sobrar letra é normal e não custa nada. Sobrar **jogada** é que dói. Essa única
regra é o jogo inteiro: ela transforma cada rodada numa decisão real em vez de um
preenchimento passivo.

## Os 5 modos

| Modo | | O que muda |
|---|---|---|
| **Chuva Calma** | 🌦️ | Sem relógio, rodadas contadas. O modo de entrada. |
| **Tempestade** | ⛈️ | 28 s por rodada. O relógio zera e a bandeja vira na marra. |
| **Dilúvio** | 🌊 | Grades em sequência, cada uma maior. 3 vidas, cada trava custa uma. |
| **Desafio Diário** | 📅 | Uma grade por dia, **idêntica para todo mundo** (seed = data). |
| **Sereno** | 🍃 | Nada evapora, nada tranca. Para relaxar ou treinar. |

Tempestade destrava no nível 3, Dilúvio no nível 5.

## Gamificação

- **Ofensiva diária** com bônus em 3, 7, 14, 30 e 60 dias
- **Baú diário** em ciclo de 7 dias (o do dia 7 é o grande)
- **3 missões novas por dia**, sorteadas pela data
- **12 conquistas** com recompensa em gotas
- **Níveis e patentes** — de "Gota Novata" a "Lenda da Chuva"
- **Loja** com 5 itens: Congelar ❄️, Trocar 🔄, Revelar 💡, Reparo 🔧, Tempo ⏱️
- **6 pontos de anúncio recompensado**, todos opcionais (ver `docs/ADMOB.md`)

## Como rodar

```bash
# Abra a pasta no Android Studio (Ladybug ou mais novo) e clique em Run.
# Ou, pela linha de comando:
./gradlew assembleDebug
./gradlew test        # 21 testes de regra e de geração de grade
```

Requisitos: JDK 17, Android SDK 35, minSdk 26 (Android 8.0).

> Se o Gradle reclamar do wrapper, rode `gradle wrapper --gradle-version 8.11.1`
> ou deixe o Android Studio regenerar na primeira sincronização — o `.jar` do
> wrapper veio de um projeto antigo.

## Arquitetura

```
domain/          Kotlin puro, zero Android — dá para testar tudo na JVM
  model/         Puzzle, GameState, GameConfig, PowerUp…
  generator/     PuzzleGenerator: monta a cruzadinha por encaixe, com seed
  words/         WordBank: ~180 palavras em PT-BR, 7 temas
  engine/        GameRules: todas as regras como funções puras sobre GameState
data/            DataStore (perfil em JSON), gamificação, repositório de grades
ads/             RewardedAdHost + implementação simulada (AdMob é plug-in)
ui/              Compose: splash, home, modos, partida, missões, loja, legal
```

`GameRules` são **funções puras**: `(GameState, ação) -> GameState`. Todo o
balanceamento é testável sem emulador — foi assim que a curva de dificuldade foi
calibrada.

### Sobre o gerador de grades

As grades não são fixas: `PuzzleGenerator` monta cada uma por encaixe a partir do
banco de palavras. A validação roda **antes** de cada encaixe, então toda grade
gerada é válida por construção — sem letra conflitante, sem palavra colada em
outra, sem sequência de letras que não tenha dica.

A mesma `seed` sempre produz a mesma grade. É isso que faz o Desafio Diário ser
igual para todos os jogadores no mesmo dia.

Há um teto de **11 colunas × 15 linhas** para o quadradinho não ficar pequeno
demais para o dedo em tela de celular.

## Estado da verificação

- ✅ `domain/` compila e **21 testes unitários passam** (regras + gerador)
- ✅ 700 grades geradas e revalidadas do zero: **0 inválidas**
- ✅ Balanceamento medido por simulação: bot que joga perfeito vence 100% com 3
  estrelas; bot que desperdiça 20% das jogadas vence ~43%; a 35% não vence mais
- ⚠️ A camada Compose (`ui/`) **não foi compilada** — o ambiente onde o projeto foi
  escrito não tinha acesso ao Android SDK nem ao repositório Maven do Google.
  Abra no Android Studio e sincronize para o primeiro build.

## Antes de publicar

1. Preencher os campos `[...]` em `ui/legal/LegalTexts.kt` (empresa, e-mail, foro)
2. Publicar a Política de Privacidade numa URL pública — a Play Store exige
3. Seguir `docs/ADMOB.md` para trocar o anúncio simulado pelo real
4. Trocar a chave de assinatura e conferir o `applicationId`
