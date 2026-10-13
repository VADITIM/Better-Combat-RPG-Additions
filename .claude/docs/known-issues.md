# Known issues

Bugs and debt that exist right now. Remove an entry when it is fixed.

## Bugs

**A dedicated server cannot load the mod.**
`fabric.mod.json` lists `punchy` in `depends`, but Punchy is client-only. Move it to `recommends`, or make `client/PunchySpear` and the Punchy mixins load only when Punchy is present.

**The Better Combat switch is a client-side static flag.**
It only works in singleplayer or for a LAN host. Multiplayer needs a synced per-player setting.

**Punchy plays its falling-crit clip for airborne swings that did not roll a crit.**
It does this when the item has a tuned `critical_hit` clip, because Punchy reads vanilla's falling check.

**The air-crit and uppercut first-person clips have a fixed length** (`anim_speed_1`). They do not follow the weapon's attack speed.

**The air-crit animation has not been tested in game.** It is a centred two-handed strike in third person; the first-person clip reuses the old critical slam.

## Debt

**The trim → power remap changes existing items.**
Weapons already trimmed with vex, flow, silence or ward now grant their new power.

**A mace with Uppercut launches on every grounded hit.**

**Shatter's kill spread fires on any kill made with a Shatter weapon,** not only lightning kills.

**`ServerNetworkMixin` targets a synthetic lambda name** (`lambda$handleAttackRequest$2`). It breaks on Better Combat updates that reorder lambdas.
