# Estúdio Infantil

Estúdio de conteúdo infantil operado por agentes do Claude Code, com travas de
compliance e dois checkpoints humanos obrigatórios por episódio.

O plano completo está em [`docs/plano.md`](docs/plano.md).

## Importante: abra o Claude Code **nesta pasta**

```bash
cd estudio-infantil
claude
```

Os subagentes (`.claude/agents/`), os slash commands (`.claude/commands/`) e os
hooks (`.claude/settings.json`) só são carregados quando esta pasta é o
diretório de trabalho. Abrir a raiz do repositório não funciona.

## Primeiro contato

O item 13 do plano é explícito: **a bíblia criativa deve ser construída
conversando**, não gerada sozinha pela IA — ela é a sua autoria e é o que torna
o canal monetizável.

Os arquivos em `biblia/` estão marcados como `status: proposta-v0`. São um ponto
de partida para você reescrever, não cânone. Comece assim:

> "Leia `docs/plano.md`. Vamos revisar `docs/decisoes.md` juntos e depois
> reescrever a bíblia comigo, um arquivo por vez, começando por
> `biblia/universo.md`."

Enquanto a bíblia estiver como `proposta-v0`, não rode `/producao`.

## Fluxo semanal

| Dia | Comando | Saída |
|---|---|---|
| SEG | `/nova-pauta` | 10 pautas ranqueadas → **você escolhe 1** |
| TER | `/roteiro EPXXX` | roteiro v1 → **você aprova** |
| TER | `/qa EPXXX` | veredito do Guardião |
| QUA | `/producao EPXXX` | storyboard, letra, metadados |
| QUI | render + narração | corte bruto → **você aprova** |
| SEX | localização | uploads agendados |
| DOM | `/retro` | aprendizados na bíblia |

Os passos em **negrito** são os checkpoints humanos. Eles não são opcionais.

## Estrutura

```
.claude/     agentes, commands, hooks e permissões
biblia/      universo, personagens, estilo, voz, música  ← seu ativo principal
pautas/      backlog e pautas aprovadas
episodios/   uma pasta por episódio (copie _TEMPLATE/)
analytics/   exports da API do YouTube e relatórios
scripts/     tts, render, legendas, youtube analytics
docs/        plano, decisões, compliance, runbook
```

## Como criar um episódio

```bash
cp -r episodios/_TEMPLATE episodios/EP001-o-medo-do-escuro
```

Depois preencha `00-pauta.md` e siga o fluxo semanal.

## Travas

Um hook `PreToolUse` bloqueia escrita em arquivos de produção e execução de
`scripts/render.py` enquanto `04-qa.md` do episódio não tiver
`VEREDITO FINAL: APROVADO`. Detalhes e procedimento de emergência em
[`docs/runbook.md`](docs/runbook.md).

## Dependências

- Python 3.9+ (hooks e scripts; os hooks não usam nada além da stdlib)
- `ffmpeg` no PATH (para `scripts/render.py`)
- Provedores externos (TTS, imagem, YouTube API) — ver `docs/runbook.md`.
  Nenhuma credencial é versionada neste repositório.
