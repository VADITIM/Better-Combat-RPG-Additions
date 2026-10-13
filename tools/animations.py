import json, os, sys
res = sys.argv[1]

# ---------- Punchy first-person clips (Bedrock format, degrees / pixels) ----------
def kf(t, v, mode="catmullrom"):
    return t, {"post": v, "lerp_mode": mode}

def track(points):
    return {f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0": {"post": v, "lerp_mode": m} for t, v, m in points}

def mirror_rot(points):
    return [(t, [v[0], -v[1], -v[2]], m) for t, v, m in points]

def mirror_pos(points):
    return [(t, [-v[0], v[1], v[2]], m) for t, v, m in points]

C, L = "catmullrom", "linear"

# Uppercut: the blade drops low and back, then rips straight up past the face and hangs at the top.
up_rot = [
    (0.0,  [0, 0, 0], C),
    (0.06, [34, 8, -24], C),
    (0.13, [44, 11, -31], L),
    (0.19, [-18, -5, 9], L),
    (0.25, [-76, -12, 22], C),
    (0.32, [-85, -13, 25], C),
    (0.5,  [-80, -11, 23], C),
    (0.75, [-28, -4, 8], C),
    (1.0,  [0, 0, 0], C),
]
up_pos = [
    (0.0,  [0, 0, 0], C),
    (0.06, [3, -6, 3], C),
    (0.13, [4, -9, 4], L),
    (0.19, [0, 2, -8], L),
    (0.25, [-2, 10, -11], C),
    (0.32, [-2, 11.5, -11], C),
    (0.5,  [-2, 10, -10], C),
    (0.75, [-1, 3, -3], C),
    (1.0,  [0, 0, 0], C),
]
up_grip = [
    (0.0,  [0, 0, 0], C),
    (0.13, [55, 0, 0], L),
    (0.25, [-28, 0, 0], C),
    (0.5,  [-24, 0, 0], C),
    (0.9,  [0, 0, 0], C),
]

# Air crit: both hands on the grip, pulled in to the middle of the screen, a high windup and a
# heavy strike straight down. Played for any sword swing in the air that rolled a critical hit.
cr_rot = [
    (0.0,  [0, 0, 0], C),
    (0.08, [-88, -14, 8], C),
    (0.18, [-102, -15, 9], L),
    (0.24, [80, -14, -2], L),
    (0.27, [60, -14, -2], L),
    (0.32, [76, -14, -2], C),
    (0.6,  [70, -13, -2], C),
    (0.88, [-12, -2, 1], C),
    (1.15, [0, 0, 0], C),
]
cr_pos = [
    (0.0,  [0, 0, 0], C),
    (0.08, [-4, 10, 6], C),
    (0.18, [-4.5, 12.5, 7], L),
    (0.24, [-4, -15, -23], L),
    (0.27, [-4, -11, -20], L),
    (0.32, [-4, -15.5, -23], C),
    (0.6,  [-4, -14.5, -22], C),
    (0.88, [-1, 2, 0], C),
    (1.15, [0, 0, 0], C),
]
cr_grip = [
    (0.0,  [0, 0, 0], C),
    (0.08, [-30, 0, 0], C),
    (0.18, [-36, 0, 0], L),
    (0.24, [56, 0, 0], L),
    (0.6,  [52, 0, 0], C),
    (1.0,  [0, 0, 0], C),
]

def clip(rot, pos, grip, length, timeline="anim_speed_1;"):
    return {
        "animation_length": length,
        "bones": {
            "right_arm": {"rotation": track(rot), "position": track(pos)},
            "left_arm": {"rotation": track(mirror_rot(rot)), "position": track(mirror_pos(pos))},
            "itemgrip_right": {"rotation": track(grip)},
            "itemgrip_left": {"rotation": track(mirror_rot(grip))},
        },
        "timeline": {"0.0": timeline},
    }

# Spear combo. Each stab clip spans exactly one swing (length 1.0) and is played at whatever speed
# makes it last the swing's cooldown, so the thrust lands on the hit: at the attack's upswing x 0.5.
# The grip follows the resource pack spear attack the player already knows: turned 90 degrees and slid
# 9 pixels back so the spear points forward past the hand. Every stab starts and ends in READY, the
# spear held at the shoulder and aimed ahead, so stabs chain without a seam. Each eases back into READY
# over the rest of its swing rather than snapping back and waiting.
GRIP, GRIP_POS = [90, 0, 0], [0, -9, 0]
READY = ([11.8, -6.3, -25.8], [-2, 4, -4], GRIP)
AIM = [12.8, 9.1, -74.9]      # forearm turned in, spear aimed at the crosshair

def stab(keys):
    """keys: (time, arm rotation, arm position, grip rotation, mode) between READY at 0 and 1."""
    rot = [(0.0, READY[0], C)] + [(t, r, m) for t, r, p, g, m in keys] + [(1.0, READY[0], C)]
    pos = [(0.0, READY[1], C)] + [(t, p, m) for t, r, p, g, m in keys] + [(1.0, READY[1], C)]
    grip = [(0.0, READY[2], C)] + [(t, g, m) for t, r, p, g, m in keys] + [(1.0, READY[2], C)]
    return rot, pos, grip, [(0.0, GRIP_POS, L), (1.0, GRIP_POS, L)]

# 1: fast, pulled back low on the right and driven in just right of centre. Hit at 0.25.
s1 = stab([
    (0.16, AIM, [-7, -2, 1], GRIP, C),
    (0.25, AIM, [-4, -3, -15], GRIP, L),
    (0.33, AIM, [-4, -3, -13.5], GRIP, C),
])
# 2: fast, swung over to the left and driven in left of centre. Hit at 0.25.
s2 = stab([
    (0.16, [30, 9.1, -100], [5, -1, -2], [90, 0, -30], C),
    (0.25, [24, 9.1, -95], [4, -3, -15], [90, 0, -22], L),
    (0.33, [24, 9.1, -95], [4, -3, -13.5], [90, 0, -22], C),
])
# 3: centred, a longer draw straight back before a deeper thrust. Hit at 0.35.
s3 = stab([
    (0.22, AIM, [0, -1, 3], GRIP, C),
    (0.35, AIM, [-1, -3, -18], GRIP, L),
    (0.45, AIM, [-1, -3, -16.5], GRIP, C),
])
# 4: heavy. A short coil down, then the hand is hauled up and back with the tip angled down at the
# target, held a beat, and driven down and forward along the aim line. It overshoots, jolts back on
# impact and stays planted before it comes back. Hit at 0.45, just right of centre like stab 1.
# The grip never turns off GRIP: Punchy rotates the grip about a pivot the slide has already moved
# past the hand, so any other grip angle swings the spear off the hand and leaves it floating.
s4 = stab([
    (0.1,  [20, -4, -36], [-2, 1, 2.5], GRIP, C),
    (0.28, [27, 4, -54], [-3, 7.5, 5], GRIP, C),
    (0.38, [30, 5, -57], [-3, 8.5, 6], GRIP, L),
    (0.45, [6, 9.1, -76], [-4, -2, -20], GRIP, L),
    (0.48, [11, 9.1, -74], [-4, 0.5, -15], GRIP, L),
    (0.53, [8, 9.1, -75], [-4, -1, -17.5], GRIP, C),
    (0.68, [9, 9.1, -75], [-4, -0.8, -16.5], GRIP, C),
])

def spear_clip(rot, pos, grip, grip_pos, length=1.0, timeline=None):
    bones = {
        "right_arm": {"rotation": track(rot), "position": track(pos)},
        "left_arm": {"rotation": track(mirror_rot(rot)), "position": track(mirror_pos(pos))},
        "itemgrip_right": {"rotation": track(grip), "position": track(grip_pos)},
        "itemgrip_left": {"rotation": track(mirror_rot(grip)), "position": track(mirror_pos(grip_pos))},
    }
    out = {"animation_length": length, "bones": bones}
    if timeline:
        out["timeline"] = {"0.0": timeline}
    return out

# Return to idle: the tail of the same resource pack spear attack. The spear flips off the thrust grip,
# spins once in the hand and settles upright while the arm comes back.
ret_rot = [(0.0, READY[0], C), (0.158, [1.0, 15.1, -57.6], C), (0.225, [10.4, 12.4, -1.6], C),
           (0.292, [11.3, 19.8, -28.3], C), (0.342, [0, 0, 0], C), (0.392, [-10, 0, 0], C),
           (0.608, [0, 0, 0], C), (0.65, [0, 0, 0], C)]
ret_pos = [(0.0, READY[1], C), (0.392, [0, 0, 0], C)]
ret_grip = [(0.0, GRIP, L), (0.033, [0, 0, 47.5], L), (0.3, [-360, 0, 47.5], L),
            (0.392, [-367.5, 0, 0], L), (0.608, [-360, 0, 0], L), (0.65, [-360, 0, 0], L)]
ret_grip_pos = [(0.0, GRIP_POS, L), (0.033, [2, -3, 0], L), (0.65, [0, 0, 0], L)]

punchy = {"format_version": "1.8.0", "animations": {
    "bcenchantments_uppercut": clip(up_rot, up_pos, up_grip, 1.0),
    # dual_handed makes Punchy animate the off arm with its own track instead of leaving it idle.
    "bcenchantments_air_crit": clip(cr_rot, cr_pos, cr_grip, 1.15, "dual_handed;anim_speed_1;"),
    # "attack" in the names lets Punchy treat them as attacks (sprint and fall blending).
    "bcenchantments_spear_attack_1": spear_clip(*s1),
    "bcenchantments_spear_attack_2": spear_clip(*s2),
    "bcenchantments_spear_attack_3": spear_clip(*s3),
    "bcenchantments_spear_attack_4": spear_clip(*s4),
    "bcenchantments_spear_return": spear_clip(ret_rot, ret_pos, ret_grip, ret_grip_pos, 0.65, "anim_speed_1;"),
}}
p = os.path.join(res, "assets/minecraft/punchy/animations")
os.makedirs(p, exist_ok=True)
json.dump(punchy, open(os.path.join(p, "bcenchantments.animation.json"), "w"), indent=1)

# ---------- Better Combat / Player Animation Library (Emotecraft format, radians) ----------
def emote(name, desc, frames, begin=8, end=20, stop=30):
    moves = []
    for tick, easing, parts in frames:
        for part, axes in parts.items():
            for axis, value in axes.items():
                moves.append({"tick": tick, "easing": easing, "turn": 0, part: {axis: value}})
    return {"name": name, "author": "bcenchantments", "description": desc,
            "emote": {"isLoop": "false", "returnTick": 2, "beginTick": begin, "endTick": end,
                      "stopTick": stop, "degrees": False, "moves": moves}}

uppercut = emote("uppercut", "One handed rising uppercut", [
    (8, "EASEINQUAD", {
        "rightArm": {"pitch": 0.75, "yaw": 0.25, "roll": 0.95, "z": 1.0},
        "rightItem": {"pitch": -0.4, "roll": -1.95, "x": 1.64, "y": -2.08, "z": -0.66},
        "leftArm": {"pitch": -0.6, "roll": -0.3},
        "torso": {"pitch": 0.35, "yaw": -0.55},
        "head": {"pitch": 0.25, "yaw": -0.4},
        "rightLeg": {"pitch": -0.35},
        "leftLeg": {"pitch": 0.3},
    }),
    (10, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.2, "yaw": 0.6, "roll": 0.4, "z": -1.2},
        "rightItem": {"pitch": -1.1, "roll": -3.0, "x": 0.02, "y": -1.28, "z": -0.44},
        "torso": {"pitch": -0.1, "yaw": 0.45},
        "head": {"pitch": -0.1, "yaw": 0.3},
    }),
    (12, "EASEOUTQUAD", {
        "rightArm": {"pitch": -2.6, "yaw": 0.75, "roll": 0.6, "z": -2.6},
        "rightItem": {"pitch": -1.6, "roll": -3.15, "x": 0.1, "y": -0.6, "z": -0.6},
        "leftArm": {"pitch": 0.5, "yaw": -0.5, "roll": -0.3},
        "torso": {"pitch": -0.35, "yaw": 0.95, "roll": -0.2},
        "head": {"pitch": -0.35, "yaw": 0.7},
        "rightLeg": {"pitch": 0.2},
        "leftLeg": {"pitch": -0.15},
    }),
    (16, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -2.75, "z": -2.7},
        "torso": {"pitch": -0.3, "yaw": 0.9},
    }),
    (20, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -2.6, "z": -2.6},
        "torso": {"pitch": -0.25, "yaw": 0.85},
    }),
])

