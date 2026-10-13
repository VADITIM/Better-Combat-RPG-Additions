package dev.vadim.bcenchantments.weapon;

import dev.vadim.bcenchantments.RpgAdditions;
import java.util.Map;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.item.ItemStack;

/**
 * The spear's four-stab combo, defined in {@code data/bcenchantments/weapon_attributes/spear.json}:
 * two fast stabs from the right and the left, a centred stab a little later, and a heavy thrust
 * from above. Better Combat gives every swing of a weapon the same length, so each stab scales the
 * attack cooldown here instead; the fast stabs are cheaper and the heavy one costs the most. Their
 * damage multipliers even it out to about the weapon's normal damage per second. Only the heavy thrust
 * knocks back, harder than a normal hit, and it is the spear's launcher for Uppercut.
 */
public final class SpearStyle implements WeaponStyle {
	public static final SpearStyle INSTANCE = new SpearStyle();

	public static final String STAB_RIGHT = RpgAdditions.MOD_ID + ":spear_stab_right";
	public static final String STAB_LEFT = RpgAdditions.MOD_ID + ":spear_stab_left";
	public static final String STAB_CENTER = RpgAdditions.MOD_ID + ":spear_stab_center";
	public static final String HEAVY = RpgAdditions.MOD_ID + ":spear_heavy";

	/** Attack cooldown of each stab, relative to the weapon's own. */
	private static final Map<String, Float> COOLDOWN = Map.of(
		STAB_RIGHT, 0.6F,
		STAB_LEFT, 0.6F,
		STAB_CENTER, 0.95F,
		HEAVY, 1.4F
	);

	/** The heavy thrust's knockback, relative to a normal hit's; the stabs before it push nothing. */
	private static final double HEAVY_KNOCKBACK = 2.2;

	private SpearStyle() {
	}

	public static boolean isSpearAttack(String animation) {
		return animation != null && COOLDOWN.containsKey(animation);
	}

	/** A spear is any weapon whose combo opens with one of the spear's stabs. */
	@Override
	public boolean matches(ItemStack stack) {
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		return attributes != null
			&& attributes.attacks() != null
			&& attributes.attacks().length > 0
			&& isSpearAttack(attributes.attacks()[0].animation());
	}

	@Override
	public boolean shapesCooldown() {
		return true;
	}

	@Override
	public float cooldownFactor(String animation) {
		return COOLDOWN.getOrDefault(animation, 1.0F);
	}

	@Override
	public double knockbackFactor(String animation) {
		if (HEAVY.equals(animation)) {
			return HEAVY_KNOCKBACK;
		}
		return isSpearAttack(animation) ? 0.0 : 1.0;
	}

	@Override
	public boolean launches(String animation) {
		return HEAVY.equals(animation);
	}
}
