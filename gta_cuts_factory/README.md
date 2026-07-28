# 🎮 GTA Cuts Factory

Fábrica local de cortes verticais (9:16) sobre **GTA 6** e **GTA 5**, para
YouTube Shorts e TikTok. Você clica em **CRIAR**, o sistema produz **3 vídeos**
(2 em português + 1 em inglês) e coloca numa **fila de aprovação** — nada é
publicado sem você liberar.

Roda **100% no seu notebook Windows**, com ferramentas gratuitas
(yt-dlp, ffmpeg, faster-whisper). IA paga e TTS premium são **opcionais**.

---

## 📍 Status da construção

Estamos construindo **por partes**, testando cada uma antes de avançar.

| Etapa | O que faz | Status |
|-------|-----------|--------|
| **1. Núcleo** | baixar vídeo → cortar 9:16 → legenda karaokê + gancho | ✅ **pronto e testado** |
| **2. Banco + Segurança** | anti-repetição (SQLite) + módulo 🛡️ anti-strike | ✅ **pronto e testado** |
| **3. Fontes + Tendências** | canais aprovados + assuntos em alta + diversificação | ✅ **pronto e testado** |
| **4. Seleção inteligente** | escolher os melhores momentos, distintos e em frase completa | ✅ **pronto e testado** |
| **5. Modo B (narrado)** | roteiro + voz sintética grátis + trailer de fundo | ✅ **pronto e testado** |
| 6. Metadados + B-roll | título/descrição/hashtags PT-EN + gameplay de fundo | ⏳ próxima |
| 7. Painel | fila de aprovação com selo de segurança 🟢🟡🔴 | ⏳ |
| 8. Publicação | YouTube (não-listado) + rascunho TikTok | ⏳ |

O plano completo está em [`ARQUITETURA.md`](ARQUITETURA.md).

---

## 🚀 Começando (resumo)

O passo a passo detalhado, com o que clicar, está em
[`INSTALACAO_WINDOWS.md`](INSTALACAO_WINDOWS.md). Resumo:

```powershell
# 1. instalar ffmpeg
winget install --id Gyan.FFmpeg -e

# 2. preparar o projeto
cd $HOME\Desktop\WebViewApp\gta_cuts_factory
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# 3. TESTE 1 — legenda e reframe (não baixa nada, 30 segundos)
python scripts\teste_rapido.py

# 4. TESTE 2 — pipeline real com um vídeo do YouTube
python scripts\etapa1_pipeline.py --url "https://youtu.be/XXXX" --inicio 00:01:20 --duracao 75 --idioma pt

# 5. TESTE 3 — banco anti-repetição + checagens de segurança
python scripts\etapa2_teste.py

# 6. TESTE 4 — o que está em alta e de onde cortar (não gera vídeo)
python scripts\etapa3_teste.py

# 7. TESTE 5 — onde cortar dentro do vídeo (demonstração com roteiro conhecido)
python scripts\etapa4_teste.py --demo

# 8. conferir se está tudo saudável (99 testes)
python testes\test_legendas.py
python testes\test_seguranca.py
python testes\test_fontes.py
python testes\test_selecao.py
```

### Modo automático 🤖

Com a Etapa 4 pronta, você não precisa mais escolher o trecho na mão:

```powershell
python scripts\etapa1_pipeline.py --url "https://youtu.be/XXXX" --auto --idioma pt
```

O `--auto` transcreve o vídeo **uma vez**, escolhe o melhor momento (fugindo de
intro, patrocínio e despedida), corta em frase completa e renderiza. Rodando de
novo no mesmo vídeo, ele escolhe **outro** momento — o anterior já está no log
anti-repetição.

### Comandos do dia a dia

```powershell
python scripts\etapa3_teste.py              # o que está em alta + candidatos
python scripts\fontes.py listar             # canais e reputação de cada um
python scripts\fontes.py fila               # vídeos esperando sua aprovação
python scripts\fontes.py reprovados         # o que foi barrado, e por quê

# ⚠️ tomou um Content ID no YouTube? avise o sistema — o canal é bloqueado na hora
python scripts\fontes.py claim "@CanalX" --nota "claim de trilha sonora"
```

Tudo o que você pode ajustar (cores, fonte, tamanho, posição da legenda,
duração do gancho, chaves de API) está em **`config/config.yaml`**.

---

## 🎬 Os dois modos de vídeo

### MODO A — CORTE (recorte transformativo)

Recorta os melhores momentos de vídeos-fonte e aplica transformação real:
reframe 9:16, legenda animada, gancho e (quando faz sentido) gameplay de fundo.

➡️ **Risco médio.** Depende do canal-fonte e de quanto você transforma.

### MODO B — NOTÍCIA/TEORIA NARRADA ⭐ recomendado