# Air crit: both arms mirrored so the hands meet on the centre line, the sword straight overhead,
# then brought straight down the middle to point ahead and below.
air_crit = emote("air_crit", "Two handed centred overhead strike, swung down in the air on a critical hit", [
    (7, "EASEOUTQUAD", {
        "rightArm": {"pitch": -3.0, "yaw": 0.42, "roll": 0.0, "z": 1.0},
        "leftArm": {"pitch": -3.0, "yaw": -0.42, "roll": 0.0, "z": 1.0},
        "rightItem": {"pitch": 0.0, "yaw": 0.0, "roll": 0.0, "x": -0.6, "y": 0.0, "z": 0.0},
        "torso": {"pitch": 0.35},
        "head": {"pitch": -0.25},
        "rightLeg": {"pitch": -0.6},
        "leftLeg": {"pitch": -0.6},
    }),
    (10, "EASEINQUAD", {
        "rightArm": {"pitch": -1.1, "yaw": 0.5, "roll": 0.0, "z": -1.6},
        "leftArm": {"pitch": -1.1, "yaw": -0.5, "roll": 0.0, "z": -1.6},
        "rightItem": {"pitch": -0.3, "yaw": 0.0, "roll": 0.0, "x": -0.6, "y": 0.2, "z": -1.0},
        "torso": {"pitch": -0.7},
        "head": {"pitch": 0.4},
        "rightLeg": {"pitch": -0.3},
        "leftLeg": {"pitch": -0.3},
    }),
    (12, "EASEOUTQUAD", {
        "rightArm": {"pitch": -0.9},
        "leftArm": {"pitch": -0.9},
        "torso": {"pitch": -0.8},
    }),
    (20, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -0.95},
        "leftArm": {"pitch": -0.95},
        "torso": {"pitch": -0.75},
    }),
], begin=7)

