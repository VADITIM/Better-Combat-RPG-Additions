package dev.vadim.bcenchantments.progression;

import dev.vadim.bcenchantments.network.PowerLevelPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Raises and lowers the level of a trim power on the weapon in the open smithing table's middle slot.
 * A level up costs {@link Refinement#UPGRADE_COST} emeralds from the inventory (blocks count nine); a
 * level down hands the same back, so a level that disappoints can be undone for free.
 */
public final class PowerLevels {
	private static final int BASE_SLOT = 1;

	private PowerLevels() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(PowerLevelPayload.TYPE, PowerLevelPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PowerLevelPayload.TYPE, (payload, context) -> shift(context.player(), payload));
	}

	private static void shift(ServerPlayer player, PowerLevelPayload payload) {
		if (!(player.containerMenu instanceof SmithingMenu menu)) {
			return;
		}
		ItemStack shifted = Refinement.shiftPower(menu.getSlot(BASE_SLOT).getItem(), payload.trim(), payload.up());
		if (shifted == null) {
			return;
		}
		boolean free = player.hasInfiniteMaterials();
		if (payload.up()) {
			if (!free && !take(player.getInventory(), Refinement.UPGRADE_COST)) {
				return;
			}
		} else if (!free) {
			give(player, Refinement.UPGRADE_COST);
		}
		menu.getSlot(BASE_SLOT).set(shifted);
		menu.broadcastChanges();
		player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, payload.up() ? 1.3F : 0.9F);
	}

	/** Removes {@code amount} emeralds from {@code inventory}, loose ones first, changing a block into loose ones if need be; false if short. */
	private static boolean take(Inventory inventory, int amount) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			total += Refinement.gemValue(inventory.getItem(i)) * inventory.getItem(i).getCount();
		}
		if (total < amount) {
			return false;
		}
		int left = amount;
		for (int pass = 0; pass < 2 && left > 0; pass++) {
			for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
				ItemStack stack = inventory.getItem(i);
				int value = Refinement.gemValue(stack);
				if (value != (pass == 0 ? 1 : 9)) {
					continue;
				}
				int used = Math.min(stack.getCount(), (left + value - 1) / value);
				stack.shrink(used);
				left -= used * value;
			}
		}
		if (left < 0) {
			giveEmeralds(inventory, -left);
		}
		return true;
	}

	private static void give(ServerPlayer player, int amount) {
		giveEmeralds(player.getInventory(), amount);
	}

	private static void giveEmeralds(Inventory inventory, int amount) {
		int left = amount;
		while (left > 0) {
			int batch = Math.min(left, 64);
			ItemStack stack = new ItemStack(Items.EMERALD, batch);
			inventory.add(stack);
			if (!stack.isEmpty()) {
				inventory.player.drop(stack, false);
			}
			left -= batch;
		}
	}
}
