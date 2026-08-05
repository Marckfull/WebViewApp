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

### Modo Receita — a campanha (60 fases + a do dia)

O freguês faz o pedido: "1 Pera, 3 Laranjas, 4 Bananas". Só conta a fruta que
você **criar fundindo** — o que já está no tabuleiro é matéria-prima. Jogadas
são contadas, e sobrar jogada no fim vale estrela (até 3 por fase).

As fases são geradas por regra, não desenhadas à mão, então a fase 27 é
idêntica para todo mundo, em qualquer aparelho, para sempre. O gerador tem uma
regra de ouro: **o tabuleiro sempre nasce com matéria-prima suficiente para
cada item do pedido, com no mínimo 4 casas livres e sem congelar peça
essencial**. Isso é garantido por teste — `RecipeTest` roda as 60 fases a cada
build.

A dificuldade sobe em três eixos: a fruta pedida (do Limão ao Abacaxi), a
profundidade (da fase 25 em diante o pedido principal nasce dois degraus
abaixo, exigindo montar as peças antes de juntá-las) e os obstáculos (gelo a
partir da fase 10, fruta podre caindo a partir da 20).

A **Receita do Dia** é uma fase sorteada pela data, igual para todo mundo no
mundo inteiro, com tabuleiro próprio e bônus de sementes. Ela substituiu o modo
"Cesta do Dia" original: eram o mesmo jogo (puzzle com jogadas contadas) em duas
telas diferentes, e dois lugares meia-boca valem menos que um lugar bom.

### Cinco modos avulsos, cinco regras diferentes

| Modo | O que muda |
|---|---|
| 🍎 **Pomar Clássico** | 4×4, sem pressa. Meta: chegar na Pitaya. |
| 🍊 **Vitamina Turbo** | 60 segundos. Cada fusão devolve tempo, combo multiplica pontos. |
| 🍇 **Geleia Congelada** | 5×5 com frutas nascendo dentro do gelo. |
| 🍐 **Zen do Pomar** | 5×5 sem relógio e sem derrota: lotou, o pomar colhe as menores sozinho. |
| 🍋 **Batalha do Suco** | Contra o Monstro Azedo: fusão vira dano, ele cospe frutas podres de volta. |

### Passe da Feira

30 degraus por temporada (uma por mês). Fichas caem de tudo que o jogador já
faz: partida terminada, fase da Receita fechada, missão do dia concluída.

Cada degrau tem duas faixas: a **grátis**, que abre só por jogar, e a de
**vídeo**, liberada degrau a degrau assistindo um anúncio premiado — nunca por
dinheiro. As três peles pagas do jogo (Sorvete, Meia-Noite e Tropical) são
recompensa dos degraus 10, 20 e 30 da faixa de vídeo.

### As frutas falam

A arte já tinha personalidade; agora tem voz. Cada fruta do Limão pra cima tem
falas próprias e estreia com um balãozinho na primeira vez que aparece na
partida — a Melancia chega gritando "CHEGUEI!", o Abacaxi avisa "Sou um
abacaxi. Resolve aí." O Monstro Azedo provoca quando ataca, e uma "fruta do
dia" (a mesma para todo mundo, sorteada pela data) recebe o jogador na tela
inicial.

As falas são deliberadamente econômicas: só estreia de fruta grande e combo
alto. Falar demais vira ruído e o jogador para de ler.

### Peles: loja e Passe não competem
A Barraquinha vende Pomar ao Amanhecer, Sorvete de Frutas e Praia Tropical. O
Passe entrega três **exclusivas de temporada** — Feira da Meia-Noite, Arraiá da
Fruta e Pomar Encantado — que nunca entram à venda. Se os dois oferecessem a
mesma coisa, o jogador ganharia de graça justamente o que ia comprar e a
vitrine perderia a função; um teste garante que nenhuma pele esteja nos dois
lugares.

### Poderes
Martelinho, Adubo Mágico, Voltar no Tempo, Peneira, Relógio de Açúcar, Fruta
Arco-íris e **Olho Bom** (acende as duas frutas que dá para juntar agora — a
válvula de escape do tabuleiro travado, e o melhor lugar do jogo para um vídeo
premiado: é onde o jogador realmente quer). Todos podem ser comprados com
Sementes, **mas o caminho principal é o vídeo** — o jogo oferece o vídeo sempre
que falta o poder.

O Adubo **conta para o pedido** no Modo Receita: ele cria uma fruta de verdade,
é o que o jogador espera, e é o que dá sentido a gastar poder na campanha.

### Progressão, conquistas e loja
Sementes, níveis com patente (de "Aprendiz de Feirante" a "Lenda da Feira"),
presente diário de 7 dias, 3 missões novas por dia, álbum de frutas, mapa de
fases com estrelas, **23 conquistas** e 5 peles de tabuleiro.

As conquistas são locais de propósito — sem login, sem conta, sem servidor —,
então funcionam offline desde o primeiro minuto. A que fica pronta durante a
partida vira comemoração na hora, com confete, antes do placar final.

### A partida sobrevive à interrupção
Ligação, notificação, bateria: em celular, sessão cortada é regra. O tabuleiro
é guardado quando o app vai para o fundo, e a tela inicial oferece
**"Continuar partida"** com o modo e o placar de onde você parou.

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