# Spear combo, one-handed. Better Combat speeds the animation up through the upswing so the hit
# always lands at endTick / 2, so every thrust sits at tick 10. Arm x/y/z are absolute (rest -5, 2, 0).
# The item keeps Better Combat's own one-handed stab grip, which points the weapon along the arm.
ITEM_DRAWN = {"x": 1.48, "y": -1.97, "z": -0.55, "pitch": -1.42, "yaw": -0.07, "roll": 1.58}
ITEM_THRUST = {"x": 0.98, "y": -2.08, "z": -1.78, "pitch": -1.38, "yaw": 0.0, "roll": 1.56}
ITEM_HELD = {"x": 0.76, "y": -2.41, "z": -0.42, "pitch": -1.39, "yaw": -0.03, "roll": 1.54}

spear_1 = emote("spear_stab_right", "Fast spear stab from the right", [
    (6, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.3, "yaw": -0.7, "roll": 0.0, "x": -7.2, "z": 0.8},
        "rightItem": ITEM_DRAWN,
        "leftArm": {"pitch": -0.45, "yaw": 0.25, "roll": -0.9},
        "torso": {"yaw": -0.6, "pitch": 0.1},
        "rightLeg": {"pitch": 0.25},
        "leftLeg": {"pitch": -0.15},
    }),
    (10, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.62, "yaw": 0.45, "roll": 0.15, "x": -5.6, "z": -2.6},
        "rightItem": ITEM_THRUST,
        "leftArm": {"pitch": 0.3, "yaw": 0.1, "roll": -0.8},
        "torso": {"yaw": 0.5, "pitch": -0.25, "roll": -0.15},
        "rightLeg": {"pitch": -0.15},
        "leftLeg": {"pitch": 0.3},
    }),
    (15, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -1.6, "yaw": 0.4, "z": -2.1},
        "rightItem": ITEM_HELD,
        "torso": {"yaw": 0.42, "pitch": -0.2},
    }),
], begin=6, stop=24)

