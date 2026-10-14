# Trim Powers & Combinations

Living design sheet for Trim Enchantments. Update it whenever a power or combination changes.

## Powers

Trim Powers (Powers for short) come only from trims: every armor trim grants its own power, and there are no enchanted books of them. Levels I–III. Charged powers show as bars under the crosshair. Any power goes on any weapon.

| Power | Type | What it does | Numbers (I / II / III) | Weapons | Based on |
|---|---|---|---|---|---|
| Lightning | Charged | Bolt on the target after a long combo (2 full combos − 2 hits) | 7 / 13 / 17 dmg | sharp weapons | Dungeons *Thundering* |
| Shockwave | Charged | Wave flies out after 3 swings in a row that each hit 3+ enemies | 4 / 6 / 8 dmg, 8 / 10 / 12 blocks | sharp weapons | Dungeons *Shockwave* |
| Chains | Charged | +5% per hit, +2% per extra enemy; charge lasts 8 s, survives misses. When full, nearby enemies and the target are rooted 0.7 s | binds 2 / 3 / 4 within 6 blocks | sharp weapons | Dungeons *Chains* |
| Echo | Charged | Every 4th swing its targets are struck again 0.3 s later; counts toward other bars, triggers no perks | 50 / 65 / 80% of the swing | sharp weapons | Dungeons *Echo* |
| Cold Edge | On hit | Hits slow; 3 s of chill freezes the target solid (freeze damage). Swords chill everything the swing hits | −10 / 25 / 40% speed | sharp weapons | Dungeons *Freezing* |
| Wind Charged | On kill | Kills burst like a wind charge (2.5 blocks), hurting and pushing | 3 / 4 / 5 dmg | sharp weapons, mace | Dungeons *Exploding* + vanilla wind charge |
| Gravity | On hit | Every landed swing pulls the enemies around the struck target toward it | radius 3 / 4 / 5 blocks, pull 0.35 / 0.45 / 0.55 | weapons | Dungeons *Gravity* |
| Momentum | Run-up | Sprint ≥ 0.4 s, then land a swing within 0.5 s: its targets are thrown back hard. Spent by that swing | knockback 0.5 / 0.7 / 0.9 | weapons | Original |
| Rampaging | On kill | Killing a monster grants attack speed for 4 s, refreshed by every further kill | +20 / 30 / 40% attack speed | weapons | Dungeons *Rampaging* |
| Uppercut | On hit | Sword's 3rd swing, spear's heavy thrust or a mace swung from the ground launches the target | launch 0.55 / 0.7 / 0.85 | swords, spear, mace | Original |

### Draft powers (for review, not built)

Their trims are refused at the smithing table until they are built.

| Power | Trim | Type | What it does | Numbers (I / II / III) | Based on |
|---|---|---|---|---|---|
| Venom | wild | On hit | Hits poison. A target poisoned for 3 s bursts into a small poison cloud (2 blocks, 3 s), once per 8 s per target | Poison 2 / 3 / 4 s | Dungeons *Jungle's Poison* / *Poison Cloud* |
| Sandstorm | dune | Charged | Every 5th landed swing throws sand: enemies around are blinded 2 s, losing their target and wandering | radius 3 / 4 / 5 | Original (desert pyramid) |
| Plunder | coast | On kill | Monster kills can drop an emerald | 4 / 7 / 10% | Dungeons *Prospector* |
| Withering | rib | On hit | Hits wither; withered enemies deal 15% less damage | Wither I 2 / 3 / 4 s | Dungeons *Weakening* + wither skeleton |
| Phase | spire | Charged | Every 6th landed swing teleports you behind the target (within 4 blocks); your next hit always crits | 6 / 5 / 4 swings | Ender / shulker teleports |
| Shared Pain | host | On kill | A kill's overkill damage carries to the nearest enemy | 3 / 4 / 5 blocks | Dungeons *Shared Pain* |
| Guarding Strike | sentry | On kill | Kills grant Resistance I | 2 / 3 / 4 s | Dungeons *Guarding Strike* |
| Daze | shaper | Charged | Every 5th landed swing dazes its targets: they can neither move nor attack | 0.8 / 1.0 / 1.2 s | Dungeons *Stunning* |

