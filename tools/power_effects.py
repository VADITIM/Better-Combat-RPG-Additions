"""Generates the trim power effect textures that play on a weapon's own pixels.

Each power gets a vertical strip of 16x16 frames in
assets/bcenchantments/textures/item/power/<power>.png. client/PowerEffects maps a frame 1:1 onto the
item's 16x16 sprite and draws it with the enchantment-glint pipeline, so only the item's pixels light
up. That pipeline blends additively with the source colour squared, so each pixel is stored as the
square root of the light it should add; black pixels are transparent and add nothing.

Weapon sprites run from the handle at the bottom left to the tip at the top right, so effects that
travel along the weapon follow that diagonal.

Run:  python tools/power_effects.py src/main/resources
Never hand-edit the output; change this script and run it again. Frame counts and frame times must
match PowerEffects.Effect.
"""
import math
import os
import random
import sys

from PIL import Image

SIZE = 16


def blank():
    return [[(0.0, 0.0, 0.0) for _ in range(SIZE)] for _ in range(SIZE)]


def add(img, x, y, color, strength):
    if 0 <= x < SIZE and 0 <= y < SIZE and strength > 0:
        r, g, b = img[y][x]
        img[y][x] = (min(1.0, r + color[0] * strength), min(1.0, g + color[1] * strength), min(1.0, b + color[2] * strength))


def rgb(hex_color):
    return ((hex_color >> 16 & 255) / 255.0, (hex_color >> 8 & 255) / 255.0, (hex_color & 255) / 255.0)


def along(x, y):
    """0 at the handle (bottom left), 1 at the tip (top right)."""
    return (x + (SIZE - 1 - y)) / (2.0 * (SIZE - 1))


def across(x, y):
    """Signed distance from the handle-to-tip diagonal, in pixels."""
    return (x - (SIZE - 1 - y)) / math.sqrt(2.0)


def lightning(frames):
    """A jagged bolt cracks down the weapon, flickers twice, and leaves a few sparks."""
    color = rgb(0xFFE45C)
    rng = random.Random(7)
    out = []
    bolt = []
    x = 13
    for y in range(SIZE):
        x = max(1, min(14, x + rng.choice((-1, -1, 0, 1)) - (1 if y % 4 == 0 else 0)))
        bolt.append((x, y))
    branch = [(bolt[5][0] - i, 5 + i) for i in range(1, 5)]
    sparks = [(rng.randrange(SIZE), rng.randrange(SIZE)) for _ in range(40)]
    for f in range(frames):
        img = blank()
        if f in (0, 1, 3):
            strength = 1.0 if f != 1 else 0.55
            for bx, by in bolt + (branch if f == 0 else []):
                add(img, bx, by, color, strength)
                add(img, bx + 1, by, color, strength * 0.35)
                add(img, bx - 1, by, color, strength * 0.35)
        elif f in (2, 4, 5):
            for bx, by in bolt[::2]:
                add(img, bx, by, color, 0.3)
        for i, (sx, sy) in enumerate(sparks):
            if (i * 7 + f) % frames == 0:
                add(img, sx, sy, color, 0.75)
        out.append(img)
    return out


def shockwave(frames):
    """A bright band of force sweeps from the handle to the tip and fades."""
    color = rgb(0x2EE6FF)
    out = []
    for f in range(frames):
        img = blank()
        front = f / (frames * 0.6)
        for y in range(SIZE):
            for x in range(SIZE):
                d = front - along(x, y)
                if 0 <= d < 0.22:
                    add(img, x, y, color, (1 - d / 0.22) * (1 - 0.5 * front if front < 1 else 0))
        out.append(img)
    return out


