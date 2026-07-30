# Balanceamento

As metas da campanha não foram escolhidas no olho. O motor é Kotlin puro e
determinístico, então foi possível **jogar as 150 fases milhares de vezes** com
bots e usar os números para calibrar.

## Método

Dois bots, propositalmente diferentes:

- **Bot aleatório** — sorteia entre as jogadas legais. Serve de piso absoluto.
- **Bot com intenção** — joga *para os objetivos*: caminha essências uma na
  direção da outra, limpa embaixo dos prismoides, mira as casas com gelo e
  encosta nas pedras. É o proxy de "jogador competente". Um humano joga melhor
  que ele.

As metas ficam em torno de **60–75% do que o bot com intenção alcança**. A folga
existe porque o jogador real tem itens, vidas extras e a opção de jogadas extras
por vídeo premiado.

## O que a medição revelou

### 1. A mecânica principal quase não acontecia

Primeira rodada de medição, fusões por fase:

```
fases   1-10:  0,55      fases  51-90:  0,10
fases  11-25:  0,16      fases 91-150:  0,09
fases  26-50:  0,21
```

Essências vinham só de match de 4, que é raro — e ainda era preciso ter **duas**
vivas ao mesmo tempo e caminhar uma até a outra. A mecânica que dá nome ao jogo
era quase inalcançável.

**Correção:** match de 3 dentro de uma cascata também condensa essência.

Depois da correção, com o bot com intenção:

```
fases   1-10: 18,5       fases  51-90: 3,9
fases  11-25:  7,2       fases 91-150: 3,8
fases  26-50:  6,3
```

### 2. Havia fases impossíveis

O gerador pedia até **64 camadas de cristal** em tabuleiros que tinham 20, e
pedras acima da quantidade existente. Hoje as metas de gelo e pedra são
limitadas pela capacidade real da máscara, e há um teste que quebra se voltar a
acontecer.

### 3. Prismoides tinham orçamento errado de jogadas

Medindo, um prismoide leva **cerca de 30 jogadas** para atravessar o tabuleiro
de cima até a base. A fase dava 3 jogadas extras por prismoide pedido.

**Correções:** 14 jogadas extras por prismoide, e permissão para vários
descerem em paralelo (antes só um por vez, o que também tornava a fase
impossível se aquele único ficasse preso numa coluna morta).

### 4. Três objetivos por fase era demais

Com um orçamento fixo de jogadas, três metas dividem a atenção a ponto de nenhuma
fechar. Hoje só as fases-chefe pedem três.

## Curva final

Taxa de vitória do **bot com intenção**, 10 tentativas por fase:

| Bloco | Vitórias |
|---|---|
| fases 1-10 | 100% |
| fases 11-25 | 92% |
| fases 26-50 | 88% |
| fases 51-90 | 74% |
| fases 91-150 | 62% |

Lembrando: esse bot é o piso, não o teto. Ele não planeja mais de uma jogada à
frente, não usa itens e não guarda essências para uma fusão melhor.

## Fórmulas atuais

Rampas lineares da fase 1 até a 150 (`Campaign.buildObjectives`):

| Objetivo | Início | Fim |
|---|---|---|
| `SCORE` | 1.100 | 2.800 |
| `COLLECT` | 15 | 24 |
| `ICE` | 8 | 55% da capacidade da máscara |
| `STONE` | 2 | 5 (limitado pela máscara) |
| `FUSION` | 1 | 3 |
| `PRISMOID` | 1 | 2 |

Orçamento de jogadas (`Campaign.movesFor`): base 26, mais um acréscimo por
objetivo — 14 por prismoide, 3 por fusão, 6 por pedra, 5 por gelo, 3 por coleta —
menos `fase/15`, menos 2 nas fases-chefe, sempre entre 16 e 55.

Cores em jogo: 4 até a fase 10, 5 até a 40, 6 daí em diante. O salto importa
mais do que parece — com 6 cores os matches são bem mais raros, e foi o que fez a
pontuação média cair de ~7.900 para ~2.200 na primeira medição.

## Como refazer a medição

Os bots de calibração não vão no APK. Para reproduzir, compile o pacote `core`
(que não depende de Android) junto de um `main` que jogue as fases e imprima as
médias — foi assim que estes números saíram. Os testes em
`app/src/test/java/com/prisma/fusao/core/` guardam as invariantes que a medição
revelou, e é neles que uma regressão aparece.
