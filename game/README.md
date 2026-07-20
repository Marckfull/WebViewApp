# Ecos de Lirael — Projeto Godot (`/game`)

Ação-aventura souls-like com pixel art HD para Android. Protagonista: **Aria**, cartógrafa
surda de um ouvido que escuta a Canção do Mundo. Ver o **GDD completo** e o
**catálogo de recursos gratuitos** em [`/docs/ECOS_DE_LIRAEL_recursos_gratuitos.md`](../docs/ECOS_DE_LIRAEL_recursos_gratuitos.md).

Este diretório é o projeto **Godot 4.x**. O wrapper Android (APK/AAB) é gerado pelo
próprio export da engine — o app WebView legado na raiz do repositório **não** é a base do jogo.

## Estado atual: Fase 0 — Fundação de combate (greybox)

Implementado o esqueleto jogável que responde ao *gate* do GDD ("o combate é divertido?"):

- **Movimento 8-direções** (teclado/gamepad + **stick virtual touch**).
- **Combate souls-like**: stamina governando ataque/esquiva/defesa, **i-frames** na esquiva,
  **parry** com janela por dificuldade, **poise/postura** → atordoamento → finalização.
- **Lock-on touch** ("Z-targeting" adaptado) — o risco nº 1 do projeto, prototipado primeiro.
- **Morte com peso**: dropa Ecos no local; regra de perda conforme dificuldade.
- **Save** JSON versionado com escrita atômica (autoload).
- **Arquitetura data-driven**: armas e inimigos em `Resource`/`.tres`.

## Estrutura

```
game/
├── project.godot              # 640×360 interna, landscape, autoloads, GL Compatibility (low-end)
├── scenes/
│   ├── world/greybox_gym.*    # CENA PRINCIPAL — gym de combate (§6.3)
│   ├── player/player.*        # Aria
│   └── enemies/dummy_enemy.*  # Ecoado de treino
├── scripts/
│   ├── autoload/              # GameConfig (input+dificuldade), GameEvents (sinais), SaveManager
│   ├── combat/                # hitbox, hurtbox, health(+poise), stamina, lock_on
│   ├── data/                  # weapon_data, enemy_data (data-driven §6.2)
│   └── ui/                    # virtual_joystick, hud
├── data/                      # instâncias .tres (espada, ecoado)
└── assets/                    # vazio — ver assets/README.md para sourcing
```

## Como rodar

1. Instalar **Godot 4.3** (standard, não .NET).
2. Abrir `game/project.godot`.
3. **F5** — abre a `greybox_gym`. No desktop: WASD mover, **J** atacar, **Espaço** esquivar,
   **K** defender/parry, **Q** lock-on. No touch: stick à esquerda, botões à direita.

## Controles

| Ação | Teclado | Gamepad | Touch |
|------|---------|---------|-------|
| Mover | WASD | stick esq. | stick virtual |
| Atacar | J | X | botão ATK |
| Esquivar | Espaço | A | botão DODGE |
| Defender/Parry | K | R1 | botão GUARD |
| Lock-on | Q | L1 | botão LOCK / tocar inimigo |
| Ocarina | F | B | botão OCARINA |

## Próximos passos (Fase 1 — Vertical Slice)

- Substituir formas geométricas por **Ninja Adventure (CC0)** — Aria + 1 dungeon.
- Integrar **godot_dialogue_manager** (NPCs, Corvo, tutorial diegético).
- Santuários (bonfire): descanso, save, respawn, fast-travel por melodia.
- Roda de melodias da Ocarina (mini-teclado de 5 notas).
- Ligar **GUT** (testes) e o **CI de export** (ver `.github/workflows/`).

## Testes e CI

Testes unitários planejados com **GUT** (§6.3) para inventário, save, stamina e quests.
O workflow em `.github/workflows/godot-ci.yml` faz *smoke test* headless (importa e inicializa
a cena principal, pegando erros de script). O export Android assinado (AAB) é a etapa final,
adicionada quando houver keystore/secrets.
