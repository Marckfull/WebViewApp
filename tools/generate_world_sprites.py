#!/usr/bin/env python3
"""Gera sprites de inimigos, boss, NPCs, ícones de itens e props,
além dos SpriteFrames (.tres) dos inimigos.

Rode da raiz do repositório:  python3 tools/generate_world_sprites.py
"""
from pathlib import Path

from PIL import Image

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
}

ICONS = {
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
}

PROP_PALETTES = {
    "grapple_post": {
        ".": None,
        "o": (28, 24, 30, 255),
        "a": (122, 86, 54, 255),
        "A": (88, 60, 38, 255),
        "m": (168, 176, 190, 255),
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
    write_enemy_tres("ecoado", ROOT / "entities/enemies/ecoado_frames.tres")
    write_enemy_tres("brutamontes", ROOT / "entities/enemies/brutamontes_frames.tres")
    write_enemy_tres("boss", ROOT / "entities/enemies/boss_frames.tres")

    for name, palette in NPCS.items():
        save(render(DOWN_BASE, palette, 24), f"npcs/{name}.png")

    for name, rows in ICONS.items():
        save(render(rows, ICON_PALETTES[name], 16), f"icons/{name}.png")

    save(render(GRAPPLE_POST, PROP_PALETTES["grapple_post"], 16), "props/grapple_post.png")
    print("OK: sprites do mundo gerados em", SPRITES)


if __name__ == "__main__":
    main()
