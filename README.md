# KARDIA PULSE

> Um jogo de cartas em que ninguém vence por ter o número mais alto.
> Vence quem deixa o outro **sem jogada**.

Jogo de cartas para Android, escrito 100% em Kotlin com Jetpack Compose.

---

## A mecânica (é nova mesmo)

Existe um único **Núcleo** compartilhado, um número que começa em 0 e anda entre `-limite` e `+limite`.

Cada carta tem uma intensidade de **1 a 9** e um **elemento**. E aqui vem a primeira diferença:
**a carta não tem sinal**. Quem joga escolhe a polaridade na hora — a mesma carta pode empurrar o
Núcleo para cima (▲) ou para baixo (▼).

A regra que muda tudo:

> **A primeira carta da rodada TRAVA a direção.**
> A partir daí todo mundo é obrigado a empurrar o Núcleo para o mesmo lado — cada vez mais perto
> do limite.

E existe uma única saída: a **Ressonância**. Jogar uma carta do mesmo elemento da última jogada
(ou um Éter, que ressoa com tudo) inverte a direção.

Ninguém pode jogar uma carta que estoure o limite. Então quem chega no turno **sem nenhuma jogada
legal sobrecarrega** e perde vida — proporcional a quão longe o Núcleo foi.

O resultado: o jogo não é sobre força, é sobre **controlar quem fica sem saída**. Você quer empurrar
o Núcleo para a beirada guardando uma ressonância na mão.

### O Colapso

Para nenhuma rodada se arrastar para sempre, o campo aperta sozinho: depois de 4 cartas, o limite do
Núcleo encolhe 1 ponto a cada carta jogada, até um piso de ±2. As faixas vermelhas nas pontas do
medidor mostram o espaço já perdido. Quanto mais longa a rodada, mais afiada ela fica.

### Corrente

Ressonâncias seguidas formam uma **Corrente**. A cada 5 elos, quem fechou o elo ganha uma carga de
poder de graça.

---

## Modos de jogo

| Modo | O que muda |
|---|---|
| **Duelo** | O confronto clássico, um contra um. |
| **Blitz** | 12 segundos por turno. Se o tempo acaba, o Núcleo joga por você. |
| **Sobrevivência** | Rivais em sequência, cada um mais afiado. A vida **não** é restaurada entre duelos. |
| **Caos** | Dois modificadores aleatórios por duelo: Gravidade, Névoa, Espelho, Frenesi, Tempestade. |
| **Desafio Diário** | Semente fixa do dia: o mundo inteiro recebe exatamente as mesmas cartas. |

---

## O que já está implementado

- **Motor de regras** puro em Kotlin, imutável e determinístico (33 testes de unidade).
- **IA com PIMC** (Perfect Information Monte Carlo): como a mão do adversário é oculta, a IA sorteia
  vários mundos possíveis a partir das cartas que ainda não viu, resolve cada um com busca alfa-beta,
  e joga o que se sai melhor na média. Quatro dificuldades. **Ela nunca olha a sua mão.**
- **Cinco temperamentos de rival** (Equilibrado, Agressivo, Avarento, Paciente, Imprevisível). A
  dificuldade controla o quanto a IA *enxerga*; a persona controla o que ela *quer*, mudando os
  pesos da avaliação. O nome do temperamento aparece na tela: dá para ler o rival antes de jogar.
- **Recuo**: desfaz a sua última ação e tudo que o rival respondeu depois. Como o estado é imutável,
  desfazer é literalmente voltar a apontar para o estado anterior. Consumível, comprável na Loja.
- **Replay por código**: todo duelo termina com uma string curta contendo semente e jogadas. Cole em
  Extras › Replay e o motor recalcula a partida inteira, carta por carta. Nenhum vídeo, nenhum
  download — um duelo completo cabe em pouco mais de cem caracteres.
- **Tutorial de 8 etapas com trava real**: cada etapa só libera a próxima depois que a ação foi
  executada dentro de um duelo de verdade — nada de slides.
- **Passe Diário** com ciclo de 7 dias, sequência (streak) e recuperação opcional por anúncio.
- **Gamificação**: XP, 6 patentes, 12 conquistas com barra de progresso, Fragmentos, Cristais,
  5 poderes consumíveis, cosméticos.
- **Áudio 100% sintetizado em tempo real** — 12 efeitos e 2 trilhas em loop, calculados por
  matemática no primeiro uso e gravados em cache. **O projeto não contém um único arquivo de áudio.**
- **Vibração** com padrão próprio para cada evento (carta, inversão, corrente, sobrecarga…).
- **Animação**: a carta voa da mão até o Núcleo, a tela treme e pisca na sobrecarga, partículas
  explodem na inversão e nas correntes longas, e o dano sobe em número grande.
- **Notificações provocadoras**, no máximo uma por dia, e só quando você não apareceu.
- **Segurança em três camadas**:
  1. o save é assinado com HMAC-SHA256 por uma chave gerada dentro do Android Keystore — editar o
     arquivo por fora quebra a assinatura;
  2. **validação de plausibilidade** em toda gravação: assinar um valor adulterado geraria uma
     assinatura válida, então o jogo também compara o que está sendo gravado com o que havia antes
     e rejeita variações impossíveis (o clássico "999.999.999 Fragmentos");
  3. **conferência da assinatura do APK** em runtime, para detectar build recompilada.
- **Política de Privacidade e Termos de Uso** completos, com portão de aceite na primeira execução.
- **Monetização**: intersticial (só ao SAIR do duelo, com limite de frequência) e premiados
  opcionais, com fluxo de consentimento UMP.

---

## Como rodar no seu notebook

### 1. Requisitos

