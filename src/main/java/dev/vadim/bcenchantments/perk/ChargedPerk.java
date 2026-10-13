package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A perk that charges over landed swings and fires once the charge is full, shown as a row of blocks
 * on the combo HUD. Each hand charges separately. List it in {@link Perks#CHARGED} to enable it.
 */
public interface ChargedPerk {
	/** {@link #keepTicks} for a charge that drops the moment Better Combat's combo breaks. */
	int BREAKS_WITH_COMBO = -1;

	/** Short name the client picks the row colour by. */
	String name();

	ResourceKey<Enchantment> enchantment();

	/** Charge needed to fire with {@code weapon}, whose combo has {@code attacks} swings. */
	int required(ItemStack weapon, int attacks);

	/** Blocks the HUD row shows; the charge is scaled onto them. */
	default int segments(ItemStack weapon, int attacks) {
		return required(weapon, attacks);
	}

	/**
	 * How long the charge survives after the last landed swing, given Better Combat's combo window,
	 * or {@link #BREAKS_WITH_COMBO}.
	 */
	int keepTicks(int comboWindowTicks);

	/** Whether a swing that hits nothing empties the charge. */
	default boolean resetsOnMiss() {
		return true;
	}

	/** The charge after {@code swing} landed. */
	int advance(int charge, SwingResult swing);

	void fire(SwingResult swing, int level);
}
