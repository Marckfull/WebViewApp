# 🚀 Checklist de publicação na Google Play

## 1. Identidade do app

- [ ] Definir o `applicationId` final em `app/build.gradle.kts`
      (hoje: `com.neuroflip.game`) — ele **não pode mudar** depois de publicado.
- [ ] Conferir o nome em `res/values/strings.xml`.
- [ ] Ícone: os vetores em `res/drawable/ic_launcher_*.xml` já geram o adaptive
      icon. Se quiser trocar, use Image Asset Studio do Android Studio.

## 2. Anúncios (AdMob)

- [ ] Criar o app no AdMob e pegar o **App ID** real.
- [ ] Substituir no `AndroidManifest.xml`:
      `com.google.android.gms.ads.APPLICATION_ID`.
- [ ] Substituir os 2 blocos em `ads/AdsManager.kt` → `object AdUnits`
      (rewarded e interstitial).
- [ ] Cadastrar seu aparelho como **dispositivo de teste** durante o
      desenvolvimento (`RequestConfiguration.Builder().setTestDeviceIds(...)`).
      Clicar nos próprios anúncios reais é a causa nº 1 de banimento.
- [ ] Preencher o `app-ads.txt` no site do app, se você tiver um.

## 3. Documentos legais

- [ ] Trocar em `ui/screens/LegalTexts.kt`:
      `COMPANY`, `CONTACT_EMAIL` e `EFFECTIVE_DATE`.
- [ ] Publicar a **Política de Privacidade numa URL pública** (GitHub Pages
      serve, e é grátis) — a ficha da Play exige o link.
- [ ] Regerar os arquivos de `docs/` se você editar os textos.

## 4. Formulário de conteúdo da Play Console

- [ ] **Segurança dos dados**: declarar que o app coleta *Identificadores do
      dispositivo (ID de publicidade)* para *Publicidade e marketing*, que os
      dados são compartilhados com o Google AdMob e que **não são criptografados
      em trânsito por você** (quem transmite é o SDK do Google).
- [ ] **Público-alvo**: 13+ (o app não é direcionado a crianças; se marcar
      "crianças" será preciso usar apenas anúncios family-safe).
- [ ] **Classificação indicativa**: preencher o questionário — jogo casual sem
      violência, mas **marque que contém anúncios**.
- [ ] Marcar "Contém anúncios" na ficha da loja.
- [ ] Declarar que **não há compras no app** (esta versão não tem billing).

## 5. Build de release

```bash
# 1. Gere uma chave (guarde o .jks e a senha em local seguro — perdeu, perdeu)
keytool -genkey -v -keystore neuroflip.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias neuroflip

# 2. Configure a assinatura (use variáveis de ambiente ou key.properties
#    fora do controle de versão — o .gitignore já ignora *.jks e key.properties)

# 3. Gere o bundle
./gradlew bundleRelease
```

- [ ] Ativar o **Play App Signing** (recomendado pelo Google).
- [ ] Testar o `.aab` pelo **teste interno** antes de mandar para produção.
- [ ] Conferir se o R8 não quebrou nada: rodar o APK de release num aparelho real
      e verificar que os anúncios e o áudio funcionam.

## 6. Ficha da loja

- [ ] Título (30 caracteres): `NeuroFlip: Memória Neon`
- [ ] Descrição curta (80): sugestão —
      *"Jogo da memória com pulso ECO, cartas que trocam de lugar e combos."*
- [ ] Descrição longa: explique as mecânicas exclusivas (ECO, Mutação,
      Sobrecarga) — é isso que diferencia o app nos resultados de busca.
- [ ] 4 a 8 capturas de tela (a splash, o tabuleiro em combo, o resultado com
      estrelas e a loja funcionam bem).
- [ ] Ícone 512×512 e banner 1024×500.

## 7. Depois do lançamento

- [ ] Acompanhar **retenção D1/D7** na Play Console; se cair, o primeiro
      suspeito é a frequência de intersticiais.
- [ ] Acompanhar o **eCPM por posição** no AdMob e desligar o que renderizar mal.
- [ ] Responder avaliações — peso real no ranking da loja.
