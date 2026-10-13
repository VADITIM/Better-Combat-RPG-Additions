# Decisions

Shipped work and why it was built this way, newest first. Dates are when it was decided.

## 2026-10-13

**Combat**

- **Air crit replaces the airborne sword slams.** The sword combo stays Better Combat's own. A swing in the air that rolls a crit becomes a centred, two-handed overhead strike. *Rejected:* the earlier 3rd-swing one-handed and two-handed slams; the user wanted the combo untouched.
- **Updraft + mace:** after the launch, the next mace swing in the air smashes as if from 6 blocks. Player and target go straight up, the player one block above the target. *Why:* the mace gets a unique two-beat trait. Slow Falling resets `fallDistance`, so it is set by hand before the attack.
- **Chains roots for 0.7 s instead of pulling.** *Rejected:* the pull felt uncontrolled.
- **Tempest throws enemies much harder** and deals knockback damage on wall impact (`perk/Impact`).
- **Shatter:** +15% lightning damage to slowed enemies, ×2 to frozen ones. Kills spread chill to up to 6 enemies within 2 blocks.
- **Cold Edge freezes after 3 s** of chill (was longer).

**Powers and progression**

- **Every trim maps to a fitting power, and every power has trims.** The mapping is thematic: by where the trim is found and what it looks like (`POWERS.md`). *Open:* whether the user wants 18 distinct powers instead.
- **"Perks" are called "Trim Powers" / "Powers"** for players. Code identifiers keep `perk`.
- **Shield trims:** a trim, the shield and an echo shard, with no refinement and one level. Only Wind Charged (gust, 30% of a wind charge, 30 s) and Cold Edge (chill the attacker, 10 s). *Why "only a few":* the user's request. Cold Edge was added as the second one and is open to removal.
- **Attack Speed stopped being a power.** Refinement grants +0.1 per level, the old maximum at level 5. *Why:* better build-crafting. Frenzy became Echo + Refinement 5.
- **Fire Aspect stacks burn** (+1 stack per melee hit, max 5, 0.2 hearts/s per stack, 4 s per level after the last hit). Armor stands never burn. *Why:* vanilla Fire Aspect is too strong on the first hit.

**Monsters**

- **Spawn cap ×1.8.** Health +0–30% per monster (mean of three rolls, so middle values are common). Damage +0–15%, rolled evenly. Health no longer scales with difficulty or player power; armor and knockback resistance still do.

**Settings and guide**

- **Better Combat switch** in settings, so vanilla sweeping farms work. Implemented as `getAttributes → null`, which restores vanilla attacks and sweeping. *Rejected for now:* BC's `CombatFlags`, which keeps the sweep suppressed.
- **Field Guide screen behind a "?" button** on the smithing table, replacing a command.

**Naming**

- **The mod is named "Better Combat: RPG Additions"**; the id stays `bcenchantments` so world data survives.

## 2026-10-12

- **V/AS base register, cut down for a game:** monochrome accent and the Minecraft font only (`rules/frontend.md`).
- **Refinement with emeralds at the smithing table,** five levels, then trim + echo shard into a power, raised to III by repeat trims.
