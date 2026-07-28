# 🏗️ GTA Cuts Factory — Plano de Arquitetura

> Leia este arquivo **antes** de rodar qualquer coisa. Ele explica o desenho do
> sistema inteiro e em que ordem vamos construir e testar cada parte.

---

## 1. Visão geral em uma imagem

```
                       ┌─────────────────────────────┐
   você clica "CRIAR"  │   PAINEL LOCAL (Flask)       │
   ───────────────────▶│   http://localhost:5000      │
                       └──────────────┬──────────────┘
                                      │ dispara
                                      ▼
                       ┌─────────────────────────────┐
                       │      ORQUESTRADOR           │
                       │  (pipeline.py — o "chefe")  │
                       └──────────────┬──────────────┘
                                      │
        ┌──────────────┬──────────────┼──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ TENDÊNCIAS│  │  FONTES   │  │ DOWNLOAD  │  │TRANSCRIÇÃO│  │  SELEÇÃO  │
  │ o que tá  │  │ canais    │  │  yt-dlp   │  │  Whisper  │  │ 3 melhores│
  │ em alta   │  │ aprovados │  │           │  │ (palavra) │  │  momentos │
  └───────────┘  └───────────┘  └───────────┘  └───────────┘  └─────┬─────┘
                                                                    │
        ┌───────────────────────────────────────────────────────────┘
        ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │  REFRAME  │─▶│ LEGENDAS  │─▶│  GANCHO   │─▶│  B-ROLL   │─▶│  RENDER   │
  │   9:16    │  │ karaokê   │  │  2 seg.   │  │ gameplay  │  │  ffmpeg   │
  └───────────┘  └───────────┘  └───────────┘  └───────────┘  └─────┬─────┘
                                                                    │
                                      ┌─────────────────────────────┘
                                      ▼
                       ┌─────────────────────────────┐
                       │  🛡️ MÓDULO DE SEGURANÇA      │  ◀── barreira obrigatória
                       │  (checagem pré-fila)        │
                       └──────────────┬──────────────┘
                            passou    │    reprovou
                          ┌───────────┴───────────┐
                          ▼                       ▼
                  ┌───────────────┐      ┌───────────────┐
                  │ FILA APROVAÇÃO│      │  REPROVADOS   │
                  │ 🟢🟡🔴 + edição│      │  + motivo     │
                  └───────┬───────┘      └───────────────┘
                          │ você clica "Publicar"
                          ▼
                  ┌───────────────┐
                  │ YouTube API   │  + pasta/rascunho TikTok
                  │ (não-listado) │
                  └───────────────┘
```

**Regra de ouro do sistema:** nada é publicado sozinho. O robô produz, o módulo
de segurança filtra, **você aprova**.

---

## 2. Estrutura de pastas (final)

```
gta_cuts_factory/
│
├── README.md                     ← como usar + boas práticas anti-strike
├── ARQUITETURA.md                ← este arquivo (o plano)
├── INSTALACAO_WINDOWS.md         ← passo a passo de instalação (iniciante)
├── requirements.txt              ← bibliotecas Python
│
├── config/
│   ├── config.yaml               ← SEU arquivo (criado a partir do exemplo)
│   ├── config.example.yaml       ← modelo com todas as opções comentadas
│   ├── canais_fontes.example.yaml← banco de canais aprovados (modelo)
│   └── canais_fontes.yaml        ← SEU banco de canais
│
├── nucleo/                       ← o "motor" (cada arquivo = uma função clara)
│   ├── config.py                 ✅ carrega e valida as configurações
│   ├── utils.py                  ✅ ffmpeg, logs, pastas, tempo
│   ├── downloader.py             ✅ baixa vídeo com yt-dlp
│   ├── transcricao.py            ✅ Whisper com timestamp por PALAVRA
│   ├── reframe.py                ✅ horizontal → 9:16 inteligente
│   ├── legendas.py               ✅ legenda karaokê + destaque de palavra
│   ├── gancho.py                 ✅ frase de impacto nos 2 primeiros segundos
│   ├── render.py                 ✅ junta tudo e chama o ffmpeg
│   │
│   ├── tendencias.py             ⏳ o que está em alta sobre GTA 6 / GTA 5
│   ├── fontes.py                 ⏳ escolhe vídeos dos canais aprovados
│   ├── selecao.py                ⏳ escolhe os 3 melhores momentos (distintos)
│   ├── broll.py                  ⏳ gameplay de fundo / split-screen
│   ├── narracao.py               ⏳ MODO B: roteiro + TTS
│   ├── metadados.py              ⏳ título, descrição, hashtags (PT/EN)
│   ├── seguranca.py              ⏳ 🛡️ checagem anti-strike + reputação
│   ├── banco.py                  ⏳ SQLite: anti-repetição + reputação
│   └── publicacao.py             ⏳ YouTube API + pasta/rascunho TikTok
│
├── painel/                       ⏳ interface web local
│   ├── app.py                    ⏳ Flask: botão CRIAR + fila + publicar
│   ├── templates/
│   └── static/
│
├── scripts/                      ← comandos que VOCÊ roda no terminal
│   ├── teste_rapido.py           ✅ testa legenda/reframe SEM baixar nada
│   └── etapa1_pipeline.py        ✅ 1 vídeo → 9:16 → legenda animada
│
├── testes/                       ← testes automáticos
│   └── test_legendas.py          ✅
│
├── dados/                        ← downloads, transcrições, banco.sqlite
└── saida/                        ← vídeos finais prontos + metadados
```

