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

**Mundo (3 cenas conectadas por portais):**
- **Pedra-Alva** (vila hub): 3 NPCs com diálogo (Mestra Odara, Corvo e Sela),
  santuário e portais para as outras áreas.
- **Cripta das Guardiãs**: corredor de Ecoados, santuário antes do boss e a
  luta contra o **Eco da Guardiã** — boss de 2 fases com barra própria no HUD:
  o portão fecha ao entrar, a fase 2 (≤50% de vida) fica mais rápida e ganha
  investida tripla, e o boss reseta se você morrer ou descansar. A vitória
  fica gravada (o boss não volta).
- **Campo de Treino**: a arena da Fase 0, para testar builds e números.

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

**Arte:**
- Aria tem **sprites reais em pixel art** (24×24): idle com piscada,
  caminhada, rolamento e ataque com espada em 3 direções (+ flip).
  Os sprites são gerados por `../tools/generate_aria_sprites.py` —
  edite as grades de caracteres e rode o script para iterar a arte.

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
| Interagir (falar/descansar) | E ou Enter | Y / Triângulo |

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

1. Playtest: a sensação do combate e a dificuldade do boss (deve matar o
   jogador mediano 2–4 vezes).
2. SFX de combate e primeira música (a trilha é mecânica central no GDD).
3. Sprites para inimigos e NPCs (mesmo pipeline do gerador da Aria).
4. Sistema de inventário e primeiro item de dungeon (Gancho-corda).
5. Ocarina de Vidro: roda de melodias e a primeira música jogável.
