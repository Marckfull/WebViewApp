# Ecos de Lirael — Status e Pendências por Setor

> Auditoria completa: o que já existe (em **greybox jogável**, validado por CI) e o que
> **falta** para o jogo final descrito no GDD. Data-base: fim da fase de prototipagem de sistemas.
>
> **Legenda:** ✅ feito · 🟡 parcial/greybox · ⬜ não iniciado · ⛔ bloqueado por arte/áudio/binários.
>
> **Estado geral:** todos os *sistemas* do GDD existem em greybox (formas geométricas, sem
> arte/som). CI: 5 smoke tests + 16 suítes GUT, verde. O gargalo agora é **conteúdo** (arte,
> áudio, level design, narrativa) — não mais engenharia de sistemas.

---

## 1. História / Narrativa

| Item | Estado | Falta |
|---|---|---|
| Lore ambiental (itens, diálogos) | 🟡 | Fragmentos existem; falta densidade e revisão |
| Memórias Perdidas | 🟢 | **12 de 12** implementadas como `.tres` + pontos de coleta (o final secreto já é completável na fatia) |
| NPCs | 🟡 | Corvo (agora **mercador com loja**), "Sobrevivente" (quest). Faltam **Lys, Selene (Guardiã Caída), Mestra Odara** como personagens |
| Roteiro dos 3 atos | 🟡 | Story bible escrito (`ECOS_DE_LIRAEL_roteiro.md`); em cena só o Ato 1 (prólogo + Cripta) |
| Cutscenes | 🟡 | Sistema pronto; **3 cutscenes narradas** (aproximação do boss, palavras da Guardiã ao cair, eco de Selene). Faltam as ilustradas (§4) |
| Finais | 🟡 | Só "fim da demonstração". Faltam **2 finais + 1 secreto** (12 memórias) |
| Bestiário/diário (lore) | 🟡 | Preenche ids automaticamente; faltam textos ricos e arte |
| Localização | 🟡 | Só PT-BR hardcoded. Falta **EN** e sistema gettext/CSV (§Alpha→Beta) |
| Diálogo ramificado | ⬜ | Sistema atual é linear (1 falante). GDD/Corvo pedem ramificação (ou trocar por godot_dialogue_manager) |

## 2. Gameplay / Sistemas

**Núcleo de combate (✅ greybox completo):** stamina, esquiva com i-frames, parry (janela por
dificuldade), poise/postura, lock-on touch, combos, morte com drop de Ecos, frascos.

| Item | Estado | Falta |
|---|---|---|
| Armas | 🟡 | **2 de 6** (espada, adagas). Faltam lança, martelo, arco (projétil), chicote — cada uma um moveset |
| Forja / upgrade | ✅ | Funcional (Ecos+minério, até +5) |
| Armaduras + amuletos | ⬜ | 4 conjuntos + amuletos com passivas e slots de build — nada |
| Atributos | 🟡 | Vitalidade/Stamina afetam stats; **Força/Destreza/Harmonia não fazem nada ainda** |
| Bosses | 🟡 | **1 de 8** principais (+4 opcionais). Falta mecânica única por dungeon (filosofia Zelda) |
| Inimigos | 🟡 | **3 variantes** (comum/noturno/couraçado) de ~35. Faltam ranged, voadores, especiais |
| Itens de dungeon | ✅ | Gancho, Bomba, Lente implementados. Falta **Botas de Corrente** (água/ímã) |
| Ocarina (melodias) | ✅ | 4 melodias funcionais (acalmar/dia-noite/viagem/selo). GDD prevê 8 |
| Progressão de morte (souls) | ✅ | Drop/recuperação de Ecos, respawn, persistência |
| Loja / mercador (Corvo) | 🟢 | Corvo abre **loja** (comprar poções/recursos por Ecos, `ShopMenu` + `ShopData`); falta vender/recomprar e estoque por dia |
| Side quests | 🟡 | **1 de 12–15**. Sistema pronto; faltam conteúdo e outros tipos (coletar/escoltar) |
| Recursos coletáveis | 🟡 | Minério e erva existem. Faltam tiers (3 minérios), madeira, cogumelos/peixes/insetos |
| Ciclo dia/noite | 🟡 | Visual (CanvasModulate) + inimigos noturnos OK. Falta **rotina de NPC / lojas fecham à noite** |
| New Game+ | ✅ | Remix por multiplicadores |
| Dificuldade | 🟡 | 3 modos existem; **balanceamento é placeholder** (sem playtest/telemetria real) |

