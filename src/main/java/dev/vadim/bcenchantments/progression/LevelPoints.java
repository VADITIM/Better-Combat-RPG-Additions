package dev.vadim.bcenchantments.progression;

import dev.vadim.bcenchantments.network.SpendPointPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Spends a weapon's level points ({@link Refinement.Points}). The smithing panel asks for one point at a
 * time; the server spends it on the weapon in the open smithing table's middle slot, so the weapon never
 * has to leave the table, and checks again that it has a point left.
 */
public final class LevelPoints {
	private static final int BASE_SLOT = 1;

	private LevelPoints() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(SpendPointPayload.TYPE, SpendPointPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SpendPointPayload.TYPE, (payload, context) -> spend(context.player(), payload));
	}

	private static void spend(ServerPlayer player, SpendPointPayload payload) {
		Refinement.Stat stat = Refinement.Stat.of(payload.stat());
		if (stat == null || !(player.containerMenu instanceof SmithingMenu menu)) {
			return;
		}
		ItemStack spent = Refinement.spendPoint(menu.getSlot(BASE_SLOT).getItem(), stat);
		if (spent == null) {
			return;
		}
		menu.getSlot(BASE_SLOT).set(spent);
		menu.broadcastChanges();
		player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
	}
}
