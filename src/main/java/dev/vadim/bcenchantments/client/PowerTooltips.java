package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Tooltips about trim powers: the refinement and combination lines on weapons (Shift opens their
 * descriptions), the power a trim template grants, and, at the smithing table, every combination the
 * weapon's next trim could make. Also keeps this mod's enchanted books out of the creative menu,
 * since powers come only from trims.
 */
public final class PowerTooltips {
	private PowerTooltips() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			boolean details = Minecraft.getInstance().hasShiftDown();
			// Both insert under the name, so the refinement lines end up above the synergies.
			Synergy.tooltip(stack, lines, details);
			Refinement.tooltip(stack, lines, details);
			template(stack, lines, details);
			if (Minecraft.getInstance().gui.screen() instanceof SmithingScreen) {
				combinations(stack, lines);
			}
		});
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
			output.getDisplayStacks().removeIf(PowerTooltips::powerBook);
			output.getSearchTabStacks().removeIf(PowerTooltips::powerBook);
		});
	}

	private static boolean powerBook(ItemStack stack) {
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		return stored != null && stored.keySet().stream()
			.anyMatch(holder -> holder.unwrapKey().map(key -> key.identifier().getNamespace().equals(RpgAdditions.MOD_ID)).orElse(false));
	}

	/** A trim template names the power it grants on a weapon, and on a shield if it works there. */
	private static void template(ItemStack stack, List<Component> lines, boolean details) {
		String trim = Refinement.trimOf(stack);
		Refinement.Perk perk = trim == null ? null : Refinement.perk(trim);
		if (perk == null) {
			return;
		}
		lines.add(Component.translatable("tooltip.bcenchantments.template.weapon", perk.displayName().withStyle(ChatFormatting.BLUE))
			.withStyle(ChatFormatting.GRAY));
		if (!perk.built()) {
			lines.add(Component.literal(" ").append(Component.translatable("tooltip.bcenchantments.template.coming")).withStyle(ChatFormatting.DARK_GRAY));
		} else if (details) {
			Synergy.describe("tooltip.bcenchantments.power." + perk.id(), lines);
		}
		if (Refinement.onShield(perk)) {
			lines.add(Component.translatable("tooltip.bcenchantments.template.shield", perk.displayName().withStyle(ChatFormatting.BLUE))
				.withStyle(ChatFormatting.GRAY));
		}
	}

	/**
	 * At the smithing table, a fully refined weapon lists what its next trim would make: with a power
	 * waiting for a partner, every combination with it, white for trims the player carries and grey
	 * for the rest; otherwise, that the next trim starts a new pair. Also what the next power costs.
	 */
	private static void combinations(ItemStack stack, List<Component> lines) {
		if (!Refinement.isWeapon(stack) || Refinement.levelOf(stack) < Refinement.MAX_LEVEL) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		List<Refinement.Power> powers = Refinement.powersOf(stack);
		List<Component> added = new ArrayList<>();
		added.add(Component.empty());
		Refinement.Perk waiting = Refinement.unpaired(stack);
		if (waiting == null) {
			added.add(Component.translatable(powers.isEmpty() ? "tooltip.bcenchantments.combine.first" : "tooltip.bcenchantments.combine.new_pair")
				.withStyle(ChatFormatting.GRAY));
		} else {
			added.add(Component.translatable("tooltip.bcenchantments.combine.title").withStyle(ChatFormatting.GRAY));
			String own = Refinement.trimName(Refinement.trim(waiting));
			for (Refinement.Perk partner : Refinement.Perk.values()) {
				if (partner == waiting || !partner.built() || Refinement.perkLevel(stack, partner.enchantment()) > 0) {
					continue;
				}
				Synergy synergy = Synergy.of(waiting.enchantment(), partner.enchantment());
				if (synergy == null) {
					continue;
				}
				String trim = Refinement.trim(partner);
				Item template = BuiltInRegistries.ITEM.getValue(Refinement.template(trim));
				boolean carried = player != null && player.getInventory().contains(carriedStack -> carriedStack.is(template));
				added.add(Component.literal(" " + own + " + " + Refinement.trimName(trim) + " = ").append(synergy.displayName())
					.withStyle(carried ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
			}
		}
		int cost = Refinement.powerCost(powers.size());
		if (cost > 0) {
			added.add(Component.translatable("tooltip.bcenchantments.combine.cost", cost).withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.addAll(added);
	}
}
