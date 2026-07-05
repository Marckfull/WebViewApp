#!/usr/bin/env python3
"""Gera sprites de inimigos, boss, NPCs, ícones de itens e props,
além dos SpriteFrames (.tres) dos inimigos.

Rode da raiz do repositório:  python3 tools/generate_world_sprites.py
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

from generate_aria_sprites import DOWN_BASE, PALETTE as ARIA_PALETTE

ROOT = Path(__file__).resolve().parent.parent / "game"
SPRITES = ROOT / "assets/sprites"


def render(rows, palette, canvas, y_shift=0):
    width = len(rows[0])
    for r in rows:
        assert len(r) == width, f"largura errada: {r!r}"
        assert set(r) <= set(palette), f"char desconhecido: {r!r}"
    img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    x0 = (canvas - width) // 2
    y0 = canvas - len(rows) - 2 + y_shift
    for y, row in enumerate(rows):
        for x, char in enumerate(row):
            color = palette[char]
            if color and 0 <= x0 + x < canvas and 0 <= y0 + y < canvas:
                img.putpixel((x0 + x, y0 + y), color)
    return img


# ---------------------------------------------------------------- inimigos

ECOADO_PALETTE = {
    ".": None,
    "o": (30, 22, 34, 255),
    "r": (140, 55, 85, 255),    # corpo
    "R": (100, 38, 62, 255),    # corpo sombra
    "E": (200, 235, 240, 255),  # olhos vazios
}

ECOADO = [
    "...oooooo...",
    "..orrrrrro..",
    ".orrrrrrrro.",
    ".orErrrrEro.",
    ".orrrrrrrro.",
    ".orrrRRrrro.",
    ".orrRRRRrro.",
    "..orRRRRro..",
    "..orr..rro..",
    "..oo....oo..",
]

BRUTA_PALETTE = {
    ".": None,
    "o": (26, 18, 32, 255),
    "v": (115, 56, 128, 255),   # corpo violeta
    "V": (82, 38, 94, 255),     # sombra
    "E": (235, 210, 160, 255),  # olhos
}

BRUTAMONTES = [
    ".....oooooooo.....",
    "...oovvvvvvvvoo...",
    "..ovvvvvvvvvvvvo..",
    "..ovvEvvvvvvEvvo..",
    ".ovvvvvvvvvvvvvvo.",
    ".ovvvvvVVVVvvvvvo.",
    ".ovvvVVVVVVVVvvvo.",
    ".ovvVVVVVVVVVVvvo.",
    ".ovvVVVVVVVVVVvvo.",
    "..ovVVVVVVVVVVvo..",
    "..ovvo.VVVV.ovvo..",
    "..ovvo.VVVV.ovvo..",
    "...oo..oooo..oo...",
]

ESPREITADOR_PALETTE = {
    ".": None,
    "o": (22, 34, 26, 255),
    "r": (88, 158, 96, 255),    # corpo verde
    "R": (56, 112, 66, 255),    # sombra
    "E": (240, 230, 140, 255),  # olhos amarelos
}

ALFA_PALETTE = {
    ".": None,
    "o": (18, 26, 20, 255),
    "f": (86, 118, 94, 255),    # pelagem
    "F": (58, 84, 64, 255),     # pelagem sombra
    "e": (216, 244, 140, 255),  # olho
}

ALFA = [
    "..............oo....",
    ".....oooooo..offo...",
    "....offffffooofffo..",
    "...offffffffffffefo.",
    "..offffffffffffffoo.",
    "..oFffffffffffffffo.",
    "..oFFffffffffffffo..",
    "...oFFFFFFFFFFFFo...",
    "...off..off...ffo...",
    "...off..off...ffo...",
    "...oo...oo....oo....",
]

SENTINELA_PALETTE = {
    ".": None,
    "o": (24, 42, 54, 255),
    "r": (126, 206, 230, 255),  # corpo ciano-vento
    "R": (84, 154, 186, 255),
    "E": (245, 255, 255, 255),  # olhos claros
}

CARCACA_PALETTE = {
    ".": None,
    "o": (40, 38, 44, 255),
    "r": (216, 212, 224, 255),  # osso e sal
    "R": (168, 164, 182, 255),
    "E": (152, 62, 66, 255),     # olhos secos
}

FORJADO_PALETTE = {
    ".": None,
    "o": (28, 30, 38, 255),
    "r": (150, 158, 172, 255),  # armadura
    "R": (100, 108, 122, 255),
    "E": (255, 150, 60, 255),   # brasas nos olhos
}

GOLEM_PALETTE = {
    ".": None,
    "o": (30, 32, 40, 255),
    "v": (150, 160, 175, 255),  # ferro
    "V": (105, 115, 130, 255),
    "E": (255, 150, 60, 255),   # brasas
}

BOSS_PALETTE = {
    ".": None,
    "o": (30, 30, 48, 255),
    "h": (208, 218, 240, 255),  # cabelo/véu espectral
    "H": (160, 172, 205, 255),
    "s": (225, 220, 230, 255),  # pele pálida
    "e": (120, 200, 220, 255),  # olhos brilhantes
    "c": (238, 230, 200, 255),  # colar
    "t": (128, 148, 200, 255),  # veste
    "T": (92, 108, 158, 255),
    "w": (222, 226, 238, 255),  # lâmina
    "W": (160, 168, 190, 255),
    "g": (90, 62, 40, 255),
}

BOSS_VENTO_PALETTE = {
    ".": None,
    "o": (28, 44, 52, 255),
    "h": (198, 238, 246, 255),  # véu de vento
    "H": (150, 200, 214, 255),
    "s": (224, 240, 240, 255),
    "e": (120, 235, 220, 255),
    "c": (232, 246, 220, 255),
    "t": (120, 196, 208, 255),  # veste ciano
    "T": (86, 150, 166, 255),
    "w": (232, 246, 250, 255),
    "W": (168, 200, 208, 255),
    "g": (90, 74, 60, 255),
}

BOSS_SAL_PALETTE = {
    ".": None,
    "o": (44, 44, 54, 255),
    "h": (234, 240, 246, 255),  # coroa de sal
    "H": (190, 205, 216, 255),
    "s": (238, 234, 240, 255),
    "e": (150, 220, 235, 255),
    "c": (240, 246, 250, 255),
    "t": (200, 214, 224, 255),  # manto cristalino
    "T": (150, 172, 186, 255),
    "w": (240, 248, 250, 255),
    "W": (180, 200, 210, 255),
    "g": (120, 110, 100, 255),
}

# Selene, a Guardiã Caída — cores mudas de violeta e cinza (ela é o Silêncio).
BOSS_SELENE_PALETTE = {
    ".": None,
    "o": (30, 26, 38, 255),
    "h": (122, 110, 152, 255),
    "H": (88, 80, 116, 255),
    "s": (182, 172, 198, 255),
    "e": (232, 120, 152, 255),  # olhos de luto
    "c": (150, 140, 172, 255),
    "t": (98, 86, 130, 255),
    "T": (66, 56, 92, 255),
    "w": (212, 202, 226, 255),
    "W": (150, 140, 172, 255),
    "g": (80, 70, 60, 255),
}

BOSS = [
    ".....oooooo.....",
    "....ohhhhhho....",
    "...ohhhhhhhho...",
    "...ohssssssho...",
    "...ohsessesho...",
    "...oHssssssHo...",
    "..oh.ossso.ho...",
    "..ohocccccohho..",
    ".ohottttttttoho.",
    ".ohotttttttoho..",
    ".oho.tttttt.oho.",
    ".oh.otttttto.ho.",
    "....otttttto..g.",
    "....oTTTTTTo..w.",
    "...otttttttto.w.",
    "...otttttttto.w.",
    "..otttttttttoWw.",
    "..oTTTTTTTTTo.W.",
    ".oTTTTTTTTTTTo..",
    "..oooooooooooo..",
]

# ------------------------------------------------------------------- NPCs


def npc_palette(hair, tunic, scarf):
    p = dict(ARIA_PALETTE)
    p["h"] = hair
    p["H"] = tuple(int(c * 0.72) for c in hair[:3]) + (255,)
    p["t"] = tunic
    p["T"] = tuple(int(c * 0.72) for c in tunic[:3]) + (255,)
    p["c"] = scarf
    return p


NPCS = {
    "odara": npc_palette((168, 158, 150, 255), (152, 82, 52, 255), (90, 82, 86, 255)),
    "corvo": npc_palette((42, 42, 58, 255), (58, 60, 82, 255), (30, 30, 40, 255)),
    "sela": npc_palette((202, 172, 96, 255), (110, 152, 84, 255), (238, 234, 224, 255)),
}

# ------------------------------------------------------------------ itens

ICON_PALETTES = {
    "minerio_eco": {
        ".": None,
        "o": (30, 26, 44, 255),
        "c": (110, 200, 220, 255),
        "C": (70, 140, 170, 255),
        "w": (230, 250, 255, 255),
    },
    "erva_lunar": {
        ".": None,
        "o": (24, 36, 26, 255),
        "g": (110, 180, 96, 255),
        "G": (70, 130, 66, 255),
        "w": (225, 240, 200, 255),
    },
    "gancho_corda": {
        ".": None,
        "o": (30, 28, 36, 255),
        "m": (168, 176, 190, 255),
        "M": (110, 118, 134, 255),
        "r": (150, 108, 66, 255),
    },
    "frasco_essencia": {
        ".": None,
        "o": (40, 44, 60, 255),
        "a": (150, 108, 66, 255),
        "l": (110, 220, 130, 255),
        "L": (76, 170, 96, 255),
        "w": (235, 255, 240, 255),
    },
    "memoria_lys": {
        ".": None,
        "o": (52, 30, 46, 255),
        "p": (238, 142, 192, 255),
        "P": (188, 96, 148, 255),
        "w": (255, 235, 248, 255),
    },
    "amuleto_eco": {
        ".": None,
        "o": (60, 26, 34, 255),
        "r": (222, 84, 104, 255),
        "w": (255, 220, 228, 255),
    },
    "amuleto_vento": {
        ".": None,
        "o": (26, 48, 54, 255),
        "c": (120, 214, 228, 255),
    },
    "talisma_sela": {
        ".": None,
        "o": (44, 38, 26, 255),
        "p": (232, 150, 190, 255),
        "w": (250, 226, 130, 255),
    },
    "bomba_eco": {
        ".": None,
        "o": (26, 28, 36, 255),
        "b": (52, 56, 70, 255),
        "w": (150, 220, 235, 255),
    },
    "lente_verdade": {
        ".": None,
        "o": (40, 40, 56, 255),
        "a": (150, 120, 66, 255),
        "g": (170, 230, 240, 255),
        "w": (245, 255, 255, 255),
    },
    "botas_corrente": {
        ".": None,
        "o": (34, 30, 40, 255),
        "b": (128, 92, 60, 255),
        "B": (90, 64, 42, 255),
        "m": (150, 200, 220, 255),
    },
    "adaga": {
        ".": None,
        "o": (38, 36, 46, 255),
        "w": (226, 232, 244, 255),
        "W": (160, 168, 190, 255),
        "g": (120, 82, 50, 255),
    },
    "martelo": {
        ".": None,
        "o": (34, 32, 40, 255),
        "m": (150, 158, 172, 255),
        "M": (104, 110, 126, 255),
        "g": (120, 82, 50, 255),
    },
}

ICONS = {
    "frasco_essencia": [
        "...oo....",
        "...oao...",
        "..o..o...",
        ".o....o..",
        ".o.ll.o..",
        ".olwllo..",
        ".olllLo..",
        ".oLLLLo..",
        "..oooo...",
    ],
    "memoria_lys": [
        "....o....",
        "...opo...",
        "..opwpo..",
        ".opppPo..",
        ".oppPPo..",
        "..oPPo...",
        "...oo....",
    ],
    "minerio_eco": [
        "....o....",
        "...oco...",
        "..ocwco..",
        ".ocwwcco.",
        ".occccCo.",
        ".oCccCCo.",
        "..oCCCo..",
        "...oCo...",
        "....o....",
    ],
    "erva_lunar": [
        "....o....",
        "...ogo...",
        "..ogwgo..",
        ".ogggggo.",
        ".ogGgGgo.",
        "..oGgGo..",
        "...oGo...",
        "...oGo...",
        "....o....",
    ],
    "gancho_corda": [
        "....oo...",
        "...omMo..",
        "...omo...",
        "...omo...",
        ".oomMoo..",
        "omm..mmo.",
        "oM....Mo.",
        ".oM..Mo..",
        "..oMMo...",
    ],
    "amuleto_eco": [
        "..oo..oo..",
        ".orrrrrro.",
        ".orwrrrro.",
        ".orrrrrro.",
        "..orrrro..",
        "...orro...",
        "....oo....",
    ],
    "amuleto_vento": [
        "...ooo...",
        "..occco..",
        ".oc..cco.",
        "....occo.",
        "...occo..",
        "..occo...",
        ".occo....",
        ".oo......",
    ],
    "talisma_sela": [
        "....o....",
        "..opppo..",
        ".oppwppo.",
        ".opwwwpo.",
        ".oppwppo.",
        "..opppo..",
        "....o....",
    ],
    "bomba_eco": [
        "......ow.",
        ".....oo..",
        "...oboo..",
        "..obbbbo.",
        ".obbbbbbo",
        ".obwbbbbo",
        ".obbbbbbo",
        "..obbbbo.",
        "...oooo..",
    ],
    "lente_verdade": [
        "...oooo..",
        "..oggggo.",
        ".oggwggo.",
        ".ogwwwgo.",
        ".oggwggo.",
        "..ogggo..",
        "...ooao..",
        ".....oao.",
        "......oo.",
    ],
    "botas_corrente": [
        ".oo...oo.",
        ".obo..obo",
        ".obo..obo",
        ".obo..obo",
        ".obbooobb",
        ".obbbbbbb",
        ".ommmmmmm",
        ".obbbbbbb",
        ".oooooooo",
    ],
    "adaga": [
        ".......oo",
        "......owo",
        ".....owwo",
        "....owwo.",
        "...owwo..",
        "..owwo...",
        ".oWWo....",
        "ogggo....",
        ".ogo.....",
    ],
    "martelo": [
        "oooooo...",
        "ommmmmo..",
        "omMMMmo..",
        "ommmmmo..",
        "ooogooo..",
        "...ogo...",
        "...ogo...",
        "...ogo...",
        "...ooo...",
    ],
}

PROP_PALETTES = {
    "grapple_post": {
        ".": None,
        "o": (28, 24, 30, 255),
        "a": (122, 86, 54, 255),
        "A": (88, 60, 38, 255),
        "m": (168, 176, 190, 255),
    },
    "anvil": {
        ".": None,
        "o": (30, 26, 30, 255),
        "m": (168, 176, 190, 255),
        "M": (110, 118, 134, 255),
        "A": (86, 66, 48, 255),
    },
    "ocarina": {
        ".": None,
        "o": (30, 30, 48, 255),
        "c": (150, 210, 230, 255),
        "C": (100, 160, 190, 255),
        "w": (235, 250, 255, 255),
    },
    "tree": {
        ".": None,
        "o": (26, 40, 26, 255),
        "g": (96, 168, 84, 255),
        "G": (66, 128, 58, 255),
        "a": (150, 108, 66, 255),
        "A": (108, 76, 46, 255),
    },
    "stand": {
        ".": None,
        "o": (40, 28, 26, 255),
        "r": (204, 74, 74, 255),
        "w": (240, 232, 214, 255),
        "a": (150, 108, 66, 255),
        "A": (108, 76, 46, 255),
    },
}

GRAPPLE_POST = [
    "..ommo..",
    ".om..mo.",
    ".om..mo.",
    "..ommo..",
    "..oaao..",
    "..oaao..",
    "..oAao..",
    "..oaao..",
    "..oAao..",
    "..oaao..",
    ".oAAAAo.",
    ".oooooo.",
]

ANVIL = [
    "ommmmmmmmmо.".replace("о", "o"),
    ".oMMMMMMMo..",
    "....oMMo....",
    "....oMMo....",
    "...oMMMMo...",
    "..oAAAAAAo..",
    "..oooooooo..",
]

OCARINA = [
    "....oo...",
    "...occo..",
    "..occcco.",
    ".occwccco",
    ".occccco.",
    "..oCCCo..",
    "...ooo...",
]

STAND = [
    "oooooooooooo",
    "orwrwrwrwrwo",
    "oooooooooooo",
    ".o........o.",
    ".o........o.",
    ".oaaaaaaaao.",
    ".oAAAAAAAAo.",
    ".o.o....o.o.",
    ".ooo....ooo.",
]

TREE = [
    ".....oooooo.....",
    "...ooggggggoo...",
    "..oggggggggggo..",
    ".oggggggggggggo.",
    ".oggggGGgggggго.".replace("г", "g").replace("о", "o"),
    "oggggGGGGGgggggo",
    "ogggGGGGGGGggggo",
    ".oGgGGGGGGGGgGo.",
    ".oGGGGGGGGGGGGo.",
    "..oGGGGGGGGGGo..",
    "...ooGGGGGGoo...",
    ".....oooooo.....",
    "......oAao......",
    "......oaao......",
    "......oAao......",
    "......oaao......",
    ".....oaAAao.....",
    ".....oooooo.....",
]

TILE_SPECS = {
    "grass": {
        "base": (107, 158, 71, 255),
        "dark": (86, 132, 58, 255),
        "light": (128, 178, 88, 255),
        "n_dark": 30, "n_light": 12,
    },
    "dirt": {
        "base": (199, 168, 107, 255),
        "dark": (172, 140, 86, 255),
        "light": (216, 188, 128, 255),
        "n_dark": 24, "n_light": 10,
    },
    "stone": {
        "base": (52, 47, 86, 255),
        "dark": (38, 34, 64, 255),
        "light": (66, 60, 104, 255),
        "n_dark": 14, "n_light": 10,
        "grid": 16,
    },
}


def make_cracked_wall():
    """Bloco de pedra rachado (destruível com Bomba de Eco)."""
    rng = random.Random("crack")
    img = Image.new("RGBA", (16, 16), (120, 122, 138, 255))
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), (84, 86, 100, 255))
    x = 8
    for y in range(1, 15):
        img.putpixel((x, y), (44, 44, 58, 255))
        if y in (4, 9, 12):
            img.putpixel((min(x + 1, 14), y), (44, 44, 58, 255))
        x = max(2, min(13, x + rng.choice([-1, 0, 1])))
    for x2 in range(3, 13, 2):
        img.putpixel((x2, 7), (58, 58, 74, 255))
    return img


# ------------------------------------------------------- retratos (busts)

BUST_PALETTES = {
    "aria": {"hair": (200, 96, 48), "hair2": (152, 64, 36), "skin": (252, 216, 168),
             "eye": (32, 40, 56), "tunic": (64, 192, 160), "collar": (248, 232, 168)},
    "odara": {"hair": (172, 164, 158), "hair2": (128, 120, 116), "skin": (232, 200, 170),
              "eye": (40, 44, 52), "tunic": (152, 82, 52), "collar": (96, 88, 92)},
    "corvo": {"hair": (46, 46, 62), "hair2": (28, 28, 40), "skin": (214, 184, 152),
              "eye": (28, 30, 40), "tunic": (60, 62, 86), "collar": (32, 32, 44)},
    "sela": {"hair": (206, 176, 100), "hair2": (156, 128, 66), "skin": (238, 208, 178),
             "eye": (56, 60, 44), "tunic": (110, 152, 84), "collar": (240, 236, 226)},
    "selene": {"hair": (124, 112, 154), "hair2": (88, 80, 116), "skin": (184, 174, 200),
               "eye": (232, 120, 152), "tunic": (98, 86, 130), "collar": (152, 142, 174)},
}

BUST_OUTLINE = (34, 30, 40, 255)


def _rgba(color):
    return color if len(color) == 4 else (color[0], color[1], color[2], 255)


def make_bust(pal):
    """Retrato 40x40 em pixel art: cabelo, rosto, olhos e ombros."""
    img = Image.new("RGBA", (40, 40), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    o = BUST_OUTLINE
    # ombros / veste
    d.rectangle([2, 33, 37, 39], fill=_rgba(pal["tunic"]), outline=o)
    d.rectangle([14, 32, 25, 37], fill=_rgba(pal["collar"]), outline=o)
    # cabelo (atrás)
    d.ellipse([6, 3, 33, 31], fill=_rgba(pal["hair"]), outline=o)
    # rosto
    d.ellipse([10, 9, 29, 32], fill=_rgba(pal["skin"]), outline=o)
    # franja
    d.pieslice([9, 5, 30, 26], start=180, end=360, fill=_rgba(pal["hair"]))
    d.line([10, 15, 29, 15], fill=_rgba(pal["hair2"]))
    # olhos
    d.rectangle([15, 18, 17, 21], fill=_rgba(pal["eye"]))
    d.rectangle([22, 18, 24, 21], fill=_rgba(pal["eye"]))
    # sombra do queixo
    d.line([14, 30, 25, 30], fill=_rgba(pal["hair2"]))
    return img


def make_tile(name, spec):
    """Tile 32x32 sem costura: base + salpicos determinísticos."""
    rng = random.Random(name)
    img = Image.new("RGBA", (32, 32), spec["base"])
    if spec.get("grid"):
        step = spec["grid"]
        for i in range(0, 32, step):
            for j in range(32):
                img.putpixel((i, j), spec["dark"])
                img.putpixel((j, i), spec["dark"])
    for _ in range(spec["n_dark"]):
        x, y = rng.randrange(32), rng.randrange(32)
        img.putpixel((x, y), spec["dark"])
        if name == "grass" and y < 31:
            img.putpixel((x, y + 1), spec["dark"])  # folha de grama
    for _ in range(spec["n_light"]):
        img.putpixel((rng.randrange(32), rng.randrange(32)), spec["light"])
    return img


def save(img, rel):
    path = SPRITES / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write_enemy_tres(name, out_path):
    content = (
        '[gd_resource type="SpriteFrames" load_steps=3 format=3]\n\n'
        f'[ext_resource type="Texture2D" path="res://assets/sprites/enemies/{name}_0.png" id="1"]\n'
        f'[ext_resource type="Texture2D" path="res://assets/sprites/enemies/{name}_1.png" id="2"]\n'
        '\n[resource]\nanimations = [{\n'
        '"frames": [{\n"duration": 1.0,\n"texture": ExtResource("1")\n}, {\n'
        '"duration": 1.0,\n"texture": ExtResource("2")\n}],\n'
        '"loop": true,\n"name": &"idle",\n"speed": 3.0\n}]\n'
    )
    out_path.write_text(content)


def main():
    # inimigos (frame 0 + frame 1 flutuando 1px)
    save(render(ECOADO, ECOADO_PALETTE, 24), "enemies/ecoado_0.png")
    save(render(ECOADO, ECOADO_PALETTE, 24, -1), "enemies/ecoado_1.png")
    save(render(BRUTAMONTES, BRUTA_PALETTE, 32), "enemies/brutamontes_0.png")
    save(render(BRUTAMONTES, BRUTA_PALETTE, 32, -1), "enemies/brutamontes_1.png")
    save(render(BOSS, BOSS_PALETTE, 32), "enemies/boss_0.png")
    save(render(BOSS, BOSS_PALETTE, 32, -1), "enemies/boss_1.png")
    save(render(ECOADO, ESPREITADOR_PALETTE, 24), "enemies/espreitador_0.png")
    save(render(ECOADO, ESPREITADOR_PALETTE, 24, -1), "enemies/espreitador_1.png")
    save(render(ALFA, ALFA_PALETTE, 24), "enemies/alfa_0.png")
    save(render(ALFA, ALFA_PALETTE, 24, -1), "enemies/alfa_1.png")
    save(render(ECOADO, FORJADO_PALETTE, 24), "enemies/forjado_0.png")
    save(render(ECOADO, FORJADO_PALETTE, 24, -1), "enemies/forjado_1.png")
    save(render(BRUTAMONTES, GOLEM_PALETTE, 32), "enemies/golem_0.png")
    save(render(BRUTAMONTES, GOLEM_PALETTE, 32, -1), "enemies/golem_1.png")
    save(render(ECOADO, SENTINELA_PALETTE, 24), "enemies/sentinela_0.png")
    save(render(ECOADO, SENTINELA_PALETTE, 24, -1), "enemies/sentinela_1.png")
    save(render(BOSS, BOSS_VENTO_PALETTE, 32), "enemies/vento_0.png")
    save(render(BOSS, BOSS_VENTO_PALETTE, 32, -1), "enemies/vento_1.png")
    save(render(ECOADO, CARCACA_PALETTE, 24), "enemies/carcaca_0.png")
    save(render(ECOADO, CARCACA_PALETTE, 24, -1), "enemies/carcaca_1.png")
    save(render(BOSS, BOSS_SAL_PALETTE, 32), "enemies/rainha_0.png")
    save(render(BOSS, BOSS_SAL_PALETTE, 32, -1), "enemies/rainha_1.png")
    save(render(BOSS, BOSS_SELENE_PALETTE, 32), "enemies/selene_0.png")
    save(render(BOSS, BOSS_SELENE_PALETTE, 32, -1), "enemies/selene_1.png")
    write_enemy_tres("ecoado", ROOT / "entities/enemies/ecoado_frames.tres")
    write_enemy_tres("brutamontes", ROOT / "entities/enemies/brutamontes_frames.tres")
    write_enemy_tres("boss", ROOT / "entities/enemies/boss_frames.tres")
    write_enemy_tres("espreitador", ROOT / "entities/enemies/espreitador_frames.tres")
    write_enemy_tres("alfa", ROOT / "entities/enemies/alfa_frames.tres")
    write_enemy_tres("forjado", ROOT / "entities/enemies/forjado_frames.tres")
    write_enemy_tres("golem", ROOT / "entities/enemies/golem_frames.tres")
    write_enemy_tres("sentinela", ROOT / "entities/enemies/sentinela_frames.tres")
    write_enemy_tres("vento", ROOT / "entities/enemies/vento_frames.tres")
    write_enemy_tres("carcaca", ROOT / "entities/enemies/carcaca_frames.tres")
    write_enemy_tres("rainha", ROOT / "entities/enemies/rainha_frames.tres")
    write_enemy_tres("selene", ROOT / "entities/enemies/selene_frames.tres")

    for name, palette in NPCS.items():
        save(render(DOWN_BASE, palette, 24), f"npcs/{name}.png")

    for name, rows in ICONS.items():
        save(render(rows, ICON_PALETTES[name], 16), f"icons/{name}.png")

    save(render(GRAPPLE_POST, PROP_PALETTES["grapple_post"], 16), "props/grapple_post.png")
    save(render(ANVIL, PROP_PALETTES["anvil"], 16), "props/anvil.png")
    save(render(OCARINA, PROP_PALETTES["ocarina"], 16), "icons/ocarina_vidro.png")
    save(render(TREE, PROP_PALETTES["tree"], 24), "props/tree.png")
    save(render(STAND, PROP_PALETTES["stand"], 16), "props/stand.png")
    save(make_cracked_wall(), "props/cracked_wall.png")

    for name, spec in TILE_SPECS.items():
        save(make_tile(name, spec), f"tiles/{name}.png")

    for name, pal in BUST_PALETTES.items():
        save(make_bust(pal), f"portraits/{name}.png")
    print("OK: sprites do mundo gerados em", SPRITES)


if __name__ == "__main__":
    main()
