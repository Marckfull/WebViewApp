---
description: Analisa retenção e propõe aprendizados para a bíblia
---

Rode o subagente `analista` sobre os dados em `analytics/retencao/`.

Se não houver export recente, rode antes:

```bash
python scripts/youtube_analytics.py --dias 28 --saida analytics/retencao/
```

Se o script falhar por falta de credencial, diga isso claramente e pare — não
invente números para preencher o relatório.

Saída: `analytics/relatorios/AAAA-MM-DD-retro.md`, com as 5 seções do agente
(curva, correlações, comparação, TOP 5 instruções, hipóteses de A/B).

Depois, **proponha** as edições na `biblia/` que os dados sustentam: arquivo,
trecho atual, trecho novo, e o dado que justifica cada uma. Não aplique nenhuma
edição na bíblia sem eu aprovar — a bíblia é autoria, não output de métrica.

Fecha com uma leitura honesta das metas: retenção média (>50%), retenção aos
30s (>70%), views por espectador (>3), % vindo de compilados (30–50%), taxa de
reprovação do Guardião (10–25%). Diga quais estão fora e o que isso significa.
