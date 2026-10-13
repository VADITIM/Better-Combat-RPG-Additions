package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A perk that charges over landed swings and fires once the charge is full, shown as a row of blocks
 * on the combo HUD. Each hand charges separately. List it in {@link Perks#CHARGED} to enable it.
 */
public interface ChargedPerk {
	/** {@link #graceTicks()} for a charge that drops the moment Better Combat's combo breaks. */
	int BREAKS_WITH_COMBO = -1;

	/** Short name the client picks the row colour by. */
	String name();

	ResourceKey<Enchantment> enchantment();

	/** Charge needed to fire, for a weapon whose combo has {@code attacks} swings. */
	int required(int attacks);

	/** How long past Better Combat's combo window a pause may last before the charge drops, or {@link #BREAKS_WITH_COMBO}. */
	int graceTicks();

	/** The charge after {@code swing} landed. */
	int advance(int charge, SwingResult swing);

	void fire(SwingResult swing, int level);
}
