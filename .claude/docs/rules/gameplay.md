# Gameplay rules

The user stated these rules for this project. They override V/AS, defaults and other skills.

## Design

- **Expand Minecraft's systems, don't replace them.** Progression uses vanilla items: emeralds, the smithing table, armor trims and echo shards. Mob buffs stay subtle: no damage sponges.
- **Powers should change how a weapon plays,** not just add damage. Players pick a weapon for how it plays, not for its DPS.
- **Monsters are easy one at a time and dangerous in groups.** Pressure comes from density, consecutive hits, positioning, armor and knockback resistance.
- **Trim Powers add to enchanting, they don't replace it.** Vanilla enchantments are the early and mid game; Trim Powers are the late-game layer on a fully refined weapon.
- **Naming:** the mod is "Trim Enchantments"; players see "Trim Powers", or "Powers" for short. The code still says `perk`; renaming it is not needed.

## Rules

- **Armor stands are unbreakable.** No attack, power or burn may break or ignite one (`ArmorStandMixin`, `vanilla/FireAspect`).
- **Mob farms must keep working.** A scaled monster still dies from the fall height it would have died from before scaling (`MobScaling.fallDamage`). The Better Combat switch exists so the user's sweeping-edge silverfish farm works.
- **Powers come only from trims,** one power per trim, never from books. Every two powers have a combination; powers pair in the order applied and a combined power is locked out of other combinations. Trim powers per weapon are unlimited: experience levels and refinement cost balance them.
- **Refinement and trim powers are for diamond and netherite weapons only,** plus the mace and the trident (`bcenchantments:refinable`, checked in `Refinement.plan`). Lesser weapons keep what they already have. Shields keep their own few powers.
- **Attack Speed is not a power.** Refinement grants it: +0.05 per level, +0.25 at level 5. Level points (one per level, +3% to the chosen stat) never go to attack speed.
- **The mace is a timing weapon** (`weapon/MaceTiming`): 1.3× ready time, a 0.15 s Perfect Hit window at 0.8 of it for 1.5× damage, 0.9× otherwise. It keeps one attack.
- **Elites hurt with statuses, not damage.** Captains rally nearby monsters and are the one to kill first. Every monster bonus scales with where it spawns: mild near world spawn, harder underground, Nether and End.
- **Monsters adapt to the power a player leans on** (`mob/Adaptation`), so players eventually swap weapons. Up to 20% of nearby spawns counter it, growing with how often that power fired; the count lives on the weapon and goes with it when it is lost. Every carried weapon adds to the difficulty on its own, each further one less. Trial chambers and Bad Omen keep their own scaling.
- **Ominous bottles scale difficulty by hand.** Bad Omen I–V toughens nearby spawns and makes kills drop emeralds (5% + 1% per level after I) and, at V, echo shards (0.2%).
- **Shields take only a few powers,** one level each, no refinement needed: Wind Charged and Cold Edge (`perk/ShieldPowers`).