## Combinations

Every two powers have one combination: 18 powers, 153 combinations, 17 for each power. Powers combine in pairs **in the order they were put on**: the 1st with the 2nd, the 3rd with the 4th, and so on. A power already in a combination is locked out of every other, so a 3rd power stays alone until a 4th joins it. A weapon holds any number of powers. Active combinations show in gold in the tooltip and in the Field Guide. At the smithing table, hovering a refined weapon lists what its next trim would make (white: you carry the trim, grey: you don't). Holding Shift over a weapon shows a short description under its trim power and each combination.

| Combination | Needs | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies get half-power bolts and a 0.5 s root instead |
| Charged Lightning | Lightning + Wind Charged | Bolt bursts like a wind charge (plain Lightning has no knockback) |
| Updraft | Uppercut + Wind Charged | You and the target go straight up, you twice as high as a block above it; neither hangs, both fall normally. **Right click in the air** (once per airtime, either hand; left click stays normal, but while the move runs, until a slam or dive lands or for 0.5 s of a lunge, no attack lands and the other hand is disabled): **mace** slams down fast and smashes the ground within 0.75 blocks of your body's edge for 1.5× a mace smash from that height; **sword** dives straight down, 25% slower than the slam (start speed and gravity), and hits everything within 3 blocks for its attack damage +10% per block fallen (max +100%); **spear**, only after an Updraft launch and only without the Lunge enchantment, lunges along the look (speed 1.2, Lunge I's hunger), hitting every enemy it passes through once for its attack damage (enchantment bonuses and on-hit effects apply) and pushing them along. Lunge itself works in the air only after an Updraft launch. Slam and dive landings take no fall damage; each move costs 1 durability |
| Frozen Chains | Chains + Cold Edge | Bound enemies are chilled |
| Frost Burst | Cold Edge + Wind Charged | Death bursts chill |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Tempest | Shockwave + Wind Charged | Shockwave throws enemies much harder; hitting a wall deals 4 / 6 / 8 knockback damage. **Mace:** a smash also hurts enemies within 2.5 blocks of the impact for 30% of its damage |
| Frenzy | Echo + Refinement 5 | Echo every 3rd swing (not a pair: any weapon with Echo, since trims need refinement 5) |
| Singularity | Gravity + Shockwave | The wave drags enemies within 4 blocks of the wave's own centre into its path (120% of Gravity's pull) |
| Black Hole | Gravity + Chains | Every bound enemy pulls the free enemies within 3.5 blocks in |
| Cyclone | Gravity + Wind Charged | Kills pull enemies within 5 blocks in; the burst goes off 0.3 s later |
| Launch | Momentum + Uppercut | A launching swing after a run-up throws the targets ×1.3 higher, and you go up after them |
| Blast Step | Momentum + Wind Charged | A kill by a swing after a run-up bursts you onward (once per swing) |
| Shatter | Cold Edge + Lightning | Bolts deal +15% to slowed enemies, ×2 to frozen ones (which also chills nearby enemies). Kills spread chill to up to 6 enemies within 2 blocks |

Chained Lightning + Wind Charged: enemies killed by the chain bolts burst.

### Draft combinations (for review, not built)

The other 140 pairs, grouped by the first power. Once approved they get built; until then a pair without a built combination does nothing but still locks both powers.

**Lightning**

