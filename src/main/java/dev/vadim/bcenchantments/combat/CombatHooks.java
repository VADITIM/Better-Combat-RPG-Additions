package dev.vadim.bcenchantments.combat;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Where features plug into combat on the server. A new perk or weapon mechanic registers here from
 * its own {@code init} instead of being wired into {@link ComboTracker} by hand.
 */
public final class CombatHooks {
	/** Runs once after every Better Combat swing that landed at least one hit. */
	@FunctionalInterface
	public interface SwingEffect {
		void afterSwing(SwingResult swing);
	}

	/** Runs after a plain vanilla attack that landed outside any Better Combat swing. */
	@FunctionalInterface
	public interface DirectHitEffect {
		void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target);
	}

	private static final List<SwingEffect> SWING = new ArrayList<>();
	private static final List<DirectHitEffect> DIRECT_HIT = new ArrayList<>();
	private static final List<Runnable> SERVER_TICK = new ArrayList<>();

	private CombatHooks() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> SERVER_TICK.forEach(Runnable::run));
	}

	public static void onSwing(SwingEffect effect) {
		SWING.add(effect);
	}

	public static void onDirectHit(DirectHitEffect effect) {
		DIRECT_HIT.add(effect);
	}

	public static void onServerTick(Runnable task) {
		SERVER_TICK.add(task);
	}

	static void swing(SwingResult swing) {
		for (SwingEffect effect : SWING) {
			effect.afterSwing(swing);
		}
	}

	static void directHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		for (DirectHitEffect effect : DIRECT_HIT) {
			effect.onDirectHit(player, weapon, target);
		}
	}
}
