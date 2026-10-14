# Decisions

Shipped work and why it was built this way, newest first. Dates are when it was decided.

## 2026-10-13 (combinations rework)

- **One power per trim, 18 in all, and a combination for every pair (153).** 8 powers and 140 combinations are drafted in `POWERS.md` for review before they are built.
- **Powers pair in the order applied** (1st+2nd, 3rd+4th, …); a power in a combination is locked out of every other. Simple to read off a weapon, no player choice needed.
- **Combining happens at the smithing table, not the anvil.** A new power costs 20 levels for the 2nd, +10 per further one; the anvil refuses a right-hand item with powers so they are never lost.
- **No books.** The enchantment JSONs stay registered (world safety for items that carry them) but apply to nothing and are hidden.

## 2026-10-13 (powers)

- **Ten powers over the 18 trims,** each trim matched by name, look or structure (`Refinement.TRIM_PERKS`, table in `POWERS.md`). Lightning (bolt) and Echo (silence) have one trim each.
- **Gravity, Momentum and Rampaging shipped** with Singularity, Black Hole, Cyclone, Launch and Blast Step. Momentum needs a sprint run-up (≥ 0.4 s) and is spent by one swing, so it rewards charging in rather than walking. Rampaging counts monster kills only, so animal farms do not feed it.
- **Shockwave particles cut to 40%** at the user's request.

## 2026-10-13

**Combat**

- **Air crit replaces the airborne sword slams.** The sword combo stays Better Combat's own. A swing in the air that rolls a crit becomes a centred, two-handed overhead strike. *Rejected:* the earlier 3rd-swing one-handed and two-handed slams; the user wanted the combo untouched.
- **Updraft's air move is on right click, one per weapon kind.** Player and target go straight up, the player twice the height of a block above the target, with no Slow Falling and no hanging target. In the air, right click with the Updraft weapon (main hand first, else off hand): the mace slams down fast and smashes the ground for 1.5× a mace smash from that height; the sword dives straight down and cuts everything around the landing (+10% damage per block fallen, up to 10); the spear lunges along the look and strikes what it passes through. Once per airtime, from any jump or fall, not only after the launch; except the spear's lunge, which needs an Updraft launch and a spear without Lunge (with Lunge, the enchantment does the lunge after a launch). Lunge never fires in the air otherwise (`LivingEntityMixin`), so a spear cannot chain lunges off a plain jump. Left click stays a normal attack, except while a move runs (a slam or dive until it lands, a lunge for 10 ticks): then no attack lands and the other hand cannot be used, so the move is the triggering hand's only action. *Rejected:* the earlier mace-only rule where the next left click in the air smashed as if from 6 blocks; the user wanted left click normal and a move for every weapon.
- **Chains roots for 0.7 s instead of pulling.** *Rejected:* the pull felt uncontrolled.
- **Tempest throws enemies much harder** and deals knockback damage on wall impact (`perk/Impact`).
- **Shatter:** +15% lightning damage to slowed enemies, ×2 to frozen ones. Kills spread chill to up to 6 enemies within 2 blocks.
- **Cold Edge freezes after 3 s** of chill (was longer).

**Powers and progression**

- **Every trim maps to a fitting power, and every power has trims.** The mapping is thematic: by where the trim is found and what it looks like (`POWERS.md`). *Open:* whether the user wants 18 distinct powers instead.
- **"Perks" are called "Trim Powers" / "Powers"** for players. Code identifiers keep `perk`.
- **Shield trims:** a trim, the shield and an echo shard, with no refinement and one level. Only Wind Charged (gust, 30% of a wind charge, 30 s) and Cold Edge (chill the attacker, 10 s). *Why "only a few":* the user's request. Cold Edge was added as the second one and is open to removal.
- **Attack Speed stopped being a power.** Refinement grants +0.05 per level, +0.25 at level 5 (it was +0.1, halved on 2026-10-13 so refinement stays a small bonus; the spear got its speed back in its own style, +75%). *Why:* better build-crafting. Frenzy became Echo + Refinement 5.
- **Fire Aspect stacks burn** (+1 stack per melee hit, max 5, 0.2 hearts/s per stack, 4 s per level after the last hit). Armor stands never burn. *Why:* vanilla Fire Aspect is too strong on the first hit.

