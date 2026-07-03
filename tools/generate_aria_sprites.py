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

PALETTE = {
    ".": None,                    # transparente
    "o": (38, 30, 42, 255),       # contorno
    "s": (236, 196, 160, 255),    # pele
    "S": (198, 152, 120, 255),    # pele sombra
    "e": (45, 35, 50, 255),       # olhos
    "h": (128, 62, 46, 255),      # cabelo ruivo
    "H": (92, 42, 34, 255),       # cabelo sombra
    "t": (92, 168, 150, 255),     # túnica verde-água
    "T": (62, 124, 110, 255),     # túnica sombra / cinto
    "c": (238, 222, 178, 255),    # cachecol creme
    "p": (72, 66, 88, 255),       # calça
    "b": (52, 46, 60, 255),       # botas
    "a": (122, 86, 54, 255),      # alforje/correia de couro
    "g": (90, 62, 40, 255),       # empunhadura
    "w": (222, 226, 238, 255),    # lâmina
    "W": (160, 168, 190, 255),    # lâmina sombra/ponta
}

DOWN_BASE = [
    "...oooooo...",
    "..ohhhhhho..",
    ".ohhhhhhhho.",
    ".ohhhhhhhho.",
    ".ohssssssho.",
    ".ohsessesho.",
    ".oHssssssHo.",
    "..oSssssSo..",
    "..occcccco..",
    ".otttttttto.",
    ".ottattttto.",
    ".ostattttso.",
    ".oSttttttSo.",
    "..oTTTTTTo..",
    "..oppppppo..",
    "..opp..ppo..",
    "..opp..ppo..",
    "..obb..bbo..",
    "..obb..bbo..",
    "..oo....oo..",
]

UP_BASE = [
    "...oooooo...",
    "..ohhhhhho..",
    ".ohhhhhhhho.",
    ".ohhhhhhhho.",
    ".ohhhhhhhho.",
    ".ohhhhhhhho.",
    ".oHhhhhhhHo.",
    "..oShhhhSo..",
    "..ochhhhco..",
    ".otthhhhtto.",
    ".otthhhhtto.",
    ".osthhhhtso.",
    ".oStthhttSo.",
    "..oTTTTTTo..",
    "..oppppppo..",
    "..opp..ppo..",
    "..opp..ppo..",
    "..obb..bbo..",
    "..obb..bbo..",
    "..oo....oo..",
]

SIDE_BASE = [
    "...oooo...",
    "..ohhhho..",
    ".ohhhhhho.",
    ".ohhhhhho.",
    ".ohhsssso.",
    ".ohhsseso.",
    ".oHhsssSo.",
    "..ohsso...",
    "..occcco..",
    ".otttttto.",
    ".otatttto.",
    ".osttttso.",
    ".oSttttSo.",
    "..oTTTTo..",
    "..oppppo..",
    "..opp.ppo.",
    "..opp.ppo.",
    "..obb.bbo.",
    "..obb.bbo.",
    "..oo..oo..",
]

SIDE_STRIDE_A = SIDE_BASE[:14] + [
    "..oppppo..",
    ".opp..ppo.",
    ".opp..ppo.",
    ".obb..bbo.",
    ".obb..bbo.",
    ".oo....oo.",
]

SIDE_STRIDE_B = SIDE_BASE[:14] + [
    "..oppppo..",
    "..oppppo..",
    "..oppppo..",
    "..obbbbo..",
    "..obbbbo..",
    "..oo..oo..",
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
    (11, 8, "s"), (11, 9, "s"), (11, 10, "g"),
    (11, 11, "w"), (11, 12, "w"), (11, 13, "w"), (11, 14, "w"), (11, 15, "W"),
])

ATTACK_DOWN = patched(padded(DOWN_BASE, 3), [
    (12, 12, "g"),
    (13, 12, "w"), (13, 13, "w"),
    (14, 12, "w"), (14, 13, "w"),
    (15, 12, "W"),
])

ATTACK_UP = patched(padded(UP_BASE, 3), [
    (11, 12, "g"),
    (10, 12, "w"), (9, 12, "w"), (8, 12, "w"), (7, 12, "w"),
    (6, 12, "w"), (5, 12, "w"), (4, 12, "w"), (3, 12, "w"),
    (2, 12, "W"),
])

DOWN_BLINK = patched(DOWN_BASE, [(5, 4, "S"), (5, 7, "S")])
SIDE_BLINK = patched(SIDE_BASE, [(5, 6, "S")])


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
