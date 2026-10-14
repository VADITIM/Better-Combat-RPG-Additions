package dev.vadim.bcenchantments.weapon;

import dev.vadim.bcenchantments.TrimEnchantments;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.item.ItemStack;

/**
 * The axe's two-cleave combo, defined in {@code data/bcenchantments/weapon_attributes/axe.json}: a
 * heavy cleave from the right across to the left, then a backhand from the left back to the right.
 * Each hits far harder than a sword or spear swing, so the combo ends in a rest: after the backhand
 * the next swing waits {@link #REST_TICKS} longer than the weapon's cooldown.
 */
public final class AxeStyle implements WeaponStyle {
	public static final AxeStyle INSTANCE = new AxeStyle();

	public static final String CLEAVE_RIGHT = TrimEnchantments.MOD_ID + ":axe_cleave_right";
	public static final String CLEAVE_LEFT = TrimEnchantments.MOD_ID + ":axe_cleave_left";

	/** The rest after each full combo: 0.35 seconds. */
	private static final int REST_TICKS = 7;

	private AxeStyle() {
	}

	/** An axe is any weapon whose combo opens with the axe's first cleave. */
	@Override
	public boolean matches(ItemStack stack) {
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		return attributes != null
			&& attributes.attacks() != null
			&& attributes.attacks().length > 0
			&& CLEAVE_RIGHT.equals(attributes.attacks()[0].animation());
	}

	@Override
	public int restAfter(String animation) {
		return CLEAVE_LEFT.equals(animation) ? REST_TICKS : 0;
	}
}
