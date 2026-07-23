# Ecos de Lirael — Status e Pendências por Setor

> Auditoria completa: o que já existe (em **greybox jogável**, validado por CI) e o que
> **falta** para o jogo final descrito no GDD. Data-base: fim da fase de prototipagem de sistemas.
>
> **Legenda:** ✅ feito · 🟡 parcial/greybox · ⬜ não iniciado · ⛔ bloqueado por arte/áudio/binários.
>
> **Estado geral:** todos os *sistemas* do GDD existem em greybox (formas geométricas, sem
> arte/som), e os 3 atos são jogáveis de ponta a ponta. CI: **17 smoke tests + 27 suítes GUT**,
> verde. O gargalo agora é **arte, áudio e densidade de conteúdo** — não mais engenharia de sistemas.

---

## 1. História / Narrativa

| Item | Estado | Falta |
|---|---|---|
| Lore ambiental (itens, diálogos) | 🟡 | Fragmentos + **inscrições examináveis** (`lore_stone`) nos 4 átrios, prenunciando a verdade de cada dungeon. Falta mais densidade e revisão final |
| Memórias Perdidas | 🟢 | **12 de 12** implementadas como `.tres` + pontos de coleta (o final secreto já é completável na fatia) |
| Diálogo ramificado | 🟢 | Escolhas em árvore (`choice_texts`/`choice_next`), botões no balão; NPC Errante com lore ramificado. Falta variáveis/condições nas escolhas |
| NPCs | 🟢 | Em cena: Corvo (mercador com loja), **Lys** (irmã, na vila e no Ato 3), **Mestra Odara** (Cripta e Forja), **Selene** (fala nas cutscenes e no duelo), aldeões, "Sobrevivente" (quest). Falta profundidade (rotinas, ramificação) |
| Roteiro dos 3 atos | 🟢 | Jogáveis em greybox os 3 atos: Ato 1 (**vila de Pedra-Alva** jogável + prólogo + Cripta), Ato 2 (Encruzilhada + 4 dungeons de **2 salas** com revelações) e Ato 3 (Coração Mudo + Lys + Selene + finais). Falta densidade de cena e arte/som |
| Cutscenes | 🟡 | Sistema pronto; **10+ cutscenes narradas** (vila/rapto de Lys, aproximação e queda da Guardiã, eco/confronto/duelo de Selene, reencontro de Lys, 4 revelações de dungeon). Faltam as ilustradas (§4) |
| Finais | 🟢 | **2 finais + 1 secreto** implementados (escolha no Coração Mudo ao derrotar Selene; o secreto exige as 12 Memórias). Falta arte/epílogos ilustrados |
| Bestiário/diário (lore) | 🟡 | Preenche ids automaticamente; faltam textos ricos e arte |
| Localização | 🟡 | **Sistema PT-BR/EN** (`Locale` + tabelas JSON, `LocaleTable` puro/testado, toggle nas Opções, idioma no save). Menu principal + pausa já traduzidos. Falta extrair todas as strings (diálogos/lore) e, se quiser, migrar para gettext |

## 2. Gameplay / Sistemas

**Núcleo de combate (✅ greybox completo):** stamina, esquiva com i-frames, parry (janela por
dificuldade), poise/postura, lock-on touch, combos, morte com drop de Ecos, frascos.

