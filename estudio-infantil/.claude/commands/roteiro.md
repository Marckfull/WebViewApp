---
description: Escreve o roteiro v1 de um episódio a partir da pauta aprovada
---

Escreva o roteiro do episódio $ARGUMENTS.

1. Localize a pasta do episódio em `episodios/` (o argumento pode ser só o
   código, ex. `EP001`). Se não existir, pare e avise.
2. Confira que `00-pauta.md` está preenchida e aprovada. Se não estiver, pare.
3. Rode o subagente `roteirista`, que deve ler TODA a `biblia/` antes de escrever.
4. Escreva `01-roteiro.md` com `status: rascunho` e `aprovado_por:` **vazio**.

Ao terminar:
- mostre o roteiro completo aqui no chat para eu ler;
- liste os 3 riscos de abandono que o roteirista identificou;
- **pare**.

Não rode o QA, não gere storyboard, letra ou metadados. Este é o checkpoint
humano 1: eu leio, peço ajustes ou aprovo. Só depois que eu escrever meu nome em
`aprovado_por:` e mudar `status:` para `aprovado` é que `/qa` faz sentido.

Nunca preencha `aprovado_por:` você mesmo, em nenhuma circunstância — nem com o
meu nome, nem com "usuário", nem com um placeholder que pareça preenchido.
