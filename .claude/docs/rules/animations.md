# Attacks and animations: Better Combat, Punchy and this mod

How a swing goes from a key press to an animation on screen and damage on the server, and how to add
or change one. Studied against Better Combat 3.2.2, Player Animation Library (PAL) and Punchy 2.8a for
Minecraft 26.2 on 2026-10-13. When either mod updates, re-check §8 before trusting the rest.

---

## 1. Who does what

| Piece | Side | Owns |
|---|---|---|
| **Better Combat (BC)** | both | Which attack a swing is (`weapon_attributes` JSON), hit timing (upswing), hit detection (client), damage multipliers, combo count, third-person animation through PAL |
| **PAL** | client | Plays BC's Emotecraft-format animations on the player model (third person, and first person when BC is allowed to) |
| **Punchy** | client only | First-person arms and items. Turns BC's first-person animation off and plays its own Bedrock-format clips instead |
| **This mod** | both | Swaps attacks per weapon style (`weapon/`), hands Punchy the clips for special attacks (`client/PunchyClips`, `client/PunchySpear`), and reads every server swing (`combat/ComboTracker`) |

So every special attack needs **two animations**: a PAL emote for third person (what other players
and F5 see) and a Punchy clip for first person (what you see). The data decides the hit; the
animations only have to land their impact frame on the hit.

## 2. One swing, end to end

1. **Client, key press** (`AttackInteractor.startUpswing`). BC asks
   `PlayerAttackHelper.getCurrentAttack(player, comboCount)`. Our `PlayerAttackHelperMixin` runs every
   result through `WeaponStyles.replace`, so a style can hand back a different `Attack` (the sword's
   air crit and uppercut). BC then:
   - computes `cooldown` = `max(getCurrentItemAttackStrengthDelay(), attack_interval_cap)` ticks
     (our `PlayerMixin` scales it per attack through `WeaponStyle.cooldownFactor`);
   - computes `upswingTicks = max(round(cooldown × upswing × upswing_multiplier), 1)`, with
     `upswing_multiplier` = 0.5 by default;
   - plays the PAL animation, sends it to the server to forward to everyone else, and fires
     `BetterCombatClientEvents.ATTACK_START`. **Our `PunchyClips` and `PunchySpear` listen here.**
2. **Client, upswingTicks later** (`performAttack`). BC finds targets with the attack's hitbox
   (`TargetFinder`, client-side only), calls `player.attack` locally, sends `C2S_AttackRequest`
   (combo count, target ids, cursor target) and fires `ATTACK_HIT`. **Punchy listens here** and
   queues its first-person attack clip. The combo count goes up by one.
3. **Server** (`ServerNetwork.handleAttackRequest`). BC calls `getCurrentAttack` again with the
   client's combo count, so our styles run again, **now on the server, at hit time**. It adds the
   attack's `damage_multiplier` as a temporary modifier, attacks each listed target, then sets the
   combo count to −1. Our `ServerNetworkMixin` brackets this with
   `ComboTracker.beginSwing`/`endSwing`, and `PlayerMixin` wraps every `player.attack`, so perks
   see one `SwingResult` per swing.

The combo index is `comboCount % attacks.length`. It resets when the player idles for
`cooldown × combo_reset_rate` (3 by default) or switches to another item.

## 3. Better Combat data: `weapon_attributes`

**Where:** `data/<ns>/weapon_attributes/<path>.json` registers the item `<ns>:<path>`. A file
whose id is not an item, such as `data/bcenchantments/weapon_attributes/spear.json`, is only a
preset that other files can name as `"parent"`. Our seven vanilla spears are one-line files with
`"parent": "bcenchantments:spear"`.

**Shape:**

```json
{ "parent": "bettercombat:sword",
  "attributes": {
    "attack_range": 0, "range_bonus": 1, "two_handed": false, "category": "spear",
    "pose": "bettercombat:pose_two_handed_polearm",
    "attacks": [ {
      "hitbox": "FORWARD_BOX",            // or VERTICAL_PLANE, HORIZONTAL_PLANE
      "damage_multiplier": 0.65, "movement_speed_multiplier": 1.0, "range_multiplier": 1.0,
      "angle": 0, "upswing": 0.5,
      "animation": "bcenchantments:spear_stab_right",
      "conditions": ["NOT_MOUNTED"],
      "swing_sound": {"id": "bettercombat:spear_stab", "pitch": 1.15},
      "impact_sound": {"id": "..."},
      "trail_particles": [ {"particle_type": "stab", "x_addition": 0.12, "y_addition": -0.12,
                            "z_addition": 0, "local_yaw": 0, "pitch_addition": 0, "roll_set": 0} ]
    } ] } }
```

