# Trim Powers & Combinations

Living design sheet for Trim Enchantments. Update it whenever a power or combination changes.

## Powers

Trim Powers (Powers for short) come only from trims: every armor trim grants its own power, and there are no enchanted books of them. Levels I–III. Charged powers show as bars under the crosshair. Any power goes on any weapon.

| Power | Type | What it does | Numbers (I / II / III) | Weapons | Based on |
|---|---|---|---|---|---|
| Lightning | Charged | Every 10 hits a bolt strikes the target | 7 / 13 / 17 dmg | sharp weapons | Dungeons *Thundering* |
| Shockwave | Charged | Wave flies out after 3 swings in a row that each hit 3+ enemies | 4 / 6 / 8 dmg, 8 / 10 / 12 blocks | sharp weapons | Dungeons *Shockwave* |
| Chains | Charged | +5% per hit, +2% per extra enemy; charge lasts 8 s, survives misses. When full, nearby enemies and the target are rooted 0.7 s | binds 2 / 3 / 4 within 6 blocks | sharp weapons | Dungeons *Chains* |
| Echo | Charged | Every 4th swing its targets are struck again 0.3 s later; counts toward other bars, triggers no perks | 50 / 65 / 80% of the swing | sharp weapons | Dungeons *Echo* |
| Cold Edge | On hit | Each hit adds a chill stack; enough stacks freeze the mob solid. The next hit on a frozen mob deals ×1.6 and ends the freeze, which also ends on its own. No attack-speed debuff. Swords chill everything the swing hits | 5 / 4 / 3 stacks, frozen 1 / 1.5 / 2 s | sharp weapons | Dungeons *Freezing* |
| Wind Charged | On kill | Kills burst like a wind charge (2.5 blocks), hurting and pushing | 3 / 4 / 5 dmg | sharp weapons, mace | Dungeons *Exploding* + vanilla wind charge |
| Gravity | On hit | Every landed swing pulls the enemies around the struck target toward it | radius 3 / 4 / 5 blocks, pull 0.35 / 0.45 / 0.55 | weapons | Dungeons *Gravity* |
| Marked | On hit | Hitting a target at point blank marks it; your next point-blank hit on a marked mob deals more damage and spends the mark | ×1.5 / ×2 / ×2.5 damage | weapons | Original |
| Rampaging | On kill | Every 2nd kill grants attack speed for 2 s; the kill count resets after each trigger | +20 / 25 / 30% attack speed | weapons | Dungeons *Rampaging* |
| Uppercut | On hit | Sword's 3rd swing, spear's heavy thrust or a mace swung from the ground launches the target | launch 0.55 / 0.7 / 0.85 | swords, spear, mace | Original |

### Draft powers (for review, not built)

Their trims are refused at the smithing table until they are built.

| Power | Trim | Type | What it does | Numbers (I / II / III) | Based on |
|---|---|---|---|---|---|
| Venomous | wild | On hit | Hits throw a splash potion of poison upwards from the target's location, once per 8 s per target. Enemies it lands on get Poison II; if it hits the player it purifies every poison effect on them | Poison II for 2 / 3 / 4 s | Dungeons *Jungle's Poison* / *Poison Cloud* |
| Curse | dune | On hit | 5 / 10 / 15% chance on a hit to put one random negative effect on the target (a "cursed" mob, for combinations) | 1.5 / 2 / 2.5 s | Original (desert pyramid) |
| Plunder | coast | On kill | Monster drops are increased; after 3–6 s (the same at every level) part of the drops turn into emeralds. 5% chance to drop double boss loot | 30 / 45 / 60% of the drops | Dungeons *Prospector* |
| Withering | rib | Charged | Every 12 hits release a burst of souls: 5 damage and Weakness to every enemy within 3 blocks | Weakness for 1 / 2 / 3 s | Dungeons *Weakening* + wither skeleton |
| Shulker Burst | spire | Charged | Every 8 hits release shulker bullets, each homing in on a random enemy and levitating it for 2 s on hit | 1 / 2 / 3 bullets | Shulker bullets |
| Shared Pain | host | On kill | A kill's overflowing damage goes to the nearest enemies within 3 blocks (every level). It reaches only those first recipients and never carries on from them | 1 / 2 / 3 enemies | Dungeons *Shared Pain* |
| Guardian Strike | vex | On kill | Every 3 kills within 10 s summon a vex that fights for you and dies after a few hits | dies after 2 / 3 / 4 hits | Dungeons *Guarding Strike* + evoker vexes |
| Copper Shock | shaper | On hit | Chance on a hit to release a copper-coloured electrical pulse that jumps between nearby enemies | 10 / 15 / 20% chance, jumps to 4 / 5 / 6 enemies | Dungeons *Stunning* (copper lightning) |

