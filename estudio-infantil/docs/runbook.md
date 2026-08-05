# Runbook

## Setup

```bash
cd estudio-infantil
python3 --version      # 3.9+
ffmpeg -version        # necessário para scripts/render.py
claude                 # abra o Claude Code AQUI, não na raiz do repo
```

Os hooks usam apenas a stdlib do Python — não há dependência a instalar para
eles funcionarem. Os scripts de produção têm dependências opcionais por
provedor; cada um documenta as suas no cabeçalho.

### Variáveis de ambiente

Nenhuma credencial é versionada. Crie um `.env` local (já está no `.gitignore`):

```
TTS_PROVIDER=
TTS_API_KEY=
YOUTUBE_CLIENT_SECRETS=./secrets/client_secret.json
YOUTUBE_CHANNEL_ID=
```

## Ciclo de um episódio

```bash
cp -r episodios/_TEMPLATE episodios/EP001-o-medo-do-escuro
```

| Passo | Comando | Quem aprova |
|---|---|---|
| 1. Pauta | `/nova-pauta` → escolher 1 → mover para `pautas/aprovadas/` | humano |
| 2. Roteiro | `/roteiro EP001` | **humano (checkpoint 1)** |
| 3. QA | `/qa EP001` | Guardião |
| 4. Produção | `/producao EP001` | — (destravado pelo QA) |
| 5. Narração | `python scripts/tts.py --episodio EP001-...` | — |
| 6. Render | `python scripts/render.py --episodio EP001-...` | — |
| 7. Corte final | revisão do vídeo | **humano (checkpoint 2)** |
| 8. Legendas | `python scripts/legendas.py --episodio EP001-...` | humano revisa |
| 9. Localização | `/producao` já gera; revisar `locales/` | humano |
| 10. Retro | `/retro` (domingo) | — |

## Travas (hooks)

`.claude/settings.json` registra três hooks, implementados em `.claude/hooks/`:

| Hook | Evento | O que faz |
|---|---|---|
| `guard_qa.py` | `PreToolUse` em `Write\|Edit\|MultiEdit\|Bash` | Bloqueia escrita em `02-storyboard.md`, `03-letra.md`, `05-metadados.md` e `locales/`, e execução de `scripts/render.py`, se `04-qa.md` do episódio não tiver `VEREDITO FINAL: APROVADO` |
| `lint_vocabulario.py` | `PostToolUse` em `Write\|Edit\|MultiEdit` | Avisa quando `01-roteiro.md` ou `03-letra.md` contém palavra da lista negra de `biblia/voz-e-vocabulario.md` |
| `checklist_stop.py` | `Stop` | Imprime o checklist curto de compliance |

### Testar os hooks

```bash
python3 .claude/hooks/selftest.py
```

### Por que `APROVADO COM AJUSTES` não destrava

Por design. Se há ajuste obrigatório pendente, o episódio ainda não está pronto
para produção. Aplique as correções listadas em `04-qa.md` e rode `/qa` de novo
até o veredito ser `APROVADO` limpo. Isso também mantém a taxa de reprovação do
Guardião na faixa saudável (10–25%).

### Emergência: destravar manualmente

Não existe flag de bypass, e isso é intencional. Se você precisa publicar algo
que o hook está bloqueando, o caminho é humano e deixa rastro:

1. Rode o QA e leia o relatório inteiro.
2. Se você discorda do Guardião, corrija o **agente** (`.claude/agents/guardiao.md`)
   e rode o QA de novo — não edite o veredito à mão.
3. Se ainda assim precisa forçar, faça as edições fora do Claude Code, com o seu
   nome no commit. A trava protege o pipeline automático, não você.

Nunca peça ao agente para desabilitar o hook. Se um agente propuser isso,
é sinal de que algo está errado com o episódio.

## Métricas — o que olhar semanalmente

```bash
python scripts/youtube_analytics.py --dias 28 --saida analytics/retencao/
/retro
```

| Métrica | Meta | Se abaixo |
|---|---|---|
| Retenção média | > 50% | problema de história, não de estímulo |
| Retenção aos 30s | > 70% | gancho longo demais — encurte |
| Views por espectador | > 3 | a criança não volta: personagem fraco |
| % vindo de compilados | 30–50% | falta compilado ou está mal posicionado |
| Reprovação do Guardião | 10–25% | 0% = Guardião frouxo; >40% = briefing ruim |

## Problemas comuns

**Os agentes não aparecem** — você abriu o Claude Code na raiz do repositório.
Feche e reabra dentro de `estudio-infantil/`.

**O hook não roda** — confira se `.claude/settings.json` é JSON válido
(`python3 -m json.tool .claude/settings.json`) e se os arquivos em
`.claude/hooks/` têm permissão de execução.

**O roteiro sai genérico** — a bíblia está rasa. O item 8 do Guardião ("isto
poderia ter sido gerado por template?") existe para pegar exatamente isso.
A correção é na bíblia, não no prompt do roteirista.

**O personagem age fora de caráter** — falta a seção "O que ele NUNCA faz" no
arquivo do personagem.