def cold_edge(frames):
    """Frost glitters across the weapon: pale points that twinkle in and out."""
    color = rgb(0xBFEFFF)
    rng = random.Random(11)
    points = [(rng.randrange(SIZE), rng.randrange(SIZE), rng.randrange(frames)) for _ in range(34)]
    out = []
    for f in range(frames):
        img = blank()
        for x, y, phase in points:
            t = ((f - phase) % frames) / frames
            s = max(0.0, 1 - abs(t - 0.15) / 0.15)
            add(img, x, y, color, s)
            if s > 0.7:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    add(img, x + dx, y + dy, color, 0.3)
        # A thin rime that never quite leaves the edge.
        for y in range(SIZE):
            for x in range(SIZE):
                if abs(across(x, y)) < 0.8 and (x + y + f // 2) % 5 == 0:
                    add(img, x, y, color, 0.25)
        out.append(img)
    return out


def uppercut(frames):
    """Streaks rise straight up through the weapon."""
    color = rgb(0xFFF1D6)
    columns = [(x, (x * 5) % frames, 3 + x % 3) for x in range(0, SIZE, 2)]
    out = []
    for f in range(frames):
        img = blank()
        for x, phase, length in columns:
            head = SIZE - 1 - ((f + phase) * 2) % (SIZE + length)
            for i in range(length):
                add(img, x, head + i, color, 1 - i / length)
        out.append(img)
    return out


def chains(frames):
    """Chain links slide from the handle to the tip."""
    color = rgb(0x9FB0BE)
    out = []
    for f in range(frames):
        img = blank()
        for y in range(SIZE):
            for x in range(SIZE):
                a = across(x, y)
                pos = (x + (SIZE - 1 - y)) - f
                link = pos % 6
                if abs(a) < 2.2:
                    ring = (link in (0, 3)) or (abs(a) > 1.2 and link in (1, 2, 4, 5))
                    if ring:
                        add(img, x, y, color, 0.8 if link in (0, 3) else 0.55)
        out.append(img)
    return out


def echo(frames):
    """A ring pulses out from the middle, then a fainter echo follows it."""
    color = rgb(0xA98BFF)
    out = []
    for f in range(frames):
        img = blank()
        for radius, strength in ((f * 0.9, 1.0), ((f - frames / 2) * 0.9, 0.5)):
            if radius < 0:
                continue
            for y in range(SIZE):
                for x in range(SIZE):
                    d = abs(math.hypot(x - 7.5, y - 7.5) - radius)
                    if d < 1.0:
                        add(img, x, y, color, strength * (1 - d) * max(0.0, 1 - radius / 11))
        out.append(img)
    return out


def wind_charged(frames):
    """Gusts curl across the weapon in wavy lines."""
    color = rgb(0xDDF8FF)
    out = []
    for f in range(frames):
        img = blank()
        for lane, offset in ((3, 0), (8, 5), (12, 10)):
            for i in range(6):
                x = (f * 2 + offset + i) % (SIZE + 6) - 3
                y = lane + round(1.5 * math.sin((x + f) * 0.7))
                add(img, x, y, color, (i + 1) / 6)
        out.append(img)
    return out


def gravity(frames):
    """Motes spiral in toward the middle of the weapon and vanish there."""
    color = rgb(0xB06CFF)
    out = []
    for f in range(frames):
        img = blank()
        for k in range(5):
            t = ((f + k * frames / 5) % frames) / frames
            radius = 8.5 * (1 - t)
            angle = k * 1.26 + t * 4.0
            x = round(7.5 + radius * math.cos(angle))
            y = round(7.5 + radius * math.sin(angle))
            add(img, x, y, color, 0.4 + 0.6 * t)
            add(img, round(7.5 + (radius + 1) * math.cos(angle - 0.25)), round(7.5 + (radius + 1) * math.sin(angle - 0.25)), color, 0.3)
        for y in range(7, 9):
            for x in range(7, 9):
                add(img, x, y, color, 0.35 + 0.25 * math.sin(f / frames * 2 * math.pi))
        out.append(img)
    return out


def momentum(frames):
    """Speed lines rush from the handle to the tip."""
    color = rgb(0xFFB347)
    lanes = (-3, -1, 1, 3)
    out = []
    for f in range(frames):
        img = blank()
        for y in range(SIZE):
            for x in range(SIZE):
                a = across(x, y)
                for n, lane in enumerate(lanes):
                    if abs(a - lane * 0.71) < 0.5:
                        t = (along(x, y) * frames * 0.8 - f * 1.6 - n * 3) % (frames * 0.8)
                        if t < 4:
                            add(img, x, y, color, 1 - t / 4)
        out.append(img)
    return out


def rampaging(frames):
    """The weapon throbs red like a heartbeat: two quick pulses, then rest."""
    color = rgb(0xFF3B30)
    out = []
    for f in range(frames):
        img = blank()
        t = f / frames
        beat = max(0.0, 1 - abs(t - 0.05) / 0.08) + 0.7 * max(0.0, 1 - abs(t - 0.25) / 0.08)
        for y in range(SIZE):
            for x in range(SIZE):
                edge = 0.35 + 0.65 * (abs(across(x, y)) / 8)
                add(img, x, y, color, beat * 0.4 * edge + (0.12 if (x * 3 + y * 5 + f) % 11 == 0 else 0))
        out.append(img)
    return out


# Power id -> (generator, frame count). Must match PowerEffects.Effect.
EFFECTS = {
    "lightning": (lightning, 32),
    "shockwave": (shockwave, 20),
    "cold_edge": (cold_edge, 24),
    "uppercut": (uppercut, 16),
    "chains": (chains, 12),
    "echo": (echo, 24),
    "wind_charged": (wind_charged, 16),
    "gravity": (gravity, 24),
    "momentum": (momentum, 16),
    "rampaging": (rampaging, 24),
}


def write(frames, path):
    image = Image.new("RGBA", (SIZE, SIZE * len(frames)), (0, 0, 0, 0))
    pixels = image.load()
    for i, frame in enumerate(frames):
        for y in range(SIZE):
            for x in range(SIZE):
                r, g, b = frame[y][x]
                if max(r, g, b) < 0.04:
                    continue
                pixels[x, i * SIZE + y] = (round(math.sqrt(r) * 255), round(math.sqrt(g) * 255), round(math.sqrt(b) * 255), 255)
    image.save(path)


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources"
    folder = os.path.join(root, "assets", "bcenchantments", "textures", "item", "power")
    os.makedirs(folder, exist_ok=True)
    for power, (generator, count) in EFFECTS.items():
        write(generator(count), os.path.join(folder, power + ".png"))
    print("wrote", len(EFFECTS), "power effects to", folder)


if __name__ == "__main__":
    main()