| Item | Estado | Falta |
|---|---|---|
| Armas | 🟢 | **6 de 6**: espada, adagas, lança, martelo, arco (projétil) e **chicote** (alcance máximo, combo longo, baixa postura). Cada uma um moveset por dano/postura/custo/velocidade/**alcance**; as extras são loot guardado no save |
| Forja / upgrade | ✅ | Funcional (Ecos+minério, até +5) |
| Armaduras + amuletos | 🟢 | Sistema completo (`EquipmentData`/`Equipment`): 2 slots com **redução de dano, vida e regen de stamina**, peças como loot (Couraça de Sal, Amuleto do Eco), **tela de equipar no Santuário** (trocar arma/armadura/amuleto entre as possuídas) + persistência. Faltam mais peças e builds |
| Atributos | 🟢 | Os **5** ligados ao jogo: Vitalidade→vida, Stamina→stamina, **Força→dano, Destreza→custo de stamina, Harmonia→janela de parry** (efeitos puros e testados) |
| Bosses | 🟢 | **8 encontros**: 6 canônicos (Guardiã do Eco; Coro Enraizado, Martelo Mudo, Sino Invertido, Maré Salgada; Selene, com duelo de melodias) + **2 opcionais** (Ecos Maiores do Alvorecer/Ocaso, enfrentados juntos na Arena dos Ecos). Cada chefe de dungeon tem **moveset-assinatura** data-driven: Coro Enraizado **invoca capangas** na fase 2, Martelo Mudo **avança no golpe**, Sino Invertido e Maré Salgada **disparam projéteis** (1 e leque de 3) |
| Inimigos | 🟡 | **6 variantes** (comum/noturno/couraçado/arqueiro ranged/alado voador/**divisor** que se parte em fragmentos ao morrer) de ~35. Faltam mais especiais (invocadores, etc.) |
| Itens de dungeon | ✅ | Os **4** implementados: Gancho, Bomba, Lente e **Botas de Corrente** (cruzam correntes de sal, `CurrentGate`) |
| Ocarina (melodias) | ✅ | **8 de 8**: acalmar, dia-noite, viagem, selo + **cura, fôlego, coragem (dano temp.), refúgio** (efeitos de suporte no Player). GDD completo |
| Progressão de morte (souls) | ✅ | Drop/recuperação de Ecos, respawn, persistência |
| Loja / mercador (Corvo) | 🟢 | Loja com **3 modos**: Comprar (ofertas), **Vender** (esvazia consumíveis/recursos por Ecos, `Shop.sell_value`) e **Recomprar** (rebate o vendido na visita). Falta estoque por dia |
| Missões principais | 🟢 | **Campanha do Ato 2 rastreada** (`MainQuestManager`): 4 missões (uma por dungeon) com etapas que avançam por eventos reais (obter item / abater chefe / abater inimigo), em paralelo (hub não linear), com **objetivo no HUD** ("N/4 Santuários"). Detalhes completos em `ECOS_DE_LIRAEL_missoes.md` |
| Side quests | 🟡 | **10 quests rastreadas** (KILL, COLLECT, ESCORT): as 5 originais + O Cão Sem Nome (vila), As Ervas da Alquimista (Cripta), **O Corvo Deve um Favor** (Encruzilhada), **A Torre da Vigia** (Ventos Altos) e **As Botas da Primeira Ecoada** (Tumbas de Sal). Do documento (MS-1..MS-10), faltam só MS-8 (Duelo da Arena) e MS-10 (Coro das 12 Memórias) |
| Recursos coletáveis | 🟢 | **3 tiers de minério** (bruto/ressonante/do eco) para a forja; **3 tiers de erva** (erva/prateada/do eco) e **forrageio** (madeira, cogumelo, peixe, inseto) espalhados pelos átrios. A **bancada da alquimista** é data-driven (`Alchemy.RECIPES`): ervas viram poções, e receitas avançadas combinam erva+forrageio em **Elixir do Eco** (buff de dano) e **Refeição Farta** (cura + fôlego) |
| Ciclo dia/noite | 🟢 | Visual (CanvasModulate) + inimigos noturnos + **rotina de NPC**: a loja do Corvo fecha à noite e NPCs têm fala noturna (Odara). Falta agenda de movimento/posições |
| New Game+ | ✅ | Remix por multiplicadores |
| Dificuldade | 🟡 | 3 modos existem; **balanceamento é placeholder** (sem playtest/telemetria real) |

## 3. Layout / Level Design

| Item | Estado | Falta |
|---|---|---|
| Salas greybox | 🟡 | **13 salas jogáveis** (vila, 2 da Cripta, Encruzilhada, 4 dungeons × 2 salas, Coração Mudo). Formas geométricas, sem tilemap real |
| Overworld / hub | 🟡 | Encruzilhada dos Ecos liga as 4 dungeons e a descida ao Coração Mudo. Falta o mundo aberto em chunks (streaming, §6.2) |
| Vila de Pedra-Alva (Ato 1) | 🟡 | **Jogável** (`vila_pedra_alva.tscn`): prólogo com Lys, aldeões e o rapto pelo Silêncio, com **dicas diegéticas de combate** no caminho até a Cripta. Falta arte |
| 5 dungeons | 🟢 | Cripta (2 salas) + as **4 do Ato 2 agora com 3 salas cada** (átrio → sala intermediária → boss): Floresta (**Bosque**/Divisor), Forja (**Fornalhas**/Couraçado), Torre (**Ventos Altos**/Alado) e Necrópole (**Tumbas de Sal**/Arqueiro). Cada sala do meio tem obstáculo, combate próprio e forrageio. Falta só aprofundar (ramais/segredos) |
| Alas de 10–15 min c/ checkpoints | 🟡 | Estrutura de portas/checkpoint pronta; falta conteúdo de nível |
| Densidade de segredos | 🟡 | Alguns (memórias). GDD pede "cada tela esconde algo" |
| Paralaxe (3+ camadas) | ⛔ | Depende de arte de cenário |
| Tilemaps (Tiled + TileMapLayer) | ⛔ | Depende de tilesets |

## 4. HUD / UI

