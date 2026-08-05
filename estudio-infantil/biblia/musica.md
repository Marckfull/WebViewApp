---
status: proposta-v0
aprovado_por:
data:
---

# Música da série

> **Atenção:** a música precisa ser **composta e licenciada por você**, com
> licença comercial documentada (item de `docs/compliance.md`). Nada de trilha
> de banco genérico sem direito a obra derivada, e nada que remeta a melodia
> existente. A letra abaixo é proposta; a melodia é trabalho humano.

## Identidade sonora

- Instrumentação: violão nylon, ukulele, xilofone, palmas, um chocalho.
  Sem bateria eletrônica, sem sintetizador, sem baixo forte.
- Andamento: 92 BPM. Tom maior. Nada acelera nunca.
- Volume: a música nunca cobre a fala. Sob diálogo, −18 dB.
- **Sem som súbito.** Nenhum efeito entra em corte seco. Toda entrada tem
  0,5s de fade.
- Cada Sentimentinho tem um som próprio de 1 segundo (ver
  `personagens/sentimentinhos.md`), sempre o mesmo, sempre antes de ele aparecer
  em quadro.

## Refrão FIXO da série — nunca alterar

O refrão é o ativo de reconhecimento da série. Ele é idêntico em todos os
episódios, palavra por palavra, nota por nota. O agente `letrista` escreve
apenas as estrofes.

```
Quando eu falo o nome,
ele fica pequenininho.
Não sumiu, não foi embora,
só ficou do tamanho certinho.

Sentimento é pra sentir,
sentimento é pra contar.
Se eu falo o que eu sinto,
fica leve de levar.
```

- 8 versos, ABAB + CDED, 6–8 sílabas por verso.
- Duração cantada: ~16 segundos. Encaixa no bloco 3:10–3:50 do roteiro, com as
  estrofes do episódio antes e depois.

## Estrofes do episódio

Escritas pelo `letrista`, uma antes e uma depois do refrão:
- 4 versos cada, ABAB, 6–8 sílabas;
- verbos no imperativo gentil;
- o conceito do episódio aparece 3 vezes ao todo (contando o refrão);
- vocabulário conforme `voz-e-vocabulario.md`.

## Refrão por idioma

O refrão traduzido também é ativo fixo de marca: escreve-se **uma vez por
idioma**, com aprovação humana, e depois não muda mais. O `localizador` não pode
improvisar refrão por episódio.

| Idioma | Status | Aprovado por |
|---|---|---|
| PT-BR | proposta-v0 | — |
| ES-LA | pendente | — |
| EN-US | pendente | — |
| HI | pendente | — |
| ID | pendente | — |

Regra da adaptação: mantenha o **sentido** ("nomear encolhe, esconder aumenta")
e o esquema de rima do idioma-alvo. Rima quebrada é pior que palavra diferente.

## Tema de abertura e encerramento

- Abertura: 5 segundos, instrumental, o mesmo em todo episódio.
- Encerramento: o refrão instrumental, baixinho, sob a ponte de 1 frase para o
  próximo episódio.
- Sem "vinheta de inscrição", sem cartela de call-to-action, sem tela final
  piscante.
