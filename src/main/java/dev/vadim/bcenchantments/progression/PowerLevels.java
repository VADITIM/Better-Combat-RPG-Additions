package dev.vadim.bcenchantments.progression;

import dev.vadim.bcenchantments.network.PowerLevelPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The smithing panel's Upgrade and Degrade buttons. Upgrade changes nothing on the weapon: it puts the
 * power's trim template in the template slot and {@link Refinement#UPGRADE_COST} emeralds from the
 * inventory in the addition slot (blocks are changed into loose emeralds). The table then offers the
 * result like any refinement step ({@link Refinement#plan}); taking it spends both. Degrade, confirmed
 * in the panel, lowers the power a level on the weapon at once and gives nothing back.
 */
public final class PowerLevels {
	private static final int TEMPLATE_SLOT = 0;
	private static final int BASE_SLOT = 1;
	private static final int ADDITION_SLOT = 2;

	private PowerLevels() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(PowerLevelPayload.TYPE, PowerLevelPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PowerLevelPayload.TYPE, (payload, context) -> prepare(context.player(), payload));
	}

	private static void prepare(ServerPlayer player, PowerLevelPayload payload) {
		if (!(player.containerMenu instanceof SmithingMenu menu)) {
			return;
		}
		ItemStack shifted = Refinement.shiftPower(menu.getSlot(BASE_SLOT).getItem(), payload.trim(), payload.up());
		if (shifted == null) {
			return;
		}
		if (!payload.up()) {
			menu.getSlot(BASE_SLOT).set(shifted);
			menu.broadcastChanges();
			return;
		}
		Item template = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Refinement.template(payload.trim()));
		boolean free = player.hasInfiniteMaterials();
		Inventory inventory = player.getInventory();

		// Whatever sits in the template and addition slots goes back to the inventory first.
		for (int slot : new int[] {TEMPLATE_SLOT, ADDITION_SLOT}) {
			ItemStack held = menu.getSlot(slot).getItem();
			if (!held.isEmpty()) {
				menu.getSlot(slot).set(ItemStack.EMPTY);
				if (!inventory.add(held)) {
					player.drop(held, false);
				}
			}
		}
		ItemStack found = free ? new ItemStack(template) : takeOne(inventory, template);
		if (found.isEmpty()) {
			menu.broadcastChanges();
			return;
		}
		if (free) {
			menu.getSlot(ADDITION_SLOT).set(new ItemStack(Items.EMERALD, Refinement.UPGRADE_COST));
		} else if (!take(inventory, Refinement.UPGRADE_COST)) {
			inventory.add(found);
			menu.broadcastChanges();
			return;
		}
		menu.getSlot(TEMPLATE_SLOT).set(found);
		menu.broadcastChanges();
	}

	/** One {@code item} out of the inventory, or empty if it has none. */
	private static ItemStack takeOne(Inventory inventory, Item item) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				return stack.split(1);
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Moves {@code amount} emeralds from the inventory to the table's addition slot, loose ones first and a
	 * block changed into loose ones if need be; false, with nothing moved, if the inventory is short.
	 */
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
			giveEmeralds(inventory.player instanceof ServerPlayer p ? p : null, inventory, -left);
		}
		if (inventory.player.containerMenu instanceof SmithingMenu menu) {
			menu.getSlot(ADDITION_SLOT).set(new ItemStack(Items.EMERALD, amount));
		}
		return true;
	}

	/** Hands {@code amount} emeralds (change from a block) back, dropping what does not fit. */
	private static void giveEmeralds(ServerPlayer player, Inventory inventory, int amount) {
		int left = amount;
		while (left > 0) {
			int batch = Math.min(left, 64);
			ItemStack stack = new ItemStack(Items.EMERALD, batch);
			inventory.add(stack);
			if (!stack.isEmpty() && player != null) {
				player.drop(stack, false);
			}
			left -= batch;
		}
	}
}