## 3. Layout / Level Design

| Item | Estado | Falta |
|---|---|---|
| Salas greybox | 🟡 | 2 salas da Cripta (paredes/portas/alcovas). Sem tilemap real |
| Overworld | ⬜ | Mundo aberto em chunks (streaming, §6.2) — não existe |
| Vila de Pedra-Alva (Ato 1) | ⬜ | Área de tutorial diegético — não existe |
| 5 dungeons | 🟡 | Só a Cripta (2 salas). Faltam Floresta Sussurrante, Forja Afundada, Torre dos Ventos, Necrópole de Sal |
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
| Inventário em grade | ⬜ | Grade paginada por categoria (§3.5) — só há contadores/slots rápidos |
| Descrições ricas de item | 🟡 | Lore em itens de dungeon; falta tela de inventário com descrições |
| Tela de mapa completa | 🟡 | Só mini-mapa. Falta mapa cheio com pins manuais |
| Ícones / fontes | ⛔ | ~200 ícones 24×24, fonte bitmap PT-BR — hoje texto/formas |
| Opções de acessibilidade | 🟡 | Dificuldade + indicador de ameaça OK. Faltam **tamanho de fonte, daltonismo, vibração, remapeamento completo** |
| Layout touch customizável | ⬜ | Botões fixos; falta posição/escala + modo canhoto (§3.1) |
| Tutorial diegético / prompts | ⬜ | Sem prompts contextuais |

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
unificado (teclado/gamepad/touch), telemetria opt-in, CI (import+smoke+GUT), **16 suítes de teste**.

| Item | Estado | Falta |
|---|---|---|
| Animação de ataque | 🟡 | Timing por timer; falta **AnimationPlayer** ligando hitbox a frames de sprite |
| Streaming de mundo (chunks) | ⬜ | Cenas inteiras hoje; GDD §6.2 pede chunks por proximidade |
| Sistema de projétil | ⬜ | Necessário para arco e inimigos ranged |
| Sistema de loja | ⬜ | Comprar/vender |
| Inventário (backend) | ⬜ | Grade/categorias/equipar armadura/amuletos |
| Sistema de cutscene | ⬜ | Player de cutscene + skip |
| Rotina de NPC / agenda | ⬜ | NPCs com horários, lojas fecham à noite |
| Localização (gettext/CSV) | ⬜ | PT-BR/EN |
| Export Android (AAB) assinado | 🟡 | Job de CI existe **desabilitado**; faltam keystore/secrets + presets |
| Performance low-end | ⬜ | Perfilar 60 FPS Moto G, RAM <700 MB, APK <300 MB; object pooling |
| Botas de Corrente (água/ímã) | ⬜ | Mecânica de movimento especial |
| Efeitos de Força/Destreza/Harmonia | ⬜ | Ligar atributos a dano/velocidade/poder de melodia |
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
3. **Narrativa:** roteiro dos 3 atos, os personagens (Lys/Selene/Odara), cutscenes, 2 finais + secreto.
4. **Conteúdo:** as 4 outras dungeons, os 7 outros bosses, ~32 inimigos, 4 armas restantes, armaduras.
5. **Sistemas restantes:** inventário em grade, loja, projéteis, streaming, rotinas de NPC.
6. **Acessibilidade/opções completas** + localização EN.
7. **Otimização low-end + export AAB assinado + soft launch.**

## O que NÃO falta (já pronto em greybox, validado)

Combate souls-like completo · 2 armas + combos + forja · atributos/frascos/poções/alquimia ·
3 itens de dungeon (gancho/bomba/lente) + portas por som · 3 inimigos + boss 2 fases + NG+ ·
ocarina 4 melodias (traversal) · dia/noite · mini-mapa · bestiário/diário/4 memórias · quest ·
save completo (autosave+3 slots+migração) · telemetria · indicador de ameaça · menus/pausa/opções ·
persistência total entre sessões/salas · CI + 16 suítes de teste.
