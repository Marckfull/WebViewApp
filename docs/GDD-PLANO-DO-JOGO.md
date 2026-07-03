# ECOS DE LIRAEL — Plano Completo de Desenvolvimento (GDD + Plano Técnico)

> Jogo de ação-aventura single-player para Android, inspirado em *The Legend of Zelda: Ocarina of Time*, com pixel art detalhada (HD pixel art), elementos souls-like e protagonista feminina.

---

## 1. Visão Geral (High Concept)

| Item | Definição |
|---|---|
| **Título provisório** | Ecos de Lirael |
| **Gênero** | Ação-Aventura / Action-RPG com elementos souls-like |
| **Plataforma** | Android (mín. Android 8.0 / API 26), portrait bloqueado em landscape |
| **Câmera** | Top-down com leve perspectiva 3/4 (estilo Zelda 2D clássico, mas com iluminação dinâmica e paralaxe) |
| **Arte** | HD Pixel Art (sprites 32×32 a 64×64 para personagens, tiles 32×32, cenários com 3+ camadas de paralaxe, iluminação 2D em tempo real, partículas) — referências: *Eastward*, *CrossCode*, *Blasphemous* |
| **Duração alvo** | 6–8 horas na campanha principal (mínimo garantido: 5h), +2–3h de conteúdo opcional |
| **Modelo** | Single-player, offline, premium ou gratuito sem anúncios intrusivos (decisão de negócio posterior) |
| **Público** | 12+ (violência estilizada leve) |

### Pilares de design
1. **Exploração recompensadora** — cada tela esconde algo: recurso, atalho, segredo, lore.
2. **Combate justo e punitivo** — o souls-like: stamina, leitura de padrões, punição por erro, recompensa por maestria. Nunca injusto.
3. **Mundo vivo e coeso** — dungeons interligadas ao overworld, NPCs com rotinas, dia/noite.
4. **História contada pelo mundo** — cutscenes curtas + lore ambiental (itens, ruínas, diálogos opcionais).

---

## 2. História

### Sinopse
Séculos atrás, o reino de **Lirael** foi protegido por cinco **Guardiãs do Eco** — mulheres capazes de ouvir a "Canção do Mundo", a melodia que mantém a realidade coesa. Quando a última Guardiã desapareceu, o **Silêncio** começou a se espalhar: uma névoa que apaga sons, cores e memórias, transformando pessoas e criaturas em **Ecoados** — cascas violentas do que já foram.

A protagonista é **Aria**, uma jovem cartógrafa da vila de **Pedra-Alva**, surda de um ouvido desde criança. Quando o Silêncio engole sua vila e leva sua irmã mais nova, Aria descobre que seu "defeito" é na verdade um dom: o ouvido surdo escuta a Canção do Mundo. Ela é a herdeira relutante das Guardiãs.

### Estrutura narrativa (3 atos)
- **Ato 1 — O Despertar (≈1h30):** vila destruída, tutorial orgânico, primeira dungeon (Cripta das Guardiãs), Aria recebe a **Ocarina de Vidro** (instrumento central, homenagem direta a OoT — melodias desbloqueiam mecânicas: teleporte entre santuários, abrir portas seladas, mudar dia/noite, acalmar Ecoados).
- **Ato 2 — Os Cinco Ecos (≈3h30):** Aria precisa restaurar os 4 **Santuários do Eco** (4 dungeons temáticas: Floresta Sussurrante, Forja Afundada, Torre dos Ventos, Necrópole de Sal). Cada santuário revela um fragmento da verdade: a última Guardiã não desapareceu — ela **criou** o Silêncio para apagar uma dor insuportável.
- **Ato 3 — A Canção Final (≈1h30):** confronto no **Coração Mudo**. Dilema final com peso: destruir a Guardiã caída ou completar a canção dela (dois finais + um final secreto se o jogador coletou as 12 **Memórias Perdidas** espalhadas pelo mundo).

### Temas
Luto, memória, o valor do que consideramos "defeito", e o perigo de fugir da dor em vez de atravessá-la. A irmã sequestrada dá motivação pessoal imediata; a verdade sobre a Guardiã dá profundidade no fim.

