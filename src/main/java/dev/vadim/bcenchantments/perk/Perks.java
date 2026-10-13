package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Dungeons-style perks, shown to players as Trim Powers. Each comes only from a trim (see
 * {@link Refinement}); the keys still name enchantments so old items and the registry agree, but no
 * book of one can be had. The code behind each registers its own hooks in {@link #init}. A new perk
 * needs a key here, a line in {@link #ALL}, a {@link Refinement.Perk} with its trim, and its hooks. Perks
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
	public static final ResourceKey<Enchantment> GRAVITY = key("gravity");
	public static final ResourceKey<Enchantment> MOMENTUM = key("momentum");
	public static final ResourceKey<Enchantment> RAMPAGING = key("rampaging");
	// Designed, not built yet: their trims are refused until they are (Refinement.Perk#built).
	public static final ResourceKey<Enchantment> VENOM = key("venom");
	public static final ResourceKey<Enchantment> SANDSTORM = key("sandstorm");
	public static final ResourceKey<Enchantment> PLUNDER = key("plunder");
	public static final ResourceKey<Enchantment> WITHERING = key("withering");
	public static final ResourceKey<Enchantment> PHASE = key("phase");
	public static final ResourceKey<Enchantment> SHARED_PAIN = key("shared_pain");
	public static final ResourceKey<Enchantment> GUARDING_STRIKE = key("guarding_strike");
	public static final ResourceKey<Enchantment> DAZE = key("daze");

	/** Every built perk, for anything that weighs how strong a weapon is. */
	public static final List<ResourceKey<Enchantment>> ALL = List.of(LIGHTNING, SHOCKWAVE, COLD_EDGE, UPPERCUT, CHAINS, ECHO, WIND_CHARGED,
		GRAVITY, MOMENTUM, RAMPAGING);

	/** Perks that charge up over a combo, in the order the HUD shows their rows. Both sides index into this list. */
	public static final List<ChargedPerk> CHARGED = List.of(Lightning.INSTANCE, Shockwave.INSTANCE, Chains.INSTANCE, Echo.INSTANCE);

	private Perks() {
	}

	public static void init() {
		CombatHooks.onSwing(ColdEdge::onSwing);
		CombatHooks.onDirectHit(ColdEdge::onDirectHit);
		CombatHooks.onServerTick(ColdEdge::tick);
		CombatHooks.onSwing(Uppercut::onSwing);
		Updraft.init();
		CombatHooks.onServerTick(Updraft::tick);
		CombatHooks.onServerTick(Shockwave::tick);
		CombatHooks.onServerTick(Control::tick);
		CombatHooks.onServerTick(Impact::tick);
		CombatHooks.onKill(WindCharged::onKill);
		CombatHooks.onKill(Lightning::onKill);
		CombatHooks.onSwing(Gravity::onSwing);
		// After Uppercut, which reads the run-up this spends.
		CombatHooks.onSwing(Momentum::onSwing);
		CombatHooks.onKill(Momentum::onKill);
		ServerTickEvents.END_SERVER_TICK.register(Momentum::tick);
		CombatHooks.onKill(Rampaging::onKill);
		CombatHooks.onServerTick(Rampaging::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(MaceTempest::afterDamage);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player && (Updraft.forgivesFall(player, source) || Uppercut.forgivesFall(player, source))));
	}

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, TrimEnchantments.id(name));
	}

	/**
	 * Level of {@code key} on {@code stack}, from its trims, read from its components so it works on
	 * either side without a registry lookup. Powers come only from trims: an old enchantment of one
	 * left on an item counts for nothing.
	 */
	public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
		return Refinement.perkLevel(stack, key);
	}
}
