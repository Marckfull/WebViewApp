# Ecos de Lirael — Projeto Godot (`/game`)

Ação-aventura souls-like com pixel art HD para Android. Protagonista: **Aria**, cartógrafa
surda de um ouvido que escuta a Canção do Mundo. Ver o **GDD completo** e o
**catálogo de recursos gratuitos** em [`/docs/ECOS_DE_LIRAEL_recursos_gratuitos.md`](../docs/ECOS_DE_LIRAEL_recursos_gratuitos.md).

Este diretório é o projeto **Godot 4.x**. O wrapper Android (APK/AAB) é gerado pelo
próprio export da engine — o app WebView legado na raiz do repositório **não** é a base do jogo.

## Estado atual: Fase 0 (combate) + fundação da Fase 1 (vertical slice)

**Fase 0 — combate greybox** (responde ao *gate* do GDD, "o combate é divertido?"):

- **Movimento 8-direções** (teclado/gamepad + **stick virtual touch**).
- **Combate souls-like**: stamina governando ataque/esquiva/defesa, **i-frames** na esquiva,
  **parry** com janela por dificuldade, **poise/postura** → atordoamento → finalização.
- **Lock-on touch** ("Z-targeting" adaptado) — o risco nº 1 do projeto, prototipado primeiro.
- **Save** JSON versionado com escrita atômica (autoload).
- **Arquitetura data-driven**: armas e inimigos em `Resource`/`.tres`.

**Fase 1 — vertical slice (fundação)**: o loop souls completo e a 1ª dungeon:

- **Cripta das Guardiãs** (`cripta_das_guardias.tscn`, cena principal): sala com paredes,
  câmera que segue Aria, NPC, santuário, itens e Ecoados.
- **Santuário (bonfire)**: descansar restaura vida/frascos, **salva**, define ponto de respawn
  e **renasce os inimigos** comuns.
- **Morte com peso**: ao morrer, Aria dropa os Ecos no local; recupera interagindo — morrer de
  novo antes apaga o drop. Renasce no último santuário.
- **Frascos de Essência** (cura tipo Estus, limitada, recarregável no santuário).
- **Sistema de interação** + **diálogo data-driven** (NPC **Corvo**, tutorial diegético,
  descrições em `.tres`).
- **Ocarina de Vidro**: item que registra a Canção do Mundo (§2), e a **roda de melodias**
  (mini-teclado de 5 notas). Tocar a Canção do Mundo **acalma os Ecoados** (§3.1).
- **1º Boss — Guardiã do Eco**: máquina de estados, ataques telegrafados, **2 fases**
  (a 50% de vida acelera e muda o padrão) e barra de vida própria (§3.2).
- **Gancho de animação** no Player: pronto para dirigir um `AnimatedSprite2D` no swap de arte.
- **Menu principal** (Novo Jogo / Continuar / Carregar / Dificuldade / Sair) e **menu de pausa**
  (Continuar / Salvar / Opções / Bestiário / Menu) com dificuldade em tempo real (§3.4, §3.6).
- **Save slots (3 manuais)** (§3.6): salvar pela pausa e carregar pelo menu, com resumo por slot
  (Ecos, Memórias). Além do autosave. Carregar retoma a **cena e posição** salvas.
- **Progressão nos santuários**: gastar Ecos para subir **Vitalidade/Stamina** (custo
  escala com o nível total, estilo souls) — aplica os stats na hora (§3.3).
- **Legibilidade de combate**: câmera com limites da sala + **reticle de lock-on** (§3.1).
- **Ecoados com combate justo**: aggro por distância, **ataque telegrafado** (aviso visual),
  janela curta de acerto e recuperação, + separação para não empilharem (§3.2).
- **Armas com moveset** (§3.3): Espada da Guardiã (forte/lenta) vs. Adagas Gêmeas
  (rápidas/fracas). Trocar muda dano, postura, custo de stamina e velocidade do golpe.
- **Persistência** (§3.6): autosave em marcos (morte, boss, melodia); **boss derrotado
  não renasce** após carregar; **EcoDrop persiste** (fechou o app, os Ecos continuam lá).
  Migração de save robusta: saves antigos ganham chaves novas sem quebrar (§6.2).
