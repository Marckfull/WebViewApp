#!/usr/bin/env python3
"""
NeuroFlip - Fabrica de Conteudo
================================
Gera, de forma procedural e infinita, os criativos diarios para alimentar
Reels / Shorts / TikTok / Stories / feed, ja com legenda, hashtags, CTA e
calendario de publicacao.

Zero dependencias externas (so a stdlib). Saida em SVG (vetorial, abre no
navegador, importa no Canva/Figma/CapCut) + HTML de preview + CSV de calendario.

Uso:
    python3 growth/tools/content_factory.py --days 30 --start 2026-09-01
    python3 growth/tools/content_factory.py --days 7 --seed 42 --lang pt
    python3 growth/tools/content_factory.py --days 14 --formats story,feed

Depois:  abra growth/out/<data-de-geracao>/index.html no navegador.
"""

import argparse
import csv
import hashlib
import html
import json
import math
import os
import random
from datetime import date, datetime, timedelta

# --------------------------------------------------------------------------
# Identidade visual
# --------------------------------------------------------------------------
BG1, BG2 = "#0B0E1A", "#171E3A"
FG, MUTED = "#F5F7FF", "#8E9AC4"
ACCENT, ACCENT2, OK = "#5B8CFF", "#FF5B7F", "#3DDC97"
FONT = "DejaVu Sans, Liberation Sans, Helvetica Neue, Arial, sans-serif"

INK = {
    "VERMELHO": "#FF4D4D", "AZUL": "#4D8DFF", "VERDE": "#3DDC97",
    "AMARELO": "#FFD84D", "ROXO": "#B36DFF", "LARANJA": "#FF9F43",
}
INK_EN = {
    "RED": "#FF4D4D", "BLUE": "#4D8DFF", "GREEN": "#3DDC97",
    "YELLOW": "#FFD84D", "PURPLE": "#B36DFF", "ORANGE": "#FF9F43",
}

FORMATS = {           # nome        largura altura   onde usar
    "story": (1080, 1920),   # Reels / Shorts / TikTok / Stories
    "feed":  (1080, 1350),   # feed Instagram / Facebook / Pinterest
    "square": (1080, 1080),  # X / LinkedIn / thumbnail
}

# --------------------------------------------------------------------------
# Copy: ganchos e legendas (o gancho e' o que segura os 3 primeiros segundos)
# --------------------------------------------------------------------------
HOOKS = {
    "pt": [
        "So 3% acertam em 5 segundos",
        "Seu cerebro trava nessa",
        "Se voce errar, e normal",
        "Teste rapido de atencao",
        "Voce consegue em 5s?",
        "Nivel: dificil",
        "Comente a resposta antes de ver",
        "90% erram a primeira",
        "Quanto tempo voce levou?",
        "Isso mede seu foco real",
    ],
    "en": [
        "Only 3% get this in 5 seconds",
        "Your brain will glitch here",
        "Missing it is normal",
        "Quick attention test",
        "Can you do it in 5s?",
        "Difficulty: hard",
        "Comment before you see it",
        "90% miss the first one",
        "How fast were you?",
        "This measures real focus",
    ],
}

CTA = {
    "pt": ["Treine todo dia no NeuroFlip", "NeuroFlip - de graca na Play Store",
           "Serie de 7 dias no NeuroFlip", "Bate seu recorde no NeuroFlip"],
    "en": ["Train daily on NeuroFlip", "NeuroFlip - free on Google Play",
           "7-day streak on NeuroFlip", "Beat your record on NeuroFlip"],
}

CAPTION_TMPL = {
    "pt": (
        "{hook}\n\n"
        "{prompt}\n\n"
        "Responde nos comentarios SEM voltar o vidio. "
        "Quem acerta em menos de 5s tem atencao seletiva acima da media.\n\n"
        "Amanha tem outro. Salva pra nao perder.\n"
        "{cta}\n\n{tags}"
    ),
    "en": (
        "{hook}\n\n"
        "{prompt}\n\n"
        "Answer in the comments WITHOUT rewinding. "
        "Under 5s means above-average selective attention.\n\n"
        "New one tomorrow. Save it.\n"
        "{cta}\n\n{tags}"
    ),
}

