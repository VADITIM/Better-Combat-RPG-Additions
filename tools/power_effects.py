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


def marked(frames):
    """A crosshair closes in on the middle of the weapon, flashes, and lets go."""
    color = rgb(0xFFB347)
    out = []
    for f in range(frames):
        img = blank()
        t = f / frames
        gap = max(1.0, 7.0 * (1 - t / 0.6))
        flash = max(0.0, 1 - abs(t - 0.65) / 0.1)
        fade = 1.0 if t < 0.75 else max(0.0, 1 - (t - 0.75) / 0.25)
        for d in range(0, 3):
            for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                x = round(7.5 + sx * (gap + d))
                y = round(7.5 + sy * (gap + d))
                add(img, x, y, color, (0.9 - d * 0.25) * fade)
        for y in range(6, 10):
            for x in range(6, 10):
                add(img, x, y, color, flash * (1 - math.hypot(x - 7.5, y - 7.5) / 3))
        out.append(img)
    return out


def venomous(frames):
    """Green drops bead on the weapon and drip from the tip toward the handle."""
    color = rgb(0x7BD34A)
    rng = random.Random(5)
    drops = [(rng.randrange(SIZE), rng.randrange(SIZE), rng.randrange(frames)) for _ in range(9)]
    out = []
    for f in range(frames):
        img = blank()
        for x, y, phase in drops:
            t = ((f - phase) % frames) / frames
            fall = round(t * 6)
            add(img, x, y + fall, color, 1 - t)
            add(img, x, y + fall - 1, color, (1 - t) * 0.4)
        for y in range(SIZE):
            for x in range(SIZE):
                if abs(across(x, y)) < 0.8 and (x * 2 + y + f) % 9 == 0:
                    add(img, x, y, color, 0.3)
        out.append(img)
    return out


def curse(frames):
    """Dark purple runes flicker in and out along the weapon."""
    color = rgb(0x9B3FD6)
    rng = random.Random(13)
    runes = [(rng.randrange(2, SIZE - 2), rng.randrange(2, SIZE - 2), rng.randrange(frames)) for _ in range(7)]
    shapes = (((0, -1), (0, 0), (0, 1), (-1, 0)), ((-1, -1), (0, 0), (1, 1), (1, -1)), ((0, -1), (-1, 1), (1, 1), (0, 0)))
    out = []
    for f in range(frames):
        img = blank()
        for i, (x, y, phase) in enumerate(runes):
            t = ((f - phase) % frames) / frames
            s = max(0.0, 1 - abs(t - 0.2) / 0.2)
            if (f + i) % 3 == 0:
                s *= 0.6
            for dx, dy in shapes[i % len(shapes)]:
                add(img, x + dx, y + dy, color, s)
        out.append(img)
    return out


def plunder(frames):
    """Emerald glints run up the weapon one after another, a coin catching the light."""
    color = rgb(0x3CE07A)
    out = []
    for f in range(frames):
        img = blank()
        for k in range(3):
            t = ((f + k * frames / 3) % frames) / frames
            if t > 0.5:
                continue
            head = t * 2
            for y in range(SIZE):
                for x in range(SIZE):
                    d = abs(along(x, y) - head)
                    if d < 0.07 and abs(across(x, y)) < 3:
                        add(img, x, y, color, (1 - d / 0.07) * 0.9)
        out.append(img)
    return out


def withering(frames):
    """Pale soul wisps rise off the weapon and fade, the colour of soul fire."""
    color = rgb(0x6FE3E8)
    wisps = [(x, (x * 7) % frames) for x in range(1, SIZE, 3)]
    out = []
    for f in range(frames):
        img = blank()
        for x, phase in wisps:
            t = ((f + phase) % frames) / frames
            y = round(SIZE - 1 - t * SIZE)
            wobble = round(math.sin((t * 6) + x) * 1.2)
            add(img, x + wobble, y, color, 1 - t)
            add(img, x + wobble, y + 1, color, (1 - t) * 0.4)
        out.append(img)
    return out