**Feito (✅ greybox):** vida/stamina/Ecos, frascos, arma (+nível), **4 slots de consumível**,
barra de boss, **mini-mapa que se desenha**, **indicador de ameaça fora da tela**, menu principal,
pausa (Salvar/Opções/Bestiário), **save slots**, forja, alquimia, viagem, diálogo, roda da ocarina.

| Item | Estado | Falta |
|---|---|---|
| Inventário / diário | 🟢 | Tela de **Inventário na pausa**: armas, armadura/amuleto, itens-chave, memórias e Ecos em texto, mais uma **grade visual da bolsa** (`ItemIcons`) — uma célula por consumível/recurso com ícone greybox colorido, nome curto e quantidade. Falta só a arte final dos ícones |
| Descrições ricas de item | 🟡 | Lore em itens de dungeon; falta tela de inventário com descrições |
| Tela de mapa completa | 🟢 | **Tela de Mapa na pausa** (`WorldMap`): lista as regiões e as salas já descobertas (cruzando o fog-of-war persistido do mini-mapa), marca a sala atual e mostra "N/M salas exploradas". Regiões intocadas aparecem como "???". Falta só desenho/pins manuais |
| Ícones / fontes | ⛔ | ~200 ícones 24×24, fonte bitmap PT-BR — hoje texto/formas |
| Opções de acessibilidade | 🟡 | Dificuldade + indicador de ameaça, **reduzir tremor de câmera** e **texto grande** (escala a fonte dos menus/diálogo via tema raiz), persistidos em settings (`Accessibility`). Faltam **daltonismo, vibração, remapeamento completo** |
| Layout touch customizável | 🟢 | Painel **Controles de toque** (Opções): **escala dos botões** (100%→160%) e **modo canhoto** (espelha o cluster de ação e joga o stick pra metade direita), via `TouchLayout`, persistidos e aplicados ao vivo. Falta só arrastar cada botão livremente |
| Tutorial diegético / prompts | 🟢 | **Dicas diegéticas** (`tutorial_hint.tscn`): auras silenciosas que revelam um texto curto quando Aria se aproxima e somem quando ela se afasta — sem menu, sem pausa. Espalhadas pela vila ensinando andar, correr, atacar, esquivar e interagir no caminho até a Cripta |

## 5. Áudio  ⛔ (nada implementado — 0%)

| Item | Estado | Falta |
|---|---|---|
| SFX | ⬜ | Footsteps por material, **hit-feedback "crocante"**, UI, notas da ocarina, ambiente |
| Trilha | ⬜ | ~25 faixas (overworld dia/noite, 5 dungeons, 8 bosses, vila, temas, finais) |
| Leitmotiv "Canção do Mundo" | ⬜ | Tema central fragmentado — precisa existir **antes** da vertical slice (§8) |
| Música adaptativa | ⬜ | Camadas entram/saem (exploração→combate→Silêncio) |
| Ocarina audível | 🟡 | Lógica das 8 melodias existe; **sem som** (só índices de nota) |
| Vozes murmúrio | ⬜ | — |
| Mix / buses / economia de bateria | ⬜ | — |
| **Ação:** contratar/definir compositor cedo | ⬜ | Pilar do jogo (música = mecânica) |

## 6. Arte Visual  ⛔ (greybox — formas geométricas)

| Item | Estado | Falta |
|---|---|---|
| Aria (protagonista) | ⛔ | Sprite 48×48, 8 direções, todas as anims (idle/andar/correr/rolar/3 ataques por arma/parry/dano/morte/ocarina). Gancho `AnimatedSprite2D` já pronto no código |
| Inimigos + bosses | ⛔ | ~35 inimigos + 12 bosses (sprites + telegrafos) |
| NPCs | ⛔ | ~25 |
| Tilesets | ⛔ | ~40 (32×32) + overlays decorativos |
| Iluminação 2D + partículas | 🟡 | Partícula de hit (CPUParticles) existe; falta luz 2D (tochas/dia-noite/sombras), folhas/poeira/névoa/brasas |
| Ícones de item | ⛔ | ~200 |
| Cutscenes ilustradas | ⛔ | ~30 telas |
| Paleta (são quente / Silêncio dessaturado) | 🟡 | Dia/noite via CanvasModulate; falta direção de cor real |
| Fontes bitmap PT-BR | ⛔ | — |

**Recomendação (do catálogo `docs/ECOS_DE_LIRAEL_recursos_gratuitos.md`):** começar com **Ninja
Adventure (CC0)** para greybox→vertical slice e arte autoral só para Aria/bosses/cutscenes.

## 7. Código / Técnico

**Feito (✅):** arquitetura data-driven (Resources/.tres), 20+ autoloads/sistemas, componentes de
combate reutilizáveis, save JSON versionado + **migração robusta** + 3 slots + autosave, input
unificado (teclado/gamepad/touch), telemetria opt-in, CI (import+smoke+GUT), **27 suítes de teste**.

