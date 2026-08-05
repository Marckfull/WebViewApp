# Checklist de compliance

Rode este checklist **por episódio** antes do upload e **por trimestre** para o
canal inteiro. O hook `Stop` imprime a versão curta ao fim de cada sessão.

## Por episódio

- [ ] Marcado como "feito para crianças" no YouTube Studio
- [ ] Divulgação de uso de IA preenchida onde aplicável (conteúdo sintético/alterado)
- [ ] Zero IP de terceiros: personagem, trilha, formato, thumbnail, fonte tipográfica
- [ ] Licença comercial documentada de todas as vozes, imagens e trilhas usadas
     (link/arquivo em `docs/licencas/`)
- [ ] `04-qa.md` com `VEREDITO FINAL: APROVADO` arquivado
- [ ] Aprovação humana do **roteiro** registrada (`aprovado_por:` + `data:`)
- [ ] Aprovação humana do **corte final** registrada
- [ ] Nenhuma chamada comercial, pedido de dado pessoal ou linguagem de urgência
- [ ] Nenhuma ação imitável perigosa em cena
- [ ] Legendas revisadas por humano (não publicar saída bruta de ASR)
- [ ] Metadados (título, descrição, tags) sem clickbait e sem nome de IP alheio

## Por canal / trimestral

- [ ] Revisão das políticas do YPP e das diretrizes de qualidade familiar
     (elas mudam — reler, não presumir)
- [ ] Revisão da política de conteúdo inautêntico / gerado por IA
- [ ] Auditoria de licenças: todas ainda válidas e cobrindo uso comercial + derivados
- [ ] Nenhuma coleta de dado de criança em qualquer canal do projeto
     (site, newsletter, formulário, comentários, app)
- [ ] Conformidade com COPPA/LGPD onde aplicável — nenhum dado de menor coletado
- [ ] Se houver publicidade/marca: divulgação clara + adequação infantil +
     regras de publicidade dirigida a crianças (CONAR no Brasil)
- [ ] Backup do registro de aprovações humanas fora do repositório

## Registro de aprovações

Cada episódio mantém o registro no cabeçalho dos próprios arquivos:

```yaml
status: aprovado
aprovado_por: <nome da pessoa>
data: AAAA-MM-DD
```

Nunca preencha esses campos em nome de outra pessoa e nunca os gere
automaticamente. Eles são a prova de input criativo humano significativo — que
é exatamente o que sustenta a monetização e a responsabilidade legal sobre
conteúdo infantil.

## O que fazer se algo passar

1. Despublicar o vídeo imediatamente (não deletar — precisa do registro).
2. Registrar o incidente em `docs/incidentes.md`: o que passou, qual item do
   Guardião falhou, por quê.
3. Adicionar uma verificação nova ao `guardiao.md` cobrindo aquele caso.
4. Reprocessar os episódios já publicados que compartilham o mesmo padrão.
