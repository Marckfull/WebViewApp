# 🪟 Instalação no Windows — passo a passo (para iniciantes)

> Siga na ordem. Cada passo diz **exatamente** o que clicar e o que digitar.
> Tempo total: ~20 minutos (a maior parte é download).

---

## Passo 0 — Abrir o "terminal" (você vai usar o tempo todo)

1. Aperte a tecla **⊞ Windows**.
2. Digite `powershell`.
3. Clique em **Windows PowerShell**.

Vai abrir uma janela azul/preta onde você digita comandos e aperta **Enter**.
É só isso. Quando eu disser "rode X", significa: digite X nessa janela e aperte Enter.

💡 Para **colar** um comando no PowerShell: `Ctrl + V` ou clique com o botão direito.

---

## Passo 1 — Instalar o Python

1. Acesse: <https://www.python.org/downloads/windows/>
2. Baixe o **Python 3.11** (recomendado) ou 3.12 — "Windows installer (64-bit)".
   - ⚠️ Evite o 3.13 por enquanto: algumas bibliotecas de vídeo ainda não têm
     versão pronta para ele.
3. Rode o instalador e **MUITO IMPORTANTE**: na primeira tela, marque a caixinha
   ☑️ **"Add python.exe to PATH"** (fica embaixo). Depois clique em
   **Install Now**.
4. Feche e abra o PowerShell de novo e confira:

```powershell
python --version
```

Deve aparecer algo como `Python 3.11.9`. Se aparecer erro, você esqueceu da
caixinha do PATH — desinstale e refaça marcando ela.

---

## Passo 2 — Instalar o ffmpeg (o programa que corta e monta o vídeo)

O jeito mais fácil (uma linha só):

```powershell
winget install --id Gyan.FFmpeg -e
```

Aceite os termos se ele perguntar. **Feche e abra o PowerShell** e teste:

```powershell
ffmpeg -version
```

Deve aparecer `ffmpeg version ...`.

### Se o `winget` não existir na sua máquina (jeito manual)

1. Baixe em <https://www.gyan.dev/ffmpeg/builds/> → **ffmpeg-release-essentials.zip**
2. Extraia o zip. Vai aparecer uma pasta tipo `ffmpeg-7.0-essentials_build`.
3. Renomeie essa pasta para `ffmpeg` e mova para `C:\ffmpeg`.
4. Confirme que existe o arquivo `C:\ffmpeg\bin\ffmpeg.exe`.
5. Você **não precisa** mexer em variáveis de ambiente: basta escrever esse
   caminho no arquivo de configuração (Passo 5), no campo `ffmpeg_caminho`.

---

## Passo 3 — Baixar este projeto e criar o "ambiente virtual"

Ambiente virtual = uma caixinha separada onde as bibliotecas deste projeto ficam,
sem bagunçar o resto do computador.

