---
status: proposta-v0
aprovado_por:
data:
---

# Estilo visual

Técnica: **2D flat**, traço limpo, sem contorno preto, texturas chapadas com
leve grão de papel. Nada de gradiente forte, nada de brilho especular, nada de
saturação estourada.

## Paleta

Máximo **2 cores dominantes por cena**, sempre uma quente + uma fria, sobre os
neutros.

| Papel | Cor | Hex |
|---|---|---|
| Neutro claro (fundo) | areia | `#F3EAD8` |
| Neutro médio | terra batida | `#D8C3A5` |
| Quente 1 | vermelho-tijolo (Tuca, Bravinho) | `#C25B4A` |
| Quente 2 | amarelo-mostarda (Janela, Nozinho) | `#E3B23C` |
| Fria 1 | verde-goiaba (folhagem, Nino) | `#6E9C6A` |
| Fria 2 | azul-portão (Portão, Chorinho) | `#5B7FA6` |
| Acento raro | roxo-claro (Medinho) | `#9B8AC4` |

Regra do acento: roxo só aparece quando Medinho está em cena. Cor com significado
fixo é o que faz a criança de 3 anos prever o que vem.

## Luz

- Sempre dia ou fim de tarde. **Nunca noite fechada.**
- Cena de "escuro" é resolvida com azul-portão dessaturado e a Janela Amarela
  acesa no quadro — nunca com preto. A regra existe por segurança, não por
  estética: escuro prolongado é o item 1 do Guardião.
- Sombras chapadas, uma direção só, 15% de opacidade.

## Câmera e corte

- Enquadramentos permitidos: `geral`, `médio`, `close`, `detalhe`.
- **Mínimo 3 segundos por plano. Corte abaixo de 2s é proibido.**
- Sem câmera subjetiva, sem plongée acentuada, sem zoom rápido, sem tremor.
- Movimento de câmera: só pan lento horizontal, no máximo um por cena.
- Transição padrão: corte simples. Fade só na entrada e na saída do episódio.

## Sufixo de estilo (obrigatório em TODO prompt de imagem)

Cole isto no fim de cada prompt visual do storyboard, sem alterar uma vírgula:

```
ilustração 2D flat, traço limpo sem contorno preto, cores chapadas com leve grão
de papel, paleta areia #F3EAD8 e terra #D8C3A5 com no máximo duas cores
dominantes, sombra chapada de 15% em uma direção só, iluminação de fim de tarde,
composição centrada e calma, estilo de livro ilustrado infantil, sem texto na
imagem
```

## Thumbnail

- Um personagem, expressão legível, olhando para o objeto do conflito.
- Duas cores dominantes, as mesmas da cena de abertura.
- **Sem** texto grande, seta, círculo vermelho, cara de choque ou boca aberta de
  espanto. Isso é sensacionalismo e é critério de baixa qualidade familiar.
- Testar em 120px de largura: se não dá pra saber quem é o personagem, refazer.

## Proibido

Cores neon ou fluorescentes; piscadas ou flashes; padrões que vibram (listras
finas de alto contraste); loop hipnótico; efeito de zoom pulsante; personagem
desaparecendo em corte; qualquer elemento que se mova mais rápido que a leitura
de uma criança de 3 anos.