**Classificação do conteúdo:** o jogo é para todas as idades, então o AdMob é
configurado com `MAX_AD_CONTENT_RATING_G` — só entram anúncios de público geral,
sem bebida, apostas ou violência. Isso é feito em código
(`AdsManager.init`), e a Política de Privacidade dentro do app descreve
exatamente esse comportamento.

## Diagnóstico

Sem servidor não há telemetria remota, mas jogo publicado às cegas não se
conserta. Se o app travar, o `CrashReporter` guarda no aparelho um relatório
(modelo, versão do Android, versão do app, tela em que estava e a pilha do erro)
e, na abertura seguinte, pergunta se o jogador quer mandá-lo por e-mail. Nada
sai sem ele tocar em "Mandar".

Para trocar por Crashlytics depois, basta adicionar o plugin do Firebase e
chamar `recordException` dentro de `CrashReporter.install` — o resto do app não
muda.

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
├── core/         Motor puro (sem Android): Engine, Tile, Fruit, GameMode,
│                 Power, Recipe (as 60 fases) e FruitVoice (as falas)
├── data/         Persistência, missões, presente diário, peles, patentes,
│                 Passe da Feira
├── game/         GameViewModel + passos do tutorial
├── audio/        SoundManager (SoundPool + MediaPlayer) e Haptics
├── ads/          AdMob (intersticial, premiado) e consentimento UMP
├── notify/       Notificações engraçadas + agendamento com WorkManager
├── diag/         Relatório de travamento guardado no aparelho
└── ui/           Compose: splash, home, tabuleiro, mapa de fases, passe,
                  conquistas, loja, ajustes, documentos
```

Todo o `core/` é 100% Kotlin puro, sem dependência de Android — por isso dá
para testá-lo direto na JVM:

```
./gradlew test    # 51 testes, todos na JVM (sem emulador):
                  # EngineTest       fusão, gelo, podre, coringa, colheita, fim
                  #                  de jogo, dica, poderes, 200 partidas aleatórias
                  # RecipeTest       as 60 fases + a do dia: matéria-prima
                  #                  suficiente, espaço para manobrar, gelo em
                  #                  peça certa, folga de jogadas, determinismo
                  # SeasonPassTest   os 30 degraus e a conta de fichas
                  # AchievementsTest catálogo, progresso e pendências
                  # VaultTest        save embaralhado, adulteração e retrocompat.
                  # SavedGameTest    a partida volta exatamente como parou
```

Os testes rodam contra os ids de recurso de verdade, então um texto faltando
em `strings.xml` vira erro de compilação, não tela em branco no aparelho.
```

## Tradução

Nenhum texto de tela vive no Kotlin. Os 479 textos do jogo estão em
`res/values/strings.xml` (interface) e `res/values/strings_legal.xml`
(documentos), e as classes de dados guardam só o id do recurso
(`@StringRes val title: Int`) — quem resolve é quem tem `Context`.

Para lançar em outro idioma, copie os dois arquivos para
`res/values-en/` (ou `-es`, `-fr`…) e traduza só os valores. Nenhuma linha de
código muda.

## Nota sobre trapaça no save

O progresso é embaralhado com checksum (`Vault`) antes de ir para as
preferências, então o caminho "abri o XML e troquei 250 por 999999" não
funciona mais. Não é criptografia e não pretende ser: **sem servidor, quem tem
o aparelho na mão sempre vence**. Como o jogo não tem ranking, o único
prejudicado por burlar é o próprio jogador — a proteção existe só para não
deixar a porta escancarada. Se um dia entrar placar online, aí sim a validação
precisa mudar de lugar (servidor), não de força.

---

## Termos e privacidade

Estão dentro do app (**Ajustes › Documentos**), escritos em português e
descrevendo exatamente o que o jogo faz: progresso salvo só no aparelho,
anúncios pelo AdMob, notificações locais, nenhum cadastro.

Contato: **formatfrute@gmail.com**

---

## Animação

O que faz o jogo parecer vivo, e onde está cada coisa:

- **Squash & stretch** (`BoardView.TileView`): a fruta em movimento estica no
  eixo do deslize e achata no outro. É o truque que separa deslize gostoso de
  deslize duro, e sai só da distância que ainda falta percorrer.
- **Clarão na fusão**: a fruta sobrevivente pisca branco antes do pulo de
  escala. É o clarão que vende o impacto, não o pulo.
- **Baque de tela** (`GameScreen`): da Pera para cima, e em toda colheita, a
  tela inteira sacode e dá um zoom de 1%.
- **Placar rolando** (`RollingNumber`): o número corre até o valor novo em vez
  de pular.
- **Transição entre telas**: deslize horizontal + fade, com o sentido invertido
  na volta.
- **Fundo vivo, com freio**: as frutas fantasmas continuam boiando, mas são 5
  em vez de 9, menores — e caem para 2 em aparelho marcado como `isLowRamDevice`.
  Cada uma é um bitmap redesenhado todo frame; em celular de entrada isso custa.

## Antes de publicar

- [ ] Trocar os IDs do AdMob (manifest + `AdsManager.kt`)
- [ ] Definir `applicationId` final e assinar o release (`signingConfigs`)
- [ ] Hospedar a Política de Privacidade em uma URL pública (o Google Play exige
      link externo, além do texto que já está no app)
- [ ] Preencher o questionário de classificação indicativa como **Livre / para
      todas as idades** (é como o AdMob já está configurado)