spear_2 = emote("spear_stab_left", "Fast spear stab from the left", [
    (6, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.35, "yaw": 0.5, "roll": -0.1, "x": -4.2, "z": 0.6},
        "rightItem": ITEM_DRAWN,
        "leftArm": {"pitch": -0.3, "yaw": 0.2, "roll": -0.7},
        "torso": {"yaw": 0.38, "pitch": 0.08},
        "rightLeg": {"pitch": 0.15},
        "leftLeg": {"pitch": -0.2},
    }),
    (10, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.6, "yaw": -0.15, "roll": 0.1, "x": -5.4, "z": -2.6},
        "rightItem": ITEM_THRUST,
        "leftArm": {"pitch": 0.35, "yaw": 0.0, "roll": -0.9},
        "torso": {"yaw": -0.2, "pitch": -0.25, "roll": 0.1},
        "rightLeg": {"pitch": -0.2},
        "leftLeg": {"pitch": 0.25},
    }),
    (15, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -1.58, "yaw": -0.1, "z": -2.1},
        "rightItem": ITEM_HELD,
        "torso": {"yaw": -0.15, "pitch": -0.2},
    }),
], begin=6, stop=24)

spear_3 = emote("spear_stab_center", "Delayed centred spear stab", [
    (7, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.25, "yaw": -0.25, "roll": 0.0, "x": -6.0, "z": 2.2},
        "rightItem": ITEM_DRAWN,
        "leftArm": {"pitch": -0.6, "yaw": 0.3, "roll": -0.8},
        "torso": {"yaw": -0.35, "pitch": 0.15},
        "rightLeg": {"pitch": 0.35},
        "leftLeg": {"pitch": -0.2},
    }),
    (10, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.62, "yaw": 0.35, "roll": 0.1, "x": -5.5, "z": -3.2},
        "rightItem": ITEM_THRUST,
        "leftArm": {"pitch": 0.4, "yaw": 0.1, "roll": -1.0},
        "torso": {"yaw": 0.4, "pitch": -0.35},
        "rightLeg": {"pitch": -0.45},
        "leftLeg": {"pitch": 0.35},
    }),
    (14, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -1.6, "yaw": 0.3, "z": -2.6},
        "rightItem": ITEM_HELD,
        "torso": {"yaw": 0.35, "pitch": -0.3},
    }),
], begin=7, stop=26)

