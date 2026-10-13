# Better Combat: RPG Additions

A Fabric mod for Minecraft 26.2 that layers a combat RPG onto vanilla, built on
[Better Combat](https://modrinth.com/mod/better-combat) and Punchy (both required).

- **Weapon playstyles**: each weapon kind plays its own way, with its own attacks and animations
  (the spear's four-stab combo, the sword's air crit and uppercut).
- **Trim Powers**, Dungeons-style: Lightning, Shockwave, Chains, Echo, Cold Edge, Uppercut, Wind Charged,
  Gravity, Momentum and Rampaging,
  reworked from Minecraft Dungeons enchantments.
- **Vanilla-friendly progression**: refine weapons at the smithing table with emeralds, then turn a
  finished weapon's trim and an echo shard into a power. Shields take a few powers too.
- **Harder hack-and-slash PvE**: more monsters, softer single hits that ramp up when they keep
  landing, and subtle hidden stat buffs that grow with the player's weapon power.

## Guide

In game, the **?** button beside the smithing table opens the Field Guide, with the recipes laid out in item icons.

**Refining.** At a smithing table, put a weapon in the middle slot and emeralds or emerald blocks in the
right slot, with the template slot empty. There are five levels, about 500 emeralds in all. Each
adds attack speed (+0.1 per level, +0.5 at level 5) and one small random boost (damage, critical chance or
critical damage).

**Powers** are enchantments, from the enchanting table, loot and villagers, and every one can also come from a trim. Lightning, Shockwave, Chains
and Echo charge over a combo and show as bars under the crosshair; the rest work on every hit.

**Trim Powers.** A weapon at refinement level 5 takes an armor trim template and an echo shard at the
smithing table and gains that trim's power at level 1. Another template of the same trim, without a
shard, raises it to 2 and then 3.

| Power | Trims |
|---|---|
| Lightning | bolt |
| Wind Charged | flow, dune |
| Cold Edge | tide, coast |
| Echo | silence |
| Shockwave | ward, rib |
| Chains | wild, vex |
| Gravity | eye, spire |
| Momentum | wayfinder, host |
| Rampaging | snout, sentry |
| Uppercut | raiser, shaper |

**Shield powers.** A shield needs no refinement: a trim template, the shield and an echo shard. Only
a few powers work on a shield, at one level:

| Power | On a blocked hit |
|---|---|
| Wind Charged | A gust throws back everything in front of the shield at 30% of a wind charge's strength; every 30 s |
| Cold Edge | The attacker, if close, is chilled hard; every 10 s |

**Combining powers.** Powers on the same weapon change each other; a trim power counts as well. Active
combinations show in gold in the tooltip.

| Combination | Powers | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies are struck by half-power lightning and briefly rooted instead |
| Charged Lightning | Lightning + Wind Charged | Lightning bursts like a wind charge |
| Updraft | Uppercut + Wind Charged | Player and target go straight up, the player a block above, while the target hangs stunned; with a mace the next swing in the air smashes as if from 6 blocks |
| Frozen Chains | Chains + Cold Edge | Bound enemies are chilled |
| Frost Burst | Cold Edge + Wind Charged | Death bursts chill what they catch |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Tempest | Shockwave + Wind Charged | The shockwave throws enemies much harder; hitting a wall deals knockback damage. A mace smash also hurts enemies within 2.5 blocks of the impact for 30% of its damage |
| Frenzy | Echo + Refinement 5 | Echo every third swing instead of every fourth |
| Shatter | Cold Edge + Lightning | Lightning deals 15% more damage to slowed enemies, and double to enemies frozen solid, which also chills those around them; kills spread chill to up to 6 enemies nearby |
| Singularity | Gravity + Shockwave | The shockwave drags the enemies beside its path into it |
| Black Hole | Gravity + Chains | Every bound enemy pulls the free enemies around it in |
| Cyclone | Gravity + Wind Charged | Kills draw the enemies around them in before the burst |
| Launch | Momentum + Uppercut | A launch after a run-up goes higher, and you go up after the target |
| Blast Step | Momentum + Wind Charged | A kill by a swing after a run-up bursts you onward |

## Building

Punchy is not on a Maven repository. Put `punchy-2.8a-fabric-26.2.jar` in `libs/`, then run:

```
./gradlew build
```

The jar lands in `build/libs/`.

`tools/animations.py` generates the first-person and third-person spear animations:

```
python tools/animations.py src/main/resources
```
