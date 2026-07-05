# Ecos de Lirael — Protótipo Jogável (Fase 0+)

Projeto **Godot 4.3+** do action-RPG descrito em [`../docs/GDD-PLANO-DO-JOGO.md`](../docs/GDD-PLANO-DO-JOGO.md).

Greybox jogável que valida o núcleo do jogo antes da arte final: combate
souls-like, exploração entre cenas, NPCs com diálogo e a primeira luta de boss.

## O que já está jogável

**Combate (núcleo souls-like):**
- Movimentação analógica em 8 direções (Aria, o retângulo verde-água 🙂).
- Ataque com varredura de espada, custo de stamina e janela de acerto.
- Esquiva-rolamento com i-frames; stamina com regeneração após pausa curta.
- **Parry** [F / PARAR]: aparar um golpe na janela certa quebra a **postura**
  do inimigo e o deixa atordoado — golpes num atordoado são **críticos**
  (finalização, 2,5×). Inimigos pesados também têm a postura quebrada por
  golpes seguidos; bosses resistem (o parry só os interrompe e chip de dano).
- **Lock-on de alvo** (Z-targeting): trava a mira no inimigo mais próximo,
  pressione de novo para alternar entre alvos; movimento vira strafe.
- Inimigos "Ecoados" com telegraph e janela de punição; mini-boss
  "Brutamontes" com poise.
- **Loop de morte souls-like**: Ecos dropados no local da morte,
  recuperáveis; santuários curam e repõem os inimigos.

**Mundo (8 cenas conectadas por portais):**
- **Pedra-Alva** (vila hub): 3 NPCs com diálogo (Mestra Odara, Corvo e Sela),
  forja, santuário e portais para as outras áreas.
- **Cripta das Guardiãs**: corredor de Ecoados, santuário antes do boss e a
  luta contra o **Eco da Guardiã** — boss de 2 fases com barra própria no HUD:
  o portão fecha ao entrar, a fase 2 (≤50% de vida) fica mais rápida e ganha
  investida tripla, e o boss reseta se você morrer ou descansar. A vitória
  fica gravada (o boss não volta).
- **Floresta Sussurrante** (a oeste da vila): novo bioma com árvores,
  **Espreitadores** (rápidos, frágeis, telegraph curto) e o **Alfa do
  Bosque** — segundo boss (mesma IA de 2 fases, mais veloz), que guarda
  uma **Memória Perdida** de Lys numa clareira aberta.
- **Forja Afundada** (ao sul da vila): dungeon alagada com **Forjados**
  blindados (quase não atordoam), poças que forçam rotas (atravesse com
  o gancho) e o **Coração da Forja** — terceiro boss, lento e brutal.
  Recompensa: a **Bomba de Eco** [B / BOMBA], que fere em área e derruba
  **paredes rachadas** (há uma escondendo um nicho de minério na própria
  Forja).
- **Torre dos Ventos** (a noroeste da vila): **correntes de vento** que
  empurram a Aria enquanto ela luta, e um arquétipo novo — a **Sentinela
  do Vento**, inimigo **à distância** que dispara rajadas telegrafadas
  (não-aparáveis). No topo, o **Guardião dos Ventos** (4º boss) guarda a
  **Lente da Verdade**: com ela na bolsa, **segredos ocultos pelo Silêncio
  aparecem** — há uma Memória Perdida escondida à vista na própria vila.
- **Necrópole de Sal** (a sudoeste): **Carcaças de Sal** entre **correntes
  de sal** que varrem e ferem a Aria — a menos que ela calce as **Botas de
  Corrente** (4º item de dungeon), recompensa da **Rainha de Sal** (5º boss).
  As Botas também firmam a Aria contra o vento da Torre, e abrem uma corrente
  que escondia uma Memória.
- **Coração Mudo** (portão selado na vila, abre com os 4 Santuários do Eco
  restaurados): o confronto final com **Selene, a Guardiã Caída** — ela fala
  antes de lutar, e ao cair leva ao **dilema e aos finais** (silenciar o eco,
  completar a canção dela, ou — com as Memórias Perdidas reunidas — o **final
  verdadeiro**, a Canção do Mundo).
- **Campo de Treino**: a arena da Fase 0, para testar builds e números.