## Combinations

Every two powers have one combination: 18 powers, 153 combinations, 17 for each power. Powers combine in pairs **in the order they were put on**: the 1st with the 2nd, the 3rd with the 4th, and so on. A power already in a combination is locked out of every other, so a 3rd power stays alone until a 4th joins it. A weapon holds any number of powers. Active combinations show in gold in the tooltip and in the Field Guide. At the smithing table, hovering a refined weapon lists what its next trim would make (white: you carry the trim, grey: you don't). Holding Shift over a weapon shows a short description under its trim power and each combination.

| Combination | Needs | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies get half-power bolts and a 0.5 s root instead |
| Charged Lightning | Lightning + Wind Charged | Bolt bursts like a wind charge (plain Lightning has no knockback) |
| Updraft | Uppercut + Wind Charged | You and the target go straight up, you twice as high as a block above it; neither hangs, both fall normally. **Right click in the air** (once per airtime, either hand; left click stays normal, but while the move runs, until a slam or dive lands or for 0.5 s of a lunge, no attack lands and the other hand is disabled): **mace** slams down fast and smashes the ground within 0.75 blocks of your body's edge for 1.5× a mace smash from that height; **sword** dives straight down, 25% slower than the slam (start speed and gravity), and hits everything within 3 blocks for its attack damage +10% per block fallen (max +100%); **spear**, only after an Updraft launch and only without the Lunge enchantment, lunges along the look (speed 1.2, Lunge I's hunger), hitting every enemy it passes through once for its attack damage (enchantment bonuses and on-hit effects apply) and pushing them along. Lunge itself works in the air only after an Updraft launch. Slam and dive landings take no fall damage; each move costs 1 durability |
| Frozen Chains | Chains + Cold Edge | Bound enemies gain a chill stack |
| Frost Burst | Cold Edge + Wind Charged | Death bursts add a chill stack to what they catch |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Tempest | Shockwave + Wind Charged | Shockwave throws enemies much harder; hitting a wall deals 4 / 6 / 8 knockback damage. **Mace:** a smash also hurts enemies within 2.5 blocks of the impact for 30% of its damage |
| Frenzy | Echo + Refinement 5 | Echo every 3rd swing (not a pair: any weapon with Echo, since trims need refinement 5) |
| Singularity | Gravity + Shockwave | The wave drags enemies within 4 blocks of the wave's own centre into its path (120% of Gravity's pull) |
| Black Hole | Gravity + Chains | Every bound enemy pulls the free enemies within 3.5 blocks in |
| Cyclone | Gravity + Wind Charged | Kills pull enemies within 5 blocks in; the burst goes off 0.3 s later |
| Launch | Marked + Uppercut | A launching swing on a marked mob throws the targets ×1.3 higher, and you go up after them |
| Blast Step | Marked + Wind Charged | A kill by a point-blank hit on a marked mob bursts you onward (once per swing) |
| Shatter | Cold Edge + Lightning | Bolts deal ×2 to frozen enemies. Lightning kills spread chill to up to 6 enemies within 2 blocks |

Chained Lightning + Wind Charged: enemies killed by the chain bolts burst.

### Draft combinations (for review, not built)

The other 140 pairs, grouped by the first power. Once approved they get built; until then a pair without a built combination does nothing but still locks both powers.

**Lightning**

