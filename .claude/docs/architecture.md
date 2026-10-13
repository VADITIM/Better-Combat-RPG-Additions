# Architecture

## Layers

1. **Data:**
   - powers are data-driven enchantments (`data/bcenchantments/enchantment`);
   - attacks are Better Combat `weapon_attributes`;
   - refinement is an item component (`Refinement.COMPONENT`), persistent and synced.
2. **Server logic:** the powers, refinement, mobs and Fire Aspect. All of it is driven by the hooks below.
3. **Client:** the HUD, settings, Field Guide, tooltips, and first-person clips through Punchy.

## The swing pipeline (server)

```
Better Combat attack request
  └ ServerNetworkMixin → ComboTracker.beginSwing        (AttackHand, combo count, cursor target)
      └ PlayerMixin.attack HEAD/RETURN → ComboTracker.before/afterAttack, Uppercut.beforeAttack
      └ ComboTracker.endSwing → SwingResult → CombatHooks.onSwing listeners
                                            → charge ChargedPerks → fire → ComboHudPayload to client
Damage events → CombatHooks.onDirectHit / onKill (Fabric AFTER_DAMAGE / AFTER_DEATH)
Server tick   → CombatHooks.onServerTick (ColdEdge, Shockwave, Control, Impact, Rampaging); Momentum tracks sprinting on its own server tick
```

**Adding a power:**

1. Write its enchantment JSON.
2. Add its key in `Perks`, and add it to `Perks.ALL`.
3. Register its hooks in `Perks.init`.
4. Add it to `Perks.CHARGED` if it charges.
5. Add it to `Refinement.Perk` and `TRIM_PERKS` for trims.
6. Update the Field Guide and the docs.

## Weapon styles (both sides)

- `WeaponStyles` holds the registered `WeaponStyle`s. The first one whose `matches` accepts the item drives it.
- `PlayerAttackHelperMixin` lets a style replace Better Combat's chosen attack on both sides.
- `PlayerMixin` scales the cooldown per attack.
- `LivingEntityMixin` scales knockback.
- `Uppercut` asks the style which attacks launch.

`rules/animations.md` covers the full Better Combat and Punchy flow.

## Crits

`CriticalHits` rolls each hand's next crit ahead of time and syncs it (`CritRollPayload`), so the client can pick the animation (the air crit) knowing the outcome. Vanilla's falling crit is replaced by the roll.

## Mobs

| What | Where |
|---|---|
| Health, damage, armor and knockback-resistance modifiers, once per monster | `MobScaling`, on `ENTITY_LOAD` |
| Monster hits on players ramping in a chain | `HitChain`, through `PlayerMixin.actuallyHurt` |
| Spawn cap | `SpawnDensity`, through `MobCategoryMixin` |

## Client state

| What | Where |
|---|---|
| HUD settings and the Better Combat switch | `config/bcenchantments-client.json`, through `HudConfig` |
| Switch → registry | `RpgAdditionsClient` copies the switch into the static `BetterCombatSwitch` every client tick; `WeaponRegistryMixin` reads it |