- **Android Studio Ladybug (2024.2)** ou mais novo
- **JDK 17** (o Android Studio já vem com um)
- Android SDK **34**

### 2. Abrir

```
File › Open… › selecione a pasta do projeto
```

Espere o Gradle sincronizar. Ele vai baixar as dependências na primeira vez (precisa de internet).

### 3. Rodar os testes do motor (não precisa de emulador nem celular)

```bash
./gradlew test
```

Roda em poucos segundos e valida as regras, o determinismo, os modificadores e a força da IA.

### 4. Instalar no celular ou no emulador

```bash
./gradlew installDebug
```

Ou o botão ▶ do Android Studio.

> **Recomendo testar em um celular de verdade**, não no emulador: a vibração e o áudio sintetizado
> são metade da sensação do jogo, e o emulador não reproduz nenhum dos dois direito.

### 5. Gerar o APK de release

```bash
./gradlew assembleRelease
```

O APK sai sem assinatura em `app/build/outputs/apk/release/`. Para publicar, crie uma keystore em
*Build › Generate Signed App Bundle / APK* no Android Studio.

---

## Antes de publicar na Play Store

Estas são as pendências reais. O jogo roda perfeitamente sem mexer em nada disso — mas **não
publique** antes de resolver:

1. **IDs do AdMob.** O projeto usa os IDs **de teste oficiais do Google**. Pode clicar à vontade
   durante o desenvolvimento que não gera receita nem infração. Troque em dois lugares:
   - `app/src/main/AndroidManifest.xml` → `com.google.android.gms.ads.APPLICATION_ID`
   - `app/build.gradle.kts` → os `buildConfigField` do bloco `release`

   > Clicar nos seus próprios anúncios reais é motivo de banimento da conta AdMob. Mantenha os IDs
   > de teste no `debug`.

2. **Dados nos documentos legais.** Em `ui/legal/LegalContent.kt`, troque os campos entre colchetes:
   `CONTACT_EMAIL`, `COMPANY` e o foro. A Play Store também exige a Política de Privacidade
   hospedada em uma **URL pública** — use o mesmo texto.

3. **Assinatura.** Crie a keystore e guarde em lugar seguro. Perder a keystore significa não
   conseguir mais atualizar o app.

   Depois de gerar o primeiro release assinado, ligue também a conferência de assinatura: rode o
   app, procure no Logcat a linha da tag `AppSignature` ("assinatura atual: ..."), e cole o valor
   em `EXPECTED_SIGNATURE` no bloco `release` do `app/build.gradle.kts`. Enquanto o campo estiver
   vazio a checagem fica desligada — que é o certo durante o desenvolvimento.

4. **`applicationId`.** Está como `com.kardiapulse.game`. Se quiser outro, mude em
   `app/build.gradle.kts` (e lembre que ele é definitivo depois da primeira publicação).

5. **Compras reais.** O "remover anúncios" hoje custa Cristais ganhos jogando. Se quiser vender por
   dinheiro, é preciso integrar a **Google Play Billing Library** — não incluí porque exige conta de
   desenvolvedor configurada e produto cadastrado no console.

---

## Sobre o nome (SEO)

**Kardia Pulse** foi escolhido por três motivos:

- **Duplo sentido**: *kardía* é "coração" em grego, e *card* está literalmente dentro da palavra.
  O jogo é sobre um Núcleo que pulsa e para quando alguém aperta demais.
- **Termo praticamente livre**: "kardia pulse" como expressão exata tem concorrência baixíssima na
  busca, então o app tende a ocupar o primeiro resultado rápido — o oposto de tentar rankear com
  "jogo de cartas".
- **Funciona em português e em inglês** sem tradução, sem acento e sem ambiguidade de grafia, o que
  facilita quem digita o nome de cabeça na busca da loja.

Sugestão de título na Play Store (o campo aceita 30 caracteres):
`Kardia Pulse: Duelo de Cartas`

---

## Estrutura do projeto

```
app/src/main/java/com/kardiapulse/game/
├── core/
│   ├── model/          Card, Element, GameState, GameMode, Power… (Kotlin puro)
│   └── engine/         GameEngine (regras), AiPlayer (PIMC), AiPersona, ReplayCode, PulseRandom
├── data/               Perfil, economia, conquistas, Passe Diário, cosméticos, tutorial
├── audio/              Síntese de som e trilha (WavSynth, Sfx, MusicTrack, AudioController)
├── haptics/            Padrões de vibração
├── ads/                AdMob + consentimento UMP
├── notifications/      Provocações + agendamento via WorkManager
├── security/           Assinatura do save, plausibilidade das gravações, leitura do ambiente
└── ui/                 Compose: tema, telas, componentes do tabuleiro
```

O pacote `core` **não importa nada de Android** — é por isso que dá para testá-lo inteiro na JVM,
sem emulador.

---

## Ideias para as próximas versões

Coisas que combinam com a mecânica e que ficaram de fora desta primeira entrega:

- **Multiplayer local por Bluetooth ou "passa e joga"** no mesmo aparelho — a mecânica é perfeita
  para isso porque cada turno é uma decisão só.
- **Modo Puzzle**: mão fixa, "vença em exatamente 3 jogadas". Rende centenas de níveis feitos à mão.
- **Baralhos com identidade**: um baralho com mais Éter (mais inversões), outro com mais cartas
  altas (mais pressão). Muda o estilo sem quebrar o equilíbrio.
- **Torneio semanal** com semente da semana e ranking local.
- **Conquistas secretas** para jogadas específicas (vencer com o Núcleo exatamente em 0, por exemplo).

---

## Licença

Projeto privado. Todos os direitos reservados.