TAGS = {
    "pt": ["#desafio", "#testedeqi", "#memoria", "#foco", "#cerebro",
           "#treinocerebral", "#quiz", "#desafiovisual", "#atencao", "#neuroflip"],
    "en": ["#brainteaser", "#iqtest", "#memory", "#focus", "#brain",
           "#braintraining", "#quiz", "#visualpuzzle", "#attention", "#neuroflip"],
}

# Melhores janelas de postagem (BRT) por plataforma - ponto de partida,
# ajuste com os dados reais do Insights depois de 2 semanas.
SLOTS = [
    ("TikTok",           "12:30"),
    ("Instagram Reels",  "19:00"),
    ("YouTube Shorts",   "18:00"),
    ("Instagram Stories", "21:00"),
    ("Pinterest",        "10:00"),
    ("X / Threads",      "13:00"),
]


# --------------------------------------------------------------------------
# Helpers de SVG
# --------------------------------------------------------------------------
def esc(t):
    return html.escape(str(t), quote=True)


def text(x, y, s, size=48, fill=FG, weight="700", anchor="middle", spacing="0"):
    return (f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" '
            f'letter-spacing="{spacing}">{esc(s)}</text>')


def wrap(s, width):
    """Quebra de linha simples por contagem de caracteres."""
    words, lines, cur = s.split(), [], ""
    for w in words:
        cand = (cur + " " + w).strip()
        if len(cand) > width and cur:
            lines.append(cur)
            cur = w
        else:
            cur = cand
    if cur:
        lines.append(cur)
    return lines


# Largura media de um glifo em fracao do corpo, para sans-serif bold.
CHAR_W = 0.58


def fit_block(cx, y, s, size, max_w, fill=FG, weight="700", lh=1.22,
              max_lines=3, min_size=26):
    """Quebra e, se preciso, encolhe o corpo ate o bloco caber em max_w.
    Sem isso, ganchos longos vazam a lateral do card."""
    if not s:
        return ""
    while True:
        per_line = max(8, int(max_w / (size * CHAR_W)))
        lines = wrap(s, per_line)
        longest = max(len(ln) for ln in lines)
        if (len(lines) <= max_lines and longest * size * CHAR_W <= max_w) \
                or size <= min_size:
            break
        size -= 2
    return "\n".join(text(cx, y + i * size * lh, ln, size, fill, weight)
                     for i, ln in enumerate(lines))


def tri(cx, cy, r, rot=0, fill=ACCENT):
    pts = []
    for k in range(3):
        a = math.radians(rot - 90 + k * 120)
        pts.append(f"{cx + r * math.cos(a):.1f},{cy + r * math.sin(a):.1f}")
    return f'<polygon points="{" ".join(pts)}" fill="{fill}"/>'


def sq(cx, cy, r, rot=0, fill=ACCENT):
    return (f'<rect x="{cx-r:.1f}" y="{cy-r:.1f}" width="{2*r}" height="{2*r}" '
            f'rx="{r*0.22:.1f}" fill="{fill}" transform="rotate({rot} {cx} {cy})"/>')


def circ(cx, cy, r, fill=ACCENT):
    return f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r:.1f}" fill="{fill}"/>'


# --------------------------------------------------------------------------
# Geradores de desafio
# Cada um recebe (rng, lang, box) e devolve dict:
#   prompt, answer, art (svg do miolo), kind, reveal (svg opcional do card 2)
# box = (x0, y0, x1, y1) da area util central
# --------------------------------------------------------------------------
def g_stroop(rng, lang, box):
    palette = INK if lang == "pt" else INK_EN
    names = list(palette)
    word = rng.choice(names)
    ink = rng.choice([n for n in names if n != word])
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    art = text(cx, cy + 40, word, size=140, fill=palette[ink], spacing="4")
    prompt = ("Qual e a COR da palavra? (nao leia, olhe)" if lang == "pt"
              else "What COLOR is the word? (don't read it)")
    return dict(kind="stroop", prompt=prompt, answer=ink, art=art, reveal=None)


