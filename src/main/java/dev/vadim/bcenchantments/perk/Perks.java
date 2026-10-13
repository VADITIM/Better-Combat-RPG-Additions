package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Dungeons-style perks. Each is a data-driven enchantment ({@code data/bcenchantments/enchantment})
 * that a trim refinement can grant as well; the code behind it registers its own hooks in {@link #init}.
 * A new perk needs its enchantment JSON, a key here, a line in {@link #ALL}, and its hooks. Perks
 * that change each other on the same weapon are listed in {@link Synergy}.
 */
public final class Perks {
	public static final ResourceKey<Enchantment> LIGHTNING = key("lightning");
	public static final ResourceKey<Enchantment> SHOCKWAVE = key("shockwave");
	public static final ResourceKey<Enchantment> COLD_EDGE = key("cold_edge");
	public static final ResourceKey<Enchantment> UPPERCUT = key("uppercut");
	public static final ResourceKey<Enchantment> CHAINS = key("chains");
	public static final ResourceKey<Enchantment> ECHO = key("echo");
	public static final ResourceKey<Enchantment> WIND_CHARGED = key("wind_charged");

	/** Every perk, for anything that weighs how strong a weapon is. */
	public static final List<ResourceKey<Enchantment>> ALL = List.of(LIGHTNING, SHOCKWAVE, COLD_EDGE, UPPERCUT, CHAINS, ECHO, WIND_CHARGED);

	/** Perks that charge up over a combo, in the order the HUD shows their rows. Both sides index into this list. */
	public static final List<ChargedPerk> CHARGED = List.of(Lightning.INSTANCE, Shockwave.INSTANCE, Chains.INSTANCE, Echo.INSTANCE);

	private Perks() {
	}

	public static void init() {
		CombatHooks.onSwing(ColdEdge::onSwing);
		CombatHooks.onDirectHit(ColdEdge::onDirectHit);
		CombatHooks.onServerTick(ColdEdge::tick);
		CombatHooks.onSwing(Uppercut::onSwing);
		CombatHooks.onServerTick(Shockwave::tick);
		CombatHooks.onServerTick(Control::tick);
		CombatHooks.onServerTick(Impact::tick);
		CombatHooks.onKill(WindCharged::onKill);
		CombatHooks.onKill(Lightning::onKill);
	}

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, RpgAdditions.id(name));
	}

	/** Level of {@code key} on {@code stack}, counting a trim refinement's power, read from its components so it works on either side without a registry lookup. */
	public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
		// A trim refinement grants its power as if the weapon were enchanted with it; the higher of the two counts.
		int level = Refinement.perkLevel(stack, key);
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (holder.is(key)) {
				return Math.max(level, enchantments.getLevel(holder));
			}
		}
		return level;
	}

	/** Sum of every perk's level on {@code stack}: 0 for a plain weapon. */
	public static int totalLevel(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (ResourceKey<Enchantment> key : ALL) {
			total += level(stack, key);
		}
		return total;
	}
}
