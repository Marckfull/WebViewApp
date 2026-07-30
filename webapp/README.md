# Conferência de Estoque — bipagem → PDF

App de página única para **conferência/contagem de estoque por leitura de código de barras**, usando como
fonte de dados uma planilha exportada do Bling (`.xlsx`, `.xls` ou `.csv`).

> **Somente leitura.** O app **nunca altera o estoque do Bling** — ele apenas lê a planilha que você
> carregar e registra a contagem manual do operador. A planilha original também não é modificada.
>
> **Nada sai do aparelho.** O arquivo é lido pelo próprio navegador; não há servidor, upload ou banco de dados.

## Como usar

1. **Carregar planilha** — arraste a exportação do Bling para a área indicada (ou toque para escolher o arquivo).
   O app lê a primeira aba e detecta sozinho a linha de cabeçalho, mesmo quando a exportação tem linhas
   de preâmbulo acima dela.
2. **Mapear colunas** — as colunas de **código de barras**, **código/SKU**, **nome do produto** e
   **ordem de serviço** são identificadas automaticamente pelos nomes do cabeçalho (GTIN, EAN, SKU,
   Descrição, Ordem de Serviço…). Confira a prévia e corrija nos seletores se algo ficou trocado.
   Só o **código de barras** é obrigatório; os demais podem ficar como "nenhuma".
3. **Bipar** — o cursor fica no campo grande de leitura. O leitor físico funciona como um teclado: ele
   digita o código e envia `Enter` sozinho. O app mostra **código, nome do produto e ordem de serviço**
   e foca o campo de **quantidade**, que é sempre digitada pelo operador (não assume 1).
   Confirme com `Enter` ou pelo botão — o foco volta ao campo de leitura para o próximo item.
4. **Contagem e PDF** — a tabela mostra tudo o que foi contado, com totais ao vivo de itens e unidades.
   O botão **Gerar PDF da contagem** baixa o arquivo `contagem_AAAA-MM-DD.pdf` com cabeçalho
   (data, hora, totais), a tabela completa e linhas para assinatura.

### Detalhes úteis do fluxo

- **Código não encontrado**: aparece um aviso âmbar orientando conferir o cadastro no Bling. O fluxo não
  trava — você pode ignorar e seguir bipando, ou registrar a contagem mesmo assim; nesse caso o item sai
  marcado como **"não cadastrado"** na tabela e no PDF.
- **Cada leitura vira uma linha nova** (porque a quantidade é digitada na hora). Se preferir ver e exportar
  um total por produto, ligue **"Agrupar por código (somar quantidades)"** no passo 4 — vem desligado por padrão.
- **Quantidade** aceita inteiros e decimais com vírgula ou ponto (`12`, `1,5`, `1.5`). Zero, negativo e
  vazio são recusados com mensagem no próprio campo.
- **Remover linha**: botão de lixeira na própria linha. No modo agrupado ele remove todas as leituras
  daquele código.
- **Limpar contagem**: pede confirmação antes de apagar tudo.
- **Busca tolerante**: espaços, hífens, pontos e o apóstrofo que o Excel às vezes coloca antes do número
  são ignorados na comparação; códigos com zeros à esquerda (UPC-A lido como EAN-13) também casam.
- **CSV**: separador (`;` ou `,`) e acentuação em UTF-8 ou latin-1 são detectados automaticamente.

## Testando sem leitor

Use o arquivo **`exemplo_bling.csv`** (8 produtos, colunas GTIN/EAN, SKU, Descrição, Ordem de Serviço):
carregue-o no passo 1 e digite um dos códigos no campo de leitura, seguido de `Enter` — por exemplo
`7891234567890` (existe) ou `1111111111111` (para ver o aviso de não encontrado).

## Como abrir

Abra `index.html` no navegador (duplo clique já funciona) ou publique a pasta em qualquer hospedagem
estática. Funciona em desktop e celular, com leitor USB ou Bluetooth.

É necessária **internet apenas no carregamento da página**, para baixar as três bibliotecas via CDN
(SheetJS, jsPDF e jsPDF-AutoTable). Se elas não carregarem, o app avisa no topo da tela em vez de falhar
em silêncio. Depois de carregada, a leitura da planilha e a geração do PDF são 100% locais.

## Observações técnicas

- Um único arquivo `index.html` com HTML, CSS e JS — sem build, sem backend, sem framework.
- O estado (planilha, mapeamento e contagem) vive **apenas em memória durante a sessão**: não usamos
  `localStorage` nem `sessionStorage`. **Recarregar a página zera a contagem** — gere o PDF antes de fechar.
- Acessibilidade: foco de teclado visível, labels associadas, avisos anunciados por leitor de tela e
  contraste adequado. As animações são desligadas quando o sistema tem
  `prefers-reduced-motion` (redução de movimento) ativado.