| Combination | Needs | Effect |
|---|---|---|
| Thunderwave | Lightning + Shockwave | Every enemy the shockwave cuts is struck by a quarter-power bolt |
| Lightning Rod | Lightning + Gravity | Before the bolt lands, enemies within 4 blocks are pulled to the target |
| Thunder Charge | Lightning + Momentum | A run-up swing adds 3 hits of Lightning charge |
| Storm Frenzy | Lightning + Rampaging | While rampaging, Lightning charges twice as fast |
| Skybreaker | Lightning + Uppercut | Launched targets are struck by a half-power bolt at the top of their flight |
| Toxic Storm | Lightning + Venom | Bolts leave a poison cloud where they strike |
| Dust Devil | Lightning + Sandstorm | Every enemy Sandstorm blinds is struck by a quarter-power bolt |
| Gilded Storm | Lightning + Plunder | Bolt kills roll Plunder twice |
| Black Lightning | Lightning + Withering | Bolts wither everything within 2 blocks |
| Blink Strike | Lightning + Phase | Phase calls a half-power bolt on the target |
| Arc | Lightning + Shared Pain | A bolt kill's overkill jumps to the nearest enemy as a bolt |
| Static Guard | Lightning + Guarding Strike | While Guarding Strike's Resistance lasts, enemies that hit you are struck by a quarter-power bolt |
| Stunbolt | Lightning + Daze | Bolts daze the target and everything within 2 blocks |

**Wind Charged**

| Combination | Needs | Effect |
|---|---|---|
| Gale Echo | Wind Charged + Echo | Echo strikes push their targets back like a small gust |
| Whiplash | Wind Charged + Chains | When Chains fires, bound enemies are yanked to the target |
| Hurricane | Wind Charged + Rampaging | While rampaging, death bursts are 50% wider |
| Toxic Gust | Wind Charged + Venom | Death bursts poison what they catch |
| Haboob | Wind Charged + Sandstorm | Death bursts blind what they catch |
| Windfall | Wind Charged + Plunder | Death bursts blow dropped loot and experience to you |
| Ashen Wind | Wind Charged + Withering | Death bursts wither what they catch |
| Slipstream | Wind Charged + Phase | Phase leaves a gust where you were, throwing enemies there away |
| Shrapnel | Wind Charged + Shared Pain | A kill's overkill is added to its death burst's damage |
| Bulwark Gust | Wind Charged + Guarding Strike | Kill bursts throw enemies away from you, not from the victim |
| Concussion | Wind Charged + Daze | Death bursts daze what they catch for 0.5 s |

**Cold Edge**

| Combination | Needs | Effect |
|---|---|---|
| Hoarfrost Echo | Cold Edge + Echo | Echo strikes chill |
| Glacier Wave | Cold Edge + Shockwave | The shockwave chills what it cuts and frosts the ground it passes (slow, 2 s) |
| Frost Vortex | Cold Edge + Gravity | Pulled enemies are chilled |
| Frostbite Charge | Cold Edge + Momentum | A run-up swing freezes its target solid at once |
| Cold Blood | Cold Edge + Rampaging | While rampaging, chill builds twice as fast |
| Frozen Ascent | Cold Edge + Uppercut | Launched targets freeze solid in the air |
| Frostbite | Cold Edge + Venom | Chilled enemies take poison damage twice as fast |
| Blizzard | Cold Edge + Sandstorm | Sandstorm becomes a blizzard: blinded enemies are chilled too |
| Ice Treasure | Cold Edge + Plunder | Kills on enemies frozen solid roll Plunder twice |
| Necrotic Frost | Cold Edge + Withering | Chill does not thaw on withered enemies |
| Cold Snap | Cold Edge + Phase | Phase freezes the target solid as you appear behind it |
| Shattering Pain | Cold Edge + Shared Pain | Overkill from a frozen kill spreads to two enemies |
| Permafrost | Cold Edge + Guarding Strike | While Guarding Strike's Resistance lasts, enemies that hit you are chilled |
| Deep Freeze | Cold Edge + Daze | Dazed enemies freeze solid at once |

**Echo**