**Dificuldade e longevidade:**
- **3 modos**, escolhidos ao iniciar (tela "Como você quer ouvir esta canção?"):
  **Balada** (5 frascos, parry generoso, mantém os Ecos ao morrer), **Canção**
  (a experiência pretendida) e **Requiem** (souls puro: inimigos batem mais
  forte, 2 frascos, janela de parry apertada). Persistido no save.
- **New Game+**: ao zerar o jogo, o título libera o NG+ — você recomeça a
  história **mantendo** itens, atributos, forja e frascos, mas os inimigos
  ficam mais duros e mais valiosos a cada ciclo. O modo e o ciclo aparecem
  no menu de pause.

**Progressão e economia:**
- **Atributos no santuário** (level up souls-like): ao descansar, abre a
  tela de fortalecimento — gaste Ecos em **Vitalidade** (+10 PV),
  **Fôlego** (+8 vigor) ou **Força** (+2 dano). O custo sobe a cada
  nível comprado. Persistido no save.
- **Loja do Corvo** (banca ao lado dele na vila): frascos extras
  (até 5) e dois amuletos passivos — **Amuleto do Eco** (+20 PV) e
  **Amuleto do Vento** (+20 vigor).
- **Side quest de Sela** ("O canteiro da mamãe"): leve 5 Ervas-lunares
  e receba o **Talismã de Sela** (ataques/esquivas gastam 20% menos
  vigor) — com diálogos por etapa da quest.
- **Cutscene de abertura** ao iniciar Novo Jogo: o Silêncio chega a
  Pedra-Alva (slides com fade, puláveis).

**Sobrevivência e mundo vivo:**
- **Frascos de Essência** (estilo Estus): 3 cargas que curam 60 de vida
  [Q / FRASCO]; recarregam ao descansar no santuário. Contador no HUD.
- **Ciclo dia/noite** (~5 min por ciclo): entardecer alaranjado e noite
  azulada nas áreas externas; à noite os Ecoados enxergam 50% mais longe
  e rendem 50% mais Ecos. A cripta é sempre escura.
- **Canto do Sol** (3ª melodia da ocarina): alterna dia e noite, como o
  clássico.
- **Mapa da Cartógrafa** [V / MAPA]: o mapa da área **se desenha por onde
  Aria passa** (mecânica temática do GDD); marca santuário, passagens e
  a posição atual. Progresso do mapa salvo no save.

**Game feel e opções:**
- **Hit-stop** (micro-congelamento no impacto), **screen shake** e
  **faíscas de partículas** em todos os golpes — o combate "morde".
- **Menu de pause** [Esc / PAUSA]: continuar, volumes de música e sons
  (persistidos em `user://settings.json`) e voltar ao título.

**Interface e persistência:**
- Tela de título com **Continuar / Novo Jogo**.
- **Save automático em JSON** (escrita atômica em `user://`): salva ao
  descansar, ao vencer boss, ao coletar Ecos e quando o app fecha ou vai
  para segundo plano (essencial em Android). Ao continuar, a jogadora
  acorda no último santuário onde descansou — regra souls.
- HUD: vida, stamina, Ecos, mensagens de evento e barra de boss.
- Caixa de diálogo que pausa o jogo (avança com interact/ataque).
- Controles de toque: joystick virtual + botões ATACAR/ROLAR/ALVO/USAR
  (aparecem só em dispositivos com touchscreen).

**Itens e exploração:**
- **Inventário (Bolsa)** com ícones, quantidades e descrições — abre
  com [I / BOLSA] e pausa o jogo. Persistido no save.
- **Recursos coletáveis** no mundo: Erva-lunar (vila) e Minério de Eco
  (treino/cripta); renascem ao descansar no santuário.
- **Gancho-corda** — primeiro item de dungeon, dropado pelo Eco da
  Guardiã: use [H / ITEM] perto de um poste com argola para se lançar
  até ele (reposicionamento rápido em combate e travessia).
- **Ocarina de Vidro** — encontrada num nicho da Cripta. [M / OCARINA]
  abre a roda de melodias: **Melodia do Retorno** (teleporta ao
  santuário da área) e **Acalento** (acalma Ecoados próximos por
  alguns segundos — eles "lembram do que eram" e param de atacar).
- **Forja da Odara** — a bigorna ao lado da ferreira: gaste 3 Minérios
  de Eco por nível para reforjar a lâmina (+4 de dano, até nível 3).
  Persistido no save.

**Arte (direção inspirada em Minish Cap):**
- Proporção **chibi** (cabeça grande), cores vivas e saturadas com
  contorno escuro; cenários coloridos (grama viva, terra clara, cripta
  em azul-púrpura saturado).
