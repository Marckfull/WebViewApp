# Mecânica do PRISMA

## Por que não é "mais um Candy Crush"

O gênero inteiro resolveu o match de 4+ da mesma forma: ele vira uma peça
especial que **explode**. Listrado, embrulhado, bomba de cor — muda o formato do
estouro, mas a decisão do jogador é sempre a mesma: *criar e detonar*.

O PRISMA quebra esse laço em dois. A peça criada por um match grande — a
**Essência** — não explode. Ela **fica**, e ela **anda**.

Isso cria uma camada de planejamento que não existe nos concorrentes:

1. Você faz um match de 4 e ganha uma essência.
2. A essência não faz nada sozinha. Ela até atrapalha: não forma match por cor,
   então é um buraco morto no tabuleiro.
3. Para ela valer alguma coisa, você precisa de **outra** essência.
4. E precisa **levar uma até a outra**, uma casa por jogada.

O jogo deixa de ser sobre a jogada atual e passa a ser sobre um projeto de duas
ou três jogadas.

---

## Regras completas

### Peças

| Peça | Como aparece | Comportamento |
|---|---|---|
| **Gema** (6 cores) | Preenchimento normal | Única que forma match por cor |
| **Essência** | Match de 4, L/T, ou match de 3 dentro de cascata | Não estoura ao ser movida; troca livre com qualquer vizinha |
| **Prisma** (incolor) | Fusão de duas essências de **cores diferentes** | Ao ser trocada: limpa linha + coluna + braços diagonais |
| **Supernova** (colorida) | Fusão de duas essências da **mesma cor** | Ao ser trocada: limpa toda aquela cor + área 5x5 |
| **Nova Cromática** | Fusão de duas peças de nível 2 encostadas | Ao ser trocada: **varre o tabuleiro inteiro** |
| **Prismoide** | Nasce no topo em fases que pedem | Cai com a gravidade, imune a estouros; precisa chegar à base |
| **Pedra** | Definida no formato da fase | Não cai nem troca; some depois de dois estouros vizinhos |

### Trocas

- Duas gemas comuns: **só** se a troca formar um match.
- Qualquer troca envolvendo peça especial: **sempre permitida**.
  - Essência ↔ vizinha: apenas reposiciona (custa uma jogada).
  - Prisma / Supernova / Nova ↔ vizinha: **detona**.
  - Especial ↔ especial de níveis diferentes: combo maior (feixes triplos, duas
    cores de uma vez). Do **mesmo** nível isso não chega a acontecer — eles se
    fundem antes de o jogador conseguir trocá-los.

### Fusão — a escada

Ao fim de cada etapa de cascata, duas peças **do mesmo nível** que estejam
encostadas se fundem automaticamente e sobem um degrau:

```
nivel 1   Essencia + Essencia   ->  Supernova (mesma cor) ou Prisma (cores diferentes)
nivel 2   Prisma/Supernova x2   ->  NOVA CROMATICA
nivel 3   (topo)                ->  nao funde com mais nada
```

A peça nova nasce na casa da peça mais recente — ou seja, naquela que o jogador
acabou de mover, que é onde ele está olhando.

**Por que o nível 3 é difícil de propósito.** Uma essência pode ser caminhada
casa a casa porque mover uma essência não a detona. Um prisma, não: trocá-lo com
qualquer vizinha o faz explodir. Logo, não existe "levar um prisma até o outro".
A única rota é: ter um nível 2 parado no tabuleiro e conseguir que uma fusão de
essências aconteça **exatamente na casa ao lado dele**. São três jogadas
encadeadas mais um pouco de sorte de cascata.

Medindo com bots que perseguem fusões, a Nova aparece em torno de **11% das
partidas** — rara o suficiente para ser um acontecimento, frequente o suficiente
para não ser folclore.

### Cascata

Toda a resolução de uma jogada acontece de uma vez no motor e depois é
reproduzida em quadros na tela. A ordem dentro de cada volta é proposital:

```
fusão  ->  match  ->  gravidade  ->  repete
```

Fusão vem primeiro para encostar duas essências responder **na hora**, sem
esperar o resto do tabuleiro assentar.

---

## Uma decisão que veio de medição, não de opinião

A primeira versão tinha só "match de 4 condensa essência". Medindo com um bot que
joga mirando os objetivos, o resultado foi **0,5 fusão por fase**. Ou seja: a
mecânica que define o jogo praticamente não acontecia.

A correção foi fazer com que **match de 3 dentro de uma cascata também condense
essência**. Isso:

- multiplicou a oferta de essências por ~7 (de 0,5 para 3,8–18 por fase,
  dependendo do número de cores);
- transformou reação em cadeia de "sorte bonita" em **fonte de recurso**;
- abriu uma diferença enorme entre jogar no automático e jogar com intenção — um
  bot aleatório faz ~0,4 fusões por fase, um bot que persegue fusões faz ~18 nas
  fases iniciais. Habilidade importa, que é o que se quer num puzzle.

Os números estão em [BALANCEAMENTO.md](BALANCEAMENTO.md).

---

## Objetivos das fases

| Tipo | Descrição |
|---|---|
| `SCORE` | Alcançar uma pontuação (toda fase tem, para o cálculo de estrelas) |
| `COLLECT` | Coletar N gemas de uma cor |
| `ICE` | Quebrar N camadas de cristal |
| `FUSION` | Realizar N fusões — o objetivo que ensina a mecânica |
| `PRISMOID` | Levar N prismoides até a base |
| `STONE` | Destruir N blocos de pedra |

Metas de gelo e pedra são **limitadas pela capacidade real do tabuleiro**. Sem
essa trava, o gerador chegou a pedir 64 camadas de cristal num tabuleiro que só
tinha 20 — uma fase impossível. Hoje existe um teste que falha se isso voltar.

---

## Pontuação

| Evento | Pontos |
|---|---|
| Gema comum | 60 |
| Essência estourada | 120 |
| Prisma | 220 |
| Supernova | 260 |
| Pedra | 90 |
| Camada de cristal | 30 |
| Nova Cromática | 400 |
| **Fusão (nível 1)** | **400** |
| **Fusão (Nova Cromática)** | **1.500** |
| **Prismoide entregue** | **500** |

Tudo dentro de uma cascata é multiplicado por `1 + 0,5 × profundidade`, então o
quarto elo de uma corrente vale 2,5 vezes o primeiro.