def shulker_burst(frames):
    """Three bright bullets orbit the middle of the weapon."""
    color = rgb(0xF2E3FF)
    out = []
    for f in range(frames):
        img = blank()
        for k in range(3):
            angle = f / frames * 2 * math.pi + k * 2 * math.pi / 3
            x = 7.5 + 5 * math.cos(angle)
            y = 7.5 + 5 * math.sin(angle)
            add(img, round(x), round(y), color, 1.0)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                add(img, round(x) + dx, round(y) + dy, color, 0.35)
            tx = 7.5 + 5 * math.cos(angle - 0.4)
            ty = 7.5 + 5 * math.sin(angle - 0.4)
            add(img, round(tx), round(ty), color, 0.3)
        out.append(img)
    return out


def guardian_strike(frames):
    """A pale wing flaps over the weapon, twice, then rests."""
    color = rgb(0xCFE3F0)
    out = []
    for f in range(frames):
        img = blank()
        t = f / frames
        flap = abs(math.sin(t * 2 * math.pi * 2)) if t < 0.5 else 0.0
        for i in range(6):
            length = 6 - i
            angle = -0.6 - flap * 0.9 + i * 0.25
            for r in range(length):
                x = round(6 + r * math.cos(angle))
                y = round(9 + r * math.sin(angle) - i * 0.3)
                add(img, x, y, color, (0.8 - r * 0.08) * (0.4 + 0.6 * (1 - t)))
        out.append(img)
    return out


def copper_shock(frames):
    """A copper spark jumps between points on the weapon, leaving short arcs."""
    color = rgb(0xE88A4A)
    rng = random.Random(3)
    points = [(rng.randrange(1, SIZE - 1), rng.randrange(1, SIZE - 1)) for _ in range(6)]
    out = []
    for f in range(frames):
        img = blank()
        step = f // 3
        if step < len(points) - 1:
            (x0, y0), (x1, y1) = points[step], points[step + 1]
            n = max(abs(x1 - x0), abs(y1 - y0), 1)
            for i in range(n + 1):
                x = round(x0 + (x1 - x0) * i / n + rng.choice((-1, 0, 0, 1)) * (0 < i < n))
                y = round(y0 + (y1 - y0) * i / n)
                add(img, x, y, color, 0.9 if f % 3 == 0 else 0.45)
            add(img, x1, y1, color, 1.0)
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


def shared_pain(frames):
    """A red pulse runs from the handle to the tip and leaps off the end, then the weapon rests."""
    color = rgb(0xE0245E)
    out = []
    for f in range(frames):
        img = blank()
        t = f / frames
        head = t * 1.6 - 0.3
        for y in range(SIZE):
            for x in range(SIZE):
                d = abs(along(x, y) - head)
                if d < 0.18:
                    add(img, x, y, color, (1 - d / 0.18) * 0.6 * (1 - abs(across(x, y)) / 9))
        out.append(img)
    return out


def perfect_hit(frames):
    """The mace's Perfect Hit window: the head flares white and four-point stars burst over it.
    Its frames follow the 0.15 s window rather than the clock, so it plays once per window."""
    color = rgb(0xFFFFFF)
    stars = [(11, 3, 0), (13, 6, 1), (8, 2, 1), (12, 1, 2), (9, 6, 2), (14, 3, 3)]
    out = []
    for f in range(frames):
        img = blank()
        t = f / (frames - 1)
        flare = 1 - abs(t - 0.35) / 0.65
        for y in range(SIZE):
            for x in range(SIZE):
                a = along(x, y)
                if a > 0.55:
                    add(img, x, y, color, 0.45 * flare * (a - 0.55) / 0.45)
        for sx, sy, start in stars:
            age = f - start
            if 0 <= age < 3:
                strength = (1.0, 0.8, 0.4)[age]
                arm = 1 if age == 0 else 2
                add(img, sx, sy, color, strength)
                for d in range(1, arm + 1):
                    for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                        add(img, sx + dx, sy + dy, color, strength * (0.6 if d == 1 else 0.3))
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
    "marked": (marked, 20),
    "rampaging": (rampaging, 24),
    "venomous": (venomous, 24),
    "curse": (curse, 24),
    "plunder": (plunder, 24),
    "withering": (withering, 24),
    "shulker_burst": (shulker_burst, 24),
    "shared_pain": (shared_pain, 20),
    "guardian_strike": (guardian_strike, 20),
    "copper_shock": (copper_shock, 18),
    "perfect_hit": (perfect_hit, 6),
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