**How `parent` merges** (`WeaponAttributesHelper.override`):

- **Attacks merge by index**, and the child's list length wins. A child with two attacks under a
  four-attack parent ends up with two.
- **Zero and null mean "inherit".** A child cannot set `damage_multiplier`, `angle` or `upswing`
  back to 0, or clear `conditions`, below a parent that set them.
- **Validation** (after merging) rejects an attack with no `hitbox` or no `animation`, or with a
  negative multiplier, angle or upswing. A rejected file is logged and skipped, and the item
  silently plays vanilla.

**`conditions`** filter the attack list *before* indexing, so the combo length changes with the
situation (dual wielding, mounted, ...). `OFF_HAND_SHIELD` is broken in BC 3.2.2 (it always passes).

**The registry is synced from the server.** A client playing on a server uses the server's data.
In singleplayer both sides share the same static maps, which matters in §8.

## 4. Third person: PAL emotes

**Where:** `assets/<ns>/player_animations/<name>.json`, referenced as `<ns>:<name>`. Our files are
generated by `tools/animations.py` (§6). BC's own files (`bettercombat:one_handed_slam` and others)
are good references; unpack the BC jar to read them.

**Format:** Emotecraft JSON, `"degrees": false` (radians). Each move is
`{"tick": t, "easing": "EASEINQUAD", "turn": 0, "<part>": {"<axis>": value}}`. The parts are `head`,
`torso`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`, `rightItem` and `leftItem`; the axes are
`pitch`, `yaw`, `roll`, `x`, `y` and `z`. Arm `x`/`y`/`z` are absolute: the rest pose is (−5, 2, 0)
for the right arm and (5, 2, 0) for the left.

**Timing: the impact frame belongs at `endTick / 2`.** BC plays the animation with a two-gear
speed. Up to the hit it runs at `endTick / (cooldown × upswing)` per tick, so whatever the weapon's
speed or the attack's `upswing`, the hit lands when the animation reaches `endTick × 0.5` (with the
default `upswing_multiplier` of 0.5). After the hit it slows into the wind-down. Our animations use
`endTick` 20, so the impact sits at tick 10. `beginTick` and `stopTick` only shape the blend in and
out.

**Mirroring:** BC mirrors the animation for off-hand attacks and left-handed players. Author
everything right-handed.

**Legs** are switched off while swimming, riding, or (with `legAnimationThreshold` > 0) moving.
The body pitch is added on top by BC (`AttackAnimationStack`), so author it for a player looking
straight ahead.

## 5. First person: Punchy clips

**What Punchy does to BC:**

- `BetterCombatCompat.applyVanillaItemDisplay` sets BC's `firstPersonAnimations` to NO, so BC never
  draws first person.
- On `ATTACK_START` and `ATTACK_HIT`, in first person, it stops BC's attack animation on the local
  player.
- `AttackStateMachine.tick` plays a first-person clip on the hit. It looks in this order:
  1. the critical clip, if the swing looked like a vanilla falling crit and the item has a tuned
     `critical_hit` entry;
  2. `resolveBetterCombatClip`, the BC combo clips from Punchy's tuning config;
  3. `resolveCustomAttackClip`, the item's own custom clip;
  4. the generic `attack` clip (`attack_mace` / `attack_trident` for those items).
- Spears only: in first person, Punchy makes `WeaponRegistry.getAttributes(Identifier)` return
  null so it can play its own spear stab (`BetterCombatWeaponRegistryMixin`).

**How our special clips get in** (`PunchyClips` + `mixin/punchy/*`):

1. `ATTACK_START` → `PunchyClips.onAttackStart` stores the clip name for the special animation,
   valid for 500 ms.
2. While one is pending, `PunchyCustomAnimationResolverMixin` cancels the critical clip and
   `PunchyAttackStateMachineMixin` cancels the BC combo lookup.
3. `resolveCustomAttackClip` is answered with our clip through
   `PunchyAnimationManager.resolveNamedClip`.

It enters through the custom lookup on purpose: Punchy freezes the off arm for two-handed clips
that come from the BC lookup.

The spear is different (`PunchySpear`). It drives Punchy's `PoseHandler` itself from the spear
state machine's tick: it plays each stab at a speed that stretches it over the swing's cooldown,
holds the ready pose between stabs, and plays a return clip when the combo ends.

**Where clips live:** any `.json` under `assets/minecraft/punchy/animations/` or
`assets/punchy/animations/`, from any resource pack. Ours are all in
`assets/minecraft/punchy/animations/bcenchantments.animation.json`. Clips are looked up **by
animation name across every pack**, so every name starts with `bcenchantments_`. A name ending
`_left` or `_right` is a handed variant: `resolveNamedClip("x")` tries `x_right` or `x_left` for
the hand, then `x`.

**Format:** Bedrock/GeckoLib (`"format_version": "1.8.0"`), degrees and pixels, keyframe times in
seconds. Each keyframe is `{"post": [x, y, z], "lerp_mode": "catmullrom"|"linear"}`. Bones:
`right_arm`, `left_arm`, `itemgrip_right`, `itemgrip_left`.

**Timeline effects** (`"timeline": {"0.0": "a;b"}`):

| Effect | Meaning |
|---|---|
| `dual_handed` | Animate the off arm with its own track. Without it the off arm stays idle |
| `dual_handed_if_has_off_item` | The same, only when the off hand holds something |
| `anim_speed_N` | Play at N× real time (clamped 0.5–5.5), **ignoring Punchy's global speed and any speed multiplier passed in** |

Without `anim_speed_N`, Punchy plays a clip at its global `ANIMATION_SPEED` (default 5.5) times
the multiplier. `PunchySpear` relies on this: `speed = 1 / (seconds × ANIMATION_SPEED)` makes a
1.0-long clip last exactly `seconds`. **A clip with `anim_speed_1` runs at a fixed length** whatever
the weapon's attack speed (this is how the uppercut and air-crit clips play today).

**Spear grip:** the spear the user knows comes from the "Untitled Punchies" pack's `attack_spear`,
with the grip turned [90, 0, 0] and slid [0, −9, 0]. Without the slide the spear renders reversed.

## 6. `tools/animations.py`: the source of truth

Both the PAL emotes and the Punchy clip file are **generated**. Never hand-edit
`player_animations/*.json` or `bcenchantments.animation.json` and leave the script behind. The
next run overwrites them, as it would have brought back the removed slams before 2026-10-13.

```bash
python tools/animations.py src/main/resources
```

- Punchy clips: write rotation, position and grip tracks as `(time, [x, y, z], mode)` lists, then
  `clip(rot, pos, grip, length, timeline)` (the left arm is mirrored automatically) or
  `spear_clip(...)`.
- PAL emotes: `emote(name, description, [(tick, easing, {part: {axis: value}}), ...], begin, end,
  stop)`.
- Add every new clip to the `punchy` dict and every new emote to the `json.dump` list at the end.

To check a run changed only what you meant, generate into a scratch folder and compare the JSON
with `src/main/resources`.

## 7. Recipes

### A new special attack for an existing weapon (like the air crit)

1. **Style** (`weapon/<Weapon>Style.java`):
   - add `public static final String NAME = TrimEnchantments.MOD_ID + ":name";`
   - return it from `replace()` through a `with(hand, NAME, angle, trail)` helper that copies the
     base `Attack` and changes the animation, hitbox, angle and trail;
   - if it launches, slows the cooldown or changes knockback, override `launches`,
     `cooldownFactor` (and `shapesCooldown`) or `knockbackFactor`.
2. **Emote:** add it to `tools/animations.py`, with the impact at tick 10 when `endTick` is 20.
3. **Punchy clip:** add `bcenchantments_<name>` to the script's `punchy` dict. Use
   `dual_handed;` if both hands hold the weapon. Leave out `anim_speed_1` if it should follow the
   swing's length.
4. **`PunchyClips.onAttackStart`:** map the animation id to the clip name.
5. Regenerate, build, deploy.

### A new combo for a whole weapon kind (like the spear)

1. Write a preset at `data/bcenchantments/weapon_attributes/<kind>.json` with every attack, and
   one-line `"parent"` files at `data/<item ns>/weapon_attributes/<item>.json` for each item.
2. Write a `WeaponStyle` whose `matches` recognises the preset (as `SpearStyle` does by its first
   animation), and register it in `WeaponStyles.init` before any broader style that would also
   match.
3. One emote per attack, with the impact at `endTick / 2`.
4. First person: if Punchy's BC combo clips are good enough, nothing else is needed. For exact
   control, follow `PunchySpear`: queue the clip on `ATTACK_START` and play it from a Punchy
   state-machine tick through `AnimationIntentCoordinator.request`.

### Only changing numbers

Damage per attack, reach and hit shape live in the JSON. Cooldown per attack and knockback live
in the style. The data needs no rebuild of the animations, but the jar still has to be rebuilt.

## 8. Edge cases and traps

**Client and server disagree:**

- **`replace()` runs twice, and at different moments:** on the client when the upswing starts, and
  on the server when the hit request arrives, `upswingTicks` later. Anything it reads that can change
  in between (on the ground or in the air, the crit roll, perk levels) can give the server a
  different attack than the one animated.
  - The server's copy decides `launches`, `cooldownFactor` and `knockbackFactor`.
  - The hit list is the client's, found with the client's hitbox.
  - Keep `replace()` deterministic. Base it on state both sides see the same way, such as the
    synced crit roll (`CriticalHits.next`).
- **The server takes the client's targets on trust.** It only checks the range, and only when
  `server_target_range_validation` is on (it is off by default). A new hitbox shape changes only
  which targets the client sends.

**Singleplayer and dedicated servers:**

- **Punchy's spear hiding leaks into singleplayer.** The integrated server shares BC's static
  registry and reads the camera's first-person state, so without `WeaponRegistryMixin` the server
  sees no attributes for spears in first person. The same mixin stops BC's fallback from replacing
  the spear preset (`WeaponAttributesFallbackMixin`).
- **The Better Combat switch is a static flag.** It works in singleplayer and for a LAN host. On a
  dedicated server only the server's value counts. BC's own `CombatFlags.setAttacksDisabled(player,
  true)` is the per-player, synced alternative, but it does not undo BC's sweep suppression.
- **Punchy is client-only, but `fabric.mod.json` lists it in `depends`.** A dedicated server
  without Punchy refuses to load the mod (see `known-issues.md`).

**Off hand and dual wielding:**

- **During an off-hand attack BC swaps the hands** (`swapHandAttributes`). Inside `player.attack`,
  `getMainHandItem()` is the off-hand weapon. Read the swing's `AttackHand.itemStack()`, not the
  hand slots.
- **Dual wielding** alternates hands on even and odd combo counts, and each hand's combo index is
  `count / 2`. A "3rd swing" special is the main hand's 3rd swing, which is the 5th swing overall.

**Timing and blending:**

- **`PunchyClips` forgets a pending clip after 500 ms.** Fine at normal speeds; an attack with a
  very slow upswing (cooldown × upswing × 0.5 > 10 ticks) would lose its first-person clip.
- **Punchy throttles its attack clips** to 3.5 per second (`AttackStateMachine`). Very fast weapons
  skip visual swings; `PunchySpear` avoids this by playing its own clips.
- **Punchy's falling crit:** Punchy picks its own critical clip from vanilla's falling check, not
  from our rolled crit. Only a pending special attack cancels it.
- **Feint key and item swaps** cancel the swing (`cancelWeaponSwing`), so no `ATTACK_HIT`
  follows. `PunchySpear` handles it by returning to idle after `RETURN_DELAY` idle ticks; any new
  first-person state has to do the same.

**Fragile hooks:**

- **`ServerNetworkMixin` targets the synthetic `lambda$handleAttackRequest$2`.** Any BC update that
  reorders lambdas breaks it. It is `require = 1`, so it fails loudly at launch.
- **Punchy mixins are `@Pseudo` with `require = 0`.** A Punchy update that renames a method makes
  them silently do nothing. The symptom is first-person specials falling back to Punchy's normal
  swing. Check `resolveCriticalName`, `resolveBetterCombatClip`, `resolveCustomAttackClip`,
  `SpearStateMachine.tick` and `resolveSpearAttackClip`.
- **`PunchySpear` imports Punchy classes directly.** It is safe only because Punchy is required on
  the client. If Punchy ever becomes optional, guard it behind `FabricLoader.isModLoaded("punchy")`.

**Other:**

- **The sweep:** BC suppresses vanilla's sweep whenever `getCurrentAttack` finds an attack. Turning
  BC off for an item (returning null from `getAttributes`) restores vanilla attacks and sweeping
  for it. That is how the Better Combat switch works.
- **Armor-stand markers** are skipped by BC. Normal armor stands are hit but must never break
  (`rules/gameplay.md`).

## 9. Checklist before calling an animation change done

- [ ] `tools/animations.py` updated and run; generated JSON matches what is committed.
- [ ] Impact frame at `endTick / 2` (emote) and on the hit in the first-person clip.
- [ ] Clip name starts with `bcenchantments_`; `PunchyClips` (or `PunchySpear`) maps to it.
- [ ] `replace()` gives the same attack on both sides for the same situation.
- [ ] `./gradlew build -q`, jar copied to the Modrinth profile; the user tests in game.
- [ ] `map.md`, `quirks.md` and `POWERS.md` updated if behaviour or files changed.