```powershell
cd $HOME\Desktop
git clone https://github.com/Marckfull/WebViewApp.git
cd WebViewApp\gta_cuts_factory
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

✅ Deu certo se aparecer `(.venv)` no começo da linha do terminal.

❌ Se aparecer um erro vermelho falando de **"execução de scripts foi desabilitada"**, rode:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

Digite `S` (ou `Y`) e Enter. Depois repita o `.\.venv\Scripts\Activate.ps1`.

> 🔁 **Toda vez** que você abrir o PowerShell para usar o sistema, precisa rodar
> de novo: `cd $HOME\Desktop\WebViewApp\gta_cuts_factory` e
> `.\.venv\Scripts\Activate.ps1`

---

## Passo 4 — Instalar as bibliotecas

```powershell
python -m pip install --upgrade pip
pip install -r requirements.txt
```

Demora alguns minutos (o `faster-whisper` é grande). Se o `pip install` falhar em
alguma linha, me mande a mensagem de erro.

---

## Passo 5 — Criar o seu arquivo de configuração

```powershell
copy config\config.example.yaml config\config.yaml
copy config\canais_fontes.example.yaml config\canais_fontes.yaml
notepad config\config.yaml
```

Vai abrir o Bloco de Notas. Por enquanto, **você só precisa mexer se**:

- o ffmpeg foi instalado manualmente → preencha
  `ffmpeg_caminho: "C:/ffmpeg/bin/ffmpeg.exe"` e
  `ffprobe_caminho: "C:/ffmpeg/bin/ffprobe.exe"`
  (use barra normal `/`, não `\`)

O resto (chaves de API) fica em branco até chegarmos nas etapas de publicação.
Salve com `Ctrl + S` e feche.

---

## Passo 6 — Instalar as fontes bonitas da legenda (opcional, mas recomendado)

A legenda funciona com fontes que já vêm no Windows (`Arial Black`, `Impact`).
Para ficar com cara de CapCut/TikTok, instale a **Montserrat ExtraBold**:

1. Acesse <https://fonts.google.com/specimen/Montserrat> e clique em
   **Get font** → **Download all**.
2. Extraia o zip, entre na pasta `static`.
3. Clique com o botão direito em `Montserrat-ExtraBold.ttf` → **Instalar para todos os usuários**.
4. No `config/config.yaml`, deixe `fonte: "Montserrat ExtraBold"`.

Se não instalar, deixe `fonte: "Arial Black"` — funciona bem também.

---

## Passo 7 — TESTE 1: legenda e reframe, sem baixar nada (30 segundos)

Esse teste cria um vídeo de teste no seu computador e aplica legenda + gancho.
Serve para provar que o ffmpeg e as legendas estão funcionando.

```powershell
python scripts\teste_rapido.py
```

No fim ele mostra o caminho do arquivo gerado. Abra:

```powershell
start saida\teste\teste_legenda.mp4
```

👀 **O que você deve ver:** vídeo vertical, gancho grande nos 2 primeiros
segundos e legenda palavra-por-palavra um pouco abaixo do centro, com
"GTA 6", "VAZOU" e "CONFIRMADO" em amarelo.

---

## Passo 8 — TESTE 2: pipeline real com 1 vídeo do YouTube

```powershell
python scripts\etapa1_pipeline.py --url "https://www.youtube.com/watch?v=QDoZgQCwiDU" --inicio 00:00:30 --duracao 60 --idioma pt
```

O que acontece:

1. baixa o vídeo (yt-dlp) → `dados/downloads/`
2. corta o trecho pedido
3. transcreve com o Whisper (na primeira vez ele baixa o modelo, ~500 MB — só uma vez)
4. gera a legenda karaokê + gancho
5. renderiza em 1080x1920 → `saida/`

⏱️ A primeira execução demora mais (download do modelo Whisper). Nas próximas,
um corte de 60s leva ~2–4 minutos em notebook comum.

---

## Passo 9 — TESTE 3: o escudo anti-strike (30 segundos, sem internet)

```powershell
python scripts\etapa2_teste.py
```

Ele encena 7 situações e mostra o sistema aceitando e recusando vídeos: trecho
repetido, assunto repetido, cinemática oficial sem narração, canal que tomou
claim e o detector de violência.

Depois, veja a memória do sistema:

```powershell
python scripts\fontes.py listar        # seus canais e a reputação de cada um
python scripts\fontes.py fila          # o que está esperando sua aprovação
python scripts\fontes.py reprovados    # o que foi barrado, com o motivo
```

⚠️ **Importante no dia a dia:** se um vídeo publicado tomar Content ID, avise o
sistema — o canal é bloqueado na hora e não é mais usado:

```powershell
python scripts\fontes.py claim "@CanalX" --nota "claim de trilha sonora"
```

---

## Passo 10 — TESTE 4: o que está em alta e de onde vamos cortar

Antes deste teste, edite o seu banco de canais e coloque canais **reais** que
você aprova (o arquivo vem com exemplos de mentira):

```powershell
notepad config\canais_fontes.yaml
```

Dicas para escolher canais:
- prefira canais de **notícia/análise falada** (mais transformativo) a canais
  que só repostam gameplay;
- se você tiver gravações suas, aponte `pasta_local` para a pasta delas —
  é a fonte de **risco zero**;
- todo canal de terceiro entra como `em_teste`. Isso é de propósito.

Agora rode (ele **não** baixa nem gera vídeo nenhum):

```powershell
python scripts\etapa3_teste.py
```

Você vai ver três blocos:

1. **assuntos em alta** — vindos da sua lista + YouTube + Reddit
2. **candidatos** — vídeos dos seus canais, com nota e o porquê de cada nota
3. **os 3 escolhidos** — com canais e assuntos diferentes entre si

Se aparecer "nenhum candidato", é sinal de que o `canais_fontes.yaml` ainda
está com os exemplos. Sem internet, use `python scripts\etapa3_teste.py --offline`.

---

## Passo 11 — TESTE 5: onde cortar dentro do vídeo

```powershell
python scripts\etapa4_teste.py --demo
```

Ele usa um vídeo de mentirinha com roteiro **conhecido de propósito** (intro
chata, patrocínio, papo morno, dois trechos quentes e despedida) e mostra o
sistema fugindo do lixo e achando o ouro. No fim, ele mesmo confere o resultado
item por item.

Para usar num vídeo seu de verdade:

```powershell
python scripts\etapa4_teste.py --video "C:\Videos\gameplay.mp4"
```

### 🤖 O modo automático

Agora o pipeline consegue escolher o trecho sozinho:

```powershell
python scripts\etapa1_pipeline.py --url "https://youtu.be/XXXX" --auto --idioma pt
```

Ele transcreve o vídeo uma vez, escolhe o melhor momento, corta em frase
completa e renderiza. Rodando de novo no mesmo vídeo, escolhe outro trecho.

Se o resultado não te agradar, ajuste os pesos em `config/config.yaml`, na
seção `selecao` — `peso_gancho`, `peso_emocao`, `nota_minima` e as listas de
expressões são todos seus para mexer.

---

## Onde conseguir cada chave de API (só nas etapas finais)

| Chave | Onde pegar | Precisa agora? |
|---|---|---|
| **YouTube Data API v3** | <https://console.cloud.google.com> → criar projeto → *APIs e Serviços* → ativar "YouTube Data API v3" → *Credenciais* → **ID do cliente OAuth** (tipo: App para computador) → baixar o `client_secret.json` e salvar em `config/` | só na Etapa 8 |
| **TikTok Content Posting API** | <https://developers.tiktok.com> → criar app → pedir o escopo `video.publish` (**exige aprovação da TikTok**) | opcional |
| **Anthropic (Claude)** | <https://console.anthropic.com> → *API Keys* | opcional |
| **OpenAI** | <https://platform.openai.com/api-keys> | opcional |
| **ElevenLabs (voz)** | <https://elevenlabs.io> → *Profile → API Key* | opcional (Edge-TTS é grátis) |

---

## Problemas comuns

| Erro | Solução |
|---|---|
| `'python' não é reconhecido` | Reinstale o Python marcando **Add to PATH** |
| `'ffmpeg' não é reconhecido` | Passo 2, ou preencha `ffmpeg_caminho` no config |
| `execução de scripts foi desabilitada` | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| Legenda aparece sem estilo / fonte errada | O nome em `fonte:` precisa ser **exatamente** o nome instalado no Windows |
| Whisper muito lento | Em `config.yaml` troque `modelo_whisper: "small"` por `"base"` |
| `HTTP Error 403` no download | Rode `pip install -U yt-dlp` (o YouTube muda direto; o yt-dlp atualiza rápido) |
