#!/usr/bin/env python3
"""Gera os sprites em pixel art da Aria (protagonista) e o resource
SpriteFrames (aria_frames.tres) usado pelo AnimatedSprite2D do player.

Cada pose é desenhada como uma grade de caracteres (1 char = 1 pixel).
Rode da raiz do repositório:  python3 tools/generate_aria_sprites.py
"""
from pathlib import Path

from PIL import Image

OUT_DIR = Path(__file__).resolve().parent.parent / "game/assets/sprites/aria"
TRES_PATH = Path(__file__).resolve().parent.parent / "game/entities/player/aria_frames.tres"
CANVAS = 24

# Direção de arte inspirada em Minish Cap (GBA): proporção chibi
# (cabeça ≈ metade da altura), cores vivas e saturadas, contorno escuro.
PALETTE = {
    ".": None,                    # transparente
    "o": (40, 32, 36, 255),       # contorno quente
    "s": (252, 216, 168, 255),    # pele
    "S": (224, 168, 120, 255),    # pele sombra
    "e": (32, 40, 56, 255),       # olhos
    "h": (200, 96, 48, 255),      # cabelo ruivo vivo
    "H": (152, 64, 36, 255),      # cabelo sombra
    "t": (64, 192, 160, 255),     # túnica verde-água viva
    "T": (32, 140, 116, 255),     # túnica sombra / cinto
    "c": (248, 232, 168, 255),    # cachecol creme
    "p": (88, 80, 112, 255),      # calça
    "b": (120, 76, 48, 255),      # botas de couro
    "a": (160, 108, 60, 255),     # alforje/correia de couro
    "g": (112, 76, 44, 255),      # empunhadura
    "w": (236, 240, 252, 255),    # lâmina
    "W": (168, 180, 208, 255),    # lâmina sombra/ponta
}

DOWN_BASE = [
    "....oooooo....",
    "..oohhhhhhoo..",
    ".ohhhhhhhhhho.",
    ".ohhhhhhhhhho.",
    "ohhhhhhhhhhhho",
    "ohhossssssohho",
    "ohossssssssoho",
    "ohoseesseesoho",
    "ohoseesseesoho",
    ".ohssssssssho.",
    "..oSssssssSo..",
    "...occcccco...",
    "..otttttttto..",
    "..ottattttto..",
    "..ostattttso..",
    "..oTTTTTTTTo..",
    "...oppppppo...",
    "...opp..ppo...",
    "...obb..bbo...",
    "...obb..bbo...",
    "...oo....oo...",
]

UP_BASE = [
    "....oooooo....",
    "..oohhhhhhoo..",
    ".ohhhhhhhhhho.",
    ".ohhhhhhhhhho.",
    "ohhhhhhhhhhhho",
    "ohhhhhhhhhhhho",
    "ohhhhhhhhhhhho",
    "ohhhhhhhhhhhho",
    "ohHhhhhhhhHhho",
    ".ohhhhhhhhhho.",
    "..oHhhhhhhHo..",
    "...ochhhhco...",
    "..otthhhhtto..",
    "..otthhhhtto..",
    "..osthhhhtso..",
    "..oTThhhhTTo..",
    "...oppppppo...",
    "...opp..ppo...",
    "...obb..bbo...",
    "...obb..bbo...",
    "...oo....oo...",
]

SIDE_BASE = [
    "...oooooo...",
    ".oohhhhhhoo.",
    ".ohhhhhhhho.",
    "ohhhhhhhhhho",
    "ohhhhsssssо.".replace("о", "o"),
    "ohhhhsseeso.",
    "ohhhhsseeso.",
    ".ohhhssssso.",
    ".ohHssssSo..",
    "..oh.osso...",
    "..ohoccco...",
    "..otttttto..",
    "..otatttto..",
    "..osttttso..",
    "..oTTTTTTo..",
    "...oppppo...",
    "...opp.po...",
    "...obb.bo...",
    "...obb.bo...",
    "...oo..oo...",
]

SIDE_STRIDE_A = SIDE_BASE[:15] + [
    "...oppppo...",
    "..opp..ppo..",
    "..obb..bbo..",
    "..obb..bbo..",
    "..oo....oo..",
]

SIDE_STRIDE_B = SIDE_BASE[:15] + [
    "...oppppo...",
    "....oppo....",
    "....obbo....",
    "....obbo....",
    "....oo.o....",
]

BALL = [
    "...oooooo...",
    "..ohhhhhho..",
    ".ohhtttthho.",
    ".otttttttto.",
    ".ottcctttto.",
    ".otttttttto.",
    ".otttttttto.",
    ".oTTttttTTo.",
    "..oTTTTTTo..",
    "..obbbbbbo..",
    "...oooooo...",
]


def patched(rows, points):
    """Retorna uma cópia das linhas com pixels substituídos."""
    grid = [list(r) for r in rows]
    for row, col, char in points:
        grid[row][col] = char
    return ["".join(r) for r in grid]


def padded(rows, extra_right):
    return [r + "." * extra_right for r in rows]