### Personagens principais
| Personagem | Papel |
|---|---|
| **Aria** | Protagonista. Cartógrafa, prática, irônica, corajosa por necessidade. Evolui de "quero minha irmã de volta" para "entendo por que o mundo silenciou". |
| **Lys** | Irmã de Aria, 10 anos. MacGuffin emocional que ganha agência no Ato 3. |
| **Corvo** | Mercador errante misterioso (vendedor itinerante + entregador de lore, estilo o mercador de RE4 + a Coruja de OoT, porém menos falastrão). |
| **A Guardiã Caída (Selene)** | Antagonista trágica. Não é "o mal" — é a dor sem tratamento. |
| **Mestra Odara** | Ferreira anciã, mentora de upgrades de equipamento. |

---

## 3. Gameplay

### 3.1 Controles (touch, com suporte a gamepad Bluetooth)
- **Stick virtual esquerdo:** movimento (8 direções, analógico).
- **Botões direita:** Ataque / Esquiva-rolamento / Interagir / Item ativo.
- **Gatilhos virtuais:** defesa com escudo (segurar) e parry (toque no tempo certo).
- **Botão de ocarina:** abre roda de melodias.
- **Auto-lock-on opcional** no inimigo mais próximo (essencial para touch; toque no inimigo para trocar de alvo) — o "Z-targeting" de OoT adaptado a 2D/touch.
- Layout dos botões 100% customizável (posição e escala) + opção de canhoto.

### 3.2 Combate (o núcleo souls-like)
- **Stamina:** ataques, esquivas e defesa consomem stamina; gerenciá-la é o coração do combate. Regenera rápido fora de ação.
- **Esquiva com i-frames** curtos; rolar na direção certa atravessa ataques.
- **Parry:** janela apertada (ajustável por dificuldade) que abre o inimigo para crítico.
- **Postura (poise) dos inimigos:** golpes pesados quebram postura → atordoamento → finalização.
- **Morte com peso:** ao morrer, Aria dropa seus **Ecos** (moeda de XP/compra) no local da morte. Uma chance de recuperar; morrer de novo antes = perde tudo. Inimigos comuns renascem ao descansar.
- **Santuários (bonfires):** pontos de descanso que restauram vida/frascos, salvam o jogo, permitem viajar (via melodia da ocarina) e gastar Ecos em atributos.
- **Frascos de Essência:** cura limitada e recarregável nos santuários (estilo Estus), upgradável.
- **Bosses:** 8 bosses principais + 4 opcionais. Padrões legíveis, 2 fases, sem apelação de câmera. Cada dungeon-boss exige o item/mecânica da própria dungeon (filosofia Zelda) **e** leitura de timing (filosofia Souls).

### 3.3 Progressão
- **Atributos (gastos em santuários):** Vitalidade, Stamina, Força, Destreza, Harmonia (poder das melodias).
- **Equipamento:** 6 armas (adaga dupla, espada+escudo, lança, martelo, arco, chicote-corda — cada uma muda o moveset), 4 conjuntos de armadura, amuletos com efeitos passivos (slots limitados → build).
- **Upgrade de arma** na forja da Mestra Odara usando recursos coletados.
- **Itens de dungeon (estilo Zelda):** Gancho-corda (grappling), Bomba de Eco, Lente da Verdade (revela invisíveis), Botas de Corrente (andar sob água/ímã). Cada um abre áreas novas no overworld (metroidvania leve).

### 3.4 Dificuldade balanceada
- **3 modos:** *Balada* (casual: mais stamina, parry generoso, mantém Ecos ao morrer), *Canção* (padrão, a experiência pretendida), *Requiem* (souls puro: menos frascos, inimigos com novos golpes).
- Curva calibrada por playtest: cada boss deve matar o jogador mediano 2–4 vezes no modo padrão, nunca mais de 8.
- Sem grind obrigatório: jogador habilidoso zera "de nível 1" (run de desafio viável).

