package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The rest some weapons take after a full combo (the axe's 0.35 seconds; {@code WeaponStyle.restAfter}).
 * Swings only start on the client, so holding the next one back here is enough.
 */
public final class ComboRest {
	private static long until;
	private static Item weapon = Items.AIR;

	private ComboRest() {
	}

	/** A swing landed: if it ends a combo, the next one waits for the weapon to ready and then the rest. */
	public static void onAttackHit(Player player, AttackHand hand) {
		int rest = hand.isOffHand() ? 0 : WeaponStyles.restAfter(hand);
		if (rest <= 0 || !(player instanceof LocalPlayer)) {
			return;
		}
		float ready = PlayerAttackHelper.getAttackCooldownTicksCapped(player) * (float) (1.0 - hand.upswingRate());
		until = player.tickCount + Math.round(ready) + rest;
		weapon = hand.itemStack().getItem();
	}

	/** Whether the player is resting after a combo with the weapon still in hand. */
	public static boolean resting(LocalPlayer player) {
		return player.tickCount < until && player.getMainHandItem().is(weapon);
	}
}
