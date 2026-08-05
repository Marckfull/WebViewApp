---
description: Gera storyboard, letra, metadados e localizações de um episódio aprovado
---

Produza o episódio $ARGUMENTS.

**Pré-condição:** `04-qa.md` do episódio deve terminar com
`VEREDITO FINAL: APROVADO`. Confira antes de qualquer coisa. Se não estiver
aprovado, pare e me diga o que falta — o hook vai bloquear você de qualquer
jeito, e insistir só gasta tempo.

Etapas, nesta ordem:

1. `storyboard` → `02-storyboard.md`
2. `letrista` → `03-letra.md`
3. Metadados → `05-metadados.md`: título (≤60 caracteres, sem clickbait, sem
   CAPS, sem nome de IP alheio), descrição (2 parágrafos + o conceito
   pedagógico explícito para o pai que lê), 10–15 tags, brief da thumbnail
   (composição, 2 cores dominantes, expressão do personagem — sem texto grande,
   sem seta, sem cara de choque).
4. `localizador` → `locales/{es,en,hi,id}/`

Ao final, apresente:
- resumo de produção do storyboard (planos, duração somada, assets novos);
- os metadados completos;
- o que precisa de revisão humana antes do upload (legendas, falante nativo,
  corte final).

Não faça upload. Não rode `scripts/render.py` sem eu pedir.