| Item | Estado | Falta |
|---|---|---|
| Animação de ataque | 🟡 | Timing por timer; falta **AnimationPlayer** ligando hitbox a frames de sprite |
| Streaming de mundo (chunks) | ⬜ | Cenas inteiras hoje; GDD §6.2 pede chunks por proximidade |
| Sistema de projétil | 🟢 | `Projectile` (estende Hitbox): move, dano por Hitbox/Hurtbox, camadas por disparador. Usado pelo **arco de Aria** e pelo **Ecoado Arqueiro**. Falta variar padrões (leque, mira preditiva) |
| Sistema de loja | ⬜ | Comprar/vender |
| Inventário (backend) | 🟡 | Equipar arma/armadura/amuleto pronto (tela no Santuário, `EquipMenu`). Falta grade/categorias geral |
| Sistema de cutscene | ⬜ | Player de cutscene + skip |
| Rotina de NPC / agenda | 🟡 | Loja fecha à noite + fala noturna por NPC (`WorldTime`, testado). Falta agenda de movimento/posições por hora |
| Localização (gettext/CSV) | ⬜ | PT-BR/EN |
| Export Android (AAB) assinado | 🟡 | Job de CI existe **desabilitado**; faltam keystore/secrets + presets |
| Performance low-end | ⬜ | Perfilar 60 FPS Moto G, RAM <700 MB, APK <300 MB; object pooling |
| Botas de Corrente | ✅ | Cruzam correntes de sal (`CurrentGate`), item-chave da Necrópole |
| Efeitos de Força/Destreza/Harmonia | ✅ | Força→dano corpo-a-corpo, Destreza→desconto no custo de stamina, Harmonia→janela de parry (`Attributes`, testado) |
| Combat gym / tuning | 🟡 | Cena greybox existe; falta ferramenta de tuning com dummies configuráveis |
| Testes de integração de combate | 🟡 | Testes puros OK; faltam i-frames/dodge/hitbox em cena |

## 8. Produção / QA / Release

| Item | Estado | Falta |
|---|---|---|
| CI build/validação | ✅ | Import + smoke + GUT a cada push |
| Export AAB assinado | ⬜ | Keystore, export_presets, habilitar job |
| Playtest + balanceamento por telemetria | ⬜ | Telemetria loga; falta coletar/analisar e ajustar |
| Localização EN | ⬜ | — |
| Página da Play Store / trailer / screenshots | ⬜ | — |
| Auditoria de licenças (CREDITS) | 🟡 | Estrutura pronta; preencher conforme assets entram |
| Contratar compositor / artista | ⬜ | Decisão de produção (§7) |
| Soft launch | ⬜ | — |

---

## Prioridades sugeridas (ordem de maior valor)

1. **Áudio + Arte** (o gargalo real): definir compositor e artista; swap dos greybox pelos packs CC0
   (Ninja Adventure/Dungeon Crawl/Kenney) usando os ganchos já prontos. Sem isto o jogo não "existe".
2. **Vertical Slice de verdade:** Ato 1 (vila + Cripta) com arte/som finais — o gate de 30 min do §7.
3. **Narrativa:** dar cena aos personagens (Lys/Selene/Odara), a vila do prólogo, mais lore ramificado.
4. **Conteúdo:** ~31 inimigos, mais peças de equipamento, mais salas por dungeon (bosses já têm movesets próprios).
5. **Sistemas restantes:** streaming de mundo aberto, resto da acessibilidade (daltonismo/vibração/remapeamento).
6. **Acessibilidade/opções completas** + localização EN.
7. **Otimização low-end + export AAB assinado + soft launch.**

## O que NÃO falta (já pronto em greybox, validado)

Combate souls-like completo · **6 armas** (incl. arco/projétil) + combos + forja com **3 tiers de minério** ·
**equipamento (armadura/amuleto) + tela de equipar** · **5 atributos com efeito** · frascos/poções/alquimia ·
**4 itens de dungeon (gancho/bomba/lente/botas)** + portas por som · **4 inimigos** (incl. arqueiro ranged) +
**8 bosses** (incl. duelo de melodias + arena opcional) + NG+ · **3 atos jogáveis** (vila → Cripta →
Encruzilhada + 4 dungeons de 2 salas → Coração Mudo) · **3 finais** (2 + secreto pelas 12 Memórias) ·
ocarina 4 melodias (traversal) · **dia/noite com rotina de NPC** · mini-mapa · bestiário · **inventário na pausa** ·
**4 side quests** (KILL+COLLECT) · loja do Corvo · save completo (autosave+3 slots+migração) · telemetria ·
indicador de ameaça · menus/pausa/opções · persistência total · CI: **17 smoke + 27 suítes de teste**.
