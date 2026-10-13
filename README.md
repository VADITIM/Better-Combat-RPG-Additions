# Trim Enchantments

A Fabric mod for Minecraft 26.2 that adds a late-game enchanting layer on top of vanilla's.
Regular enchantments still carry the early and mid game; once a weapon is fully refined, put a trim
on it at the smithing table and it gains that trim's **Trim Power**; put on two and they combine. Built on
[Better Combat](https://modrinth.com/mod/better-combat) and Punchy (both required).

- **Weapon playstyles**: each weapon kind plays its own way, with its own attacks and animations
  (the spear's four-stab combo, the sword's air crit and uppercut).
- **Trim Powers**, Dungeons-style: one power for each of the 18 armor trims, combining in pairs,
  reworked from Minecraft Dungeons enchantments.
- **Vanilla-friendly progression**: refine weapons at the smithing table with emeralds, then add trims
  and echo shards to a finished weapon for its powers. Shields take a few powers too.
- **Harder hack-and-slash PvE**: more monsters, softer single hits that ramp up when they keep
  landing, and subtle hidden stat buffs that grow with the player's weapon power.
- **Visible powers**: each trim power plays its own animation on the weapon's pixels, in the inventory and in hand.
- **Enemy health bars**: a small upright bar beside each hurt monster's head shows its health, with the damage of
  the current combo held in white until the combo ends.
- **Elites and captains**: some monsters spawn as elites whose hits slow, weaken or strip armor.
  Captains wear an ominous banner and hurry and shield the monsters around them.
- **Harder by place**: mild near world spawn, harder underground, in the Deep Dark, the Nether and the End.
- **Ominous difficulty**: drink an ominous bottle to make monsters tougher on purpose. While Bad
  Omen lasts, kills can drop emeralds (5% at I, up to 9% at V), and at level V an echo shard (0.2%).

## Guide

In game, the **?** button beside the smithing table opens the Field Guide, with the recipes laid out in item icons.

**Refining.** At a smithing table, put a weapon in the middle slot and emeralds or emerald blocks in the
right slot, with the template slot empty. There are five levels, about 500 emeralds in all. Each
adds attack speed (+0.05 per level, +0.25 at level 5) and one small random boost (damage, critical chance or
critical damage).

**Powers** come only from trims: every armor trim grants its own power on a weapon. Lightning,
Shockwave, Chains and Echo charge over a combo and show as bars under the crosshair; the rest work on
every hit.

**Trim Powers.** A weapon at refinement level 5 takes an armor trim template and an echo shard at the
smithing table and gains that trim's power at level 1. A different trim and an echo shard add another
power for experience levels (20 for the second, 10 more for each after). The same trim again, without a
shard, raises its power to 2 and then 3. A weapon holds any number of powers.

| Power | Trim | | Power | Trim |
|---|---|---|---|---|
| Lightning | bolt | | Uppercut | raiser |
| Wind Charged | flow | | Venom* | wild |
| Cold Edge | tide | | Sandstorm* | dune |
| Echo | silence | | Plunder* | coast |
| Shockwave | ward | | Withering* | rib |
| Chains | vex | | Phase* | spire |
| Gravity | eye | | Shared Pain* | host |
| Momentum | wayfinder | | Guarding Strike* | sentry |
| Rampaging | snout | | Daze* | shaper |

\* Designed, not in the game yet; their trims are refused until they are.

**Shield powers.** A shield needs no refinement: a trim template, the shield and an echo shard. Only
a few powers work on a shield, at one level:

| Power | On a blocked hit |
|---|---|
| Wind Charged | A gust throws back everything in front of the shield at 30% of a wind charge's strength; every 30 s |
| Cold Edge | The attacker, if close, is chilled hard; every 10 s |

**Combining powers.** Every two powers have a combination. Powers pair up in the order they were put on
(1st with 2nd, 3rd with 4th, …), and a power already in a combination is locked out of every other. At
the smithing table, hovering a refined weapon lists what its next trim would make. Active combinations
show in gold in the tooltip; hold Shift for descriptions. The built ones:

| Combination | Powers | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies are struck by half-power lightning and briefly rooted instead |
| Charged Lightning | Lightning + Wind Charged | Lightning bursts like a wind charge |
| Updraft | Uppercut + Wind Charged | Player and target go straight up, the player twice as high as a block above it, both falling normally. Right click in the air: the mace slams down for a 1.5× smash, the sword dives and cuts around where it lands, the spear (after a launch, without Lunge) lunges through enemies. Lunge works in the air only after a launch |
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

`tools/power_effects.py` generates the animation each trim power plays on its weapon:

```
python tools/power_effects.py src/main/resources
```

## License

All Rights Reserved. See [LICENSE](LICENSE). Better Combat and Punchy are separate mods under their own licenses.
