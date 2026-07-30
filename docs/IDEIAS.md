# 💡 Ideias para as próximas versões do NeuroFlip

Ordenadas por **impacto ÷ esforço**. As marcadas com 💰 mexem diretamente na receita.

---

## Prioridade alta (próxima release)

### 1. 🗓 Desafio Diário com semente fixa 💰
Todo mundo joga **exatamente o mesmo tabuleiro** do dia (a semente é a data), com
ranking local do mês e selo de dias completados. É a mecânica que mais gera
retorno diário em jogos casuais, e casa perfeitamente com o vídeo premiado
("tentativa extra assistindo um vídeo").
*Esforço: baixo — o `GameEngine` já aceita um `Random` com semente.*

### 2. 🎡 Roleta de recompensas 💰
Uma roleta com 8 fatias (neurônios, power-ups, tema temporário) girada 1× por dia
de graça, com **giro extra por vídeo premiado**. Animação de desaceleração é
simples de fazer com `Animatable` + easing.
*Esforço: baixo. Retorno: alto — é o formato de vídeo premiado com maior taxa de opt-in.*

### 3. 🎯 Missões semanais
"Faça 3 combos ×4", "termine 5 níveis sem errar", "use o ECO 20 vezes".
Recompensa em neurônios, com barra de progresso na tela inicial. Dá objetivo a
quem já zerou a campanha.
*Esforço: médio (precisa de contadores persistidos).*

### 4. 🧩 Passe de temporada gratuito 💰
30 níveis de recompensa por temporada de 30 dias. A trilha gratuita entrega
neurônios; a trilha "premium" é destravada **assistindo X vídeos** (em vez de
pagar). Monetiza sem colocar paywall.
*Esforço: médio-alto.*

---

## Mecânicas novas de jogo

### 5. ⚔️ Duelo Sináptico (1×1 local)
Dois jogadores no mesmo aparelho, alternando jogadas no mesmo tabuleiro. Quem
acerta joga de novo. A **Mutação** vira arma: quem erra provoca uma. Depois pode
virar online com Firebase (plano gratuito).

### 6. 🦠 Modo Corrupção
Uma carta "infectada" contamina uma vizinha a cada 3 jogadas. Se metade do
tabuleiro for infectada, você perde. Obriga a priorizar região do tabuleiro em
vez de só memorizar.

### 7. 🔗 Cadeia Sináptica
Bônus enorme por resolver pares **fisicamente adjacentes** em sequência,
desenhando uma linha de energia entre eles. Aprofunda a camada espacial que o
ECO já introduziu.

### 8. 🌀 Rotação do tabuleiro
Em níveis avançados, o tabuleiro inteiro gira 90° com animação. Você lembra do
símbolo, mas precisa recalcular a posição.

### 9. 👁 Modo Cego
As cartas ficam visíveis por 5 s no início e depois **o verso fica opaco**: sem
pulso, sem preview. Modo hardcore para quem domina o jogo.

### 10. 🎼 Modo Ritmo
As mutações acontecem no compasso da trilha (que já é gerada em BPM conhecido —
`MusicEngine` sabe exatamente onde está a batida). Virar carta no tempo dá bônus.

---

## Retenção e social

### 11. 🏆 Conquistas e Google Play Games
Login opcional, conquistas ("30 níveis com 3 estrelas", "combo ×8"), ranking
mundial de Blitz. SDK gratuito.

### 12. 📱 Widget de bônus diário
Widget na tela inicial mostrando o bônus disponível e a sequência de dias. Um
toque abre o app já na tela de resgate. Aumenta muito o retorno diário.

### 13. 🔔 Notificação de sequência
"Seu bônus de 5 dias expira em 4h." Só com permissão, no máximo 1 por dia.

### 14. 🤝 Compartilhar resultado
Cartão de resultado em imagem (estilo Wordle, com blocos ▪️🟦🟨) para colar em
grupos. Divulgação orgânica sem custo.

---

## Acessibilidade e polimento

### 15. ♿ Modo daltônico
Os símbolos já são emojis distintos (não dependem só de cor), mas vale um modo
com formas geométricas de alto contraste e rótulos.

### 16. 🔇 Modo silencioso inteligente
Detectar chamada/fone desconectado e pausar a trilha automaticamente.

### 17. 📐 Suporte a tablet e paisagem
O tabuleiro já é calculado por `BoxWithConstraints`; falta um layout em duas
colunas (HUD ao lado) para telas largas.

### 18. ☁️ Backup do progresso
Sincronizar `PlayerState` com o Google Drive App Data (gratuito) para não perder
progresso ao trocar de aparelho.

---

## Ajustes de monetização a testar 💰

- **Banner adaptativo** só na tela inicial e na loja (nunca durante a partida).
- **Rewarded interstitial** na transição de nível — formato opt-in que rende mais
  que o intersticial comum, mantendo o jogador no controle.
- **Oferta de estreia**: no 3º dia, "dobre seus neurônios assistindo 1 vídeo".
- **A/B do preço dos temas** (350 vs 500 neurônios) para calibrar quanto vídeo o
  jogador precisa assistir para desbloquear um tema.
- Medir **eCPM por posição** e cortar qualquer ponto que atrapalhe a retenção do
  dia 1 — retenção vale mais que impressão.
