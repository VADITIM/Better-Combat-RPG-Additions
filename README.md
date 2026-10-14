# Trim Enchantments

Trim Enchantments reworks Minecraft's combat into something closer to a fast-paced hack 'n' slash RPG, with a lot of inspiration from Minecraft Dungeons. It adds no new items. Instead it gives real uses to things the game already has: echo shards, armor trims, emeralds and the smithing table all become part of a deeper weapon progression.

Requires [Better Combat](https://modrinth.com/mod/better-combat) and Punchy.

## ⚔️ Overhauled Combat

Every weapon gets its own way of fighting. Spears now have a four-hit combo, which makes choosing between a sword and a spear a matter of how you like to fight. The mace gets a more natural, reliable attack pattern, so it works as a proper end-game weapon instead of something you only pull out in specific situations.

## ✨ Trim Powers

Armor trims become Trim Powers, effects you add to weapons and shields at the smithing table. There are 18 of them, and any two can be combined, for 153 combinations in total. Each power changes how a weapon plays, and combining them lets you shape your gear into a build of your own. Because they can get very strong, the world gets tougher to match.

## 🔨 Weapon Refinement

Weapons can be refined with emeralds. Each level raises attack speed a little and gives a small random boost to damage, crit chance and crit damage. Attack speed is the gain you'll feel most, since the weapon keeps getting more responsive the more you put into it.

Once a weapon reaches refinement level V, it can take Trim Powers. From there the path goes from refinement to your first power and on to combinations. Powers pair up in the order you add them, so each second power combines with the one before it. The same power can't be added twice, which keeps the system about choosing combinations rather than stacking one effect.

## 💀 Dynamic Difficulty

To balance out powers this strong, the world gets more dangerous over time.

Difficulty is tracked chunk by chunk. The longer an area stays loaded, the stronger its monsters get, up to double their stats after about an hour, and it resets after five Minecraft days. On top of that, the whole world gets harder each day, reaching about 1.4× at day 200.

Monsters don't just get more health. They also hit harder, spawn more often, and do more damage with each hit they land on you in a row, up to double. One monster on its own is easy, but getting surrounded quickly becomes dangerous.

## 💎 A Reason to Keep Going

In vanilla, gear progression mostly stops once you have netherite. Trim Enchantments gives you something to keep working toward. You keep improving your weapons and building toward your own setup, and the progression is expensive enough that you earn it over time.

Along the way, vanilla systems get a new purpose. Armor trims become upgrades that change how you play rather than just cosmetics. Emeralds become a real currency, echo shards are worth something late in the game, and the smithing table is where your weapons grow. The result feels like a hack 'n' slash RPG while still feeling like Minecraft.

Refine your weapons, build your Trim Powers and survive a world that keeps getting harder. There's always another upgrade to work toward.

---

## All Trim Powers

Each armor trim grants one power, raised from level I to III. Powers marked \* are designed but not in the game yet; their trims are refused at the smithing table until they are.

| Power | Trim | What it does | I / II / III |
|---|---|---|---|
| Lightning | bolt | A bolt strikes the target after a long combo | 7 / 13 / 17 damage |
| Shockwave | ward | After three swings in a row that each hit 3+ enemies, a wave flies out | 4 / 6 / 8 damage, 8 / 10 / 12 blocks |
| Chains | vex | Hits build a charge; when full, the target and nearby enemies are rooted for 0.7 s | binds 2 / 3 / 4 enemies |
| Echo | silence | Every 4th swing strikes its targets again 0.3 s later | 50 / 65 / 80% of the swing |
| Cold Edge | tide | Hits slow; 3 s of chill freezes the target solid | −10 / 25 / 40% speed |
| Wind Charged | flow | Kills burst like a wind charge, hurting and pushing what's around | 3 / 4 / 5 damage |
| Gravity | eye | Every landed swing pulls nearby enemies toward the target | 3 / 4 / 5 block radius |
| Momentum | wayfinder | Sprint, then land a swing within 0.5 s to throw its targets back hard | knockback 0.5 / 0.7 / 0.9 |
| Rampaging | snout | Kills grant attack speed for 4 s, refreshed by every further kill | +20 / 30 / 40% |
| Uppercut | raiser | The sword's 3rd swing, the spear's heavy thrust or a grounded mace swing launches the target | launch 0.55 / 0.7 / 0.85 |
| Venom\* | wild | Hits poison; a target poisoned for 3 s bursts into a small poison cloud | 2 / 3 / 4 s poison |
| Sandstorm\* | dune | Every 5th landed swing throws sand, blinding enemies around you | 3 / 4 / 5 block radius |
| Plunder\* | coast | Monster kills can drop an emerald | 4 / 7 / 10% |
| Withering\* | rib | Hits wither; withered enemies deal 15% less damage | 2 / 3 / 4 s |
| Phase\* | spire | Every few landed swings you teleport behind the target, and your next hit crits | every 6 / 5 / 4 swings |
| Shared Pain\* | host | A kill's overkill damage carries to the nearest enemy | 3 / 4 / 5 blocks |
| Guarding Strike\* | sentry | Kills grant Resistance I | 2 / 3 / 4 s |
| Daze\* | shaper | Every 5th landed swing dazes its targets: they can't move or attack | 0.8 / 1.0 / 1.2 s |

### Shield powers

Shields need no refinement and take two powers at one level each.

| Power | Trim | On a blocked hit |
|---|---|---|
| Wind Charged | flow | A gust throws back everything in front of the shield (every 30 s) |
| Cold Edge | tide | A nearby attacker is chilled hard (every 10 s) |

## All Combinations

Every two powers have one combination: 153 in all. Powers pair in the order you put them on (1st with 2nd, 3rd with 4th, …), and a power already in a combination is locked out of every other.

### In the game

| Combination | Powers | Effect |
|---|---|---|
| Chained Lightning | Chains + Lightning | Bound enemies get half-power bolts and a short root |
| Charged Lightning | Lightning + Wind Charged | The bolt bursts like a wind charge |
| Updraft | Uppercut + Wind Charged | You and the target go straight up. Right click in the air: the mace slams down for a 1.5× smash, the sword dives and cuts around where it lands, the spear lunges through enemies |
| Frozen Chains | Chains + Cold Edge | Bound enemies are chilled |
| Frost Burst | Cold Edge + Wind Charged | Death bursts chill what they catch |
| Resonance | Echo + Lightning | Echoes spark a quarter-power bolt |
| Tempest | Shockwave + Wind Charged | The shockwave throws enemies much harder, and slamming them into a wall hurts. A mace smash also splashes nearby enemies |
| Shatter | Cold Edge + Lightning | Bolts hit slowed enemies harder and frozen ones twice as hard; kills spread chill |
| Singularity | Gravity + Shockwave | The shockwave drags enemies beside its path into it |
| Black Hole | Gravity + Chains | Every bound enemy pulls the free enemies around it in |
| Cyclone | Gravity + Wind Charged | Kills pull nearby enemies in before the burst |
| Launch | Momentum + Uppercut | A launch after a run-up goes higher, and you go up after the target |
| Blast Step | Momentum + Wind Charged | A kill by a run-up swing bursts you onward |
| Frenzy | Echo + Refinement V | Echo strikes every 3rd swing instead of every 4th |

### Planned

**Lightning**

| Combination | Powers | Effect |
|---|---|---|
| Thunderwave | Lightning + Shockwave | Every enemy the shockwave cuts is struck by a quarter-power bolt |
| Lightning Rod | Lightning + Gravity | Before the bolt lands, nearby enemies are pulled to the target |
| Thunder Charge | Lightning + Momentum | A run-up swing adds 3 hits of Lightning charge |
| Storm Frenzy | Lightning + Rampaging | While rampaging, Lightning charges twice as fast |
| Skybreaker | Lightning + Uppercut | Launched targets are struck by a half-power bolt at the top of their flight |
| Toxic Storm | Lightning + Venom | Bolts leave a poison cloud where they strike |
| Dust Devil | Lightning + Sandstorm | Every enemy Sandstorm blinds is struck by a quarter-power bolt |
| Gilded Storm | Lightning + Plunder | Bolt kills roll Plunder twice |
| Black Lightning | Lightning + Withering | Bolts wither everything nearby |
| Blink Strike | Lightning + Phase | Phase calls a half-power bolt on the target |
| Arc | Lightning + Shared Pain | A bolt kill's overkill jumps to the nearest enemy as a bolt |
| Static Guard | Lightning + Guarding Strike | While Resistance lasts, enemies that hit you are struck by a quarter-power bolt |
| Stunbolt | Lightning + Daze | Bolts daze the target and everything around it |

**Wind Charged**

| Combination | Powers | Effect |
|---|---|---|
| Gale Echo | Wind Charged + Echo | Echo strikes push their targets back like a small gust |
| Whiplash | Wind Charged + Chains | When Chains fires, bound enemies are yanked to the target |
| Hurricane | Wind Charged + Rampaging | While rampaging, death bursts are 50% wider |
| Toxic Gust | Wind Charged + Venom | Death bursts poison what they catch |
| Haboob | Wind Charged + Sandstorm | Death bursts blind what they catch |
| Windfall | Wind Charged + Plunder | Death bursts blow loot and experience to you |
| Ashen Wind | Wind Charged + Withering | Death bursts wither what they catch |
| Slipstream | Wind Charged + Phase | Phase leaves a gust where you were |
| Shrapnel | Wind Charged + Shared Pain | A kill's overkill is added to its death burst |
| Bulwark Gust | Wind Charged + Guarding Strike | Kill bursts throw enemies away from you |
| Concussion | Wind Charged + Daze | Death bursts briefly daze what they catch |

**Cold Edge**

| Combination | Powers | Effect |
|---|---|---|
| Hoarfrost Echo | Cold Edge + Echo | Echo strikes chill |
| Glacier Wave | Cold Edge + Shockwave | The shockwave chills what it cuts and frosts the ground it passes |
| Frost Vortex | Cold Edge + Gravity | Pulled enemies are chilled |
| Frostbite Charge | Cold Edge + Momentum | A run-up swing freezes its target solid at once |
| Cold Blood | Cold Edge + Rampaging | While rampaging, chill builds twice as fast |
| Frozen Ascent | Cold Edge + Uppercut | Launched targets freeze solid in the air |
| Frostbite | Cold Edge + Venom | Chilled enemies take poison damage twice as fast |
| Blizzard | Cold Edge + Sandstorm | Blinded enemies are chilled too |
| Ice Treasure | Cold Edge + Plunder | Kills on frozen enemies roll Plunder twice |
| Necrotic Frost | Cold Edge + Withering | Chill doesn't thaw on withered enemies |
| Cold Snap | Cold Edge + Phase | Phase freezes the target solid as you appear behind it |
| Shattering Pain | Cold Edge + Shared Pain | Overkill from a frozen kill spreads to two enemies |
| Permafrost | Cold Edge + Guarding Strike | While Resistance lasts, enemies that hit you are chilled |
| Deep Freeze | Cold Edge + Daze | Dazed enemies freeze solid at once |

**Echo**

| Combination | Powers | Effect |
|---|---|---|
| Aftershock | Echo + Shockwave | A second, half-damage wave follows every shockwave |
| Reverberation | Echo + Chains | Echo strikes add Chains charge as full hits |
| Pull Echo | Echo + Gravity | Echo strikes pull nearby enemies to their target |
| Double Step | Echo + Momentum | A run-up swing always echoes |
| Bloodlust | Echo + Rampaging | While rampaging, Echo strikes every 3rd swing |
| Rebound | Echo + Uppercut | Launches echo: the target is launched again, a little higher |
| Toxic Echo | Echo + Venom | Echo strikes poison |
| Whispering Sands | Echo + Sandstorm | Echo strikes briefly blind their targets |
| Echoing Coin | Echo + Plunder | Echo kills roll Plunder twice |
| Withering Echo | Echo + Withering | Echo strikes wither |
| Afterimage | Echo + Phase | After Phase, an echo strikes the target from where you were |
| Resounding Pain | Echo + Shared Pain | Echo kills spread their overkill too |
| Ward Echo | Echo + Guarding Strike | Echo kills grant Resistance |
| Ringing Blow | Echo + Daze | Echo strikes briefly daze |

**Shockwave**

| Combination | Powers | Effect |
|---|---|---|
| Snare Wave | Shockwave + Chains | The shockwave briefly roots what it cuts |
| Surge | Shockwave + Momentum | A run-up swing fires the shockwave with two of its three swings charged |
| Warpath | Shockwave + Rampaging | While rampaging, Shockwave needs 2 swings instead of 3 |
| Upheaval | Shockwave + Uppercut | The shockwave launches what it cuts |
| Plague Wave | Shockwave + Venom | The shockwave leaves a poison trail |
| Sandwave | Shockwave + Sandstorm | The shockwave blinds what it cuts |
| Gold Rush | Shockwave + Plunder | Shockwave kills roll Plunder twice |
| Death Wave | Shockwave + Withering | The shockwave withers what it cuts |
| Rift Wave | Shockwave + Phase | Phase fires a half-power shockwave through the target |
| Crushing Wave | Shockwave + Shared Pain | Each kill adds its overkill to the wave's damage |
| Shield Wall | Shockwave + Guarding Strike | Firing a shockwave grants Resistance I |
| Stunwave | Shockwave + Daze | The shockwave dazes what it cuts |

**Chains**

| Combination | Powers | Effect |
|---|---|---|
| Grapple | Chains + Momentum | A run-up swing fires Chains if it's half charged |
| Hunter's Chains | Chains + Rampaging | Kills add 20% Chains charge |
| Chain Lift | Chains + Uppercut | Launching a bound enemy launches every enemy bound with it |
| Venom Links | Chains + Venom | Bound enemies are poisoned |
| Blind Bind | Chains + Sandstorm | Bound enemies are blinded |
| Chain Gang | Chains + Plunder | Enemies killed while rooted roll Plunder twice |
| Death Grip | Chains + Withering | Bound enemies wither |
| Anchor | Chains + Phase | Phase teleports you to the farthest bound enemy |
| Shared Bonds | Chains + Shared Pain | 30% of the damage to the target is shared with every enemy bound to it |
| Iron Ward | Chains + Guarding Strike | Firing Chains grants Resistance I |
| Shackles | Chains + Daze | Bound enemies stay dazed while they're rooted |

**Gravity**

| Combination | Powers | Effect |
|---|---|---|
| Slingshot | Gravity + Momentum | A run-up swing pulls twice as hard |
| Feeding Frenzy | Gravity + Rampaging | While rampaging, Gravity reaches 2 blocks further |
| Implosion | Gravity + Uppercut | Launched targets pull nearby enemies under them as they rise |
| Miasma | Gravity + Venom | Pulled enemies are poisoned |
| Sinkhole | Gravity + Sandstorm | Pulled enemies sink and slow down as in quicksand |
| Magnet | Gravity + Plunder | Gravity also pulls items and experience to you |
| Event Horizon | Gravity + Withering | Pulled enemies wither |
| Warp | Gravity + Phase | Phase pulls everything near your old spot to the target |
| Collapse | Gravity + Shared Pain | Overkill splits across every pulled enemy |
| Gravity Well | Gravity + Guarding Strike | Enemies pulled close to you deal less damage for a moment |
| Crush | Gravity + Daze | Pulled enemies that reach the target are dazed |

**Momentum**

| Combination | Powers | Effect |
|---|---|---|
| Stampede | Momentum + Rampaging | While rampaging, a run-up builds in half the time |
| Toxic Charge | Momentum + Venom | A run-up swing poisons hard |
| Dust Trail | Momentum + Sandstorm | Sprinting kicks up sand that blinds the enemies you pass |
| Smash and Grab | Momentum + Plunder | Run-up kills roll Plunder twice |
| Death Charge | Momentum + Withering | A run-up swing withers its targets hard |
| Blitz | Momentum + Phase | A run-up swing triggers Phase at once |
| Bowling | Momentum + Shared Pain | Run-up kills throw their overkill at the enemy behind them |
| Shield Charge | Momentum + Guarding Strike | Sprinting grants Resistance I |
| Bull Rush | Momentum + Daze | A run-up swing dazes its targets |

**Rampaging**

| Combination | Powers | Effect |
|---|---|---|
| Juggernaut | Rampaging + Uppercut | While rampaging, every 3rd swing launches |
| Blood Toxin | Rampaging + Venom | While rampaging, poison lasts twice as long |
| Desert Rage | Rampaging + Sandstorm | Kills while rampaging blind the enemies around the victim |
| Pillage | Rampaging + Plunder | While rampaging, Plunder's chance doubles |
| Reaper | Rampaging + Withering | Kills on withered enemies extend the rampage |
| Shadow Rush | Rampaging + Phase | While rampaging, Phase charges twice as fast |
| Massacre | Rampaging + Shared Pain | Kills by overkill refresh the rampage and spread again |
| Berserker | Rampaging + Guarding Strike | Each kill while rampaging heals a heart |
| Terror | Rampaging + Daze | Kills while rampaging daze the enemies nearby |

**Uppercut**

| Combination | Powers | Effect |
|---|---|---|
| Spore Launch | Uppercut + Venom | Launched targets leave a poison cloud where they land |
| Sand Geyser | Uppercut + Sandstorm | Launches blind the enemies around the target |
| Shakedown | Uppercut + Plunder | A launched enemy may drop an emerald |
| Gallows | Uppercut + Withering | Launched targets wither |
| Air Step | Uppercut + Phase | After a launch, Phase puts you above the target for a follow-up |
| Skyfall | Uppercut + Shared Pain | A launched target that dies spreads its overkill where it lands |
| Rising Guard | Uppercut + Guarding Strike | Launching grants Resistance I |
| Ragdoll | Uppercut + Daze | Launched targets stay dazed until they land |

**Venom**

| Combination | Powers | Effect |
|---|---|---|
| Toxic Dust | Venom + Sandstorm | Sandstorm also poisons |
| Antidote Trade | Venom + Plunder | Kills on poisoned enemies roll Plunder twice |
| Blight | Venom + Withering | Poison and wither on the same enemy deal 50% more |
| Venom Fang | Venom + Phase | Phase's hit poisons hard |
| Contagion | Venom + Shared Pain | A poisoned enemy's death passes its poison on |
| Thick Skin | Venom + Guarding Strike | Killing a poisoned enemy grants Resistance for twice as long |
| Paralysis | Venom + Daze | Poisoned enemies stay dazed longer |

**Sandstorm**

| Combination | Powers | Effect |
|---|---|---|
| Buried Treasure | Sandstorm + Plunder | Kills on blinded enemies roll Plunder twice |
| Mummify | Sandstorm + Withering | Blinded enemies wither |
| Mirage | Sandstorm + Phase | Phase raises a sandstorm where you were |
| Sand Blast | Sandstorm + Shared Pain | Overkill spreads to every blinded enemy nearby |
| Dust Cloak | Sandstorm + Guarding Strike | Blinded enemies deal half damage to you |
| Dust Daze | Sandstorm + Daze | Sandstorm also briefly dazes |

**Plunder**

| Combination | Powers | Effect |
|---|---|---|
| Grave Robber | Plunder + Withering | Kills on withered enemies drop double experience |
| Pickpocket | Plunder + Phase | Phase's hit rolls Plunder |
| Jackpot | Plunder + Shared Pain | Kills by overkill roll Plunder |
| Treasure Guard | Plunder + Guarding Strike | Picking up an emerald grants Resistance I |
| Mugging | Plunder + Daze | Kills on dazed enemies roll Plunder twice |

**Withering**

| Combination | Powers | Effect |
|---|---|---|
| Soul Step | Withering + Phase | Phase withers the enemies around where you appear |
| Decay | Withering + Shared Pain | A withered enemy's death passes its wither on |
| Death Ward | Withering + Guarding Strike | Withered enemies that hit you take damage |
| Petrify | Withering + Daze | Dazed enemies wither twice as fast |

**Phase**

| Combination | Powers | Effect |
|---|---|---|
| Rift | Phase + Shared Pain | Overkill from Phase's crit spreads to two enemies |
| Blink Guard | Phase + Guarding Strike | Phase grants Resistance I |
| Ambush | Phase + Daze | Phase dazes the target |

**Shared Pain**

| Combination | Powers | Effect |
|---|---|---|
| Martyr | Shared Pain + Guarding Strike | Overkill also heals you for a quarter of it |
| Shockfall | Shared Pain + Daze | Enemies hit by overkill are dazed |

**Guarding Strike**

| Combination | Powers | Effect |
|---|---|---|
| Stalwart | Guarding Strike + Daze | Kills while Resistance lasts daze the enemies nearby |

## Building

Punchy is not on a Maven repository. Put `punchy-2.8a-fabric-26.2.jar` in `libs/`, then run:

```
./gradlew build
```

The jar lands in `build/libs/`.

## License

All Rights Reserved. See [LICENSE](LICENSE). Better Combat and Punchy are separate mods under their own licenses.