### 3.5 Sistemas de mundo
- **Inventário:** grade paginada por categoria (armas / armaduras / consumíveis / recursos / itens-chave / memórias), com descrição rica em lore em cada item. Acesso rápido a 4 slots de consumível no HUD.
- **Mapa:** Aria é cartógrafa — o mapa **se desenha conforme ela explora** (mecânica temática!). Pins manuais do jogador, marcação automática de santuários/segredos descobertos, mini-mapa opcional no HUD.
- **Recursos coletáveis:** minérios (3 tiers), madeiras, ervas (poções na alquimista), fragmentos de eco (upgrade de frasco), cogumelos/peixes/insetos (comércio e side quests). Nódulos renascem com o ciclo de descanso.
- **Ciclo dia/noite** (acelerável por melodia): inimigos diferentes à noite, lojas fecham, certos segredos só aparecem no escuro.
- **Side quests (12–15):** com histórias fechadas e recompensas únicas (nada de "mate 10 ratos" sem contexto).
- **Fast travel** entre santuários via melodia (desbloqueado no fim do Ato 1).

### 3.6 O que mais um jogo desse tipo precisa (adições minhas)
- **Save automático + 3 slots manuais**, save-anywhere suspenso (mobile: o jogador é interrompido o tempo todo — retomar exatamente onde parou é obrigatório).
- **Acessibilidade:** remapeamento total, tamanho de fonte, daltonismo, vibração opcional, indicadores visuais de áudio (a própria Aria é parcialmente surda — o jogo deve ser exemplar nisso: ataques fora da tela têm indicador visual).
- **Sessões curtas por design:** dungeons divididas em alas de 10–15 min com checkpoints; nada exige 40 min contínuos.
- **Bestiário e diário de lore** preenchidos automaticamente.
- **New Game+** com remix de inimigos (barato de produzir, dobra a longevidade).
- **Tutorial diegético:** nada de paredes de texto; a vila do Ato 1 ensina jogando.
- **Economia de bateria:** cap de 30/60 FPS selecionável, modo economia.

---

## 4. Direção de Arte

- **Resolução interna:** 640×360 (16:9), escalada por inteiro com barras adaptativas para telas 20:9 (área extra mostra mais cenário, nunca corta gameplay).
- **Paleta:** mundo "são" em tons quentes saturados; áreas do Silêncio dessaturadas com azul-cinza — a cor conta a história.
- **Personagens:** sprites 48×48 base, 8 direções para Aria, 4–8 frames por ação (idle, andar, correr, rolar, 3 ataques por arma, parry, dano, morte, ocarina).
- **Cenários:** tiles 32×32 com overlays decorativos grandes; 3 camadas de paralaxe em exteriores; iluminação 2D (tochas, dia/noite, sombras suaves); partículas (folhas, poeira, névoa do Silêncio, brasas).
- **UI:** pixel art consistente, ícones 24×24, fontes bitmap com acentuação PT-BR completa.
- **Estimativa de assets:** ~40 tilesets, ~35 inimigos + 12 bosses (sprites), ~25 NPCs, ~200 ícones de item, ~30 telas de cutscene ilustradas.

---

## 5. Trilha Sonora e Áudio

A música é **mecânica central** (ocarina), então o áudio é prioridade, não enfeite.

- **Compositor dedicado** (ou banco + trilha original para temas-chave). Direção: orquestral-híbrido com instrumentos solo marcantes (ocarina real, violoncelo, harpa) sobre camadas eletrônicas sutis — memorável e "assobiável", como Koji Kondo.
- **Tema principal com leitmotiv**: a "Canção do Mundo" aparece fragmentada em todas as faixas e se completa no final. Payoff emocional garantido.
- **Música adaptativa:** camadas que entram/saem (exploração → combate → perto do Silêncio a música literalmente perde instrumentos até restar silêncio — tema = mecânica = som).
- **8 melodias de ocarina jogáveis** (mini-teclado de 5 notas, como OoT).
- ~25 faixas: overworld (dia/noite), 5 dungeons, 8 bosses, vila, temas de personagem, finais.
- SFX: footsteps por material, hit-feedback "crocante" (essencial para combate souls), vozes estilo murmúrio (sem dublagem completa — barato e charmoso).

---

## 6. Plano Técnico

