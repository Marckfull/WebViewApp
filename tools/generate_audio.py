#!/usr/bin/env python3
"""Sintetiza os SFX e as músicas do protótipo (WAV 22050 Hz mono 16-bit).

Tudo procedural — sem samples externos. Rode da raiz do repositório:
    python3 tools/generate_audio.py
"""
import math
import random
import struct
import wave
from pathlib import Path

SR = 22050
OUT = Path(__file__).resolve().parent.parent / "game/assets/audio"

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def note_freq(name):
    """"A4" -> 440.0; suporta sustenido, ex.: "C#5"."""
    letter = name[0]
    rest = name[1:]
    sharp = rest.startswith("#")
    octave = int(rest[1:] if sharp else rest)
    semitone = NOTE_INDEX[letter] + (1 if sharp else 0)
    midi = 12 * (octave + 1) + semitone
    return 440.0 * 2 ** ((midi - 69) / 12)


def tone(freq, dur, wave_type="sine", vol=0.5, attack=0.01, release=0.08,
         vibrato_hz=0.0, vibrato_depth=0.0, glide_to=None):
    n = int(dur * SR)
    out = [0.0] * n
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = freq
        if glide_to is not None:
            f = freq + (glide_to - freq) * (i / n)
        if vibrato_hz:
            f += vibrato_depth * math.sin(2 * math.pi * vibrato_hz * t)
        phase += 2 * math.pi * f / SR
        if wave_type == "sine":
            s = math.sin(phase)
        elif wave_type == "square":
            s = 1.0 if math.sin(phase) >= 0 else -1.0
        elif wave_type == "saw":
            s = 2.0 * ((phase / (2 * math.pi)) % 1.0) - 1.0
        else:  # triangle
            s = 2.0 / math.pi * math.asin(math.sin(phase))
        # envelope
        a = min(1.0, t / attack) if attack > 0 else 1.0
        rel_start = dur - release
        r = 1.0 if t < rel_start else max(0.0, (dur - t) / release)
        out[i] = s * vol * a * r
    return out


def noise(dur, vol=0.5, lowpass=1, attack=0.005, release=0.05):
    """Ruído branco com média móvel (lowpass grosseiro)."""
    rng = random.Random(42)
    n = int(dur * SR)
    raw = [rng.uniform(-1, 1) for _ in range(n)]
    if lowpass > 1:
        acc = 0.0
        out = []
        for i, s in enumerate(raw):
            acc += s
            if i >= lowpass:
                acc -= raw[i - lowpass]
            out.append(acc / lowpass)
        raw = out
    for i in range(n):
        t = i / SR
        a = min(1.0, t / attack) if attack > 0 else 1.0
        rel_start = dur - release
        r = 1.0 if t < rel_start else max(0.0, (dur - t) / release)
        raw[i] *= vol * a * r
    return raw


def mix(buf, samples, at_sec=0.0):
    off = int(at_sec * SR)
    need = off + len(samples)
    if need > len(buf):
        buf.extend([0.0] * (need - len(buf)))
    for i, s in enumerate(samples):
        buf[off + i] += s


