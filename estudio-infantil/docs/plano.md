# Estúdio Infantil Automatizado — Plano de Implantação no Claude Code

> Plano operacional completo para montar um estúdio de conteúdo infantil com
> agentes do Claude Code, desenhado para **passar** nas políticas do YouTube
> (conteúdo autêntico + qualidade familiar) em vez de tentar driblá-las.

---

## 0. Princípios inegociáveis do projeto

1. **IA é ferramenta, não autora.** Todo episódio passa por 2 aprovações humanas
   obrigatórias (roteiro e corte final).
2. **Propriedade autoral própria.** Personagens seus, universo seu, música sua.
   Zero personagens de terceiros.
3. **Retenção por história, não por estímulo.** Nada de cortes frenéticos,
   loops hipnóticos, cores estouradas ou "unboxing" infinito.
4. **AdSense é a menor fonte de receita.** Planeje marca, merch e produto desde
   o dia 1.
5. **Escala vem de idiomas e de séries, não de volume genérico.**

---

## 1. Decisões que você toma ANTES de abrir o Claude Code

| Decisão | Opções | Recomendação inicial |
|---|---|---|
| Faixa etária | 2–4 / 4–6 / 6–9 | **3–6 anos** |
| Formato base | Música / História / Educativo / Misto | **Série narrativa com refrão musical recorrente** |
| Duração | 3–5 min + compilado 45–60 min | Ambos |
| Estilo visual | 2D flat / 3D / stop-motion digital | **2D flat** |
| Idioma-mãe | PT-BR | PT-BR → ES → EN → HI → ID |
| Pilar pedagógico | Emoções / Números / Natureza / Rotina | **Emoções + rotina** |

**Ação:** escreva isso em `docs/decisoes.md`.

---

## 2. Estrutura do repositório

```
estudio-infantil/
├── CLAUDE.md
├── .claude/
│   ├── agents/
│   ├── commands/
│   └── settings.json
├── biblia/
│   ├── universo.md
│   ├── personagens/
│   ├── estilo-visual.md
│   ├── voz-e-vocabulario.md
│   └── musica.md
├── pautas/
│   ├── backlog.md
│   └── aprovadas/
├── episodios/
│   └── EPXXX-slug/
│       ├── 00-pauta.md
│       ├── 01-roteiro.md
│       ├── 02-storyboard.md
│       ├── 03-letra.md
│       ├── 04-qa.md
│       ├── 05-metadados.md
│       └── locales/
├── analytics/
│   ├── retencao/
│   └── relatorios/
├── scripts/
│   ├── tts.py
│   ├── render.py
│   ├── legendas.py
│   └── youtube_analytics.py
└── docs/
    ├── decisoes.md
    ├── compliance.md
    └── runbook.md
```

---

## 3. `CLAUDE.md`

Ver arquivo na raiz do estúdio.

---

## 4. A Bíblia Criativa (o ativo mais importante)

Sem isso, tudo que os agentes gerarem vai parecer genérico — exatamente o que
os classificadores de "conteúdo baseado em template" detectam.

**`biblia/universo.md`** deve responder:
- Onde se passa? (um lugar único e nomeável — não "uma floresta")
- Qual a regra mágica/lúdica desse mundo?
- Que tipo de problema acontece lá? (sempre emocional/cotidiano)
- O que NUNCA acontece nesse mundo? (lista explícita)

**`biblia/personagens/*.md`** — para cada personagem: idade aparente, traço
dominante, defeito adorável, bordão, como reage ao medo/frustração/alegria,
o que NUNCA faz, voz (tom, ritmo, altura — importante para o TTS).

**`biblia/voz-e-vocabulario.md`** — lista de palavras permitidas por faixa,
frases-modelo e uma lista negra.

---

## 5. Os 7 subagentes

`pesquisador-pauta`, `roteirista`, `guardiao`, `storyboard`, `letrista`,
`localizador`, `analista`. Ver `.claude/agents/`.

O `guardiao` é o mais importante: a função dele é REPROVAR. Falso positivo
custa barato, falso negativo custa o canal.

---

## 6. Slash commands

- `/nova-pauta` → chama `pesquisador-pauta`, atualiza o backlog.
- `/roteiro EPXXX` → chama `roteirista`, para e pede aprovação humana.
- `/qa EPXXX` → chama `guardiao`, bloqueia avanço se reprovado.
- `/producao EPXXX` → só roda com veredito APROVADO.
- `/retro` → chama `analista` e atualiza a bíblia com aprendizados.

---

## 7. Hooks — a trava automática

- **PreToolUse** em `Write`/`Edit`/`Bash` tocando arquivos de produção:
  bloqueia se não houver `VEREDITO FINAL: APROVADO` em `04-qa.md`.
- **PostToolUse** após escrita de roteiro: lint de vocabulário contra a lista
  negra.
- **Stop**: imprime o checklist de compliance no fim da sessão.

---

## 8. Integrações externas

| Função | Como conectar |
|---|---|
| Narração (TTS) | `scripts/tts.py` com provedor de licença comercial |
| Geração de imagem | ferramenta com direitos comerciais claros |
| Montagem/render | `ffmpeg` via `scripts/render.py` |
| Legendas | Whisper local ou script próprio, com revisão humana |
| YouTube Data + Analytics API | `scripts/youtube_analytics.py`, OAuth próprio |
| Servidor MCP | opcional |

---

## 9. Pipeline operacional

```
SEG  /nova-pauta            → 10 pautas          → [VOCÊ escolhe 1]  ← humano
TER  /roteiro EPXXX         → roteiro v1         → [VOCÊ revisa e aprova]  ← humano
TER  /qa EPXXX              → veredito           → correções
QUA  /producao EPXXX        → storyboard, letra, metadados
QUI  render + narração      → corte bruto        → [VOCÊ revisa o corte]  ← humano
SEX  localização 4 idiomas  → uploads agendados
DOM  /retro                 → aprendizados na bíblia
```

Capacidade realista: **1 episódio/semana × 5 idiomas**, mais 1 compilado mensal
por idioma.

---

## 10. Métricas que importam

| Métrica | Meta |
|---|---|
| Retenção média | > 50% |
| Retenção aos 30s | > 70% |
| Views por espectador | > 3 |
| % vindo de compilados | 30–50% |
| Taxa de reprovação do Guardião | 10–25% |

Ignore: número de vídeos publicados.

---

## 11. Checklist de compliance

Ver `docs/compliance.md`.

---

## 12. Roadmap de 90 dias

- **Dias 1–15 — Fundação (sem publicar).** Decisões, bíblia completa, repositório,
  agentes, hooks. 1 episódio-piloto à mão para calibrar.
- **Dias 16–45 — Banco de 8 episódios.** Sem publicar (ou 2 não listados).
- **Dias 46–75 — Lançamento.** 8 episódios em 2 semanas, depois 1/semana.
  Primeiro compilado. Localização ES.
- **Dias 76–90 — Loop de dados.** `/retro` semanal, A/B de thumbnail e gancho,
  avaliar EN, iniciar canal paralelo para PAIS.

---

## 13. Como começar hoje

Abra o Claude Code na pasta `estudio-infantil/` e peça:

> "Leia o plano em docs/plano.md. Vamos começar pela seção 1: me faça as
> perguntas necessárias para preencher docs/decisoes.md e depois monte a
> bíblia criativa comigo, um arquivo por vez. Não crie os agentes ainda."

Construa a bíblia **conversando**, não deixando a IA inventar sozinha.