| Combination | Needs | Effect |
|---|---|---|
| Thunderwave | Lightning + Shockwave | Every enemy the shockwave cuts is struck by a quarter-power bolt |
| Lightning Rod | Lightning + Gravity | Before the bolt lands, enemies within 4 blocks are pulled to the target |
| Thunder Charge | Lightning + Marked | A hit on a marked mob adds 3 hits of Lightning charge |
| Storm Frenzy | Lightning + Rampaging | While rampaging, Lightning charges twice as fast |
| Skybreaker | Lightning + Uppercut | Launched targets are struck by a half-power bolt at the top of their flight |
| Toxic Storm | Lightning + Venomous | Bolts leave a poison splash where they strike |
| Hexbolt | Lightning + Curse | Every Curse that lands also calls a quarter-power bolt on its target |
| Gilded Storm | Lightning + Plunder | Bolt kills convert twice the usual share of their drops into emeralds |
| Black Lightning | Lightning + Withering | Bolts release a soul burst where they strike |
| Shulker Storm | Lightning + Shulker Burst | Each Shulker Burst bullet that hits calls a quarter-power bolt on its target |
| Arc | Lightning + Shared Pain | A bolt kill's overkill reaches Shared Pain's enemies as bolts |
| Static Guard | Lightning + Guardian Strike | Your vexes strike what they hit with a quarter-power bolt |
| Copper Storm | Lightning + Copper Shock | Every enemy a Copper Shock pulse hits is struck by a quarter-power bolt |

**Wind Charged**

| Combination | Needs | Effect |
|---|---|---|
| Gale Echo | Wind Charged + Echo | Echo strikes push their targets back like a small gust |
| Whiplash | Wind Charged + Chains | When Chains fires, bound enemies are yanked to the target |
| Hurricane | Wind Charged + Rampaging | While rampaging, death bursts are 50% wider |
| Toxic Gust | Wind Charged + Venomous | Death bursts poison what they catch |
| Hexwind | Wind Charged + Curse | Death bursts curse what they catch |
| Windfall | Wind Charged + Plunder | Death bursts blow dropped loot and experience to you |
| Ashen Wind | Wind Charged + Withering | Death bursts release a soul burst |
| Shulker Gust | Wind Charged + Shulker Burst | Bullets that hit throw their target back like a small gust |
| Shrapnel | Wind Charged + Shared Pain | A kill's overkill is added to its death burst's damage |
| Bulwark Gust | Wind Charged + Guardian Strike | Kill bursts throw enemies away from you, not from the victim |
| Spark Burst | Wind Charged + Copper Shock | Death bursts release a Copper Shock pulse from the victim |

**Cold Edge**

| Combination | Needs | Effect |
|---|---|---|
| Hoarfrost Echo | Cold Edge + Echo | Echo strikes add a chill stack |
| Glacier Wave | Cold Edge + Shockwave | The shockwave chills what it cuts and frosts the ground it passes (a chill stack to enemies crossing it, 2 s) |
| Frost Vortex | Cold Edge + Gravity | Pulled enemies gain a chill stack |
| Frostbite Charge | Cold Edge + Marked | A hit on a marked mob freezes it solid at once |
| Cold Blood | Cold Edge + Rampaging | While rampaging, each hit adds two chill stacks |
| Frozen Ascent | Cold Edge + Uppercut | Launched targets freeze solid in the air |
| Frostbite | Cold Edge + Venomous | Chilled enemies take poison damage twice as fast |
| Cursed Frost | Cold Edge + Curse | Cursed enemies gain a chill stack |
| Ice Treasure | Cold Edge + Plunder | Kills on frozen enemies convert twice the usual share of their drops |
| Necrotic Frost | Cold Edge + Withering | Freezes last twice as long on weakened enemies |
| Cold Snap | Cold Edge + Shulker Burst | Bullets that hit freeze their target solid |
| Shattering Pain | Cold Edge + Shared Pain | Overkill from a frozen kill reaches one more enemy |
| Permafrost | Cold Edge + Guardian Strike | Enemies hit by a vex gain a chill stack |
| Deep Freeze | Cold Edge + Copper Shock | A Copper Shock pulse freezes chilled enemies solid at once |

