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
| Withering | Charged | Every 12 enemies hit (the count lasts 8 s and survives misses) a soul burst at the target deals 5 magic damage and weakens enemies within 3 blocks | Weakness 1 / 2 / 3 s | weapons | Dungeons *Weakening* + wither skeleton |
| Shulker Burst | Charged | Every 8 enemies hit (the count lasts 8 s and survives misses) shulker bullets fly from you at random enemies within 12 blocks; a hit deals the bullet's 4 damage and levitates 2 s | 1 / 2 / 3 bullets | weapons | End city shulkers |
| Cold Edge | On hit | Hits slow; 3 s of chill freezes the target solid (freeze damage). Swords chill everything the swing hits | −10 / 25 / 40% speed | sharp weapons | Dungeons *Freezing* |
| Gravity | On hit | Every landed swing pulls the enemies around the struck target toward it | radius 3 / 4 / 5 blocks, pull 0.175 / 0.225 / 0.275 (synergies pull with twice that) | weapons | Dungeons *Gravity* |
| Marked | Point blank | A hit within 2.5 blocks marks its target for 5 s; the next point-blank hit on it spends the mark and deals extra damage as a second hit | ×1.5 / 2 / 2.5 the swing | weapons | Original |
| Uppercut | On hit | Sword's 3rd swing, spear's heavy thrust or a mace swung from the ground launches the target | launch 0.55 / 0.7 / 0.85 | swords, spear, mace | Original |
| Venomous | On hit | A hit throws a poison splash up off the target, at most once per 8 s. Where it lands, enemies within 2.5 blocks get Poison II; if it lands on you it clears your harmful effects | Poison II 2 / 3 / 4 s | weapons | Dungeons *Jungle's Poison* |
| Curse | On hit | Each enemy hit may be cursed: one random harmful effect (Slowness II, Weakness, Wither, Poison or Blindness) | 5 / 10 / 15%, 1.5 / 2 / 2.5 s | weapons | Original (desert pyramid) |
| Copper Shock | On hit | A swing may release a copper pulse from its target that jumps from enemy to enemy (4 blocks a jump, never the same one twice), 3 magic damage each | 10 / 15 / 20%, 4 / 5 / 6 enemies | weapons | Dungeons *Thundering*, copper |
| Wind Charged | On kill | Kills burst like a wind charge (2.5 blocks), hurting and pushing | 3 / 4 / 5 dmg | sharp weapons, mace | Dungeons *Exploding* + vanilla wind charge |
| Rampaging | On kill | Killing a monster grants attack speed for 4 s, refreshed by every further kill | +20 / 30 / 40% attack speed | weapons | Dungeons *Rampaging* |
| Plunder | On kill | Monster drops grow. After 7 s on the ground each item still there has a 10% chance to turn into an emerald. Bosses (Wither, Ender Dragon, Elder Guardian, Warden) drop everything twice 5% of the time | +30 / 45 / 60% drops | weapons | Dungeons *Prospector* |
| Shared Pain | On kill | A monster kill's overkill (the damage beyond its last health) goes to the nearest monsters within 3 blocks as magic damage, each taking all of it. A kill by overkill passes nothing on | 1 / 2 / 3 monsters | weapons | Dungeons *Shared Pain* |
| Guardian Strike | On kill | 3 monster kills within 10 s summon a vex that hunts monsters within 12 blocks of you. It ignores players, nothing you do hurts it, and it vanishes after its hits or 30 s | vanishes after 2 / 3 / 4 hits | weapons | Dungeons *Guarding Strike* + evoker vexes |

## Combinations

