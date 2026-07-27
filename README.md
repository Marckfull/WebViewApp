# NEON SOMBRA

> O lado escuro dos blocos.

Jogo de Tetris para Android, escrito em **Kotlin** com **Jetpack Compose**.
Arte neon, trilha chiptune sintetizada em tempo real e um diferencial: tudo o
que voce empilha no fundo do tabuleiro ganha um **espelho sombrio** na parte de
cima, roubando a sua visibilidade.

---

## O diferencial: o espelho sombrio

A cada bloco que trava no fundo, uma copia quase preta dele aparece refletida na
parte de cima do tabuleiro (a linha 19 espelha na linha 0, a 18 na 1, e assim
por diante).

- A sombra **nao colide** com nada: ela nao empurra, nao trava e nao muda a
  fisica do jogo.
- A sombra **atrapalha a leitura** do tabuleiro justamente na area onde as pecas
  novas caem.
- Quanto maior o nivel, mais densa ela fica (de 50% a 92% de opacidade).

Ou seja: deixar a pilha crescer nao so aproxima o fim de jogo, como tambem cega
o jogador. Jogar limpo e a unica forma de continuar enxergando.

### A sombra ataca

Do **nivel 5** em diante o lado sombrio revida: de tempos em tempos uma celula
da sombra **solidifica** e vira bloco de verdade, marcado com um X roxo. Esse
bloco colide, ocupa espaco e conta para completar linha como qualquer outro.

Duas travas mantem o jogo justo: o ataque nunca acontece nas duas primeiras
linhas das colunas onde as pecas nascem, e no maximo 8 blocos sombrios existem
ao mesmo tempo. O intervalo entre ataques encurta com o nivel (de 10s a 4s).

### A Purga

Limpar **quatro linhas de uma vez** apaga a sombra inteira por **15 segundos**:
sem espelho e sem ataque. A moldura do tabuleiro fica verde e pulsa, e nos dois
segundos finais a sombra volta aparecendo aos poucos, avisando que o intervalo
acabou.

### Combo

Limpezas em jogadas seguidas acumulam combo: cada elo rende **+50 x nivel** e
faz o aparelho **vibrar no compasso da trilha** -- os pulsos duram uma
semicolcheia do mesmo BPM da musica, e a sequencia cresce com o combo.

## Controles

Tudo acontece com um dedo, direto no tabuleiro:

| Gesto | Efeito |
|---|---|
| Arrastar para os lados | Move a peca, coluna a coluna |
| Segurar o dedo na tela | A peca cai depressa (e rende 1 ponto por linha) |
| Tocar rapidinho | Gira a peca (3 posicoes novas antes de voltar a original) |
| Deslizar rapido para baixo | A peca despenca e trava na hora (2 pontos por linha) |

## Telas

1. **Splash** — logo acendendo, as sete pecas desfilando e barra de carregamento.
2. **Termos de uso e privacidade** — exigido no primeiro acesso, depois fica
   disponivel para leitura no menu.
3. **Tutorial** — cinco paginas ilustradas e animadas, mostradas na primeira
   abertura (e sempre que o jogador quiser rever em "Como jogar").
4. **Menu** — apelido do jogador, recorde e os caminhos do jogo.
5. **Jogo** — tabuleiro a esquerda; a direita o nome do jogador, a pontuacao, o
   recorde, nivel/linhas e as **tres proximas pecas**; o nome do jogo aceso no
   alto de tudo.
6. **Configuracoes** — musica, efeitos sonoros, vibracao e peca fantasma podem
   ser ligados e desligados; da tambem para apagar o recorde.

Na tela de jogo, uma faixa entre o logo e o tabuleiro avisa o que esta em jogo
agora: a Purga correndo com a contagem, o combo em andamento ou o alerta de que
a sombra comecou a atacar.

## Pontuacao

| Linhas de uma vez | Pontos |
|---|---|
| 1 | 100 x nivel |
| 2 | 300 x nivel |
| 3 | 500 x nivel |
| 4 (o "tetris") | 800 x nivel |

Queda acelerada rende 1 ponto por linha e a queda instantanea rende 2. Cada elo
de combo soma +50 x nivel. A cada 10 linhas o nivel sobe: as pecas caem mais
rapido, a sombra fica mais densa e (do nivel 5 em diante) ataca com mais
frequencia.

## Som sem nenhum arquivo de audio

A trilha e os efeitos sao **sintetizados em PCM em tempo de execucao** por
`SoundEngine`: ondas quadradas, triangulares e ruido montados nota a nota e
tocados por `AudioTrack`. O repositorio nao carrega nenhum `.mp3` ou `.ogg`, e a
trilha (baixo + arpejo + chimbal sobre Am - F - C - G) e um loop gerado na
primeira vez que a musica e ligada.

## Como rodar

Precisa do Android Studio (Ladybug ou mais novo) com o SDK 35 instalado.

```bash
git clone <este-repositorio>
cd WebViewApp
./gradlew assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # instala no aparelho conectado
./gradlew test                 # roda os testes do motor do jogo
```

Ou simplesmente abra a pasta no Android Studio e clique em Run.

- `minSdk` 24 (Android 7.0) - `targetSdk`/`compileSdk` 35
- AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01

## Organizacao do codigo

```
app/src/main/java/com/neonsombra/game/
├─ MainActivity.kt              tela cheia, tema e injecao das dependencias
├─ NeonSombraApplication.kt     preferencias e som vivos durante todo o processo
├─ audio/SoundEngine.kt         sintese da trilha, dos efeitos e da vibracao
├─ data/Prefs.kt                apelido, recorde e interruptores (SharedPreferences)
├─ game/
│  ├─ Tetromino.kt              as sete pecas e suas quatro rotacoes
│  ├─ TetrisEngine.kt           TODA a regra do jogo, sem Android nem Compose
│  └─ GameViewModel.kt          relogio do jogo, gestos e disparo de som
└─ ui/
   ├─ NeonSombraApp.kt          navegacao entre as telas
   ├─ components/               logo, botoes, paineis e o cenario animado
   ├─ game/BoardCanvas.kt       desenho do tabuleiro e das miniaturas
   ├─ screens/                  splash, termos, tutorial, menu, jogo, ajustes
   └─ theme/                    paleta neon e tipografia
```

`TetrisEngine` e Kotlin puro de proposito: da para testar a regra inteira na JVM,
sem emulador. Os testes ficam em
`app/src/test/java/com/neonsombra/game/game/TetrisEngineTest.kt`: sao 33 testes
cobrindo rotacao, queda, queda acelerada, queda instantanea, limpeza de linha,
pontuacao, combo, o espelho sombrio, o ataque da sombra (incluindo as travas de
area de spawn e teto de blocos), a Purga, fim de jogo, pausa e reinicio.

## Privacidade

O jogo nao pede permissao de internet, nao coleta dados e nao tem anuncios. A
unica permissao usada e `VIBRATE`, e ela pode ser desligada nas configuracoes.
Apelido, recorde e preferencias ficam so no aparelho.