| Combination | Needs | Effect |
|---|---|---|
| Aftershock | Echo + Shockwave | A second, half-damage wave follows every shockwave 0.5 s later |
| Reverberation | Echo + Chains | Echo strikes add Chains charge as full hits |
| Pull Echo | Echo + Gravity | Echo strikes pull nearby enemies to their target |
| Double Step | Echo + Momentum | A run-up swing always echoes |
| Bloodlust | Echo + Rampaging | While rampaging, Echo strikes every 3rd swing |
| Rebound | Echo + Uppercut | Launches echo: the target is launched again 0.3 s later, a little higher |
| Toxic Echo | Echo + Venom | Echo strikes poison |
| Whispering Sands | Echo + Sandstorm | Echo strikes blind their targets for 1 s |
| Echoing Coin | Echo + Plunder | Echo kills roll Plunder twice |
| Withering Echo | Echo + Withering | Echo strikes wither |
| Afterimage | Echo + Phase | After Phase, an echo strikes the target from where you were |
| Resounding Pain | Echo + Shared Pain | Echo kills spread their overkill too |
| Ward Echo | Echo + Guarding Strike | Echo kills grant Guarding Strike's Resistance |
| Ringing Blow | Echo + Daze | Echo strikes daze for 0.4 s |

**Shockwave**

| Combination | Needs | Effect |
|---|---|---|
| Snare Wave | Shockwave + Chains | The shockwave roots what it cuts for 0.5 s |
| Surge | Shockwave + Momentum | A run-up swing fires the shockwave with 2 of its 3 swings charged |
| Warpath | Shockwave + Rampaging | While rampaging, Shockwave needs 2 swings instead of 3 |
| Upheaval | Shockwave + Uppercut | The shockwave launches what it cuts |
| Plague Wave | Shockwave + Venom | The shockwave leaves a poison trail |
| Sandwave | Shockwave + Sandstorm | The shockwave blinds what it cuts |
| Gold Rush | Shockwave + Plunder | Shockwave kills roll Plunder twice |
| Death Wave | Shockwave + Withering | The shockwave withers what it cuts |
| Rift Wave | Shockwave + Phase | Phase fires a half-power shockwave through the target |
| Crushing Wave | Shockwave + Shared Pain | Each kill adds its overkill to the wave's damage |
| Shield Wall | Shockwave + Guarding Strike | Firing a shockwave grants Resistance I for 2 s |
| Stunwave | Shockwave + Daze | The shockwave dazes what it cuts |

**Chains**

| Combination | Needs | Effect |
|---|---|---|
| Grapple | Chains + Momentum | A run-up swing fires Chains if it is half charged |
| Hunter's Chains | Chains + Rampaging | Kills add 20% Chains charge |
| Chain Lift | Chains + Uppercut | Launching a bound enemy launches every enemy bound with it |
| Venom Links | Chains + Venom | Bound enemies are poisoned |
| Blind Bind | Chains + Sandstorm | Bound enemies are blinded |
| Chain Gang | Chains + Plunder | Bound enemies killed while rooted roll Plunder twice |
| Death Grip | Chains + Withering | Bound enemies wither |
| Anchor | Chains + Phase | Phase teleports you to the farthest bound enemy instead |
| Shared Bonds | Chains + Shared Pain | Damage to the target is shared 30% with every enemy bound to it |
| Iron Ward | Chains + Guarding Strike | Firing Chains grants Resistance I for 2 s |
| Shackles | Chains + Daze | Bound enemies are dazed for as long as they are rooted |

**Gravity**

| Combination | Needs | Effect |
|---|---|---|
| Slingshot | Gravity + Momentum | A run-up swing pulls twice as hard |
| Feeding Frenzy | Gravity + Rampaging | While rampaging, Gravity reaches 2 blocks further |
| Implosion | Gravity + Uppercut | Launched targets pull nearby enemies under them as they rise |
| Miasma | Gravity + Venom | Pulled enemies are poisoned |
| Sinkhole | Gravity + Sandstorm | Pulled enemies sink: slowed 1 s as in quicksand |
| Magnet | Gravity + Plunder | Gravity also pulls dropped items and experience to you |
| Event Horizon | Gravity + Withering | Pulled enemies wither |
| Warp | Gravity + Phase | Phase pulls everything near your old spot to the target |
| Collapse | Gravity + Shared Pain | Overkill splits across every pulled enemy |
| Gravity Well | Gravity + Guarding Strike | Enemies pulled within 2 blocks of you deal 15% less damage for 2 s |
| Crush | Gravity + Daze | Pulled enemies that reach the target are dazed |