Every two powers have one combination: 18 powers, 153 combinations, 17 for each power. Powers combine in pairs **in the order they were put on**: the 1st with the 2nd, the 3rd with the 4th, and so on. A power already in a combination is locked out of every other, so a 3rd power stays alone until a 4th joins it. A weapon holds any number of powers. Active combinations show in gold in the tooltip and in the Field Guide. At the smithing table, hovering a refined weapon lists what its next trim would make (white: you carry the trim, grey: you don't). Holding Shift over a weapon shows a short description under its trim power and each combination.

Combinations are built in batches, one power's 17 at a time. Batch 1 is Lightning, batch 2 Wind Charged.

| Combination | Needs | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies get half-power bolts and a 0.5 s root instead |
| Charged Lightning | Lightning + Wind Charged | Bolt bursts like a wind charge (plain Lightning has no knockback) |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Shatter | Cold Edge + Lightning | Bolts deal +15% to slowed enemies, ×2 to frozen ones (which also chills nearby enemies). Kills spread chill to up to 6 enemies within 2 blocks |
| Thunderwave | Lightning + Shockwave | Every enemy the shockwave cuts is struck by a quarter-power bolt |
| Lightning Rod | Lightning + Gravity | Before the bolt lands, enemies within 5 blocks are pulled to the target at 150% of Gravity's full pull (the bolt waits 0.3 s for them). If the target dies meanwhile, the bolt strikes the nearest enemy within 5 blocks of where it stood |
| Thunder Charge | Lightning + Marked | Spending a mark adds 3 hits of Lightning charge |
| Storm Frenzy | Lightning + Rampaging | While rampaging, Lightning charges twice as fast |
| Skybreaker | Lightning + Uppercut | Launched targets are struck by a half-power bolt at the top of their flight |
| Toxic Storm | Lightning + Venomous | Every bolt of the weapon leaves a Venomous splash where it strikes (no cooldown) |
| Hexbolt | Lightning + Curse | Every curse that lands also calls a quarter-power bolt on its target |
| Gilded Storm | Lightning + Plunder | A kill by the weapon's lightning drops 3× the loot instead of Plunder's bonus |
| Black Lightning | Lightning + Withering | Every bolt of the weapon releases a Withering soul burst where it strikes |
| Shulker Storm | Lightning + Shulker Burst | Each bullet that hits calls a quarter-power bolt on its target |
| Arc | Lightning + Shared Pain | A lightning kill's overkill reaches Shared Pain's monsters as lightning bolts |
| Static Guard | Lightning + Guardian Strike | Each vex hit strikes its target with a quarter-power bolt |
| Copper Storm | Lightning + Copper Shock | Each enemy a pulse reaches has a 50% chance to be struck by a quarter-power bolt |
| Updraft | Uppercut + Wind Charged | You and the target go straight up, you twice as high as a block above it; neither hangs, both fall normally. **Right click in the air** (once per airtime, either hand; left click stays normal, but while the move runs, until a slam or dive lands or for 0.5 s of a lunge, no attack lands and the other hand is disabled): **mace** slams down fast and smashes the ground within 0.75 blocks of your body's edge for 1.5× a mace smash from that height; **sword** dives straight down, 25% slower than the slam (start speed and gravity), and hits everything within 3 blocks for its attack damage +10% per block fallen (max +100%); **spear**, only after an Updraft launch and only without the Lunge enchantment, lunges along the look (speed 1.2, Lunge I's hunger), hitting every enemy it passes through once for its attack damage (enchantment bonuses and on-hit effects apply) and pushing them along. Lunge itself works in the air only after an Updraft launch. Slam and dive landings take no fall damage; each move costs 1 durability |
| Frozen Chains | Chains + Cold Edge | Bound enemies are chilled |
| Frost Burst | Cold Edge + Wind Charged | Death bursts chill |
| Tempest | Shockwave + Wind Charged | Shockwave throws enemies much harder; hitting a wall deals 4 / 6 / 8 knockback damage. **Mace:** a smash also hurts enemies within 2.5 blocks of the impact for 30% of its damage |
| Frenzy | Echo + Refinement 5 | Echo every 3rd swing (not a pair: any weapon with Echo, since trims need refinement 5) |
| Singularity | Gravity + Shockwave | The wave drags enemies within 4 blocks of the wave's own centre into its path (120% of Gravity's pull) |
| Black Hole | Gravity + Chains | Every bound enemy pulls the free enemies within 3.5 blocks in |
| Cyclone | Gravity + Wind Charged | Kills pull enemies within 5 blocks in; the burst goes off 0.3 s later |
| Launch | Marked + Uppercut | A launching swing on a marked mob throws the targets ×1.3 higher, and you go up after them |
| Blast Step | Marked + Wind Charged | A kill by a point-blank hit on a marked mob bursts you onward (once per swing) |
| Gale Echo | Wind Charged + Echo | Each Echo strike that lands pushes its target away from you like a small gust (60% of a death burst's push) |
| Whiplash | Wind Charged + Chains | When Chains fires, the bound enemies are yanked to the target (landing about 1.5 blocks from it), then rooted |
| Hurricane | Wind Charged + Rampaging | While rampaging, death bursts are 50% wider (3.75 blocks); the kill that starts the rampage counts |
| Toxic Gust | Wind Charged + Venomous | Death bursts give what they catch Poison II for 1.5 s |
| Hexwind | Wind Charged + Curse | Each enemy a death burst catches has a 40% chance to be cursed (Curse's own level and time) |
| Windfall | Wind Charged + Plunder | Each drop of a Plunder kill has a 25% chance to charge up for 1 s (crackling gusts) and then burst like a wind charge, 2.5 blocks, 2 damage. Items are not pushed; a drop picked up first never bursts |
| Ashen Wind | Wind Charged + Withering | Death bursts also release a Withering soul burst |
| Shulker Gust | Wind Charged + Shulker Burst | Each bullet that hits throws its target away from you like a small gust (80% of a death burst's push) |
| Shrapnel | Wind Charged + Shared Pain | Every monster the overkill reaches bursts like a wind charge, 2 blocks, 4 damage. Kills by that burst pass no overkill on |
| Bulwark Gust | Wind Charged + Guardian Strike | Death bursts throw enemies away from you instead of away from the burst |
| Spark Burst | Wind Charged + Copper Shock | Every enemy a death burst pushes zaps the nearest other enemy within 4 blocks for a Copper Shock jump's 3 damage |

Chained Lightning + Wind Charged: enemies killed by the chain bolts burst.

Every bolt still lands when its target died before it arrived: it hits the nearest enemy within 1.5 blocks of where the target stood (Lightning Rod looks within 5), or else strikes the empty spot, where Toxic Storm's splash and Black Lightning's soul burst still go off.

### Planned combinations

The other 116 pairs, grouped by power, with the review notes from the design review. A pair without a built combination does nothing but still locks both powers.

**Cold Edge**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Hoarfrost Echo | Cold Edge + Echo | Echo strikes add a chill stack | Change: 2* stacks |
| Glacier Wave | Cold Edge + Shockwave | The shockwave chills what it cuts and frosts the ground it passes (a chill stack to enemies crossing it, 2 s) | Change: + Frozen enemies are shattered on Shockwave contact |
| Frost Vortex | Cold Edge + Gravity | Pulled enemies gain a chill stack | Change: gain 0.5 stacks* |
| Frostbite Charge | Cold Edge + Marked | A hit on a marked mob freezes it solid at once | Approved |
| Cold Blood | Cold Edge + Rampaging | While rampaging, each hit adds two chill stacks | Approved |
| Frozen Ascent | Cold Edge + Uppercut | Launched targets freeze solid in the air | Approved |
| Frostbite | Cold Edge + Venomous | Chilled enemies take poison damage twice as fast | Approved |
| Cursed Frost | Cold Edge + Curse | Cursed enemies gain a chill stack | Change: Cursing has a 50% chance to freeze |
| Ice Treasure | Cold Edge + Plunder | Kills on frozen enemies convert twice the usual share of their drops | Change: Items which turned into emeralds apply 0.5 chill every second in a 1 block radius (not stacking) |
| Necrotic Frost | Cold Edge + Withering | Freezes last twice as long on weakened enemies | Approved |
| Cold Snap | Cold Edge + Shulker Burst | Bullets that hit freeze their target solid | Approved |
| Shattering Pain | Cold Edge + Shared Pain | Overkill from a frozen kill reaches one more enemy | Change: Overkilling Frozen enemies applies also freeze to damaged enemies |
| Permafrost | Cold Edge + Guardian Strike | Enemies hit by a vex gain a chill stack | Change: 0.5 chill stack* |
| Deep Freeze | Cold Edge + Copper Shock | A Copper Shock pulse freezes chilled enemies solid at once | Change: 1 chill stack on shock |

**Echo**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Aftershock | Echo + Shockwave | A second, half-damage wave follows every shockwave | Approved |
| Reverberation | Echo + Chains | Echo strikes add Chains charge as full hits | Change: Enemies hit by chains give these hits as charge |
| Pull Echo | Echo + Gravity | Echo strikes pull nearby enemies to their target | Change: Echo strikes pull is 2x stronger |
| Double Step | Echo + Marked | A hit on a marked mob always echoes | Approved |
| Bloodlust | Echo + Rampaging | While rampaging, Echo strikes every 3rd swing | Approved |
| Rebound | Echo + Uppercut | Launches echo: the target is launched again, a little higher | Change: + takes double fall damage in that case |
| Toxic Echo | Echo + Venomous | Echo strikes poison | Change: Echo strikes on poisoned mobs applies poison IV isntead of II |
| Echoing Curse | Echo + Curse | Echo strikes can curse too, at the same chance | Change: Apply a secondary debuff |
| Echoing Coin | Echo + Plunder | Echo kills convert twice the usual share of their drops | Approved |
| Soul Echo | Echo + Withering | Echo strikes count as hits toward the soul burst | Change: Double Soul Burst |
| Ghost Volley | Echo + Shulker Burst | Echo strikes count as hits toward Shulker Burst | Change: + double shulker bursts |
| Resounding Pain | Echo + Shared Pain | Echo kills spread their overkill too | Approved |
| Ward Echo | Echo + Guardian Strike | Echo kills summon a vex too | Change: Vex Kills summon a new vex |
| Static Echo | Echo + Copper Shock | Echo strikes can release a Copper Shock pulse too | Change: + with 70% efficiency |

**Shockwave**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Snare Wave | Shockwave + Chains | The shockwave briefly roots what it cuts | Approved |
| Surge | Shockwave + Marked | A hit on a marked mob fires the shockwave with 2 of its 3 swings charged | Change: hit on marked target fires a shockwave with 40% its distance and strength |
| Warpath | Shockwave + Rampaging | While rampaging, Shockwave needs 2 swings instead of 3 | Approved |
| Upheaval | Shockwave + Uppercut | The shockwave launches what it cuts | Approved |
| Plague Wave | Shockwave + Venomous | The shockwave leaves a poison splash trail | Approved |
| Curse Wave | Shockwave + Curse | The shockwave can curse what it cuts, at the same chance | Approved |
| Gold Rush | Shockwave + Plunder | Shockwave kills convert twice the usual share of their drops | Change: Damageing with shockwave has a 35% chance to drop loot |
| Death Wave | Shockwave + Withering | The shockwave weakens what it cuts | Approved |
| Shulker Wave | Shockwave + Shulker Burst | Every shockwave also releases a Shulker Burst | Change: Shockwave hits release a shulker bullet with 20% chance |
| Crushing Wave | Shockwave + Shared Pain | Each kill adds its overkill to the wave's damage | Approved |
| Shield Wall | Shockwave + Guardian Strike | Firing a shockwave summons a vex | Change: summons vex for every 2 emenies hit* |
| Arc Wave | Shockwave + Copper Shock | The shockwave releases a Copper Shock pulse from the first enemy it cuts | Change: 40% chance on shcokwave hits to release copper shock |

**Chains**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Grapple | Chains + Marked | A hit on a marked mob fires Chains if it is half charged | Change: Chained enemies become marked and consume their mark, being damaged by 2 for each enemy hit |
| Hunter's Chains | Chains + Rampaging | Kills add 20% Chains charge | Approved |
| Chain Lift | Chains + Uppercut | Launching a bound enemy launches every enemy bound with it | Approved |
| Venom Links | Chains + Venomous | Bound enemies are poisoned | Approved |
| Cursed Bind | Chains + Curse | Bound enemies are cursed | Change: have a chance to be cursed by 50% |
| Chain Gang | Chains + Plunder | Enemies killed while rooted convert twice the usual share of their drops | Change: Items from a mob (rotten flesh for zombies, bone for skeletton etc.) will chain an enemy once |
| Death Grip | Chains + Withering | Bound enemies are weakened | Change: Weakened enemies have their duration of root increased by 2x |
| Tether | Chains + Shulker Burst | Shulker Burst bullets home in on bound enemies first | Change: + reapplies bound |
| Shared Bonds | Chains + Shared Pain | 30% of the damage to the target is shared with every enemy bound to it | Approved |
| Iron Ward | Chains + Guardian Strike | Firing Chains summons a vex | Change: Vex hitting enemies mark them for the vex for 3 seconds, if a vex hits another target they will root together. Root max 2 mobs per pair |
| Shackles | Chains + Copper Shock | Copper Shock pulses root the enemies they hit for 0.5 s | Approved |

**Gravity**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Slingshot | Gravity + Marked | A hit on a marked mob pulls twice as hard | Approved |
| Feeding Frenzy | Gravity + Rampaging | While rampaging, Gravity reaches 2 blocks further | Approved |
| Implosion | Gravity + Uppercut | Launched targets pull nearby enemies under them as they rise | Approved |
| Miasma | Gravity + Venomous | Pulled enemies are poisoned | Change: already poisoned enemies when pulled will get their poison duration reset |
| Hex Well | Gravity + Curse | Pulled enemies are cursed | Change: 10% chance to curse all pulled enemies |
| Magnet | Gravity + Plunder | Gravity also pulls items and experience to you | Approved |
| Event Horizon | Gravity + Withering | Pulled enemies are weakened | Change: Weakened enemies have a 5% chance to die on pull |
| Lure | Gravity + Shulker Burst | Bullets that hit pull nearby enemies to their target | Approved |
| Collapse | Gravity + Shared Pain | Overkill goes to the pulled enemies first | Change: On overkill damage on enemies, all of them will pull |
| Gravity Well | Gravity + Guardian Strike | Vexes pull in the enemies around them | Approved |
| Crush | Gravity + Copper Shock | Pulled enemies that reach the target are struck by a Copper Shock pulse | Change: A shock pulse will pull all enemeis hit |

**Marked**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Stampede | Marked + Rampaging | While rampaging, a hit on a marked mob does not spend the mark | Change: While rampaging, a consumed mark spreads on 3 enemies for 3 seconds |
| Toxic Charge | Marked + Venomous | A hit on a marked mob throws a Venomous splash at once, ignoring its 8 s limit | Approved |
| Cursed Trail | Marked + Curse | A hit on a marked mob always curses it | Approved |
| Smash and Grab | Marked + Plunder | Kills on a marked mob convert twice the usual share of their drops | Change: Has a 10% chance to start a heavy drop of loot (3x more items on topon marked kill |
| Death Charge | Marked + Withering | A hit on a marked mob releases a soul burst | Approved |
| Blitz | Marked + Shulker Burst | A hit on a marked mob triggers Shulker Burst at once | Approved |
| Bowling | Marked + Shared Pain | Kills on a marked mob throw their overkill at the enemy behind them | Change: Overkill damage is increased by 1.5x when killing marked target |
| Vanguard | Marked + Guardian Strike | A hit on a marked mob summons a vex | Change: Vex hits mark mobs for 3s. Vexes can also trigger marked hit and consuming it for double damage |
| Bull Rush | Marked + Copper Shock | A hit on a marked mob always releases a Copper Shock pulse | Change: Killing a marked enemy releases copper shock with 2x effectiveness and +2 targets |

**Rampaging**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Juggernaut | Rampaging + Uppercut | While rampaging, every 3rd swing launches | Change: Uppercut effectiveness is trippled, if mob hits a ceiling it will get damaged calculated by speed. |
| Blood Toxin | Rampaging + Venomous | While rampaging, poison lasts twice as long | Change: thrice* as long |
| Cursed Rage | Rampaging + Curse | Kills while rampaging curse the enemies around the victim | Change: Hitting cursed enemies increases rampaged effectiveness by 1.3x |
| Pillage | Rampaging + Plunder | While rampaging, Plunder converts twice the usual share of drops | Change: 50% chance to get double loot |
| Reaper | Rampaging + Withering | Kills on weakened enemies extend the rampage by 2 s | Change: Weakened enemies take 50% more damage while in rampage |
| Shadow Rush | Rampaging + Shulker Burst | While rampaging, Shulker Burst charges twice as fast | Approved |
| Massacre | Rampaging + Shared Pain | Kills by overkill count as two kills for Rampaging | Approved |
| Berserker | Rampaging + Guardian Strike | Vexes summoned while rampaging last one hit longer | Change: + have double attack speed and attack lives |
| Terror | Rampaging + Copper Shock | Kills while rampaging release a Copper Shock pulse from the victim | Approved |

**Uppercut**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Spore Launch | Uppercut + Venomous | Launched targets throw a Venomous splash where they land | Approved |
| Hex Geyser | Uppercut + Curse | Launches curse the enemies around the target | Approved |
| Shakedown | Uppercut + Plunder | A launched enemy may drop an emerald | Change: Launching enemies drops from 1-6 emeralds |
| Gallows | Uppercut + Withering | Launched targets are weakened | Change: weakened for double the weakening efficency |
| Skyshot | Uppercut + Shulker Burst | After a launch, a Shulker Burst bullet homes in on the launched target | Change: After a launch, 3 shulker bursts immediately target three different mobs |
| Skyfall | Uppercut + Shared Pain | A launched target that dies spreads its overkill around where it lands | Approved |
| Rising Guard | Uppercut + Guardian Strike | Launching summons a vex | Change: Launching does not summon vex but instead displays multiple vex hits on that target and burst him 3-5 blocks flying in front of the player |
| Ragdoll | Uppercut + Copper Shock | Launching releases a Copper Shock pulse from the target | Approved |

**Venomous**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Toxic Curse | Venomous + Curse | Cursed enemies are poisoned too | Change: Poison also applies curse |
| Antidote Trade | Venomous + Plunder | Kills on poisoned enemies convert twice the usual share of their drops | Change: emerald mutated loot becomes poisoned on their block, enemy stepping over it gains posion 2 for 1s |
| Blight | Venomous + Withering | Poison deals 50% more to weakened enemies | Change: 100%* |
| Venom Bullets | Venomous + Shulker Burst | Shulker Burst bullets poison on hit (Poison II) | Change: prioritizes poisoned enemies, hitting poisoned enemies extends levitation by double |
| Contagion | Venomous + Shared Pain | A poisoned enemy's death passes its poison on | Approved |
| Venom Fang | Venomous + Guardian Strike | Vexes poison what they hit | Approved |
| Toxic Arc | Venomous + Copper Shock | Copper Shock pulses poison the enemies they hit | Change: Copper shock 3x damage to poisoned enemies |

**Curse**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Cursed Hoard | Curse + Plunder | Kills on cursed enemies convert twice the usual share of their drops | Change: Kills on cursed enemies, the drops of the mob will do 1 damage per drop to all enemies of the same type in 3 blocks radius |
| Rotting Curse | Curse + Withering | Cursed enemies are weakened | Change: debuff level +2 |
| Hex Bullets | Curse + Shulker Burst | Bullets that hit apply a Curse at once | Approved |
| Curse Spread | Curse + Shared Pain | Overkill goes to cursed enemies first | Change: Cursed mobs obtain 2x damage from overkill |
| Hex Ward | Curse + Guardian Strike | Vexes curse what they hit | Change: Cursed enemies do not drain vexs health on attack |
| Curse Shock | Curse + Copper Shock | Copper Shock pulses carry a Curse | Change: a cursed enemy shocked by a pulse will get its debuff +1 level and +3s |

**Plunder**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Grave Robber | Plunder + Withering | Kills on weakened enemies drop double experience | Approved |
| Pickpocket | Plunder + Shulker Burst | Kills by a bullet convert twice the usual share of their drops | Change: The higher an enemy has fell after levitation, the more items it will drop (max 10x) 1x per 2 blocks |
| Jackpot | Plunder + Shared Pain | Kills by overkill convert twice the usual share of their drops | Change: Streak: overkill carries on after a kill (plain Shared Pain stops); each link in the chain raises the share Plunder converts: x1, x2, x3 |
| Treasure Guard | Plunder + Guardian Strike | Kills by a vex convert twice the usual share of their drops | Change: Vex hits drop 1-3 emeralds |
| Mugging | Plunder + Copper Shock | Kills by a Copper Shock pulse convert twice the usual share of their drops | Change: Shocking enemies has a chance of 20% to drop 4 emeralds |

**Withering**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Soul Bullets | Withering + Shulker Burst | Bullets weaken their target | Change: If a bullet hits a weakned target, it spreads and does 6 damage in a 3 block radius |
| Decay | Withering + Shared Pain | A weakened enemy's death passes its weakness to the nearest enemy | Approved |
| Wraith Vex | Withering + Guardian Strike | Vexes weaken what they hit | Change: + stacking with each |
| Soul Arc | Withering + Copper Shock | Copper Shock pulses weaken the enemies they hit | Approved |

**Shulker Burst**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Rift | Shulker Burst + Shared Pain | Bullet kills carry their overkill to Shared Pain's enemies too | Change: Overkills release 3 bullets |
| Shulker Guard | Shulker Burst + Guardian Strike | Releasing a Shulker Burst summons a vex | Approved |
| Shock Bullets | Shulker Burst + Copper Shock | Bullets that hit release a Copper Shock pulse | Approved |

**Shared Pain**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Martyr | Shared Pain + Guardian Strike | Kills by overkill give your vexes one more hit | Change: 2 more hits* |
| Shockfall | Shared Pain + Copper Shock | Enemies hit by overkill are struck by a Copper Shock pulse | Change: Shock Pulse spreads by any overkill, each new wave with 65%/n effectiveness |

**Guardian Strike**

| Combination | Needs | Planned effect | Review |
|---|---|---|---|
| Stalwart | Guardian Strike + Copper Shock | Vexes release a Copper Shock pulse when they die | Approved |

## Weapon styles

| Weapon | Style |
|---|---|
| Sword | Normal Better Combat combo. Air crit: any swing in the air that rolls a crit becomes a two-handed, centred overhead strike. 3rd swing on the ground becomes a rising cut with Uppercut |
| Spear | Every spear has attack speed 1.2 (the copper spear's, rounded up), whatever its material. Four-stab combo, every stab +75% faster; the first two stabs knock back 0.3× a normal hit, the third none, the heavy thrust 2.2× and launches |
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
| Chains | sentry | pillager outpost guards hold the line |
| Gravity | eye | the eye of ender pulls toward the End |
| Marked | wayfinder | trail-ruins travellers mark the way |
| Rampaging | snout | piglin brutes |
| Uppercut | raiser | "raise" a foe |
| Venomous | wild | jungle temple poison and vines |
| Curse | dune | the desert pyramid's curse |
| Plunder | coast | shipwreck treasure |
| Withering | rib | the fortress's wither skeletons |
| Shulker Burst | spire | End city shulkers |
| Shared Pain | host | trail ruins: a host shares the load |
| Guardian Strike | vex | the evoker's vexes, now on your side |
| Copper Shock | shaper | trail ruins: copper and its spark |

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
| Player power | The strongest nearby player's (48 blocks) carried weapons. Each weapon is its trim power levels + 2 per power pair; the strongest counts in full, the next ½, then ¼ and so on, max 30. Shields don't count |
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

**Structures** have their own place. Inside a structure's pieces the structure's multiplier and tier count, unless the surroundings above are harder. Each asks for better gear than the last. Villages stay a refuge, and trial chambers are left out because ominous bottles already scale them. Each row is a structure tag (`bcenchantments:difficulty/<name>`), so a datapack can add modded structures.

| Structure | Multiplier | Tier |
|---|---|---|
| Ruins: ruined portals, shipwrecks, ocean ruins, trail ruins | ×1.15 | Regular |
| Temples: desert and jungle pyramids, swamp huts, igloos | ×1.25 | Hard |
| Mineshafts | ×1.4 | Hard |
| Pillager outposts | ×1.5 | Hard |
| Ocean monuments | ×1.6 | Very Hard |
| Strongholds | ×1.75 | Very Hard |
| Nether fortresses | ×1.8 | Very Hard |
| Woodland mansions | ×1.9 | Extreme |
| Bastion remnants | ×2 | Extreme |
| Ancient cities | ×2 | Extreme |
| End cities | ×2.1 | Extreme |

The **tier** is raised by Bad Omen on a nearby player: one step at I–II, two at III–IV, three at V, up to **Possible Death**. It picks the armor below, and the difficulty panel above the inventory shows it beside the structure, or else the biome, with the multiplier: Stronghold, Very Hard ×1.75.

### Adaptation

Monsters learn the trim power a player leans on. Every weapon counts how often each of its powers fires (saved on the weapon, so a lost or destroyed weapon takes its count with it). Across the weapons the player carries, the power furthest toward full adaptation is the one monsters adapt to, and that share of new monsters spawning within 48 blocks counter it.

| Power kind | Fires to full adaptation |
|---|---|
| Every swing: Cold Edge, Gravity | 1500 |
| Conditional: Marked, Uppercut, Wind Charged, Rampaging, Venomous, Curse, Plunder, Shared Pain, Guardian Strike, Copper Shock | 500 |
| Charged: Lightning, Shockwave, Chains, Echo, Withering, Shulker Burst | 150 |

Counter share = 20% × (fires / fires to full), so at most 20% of spawns. A countering monster carries its counter in its name; when the power hits it, it shows a puff and a dull clang.

| Countered power | Name | Effect on that monster |
|---|---|---|
| Lightning | Insulated | Bolts deal 25% |
| Shockwave | Braced | Waves deal 25%, no push |
| Cold Edge | Warm | Never chilled or frozen |
| Uppercut | Heavy | Not launched |
| Chains | Slippery | Never rooted |
| Echo | Muffled | Echoes deal 25% |
| Wind Charged | Anchored | Bursts deal 25%, no push |
| Gravity | Weighted | Never pulled |
| Marked | Wary | Never marked |
| Rampaging | Grim | Killing it doesn't start or refresh a rampage |
| Shared Pain | Numb | Carried overkill deals 25% to it |
| Venomous | Immune | Never poisoned by a splash |
| Curse | Warded | Never cursed |
| Plunder | Penniless | Its drops don't grow or turn to emeralds |
| Withering | Soulless | Soul bursts deal 25%, no weakness |
| Shulker Burst | Grounded | Bullets don't levitate it |
| Guardian Strike | Ghostly | Vexes never hunt it |
| Copper Shock | Earthed | Pulses deal 25% |

Synergies are countered by the power whose effect they use: a bolt from Chained Lightning is still lightning, a Frost Burst chill is still Cold Edge. Switching to a weapon with other powers, or leaving the old one in a chest, moves the counters on.

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
