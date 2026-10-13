package dev.vadim.bcenchantments.progression;

import dev.vadim.bcenchantments.network.ForgePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The testing shortcut behind the settings screen's weapon forge: hands the player a finished weapon with
 * the chosen trim powers, skipping refinement and the smithing table. Only creative players, operators and
 * singleplayer worlds may use it; the server rebuilds the weapon through {@link Refinement#forge}, so the
 * client's choices are checked again here.
 */
public final class Forge {
	private Forge() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(ForgePayload.TYPE, ForgePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ForgePayload.TYPE, (payload, context) -> give(context.player(), payload));
	}

	private static void give(ServerPlayer player, ForgePayload payload) {
		boolean allowed = player.hasInfiniteMaterials() || player.level().getServer().isSingleplayer()
			|| player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		if (!allowed) {
			player.sendSystemMessage(Component.translatable("forge.bcenchantments.denied"));
			return;
		}
		Item item = BuiltInRegistries.ITEM.getOptional(payload.item()).orElse(null);
		ItemStack weapon = item == null ? null : Refinement.forge(new ItemStack(item), payload.powers());
		if (weapon == null) {
			player.sendSystemMessage(Component.translatable("forge.bcenchantments.refused"));
			return;
		}
		player.getInventory().placeItemBackInInventory(weapon);
		player.sendSystemMessage(Component.translatable("forge.bcenchantments.done", weapon.getDisplayName()));
	}
}