Pega um assunto em alta, monta um roteiro curto, narra com voz sintética e usa
trailer oficial/gameplay de fundo com legenda por cima.

➡️ **Risco baixíssimo de Content ID.** O áudio é 100% seu (voz sintética sobre
roteiro original), e o vídeo de fundo entra como material de apoio de um
conteúdo comentado — exatamente o uso que a Rockstar permite.

Você escolhe a proporção no `config.yaml`:

```yaml
producao:
  modo_a_cortes: 2      # 2 cortes
  modo_b_narrados: 1    # + 1 narrado
```

**Como gerar um vídeo narrado:**

```powershell
python scripts\etapa5_narrado.py ^
    --assunto "GTA 6 segundo trailer" ^
    --fundo "C:\Videos\trailer_oficial.mp4" ^
    --fatos "a Rockstar publicou um comunicado no site oficial" ^
            "o comunicado fala do trailer, sem data fechada" ^
            "a página do jogo foi atualizada no mesmo dia"
```

⚠️ **Os fatos são a alma do vídeo — e o sistema não inventa notícia.** Ele narra
o que VOCÊ apurar. Sem `--fatos`, ele monta um roteiro de pergunta aberta,
dizendo claramente que nada foi confirmado. Toda narração leva a ressalva
"enquanto a Rockstar não confirmar, trate como rumor".

Isso não é frescura: publicar "a Rockstar CONFIRMOU" quando ninguém confirmou
queima a confiança do canal e ainda esbarra na política de conteúdo enganoso
do YouTube.

**Voz:** o padrão é o **Edge-TTS, grátis**, que ainda informa o tempo exato de
cada palavra — a legenda karaokê fica perfeitamente sincronizada, sem precisar
transcrever nada. Para testar o Modo B sem internet, use `tts.provedor: "teste"`
(gera vídeo mudo, só para conferir o resto).

---

## 🛡️ Boas práticas anti-strike (leia antes de publicar)

### 1. Content ID não é strike — mas atrapalha

Um **Content ID claim** normalmente só tira a monetização ou coloca anúncio do
dono do conteúdo. Um **strike de direitos autorais** é grave (3 = canal
removido). O sistema é desenhado para evitar os dois, com prioridade para
nunca chegar perto de um strike.

### 2. Corte bom não é corte aleatório

O sistema procura trechos com **gancho, palavra-chave e emoção**, e foge de
introdução, patrocínio e despedida. Um detalhe importante: se não houver trecho
bom o bastante (nota mínima em `selecao.nota_minima`), ele entrega **menos
vídeos** em vez de completar a cota com material fraco. Três cortes por dia só
valem a pena se os três forem bons.

### 3. Transformação real é a sua defesa

Um corte de 60 s que é só um pedaço do vídeo de outra pessoa, com legenda
automática, é **reupload com enfeite**. O que muda o jogo:

- **reframe 9:16** com foco na ação (o enquadramento é uma escolha editorial sua)
- **gancho próprio** de abertura
- **legenda de alto impacto** com destaque de palavras
- **comentário/narração sua** por cima — o item mais forte de todos
- **b-roll/gameplay** de fundo
- **crédito visível** ao autor original

Quanto mais itens dessa lista, mais transformativo — e mais seguro.

### 4. Regras da Rockstar (o sistema aplica automaticamente)

- ✅ Cutscene/cinemática **dentro** de conteúdo narrado ou de um vídeo maior
- ❌ Cutscene **isolada**, sem comentário → o módulo de segurança **bloqueia**
- ❌ Final do jogo, grandes revelações, spoilers de enredo → **bloqueado**
- ❌ Compilações editadas só de cinemáticas → **bloqueado**

### 5. Política de conteúdo violento do YouTube

O sistema evita por padrão violência gráfica forte (sangue em excesso, tortura,
violência sustentada contra NPCs). Quando um trecho candidato tem violência
forte, ele é **descartado**, tem a cena **encurtada/borrada**, ou entra na fila
com **aviso amarelo** para você decidir.

A política considera a **duração acumulada** das cenas gráficas — por isso o
sistema soma o total do corte (limite configurável em
`seguranca.segundos_maximos_cena_grafica`) em vez de olhar cena por cena.

**Prefira sempre**: corridas, momentos engraçados de NPC, passeios pela cidade,
descobertas do mapa, reações e comentários. Esses trechos retêm igual ou mais
que violência — e não colocam o canal em risco.

> ⚠️ Políticas de plataforma mudam. Antes de escalar o volume, confira a versão
> atual em <https://support.google.com/youtube/answer/2802002> e ajuste o
> `config.yaml` se necessário.

### 6. Metadados honestos ajudam a classificação