Legenda: ✅ = pronto e testado nesta etapa · ⏳ = próximas etapas.

---

## 3. Ordem de construção (construção incremental)

Cada etapa só começa quando a anterior estiver **rodando na sua máquina**.

| Etapa | O que entrega | Como você testa |
|-------|---------------|-----------------|
| **1. NÚCLEO** ✅ | baixar 1 vídeo → cortar 9:16 → legenda karaokê com destaque de palavra-chave → gancho de 2s | `python scripts/teste_rapido.py` e depois `python scripts/etapa1_pipeline.py --url ...` |
| **2. BANCO + SEGURANÇA** | SQLite (anti-repetição + reputação de fontes) e o módulo 🛡️ que aprova/reprova | rodar a etapa 1 duas vezes no mesmo trecho → segunda vez deve ser bloqueada |
| **3. FONTES + TENDÊNCIAS** | banco de canais aprovados, busca do que está em alta, escolha diversificada | listar os candidatos que ele encontrou, sem gerar vídeo |
| **4. SELEÇÃO INTELIGENTE** | escolher os 3 melhores momentos DISTINTOS (heurística + IA opcional) | ver os 3 trechos escolhidos e os motivos |
| **5. MODO B (narrado)** | roteiro + TTS + trailer oficial de fundo | gerar 1 vídeo narrado completo |
| **6. METADADOS + B-ROLL** | título/descrição/hashtags PT e EN, gameplay de fundo/split | conferir textos gerados |
| **7. PAINEL** | Flask com botão CRIAR, fila, selo 🟢🟡🔴, edição, reprovados | abrir `http://localhost:5000` |
| **8. PUBLICAÇÃO** | YouTube (não-listado) + pasta/rascunho TikTok | subir 1 vídeo de teste não-listado |

---

## 4. Decisões técnicas (e por quê)

| Assunto | Escolha | Motivo |
|---|---|---|
| Download | **yt-dlp** | grátis, open-source, o mais atualizado |
| Vídeo | **ffmpeg** | padrão da indústria, roda local, rápido |
| Transcrição | **faster-whisper** (`small`/`medium`) | grátis, offline, dá timestamp **por palavra** — essencial para a legenda karaokê |
| Legenda | **ASS (Advanced SubStation)** queimada pelo ffmpeg/libass | qualidade de legenda tipo CapCut sem depender de editor: cor por palavra, contorno grosso, sombra, animação de "pop" |
| Reframe | **OpenCV** (rosto/movimento) + fallback crop central / fundo desfocado | mantém a ação no enquadramento sem custo de IA paga |
| Banco | **SQLite** | um arquivo só, zero instalação, perfeito para local |
| Painel | **Flask** | mais simples de entender que FastAPI para quem está começando |
| IA de texto | **opcional** (Claude/OpenAI) para gancho, roteiro e metadados | funciona sem IA (modelos de frase prontos); com IA fica melhor. Custo ≈ **US$ 0,01–0,03 por vídeo** |
| TTS (Modo B) | **Edge-TTS grátis** por padrão; ElevenLabs opcional | Edge-TTS tem vozes PT-BR boas e custa **R$ 0,00**. ElevenLabs ≈ US$ 0,02–0,05/vídeo |

