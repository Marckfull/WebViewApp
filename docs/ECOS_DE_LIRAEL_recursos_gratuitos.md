# Ecos de Lirael — Catálogo de Recursos Gratuitos (Sourcing Bible)

> Documento de **produção** que mapeia repositórios e packs **gratuitos e comercialmente
> utilizáveis** a cada seção do GDD *Ecos de Lirael*. Tratado como um estúdio AAA trataria
> seu *asset pipeline*: cada recurso tem função clara, licença verificada e ponto de entrada
> no cronograma (Fase 0 → 4).
>
> **Regra de ouro de produção:** nada entra no repositório sem a licença conferida e
> registrada. Ver seção 9 (Governança de Licenças). CC0 é sempre preferível — sem obrigação
> de atribuição, sem risco jurídico, sem "license rot".

---

## Índice

1. [Fundação — Engine e Projeto](#1-fundação--engine-e-projeto)
2. [Templates de Gameplay (o núcleo jogável)](#2-templates-de-gameplay-o-núcleo-jogável)
3. [Combate Souls-like](#3-combate-souls-like)
4. [Arte — Sprites, Tilesets e Personagens](#4-arte--sprites-tilesets-e-personagens)
5. [UI e Controles Touch (Android)](#5-ui-e-controles-touch-android)
6. [Áudio e Trilha Sonora (a Ocarina)](#6-áudio-e-trilha-sonora-a-ocarina)
7. [Sistemas — Diálogo, Save, Inventário, Quests](#7-sistemas--diálogo-save-inventário-quests)
8. [Pipeline, Ferramentas e CI/CD](#8-pipeline-ferramentas-e-cicd)
9. [Governança de Licenças](#9-governança-de-licenças)
10. [Plano de Montagem por Fase](#10-plano-de-montagem-por-fase)

---

## 1. Fundação — Engine e Projeto

Alinhado à **§6.1 do GDD** (Godot 4.x, GDScript, export Android nativo).

| Recurso | O que é | Licença | Onde entra |
|---|---|---|---|
| **Godot Engine** ([github.com/godotengine/godot](https://github.com/godotengine/godot)) | Engine base. Melhor pipeline 2D do mercado: pixel snapping, iluminação 2D em tempo real, TileMapLayer nativo, partículas, export AAB/APK de primeira classe. | MIT (sem royalties) | Fase 0 |
| **Godot Demo Projects** ([github.com/godotengine/godot-demo-projects](https://github.com/godotengine/godot-demo-projects)) | Coleção oficial: 2D platformer, top-down, iluminação 2D, partículas, GUI, salvamento. Referência canônica de "como fazer certo". | MIT | Fase 0 — estudo |
| **Awesome Godot** ([github.com/godotengine/awesome-godot](https://github.com/godotengine/awesome-godot)) | Índice curado de plugins, addons e recursos. Ponto de partida para descobrir tudo abaixo e mais. | Curadoria | Contínuo |

**Decisão de arquitetura (§6.1):** o repositório atual (WebViewApp, Java/WebView) **não** serve de
base para um jogo desse porte. O projeto Godot vive em `/game` neste repositório; o wrapper Android
é gerado pelo próprio export da engine (AAB para a Play Store).

---

## 2. Templates de Gameplay (o núcleo jogável)

Alinhado à **§3 (Gameplay)** e **§7 Fase 0/1**. Estes servem como *esqueleto de referência* —
estudar a arquitetura, não copiar-colar cegamente. O combate souls-like autoral (§3.2) é o
diferencial e será construído sobre estas bases.

| Recurso | Cobre do GDD | Licença | Nota de produção |
|---|---|---|---|
| **vmarnauza/godot-rpg** ([link](https://github.com/vmarnauza/godot-rpg)) | Movimento top-down 8-direções, câmera, interação com NPC — base do overworld estilo Zelda (§Câmera, §3.1). | MIT/aberta (conferir) | Melhor match de "Zelda-like em Godot 4". Base do protótipo de exploração. |
| **gdquest-demos/godot-open-rpg** ([link](https://github.com/gdquest-demos/godot-open-rpg)) | Arquitetura *data-driven* de RPG, estados, UI de menu, sistema de grid. GDQuest é referência de qualidade de código. | MIT | Estudar a **arquitetura data-driven** (§6.2) mais que o combate (o deles é por turnos). |
| **GitHub Topic: `action-rpg-game`** ([topic](https://github.com/topics/action-rpg-game)) e **`action-rpg` (GDScript)** ([topic](https://github.com/topics/action-rpg?l=gdscript)) | Dezenas de projetos ARPG em tempo real com hitbox/hurtbox, i-frames, knockback. | Variadas | Minerar padrões de **hitbox/hurtbox area-based** — o jeito idiomático em Godot. |

**Padrão-chave a extrair destes repos:** o sistema **Hitbox/Hurtbox via `Area2D`** — a espinha dorsal
de todo combate 2D em Godot (dano, i-frames de esquiva §3.2, parry, quebra de postura). É reutilizável
para Aria, os ~35 inimigos e os 12 bosses.

---

## 3. Combate Souls-like

O coração do jogo (**§3.2**): stamina, i-frames, parry, poise, "morte com peso", bonfires.

| Recurso | O que fornece | Licença | Uso |
|---|---|---|---|
| **wadlo/open-combat-engine** ([link](https://github.com/wadlo/open-combat-engine)) | Motor de combate open-source para Godot 4.x, com componentes de combate estilo ECS (composição sobre herança). | Open source (conferir no repo) | Referência de **arquitetura de componentes** para stats, dano, status. Escalável para o bestiário grande do GDD. |
| **goodai9/goodai9-combat-system** ([link](https://github.com/goodai9/goodai9-combat-system)) | Sistema enxuto de dar/receber dano para jogos 2D em Godot. | Open source (conferir) | Ponto de partida rápido para o *greybox* da Fase 0 (dano + morte + respawn). |
| **GitHub Topic: `souls-like`** ([topic](https://github.com/topics/souls-like)) | Ecossistema de projetos souls-like (a maioria 3D, mas os padrões de **stamina, lock-on, poise e "recuperar almas no local da morte"** são idênticos). | Variadas | Estudar a lógica de **stamina + lock-on + drop de moeda ao morrer** (Ecos, §3.2). |

### Peças autorais a construir sobre estas bases (não há atalho gratuito 1:1 — é o diferencial do jogo)

- **Barra de stamina** que governa ataque/esquiva/defesa (§3.2). Regen rápido fora de ação.
- **Esquiva com i-frames curtos** — implementada desligando a Hurtbox por N frames.
- **Parry** — janela apertada, ajustável por dificuldade (Balada/Canção/Requiem, §3.4).
- **Poise/postura** dos inimigos → atordoamento → finalização.
- **Lock-on / "Z-targeting" para touch** (§3.1) — o item **mais crítico e mais difícil**. É o
  *gate* da Fase 0: toque no inimigo para travar/trocar alvo. **Prototipar primeiro de tudo.**
- **Santuários (bonfires)**: descanso, save, respawn de inimigos comuns, fast-travel por melodia,
  gasto de Ecos em atributos.

> **Nota de risco (§7):** "combate touch não ficar bom" é o risco nº 1. Por isso a Fase 0 é um
> *gate* real. Nenhum template gratuito resolve isso — eles resolvem o *encanamento* (dano, stamina,
> lock-on genérico) e liberam tempo para você focar no *feel* touch, que é autoral.

---

## 4. Arte — Sprites, Tilesets e Personagens

Alinhado à **§4 (Direção de Arte)**: HD pixel art, tiles 32×32, sprites 48×48, iluminação 2D,
paralaxe, paleta que conta a história (mundo são quente / Silêncio dessaturado).

### 4.1 Pacote completo estilo Zelda (a espinha dorsal visual do protótipo)

| Recurso | Conteúdo | Licença | Uso |
|---|---|---|---|
| **Ninja Adventure Asset Pack** — pixel-boy ([itch.io](https://pixel-boy.itch.io/ninja-adventure-asset-pack)) | Pack top-down **completíssimo**: personagens, inimigos, tilesets, HUD, itens, efeitos, e **até música/SFX**. Perfeito para *vertical slice*. | **CC0** ✅ | **Recurso nº 1 para greybox e Fase 1.** Permite montar a vila + 1ª dungeon sem esperar arte final. |
| **Mystic Woods** — 16×16 ([itch.io](https://itch.io/game-assets/assets-cc0/tag-top-down)) | Pack top-down "cute", floresta/vila. | CC0 (conferir versão) | Referência para a **Floresta Sussurrante** (§Ato 2). |
| **Tiny RPG Fantasy / Ansimuz Legacy Collection** (via listagem CC0 top-down do itch.io [link](https://itch.io/game-assets/assets-cc0/tag-top-down)) | Coleções fantasy CC0. | CC0 | Preenchimento de overworld e props. |

### 4.2 Tilesets modulares (overworld, dungeons — ~40 tilesets no GDD)

| Recurso | Conteúdo | Licença | Uso |
|---|---|---|---|
| **Pipoya RPG Tileset 32×32** ([itch.io](https://pipoya.itch.io/pipoya-rpg-tileset-32x32)) | Outdoor + indoor 32×32, exatamente a grade do GDD. Livre para usar e editar. | Free (conferir termos) | Base modular do overworld — encaixa na filosofia "tiles modulares" (§7 mitigação de escopo). |
| **Dungeon Crawl 32×32 Tiles** ([OpenGameArt](https://opengameart.org/content/dungeon-crawl-32x32-tiles)) | **+3000 tiles**: terreno, paredes, features de dungeon, monstros, efeitos, itens, GUI. Do Dungeon Crawl Stone Soup. | **CC0** ✅ | Mina gigantesca para as 5 dungeons (Cripta, Forja Afundada, Necrópole de Sal…). |
| **32×32 Dungeon Tileset** ([OpenGameArt](https://opengameart.org/content/32x32-dungeon-tileset)) | Tileset de dungeon enxuto. | **CC0** ✅ | Blockout rápido de dungeons na Fase 0/1. |
| **DENZI / Tilesets e Backgrounds (PixelArt)** ([OpenGameArt](https://opengameart.org/content/tilesets-and-backgrounds-pixelart)) | Tilesets ortogonais 32×32 clássicos. | CC-BY/CC0 (varia) | Variedade de biomas. |

### 4.3 Coleções curadas (para não garimpar no escuro)

| Recurso | O que é | Uso |
|---|---|---|
| **Kenney Assets** ([kenney.nl/assets](https://www.kenney.nl/assets) · [1-Bit Pack](https://kenney-assets.itch.io/1-bit-pack)) | Milhares de assets **CC0** consistentes (2D, UI, ícones, fontes, áudio). O 1-Bit Pack tem +1000 tiles/chars/itens para RPG. | **CC0** ✅ — base de UI, ícones (§4: ~200 ícones de item) e prototipagem. |
| **CC0/OGA-BY Pixel Art (coleção)** ([OpenGameArt](https://opengameart.org/content/cc0oga-by-pixel-art)) | Curadoria de pixel art livre de OpenGameArt. | Descoberta rápida de sprites licenciados. |
| **Gist "Gamedev free assets"** ([gist](https://gist.github.com/UkoeHB/9991c1a60e887e448800ed2f740a037a)) | Lista longa e mantida de fontes de assets gratuitos. | Índice-mestre de sourcing. |

> **Estratégia de arte de produção (§4 + §7):** use os packs CC0 acima como **greybox visual** e
> **vertical slice** para validar gameplay sem travar no artista. A **arte final autoral** (a
> identidade de Aria, 8 direções, a paleta "cor conta a história") é feita pelo artista da equipe —
> os packs cobrem inimigos secundários, props, tiles modulares e ícones, liberando o artista para o
> que importa: protagonista, bosses e telas de cutscene. Isso materializa a mitigação de escopo do
> GDD (reuso inteligente, tiles modulares, remix de inimigos).

---

## 5. UI e Controles Touch (Android)

Alinhado à **§3.1** (stick virtual, botões customizáveis, canhoto, lock-on touch) e **§3.6**
(acessibilidade, remapeamento total).

| Recurso | O que fornece | Licença | Uso |
|---|---|---|---|
| **MarcoFazioRandom/Virtual-Joystick-Godot** ([link](https://github.com/MarcoFazioRandom/Virtual-Joystick-Godot)) | Joystick virtual maduro e popular. Modos **Fixed** e **Dynamic** (aparece onde o dedo toca). | MIT | **Escolha principal** para o stick de movimento 8-direções analógico (§3.1). |
| **HubbleCommand/mobile_controls** ([link](https://github.com/HubbleCommand/mobile_controls)) | Coleção de controles mobile Godot 4.x (joystick flutuante + botões). | Open source | Botões de ação, gatilhos de escudo/parry, roda da ocarina. |
| **Wesley-Source/versatile-mobile-joystick** ([link](https://github.com/Wesley-Source/versatile-mobile-joystick)) | Joystick altamente customizável (posição/escala). | Open source | Suporta o requisito "layout 100% customizável + canhoto" (§3.1). |
| **scottpetrovic/godot-mobile-example** ([link](https://github.com/scottpetrovic/godot-mobile-example)) | Exemplo de input unificado teclado/mouse/touch. | Open source | Suporte a **gamepad Bluetooth + touch** simultâneos (§3.1). Base de arquitetura de input. |

**Peça autoral:** a **roda de melodias da Ocarina** e o **mini-teclado de 5 notas** (§3.1, §6) — UI
própria, construída sobre o sistema de botões touch acima.

---

## 6. Áudio e Trilha Sonora (a Ocarina)

Alinhado à **§5**: música é **mecânica central**, não enfeite. ~25 faixas, 8 melodias de ocarina,
música adaptativa por camadas, leitmotiv da "Canção do Mundo".

| Recurso | Conteúdo | Licença | Uso |
|---|---|---|---|
| **CC0 Fantasy Music & Sounds** ([OpenGameArt](https://opengameart.org/content/cc0-fantasy-music-sounds)) | Temas de batalha, vila, orquestral fantasy, loops épicos. | **CC0** ✅ | Trilha *placeholder* para vertical slice; algumas faixas podem virar finais. |
| **CC0 Music (coleção)** ([OpenGameArt](https://opengameart.org/content/cc0-music-0)) · **CC0 Cinematic Music** ([link](https://opengameart.org/content/cc0-cinematic-music)) | Grandes coleções livres, incl. cinemático/dramático. | **CC0** ✅ | Cutscenes, temas de boss provisórios. |
| **Audio – Commercial use OK** ([OpenGameArt](https://opengameart.org/content/audio-commercial-use-ok)) | Peças orquestrais e loops liberados para uso comercial. | Comercial OK (conferir cada) | Overworld dia/noite. |
| **Kenney Audio** ([kenney.nl/assets](https://www.kenney.nl/assets)) | Packs de SFX CC0: impactos, UI, efeitos retrô. | **CC0** ✅ | **SFX de combate "crocante"** (§5 — feedback de hit é essencial para souls), UI, footsteps. |

> **Nota de produção (§5, §7):** música placeholder gratuita destrava a Fase 1, mas o **leitmotiv da
> "Canção do Mundo" é autoral e insubstituível** — precisa existir *antes* da vertical slice (é
> "próximo passo imediato" nº 5 do GDD). Contrate/defina o compositor cedo. Os packs CC0 cobrem SFX
> e faixas de ambientação secundária; os ~8 temas-chave e as 8 melodias de ocarina são compostos.
> O sistema de **música adaptativa por camadas** (instrumentos que somem perto do Silêncio) é código
> autoral usando os `AudioStreamPlayer` do Godot com bus por camada.

---

## 7. Sistemas — Diálogo, Save, Inventário, Quests

Alinhado à **§3.5** (inventário, mapa, side quests), **§3.6** (save-anywhere, bestiário) e **§6.2**
(data-driven, save JSON versionado, autosave).

| Recurso | Cobre do GDD | Licença | Uso |
|---|---|---|---|
| **nathanhoad/godot_dialogue_manager** ([link](https://github.com/nathanhoad/godot_dialogue_manager)) | Sistema de diálogo **não-linear** maduro: ramificações, *mutations* (fala com o estado do jogo), **localização gettext/CSV** (PT-BR + EN, §Alpha→Beta), efeitos de texto (pausas, velocidade). | MIT | **Padrão-ouro.** Cobre NPCs com rotinas, lore ambiental, Corvo mercador, diálogos opcionais (§2, §3.5). A localização nativa atende o requisito PT-BR + EN. |
| **gdquest-demos/godot-open-rpg** (de novo, [link](https://github.com/gdquest-demos/godot-open-rpg)) | Arquitetura **data-driven** de itens/menus (§6.2: itens, inimigos, quests, diálogos em Resources/JSON). | MIT | Modelo de como estruturar `Resource` customizados para armas, armaduras, amuletos, memórias. |
| **Godot Demo Projects → "Saving Games"** (em [godot-demo-projects](https://github.com/godotengine/godot-demo-projects)) | Padrão oficial de save. | MIT | Base do **save JSON versionado, escrita atômica, autosave** (§6.2) e save-anywhere suspenso (§3.6). |
| **Awesome Godot → seção Inventory/Quest** ([awesome-godot](https://github.com/godotengine/awesome-godot)) | Índice de addons de inventário (grade paginada) e quests. | Curadoria | Escolher addon de inventário para a grade por categoria (§3.5) ou usar de referência. |

**Peças autorais sobre estas bases:**
- **Mapa que se desenha ao explorar** (§3.5) — mecânica temática (Aria é cartógrafa). Autoral, usando
  `SubViewport` + fog-of-war.
- **Bestiário/diário preenchidos automaticamente** (§3.6) — flags data-driven disparadas por eventos.
- **Ciclo dia/noite acelerável por melodia** (§3.5) — shader de iluminação 2D + relógio de jogo.

---

## 8. Pipeline, Ferramentas e CI/CD

Alinhado à **§6.3** (testes unitários, CI Android automatizado a cada merge, telemetria).

| Recurso | Função | Licença | Uso |
|---|---|---|---|
| **Aseprite** ([github.com/aseprite/aseprite](https://github.com/aseprite/aseprite)) | Editor de pixel art padrão da indústria. **Código-fonte é gratuito** — pode compilar você mesmo sem custo (o binário pré-compilado é pago). | EULA (fonte compilável grátis) | Produção de todos os sprites/animações autorais (§4). |
| **LibreSprite** ([github.com/LibreSprite/LibreSprite](https://github.com/LibreSprite/LibreSprite)) | Fork 100% livre do Aseprite antigo. | Binário e fonte gratuitos | Alternativa sem compilar nada. |
| **Tiled Map Editor** ([github.com/mapeditor/tiled](https://github.com/mapeditor/tiled)) | Editor de mapas com tilesets. Importável no Godot. | GPL/BSD | Montar overworld e dungeons a partir dos tilesets modulares (§4). |
| **GUT — Godot Unit Test** ([github.com/bitwes/Gut](https://github.com/bitwes/Gut)) | Framework de testes unitários para Godot. | MIT | Cobre **§6.3**: testes de inventário, save, economia de stamina, quests. |
| **godot-ci** ([github.com/abarichello/godot-ci](https://github.com/abarichello/godot-ci)) | Imagem Docker + GitHub Actions para **export headless** do Godot. | MIT | Materializa o **CI Android a cada merge** (§6.3) — build AAB automatizado. |

> **CI concreto:** `godot-ci` + GitHub Actions = a cada push na branch de feature, um workflow roda o
> export headless e publica o AAB como artifact. É exatamente o "build Android automatizado a cada
> merge" do §6.3, de graça.

---

## 9. Governança de Licenças

**Regra AAA:** licença é risco jurídico. Trate como tal.

| Licença | Uso comercial | Atribuição | Veredito para o projeto |
|---|---|---|---|
| **CC0** | ✅ | Não exigida | **Preferencial.** Zero fricção. Kenney, Ninja Adventure, Dungeon Crawl, muitas faixas OGA. |
| **MIT / BSD / Apache** | ✅ | Aviso de copyright no crédito | ✅ OK para código (engine, addons, joystick, dialogue manager). |
| **CC-BY** | ✅ | **Obrigatória** — creditar o autor | ✅ OK, mas exige manter `CREDITS.md` rigoroso. Comum em OpenGameArt. |
| **CC-BY-SA** | ✅ | Obrigatória + *share-alike* | ⚠️ **Cuidado** — pode "contaminar" derivados. Evitar em assets modificados/mesclados. |
| **CC-NC (Non-Commercial)** | ❌ | — | 🚫 **Proibido** se o jogo for premium/monetizado. Filtrar sempre. |
| **GPL** | ✅ (ferramenta) | — | OK para **ferramentas** (Tiled). **Não** misturar código GPL no jogo se quiser fechar a fonte. |

**Processo obrigatório de produção:**
1. Todo asset que entra no repo tem sua origem + licença registrada em **`/game/CREDITS.md`** no
   mesmo commit.
2. Guardar o arquivo de licença original junto ao asset (`/game/assets/<pack>/LICENSE.txt`).
3. Conferir a licença **na página oficial do pack** antes de usar — "achei que era CC0" não é defesa.
   As buscas acima indicam a licença, mas a **página fonte é a autoridade**.
4. Rodar uma auditoria de licenças antes de cada milestone (fim de fase).

---

## 10. Plano de Montagem por Fase

Mapeando os recursos ao cronograma do **§7** do GDD.

### Fase 0 — Pré-produção (4 sem) · *Gate: o combate é divertido?*
- **Engine:** Godot 4.x + estudar `godot-demo-projects` e `vmarnauza/godot-rpg`.
- **Input:** integrar `MarcoFazioRandom/Virtual-Joystick-Godot` + `scottpetrovic/godot-mobile-example`.
- **Combate greybox:** montar Hitbox/Hurtbox + stamina + i-frames sobre `goodai9-combat-system` /
  `open-combat-engine`; **prototipar o lock-on touch primeiro** (risco nº 1).
- **Arte greybox:** **Ninja Adventure (CC0)** para Aria + 1 inimigo + 1 boss.
- **Áudio:** SFX de hit do **Kenney Audio (CC0)**.

### Fase 1 — Vertical Slice (8 sem) · *Gate: 30 min que representam o jogo*
- **Mundo:** Tiled + Pipoya/Dungeon Crawl tiles → vila de Pedra-Alva + Cripta das Guardiãs.
- **Diálogo/NPC:** `godot_dialogue_manager` (Corvo, tutorial diegético).
- **Save/Inventário:** padrão de save dos demos oficiais + arquitetura data-driven do `godot-open-rpg`.
- **Música:** 3 faixas — **leitmotiv autoral do compositor** (não placeholder) + 2 CC0 de ambientação.
- **Arte:** identidade final de Aria começa aqui (packs cobrem o resto).

### Fase 2 — Produção (24 sem)
- **Dungeons:** Dungeon Crawl (+3000 tiles CC0) alimenta Floresta Sussurrante, Forja Afundada, Torre
  dos Ventos, Necrópole de Sal. Remix de inimigos (Ninja Adventure + arte autoral).
- **Sistemas completos:** quests, mapa que se desenha, dia/noite, bestiário.
- **CI:** ligar `godot-ci` + GitHub Actions (build AAB a cada merge).
- **QA:** `GUT` cobrindo inventário/save/stamina/quests.

### Fase 3 — Alpha → Beta (8 sem)
- **Localização:** gettext/CSV do `dialogue_manager` → PT-BR nativo + EN.
- **Auditoria de licenças** completa (`CREDITS.md`) antes do content-complete.
- **Telemetria opt-in** e balanceamento por dados (§6.3).

### Fase 4 — Polimento e Lançamento (4 sem)
- Build assinada **AAB** via export Godot, page da Play Store, soft launch.

---

### Resumo de 1 linha por necessidade

| Necessidade | Recurso gratuito principal | Licença |
|---|---|---|
| Engine | **Godot 4.x** | MIT |
| Base Zelda-like | **vmarnauza/godot-rpg** | aberta |
| Combate | **open-combat-engine** + autoral | aberta |
| Arte (greybox→slice) | **Ninja Adventure Asset Pack** | **CC0** |
| Tiles de dungeon | **Dungeon Crawl 32×32** (+3000) | **CC0** |
| UI/ícones | **Kenney** | **CC0** |
| Joystick touch | **Virtual-Joystick-Godot** | MIT |
| Diálogo + i18n | **godot_dialogue_manager** | MIT |
| Música/SFX | **OpenGameArt CC0** + **Kenney Audio** | **CC0** |
| Editor de sprites | **LibreSprite** / Aseprite (fonte) | livre |
| Mapas | **Tiled** | GPL/BSD |
| Testes | **GUT** | MIT |
| CI Android | **godot-ci** | MIT |

---

## Fontes

- Godot Engine — https://github.com/godotengine/godot · https://github.com/godotengine/godot-demo-projects · https://github.com/godotengine/awesome-godot
- Templates gameplay — https://github.com/vmarnauza/godot-rpg · https://github.com/gdquest-demos/godot-open-rpg · https://github.com/topics/action-rpg-game
- Combate — https://github.com/wadlo/open-combat-engine · https://github.com/goodai9/goodai9-combat-system · https://github.com/topics/souls-like
- Arte — https://pixel-boy.itch.io/ninja-adventure-asset-pack · https://pipoya.itch.io/pipoya-rpg-tileset-32x32 · https://opengameart.org/content/dungeon-crawl-32x32-tiles · https://opengameart.org/content/32x32-dungeon-tileset · https://www.kenney.nl/assets · https://kenney-assets.itch.io/1-bit-pack · https://opengameart.org/content/cc0oga-by-pixel-art · https://gist.github.com/UkoeHB/9991c1a60e887e448800ed2f740a037a
- UI/Touch — https://github.com/MarcoFazioRandom/Virtual-Joystick-Godot · https://github.com/HubbleCommand/mobile_controls · https://github.com/Wesley-Source/versatile-mobile-joystick · https://github.com/scottpetrovic/godot-mobile-example
- Áudio — https://opengameart.org/content/cc0-fantasy-music-sounds · https://opengameart.org/content/cc0-music-0 · https://opengameart.org/content/cc0-cinematic-music · https://opengameart.org/content/audio-commercial-use-ok
- Sistemas — https://github.com/nathanhoad/godot_dialogue_manager
- Ferramentas/CI — https://github.com/aseprite/aseprite · https://github.com/LibreSprite/LibreSprite · https://github.com/mapeditor/tiled · https://github.com/bitwes/Gut · https://github.com/abarichello/godot-ci
