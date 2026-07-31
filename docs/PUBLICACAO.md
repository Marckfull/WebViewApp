# Checklist de publicação

O projeto está pronto para compilar e jogar, mas **não** para ser publicado sem
antes trocar o que está listado aqui.

---

## 1. Obrigatório antes de enviar à Play Store

### IDs do AdMob

Hoje são os IDs oficiais de **teste** do Google — eles não geram receita.

- `app/build.gradle.kts` → `AD_APP_ID`, `AD_UNIT_REWARDED`, `AD_UNIT_INTERSTITIAL`
  (nos dois blocos, `debug` e `release`)
- `app/src/main/res/values/strings.xml` → `admob_app_id`

Deixe `ADS_TEST_MODE = true` enquanto testar em aparelhos próprios: clicar no
próprio anúncio real é tráfego inválido e pode derrubar a conta.

### Textos legais

Em `app/src/main/java/com/prisma/fusao/ui/screens/LegalScreens.kt`, substitua:

- `[NOME DO DESENVOLVEDOR/EMPRESA]`
- `[E-MAIL DE CONTATO]`

Confira também a data em `LegalTexts.LAST_UPDATE`.

### Política de Privacidade hospedada

A Play exige uma **URL pública** com a política, informada na ficha do app. O
texto pode ser o mesmo que está dentro do jogo — publique-o em qualquer página
estável (site próprio, GitHub Pages) e cole o link no Play Console.

### Identidade do app

- `applicationId` em `app/build.gradle.kts` (hoje `com.prisma.fusao`) — depois de
  publicado ele **não pode mais mudar**.
- `versionCode` / `versionName`.
- Chave de assinatura de release (`signingConfigs`) e o `.jks` guardado fora do
  repositório.

---

## 2. Formulários do Play Console

- **Segurança dos dados**: declare que o app não coleta dados próprios e que a
  publicidade (AdMob) pode usar o identificador de publicidade. Declare o uso da
  permissão `com.google.android.gms.permission.AD_ID`.
- **Classificação indicativa**: questionário IARC. O jogo não tem violência nem
  conteúdo sensível, mas **contém anúncios** — marque essa opção.
- **App contém anúncios**: sim.
- **Público-alvo**: 13+. Mirar público infantil exige revisar toda a configuração
  de anúncios (ver [ANUNCIOS.md](ANUNCIOS.md), seção 6).

---

## 3. Compras no app

O código já tem a estrutura de economia (moedas, itens, remoção de anúncios),
mas **não** integra o Google Play Billing. Enquanto isso, a loja é honesta com o
jogador: só vende com moedas ganhas jogando, e diz na tela que compras com
dinheiro real não estão ativas.

Para ativar: adicionar `com.android.billingclient:billing-ktx`, criar os produtos
no Play Console e ligar a compra ao campo `PlayerState.removeAdsPurchased` e ao
crédito de moedas.

---

## 4. Antes de cada release

```bash
./gradlew testDebugUnitTest     # motor e tutorial
./gradlew lint                  # análise estática
./gradlew assembleRelease       # ou bundleRelease para o .aab
```

Teste manual mínimo, num aparelho limpo:

1. Primeira execução: splash → aceite dos termos → tutorial → mapa.
2. Recusar um vídeo premiado e confirmar que **nada** fica bloqueado.
3. Ajustes → Opções de privacidade abre o formulário do UMP.
4. Ajustes → Apagar meus dados limpa tudo e devolve ao fluxo inicial.
5. Perder uma fase, aceitar as jogadas extras por vídeo e continuar a partida.
6. Modo avião: o jogo tem que funcionar inteiro, só sem anúncios.

---

## 5. Nota sobre o ambiente de desenvolvimento

Este projeto foi escrito num ambiente **sem acesso ao repositório Maven do Google
e sem o Android SDK** — `dl.google.com` estava bloqueado por política de rede.
O que foi verificado, e como:

| Camada | Verificação |
|---|---|
| Motor, campanha, roteiro do tutorial | Compilados e **testados de verdade**: 20 testes JUnit passando, mais bots jogando as 150 fases milhares de vezes |
| Telas, renderizador, áudio, anúncios | **Compilados** contra stubs escritos à mão das APIs de Compose/AndroidX/AdMob — sintaxe, tipos, escopos (`ColumnScope`/`RowScope`/`BoxScope`) e consistência entre os arquivos do projeto |

O segundo caso **não substitui** um `assembleDebug`: ele valida o código do
projeto, mas não pega divergência entre um stub e a assinatura real da
biblioteca. Foi assim, por exemplo, que apareceu um import errado real
(`rememberInfiniteTransition` vinha de `androidx.compose.runtime`, quando mora em
`androidx.compose.animation.core`) — mas um erro do tipo "este parâmetro do
Compose mudou de nome na versão X" passaria batido.

Na primeira build dentro do Android Studio, portanto, ainda pode haver ajuste
pontual de assinatura. A lógica de jogo, essa está verificada de fato.
