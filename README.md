# Better Combat: RPG Additions

A Fabric mod for Minecraft 26.2 that layers a combat RPG onto vanilla, built on
[Better Combat](https://modrinth.com/mod/better-combat) and Punchy (both required).

- **Weapon playstyles**: each weapon kind plays its own way, with its own attacks and animations
  (the spear's four-stab combo, the sword's slam and uppercut).
- **Dungeons-style perks**: Lightning, Shockwave, Cold Edge, Uppercut and Attack Speed, reworked
  from Minecraft Dungeons enchantments.
- **Vanilla-friendly progression**: refine weapons at the smithing table with emeralds, then turn a
  finished weapon's trim and an echo shard into a perk.
- **Harder hack-and-slash PvE**: more monsters, softer single hits that ramp up when they keep
  landing, and subtle hidden stat buffs that grow with the player's weapon power.

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
