# Trim Enchantments

A Fabric mod for Minecraft 26.2 that adds Trim Powers, a late-game enchanting layer on top of
vanilla enchantments (which stay the early and mid game): every trim grants a Trim Power on a fully
refined weapon, and any two combine. Around that core it has:

- Trim Powers adapted from Minecraft Dungeons enchantments;
- weapon refinement at the smithing table;
- a playstyle for each weapon kind;
- a monster overhaul.

It is an addon to **Better Combat** and **Punchy**, and requires both.

- **Mod id:** `bcenchantments`. Never rename it: item data saved in worlds depends on it.
- **Display name:** "Trim Enchantments". Describe the mod as being about Trim Powers; players still see "Trim Powers" (or "Powers") for the powers themselves.
- **Repository:** https://github.com/VADITIM/Trim-Enchantments, branch `master`.

V/AS: this project uses the **base** register, cut down for a game. The cut-down rules are in `docs/rules/frontend.md`, and `client/Theme.java` implements them.

## Stack

- Java 25, Fabric Loader 0.19.5, Fabric API, Loom.
- Minecraft 26.2 uses unobfuscated Mojang names. GUI drawing goes through `GuiGraphicsExtractor`: screens override `extractRenderState` and `extractBackground`.
- Better Combat 3.2.2 and Player Animation Library come from the Modrinth maven.
- Punchy 2.8a compiles against `libs/punchy-2.8a-fabric-26.2.jar`. It is not on a maven and the jar is not committed.
- Mixins use MixinExtras.

## Commands

```bash
./gradlew build -q
```
```bash
cp build/libs/trim-enchantments-0.1.0.jar "/c/Users/vn/AppData/Roaming/ModrinthApp/profiles/Main 1.0.0/mods/"
```
```bash
python tools/animations.py src/main/resources
```
```bash
python tools/power_effects.py src/main/resources
```

1. Build.
2. Deploy: copy the jar after every build.
3. Regenerate the animations: every PAL emote and Punchy clip is generated, never hand-edited.
4. Regenerate the power effect textures the same way.

## Working rules

- **Commit or push only when asked.** Never add Claude attribution to commits, PRs or pushes.
- **Don't test in game.** The user tests every change in game themselves; build and deploy, then hand over. Live sessions happen only when asked; see `docs/quirks.md` § Testing.
- **Keep docs current in the same change.** `docs/POWERS.md` is the user's design sheet on GitHub; update it whenever a power, combination, number or trim changes.
- **Releases:** on command.

## Docs

- `docs/map.md`: where everything is.
- `docs/architecture.md`: how the parts connect.
- `docs/decisions.md`: what was chosen and why.
- `docs/quirks.md`: traps.
- `docs/known-issues.md`: current bugs and debt.
- `docs/roadmap.md`: planned work.
- `docs/POWERS.md`: every power, combination and number.

@docs/rules/gameplay.md
@docs/rules/frontend.md

Before touching attacks, weapon styles, `weapon_attributes`, PAL emotes or Punchy clips, read
`docs/rules/animations.md`. It explains how Better Combat and Punchy work with this mod, gives
recipes, and lists the edge cases.
