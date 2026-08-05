---
description: Roda o QA obrigatório do Guardião em um episódio
---

Rode o subagente `guardiao` sobre o episódio $ARGUMENTS.

Leia `01-roteiro.md` e toda a `/biblia/`.
Escreva o relatório em `04-qa.md`.

Se o veredito for REPROVADO, NÃO prossiga para nenhuma etapa seguinte
e me avise com o motivo em uma frase.

Se for APROVADO COM AJUSTES: liste as correções obrigatórias, aplique-as no
roteiro (criando `01-roteiro-v2.md`, sem sobrescrever o aprovado) e rode o
Guardião de novo. A trava de produção só abre com `VEREDITO FINAL: APROVADO`
limpo — isso é intencional, não tente contornar.

Ao final, informe: veredito, quantos itens ficaram OK / AJUSTAR / REPROVAR, e
qual é o próximo passo.