- **Bestiário automático** (§3.6): preenche ao derrotar inimigos; visualizável no menu de pausa.
- **Dungeon em alas** (§3.6): a Cripta tem 2 salas ligadas por **portas**; atravessar recria a
  sala (streaming leve) e reposiciona Aria na entrada — cada ala é um **checkpoint** natural.
  O EcoDrop guarda a sala onde caiu (não reaparece na ala errada).
- **Combos** (§3.2/§3.3): encadear golpes na janela avança o combo até `combo_length` da arma;
  o golpe final é um **finalizador** (mais dano de postura). Muda o ritmo por arma.
- **Ciclo dia/noite** (§3.5/§4): a **Canção do Crepúsculo** (2ª melodia da Ocarina) alterna
  dia↔noite; um `CanvasModulate` tinge a cena (azul-frio à noite) — "a cor conta a história".
- **Fast-travel via Ocarina** (§3.1/§3.5): a **Canção do Retorno** (3ª melodia) abre a viagem
  entre **santuários descobertos** — descansar num santuário o registra; escolher um teleporta
  Aria (trocando de sala se preciso). A ocarina vira sistema de traversal, um pilar do GDD.
- **Feedback de hit** (§5): flash de dano (Aria, Ecoados, boss) + tremor de câmera ao ser atingida.
- **Acessibilidade — indicador de ameaça** (§3.6): Aria é surda de um ouvido, então ataques de
  inimigos **fora da tela** viram uma **seta na borda** apontando para a ameaça (o que ela "ouviria").
- **Inimigos noturnos** (§3.5): o **Ecoado Noturno** (mais rápido/perigoso) só aparece à noite —
  dormente e invisível de dia. Respawn preserva a variante (`EnemyData`).
- **Variedade de inimigos** (§3.2): **Ecoado Couraçado** (tanque, lento, postura alta — exige
  golpes pesados/finalizadores para atordoar). Cor por `EnemyData.tint` no greybox.
- **Metroidvania leve** (§3.3): o **Gancho-corda** (item de dungeon) abre uma passagem antes
  bloqueada — uma alcova selada com a **1ª Memória Perdida** (§2). Sem o gancho, a porta dá a dica;
  com ele, Aria se puxa pelo vão. Itens/memórias persistem e aparecem no Diário (pausa).
- **Mini-mapa que se desenha** (§3.5): Aria é cartógrafa — o mapa no canto revela-se conforme
  ela explora (fog-of-war), marca pontos de interesse descobertos e **persiste por sala** no save.
- **Partículas de impacto** (§5): faísca a cada golpe acertado (Aria, Ecoados, boss), via `CombatFx`.
- **Desfecho da fatia** (§7): derrotar a Guardiã do Eco dispara a tela de **fim da demonstração**
  (fade + fecho temático) com opção de **voltar ao menu** ou **Novo Jogo+**.
- **New Game+** (§3.6): rejogar mantendo a progressão (atributos, armas, itens, melodias,
  memórias, recursos) com o mundo **remixado mais difícil** (inimigos +vida/+dano por ciclo).
  Barato de produzir, dobra a longevidade. Ciclo mostrado no Diário.
- **2ª habilidade — Bomba de Eco** (§3.3): estilhaça uma **parede rachada** (gating diferente do
  gancho: remove barreira), abrindo uma câmara selada com a **2ª Memória Perdida** na Ala 2.
  Paredes quebradas persistem no save.
- **3ª habilidade — Lente da Verdade** (§3.3): **revela o invisível** — uma **3ª Memória Perdida**
  só aparece (no mundo e no mini-mapa) depois que Aria pega a Lente. Gating por revelação.
- **Consumíveis — 4 slots no HUD** (§3.5): acesso rápido a 4 slots; **Poção de Vigor** (restaura
  stamina) e **Poção de Cura** (restaura vida) já implementadas. Trocar de slot (C / botão TROCA)
  e usar o ativo (G / botão ITEM). Contagens persistem no save; slot ativo destacado.
- **Forja da Mestra Odara** (§3.3): coletar **minério** e gastar Ecos + minério na forja para
  subir o nível da arma equipada (mais dano, até +5). Nível persiste e aparece no rótulo da arma.
- **Alquimia** (§3.5): coletar **ervas** e destilá-las em Poções de Vigor na alquimista
  (2 ervas → 1 poção). Fecha o ciclo coletar-recurso → craft → consumível.
