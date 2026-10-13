# Gameplay rules

The user stated these rules for this project. They override V/AS, defaults and other skills.

## Design

- **Expand Minecraft's systems, don't replace them.** Progression uses vanilla items: emeralds, the smithing table, armor trims and echo shards. Mob buffs stay subtle: no damage sponges.
- **Powers should change how a weapon plays,** not just add damage. Players pick a weapon for how it plays, not for its DPS.
- **Monsters are easy one at a time and dangerous in groups.** Pressure comes from density, consecutive hits, positioning, armor and knockback resistance.
- **Naming:** players see "Trim Powers", or "Powers" for short. The code still says `perk`; renaming it is not needed.

## Rules

- **Armor stands are unbreakable.** No attack, power or burn may break or ignite one (`ArmorStandMixin`, `vanilla/FireAspect`).
- **Mob farms must keep working.** A scaled monster still dies from the fall height it would have died from before scaling (`MobScaling.fallDamage`). The Better Combat switch exists so the user's sweeping-edge silverfish farm works.
- **Powers come only from trims,** one power per trim, never from books. Every two powers have a combination; powers pair in the order applied and a combined power is locked out of other combinations. Trim powers per weapon are unlimited: experience levels and refinement cost balance them.
- **Attack Speed is not a power.** Refinement grants it: +0.1 per level, +0.5 at level 5.
- **Shields take only a few powers,** one level each, no refinement needed: Wind Charged and Cold Edge (`perk/ShieldPowers`).