**Nada de IA paga é obrigatório.** O sistema roda 100% de graça; as chaves são
plugáveis e o custo aparece na tela antes de usar.

---

## 5. O caminho de um vídeo, passo a passo (o que acontece por baixo)

1. **Tendências** → lista de assuntos quentes (ex.: "GTA 6 data de lançamento").
2. **Fontes** → dos canais que VOCÊ aprovou, pega vídeos recentes sobre esses assuntos.
   Ordem de prioridade: `oficial` (Rockstar) → `próprio` (suas gravações) → `em teste`.
3. **Download** (yt-dlp) → salva em `dados/downloads/`.
4. **Transcrição** (Whisper) → `dados/transcricoes/<id>.json` com **cada palavra e seu tempo**.
5. **Seleção** → acha 3 trechos de 60–180s, com começo/fim em frase completa,
   **distintos entre si** e ainda não usados (consulta o banco).
6. **Reframe 9:16** → detecta onde está a ação/pessoa e corta 1080x1920.
7. **Legendas** → gera o `.ass` karaokê com destaque de palavras-chave.
8. **Gancho** → frase de impacto grande nos primeiros 2 segundos.
9. **B-roll** (se o trecho for "cabeça falando") → gameplay de fundo ou split-screen.
10. **Render** (ffmpeg) → `saida/<data>/video_1_pt.mp4`.
11. **🛡️ Segurança** → 5 checagens (repetição, fonte bloqueada, cinemática isolada,
    violência, spoiler). Passou → fila. Reprovou → lista "reprovados" com o motivo.
12. **Metadados** → título/descrição/hashtags no idioma do vídeo + crédito da fonte.
13. **Fila** → você revisa, edita e clica em publicar.

---

## 6. 🛡️ Módulo de segurança — desenho

Roda **sempre**, como uma barreira antes da fila. Retorna um selo:

| Selo | Quando | O que acontece |
|---|---|---|
| 🟢 **Verde** | fonte `oficial` ou `próprio`, sem violência forte, com narração/comentário | entra na fila, recomendado |
| 🟡 **Amarelo** | fonte `em teste` OU violência moderada | entra na fila, mas com aviso |
| 🔴 **Vermelho** | qualquer risco alto | **não** entra na fila; vai para "reprovados" com o motivo |

Checagens implementadas na Etapa 2:

1. `trecho_ja_usado()` — sobreposição de timestamps no banco.
2. `fonte_bloqueada()` — reputação do canal.
3. `cinematica_isolada()` — cutscene sem fala/comentário por cima = **bloqueio**
   (regra da Rockstar).
4. `nivel_violencia()` — analisa amostras de frames (vermelho/sangue, movimento
   brusco) + palavras da transcrição → descartar, borrar ou avisar. Conta a
   **duração acumulada** de cenas gráficas, como a política do YouTube faz.
5. `spoiler()` — lista de termos de enredo/final → bloqueio.

**Reputação de fontes (SQLite):** cada canal guarda
`videos_gerados, claims, restricoes_idade, remocoes, status`. Um claim ou
restrição → rebaixa para `bloqueado` automaticamente e avisa no painel.

---

## 7. Onde entram as chaves de API (resumo)

| Serviço | Obrigatório? | Para quê | Custo |
|---|---|---|---|
| YouTube Data API v3 | só para publicar | upload não-listado | grátis (cota diária) |
| TikTok Content Posting | opcional | enviar rascunho | grátis, exige app aprovado |
| Anthropic/OpenAI | opcional | gancho, roteiro, metadados | ~US$ 0,01–0,03/vídeo |
| ElevenLabs | opcional | voz do Modo B | ~US$ 0,02–0,05/vídeo (Edge-TTS é grátis) |

Todas ficam em `config/config.yaml`, que **nunca** vai para o Git
(já está no `.gitignore`).
