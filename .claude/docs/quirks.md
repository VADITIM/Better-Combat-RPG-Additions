# Quirks

Traps and edge cases, each written as the symptom you will run into first.

The Better Combat and Punchy traps are in `rules/animations.md` § 8.

## Combat

**Under Slow Falling, a mace never smashes.**
`LivingEntity.aiStep` resets `fallDistance` under Slow Falling. Updraft sets it by hand in `Uppercut.beforeAttack`.

**A mace swing triggers its smash only above 1.5 blocks of fall, and never while gliding.**
That is vanilla's `MaceItem.canSmashAttack`. `getAttackDamageBonus` reads `fallDistance` directly.

**Fire damage from our burn deals no knockback.**
Expected: `ON_FIRE` is in the `no_knockback` tag.

**Vanilla's own fire tick would burn a stacked target twice.**
`FireAspect` cancels vanilla's `ON_FIRE` damage that has no attacker while its own burn is running.

**A shield power fires but the shield was knocked out of use.**
`applyItemBlocking` can disable the shield before it returns, so `LivingEntityMixin` remembers the item at HEAD.

## Mobs

**A scaled monster no longer dies from the farm's drop.**
Fall damage must be scaled by the health that was added (`MobScaling.fallDamage`). Keep that in mind when changing health modifiers.

**A monster that rolled exactly 0 bonus health is scaled again on every load.**
The scaled marker is the health modifier, and `add` skips zero amounts. A double roll of exactly 0 is practically impossible.

## Build and tooling

**A static `init()` in a `Screen` subclass fails to compile.**
It clashes with `Screen.init()`. Use another name (`register()`).

**`gh` is not installed.**
Push with plain `git push`.

**Writing a file from Python sometimes fails with `Errno 22`.**
The file is briefly locked by the IDE or Gradle. Retry; the file is untouched.

## Testing

Live test sessions happen only when the user asks for one:

- The Modrinth profile is `Main 1.0.0`. The test world is the superflat "New World (1)".
- NVIDIA TDR crashes (`VK_ERROR_DEVICE_LOST`) predate the mod; relaunch.
- A NoAI mob ignores knockback. To measure knockback, use Slowness 255 on a normal mob instead.
- Check `key_key.chat` in `options.txt` before scripting chat commands; it has been rebound to Enter before.