**Echo**

| Combination | Needs | Effect |
|---|---|---|
| Aftershock | Echo + Shockwave | A second, half-damage wave follows every shockwave 0.5 s later |
| Reverberation | Echo + Chains | Echo strikes add Chains charge as full hits |
| Pull Echo | Echo + Gravity | Echo strikes pull nearby enemies to their target |
| Double Step | Echo + Marked | A hit on a marked mob always echoes |
| Bloodlust | Echo + Rampaging | While rampaging, Echo strikes every 3rd swing |
| Rebound | Echo + Uppercut | Launches echo: the target is launched again 0.3 s later, a little higher |
| Toxic Echo | Echo + Venomous | Echo strikes poison |
| Echoing Curse | Echo + Curse | Echo strikes can curse too, at the same chance |
| Echoing Coin | Echo + Plunder | Echo kills convert twice the usual share of their drops |
| Soul Echo | Echo + Withering | Echo strikes count as hits toward the soul burst |
| Ghost Volley | Echo + Shulker Burst | Echo strikes count as hits toward Shulker Burst |
| Resounding Pain | Echo + Shared Pain | Echo kills spread their overkill too |
| Ward Echo | Echo + Guardian Strike | Echo kills summon a vex too |
| Static Echo | Echo + Copper Shock | Echo strikes can release a Copper Shock pulse too |

**Shockwave**

| Combination | Needs | Effect |
|---|---|---|
| Snare Wave | Shockwave + Chains | The shockwave roots what it cuts for 0.5 s |
| Surge | Shockwave + Marked | A hit on a marked mob fires the shockwave with 2 of its 3 swings charged |
| Warpath | Shockwave + Rampaging | While rampaging, Shockwave needs 2 swings instead of 3 |
| Upheaval | Shockwave + Uppercut | The shockwave launches what it cuts |
| Plague Wave | Shockwave + Venomous | The shockwave leaves a poison splash trail |
| Curse Wave | Shockwave + Curse | The shockwave can curse what it cuts, at the same chance |
| Gold Rush | Shockwave + Plunder | Shockwave kills convert twice the usual share of their drops |
| Death Wave | Shockwave + Withering | The shockwave weakens what it cuts |
| Shulker Wave | Shockwave + Shulker Burst | Every shockwave also releases a Shulker Burst |
| Crushing Wave | Shockwave + Shared Pain | Each kill adds its overkill to the wave's damage |
| Shield Wall | Shockwave + Guardian Strike | Firing a shockwave summons a vex |
| Arc Wave | Shockwave + Copper Shock | The shockwave releases a Copper Shock pulse from the first enemy it cuts |

**Chains**

| Combination | Needs | Effect |
|---|---|---|
| Grapple | Chains + Marked | A hit on a marked mob fires Chains if it is half charged |
| Hunter's Chains | Chains + Rampaging | Kills add 20% Chains charge |
| Chain Lift | Chains + Uppercut | Launching a bound enemy launches every enemy bound with it |
| Venom Links | Chains + Venomous | Bound enemies are poisoned |
| Cursed Bind | Chains + Curse | Bound enemies are cursed |
| Chain Gang | Chains + Plunder | Enemies killed while rooted convert twice the usual share of their drops |
| Death Grip | Chains + Withering | Bound enemies are weakened |
| Tether | Chains + Shulker Burst | Shulker Burst bullets home in on bound enemies first |
| Shared Bonds | Chains + Shared Pain | Damage to the target is shared 30% with every enemy bound to it |
| Iron Ward | Chains + Guardian Strike | Firing Chains summons a vex |
| Shackles | Chains + Copper Shock | Copper Shock pulses root the enemies they hit for 0.5 s |

**Gravity**

