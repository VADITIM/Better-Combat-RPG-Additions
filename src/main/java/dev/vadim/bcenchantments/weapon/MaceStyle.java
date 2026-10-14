package dev.vadim.bcenchantments.weapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

/**
 * The mace: a swing from the ground launches with Uppercut, and a swing from the air stays the
 * mace's own smash. With Updraft that makes it a two-beat weapon: launch, then right click to slam down.
 * It is a timing weapon: it readies {@link MaceTiming#COOLDOWN} times slower than vanilla, and a swing
 * in the short Perfect Hit window hits harder; see {@link MaceTiming}.
 */
public final class MaceStyle implements WeaponStyle {
	public static final MaceStyle INSTANCE = new MaceStyle();

	private MaceStyle() {
	}

	@Override
	public boolean matches(ItemStack stack) {
		return stack.getItem() instanceof MaceItem;
	}

	@Override
	public boolean shapesCooldown() {
		return true;
	}

	@Override
	public float cooldownFactor(String animation) {
		return MaceTiming.COOLDOWN;
	}

	@Override
	public boolean launches(Player player, String animation) {
		return player.onGround();
	}
}
