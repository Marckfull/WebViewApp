# 🖨️ Sistema de Gestão para Gráfica

Sistema simples para gerenciar pedidos, materiais, clientes e pagamentos da sua gráfica.
Tudo em **um único arquivo** (`index.html`) — funciona **offline**, sem instalar nada.

## ✅ O que ele já faz

- **Materiais/Produtos**: cadastrar, editar e excluir (já vem com banner, adesivo, lona, cartão, panfleto etc.)
  - Cobrança por **m²** (usa largura × altura), por **unidade** ou por **milheiro**
- **Novo pedido**: escolher material, informar tamanho/medidas, quantidade, preço → calcula o total sozinho
- **Clientes**: número do cliente, nome, telefone, CPF/CNPJ, endereço, e-mail
- **Pagamentos**: lançar quanto pagou, como pagou (dinheiro, pix, cartão...), quando pagou, e mostra **quanto falta**
  - Status automático: **Em aberto / Parcial / Pago**
- **Produção**: marcar cada pedido como Orçamento → Em produção → Pronto → Entregue
- **Andamento**: ir adicionando detalhes com data conforme produz ("arte aprovada", "entrou na fila"...)
- **Recibo não fiscal**: botão de imprimir com todos os dados do pedido e pagamentos
- **Busca**: por número do pedido, por cliente ou por produto; filtros por status e produção
- **Painel**: total de pedidos, total vendido, recebido e a receber
- **Backup**: exportar/importar tudo em `.json` (para não perder e passar entre aparelhos)

## ▶️ Como usar

**Opção 1 — Direto no computador ou celular (grátis, offline):**
Abra o arquivo `index.html` no navegador (Chrome, Edge etc.). Pronto.
Os dados ficam salvos no próprio navegdor daquele aparelho.
> ⚠️ Faça **backup** de vez em quando (aba Config) e guarde o `.json`.

**Opção 2 — Dentro do seu app Android (WebView):**
Coloque este arquivo dentro do app e aponte o WebView para ele.

**Opção 3 — Online no Firebase (acessar de qualquer lugar):** veja abaixo.

---

## 💰 Local x Online — e quanto custa o Firebase?

### Resumo direto
Para **uma gráfica só sua**, o **Firebase é praticamente GRÁTIS**.

### Local (como está agora)
- **Custo: R$ 0.** Nada de mensalidade.
- Funciona offline, muito rápido.
- **Limitação:** os dados ficam **só naquele aparelho**. Se quiser usar em 2 lugares, precisa passar o backup manualmente.

### Online no Firebase Hosting (só hospedar o site)
- Colocar este `index.html` no ar para abrir de qualquer celular/PC pelo link.
- **Plano grátis (Spark):** 10 GB de armazenamento e ~360 MB de transferência por dia.
  - Este site tem ~40 KB. Você poderia abrir ele **milhares de vezes por dia** e não sairia do grátis.
- **Custo real esperado: R$ 0/mês.**
- Mas atenção: só hospedar **não sincroniza os dados** entre aparelhos (os dados continuam no navegador de cada um).

### Online com sincronização real (Firestore — dados na nuvem)
Se você quiser que os pedidos apareçam **iguais em todos os aparelhos** ao mesmo tempo, aí usa o banco **Firestore**.
- **Plano grátis (Spark) dá por mês, por dia:**
  - 50.000 leituras/dia, 20.000 gravações/dia, 1 GB de dados guardados.
  - Uma gráfica pequena/média faz talvez algumas centenas de operações por dia → **fica dentro do grátis.**
- **Custo real esperado: R$ 0/mês** para o seu volume.
- Se um dia crescer muito e passar do limite, o plano pago (Blaze) cobra centavos: ~US$ 0,06 por 100 mil leituras. Na prática, alguns **poucos reais por mês** só se virar um volume grande.

### 📌 Minha recomendação
1. **Comece LOCAL** (como está) — custo zero, já resolve tudo e você testa na prática.
2. Se precisar acessar de vários lugares, suba no **Firebase Hosting** (continua grátis).
3. Só parta para o **Firestore** (sincronização na nuvem) se realmente precisar de vários aparelhos vendo os mesmos dados ao mesmo tempo — e mesmo assim deve continuar **grátis** no seu volume.

> Ou seja: dá pra ir do grátis ao grátis. Você provavelmente **não vai pagar nada** tão cedo.

---

## 🚀 Como publicar no Firebase Hosting (passo a passo rápido)

1. Crie uma conta em https://firebase.google.com (grátis) e um novo projeto.
2. No seu PC, instale o Node.js e rode: `npm install -g firebase-tools`
3. `firebase login`
4. Na pasta `sistema-grafica`, rode: `firebase init hosting`
   - Escolha o projeto criado
   - Diretório público: `.` (ponto) — ou copie o `index.html` para uma pasta `public`
   - App de página única: **Não**
5. `firebase deploy`
6. Ele te dá um link tipo `https://seu-projeto.web.app` — pronto, no ar e grátis.

> Se depois quiser a sincronização na nuvem (Firestore), me avise que eu adapto o sistema para salvar no Firestore em vez do navegador.