### 6.1 Engine — decisão
**Recomendação: Godot 4.x (GDScript).** Motivos:
- Gratuita e sem royalties; export Android de primeira classe (APK/AAB).
- Melhor pipeline 2D do mercado para pixel art (pixel snapping, iluminação 2D, tilemap nativo, partículas).
- Leve no dispositivo (relevante para Android low-end).
- Alternativa: Unity (C#) se a equipe já domina — porém runtime maior e overhead 2D pior.
- **O repositório atual (WebViewApp Java/WebView) não serve de base** — jogo desse porte em WebView/HTML5 teria performance e distribuição ruins. O projeto Godot viverá neste repositório em `/game`, com o wrapper Android gerado pelo próprio export da engine.

### 6.2 Arquitetura de código
```
game/
├── core/           # game loop, save system, event bus, settings
├── entities/       # player, enemies (state machines), NPCs
│   ├── player/     # movimento, combate, stamina, inventário equipado
│   └── enemies/    # IA por behavior tree simples + padrões de boss
├── systems/        # inventário, mapa, quests, dia-noite, música adaptativa
├── world/          # cenas de overworld (chunks), dungeons, santuários
├── ui/             # HUD, menus, diálogo, roda de melodias
├── data/           # Resources: itens, inimigos, quests, diálogos (tudo data-driven)
└── audio/          # gerenciador de camadas musicais, SFX pooling
```
- **Data-driven:** itens, inimigos, quests e diálogos em Resources/JSON — designers ajustam sem tocar código.
- **Save:** JSON versionado + criptografia leve, escrita atômica, autosave em eventos-chave.
- **Streaming de mundo:** overworld em chunks carregados por proximidade (memória de celular é curta).
- **Performance alvo:** 60 FPS em um Moto G moderno (Snapdragon 6xx), 30 FPS estável em low-end; RAM < 700 MB; APK < 300 MB.

### 6.3 Controle de qualidade
- Testes unitários para sistemas puros (inventário, save, economia de stamina, quests).
- Cena de "gym" de combate para tuning de cada inimigo isolado.
- Telemetria opt-in de playtest (onde morrem, onde travam, tempo por área) para balancear a dificuldade com dados.
- CI: build Android automatizado a cada merge (GitHub Actions + export headless do Godot).

---

## 7. Escopo e Cronograma (equipe pequena: 1 dev + 1 artista + 1 compositor part-time)

| Fase | Duração | Entregas |
|---|---|---|
| **0. Pré-produção** | 4 sem | GDD fechado (este doc), protótipo de combate em greybox: mover/atacar/rolar/stamina/1 inimigo/1 boss. **Gate: o combate é divertido?** |
| **1. Vertical Slice** | 8 sem | Ato 1 completo com arte final: vila, 1ª dungeon, 1º boss, inventário, save, mapa, 2 melodias, 3 faixas de música. **Gate: 30 min que representam o jogo inteiro.** |
| **2. Produção** | 24 sem | Atos 2 e 3, 4 dungeons, todos os sistemas, todos os bosses, side quests, trilha completa. Playtests quinzenais. |
| **3. Alpha → Beta** | 8 sem | Content-complete → balanceamento com telemetria, otimização de devices low-end, acessibilidade, localização (PT-BR nativo + EN). |
| **4. Polimento e Lançamento** | 4 sem | Bugfix, page da Play Store, build assinada AAB, soft launch. |
| **Total** | **~12 meses** | |

**Regra de corte de escopo** (se o prazo apertar, cortar nesta ordem): 4º final secreto → New Game+ → 2 bosses opcionais → 1 dungeon opcional. **Nunca cortar:** polimento do combate, música, o dilema final.

### Riscos principais
1. **Escopo** — 5h+ de conteúdo é MUITO para equipe pequena → mitigação: vertical slice cedo, corte planejado, reuso inteligente de assets (remix de inimigos, tiles modulares).
2. **Combate touch não ficar bom** → mitigação: é a primeira coisa prototipada (Fase 0 é um gate real).
3. **Performance em low-end** → mitigação: resolução interna baixa por design + testes em device fraco desde a Fase 1.

---

## 8. Próximos passos imediatos

1. Validar este GDD (você — ajustes de história, nome, escopo).
2. Instalar Godot 4.x e criar o projeto em `/game` neste repositório.
3. Fase 0: protótipo de combate greybox (mover, rolar, stamina, 1 inimigo) — ~2 semanas.
4. Definir a identidade visual de Aria (3 concepts de sprite para escolha).
5. Contratar/definir compositor cedo (o leitmotiv precisa existir antes da vertical slice).