def write_wav(path, buf):
    path.parent.mkdir(parents=True, exist_ok=True)
    peak = max(1.0, max(abs(s) for s in buf))
    data = b"".join(
        struct.pack("<h", int(max(-1.0, min(1.0, s / peak * 0.92)) * 32767))
        for s in buf
    )
    with wave.open(str(path), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(data)


# ----------------------------------------------------------------- SFX

def sfx_swing():
    buf = []
    mix(buf, noise(0.14, vol=0.5, lowpass=3, release=0.09))
    mix(buf, tone(700, 0.12, "sine", vol=0.12, glide_to=220, release=0.08))
    return buf


def sfx_hit():
    buf = []
    mix(buf, tone(140, 0.14, "sine", vol=0.7, glide_to=60, release=0.1))
    mix(buf, noise(0.06, vol=0.4, lowpass=2, release=0.04))
    return buf


def sfx_hurt():
    buf = []
    mix(buf, tone(220, 0.2, "square", vol=0.25, glide_to=90, release=0.12))
    mix(buf, tone(110, 0.18, "sine", vol=0.4, glide_to=55, release=0.12))
    return buf


def sfx_roll():
    return noise(0.24, vol=0.35, lowpass=6, attack=0.04, release=0.14)


def sfx_pickup():
    buf = []
    mix(buf, tone(note_freq("A5"), 0.09, "sine", vol=0.4, release=0.05))
    mix(buf, tone(note_freq("E6"), 0.12, "sine", vol=0.35, release=0.08), 0.07)
    return buf


def sfx_death():
    buf = []
    mix(buf, tone(220, 0.9, "saw", vol=0.3, glide_to=50, release=0.5))
    mix(buf, tone(110, 0.9, "sine", vol=0.4, glide_to=28, release=0.5))
    return buf


def sfx_enemy_death():
    buf = []
    mix(buf, tone(320, 0.3, "square", vol=0.22, glide_to=70, release=0.18))
    mix(buf, noise(0.12, vol=0.25, lowpass=4, release=0.1), 0.04)
    return buf


def sfx_shrine():
    buf = []
    for i, n in enumerate(["A4", "C#5", "E5", "A5"]):
        mix(buf, tone(note_freq(n), 0.7, "sine", vol=0.28, release=0.5), i * 0.12)
    return buf


def sfx_victory():
    buf = []
    for i, n in enumerate(["C5", "E5", "G5", "C6"]):
        mix(buf, tone(note_freq(n), 0.5, "triangle", vol=0.3, release=0.35), i * 0.11)
    return buf


def sfx_roar():
    buf = []
    mix(buf, tone(75, 0.7, "saw", vol=0.5, glide_to=45,
                  vibrato_hz=11, vibrato_depth=8, release=0.35))
    mix(buf, noise(0.6, vol=0.3, lowpass=10, attack=0.05, release=0.3))
    return buf


def sfx_blip():
    return tone(660, 0.06, "square", vol=0.2, release=0.03)


def sfx_forge():
    buf = []
    for at in (0.0, 0.22):
        mix(buf, tone(1180, 0.12, "square", vol=0.12, glide_to=990,
                      release=0.09), at)
        mix(buf, tone(640, 0.16, "sine", vol=0.3, glide_to=520,
                      release=0.12), at)
        mix(buf, noise(0.03, vol=0.25, lowpass=2, release=0.02), at)
    return buf


def _ocarina_phrase(names_durs):
    buf = []
    at = 0.0
    for name, dur in names_durs:
        mix(buf, tone(note_freq(name), dur, "sine", vol=0.35,
                      attack=0.04, release=0.15,
                      vibrato_hz=5.5, vibrato_depth=4.0), at)
        at += dur
    return buf


def sfx_melody_return():
    # o leitmotiv da Canção do Mundo
    return _ocarina_phrase([("E5", 0.32), ("C5", 0.32), ("D5", 0.32), ("A4", 0.62)])


def sfx_melody_calm():
    return _ocarina_phrase([("A4", 0.4), ("G4", 0.4), ("E4", 0.75)])


# --------------------------------------------------------------- músicas

def melody(buf, beat, notes, wave_type="sine", vol=0.3, vib=5.0, depth=3.0):
    """notes: lista de (tempo_em_beats, nome|None, duração_em_beats)."""
    for start, name, dur in notes:
        if name is None:
            continue
        mix(buf, tone(note_freq(name), dur * beat * 0.95, wave_type,
                      vol=vol, attack=0.02, release=0.12,
                      vibrato_hz=vib, vibrato_depth=depth), start * beat)


def chords(buf, beat, bars, wave_type="triangle", vol=0.07):
    """bars: lista de listas de notas (um acorde por compasso de 4 beats)."""
    for i, chord in enumerate(bars):
        for n in chord:
            mix(buf, tone(note_freq(n), 4 * beat, wave_type,
                          vol=vol, attack=0.3, release=0.8), i * 4 * beat)


def music_village():
    beat = 0.65
    buf = []
    chords(buf, beat, [
        ["A2", "E3", "A3", "C4"], ["F2", "C3", "F3", "A3"],
        ["C3", "G3", "C4", "E4"], ["G2", "D3", "G3", "B3"],
        ["A2", "E3", "A3", "C4"], ["F2", "C3", "F3", "A3"],
        ["G2", "D3", "G3", "B3"], ["A2", "E3", "A3", "C4"],
    ])
    # motivo da "Canção do Mundo"
    melody(buf, beat, [
        (0, "E5", 1.5), (1.5, "C5", 0.5), (2, "D5", 1), (3, "A4", 1),
        (4, "C5", 1.5), (5.5, "A4", 0.5), (6, "B4", 1), (7, "G4", 1),
        (8, "E5", 1), (9, "G5", 1), (10, "E5", 1), (11, "D5", 1),
        (12, "C5", 2), (14, "D5", 2),
        (16, "E5", 1.5), (17.5, "C5", 0.5), (18, "D5", 1), (19, "A4", 1),
        (20, "C5", 1.5), (21.5, "A4", 0.5), (22, "B4", 1), (23, "D5", 1),
        (24, "E5", 1), (25, "G5", 1), (26, "A5", 2),
        (28, "G5", 1), (29, "E5", 1), (30, "D5", 0.5), (30.5, "C5", 0.5),
        (31, "A4", 1),
    ], vol=0.3, vib=5.5, depth=4.0)
    return buf


def music_crypt():
    buf = []
    dur = 16.0
    mix(buf, tone(note_freq("D2"), dur, "saw", vol=0.10,
                  attack=1.5, release=2.5))
    mix(buf, tone(note_freq("D3") + 1.5, dur, "sine", vol=0.12,
                  attack=1.5, release=2.5))
    mix(buf, tone(note_freq("A3"), dur, "sine", vol=0.05,
                  attack=2.0, release=3.0))
    for at, n in [(2.0, "D5"), (6.0, "F5"), (9.5, "E5"), (12.5, "C#5")]:
        mix(buf, tone(note_freq(n), 2.2, "sine", vol=0.12,
                      attack=0.4, release=1.6, vibrato_hz=4, vibrato_depth=2),
            at)
    mix(buf, noise(dur, vol=0.02, lowpass=40, attack=2.0, release=3.0))
    return buf


def music_boss():
    beat = 0.4
    buf = []
    bass_line = ["A2", "A2", "A2", "A2", "G2", "G2", "F2", "E2"] * 4
    for i, n in enumerate(bass_line):
        mix(buf, tone(note_freq(n), beat * 0.9, "square", vol=0.13,
                      release=0.06), i * beat)
    n_beats = len(bass_line)
    for i in range(n_beats):
        if i % 2 == 0:
            mix(buf, tone(70, 0.1, "sine", vol=0.5, glide_to=40,
                          release=0.06), i * beat)
        else:
            mix(buf, noise(0.05, vol=0.16, lowpass=2, release=0.03), i * beat)
    melody(buf, beat, [
        (0, "A4", 1), (1, "C5", 1), (2, "E5", 1), (3, "C5", 1),
        (4, "A4", 1), (5, "C5", 1), (6, "F5", 1), (7, "D5", 1),
        (8, "A4", 1), (9, "C5", 1), (10, "E5", 1), (11, "G5", 1),
        (12, "F5", 0.5), (12.5, "E5", 0.5), (13, "D5", 1), (14, "E5", 2),
        (16, "A5", 1), (17, "G5", 1), (18, "E5", 1), (19, "C5", 1),
        (20, "D5", 1), (21, "F5", 1), (22, "D5", 1), (23, "B4", 1),
        (24, "C5", 1), (25, "E5", 1), (26, "A5", 1), (27, "E5", 1),
        (28, "F5", 0.5), (28.5, "E5", 0.5), (29, "D5", 1), (30, "E5", 2),
    ], wave_type="square", vol=0.10, vib=0, depth=0)
    return buf


def main():
    sfx = {
        "swing": sfx_swing(), "hit": sfx_hit(), "hurt": sfx_hurt(),
        "roll": sfx_roll(), "pickup": sfx_pickup(), "death": sfx_death(),
        "enemy_death": sfx_enemy_death(), "shrine": sfx_shrine(),
        "victory": sfx_victory(), "roar": sfx_roar(), "blip": sfx_blip(),
        "forge": sfx_forge(), "melody_return": sfx_melody_return(),
        "melody_calm": sfx_melody_calm(),
    }
    for name, buf in sfx.items():
        write_wav(OUT / "sfx" / f"{name}.wav", buf)
    for name, buf in [("village", music_village()), ("crypt", music_crypt()),
                      ("boss", music_boss())]:
        write_wav(OUT / "music" / f"{name}.wav", buf)
    print(f"OK: {len(sfx)} SFX + 3 músicas em {OUT}")


if __name__ == "__main__":
    main()