spear_4 = emote("spear_heavy", "Heavy downward spear thrust", [
    (4, "EASEOUTQUAD", {
        "rightArm": {"pitch": -2.4, "yaw": 0.1, "roll": 0.05, "x": -5.2, "z": 0.5},
        "rightItem": ITEM_DRAWN,
        "leftArm": {"pitch": -0.9, "yaw": 0.3, "roll": -0.4},
        "torso": {"yaw": -0.25, "pitch": 0.2},
        "rightLeg": {"pitch": 0.2},
        "leftLeg": {"pitch": -0.25},
    }),
    (8, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -2.85, "yaw": 0.15, "roll": 0.1, "x": -5.3, "z": 1.2},
        "leftArm": {"pitch": -1.2, "yaw": 0.4, "roll": -0.3},
        "torso": {"yaw": -0.3, "pitch": 0.3},
        "leftLeg": {"pitch": -0.3},
    }),
    (10, "EASEINQUAD", {
        "rightArm": {"pitch": -1.15, "yaw": 0.25, "roll": 0.1, "x": -5.4, "z": -3.2},
        "rightItem": ITEM_THRUST,
        "leftArm": {"pitch": 0.5, "yaw": 0.1, "roll": -1.0},
        "torso": {"yaw": 0.35, "pitch": -0.55},
        "rightLeg": {"pitch": -0.55},
        "leftLeg": {"pitch": 0.4},
    }),
    (11, "EASEOUTQUAD", {
        "rightArm": {"pitch": -1.05, "z": -2.9},
        "torso": {"pitch": -0.62},
    }),
    (14, "EASEINOUTQUAD", {
        "rightArm": {"pitch": -1.12, "z": -3.0},
        "rightItem": ITEM_HELD,
        "torso": {"yaw": 0.3, "pitch": -0.5},
    }),
], begin=8, stop=30)

p = os.path.join(res, "assets/bcenchantments/player_animations")
os.makedirs(p, exist_ok=True)
json.dump(uppercut, open(os.path.join(p, "uppercut.json"), "w"), indent=1)
json.dump(air_crit, open(os.path.join(p, "air_crit.json"), "w"), indent=1)
for anim in (spear_1, spear_2, spear_3, spear_4):
    json.dump(anim, open(os.path.join(p, anim["name"] + ".json"), "w"), indent=1)
print("ok")