- **Side quest** (§3.5): NPC oferece "A Casca Teimosa" (derrote o Ecoado Couraçado); o
  `QuestManager` acompanha o progresso via eventos e o NPC paga a recompensa. Data-driven
  (`QuestData`), estado persiste no save.
- **Telemetria opt-in** (§6.3): desligada por padrão; ligável nas Opções (pausa). Registra
  localmente onde Aria morre/descansa/derrota bosses (JSONL em `user://telemetry/`) para
  balancear a dificuldade com dados. Nada sai do dispositivo.

## Estrutura

```
game/
├── project.godot              # 640×360 interna, landscape, autoloads, GL Compatibility (low-end)
├── scenes/
│   ├── world/                 # cripta_das_guardias (PRINCIPAL), greybox_gym, shrine, eco_drop, ocarina
│   ├── player/player.*        # Aria (+ Camera2D, InteractionDetector)
│   ├── enemies/dummy_enemy.*  # Ecoado
│   └── npc/corvo.*            # mercador Corvo
├── scripts/
│   ├── autoload/              # GameConfig, GameEvents, SaveManager, DialogueManager
│   ├── combat/                # hitbox, hurtbox, health(+poise), stamina, lock_on
│   ├── systems/               # interactable, npc
│   ├── world/                 # game_world (loop souls), shrine, eco_drop, melody_pickup
│   ├── data/                  # weapon_data, enemy_data, dialogue_data (data-driven §6.2)
│   └── ui/                    # virtual_joystick, hud
├── data/                      # .tres: armas, inimigos, diálogos
└── assets/                    # vazio — ver assets/README.md para sourcing
```

## Como rodar

1. Instalar **Godot 4.3** (standard, não .NET).
2. Abrir `game/project.godot`.
3. **F5** — abre a **Cripta das Guardiãs**. Pegue a Ocarina (E/USAR), fale com o Corvo,
   descanse no Santuário e enfrente os Ecoados. A `greybox_gym.tscn` continua disponível
   para tuning isolado de combate.

## Controles

| Ação | Teclado | Gamepad | Touch |
|------|---------|---------|-------|
| Mover | WASD | stick esq. | stick virtual |
| Atacar | J | X | botão ATK |
| Esquivar | Espaço | A | botão DODGE |
| Defender/Parry | K | R1 | botão GUARD |
| Lock-on | Q | L1 | botão LOCK / tocar inimigo |
| Interagir | E | Y | botão USAR |
| Curar (frasco) | H | D-pad ↑ | botão HEAL |
| Ocarina | F | B | botão OCARINA |
| Trocar arma | Tab | D-pad → | botão SWAP |
| Usar item (slot) | G | D-pad ↓ | botão ITEM |
| Trocar slot de item | C | D-pad ← | botão TROCA |
| Pausa / Opções | Esc | Start | botão II |

## Próximos passos (completar a Vertical Slice)

- **Arte**: substituir os `Polygon2D` por **Ninja Adventure (CC0)** — o Player já tem o gancho
  para um `AnimatedSprite2D` "Sprite" (idle/walk/attack/dodge/hurt, 8 direções); tiles da Cripta
  via `TileMapLayer` + **Tiled**.
- **Mais melodias da Ocarina** (fast-travel entre santuários, dia/noite) — sistema já é
  data-driven (`MelodyData`), basta novos `.tres`.
- **Iluminação 2D** (tochas, dessaturação do Silêncio) e áudio (SFX Kenney CC0 + leitmotiv).
- **GUT**: testes de save, stamina, economia de Ecos e matching de melodias (§6.3).
- Em produção, avaliar troca do diálogo próprio pelo **godot_dialogue_manager** (i18n PT-BR/EN).

## Testes e CI

**Testes unitários com GUT** (`addons/gut/`, MIT) em `test/unit/` — cobrem save
(round-trip, versão), economia de stamina, regras de dificuldade e o matching de
melodias da Ocarina (§6.3).

Rodar localmente:

```
godot --headless -s res://addons/gut/gut_cmdln.gd -gdir=res://test/unit -ginclude_subdirs -gexit
```

O workflow `.github/workflows/godot-ci.yml` a cada push: (1) importa o projeto e valida
os scripts, (2) *smoke test* — inicializa a Cripta em headless, (3) roda a suíte GUT.
O export Android assinado (AAB) é a etapa final, adicionada quando houver keystore/secrets.