**Momentum**

| Combination | Needs | Effect |
|---|---|---|
| Stampede | Momentum + Rampaging | While rampaging, a run-up builds in half the time |
| Toxic Charge | Momentum + Venom | A run-up swing poisons hard (Poison II) |
| Dust Trail | Momentum + Sandstorm | Sprinting kicks up sand that blinds the enemies you pass |
| Smash and Grab | Momentum + Plunder | Run-up kills roll Plunder twice |
| Death Charge | Momentum + Withering | A run-up swing withers its targets (Wither II) |
| Blitz | Momentum + Phase | A run-up swing triggers Phase at once |
| Bowling | Momentum + Shared Pain | Run-up kills throw their overkill at the enemy behind them |
| Shield Charge | Momentum + Guarding Strike | Sprinting grants Resistance I |
| Bull Rush | Momentum + Daze | A run-up swing dazes its targets |

**Rampaging**

| Combination | Needs | Effect |
|---|---|---|
| Juggernaut | Rampaging + Uppercut | While rampaging, every 3rd swing launches |
| Blood Toxin | Rampaging + Venom | While rampaging, poison lasts twice as long |
| Desert Rage | Rampaging + Sandstorm | Kills while rampaging blind the enemies around the victim |
| Pillage | Rampaging + Plunder | While rampaging, Plunder's chance doubles |
| Reaper | Rampaging + Withering | Kills on withered enemies extend the rampage by 2 s |
| Shadow Rush | Rampaging + Phase | While rampaging, Phase charges twice as fast |
| Massacre | Rampaging + Shared Pain | Kills by overkill refresh the rampage and spread again |
| Berserker | Rampaging + Guarding Strike | Each kill while rampaging also heals 1 heart |
| Terror | Rampaging + Daze | Kills while rampaging daze the enemies within 3 blocks |

**Uppercut**

| Combination | Needs | Effect |
|---|---|---|
| Spore Launch | Uppercut + Venom | Launched targets leave a poison cloud where they land |
| Sand Geyser | Uppercut + Sandstorm | Launches blind the enemies around the target |
| Shakedown | Uppercut + Plunder | A launched enemy has a 10% chance to drop an emerald, once each |
| Gallows | Uppercut + Withering | Launched targets wither |
| Air Step | Uppercut + Phase | After a launch, Phase puts you above the target for a follow-up |
| Skyfall | Uppercut + Shared Pain | A launched target that dies spreads its overkill to everything within 2 blocks of where it lands |
| Rising Guard | Uppercut + Guarding Strike | Launching grants Resistance I for 2 s |
| Ragdoll | Uppercut + Daze | Launched targets stay dazed until they land |

**Venom**

| Combination | Needs | Effect |
|---|---|---|
| Toxic Dust | Venom + Sandstorm | Sandstorm also poisons |
| Antidote Trade | Venom + Plunder | Kills on poisoned enemies roll Plunder twice |
| Blight | Venom + Withering | Poison and wither on the same enemy deal 50% more |
| Venom Fang | Venom + Phase | Phase's hit poisons hard (Poison II) |
| Contagion | Venom + Shared Pain | A poisoned enemy's death passes its poison to the nearest enemy |
| Thick Skin | Venom + Guarding Strike | Killing a poisoned enemy grants Resistance for twice as long |
| Paralysis | Venom + Daze | Poisoned enemies stay dazed 50% longer |

**Sandstorm**

| Combination | Needs | Effect |
|---|---|---|
| Buried Treasure | Sandstorm + Plunder | Kills on blinded enemies roll Plunder twice |
| Mummify | Sandstorm + Withering | Blinded enemies wither |
| Mirage | Sandstorm + Phase | Phase raises a sandstorm where you were |
| Sand Blast | Sandstorm + Shared Pain | Overkill spreads to every blinded enemy within 3 blocks |
| Dust Cloak | Sandstorm + Guarding Strike | Blinded enemies deal half damage to you |
| Dust Daze | Sandstorm + Daze | Sandstorm also dazes for 0.5 s |