- **Tiles desenhados** (32×32, sem costura): grama com folhinhas, terra
  salpicada e lajota de pedra na cripta — aplicados por textura
  repetida nos polígonos de chão.
- Sprites de **todos** os personagens: Aria (idle com piscada, andar,
  rolar, atacar em 3 direções + flip), Ecoado, Brutamontes, o boss
  espectral e os 3 NPCs; ícones de itens e props.
- Pipeline iterável: `../tools/generate_aria_sprites.py` e
  `../tools/generate_world_sprites.py` — cada pose é uma grade de
  caracteres (1 letra = 1 pixel); edite e rode para regenerar.

**Áudio (100% procedural):**
- **3 músicas** compostas por síntese (`../tools/generate_audio.py`):
  tema da vila com o leitmotiv da "Canção do Mundo" (ocarina + pads),
  drone tenso da cripta e tema de boss acelerado. Loop automático e
  troca sozinha ao engajar/derrotar o boss.
- **11 SFX**: golpe, acerto, dano, rolamento, coleta, morte, santuário,
  rugido do boss, vitória, UI.

## Como rodar

1. Instale o [Godot 4.3+](https://godotengine.org/download) (a versão padrão, não a .NET).
2. Abra o Godot → *Import* → selecione `game/project.godot`.
3. Pressione **F5** para rodar.

### Controles (desktop)

| Ação | Tecla | Gamepad |
|---|---|---|
| Mover | WASD / setas | Analógico esquerdo |
| Atacar | J ou Z | X / Quadrado |
| Rolar | K ou Espaço | A / Cruz |
| Aparar (parry) | F | R2 / RT |
| Lock-on (alvo) | L ou Tab | R1 / RB |
| Usar item (gancho) | H | B / Círculo |
| Bomba de Eco | B | D-pad esquerda |
| Beber Frasco de Essência | Q | D-pad cima |
| Ocarina (melodias) | M | L1 / LB |
| Mapa | V | D-pad direita |
| Bolsa (inventário) | I | Select / Back |
| Pause / opções | Esc | Start |
| Interagir (falar/descansar/forjar) | E ou Enter | Y / Triângulo |

No Android/touchscreen: arraste na metade esquerda da tela para mover;
botões ATACAR / ROLAR / USAR à direita.

## Exportar para Android

1. Godot → *Editor* → *Manage Export Templates* → baixe os templates da sua versão.
2. Instale o Android SDK (ou use o Android Studio já instalado) e configure o
   caminho em *Editor Settings* → *Export* → *Android*.
3. *Project* → *Export* → *Add* → *Android*; gere uma debug keystore quando pedido.
4. Exporte o APK e instale no aparelho (`adb install`).

O projeto já está configurado para mobile: renderer `gl_compatibility`,
resolução interna 640×360 com stretch `viewport/expand`, orientação
landscape por sensor e filtro de textura *nearest* (pixel art).

## Estrutura

```
game/
├── assets/      # sprites em pixel art (gerados por tools/)
├── core/        # autoloads: GameState, GameEvents e SaveManager
├── systems/     # componentes reutilizáveis: Health, Stamina, Hitbox, Hurtbox
├── entities/
│   ├── player/  # Aria: máquina de estados + lock-on + sprites animados
│   ├── enemies/ # EnemyBase (Ecoado, Brutamontes) + BossEcoGuardia (2 fases)
│   └── npc.*    # NPCs com diálogo
├── world/       # Level (base de cena), vila, cripta, gym, portais,
│                # santuário, spawners, pickups de Eco
└── ui/          # título, HUD (+ barra de boss), diálogo, controles de toque
```

## Próximos passos (ver GDD, seção 8)

A **campanha principal está content-complete**: Atos 1–3, 5 dungeons + 6
bosses + Selene, os 4 itens de dungeon e os 3 finais. A partir daqui o
trabalho é de refino e longevidade:

1. **Playtest e balanceamento** — jogar de ponta a ponta e calibrar
   vida/dano/custos de cada boss e área (a prioridade real).
2. **Export Android de teste (APK)** e ajuste dos controles de toque.
3. Mais Memórias Perdidas (das 12) e side quests; bestiário/diário.
4. Combo de 3 golpes / ataque carregado; mais armas com movesets.
5. Localização EN e preparação da página da loja.
