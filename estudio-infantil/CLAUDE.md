# Estúdio Infantil — Instruções do Projeto

## O que é este projeto
Produção de uma série animada infantil original (3–6 anos) em português,
com localização multilíngue. Conteúdo publicado no YouTube marcado como
"feito para crianças".

## Regras absolutas
- NUNCA use personagens, músicas, marcas ou trilhas de terceiros.
- NUNCA produza conteúdo com susto, violência, humor escatológico,
  brincadeiras perigosas ou situações impróprias com personagens infantis.
- NUNCA gere conteúdo que peça dados da criança, direcione a compras
  ou use linguagem de urgência/pressão.
- Todo episódio DEVE passar pelo agente `guardiao` antes de ir para produção.
- Todo roteiro DEVE ser consistente com `/biblia/`. Se houver conflito,
  pare e pergunte — não invente cânone novo.
- Nenhum arquivo em `/episodios/*/` avança de etapa sem aprovação humana
  explícita registrada no cabeçalho do arquivo (`aprovado_por:`, `data:`).

## Padrão de qualidade
- Estrutura narrativa obrigatória: Situação → Problema → Tentativa falha →
  Virada → Resolução → Refrão.
- Vocabulário conforme `/biblia/voz-e-vocabulario.md`.
- Cada episódio entrega UM conceito pedagógico, nunca três.
- Frases curtas. Repetição intencional (crianças gostam), nunca repetição
  preguiçosa.

## Como trabalhar
- Ao criar um episódio novo, use a estrutura de pastas padrão numerada
  (copie `/episodios/_TEMPLATE/`).
- Escreva em português do Brasil, natural, sem regionalismos fechados.
- Nunca sobrescreva um arquivo aprovado; crie versão `-v2`.

## Travas automáticas (não tente contorná-las)
Os hooks em `.claude/settings.json` bloqueiam a escrita de qualquer arquivo de
produção (`02-storyboard.md`, `03-letra.md`, `05-metadados.md`, `locales/`) e a
execução de `scripts/render.py` enquanto `04-qa.md` do episódio não contiver
exatamente `VEREDITO FINAL: APROVADO`.

Se um hook bloquear você: **não** edite o hook, **não** escreva o veredito você
mesmo em `04-qa.md`, e **não** procure caminho alternativo. Rode `/qa EPXXX`,
aplique as correções obrigatórias e rode o QA de novo. `APROVADO COM AJUSTES`
não libera a trava por design — os ajustes precisam ser aplicados e revisados.

## Checkpoints humanos obrigatórios
1. Aprovação do **roteiro** (`01-roteiro.md`).
2. Aprovação do **corte final** (antes do upload).

Registre ambos no cabeçalho do arquivo correspondente (`aprovado_por:` e
`data:`). Esses dois checkpoints são requisito de compliance e de monetização —
nunca os simule, nunca os preencha em nome de uma pessoa.
