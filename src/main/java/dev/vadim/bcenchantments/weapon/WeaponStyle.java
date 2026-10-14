package dev.vadim.bcenchantments.weapon;

import net.bettercombat.api.AttackHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How one kind of weapon plays beyond its Better Combat attributes. Every hook defaults to plain
 * Better Combat behaviour, so a style only overrides what makes it different. Register new styles in
 * {@link WeaponStyles}.
 */
public interface WeaponStyle {
	/** Whether this style drives {@code stack}. The first registered style that matches wins. */
	boolean matches(ItemStack stack);

	/**
	 * The attack to swing instead of the one Better Combat picked. Runs on both sides through the
	 * same hook, so the client animates what the server hits with.
	 */
	default AttackHand replace(Player player, AttackHand hand) {
		return hand;
	}

	/** Whether any attack of this style changes its cooldown; only then is {@link #cooldownFactor} asked. */
	default boolean shapesCooldown() {
		return false;
	}

	/** The attack's cooldown, relative to the weapon's own. */
	default float cooldownFactor(String animation) {
		return 1.0F;
	}

	/** How much of a hit's knockback the attack keeps. */
	default double knockbackFactor(String animation) {
		return 1.0;
	}

	/** Extra ticks the player has to wait after this attack before the next swing can start; it ends a combo. */
	default int restAfter(String animation) {
		return 0;
	}

	/** Whether the attack throws what it hits into the air when the weapon has Uppercut. */
	default boolean launches(Player player, String animation) {
		return false;
	}
}