| Combination | Needs | Effect |
|---|---|---|
| Slingshot | Gravity + Marked | A hit on a marked mob pulls twice as hard |
| Feeding Frenzy | Gravity + Rampaging | While rampaging, Gravity reaches 2 blocks further |
| Implosion | Gravity + Uppercut | Launched targets pull nearby enemies under them as they rise |
| Miasma | Gravity + Venomous | Pulled enemies are poisoned |
| Hex Well | Gravity + Curse | Pulled enemies are cursed |
| Magnet | Gravity + Plunder | Gravity also pulls dropped items and experience to you |
| Event Horizon | Gravity + Withering | Pulled enemies are weakened |
| Lure | Gravity + Shulker Burst | Bullets that hit pull nearby enemies to their target |
| Collapse | Gravity + Shared Pain | Overkill goes to the pulled enemies first |
| Gravity Well | Gravity + Guardian Strike | Vexes pull in the enemies around them |
| Crush | Gravity + Copper Shock | Pulled enemies that reach the target are struck by a Copper Shock pulse |

**Marked**

| Combination | Needs | Effect |
|---|---|---|
| Stampede | Marked + Rampaging | While rampaging, a hit on a marked mob does not spend the mark |
| Toxic Charge | Marked + Venomous | A hit on a marked mob throws a Venomous splash at once, ignoring its 8 s limit |
| Cursed Trail | Marked + Curse | A hit on a marked mob always curses it |
| Smash and Grab | Marked + Plunder | Kills on a marked mob convert twice the usual share of their drops |
| Death Charge | Marked + Withering | A hit on a marked mob releases a soul burst |
| Blitz | Marked + Shulker Burst | A hit on a marked mob triggers Shulker Burst at once |
| Bowling | Marked + Shared Pain | Kills on a marked mob throw their overkill at the enemy behind them |
| Vanguard | Marked + Guardian Strike | A hit on a marked mob summons a vex |
| Bull Rush | Marked + Copper Shock | A hit on a marked mob always releases a Copper Shock pulse |

**Rampaging**

| Combination | Needs | Effect |
|---|---|---|
| Juggernaut | Rampaging + Uppercut | While rampaging, every 3rd swing launches |
| Blood Toxin | Rampaging + Venomous | While rampaging, poison lasts twice as long |
| Cursed Rage | Rampaging + Curse | Kills while rampaging curse the enemies around the victim |
| Pillage | Rampaging + Plunder | While rampaging, Plunder converts twice the usual share of drops |
| Reaper | Rampaging + Withering | Kills on weakened enemies extend the rampage by 2 s |
| Shadow Rush | Rampaging + Shulker Burst | While rampaging, Shulker Burst charges twice as fast |
| Massacre | Rampaging + Shared Pain | Kills by overkill count as two kills for Rampaging |
| Berserker | Rampaging + Guardian Strike | Vexes summoned while rampaging last one hit longer |
| Terror | Rampaging + Copper Shock | Kills while rampaging release a Copper Shock pulse from the victim |

**Uppercut**

| Combination | Needs | Effect |
|---|---|---|
| Spore Launch | Uppercut + Venomous | Launched targets throw a Venomous splash where they land |
| Hex Geyser | Uppercut + Curse | Launches curse the enemies around the target |
| Shakedown | Uppercut + Plunder | A launched enemy has a 10% chance to drop an emerald, once each |
| Gallows | Uppercut + Withering | Launched targets are weakened |
| Skyshot | Uppercut + Shulker Burst | After a launch, a Shulker Burst bullet homes in on the launched target |
| Skyfall | Uppercut + Shared Pain | A launched target that dies spreads its overkill around where it lands |
| Rising Guard | Uppercut + Guardian Strike | Launching summons a vex |
| Ragdoll | Uppercut + Copper Shock | Launching releases a Copper Shock pulse from the target |

**Venomous**

| Combination | Needs | Effect |
|---|---|---|
| Toxic Curse | Venomous + Curse | Cursed enemies are poisoned too |
| Antidote Trade | Venomous + Plunder | Kills on poisoned enemies convert twice the usual share of their drops |
| Blight | Venomous + Withering | Poison deals 50% more to weakened enemies |
| Venom Bullets | Venomous + Shulker Burst | Shulker Burst bullets poison on hit (Poison II) |
| Contagion | Venomous + Shared Pain | A poisoned enemy's death passes its poison to the nearest enemy |
| Venom Fang | Venomous + Guardian Strike | Vexes poison what they hit |
| Toxic Arc | Venomous + Copper Shock | Copper Shock pulses poison the enemies they hit |