**Power effects**

- **Effects play on the item's pixels, not around the slot.** The old Lightning overlay blitted a 24 px strike around the icon in GUIs only. Now every power's effect is extra item quads drawn with the glint pipeline (equal depth test, additive), so it is masked to the sprite and shows in hand too. UVs come from model positions, so one effect pixel lands on one item pixel. *Rejected:* a custom `FoilType` (an enum, awkward to extend) and per-item model overrides (one per weapon).

**Monsters**

- **Spawn cap ×1.8.** Health +0–30% per monster (mean of three rolls, so middle values are common). Damage +0–15%, rolled evenly. Health no longer scales with difficulty or player power; armor and knockback resistance still do.
- **Difficulty plan (from an outside proposal), what was taken:** elites and captains, status attacks, place multipliers, and a power measure that counts power pairs. *Rejected:* a threat level from recent power damage and adaptive resistance (both punish using the powers you earned), Echo Strain weapon upkeep (a chore, and experience cost already balances powers), and flanking AI (a rewrite of vanilla AI for little gain).
- **Elites hurt with statuses, not damage.** Frost, Weakening and Shredding add pressure without turning monsters into sponges. Captains are the kill-first target that breaks up click-and-slash fights; the ominous banner reuses vanilla's raid-captain look. Ranks are entity tags, saved with the monster, and their health goes into the same health modifier so mob farms keep their drop height.
- **Place multiplies every bonus.** Near spawn ×0.5 keeps the home base calm; caves ×1.25, Deep Dark and Nether ×1.5, Soul Sand Valley and Basalt Deltas ×1.65, End ×1.75.
- **Each place also has a named tier** (Regular to Possible Death) that picks monster armor and is shown on the HUD. Bad Omen raises it, so Possible Death is only reached by choice. Armor drops at 4% (vanilla 8.5%) so farms don't flood with it.
- **The trident is the spacing weapon.** Sword is close combat, spear versatile reach, trident specialised reach plus mobility: Perfect Spacing at the end of its reach, with Loyalty, Riptide and Channeling feeding it. It has less reach than the spear and no bonus up close, so it is not a better spear.
- **Player power counts pairs.** Weapons hold unlimited powers now, so the measure is power levels + 2 per pair, capped at 30 instead of 12, with smaller per-point armor and knockback resistance.
- **Ominous bottles are the difficulty dial.** Bad Omen I–V adds health +6%, damage +3%, armor +0.5 and knockback resistance +0.02 per level to monsters spawning near the player, and the player's monster kills drop an emerald at 4% + 1% per level, plus an echo shard at 0.2% at level V. It reuses a vanilla item, it is opt-in, and it pays in the refinement currencies. The buff is applied at spawn like the rest of the scaling, so drinking a bottle affects new monsters, not ones already loaded.

**Settings and guide**

- **Better Combat switch** in settings, so vanilla sweeping farms work. Implemented as `getAttributes → null`, which restores vanilla attacks and sweeping. *Rejected for now:* BC's `CombatFlags`, which keeps the sweep suppressed.
- **Minimal particles is a client setting.** The server wraps the mod's particle packets so each client thins them by its own setting: bursts keep a fifth (at least one), lone particles in lines and rings one in three, gust emitters become one small gust. `WindBurst` now sends its gust emitter directly instead of a silent, pushless explosion, so it can be thinned too.
- **Field Guide screen behind a "?" button** on the smithing table, replacing a command.

**Naming**

- **The mod is named "Trim Enchantments"** (previously "Better Combat: RPG Additions"): the mod is framed around Trim Powers, a late-game enchanting layer added on top of vanilla enchantments, which stay the early and mid game. Trims never replace enchantments. The id stays `bcenchantments` so world data survives; the main classes are `TrimEnchantments` and `TrimEnchantmentsClient`.

## 2026-10-12

- **V/AS base register, cut down for a game:** monochrome accent and the Minecraft font only (`rules/frontend.md`).
- **Refinement with emeralds at the smithing table,** five levels, then trim + echo shard into a power, raised to III in the smithing panel for 64 emeralds a level, and lowered again for a full refund so a level can be tried out.