# Ataques: pose base + braço estendido com espada (via patches).
ATTACK_SIDE = patched(padded(SIDE_BASE, 6), [
    (13, 9, "s"), (13, 10, "g"),
    (13, 11, "w"), (13, 12, "w"), (13, 13, "w"), (13, 14, "w"), (13, 15, "W"),
])

ATTACK_DOWN = patched(padded(DOWN_BASE, 3), [
    (14, 12, "g"),
    (15, 12, "w"), (16, 12, "w"), (17, 12, "w"),
    (18, 12, "W"),
])

ATTACK_UP = patched(padded(UP_BASE, 3), [
    (13, 14, "g"),
    (12, 14, "w"), (11, 14, "w"), (10, 14, "w"), (9, 14, "w"),
    (8, 14, "w"), (7, 14, "w"), (6, 14, "w"), (5, 14, "w"),
    (4, 14, "w"), (3, 14, "w"),
    (2, 14, "W"),
])

DOWN_BLINK = patched(DOWN_BASE, [
    (7, 4, "s"), (7, 5, "s"), (7, 8, "s"), (7, 9, "s"),
    (8, 4, "S"), (8, 5, "S"), (8, 8, "S"), (8, 9, "S"),
])
SIDE_BLINK = patched(SIDE_BASE, [
    (5, 7, "s"), (5, 8, "s"),
    (6, 7, "S"), (6, 8, "S"),
])


def render(rows, y_shift=0):
    """Desenha a grade centralizada num canvas CANVAS x CANVAS."""
    width = len(rows[0])
    for r in rows:
        assert len(r) == width, f"linha com largura errada: {r!r}"
        assert set(r) <= set(PALETTE), f"char desconhecido em: {r!r}"
    img = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    x0 = (CANVAS - width) // 2
    y0 = CANVAS - len(rows) - 2 + y_shift
    for y, row in enumerate(rows):
        for x, char in enumerate(row):
            color = PALETTE[char]
            if color and 0 <= x0 + x < CANVAS and 0 <= y0 + y < CANVAS:
                img.putpixel((x0 + x, y0 + y), color)
    return img


# nome do arquivo -> (grade, deslocamento vertical)
IMAGES = {
    "down_base": (DOWN_BASE, 0),
    "down_blink": (DOWN_BLINK, 0),
    "down_bob": (DOWN_BASE, -1),
    "up_base": (UP_BASE, 0),
    "up_bob": (UP_BASE, -1),
    "side_base": (SIDE_BASE, 0),
    "side_blink": (SIDE_BLINK, 0),
    "side_stride_a": (SIDE_STRIDE_A, 0),
    "side_stride_b": (SIDE_STRIDE_B, 0),
    "ball_0": (BALL, 0),
    "ball_1": (BALL, -2),
    "attack_down": (ATTACK_DOWN, 0),
    "attack_up": (ATTACK_UP, 0),
    "attack_side": (ATTACK_SIDE, 0),
}

# animação -> (frames, velocidade, loop)
ANIMATIONS = {
    "idle_down": (["down_base", "down_blink"], 1.5, True),
    "idle_up": (["up_base"], 1.0, True),
    "idle_side": (["side_base", "side_blink"], 1.5, True),
    "walk_down": (["down_base", "down_bob"], 6.0, True),
    "walk_up": (["up_base", "up_bob"], 6.0, True),
    "walk_side": (["side_stride_a", "side_stride_b"], 6.0, True),
    "roll_down": (["ball_0", "ball_1"], 10.0, True),
    "roll_up": (["ball_0", "ball_1"], 10.0, True),
    "roll_side": (["ball_0", "ball_1"], 10.0, True),
    "attack_down": (["down_base", "attack_down"], 10.0, False),
    "attack_up": (["up_base", "attack_up"], 10.0, False),
    "attack_side": (["side_base", "attack_side"], 10.0, False),
}


def write_tres():
    names = list(IMAGES)
    ext_lines = []
    for i, name in enumerate(names, start=1):
        ext_lines.append(
            f'[ext_resource type="Texture2D" '
            f'path="res://assets/sprites/aria/{name}.png" id="{i}"]'
        )
    id_of = {name: i for i, name in enumerate(names, start=1)}
    anim_blocks = []
    for anim, (frames, speed, loop) in ANIMATIONS.items():
        frame_items = ", ".join(
            f'{{\n"duration": 1.0,\n"texture": ExtResource("{id_of[f]}")\n}}'
            for f in frames
        )
        anim_blocks.append(
            f'{{\n"frames": [{frame_items}],\n'
            f'"loop": {"true" if loop else "false"},\n'
            f'"name": &"{anim}",\n'
            f'"speed": {speed}\n}}'
        )
    content = (
        f"[gd_resource type=\"SpriteFrames\" "
        f"load_steps={len(names) + 1} format=3]\n\n"
        + "\n".join(ext_lines)
        + "\n\n[resource]\nanimations = ["
        + ", ".join(anim_blocks)
        + "]\n"
    )
    TRES_PATH.write_text(content)


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, (rows, y_shift) in IMAGES.items():
        render(rows, y_shift).save(OUT_DIR / f"{name}.png")
    write_tres()
    print(f"OK: {len(IMAGES)} sprites em {OUT_DIR}")
    print(f"OK: {TRES_PATH}")


if __name__ == "__main__":
    main()