**Plunder**

| Combination | Needs | Effect |
|---|---|---|
| Grave Robber | Plunder + Withering | Kills on withered enemies drop double experience |
| Pickpocket | Plunder + Phase | Phase's hit rolls Plunder |
| Jackpot | Plunder + Shared Pain | Kills by overkill roll Plunder |
| Treasure Guard | Plunder + Guarding Strike | Picking up an emerald grants Resistance I for 2 s |
| Mugging | Plunder + Daze | Kills on dazed enemies roll Plunder twice |

**Withering**

| Combination | Needs | Effect |
|---|---|---|
| Soul Step | Withering + Phase | Phase withers the enemies around where you appear |
| Decay | Withering + Shared Pain | A withered enemy's death passes its wither to the nearest enemy |
| Death Ward | Withering + Guarding Strike | Withered enemies that hit you take 2 damage |
| Petrify | Withering + Daze | Dazed enemies wither twice as fast |

**Phase**

| Combination | Needs | Effect |
|---|---|---|
| Rift | Phase + Shared Pain | Overkill from Phase's crit spreads to two enemies |
| Blink Guard | Phase + Guarding Strike | Phase grants Resistance I for 2 s |
| Ambush | Phase + Daze | Phase dazes the target |

**Shared Pain**

| Combination | Needs | Effect |
|---|---|---|
| Martyr | Shared Pain + Guarding Strike | Overkill also heals you for 25% of it |
| Shockfall | Shared Pain + Daze | Enemies hit by overkill are dazed |

**Guarding Strike**

| Combination | Needs | Effect |
|---|---|---|
| Stalwart | Guarding Strike + Daze | Kills while Resistance lasts daze the enemies within 3 blocks |

## Weapon styles