**Curse**

| Combination | Needs | Effect |
|---|---|---|
| Cursed Hoard | Curse + Plunder | Kills on cursed enemies convert twice the usual share of their drops |
| Rotting Curse | Curse + Withering | Cursed enemies are weakened |
| Hex Bullets | Curse + Shulker Burst | Bullets that hit apply a Curse at once |
| Curse Spread | Curse + Shared Pain | Overkill goes to cursed enemies first |
| Hex Ward | Curse + Guardian Strike | Vexes curse what they hit |
| Curse Shock | Curse + Copper Shock | Copper Shock pulses carry a Curse |

**Plunder**

| Combination | Needs | Effect |
|---|---|---|
| Grave Robber | Plunder + Withering | Kills on weakened enemies drop double experience |
| Pickpocket | Plunder + Shulker Burst | Kills by a bullet convert twice the usual share of their drops |
| Jackpot | Plunder + Shared Pain | Kills by overkill convert twice the usual share of their drops |
| Treasure Guard | Plunder + Guardian Strike | Kills by a vex convert twice the usual share of their drops |
| Mugging | Plunder + Copper Shock | Kills by a Copper Shock pulse convert twice the usual share of their drops |

**Withering**

| Combination | Needs | Effect |
|---|---|---|
| Soul Bullets | Withering + Shulker Burst | Bullets weaken their target |
| Decay | Withering + Shared Pain | A weakened enemy's death passes its weakness to the nearest enemy |
| Wraith Vex | Withering + Guardian Strike | Vexes weaken what they hit |
| Soul Arc | Withering + Copper Shock | Copper Shock pulses weaken the enemies they hit |

**Shulker Burst**

| Combination | Needs | Effect |
|---|---|---|
| Rift | Shulker Burst + Shared Pain | Bullet kills carry their overkill to Shared Pain's enemies too |
| Shulker Guard | Shulker Burst + Guardian Strike | Releasing a Shulker Burst summons a vex |
| Shock Bullets | Shulker Burst + Copper Shock | Bullets that hit release a Copper Shock pulse |

**Shared Pain**

| Combination | Needs | Effect |
|---|---|---|
| Martyr | Shared Pain + Guardian Strike | Kills by overkill give your vexes one more hit |
| Shockfall | Shared Pain + Copper Shock | Enemies hit by overkill are struck by a Copper Shock pulse |

**Guardian Strike**

| Combination | Needs | Effect |
|---|---|---|
| Stalwart | Guardian Strike + Copper Shock | Vexes release a Copper Shock pulse when they die |

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
| Chains | sentry | pillager outpost guards: captured and held |
| Gravity | eye | the eye of ender pulls toward the End |
| Marked | wayfinder | trail ruins: marking the way |
| Rampaging | snout | piglin brutes |
| Uppercut | raiser | "raise" a foe |
| Venomous | wild | jungle temple poison and vines |
| Curse | dune | desert pyramid curses |
| Plunder | coast | shipwreck treasure |
| Withering | rib | the fortress's wither skeletons and their souls |
| Shulker Burst | spire | End city shulkers fire homing bullets |
| Shared Pain | host | trail ruins: a host shares the load |
| Guardian Strike | vex | the evoker's vexes, summoned to fight for you |
| Copper Shock | shaper | trail ruins: copper-age shaping |

## Shield powers

No refinement needed: trim template + shield + echo shard → the power, one level only. Only these work on a shield:

| Power | Trims | On a blocked hit | Cooldown |
|---|---|---|---|
| Wind Charged | flow | Gust from the shield throws back everything in front (4 blocks, 30% of a wind charge) | 30 s |
| Cold Edge | tide | The attacker is frozen solid for 1 s | 10 s |

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
