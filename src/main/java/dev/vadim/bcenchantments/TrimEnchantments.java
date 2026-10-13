package dev.vadim.bcenchantments;

import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.mob.MobScaling;
import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.network.CritRollPayload;
import dev.vadim.bcenchantments.perk.Perks;
import dev.vadim.bcenchantments.progression.Refinement;
import dev.vadim.bcenchantments.vanilla.FireAspect;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

/**
 * Trim Enchantments: a late-game enchanting layer on top of vanilla enchantments. On a fully refined
 * weapon every trim grants a Trim Power ({@code perk}) and any two combine. Builds on Better Combat and Punchy: each weapon kind plays its own way
 * ({@code weapon}), weapons are refined at the
 * smithing table ({@code progression}), and monsters scale up to meet the stronger player ({@code mob}).
 * The mod id stays {@code bcenchantments} so existing items and worlds keep their data.
 */
public class TrimEnchantments implements ModInitializer {
	public static final String MOD_ID = "bcenchantments";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(ComboHudPayload.TYPE, ComboHudPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CritRollPayload.TYPE, CritRollPayload.CODEC);
		CriticalHits.init();
		Refinement.init();
		WeaponStyles.init();
		Perks.init();
		MobScaling.init();
		CombatHooks.init();
		FireAspect.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
