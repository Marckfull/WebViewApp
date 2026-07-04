# Ecos de Lirael — Protótipo Jogável (Fase 0+)

Projeto **Godot 4.3+** do action-RPG descrito em [`../docs/GDD-PLANO-DO-JOGO.md`](../docs/GDD-PLANO-DO-JOGO.md).

Greybox jogável que valida o núcleo do jogo antes da arte final: combate
souls-like, exploração entre cenas, NPCs com diálogo e a primeira luta de boss.

## O que já está jogável

**Combate (núcleo souls-like):**
- Movimentação analógica em 8 direções (Aria, o retângulo verde-água 🙂).
- Ataque com varredura de espada, custo de stamina e janela de acerto.
- Esquiva-rolamento com i-frames; stamina com regeneração após pausa curta.
- **Lock-on de alvo** (Z-targeting): trava a mira no inimigo mais próximo,
  pressione de novo para alternar entre alvos; movimento vira strafe.
- Inimigos "Ecoados" com telegraph e janela de punição; mini-boss
  "Brutamontes" com poise.
- **Loop de morte souls-like**: Ecos dropados no local da morte,
  recuperáveis; santuários curam e repõem os inimigos.

**Mundo (4 cenas conectadas por portais):**
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
- **Campo de Treino**: a arena da Fase 0, para testar builds e números.

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
| Lock-on (alvo) | L ou Tab | R1 / RB |
| Usar item (gancho) | H | B / Círculo |
| Beber Frasco de Essência | Q | D-pad cima |
| Ocarina (melodias) | M | L1 / LB |
| Mapa | V | D-pad direita |
| Bolsa (inventário) | I | Select / Back |
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

1. **Playtest geral** — o protótipo cobre agora todos os sistemas
   centrais do GDD; a prioridade é jogar e calibrar números.
2. Menu de pause com opções (volume, resetar save).
3. Terceira dungeon (Forja Afundada) com o item Bomba de Eco.
4. Polimento de combate: hit-stop, partículas, screen shake.
5. Export Android de teste (APK) e ajuste dos controles de toque
   em aparelho real.
