# Trim Powers & Combinations

Living design sheet for Better Combat: RPG Additions. Update it whenever a power or combination changes.

## Powers

Trim Powers (Powers for short) are enchantments that a trim can also grant. Levels I–III unless noted. Charged powers show as bars under the crosshair.

| Power | Type | What it does | Numbers (I / II / III) | Weapons | Based on |
|---|---|---|---|---|---|
| Lightning | Charged | Bolt on the target after a long combo (2 full combos − 2 hits) | 7 / 13 / 17 dmg | sharp weapons | Dungeons *Thundering* |
| Shockwave | Charged | Wave flies out after 3 swings in a row that each hit 3+ enemies | 4 / 6 / 8 dmg, 8 / 10 / 12 blocks | sharp weapons | Dungeons *Shockwave* |
| Chains | Charged | +5% per hit, +2% per extra enemy; charge lasts 8 s, survives misses. When full, nearby enemies and the target are rooted 0.7 s | binds 2 / 3 / 4 within 6 blocks | sharp weapons | Dungeons *Chains* |
| Echo | Charged | Every 4th swing its targets are struck again 0.3 s later; counts toward other bars, triggers no perks | 50 / 65 / 80% of the swing | sharp weapons | Dungeons *Echo* |
| Cold Edge | On hit | Hits slow; 3 s of chill freezes the target solid (freeze damage). Swords chill everything the swing hits | −10 / 25 / 40% speed | sharp weapons | Dungeons *Freezing* |
| Wind Charged | On kill | Kills burst like a wind charge (2.5 blocks), hurting and pushing | 3 / 4 / 5 dmg | sharp weapons, mace | Dungeons *Exploding* + vanilla wind charge |
| Uppercut | On hit | Sword's 3rd swing, spear's heavy thrust or a mace swung from the ground launches the target | launch 0.55 / 0.7 / 0.85 | swords, mace (spear via trim) | Original |

## Combinations

Active when the weapon has every listed power (enchantment or trim, any level). Shown in gold in the tooltip and in the Field Guide.

| Combination | Needs | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies get half-power bolts and a 0.5 s root instead |
| Charged Lightning | Lightning + Wind Charged | Bolt bursts like a wind charge (plain Lightning has no knockback) |
| Updraft | Uppercut + Wind Charged | You and the target go straight up, you one block above, while it hangs stunned 1.5 s. **Mace:** next swing in the air smashes as if from 6 blocks |
| Frozen Chains | Chains + Cold Edge | Bound enemies are chilled |
| Frost Burst | Cold Edge + Wind Charged | Death bursts chill |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Tempest | Shockwave + Wind Charged | Shockwave throws enemies much harder; hitting a wall deals 4 / 6 / 8 knockback damage |
| Frenzy | Echo + Refinement 5 | Echo every 3rd swing |
| Shatter | Cold Edge + Lightning | Bolts deal +15% to slowed enemies, ×2 to frozen ones (which also chills nearby enemies). Kills spread chill to up to 6 enemies within 2 blocks |

Chained Lightning + Wind Charged: enemies killed by the chain bolts burst.

## Weapon styles

| Weapon | Style |
|---|---|
| Sword | Normal Better Combat combo. Air crit: any swing in the air that rolls a crit becomes a two-handed, centred overhead strike. 3rd swing on the ground becomes a rising cut with Uppercut |
| Spear | Four-stab combo; heavy thrust launches |
| Mace | Swing on the ground launches with Uppercut; swing in the air is the mace smash |

## Vanilla changes

| Enchantment | Change |
|---|---|
| Fire Aspect | Each melee hit adds 1 burn stack (max 5); each stack deals 0.2 hearts/s. Burn lasts 4 s per level after the last hit; water and rain put it out. Armor stands never burn |
| Attack Speed | No longer an enchantment; comes from refinement instead |

## Refinement

Smithing table, empty template slot, weapon in the middle, emeralds or emerald blocks on the right. The **?** button beside the smithing table opens the Field Guide.

| Level | Cost (±10%) | Attack speed | Random boost |
|---|---|---|---|
| 1 | 60 | +0.1 | damage, crit chance or crit damage |
| 2 | 80 | +0.2 | " |
| 3 | 100 | +0.3 | " |
| 4 | 120 | +0.4 | " |
| 5 | 140 | +0.5 | " |

## Trim Powers

At refinement 5: trim template + echo shard → trim's power at level 1. Same trim again, no shard → level 2, then 3.

| Power | Trims | Why |
|---|---|---|
| Lightning | bolt, spire, eye | storm bolt, sky-high End city, ender eye |
| Wind Charged | flow, dune, wayfinder | breeze vaults, desert winds, the traveller |
| Shockwave | rib, snout | fortress and bastion brutes |
| Cold Edge | tide, coast | the sea |
| Echo | silence, ward | ancient city, home of echo shards |
| Chains | wild, vex | binding vines, evoker fangs |
| Uppercut | sentry, raiser, shaper, host | outposts and trail ruins |

## Shield powers

No refinement needed: trim template + shield + echo shard → the power, one level only. Only these work on a shield:

| Power | Trims | On a blocked hit | Cooldown |
|---|---|---|---|
| Wind Charged | flow, dune, wayfinder | Gust from the shield throws back everything in front (4 blocks, 30% of a wind charge) | 30 s |
| Cold Edge | tide, coast | The attacker, within 5 blocks, is chilled hard (−40% speed) | 10 s |

## Mobs

| Change | Value |
|---|---|
| Monster spawn cap | ×1.8 |
| Hit chain | First monster hit 0.75× damage, +0.25 per further hit (max 2×); resets after 2 s without a monster hit |
| Health | +0–30% per monster, middle values common, the ends rare |
| Damage | +0–15% per monster, rolled evenly |
| Armor / knockback resistance | Small bonus, more with local difficulty and nearby player power |
| Player power | Power levels on the strongest nearby player's weapons (48 blocks, max 12) |
| Mob farms | Scaled mobs still die from their original fall height |

## Settings

| Setting | Where | What it does |
|---|---|---|
| Better Combat On / Off | Mod Menu → settings | Off restores vanilla attacks and sweeping, for farms. Singleplayer / LAN host only |
| Charge bars | Mod Menu → settings | Under or beside the crosshair, offset, size |

## Ideas

| Idea | What it does | Combinations |
|---|---|---|
| Gravity | Hits pull nearby enemies slightly toward the struck target | Singularity (+ Shockwave): enemies pulled into the wave's path · Black Hole (+ Chains): bound enemies pull others in · Cyclone (+ Wind Charged): kills pull enemies in before the burst |
| Momentum | Moving shortly before attacking empowers knockback / the weapon's special | Launch (+ Uppercut): you and the target are launched · Blast Step (+ Wind Charged): empowered kills give a forward burst |
| Rampaging | Kills grant attack speed for a short time, refreshed by further kills | Another route into fast builds; pairs with Frenzy |