def fit_grid(box, cols, target_cells):
    """Escolhe o numero de linhas que deixa as celulas mais proximas do quadrado."""
    x0, y0, x1, y1 = box
    cw = (x1 - x0) / cols
    rows = max(3, min(target_cells // cols + 1, int(round((y1 - y0) / cw))))
    return cols, rows


def g_odd_one_out(rng, lang, box):
    x0, y0, x1, y1 = box
    cols, rows = fit_grid(box, 5, 30)
    cw, ch = (x1 - x0) / cols, (y1 - y0) / rows
    ch = min(ch, cw)
    y0 = (y0 + y1) / 2 - ch * rows / 2
    r = min(cw, ch) * 0.30
    tgt_c, tgt_r = rng.randrange(cols), rng.randrange(rows)
    base_rot = rng.choice([0, 15, 30])
    shape = rng.choice(["tri", "sq"])
    parts = []
    for rr in range(rows):
        for cc in range(cols):
            cx, cy = x0 + cw * (cc + .5), y0 + ch * (rr + .5)
            odd = (cc == tgt_c and rr == tgt_r)
            rot = base_rot + (rng.choice([28, -28]) if odd else 0)
            col = ACCENT2 if odd and rng.random() < .35 else ACCENT
            parts.append(tri(cx, cy, r, rot, col) if shape == "tri"
                         else sq(cx, cy, r, rot, col))
    prompt = ("Ache o unico diferente" if lang == "pt" else "Find the odd one out")
    ans = (f"linha {tgt_r+1}, coluna {tgt_c+1}" if lang == "pt"
           else f"row {tgt_r+1}, column {tgt_c+1}")
    return dict(kind="odd_one_out", prompt=prompt, answer=ans,
                art="\n".join(parts), reveal=None)


def g_count(rng, lang, box):
    x0, y0, x1, y1 = box
    cols, rows = fit_grid(box, 6, 42)
    cw, ch = (x1 - x0) / cols, (y1 - y0) / rows
    ch = min(ch, cw)
    y0 = (y0 + y1) / 2 - ch * rows / 2
    r = min(cw, ch) * 0.30
    n_target = rng.randint(max(5, cols * rows // 5), max(7, cols * rows // 3))
    slots = [(c, rr) for rr in range(rows) for c in range(cols)]
    rng.shuffle(slots)
    target = set(slots[:n_target])
    parts = []
    for rr in range(rows):
        for cc in range(cols):
            cx, cy = x0 + cw * (cc + .5), y0 + ch * (rr + .5)
            if (cc, rr) in target:
                parts.append(circ(cx, cy, r, ACCENT2))
            else:
                parts.append(rng.choice([
                    sq(cx, cy, r * .95, rng.choice([0, 45]), ACCENT),
                    tri(cx, cy, r * 1.05, rng.choice([0, 180]), ACCENT),
                ]))
    prompt = ("Quantos CIRCULOS tem na tela?" if lang == "pt"
              else "How many CIRCLES are on screen?")
    return dict(kind="count", prompt=prompt, answer=str(n_target),
                art="\n".join(parts), reveal=None)


def g_sequence(rng, lang, box):
    x0, y0, x1, y1 = box
    rule = rng.choice(["add", "mul", "fib", "alt", "sqr"])
    a = rng.randint(2, 9)
    if rule == "add":
        d = rng.randint(3, 11)
        seq = [a + d * i for i in range(6)]
    elif rule == "mul":
        m = rng.choice([2, 3])
        seq = [a * m ** i for i in range(6)]
    elif rule == "fib":
        seq = [a, rng.randint(2, 9)]
        while len(seq) < 6:
            seq.append(seq[-1] + seq[-2])
    elif rule == "sqr":
        seq = [(a + i) ** 2 for i in range(6)]
    else:
        d1, d2 = rng.randint(2, 7), rng.randint(8, 15)
        seq, cur = [a], a
        for i in range(5):
            cur += d1 if i % 2 == 0 else d2
            seq.append(cur)
    shown, ans = seq[:5], seq[5]
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    parts, n = [], 6
    bw = (x1 - x0) / n
    for i in range(n):
        bx = x0 + bw * i + bw * .5
        val = str(shown[i]) if i < 5 else "?"
        fill = "#1E2647" if i < 5 else "#2A1A33"
        stroke = ACCENT if i < 5 else ACCENT2
        parts.append(f'<rect x="{bx-bw*.42:.1f}" y="{cy-90}" width="{bw*.84:.1f}" '
                     f'height="180" rx="28" fill="{fill}" stroke="{stroke}" stroke-width="4"/>')
        parts.append(text(bx, cy + 26, val, size=min(70, int(bw * .5)),
                          fill=FG if i < 5 else ACCENT2))
    prompt = ("Qual e o proximo numero?" if lang == "pt" else "What comes next?")
    return dict(kind="sequence", prompt=prompt, answer=str(ans),
                art="\n".join(parts), reveal=None)


def g_memory_flash(rng, lang, box):
    """Card em 2 tempos: memorize -> responda. Perfeito para video."""
    x0, y0, x1, y1 = box
    n = 3
    cw = (x1 - x0) / n
    ch = min(cw, (y1 - y0) / n)
    top = (y0 + y1) / 2 - ch * n / 2
    grid = [[rng.randint(1, 9) for _ in range(n)] for _ in range(n)]
    hidden = rng.sample([(r, c) for r in range(n) for c in range(n)], 2)

    def draw(hide):
        p = []
        for r in range(n):
            for c in range(n):
                cx, cy = x0 + cw * (c + .5), top + ch * (r + .5)
                is_h = hide and (r, c) in hidden
                p.append(f'<rect x="{cx-cw*.40:.1f}" y="{cy-ch*.40:.1f}" '
                         f'width="{cw*.80:.1f}" height="{ch*.80:.1f}" rx="26" '
                         f'fill="{"#2A1A33" if is_h else "#1E2647"}" '
                         f'stroke="{ACCENT2 if is_h else ACCENT}" stroke-width="4"/>')
                p.append(text(cx, cy + 30, "?" if is_h else grid[r][c],
                              size=int(ch * .45), fill=ACCENT2 if is_h else FG))
        return "\n".join(p)

    ans = ", ".join(str(grid[r][c]) for r, c in hidden)
    prompt = ("Memorize 5 segundos" if lang == "pt" else "Memorize for 5 seconds")
    rev = ("Quais numeros sumiram?" if lang == "pt" else "Which numbers vanished?")
    return dict(kind="memory_flash", prompt=prompt, answer=ans,
                art=draw(False), reveal=draw(True), reveal_prompt=rev)


def g_mirror(rng, lang, box):
    x0, y0, x1, y1 = box
    n_pts = rng.randint(5, 7)
    angles = sorted(rng.uniform(0, 2 * math.pi) for _ in range(n_pts))
    # afasta angulos muito proximos para nao criar "lascas"
    angles = [a + i * 0.35 for i, a in enumerate(angles)]
    seed_pts = [(math.cos(a) * r, math.sin(a) * r)
                for a, r in ((a, rng.uniform(0.55, 1.0)) for a in angles)]

    def poly(cx, cy, s, flip=False, rot=0, fill=ACCENT):
        pts = []
        for (px, py) in seed_pts:
            X, Y = (-px if flip else px), py
            a = math.radians(rot)
            rx = X * math.cos(a) - Y * math.sin(a)
            ry = X * math.sin(a) + Y * math.cos(a)
            pts.append(f"{cx + rx*s:.1f},{cy + ry*s:.1f}")
        return f'<polygon points="{" ".join(pts)}" fill="{fill}"/>'

    cx, cy = (x0 + x1) / 2, y0 + (y1 - y0) * 0.27
    s = min(x1 - x0, y1 - y0) * 0.16
    parts = [poly(cx, cy, s, False, 0, FG)]
    correct = rng.randrange(4)
    oy = y0 + (y1 - y0) * 0.72
    ow = (x1 - x0) / 4
    for i in range(4):
        ox = x0 + ow * (i + .5)
        parts.append(f'<rect x="{ox-ow*.44:.1f}" y="{oy-ow*.48:.1f}" '
                     f'width="{ow*.88:.1f}" height="{ow*.96:.1f}" rx="24" '
                     f'fill="#141A33" stroke="{ACCENT}" stroke-width="3"/>')
        parts.append(poly(ox, oy, s * .55, i == correct,
                          0 if i == correct else rng.choice([25, -40, 90]), ACCENT))
        parts.append(text(ox, oy + ow * .44 + 46, "ABCD"[i], size=44, fill=MUTED))
    prompt = ("Qual e o espelho da forma de cima?" if lang == "pt"
              else "Which one is the mirror of the top shape?")
    return dict(kind="mirror", prompt=prompt, answer="ABCD"[correct],
                art="\n".join(parts), reveal=None)


GENERATORS = [g_stroop, g_odd_one_out, g_count, g_sequence, g_memory_flash, g_mirror]


# --------------------------------------------------------------------------
# Montagem do card
# --------------------------------------------------------------------------
def art_box(W, H):
    """Area util central onde o desafio e desenhado. Fonte unica de verdade."""
    return (90, int(H * 0.30), W - 90, int(H * 0.80))


def render(W, H, hook, prompt, art_svg, cta, badge):
    pad = 70
    hook_y = int(H * 0.115)
    prompt_y = int(H * 0.225)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="0.4" y2="1">
    <stop offset="0%" stop-color="{BG1}"/><stop offset="100%" stop-color="{BG2}"/>
  </linearGradient>
  <linearGradient id="acc" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0%" stop-color="{ACCENT}"/><stop offset="100%" stop-color="{ACCENT2}"/>
  </linearGradient>
</defs>
<rect width="{W}" height="{H}" fill="url(#bg)"/>
<rect x="0" y="0" width="{W}" height="10" fill="url(#acc)"/>
<g opacity="0.9">{text(W/2, pad + 46, badge, 34, MUTED, "700", spacing="6")}</g>
{fit_block(W/2, hook_y + 40, hook, 62, W - 2*pad, FG, "800", max_lines=2)}
{fit_block(W/2, prompt_y + 34, prompt, 44, W - 2*pad, ACCENT, "700", max_lines=2)}
{art_svg}
{fit_block(W/2, int(H*0.885), cta, 40, W - 2*pad, MUTED, "600", max_lines=1)}
{text(W/2, int(H*0.885) + 74, "NEUROFLIP", 46, FG, "800", spacing="10")}
</svg>'''


def make_card(rng, lang, day, idx, fmt, gen=None, sub_seed=None):
    """Gera um card. Passando `gen` e `sub_seed` iguais, formatos diferentes
    produzem o MESMO desafio - e o que garante que o Story e o Feed do dia
    contem a mesma peca, so que enquadrada para cada rede."""
    W, H = FORMATS[fmt]
    gen = gen or rng.choice(GENERATORS)
    grng = random.Random(sub_seed if sub_seed is not None else rng.random())
    hook = grng.choice(HOOKS[lang])
    cta = grng.choice(CTA[lang])
    badge = day.strftime("%d/%m") if lang == "pt" else day.strftime("%b %d")
    probe = gen(grng, lang, art_box(W, H))
    svg = render(W, H, hook, probe["prompt"], probe["art"], cta, badge)
    reveal_svg = None
    if probe.get("reveal"):
        reveal_svg = render(W, H, probe.get("reveal_prompt", "?"),
                            "", probe["reveal"], cta, badge)
    cid = f"{day.isoformat()}-{idx:02d}-{probe['kind']}-{fmt}"
    return dict(cid=cid, kind=probe["kind"], hook=hook, prompt=probe["prompt"],
                answer=probe["answer"], svg=svg, reveal=reveal_svg, cta=cta, fmt=fmt)


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Fabrica de conteudo do NeuroFlip")
    ap.add_argument("--days", type=int, default=30, help="quantos dias gerar")
    ap.add_argument("--per-day", type=int, default=1, help="pecas por dia")
    ap.add_argument("--start", default=None, help="data inicial YYYY-MM-DD")
    ap.add_argument("--lang", default="pt", choices=["pt", "en"])
    ap.add_argument("--seed", type=int, default=None)
    ap.add_argument("--formats", default="story,feed",
                    help="story,feed,square separados por virgula")
    ap.add_argument("--out", default=None, help="pasta de saida")
    args = ap.parse_args()

    start = (datetime.strptime(args.start, "%Y-%m-%d").date()
             if args.start else date.today())
    seed = args.seed if args.seed is not None else int(start.strftime("%Y%m%d"))
    rng = random.Random(seed)
    fmts = [f.strip() for f in args.formats.split(",") if f.strip() in FORMATS]

    root = args.out or os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), "out", f"{start.isoformat()}-{args.lang}")
    os.makedirs(root, exist_ok=True)

    # baralho: cada tipo de desafio aparece uma vez antes de repetir,
    # para a semana nao virar 4 sequencias numericas seguidas
    deck = []

    def next_generator():
        if not deck:
            d = GENERATORS[:]
            rng.shuffle(d)
            deck.extend(d)
        return deck.pop()

    rows, captions, gallery = [], [], []
    for d in range(args.days):
        day = start + timedelta(days=d)
        for i in range(args.per_day):
            primary = None
            day_gen = next_generator()
            day_seed = rng.random()
            for fmt in fmts:
                card = make_card(rng, args.lang, day, i, fmt,
                                 gen=day_gen, sub_seed=day_seed)
                if primary is None:
                    primary = card
                fn = f"{card['cid']}.svg"
                with open(os.path.join(root, fn), "w", encoding="utf-8") as fh:
                    fh.write(card["svg"])
                if card["reveal"]:
                    with open(os.path.join(root, f"{card['cid']}-resposta.svg"),
                              "w", encoding="utf-8") as fh:
                        fh.write(card["reveal"])
                if fmt == fmts[0]:
                    gallery.append((fn, card))

            tags = " ".join(rng.sample(TAGS[args.lang], 7))
            cap = CAPTION_TMPL[args.lang].format(
                hook=primary["hook"], prompt=primary["prompt"],
                cta=primary["cta"], tags=tags)
            platform, hour = SLOTS[(d + i) % len(SLOTS)]
            captions.append((day, primary, cap, platform, hour, tags))
            rows.append({
                "data": day.isoformat(), "horario_brt": hour, "plataforma": platform,
                "tipo": primary["kind"], "arquivo": f"{primary['cid']}.svg",
                "gancho": primary["hook"], "pergunta": primary["prompt"],
                "resposta": primary["answer"], "cta": primary["cta"],
                "hashtags": tags, "status": "a_produzir",
            })

    # calendario
    with open(os.path.join(root, "calendario.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)

    # legendas
    with open(os.path.join(root, "legendas.md"), "w", encoding="utf-8") as fh:
        fh.write(f"# Legendas prontas - {start.isoformat()} ({args.lang})\n\n")
        for day, card, cap, platform, hour, tags in captions:
            fh.write(f"## {day.isoformat()} - {platform} - {hour} BRT\n\n")
            fh.write(f"**Arquivo:** `{card['cid']}.svg`  \n")
            fh.write(f"**Resposta (fixar no comentario depois de 1h):** {card['answer']}\n\n")
            fh.write("```\n" + cap + "\n```\n\n---\n\n")

    # preview
    cards_html = "\n".join(
        f'<figure><img src="{esc(fn)}" alt="{esc(c["prompt"])}">'
        f'<figcaption><b>{esc(c["kind"])}</b><br>{esc(c["hook"])}<br>'
        f'<span>resposta: {esc(c["answer"])}</span></figcaption></figure>'
        for fn, c in gallery)
    with open(os.path.join(root, "index.html"), "w", encoding="utf-8") as fh:
        fh.write(f"""<!doctype html><meta charset="utf-8">
<title>NeuroFlip - conteudo {start.isoformat()}</title>
<style>
body{{background:{BG1};color:{FG};font-family:{FONT};margin:0;padding:32px}}
h1{{font-size:22px;letter-spacing:.04em}} p{{color:{MUTED}}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:22px}}
figure{{margin:0;background:#141A33;border-radius:16px;overflow:hidden}}
img{{width:100%;display:block}}
figcaption{{padding:12px;font-size:12px;line-height:1.5}}
figcaption span{{color:{OK}}}
</style>
<h1>NeuroFlip - {len(gallery)} pecas geradas ({start.isoformat()}, {args.lang})</h1>
<p>Clique com o botao direito &rarr; salvar imagem, ou abra o SVG no Canva/CapCut.
Calendario: <code>calendario.csv</code> &middot; Legendas: <code>legendas.md</code></p>
<div class="grid">{cards_html}</div>""")

    print(f"OK  {len(rows)} dias x {len(fmts)} formatos -> {root}")
    print(f"    abra: {os.path.join(root, 'index.html')}")


if __name__ == "__main__":
    main()
