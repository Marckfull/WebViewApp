# growth/ — ferramentas de aquisição do NeuroFlip

Ferramentas de apoio ao plano de crescimento do app **NeuroFlip**
(`com.neuroflip.game`). Não fazem parte do build do aplicativo Android —
rodam à parte, na máquina de quem opera o marketing ou em CI.

## `tools/content_factory.py` — fábrica de desafios

Motor do **sistema 05** do plano: gera procedural e infinitamente os criativos
diários que alimentam Reels, Shorts, TikTok, Stories e feed, já com legenda,
hashtags, CTA, resposta e calendário de publicação.

Seis tipos de desafio: Stroop (cor × palavra), achar o diferente, contagem,
sequência numérica, memória relâmpago (card em dois tempos, feito para vídeo)
e espelhamento de forma. Um baralho embaralhado garante que todos apareçam
antes de qualquer repetição, para a semana não virar quatro sequências seguidas.

Só usa a biblioteca padrão do Python 3. Saída em SVG — abre no navegador e
importa no Canva, Figma ou CapCut sem perda de qualidade.

```bash
# 30 dias de conteúdo em português, formatos story e feed
python3 growth/tools/content_factory.py --days 30 --start 2026-09-01

# uma semana em inglês, só story, com semente fixa (reprodutível)
python3 growth/tools/content_factory.py --days 7 --lang en --formats story --seed 42
```

Cada execução escreve em `growth/out/<data>-<idioma>/`:

| Arquivo | Para que serve |
| --- | --- |
| `index.html` | galeria de todas as peças, para revisar de uma vez |
| `*.svg` | os criativos, um por dia e por formato |
| `*-resposta.svg` | segundo card dos desafios de memória |
| `calendario.csv` | data, horário, plataforma, gancho, resposta e status |
| `legendas.md` | legenda pronta para copiar, com a resposta a fixar no comentário |

Formatos disponíveis em `--formats`: `story` (1080×1920, Reels/Shorts/TikTok/Stories),
`feed` (1080×1350, Instagram/Facebook/Pinterest) e `square` (1080×1080, X/LinkedIn).
No mesmo dia, todos os formatos trazem o mesmo desafio, apenas reenquadrado.

Os horários por plataforma em `SLOTS` são um ponto de partida — troque pelos
horários reais depois de duas semanas de dados do Insights de cada rede.