Use títulos e tags que descrevem o que é de fato: "Gameplay",
"Cutscene Narrada", "Story Discussion", "Notícia". Isso ajuda o YouTube a
classificar corretamente e evita restrição de idade por engano.
**Nunca** use clickbait que promete algo que o vídeo não mostra — além de
derrubar a retenção, é o caminho mais rápido para "conteúdo enganoso".

### 7. Sistema de reputação de fontes

Cada canal-fonte tem um status no banco local:

| Status | Significado |
|---|---|
| `oficial` | Rockstar Games — usar **sempre** com narração sua (Modo B) |
| `proprio` | suas gravações — risco zero |
| `em_teste` | canal de terceiro novo — usado com cautela e monitorado |
| `bloqueado` | deu problema (claim/restrição/remoção) — não é mais usado |

Todo canal novo entra como **`em_teste`**. Quando você registrar um problema
(`python scripts\fontes.py claim "@CanalX"`), o canal é **bloqueado na hora** e
nunca mais é usado — nem que ele continue escrito no seu `canais_fontes.yaml`.
Só você pode liberar de volta, com `desbloquear`.

### 8. Diversifique e vá devagar

- não corte sempre do mesmo canal — o sistema alterna sozinho: na escolha dos
  3 vídeos ele exige **canais diferentes E assuntos diferentes**, e nunca tira
  mais que 3 cortes do mesmo vídeo-fonte (`producao.max_cortes_por_video`);
- comece com **3 vídeos por dia**, observe 1 semana, e só então aumente;
- olhe o YouTube Studio: se aparecer claim, tire aquela fonte de circulação
  antes de gerar mais.

### 9. Crédito sempre

O link do vídeo original vai automaticamente na descrição. Crédito **não é**
licença — mas mostra boa-fé, e vários canais liberam cortes justamente por
causa dele. **O ideal é pedir autorização por escrito** aos canais que você usa
com frequência: com autorização, o risco cai praticamente a zero.

---

## ⚠️ Limitações que você precisa saber

### TikTok não permite publicação 100% automática

A **Content Posting API** do TikTok só publica direto se o seu app passar por
uma **auditoria de parceiro** da TikTok. Sem essa aprovação, o app fica no modo
não auditado e só consegue enviar o vídeo como **RASCUNHO (inbox)** — você
termina a postagem no celular.

Por isso o sistema faz o seguinte:

1. salva o vídeo final + legenda + hashtags numa pasta organizada
   (`saida/tiktok/`), pronta para você arrastar para o app; **e**
2. opcionalmente envia como rascunho via API, se você configurar as credenciais.

### YouTube

O upload sempre entra como **não-listado**. Você confere no YouTube Studio e
troca para público quando quiser. A cota diária padrão da API dá cerca de
**6 uploads por dia** — mais que suficiente para 3 vídeos por clique.

### Whisper

A primeira execução baixa o modelo (~500 MB no tamanho `small`). Depois disso
funciona **offline**. Num notebook sem placa de vídeo, um corte de 60 s leva
cerca de 1–2 minutos para transcrever.

---

## 💰 Custo por vídeo

| Item | Custo |
|---|---|
| Download, corte, legenda, render | **R$ 0,00** (tudo local) |
| Transcrição (faster-whisper) | **R$ 0,00** (local) |
| Voz do Modo B (Edge-TTS) | **R$ 0,00** |
| IA para gancho/roteiro/metadados (opcional) | ~US$ 0,01–0,03 |
| Voz premium ElevenLabs (opcional) | ~US$ 0,02–0,05 |

Com tudo desligado, o sistema roda de graça. As opções pagas ficam em
`config.yaml` (`ia.ativa` e `tts.provedor`) e vêm **desligadas por padrão**.

---

## 📂 Onde ficam as coisas

```
config/config.yaml        ← todos os ajustes e chaves (não vai para o Git)
config/canais_fontes.yaml ← seu banco de canais aprovados
dados/downloads/          ← vídeos baixados
dados/transcricoes/       ← transcrições (reaproveitadas, não retranscreve)
saida/AAAA-MM-DD/         ← vídeos prontos + miniatura + ficha .json
```

---

## ❓ Deu erro?

Comece rodando os testes: `python testes\test_legendas.py`.
A tabela de problemas comuns está no fim do
[`INSTALACAO_WINDOWS.md`](INSTALACAO_WINDOWS.md).

---

## ⚖️ Responsabilidade

Esta ferramenta automatiza edição e organização. A decisão de publicar é sua, e
a responsabilidade pelo conteúdo publicado também. Use fontes que você tem
direito de usar, dê crédito, e respeite os Termos de Uso do YouTube, do TikTok e
as [diretrizes de vídeos da Rockstar Games](https://support.rockstargames.com).