| Weapon | Style |
|---|---|
| Sword | Normal Better Combat combo. Air crit: any swing in the air that rolls a crit becomes a two-handed, centred overhead strike. 3rd swing on the ground becomes a rising cut with Uppercut |
| Spear | Four-stab combo, every stab +75% faster; the first two stabs knock back 0.3× a normal hit, the third none, the heavy thrust 2.2× and launches |
| Mace | One attack. A timing weapon: it readies 1.3× slower than vanilla. At 0.8 of that wait it sparkles for 0.15 s (3 ticks); a fresh press then is a **Perfect Hit** for 1.5× damage, every other swing deals 0.9×. A press before the window fumbles it until the next swing, and a held attack key never lands one. Timed swings always hit at full attack strength. Swing on the ground launches with Uppercut; swing in the air is the mace smash (Tempest splashes it) |
| Axe | Two-cleave combo: right to left (1.3× damage), then a backhand left to right (1.5×), each a 110° sweep. After the backhand the next swing waits 0.35 s past the cooldown |
| Trident | The reach and mobility weapon, one-handed, reach +0.75 (the spear's is +1). Three-thrust combo: two quick jabs (0.8×, 0.85× damage, 0.75× cooldown each) and a lunge (1.3×, 1.35× cooldown, 1.6× knockback). **Perfect Spacing:** a thrust whose target is at 75% of its reach or further deals 1.3× damage, with a splash. Loyalty starts that sweet spot 5% nearer per level; during a Riptide spin and 1.5 s after it every thrust is Perfect Spacing; with Channeling, a Perfect Spacing lunge in a thunderstorm calls lightning onto a target under open sky |

## Vanilla changes

| Enchantment | Change |
|---|---|
| Fire Aspect | Each melee hit adds 1 burn stack (max 5); each stack deals 0.2 hearts/s. Burn lasts 4 s per level after the last hit; water and rain put it out. Armor stands never burn |
| Attack Speed | No longer an enchantment; comes from refinement instead |

## Refinement

Only **diamond and netherite** weapons, the mace and the trident can be refined and take trim powers (`bcenchantments:refinable`); lesser weapons keep what they already have. Shields are not affected.

Smithing table, empty template slot, weapon in the middle, emeralds or emerald blocks on the right. The **?** button beside the smithing table opens the Field Guide.

| Level | Cost (±10%) | Attack speed | Random boost |
|---|---|---|---|
| 1 | 60 | +0.05 | damage, crit chance and crit damage, each by 0.25 to 2% |
| 2 | 80 | +0.1 | " |
| 3 | 100 | +0.15 | " |
| 4 | 120 | +0.2 | " |
| 5 | 140 | +0.25 | " |

Damage grows by that share of the weapon's own; crit chance and crit damage by that many points.

Every level also earns a **level point**. With the weapon in the smithing table's middle slot, the side panel spends it on a stat of the player's choice, 3% per point: damage +3% of the weapon's own, crit chance +3 points, crit damage +3 points. Attack speed takes none; spent points are not refunded.

## Trim Powers

All at the smithing table, on a weapon at refinement 5:

| Step | Inputs | Cost |
|---|---|---|
| First power | trim template + weapon + echo shard | 10 levels |
| Another power | a different trim template + weapon + echo shard | 10 more levels for each (2nd 20, 3rd 30, …) |
| Raise a power | the power's trim template + weapon + 64 emeralds; Upgrade in the smithing panel puts them on the table | the trim and 64 emeralds per level (blocks count nine); up to level III |
| Lower a power | Degrade in the smithing panel, then Confirm | nothing is refunded; down to level I |

A weapon or shield taken out of the table upgraded goes straight back in the middle slot.

The anvil refuses an item with trim powers in its right slot, so powers can't be lost merging weapons. One power per trim:

| Power | Trim | Why |
|---|---|---|
| Lightning | bolt | the lightning bolt itself, from the copper trial chambers |
| Wind Charged | flow | the breeze's wind |
| Cold Edge | tide | ocean monument: the cold sea |
| Echo | silence | the ancient city, home of echo shards |
| Shockwave | ward | the warden's sonic boom |
| Chains | vex | evoker fangs that hold you in place |
| Gravity | eye | the eye of ender pulls toward the End |
| Momentum | wayfinder | trail-ruins travellers on the road |
| Rampaging | snout | piglin brutes |
| Uppercut | raiser | "raise" a foe |
| Venom | wild | jungle temple poison and vines |
| Sandstorm | dune | desert sandstorms |
| Plunder | coast | shipwreck treasure |
| Withering | rib | the fortress's wither skeletons |
| Phase | spire | End city shulkers and endermen teleport |
| Shared Pain | host | trail ruins: a host shares the load |
| Guarding Strike | sentry | pillager outpost guards |
| Daze | shaper | trail ruins: shaping the fight |

## Shield powers

No refinement needed: trim template + shield + echo shard → the power, one level only. Only these work on a shield:

| Power | Trims | On a blocked hit | Cooldown |
|---|---|---|---|
| Wind Charged | flow | Gust from the shield throws back everything in front (4 blocks, 30% of a wind charge) | 30 s |
| Cold Edge | tide | The attacker, within 5 blocks, is chilled hard (−40% speed) | 10 s |

## Mobs

| Change | Value |
|---|---|
| Monster spawn cap | ×1.8 |
| Hit chain | First monster hit 0.75× damage, +0.25 per further hit (max 2×); resets after 2 s without a monster hit |
| Health | +0–30% per monster, middle values common, the ends rare |
| Damage | +0–15% per monster, rolled evenly |
| Armor | +1, +1 at full local difficulty, +0.15 per player power |
| Knockback resistance | +0.05, +0.05 at full local difficulty, +0.01 per player power |
| Player power | On the strongest nearby player's held weapons (48 blocks): trim power levels + 2 per power pair, max 30 |
| Mob farms | Scaled mobs still die from their original fall height |

### Ominous difficulty

Drinking an ominous bottle (Bad Omen I–V) raises the difficulty on purpose. Monsters that spawn within 48 blocks of a player with Bad Omen get extra stats per omen level, on top of the rolls above, and monsters that player kills can drop loot.

| Omen level | Health | Damage | Armor | Knockback res. | Emerald on kill | Echo shard on kill |
|---|---|---|---|---|---|---|
| I | +6% | +3% | +0.5 | +0.02 | 5% | — |
| II | +12% | +6% | +1 | +0.04 | 6% | — |
| III | +18% | +9% | +1.5 | +0.06 | 7% | — |
| IV | +24% | +12% | +2 | +0.08 | 8% | — |
| V | +30% | +15% | +2.5 | +0.10 | 9% | 0.2% |

Vanilla still turns Bad Omen into a raid in a village and a trial omen at a trial spawner.

### Place

Every bonus above (health, damage, armor, knockback resistance, omen stats and elite odds) is multiplied by where the monster spawns.

| Where | Multiplier | Tier |
|---|---|---|
| Overworld surface within 256 blocks of world spawn | ×0.5 | Regular |
| Overworld surface elsewhere | ×1 | Regular |
| Underground (below sea level, no sky) | ×1.25 | Hard |
| Deep Dark | ×1.5 | Very Hard |
| Nether: Nether Wastes, Crimson and Warped Forest | ×1.5 | Hard |
| Nether: Soul Sand Valley, Basalt Deltas | ×1.65 | Very Hard |
| End | ×1.75 | Extreme |

The **tier** is raised by Bad Omen on a nearby player: one step at I–II, two at III–IV, three at V, up to **Possible Death**. It picks the armor below, and the HUD shows it in the bottom right corner with the biome: "Nether Wastes - Hard".

### Armor

Zombies, husks, drowned, zombie villagers, skeletons, strays, bogged, wither skeletons, piglins, piglin brutes and zombified piglins (`bcenchantments:armored_monsters`) can spawn in armor. One material per outfit, the better ones rarer within the range; the helmet always comes, each other piece is its own roll. Slots already filled stay. Each piece drops 4% of the time.

| Tier | Wears armor | Materials | Other pieces | Enchanted | Enchanting level |
|---|---|---|---|---|---|
| Regular | 10% | copper, chainmail | 35% | — | — |
| Hard | 22% | copper to iron | 45% | 15% | 8 |
| Very Hard | 35% | chainmail to diamond | 55% | 35% | 15 |
| Extreme | 50% | iron to netherite | 65% | 60% | 22 |
| Possible Death | 65% | diamond, netherite | 80% | 85% | 30 |

### Elites and captains

A monster can spawn as an **elite** with one affix. Its hits apply a status rather than more damage, and it shows the affix with a faint particle every second. Its name ("Frost Zombie") shows when you look at it.

| Affix | On hit | Particle |
|---|---|---|
| Frost | Slowness II, 3 s | Snowflake |
| Weakening | Weakness I, 5 s | Smoke |
| Shredding | −2 armor per hit, stacks to −6, all back after 5 s without a hit | Crit |

A **captain** is an elite that wears an ominous banner ("Frost Zombie Captain"). Every second it gives monsters within 8 blocks Speed I and Resistance I for 2 s, so kill it first. Only zombies, skeletons and piglins can be captains; illagers keep vanilla's.

| | Value |
|---|---|
| Elite chance | (3% + 1% per omen level + 0.1% per player power) × place |
| Captain chance | 35% of the elite chance, zombies, skeletons and piglins only |
| Extra health | Elite +20%, captain +40% |
| Emerald when a player kills it | Elite 20%, captain 50% |
| Excluded | Wither, Ender Dragon |

## Settings

| Setting | Where | What it does |
|---|---|---|
| Better Combat On / Off | Mod Menu → settings | Off restores vanilla attacks and sweeping, for farms. Singleplayer / LAN host only |
| Charge bars | Mod Menu → settings | Under or beside the crosshair, offset, size |
